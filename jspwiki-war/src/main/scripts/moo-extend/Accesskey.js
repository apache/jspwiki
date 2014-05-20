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
    - underline the accesskey ( wrap inside span.accesskey )
    - add a suffix to the title attribute of the element with the accesskey
      in square brackets : "title [ key ]"

Arguments:
    element - DOM element
    template - (string) html template replacement string, default <span class='accesskey'>$1</span>
*/

function Accesskey(element, template){

    if( !element.getElement('span.accesskey') ){

        var key = element.get('accesskey'),
            title = element.get('title');

        if( key ){

            element.set({
                html: element.get('html').replace(
                    RegExp( '('+key+')', 'i'),
                    template || "<span class='accesskey'>$1</span>" )
            });
            if( title ){ element.set('title', title + ' [ '+key+' ]'); }

           //console.log("ACCESSKEY ::",key, element.get('text'), element.get('title') );

        }
    }
}
