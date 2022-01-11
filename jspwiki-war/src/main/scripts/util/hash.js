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
Function: setHash
    Set URL #HASH without page jump; fallback if pushState not supported

Example:
>	"here is a new hash".setHash();
*/
$.setHash = function(newHash) {

    if (history.pushState) {

        //history.pushState( state-object, title-ffs, "#" + hash );
        history.pushState(null, "", "#" + newHash);

    } else {

        var el = $(newHash),        //fixme  $("#"+newHash)
            id = el && el.id;

        el && el.removeAttribute(id); //prevent page jump

        location.hash = "#" + newHash;

        el && el.setAttribute("id", id);
    }
}
