/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*jslint forin: true, onevar: true, nomen: true, plusplus: true, immed: true */

/*
Function: toISOString
    Return the current date in ISO8601 format 'yyyy-mm-dd'.
    (ref. EcmaScript 5)

Example:
> alert( new Date().toISOString() ); // alerts 2009-05-21
> alert( new Date().toISOString() ); // alerts 2009-05-21T16:06:05.000TZ
*/

/* Obsolete -- now covered by ECMAScript5, ok in most browsers
Date.extend({
    toISOString: function(){
        var d = this,
            dd = d.getDate(),
            mm = d.getMonth()+1;

        return d.getFullYear() + '-' + (mm<10 ?'0':'') + mm + '-' + (dd<10 ?'0':'') + dd;
    }
});
*/
