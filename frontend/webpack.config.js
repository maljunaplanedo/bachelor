const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const webpack = require('webpack');

module.exports = {
    mode: process.env.DBHUB_WEBPACK_MODE,
    entry: path.join(__dirname, 'app', 'index.tsx'),
    resolve: {
        extensions: ['.ts', '.tsx', '.js'],
    },
    module: {
        rules: [
            {
                test: /\.tsx?$/,
                use: 'ts-loader',
                exclude: '/node_modules/',
            },
            {
                test: /\.css$/,
                use: [
                    MiniCssExtractPlugin.loader,
                    'css-loader'
                ]
            },
            {
                test: /\.ttf$/,
                type: 'asset/resource'
            }
        ],
    },
    output: {
        filename: '[name].js',
        path: path.resolve(__dirname, 'dist'),
    },
    plugins: [
        new HtmlWebpackPlugin({
            template: path.join(__dirname, 'app', 'index.html'),
        }),
        new MiniCssExtractPlugin({
            filename: 'style.css',
        }),
        new webpack.DefinePlugin({
            'process.env.DBHUB_API_URL': JSON.stringify(process.env.DBHUB_API_URL),
        })
    ],
}
