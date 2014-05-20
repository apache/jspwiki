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
Class: Placeholder
    Polyfill for non-html5 browsers.

DOM structure:
>    <input name="search" placeholder="Search..." />

Example:
>    $$('input[paceholder]').placeholder();

*/

Element.implement({

    placeholderX: ('xxplaceholder' in document.createElement('input')) ? function(){} : function(){

        var element = this,
            span = new Element('span.placeholder[role=display]',{
                text: element.placeholder,
                styles: {
                    position: 'relative',
                    top: element.offsetTop,
                    left: element.offsetLeft
                }
            });

        element.addEvents({
            focus: function(){ span.hide(); },
            blur: function(){ if (!element.value && !element.innerHTML){ span.show(); } }
        });

        element.offsetParent.appendChild(span);

        return element;
    }

});


