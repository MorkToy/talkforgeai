const {defineConfig} = require('@vue/cli-service');
module.exports = defineConfig({
  transpileDependencies: true,
  // proxy all webpack dev-server requests starting with /api
  // to our Spring Boot backend (localhost:8098) using http-proxy-middleware
  // see https://cli.vuejs.org/config/#devserver-proxy
  devServer: {
    proxy: {
      '^/api': {
        target: 'http://localhost:8090', // this configuration needs to correspond to the Spring Boot backends' application.properties server.port
        changeOrigin: true,
        ws: true, // websockets
        compress: false, // deactivates compression, which is enabled by default. Needed for SSE.
      },
    },
  },
  // Change build paths to make them Maven compatible
  // see https://cli.vuejs.org/config/
  outputDir: 'target/dist',
  assetsDir: 'static',
});
