const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// change the server path to your server location path
const exoServerPath = "/exo-server/";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, exoServerPath + 'webapps/chat/')
  },
  devtool: 'inline-source-map'
});

module.exports = config;
