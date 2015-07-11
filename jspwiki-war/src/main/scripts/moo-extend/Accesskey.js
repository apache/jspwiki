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
/*
Function: Accesskey
    Highlight the available accesskeys to the user:
    - wrap the access key inside a span.accesskey to apply css style
      (eg. underlining the access key)
    - add a suffix to the title attribute of the element with the accesskey
      in square brackets : "title [ key ]"

Arguments:
    element - DOM element

Example:
(start code)
    new Accesskey( $('#menu) );

    //before
    <a id="menu" accesskey="m" title="main menu">Menu</a>

    //after
    <a id="menu" accesskey="m" title="main menu [m]"><span class="accesskey">M</span>enu</a>

(end)
*/

function Accesskey(element){

    var accesskey = "accesskey",
        key = element.get( accesskey ).slice(0,1),
        title = element.get( "title" );

    if( key && !element.getElement( "span." + accesskey ) ){


        element.set({
            html: element.get('html').replace(
                RegExp( "^([^<]*)(" + key + ")", "i" ),
                "$1<span class='" + accesskey + "'>$2</span>"
            )
        });

        if(title){ element.set( "title", title + " [" + key + "]" ); }

        //console.log("ACCESSKEY ::",key, element.get('text'), element.get('title') );

    }
}