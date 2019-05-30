/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*eslint-env browser */

/*
Polyfill  IE11
*/

//https://github.com/jserz/js_piece/blob/master/DOM/ChildNode/remove()/remove().md
(function (ElementRemove, TextRemove) {
    function remove() { this.parentNode && this.parentNode.removeChild(this); }

    if (!ElementRemove) ElementRemove = remove;
    if (Text && !TextRemove) TextRemove = remove;

})(Element.prototype.remove, Text.prototype.remove);


if (!Element.prototype.matches) {
    Element.prototype.matches = Element.prototype.msMatchesSelector || Element.prototype.webkitMatchesSelector
}

//https://developer.mozilla.org/en-US/docs/Web/API/Element/closest
//https://github.com/jonathantneal/closest/blob/master/src/index.js
if (!Element.prototype.closest) {
    Element.prototype.closest = function (selector) {
        var element = this;

        while (element && (element.nodeType === 1) && !element.matches(selector)) {
            element = element.parentNode;
        }
        return element ? element : null;
    }
}
