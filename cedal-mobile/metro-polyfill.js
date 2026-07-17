// metro-polyfill.js
if (!Array.prototype.toReversed) {
  // Non-mutating reverse polyfill
  // eslint-disable-next-line no-extend-native
  Array.prototype.toReversed = function () {
    return [...this].reverse();
  };
}
