const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(__dirname, './target/chat/'),
    filename: 'js/[name].bundle.js'
  }
});

module.exports = config;