webpackJsonp([2,8,9],{

/***/ 0:
/***/ (function(module, exports, __webpack_require__) {

	"use strict";

	var ReplaceCharacter = __webpack_require__(139).ReplaceCharacter;

	var ParseLinks = __webpack_require__(140).ParseLinks;

	var ReplaceStringForAol = __webpack_require__(141).ReplaceStringForAol;

	module.exports = angular.module("artifactory.filters", []).filter("replaceCharacter", ReplaceCharacter).filter("parseLinks", ParseLinks).filter("replaceStringForAol", ReplaceStringForAol);

/***/ }),

/***/ 139:
/***/ (function(module, exports) {

	"use strict";

	exports.ReplaceCharacter = ReplaceCharacter;
	Object.defineProperty(exports, "__esModule", {
	    value: true
	});

	function ReplaceCharacter() {

	    return function (input, search, replace) {
	        if (input) {
	            var regex = new RegExp(search, "g");
	            return input.replace(regex, replace);
	        }

	        return input;
	    };
	}

/***/ }),

/***/ 140:
/***/ (function(module, exports) {

	"use strict";

	exports.ParseLinks = ParseLinks;
	Object.defineProperty(exports, "__esModule", {
	    value: true
	});

	function ParseLinks() {

	    return function (str) {
	        return str.replace(/\[(.*?)\]/g, function (match) {
	            var linkData = match.substring(1, match.length - 1).split(",");
	            if (linkData.length == 2) return "<a href=\"" + linkData[0].trim() + "\" target=\"_blank\">" + linkData[1].trim() + "</a>";else return match;
	        });
	    };
	}

/***/ }),

/***/ 141:
/***/ (function(module, exports) {

	"use strict";

	exports.ReplaceStringForAol = ReplaceStringForAol;
	Object.defineProperty(exports, "__esModule", {
	    value: true
	});
	// TODO: pass in key to filter instead of the text
	var dictionary = {
	    "Import & Export": "Import",
	    "Repositories Import & Export": "Import Repositories"
	};

	function ReplaceStringForAol(ArtifactoryFeatures) {

	    return function (str) {
	        if (ArtifactoryFeatures.isAol()) {
	            return dictionary[str] || str;
	        } else {
	            return str;
	        }
	    };
	}

/***/ })

});