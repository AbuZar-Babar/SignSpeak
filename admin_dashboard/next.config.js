/** @type {import('next').NextConfig} */
const path = require('path');
const webpack = require('webpack');

const nextConfig = {
    transpilePackages: ['firebase', '@firebase/auth'],
    serverExternalPackages: ['undici'],
    // Turbopack config (Next.js 16+ uses Turbopack by default)
    turbopack: {},
    // Webpack config for compatibility
    webpack: (config, { isServer, webpack }) => {
        if (!isServer) {
            // Exclude Node.js modules from client bundle
            config.resolve.fallback = {
                ...config.resolve.fallback,
                undici: false,
                fs: false,
                net: false,
                tls: false,
                crypto: false,
            };
            
            // Alias undici to empty module for client builds
            config.resolve.alias = {
                ...config.resolve.alias,
                'undici': path.resolve(__dirname, 'lib/empty-module.js'),
            };
            
            // Use NormalModuleReplacementPlugin to replace undici imports
            config.plugins.push(
                new webpack.NormalModuleReplacementPlugin(
                    /^undici$/,
                    path.resolve(__dirname, 'lib/empty-module.js')
                )
            );
            
            // IgnorePlugin to prevent bundling undici
            config.plugins.push(
                new webpack.IgnorePlugin({
                    resourceRegExp: /^undici$/,
                })
            );
        }
        return config;
    },
}

module.exports = nextConfig
