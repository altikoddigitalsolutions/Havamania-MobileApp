const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');

/**
 * Metro configuration
 * https://reactnative.dev/docs/metro
 *
 * @type {import('metro-config').MetroConfig}
 */
const defaultConfig = getDefaultConfig(__dirname);

const config = {
  resolver: {
    // Metro'nun Android derleme dosyalarını izlemesini engelleyerek ENOENT hatalarını çözeriz
    blacklistRE: /android\/|node_modules\/.*\/android\/.*/,
    blockList: [
      /.*\/android\/.*/,
      /.*\/node_modules\/.*\/android\/.*/,
      /.*\/build\/.*/,
      /.*\/\.cxx\/.*/
    ],
  },
  // İzlenmeyecek klasörler
  watchFolders: [],
};

module.exports = mergeConfig(defaultConfig, config);
