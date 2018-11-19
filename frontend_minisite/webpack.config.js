require('dotenv').config()
const path = require('path')
const webpack = require('webpack')

const { VueLoaderPlugin } = require('vue-loader')
const MiniCssExtractPlugin = require('mini-css-extract-plugin')
const FriendlyErrorsWebpackPlugin = require('friendly-errors-webpack-plugin')
const WebpackNotifierPlugin = require('webpack-notifier')
const HtmlWebpackPlugin = require('html-webpack-plugin')

const webpackDevServerPort = parseInt(process.env.PORT || '3000', 10)
const production = process.env.NODE_ENV === 'production'
const api_url = process.env.API_URL || 'https://api.ipify.org?format=text'

const routes = [
  {
    title: 'Home',
    page: 'index'
  }
]

let postcssPlugins = [
  require('autoprefixer')()
]

if (production) {
  postcssPlugins.push(require('cssnano')())
}

let cssLoaders = [
  production ? MiniCssExtractPlugin.loader : 'vue-style-loader',
  {
    loader: 'css-loader',
    options: {
      minimize: production,
      sourceMap: true
    }
  },
  {
    loader: 'postcss-loader',
    options: {
      ident: 'postcss',
      sourceMap: true,
      plugins: postcssPlugins
    }
  }
]

module.exports = {
  entry: {
    app: [
      './assets/js/app.js',
      './assets/sass/app.scss'
    ]
  },
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: production ? 'js/[name].[chunkhash].js' : 'js/[name].js'
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: cssLoaders
      },
      {
        test: /\.scss$/,
        use: cssLoaders.concat([
          {
            loader: 'sass-loader',
            options: {
              outputStyle: 'expanded',
              sourceMap: true
            }
          }
        ])
      },
      {
        test: /\.(js|vue)$/,
        loader: 'eslint-loader',
        enforce: 'pre'
      },
      {
        test: /\.vue$/,
        loader: 'vue-loader'
      },
      {
        test: /\.js$/,
        exclude: /node_modules/,
        loader: 'babel-loader',
        options: {
          presets: ['@babel/preset-env']
        }
      },
      {
        test: /\.html$/,
        loader: 'html-loader'
      },
      {
        test: /\.(png|jpe?g|gif)$/,
        use: [
          {
            loader: 'url-loader',
            options: {
              name: 'images/[name].[ext]?[hash]',
              limit: 4096
            }
          }
        ]
      },
      {
        test: /\.(woff2?|ttf|eot|svg|otf)$/,
        loader: 'url-loader',
        options: {
          name: 'fonts/[name].[ext]?[hash]',
          limit: 4096
        }
      },
      {
        test: /\.hbs$/,
        loader: 'handlebars-loader',
        options: {
          rootRelative: '../',
          helperDirs: [
            path.join(__dirname, 'views', 'helpers')
          ]
        }
      }
    ]
  },
  optimization: {
    splitChunks: {
      chunks: 'initial'
    }
  },
  plugins: [
    new webpack.DefinePlugin({
      "API_URL": JSON.stringify(api_url)
    }), 
    new webpack.ProvidePlugin({
      $: 'jquery',
      jQuery: 'jquery',
      'window.jQuery': 'jquery',
      Popper: ['popper.js', 'default']
    }),
    new VueLoaderPlugin(),
    new MiniCssExtractPlugin({
      filename: 'css/[name].[chunkhash].css'
    }),
    new FriendlyErrorsWebpackPlugin(),
    new WebpackNotifierPlugin()
  ],
  resolve: {
    extensions: ['.js', '.vue', '.json'],
    alias: {
      'sweetalert2$': 'sweetalert2/dist/sweetalert2.js',
      'vue$': 'vue/dist/vue.esm.js',
      'handlebars': 'handlebars/dist/handlebars.js'
    }
  },
  devtool: production ? 'source-map' : 'cheap-module-eval-source-map',
  devServer: {
    historyApiFallback: true,
    noInfo: true,
    compress: true,
    quiet: true,
    port: webpackDevServerPort
  }
}

routes.forEach((route) => {
  module.exports.plugins.push(
    new HtmlWebpackPlugin({
      title: route.title,
      apiUrl: api_url,
      filename: `${route.page}.html`,
      template: `views/pages/${route.page}.hbs`,
      minify: {
        removeComments: true,
        collapseWhitespace: true,
        removeAttributeQuotes: true
      }
    })
  )
})
