const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

const CopyWebpackPlugin = require('copy-webpack-plugin');
const apiMocker = require('connect-api-mocker');
// change the server path to your server location path
const exoServerPath = "/media/boubaker/Data/exo_sources/exo-working/platform-5.1.x-SNAPSHOT/";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, exoServerPath + 'webapps/chat/'),
    filename: 'js/[name].bundle.js'
  },
  devServer: {
    contentBase: path.resolve("./src/main/webapp"),
    before: function(app) {
      app.use('/chatServer', apiMocker('src/main/webapp/js/mock'));
    },
    port: 4000,
    proxy: {
        '/rest/': {
          target:'http://localhost:8080',
          changeOrigin: true
        },
        '/portal/rest/': {
          target:'http://localhost:8080',
          changeOrigin: true
        },
        '/chat/': {
          target:'http://localhost:4000',
          pathRewrite: {'^/chat' : ''}
        }
    }
  },
  devtool: 'inline-source-map'
});

config.plugins.push(new CopyWebpackPlugin([{from: 'src/main/webapp/lang/*.json', to: './lang', flatten: true}]));

module.exports = config;
