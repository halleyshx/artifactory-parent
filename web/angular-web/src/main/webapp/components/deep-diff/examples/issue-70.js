var deepDiff = require("../");

var left = {foo: undefined};
var right = {};

console.log(deepDiff.diff(left, right));
