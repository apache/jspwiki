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
/*global $, $$, getElement */

//FIXME :: move to extend-event.js
/**
 *
 */
$.bind = function(element, eventNames, callback) {
    //fixme: replace mootools api
    //fixme: allow eventNames to support mulitple space-separated event types
    //fixme: allow (el,event,cb)   and (el, {ev1:cb, ev2:cb})
    element.addEvent(eventNames, callback);
};
/**
 *
 */
$.unbind = function(element, eventNames, callback) {
    //fixme: replace mootools api
    element.removeEvent(eventNames, callback);
};

/**
 * Similar to $.bind(), but callback will be called only once
 */
$.once = function(element, eventNames, callback) {
    function once() {
        $.unbind(element, eventNames, once);
        return callback.apply(element, arguments);
    }
    $.bind(element, eventNames, once);
};

/**
 * Event delegation
 * @param {HTMLElement} element
 * @param {string} eventNames
 * @param {string} targetSelector - the selector to match the target of the event
 * @param {function} callback
 * @example:
 *   $.delegate($("ul"), "click", "li", handleListItem);
 */
$.delegate = function(element, eventNames, targetSelector, callback) {
    $.bind(element, eventNames, function(event) {
        if (event.target.closest(targetSelector)) {
            callback.call(event.target, event);
        }
    });
};
