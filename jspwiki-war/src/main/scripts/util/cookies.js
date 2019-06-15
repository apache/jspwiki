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
Minimal cookie handling

Get a cookie
>  $.cookie("sweetCookie");

Set a cookie  (returns the name of the cookie)
> $.cookie("sweetCookie", "some-data");
> $.cookie("sweetCookie", "some-data", { expiry: 5 });  //expires in 5 days
> $.cookie({name:"sweetCookie", expiry:5}, "some-data");  //expires in 5 days
*/
$.cookie = function (name, value, options) {

    if (name.name) {
        options = name;
        name = options.name;
    }

    if (!/%/.test(name)) { name = encodeURIComponent(name); } //avoided double encoding..

    if (value == undefined) {
        // read cookie
        value = document.cookie.match('(?:^|;)\\s*' + name + '=([^;]*)');
        return (value) ? decodeURIComponent(value[1]).replace(/\+/g, " ") : null;
    }

    // write or delete a cookie
    var brownies = name + '=' + encodeURIComponent(value);

    if (options) {
        if ('path' in options) brownies += ';path=' + options.path;
        if ('domain' in options) brownies += ';domain=' + options.domain;
        if ('secure' in options && options.secure) brownies += ';secure';
        if ('expiry' in options) {
            var expiry = options.expiry;
            if (!isNaN(expiry)) {
                //its a number, indicating the relative number of days ; to be converted to milliseconds
                expiry = new Date(expiry * 864e5 /*1000 * 60 * 60 * 24*/ + Date.now());
            }
            brownies += ';expires=' + expiry.toUTCString();
        }
    }
    document.cookie = brownies;
}
$.cookie.delete = function (name) { $.cookie(name, "", { expiry: -1 }); }


/*
JSON encoded cookies

> $.cookie.json({name:cookie-name, expiry:20}, "version");         //get version
> $.cookie.json({name:cookie-name, expiry:20}, "version", "v-27"); //change version
> $.cookie.json({name:cookie-name, expiry:20}, "");                //erase cookie

*/
$.cookie.json = function (cookieParams, key, value) {

    var $cookie = $.cookie,
        json = $cookie(cookieParams);  //read the browser cookie

    if (key == "") { $cookie.delete(cookieParams.name); }

    json = JSON.parse(json) || {};  //ffs: do we need to catch SyntaxError?

    if (value != undefined) { //write prefs
        json[key] = value;
        $cookie(cookieParams, JSON.stringify(json));
    }
    return json[key];
}
