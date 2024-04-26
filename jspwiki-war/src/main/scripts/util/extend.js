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
/*global $ */

/*
Basic js helper routines, inspired on Bliss.js <url>
    $.extend
    $.overload
*/

/**
 * Extend
 *    Returns extended objects
 * @param {Object} target
 * @param {Object} src1
 * @returns {Object}
 */
$.extend = function(target, src1) {
    //todo
};

/*
Overload
*/
$.overload = function() {};

/**
 * Returns the [[Class]] of an object in lowercase (eg. array, date, regexp, string etc)
 * Inspired by: https://blissfuljs.com/docs.html#fn-type
 * @param {any} obj
 * @returns {string} type of the object in lowercase
 */
$.type = function(obj) {
    var ret;

    if (obj === null || obj === undefined) {
        return '' + obj;
    }

    if (
        (ret = (
            Object.prototype.toString
                .call(obj)
                .match(/^\[object\s+(.*?)\]$/)[1] || ''
        ).toLowerCase()) == 'number' &&
        isNaN(obj)
    ) {
        return 'nan';
    }
    return ret;
};

/**
 * Simple async wait to execute callback until after the DOM is ready
 * @param {function} callback
 */
$.ready = function(callback) {
    var context = document;
    if (context.readyState !== 'loading') {
        callback();
    } else {
        $.once(context, 'DOMContentLoaded', callback);
    }
};
