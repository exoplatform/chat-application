const path = require("path");
const CopyWebpackPlugin = require("copy-webpack-plugin");
const ExtractTextWebpackPlugin = require("extract-text-webpack-plugin");
const dev = process.env.NODE_ENV == "dev";
const exoServerPath = "../../../exo-servers/platform-5.1-chat-ux/";

let config = {
  context: path.resolve(__dirname, "."),
  entry: {
    chat: "./src/main/webapp/vue-app/main.js"
  },
  output: {
    path: dev ? path.resolve(__dirname, exoServerPath + "webapps/chatApplicationVue/") : path.resolve(__dirname, './target/chatApplicationVue/'),
    filename: "js/[name].bundle.js"
  },
  devServer: {
    contentBase: path.resolve("./src/main/webapp"),
    port: 4000,
    proxy: {
        '/rest/': {
          target:'http://localhost:8080',
          changeOrigin: true
        },
        '/chatServer': {
          target:'http://localhost:3000',
          changeOrigin: true
        },
        '/chatApplicationVue/': {
          target:'http://localhost:4000',
          pathRewrite: {"^/chatApplicationVue" : ""}
        }
    }
  },
  devtool: dev ? "inline-source-map" : false,
  module: {
    rules: [
      {
        test: /\.css$/,
        use: ["vue-style-loader", "css-loader"]
      },
      {
        test: /\.less$/,
        use: ExtractTextWebpackPlugin.extract({
          fallback: 'vue-style-loader',
          use: [
            {
              loader: 'css-loader',
              options: {
                sourceMap: true
              }
            },
            {
              loader: 'less-loader',
              options: {
                sourceMap: true
              }
            }
          ]
        })
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          "babel-loader",
          "eslint-loader",
        ]
      },
      {
        test: /\.vue$/,
        loader: "vue-loader"
      }
    ]
  },
  plugins: [
    new ExtractTextWebpackPlugin("css/[name].css")
  ],
  externals: {
    vue: 'Vue'
  }
};

if (dev) {
  config.plugins.push(new CopyWebpackPlugin([{from: 'src/main/webapp/lang/*.json', to: './lang', flatten: true}]));
}

module.exports = config;