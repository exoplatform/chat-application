const path = require('path');
const { merge } = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

let config = merge(webpackCommonConfig, {
  mode: 'production',
  output: {
    path: path.resolve(__dirname, './target/chat/')
  }
});

module.exports = config;