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

/* FORM element support routines */

/**
 * Function: observe
 *     Observe a dom element for changes, and trigger a callback function.
 *
 * @param {InputElement} element
 * @param {Function} callback - this function is called when the dom element changed
 * @param {Number} [delay=300] - timeout in ms, default = 300ms
 * @param {String} [event="keyup"] - event-type to observe
 *
 * @example
 *     $.observe(formInput, function(){
 *         alert("my value changed to "+this.get("value") );
 *     });
 *
 */
$.observe = function(element, callback, delay, event) {
    var refValue = element.value,
        timer = null;

    function hasChanged() {
        var value = element.value;
        if (value != refValue) {
            refValue = value;
            clearTimeout(timer);
            timer = setTimeout(callback, delay);
        }
    }

    if (isNaN(delay)) {
        event = delay;
        delay = 300;
    }

    event = event || 'keyup';
    //$.set(element, { autocomplete: 'off', events: { event, hasChanged } });
    //$.set(element, { autocomplete: 'off', events: { event, hasChanged } });
    element.set({ autocomplete: 'off' }).addEvent(event, hasChanged);
    return element;
};

/**
 * FORM element
 *   Function: getDefaultValue
 *       Returns the default value of a form element.
 *       Inspired by get("value") of mootools, v1.1
 *
 *   Note:
 *       Checkboxes will return true/false depending on the default checked status.
 *       ( input.checked to read actual value )
 *       The value returned in a POST will be input.get("value")
 *       and is depending on the value set by the "value" attribute (optional)
 *
 *   Returns:
 *       (value) - the default value of the element; or false if not applicable.
 *
 *   Examples:
 *   > $("thisElement").getDefaultValue();
 *
 */
$.getDefaultValue = function(element) {};
