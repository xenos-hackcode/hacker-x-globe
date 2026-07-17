// metro.config.js
require("./metro-polyfill"); // make sure toReversed exists

const { getDefaultConfig } = require("@expo/metro-config");

const config = getDefaultConfig(__dirname);

// Silence invalid exports for unused three.js example loaders
config.resolver = config.resolver || {};

// CanvasKit (Skia web) ships a .wasm binary that Metro must treat as an asset
config.resolver.assetExts = [...config.resolver.assetExts, "wasm"];

config.resolver.blockList = [
  /three[\\/]examples[\\/]jsm[\\/]loaders[\\/]ColladaLoader/,
  /three[\\/]examples[\\/]jsm[\\/]loaders[\\/]GLTFLoader/,
  /three[\\/]examples[\\/]jsm[\\/]loaders[\\/]MTLLoader/,
  /three[\\/]examples[\\/]jsm[\\/]loaders[\\/]OBJLoader/,
];

module.exports = config;
