const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// change the server path to your server location path
const exoServerPath = "/home/exo/Server/binaries/plfent-6.3.x-maintenance-20211122.164455-844/platform-6.3.x-maintenance-SNAPSHOT/";
let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, exoServerPath + 'webapps/chat/')
  },
  devtool: 'inline-source-map'
});

module.exports = config;
