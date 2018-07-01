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
Dynamic Style: Tips
    Add Tip behavior to a set of DOM Elements

Bootstrap
(start code)
    //tip anchors
    <element> Caption
        <body> ...body... </body>
    </element>

    //layout of the tip, with absolute position
    div.tooltip(.active)(.top|.left|.right|.bottom)
        div.tooltip-inner
            <body> ... </body>
        div.tooltip-arrow
(end)
*/
var Tips = function Tips(elements){

        var tt = 'div.tooltip',
            zTip = [tt,[tt+'-inner'/*,tt+'-arrow'*/]].slick().inject(document.body),
            inner = zTip.firstChild,
            isVisible;

        function tipEvent( e ){

            if( /move/.test(event.type) ){

                if( !isVisible ){

                    /*not yet visible:  copy content into the TIP */
                    inner.adopt( this.firstChild );
                    isVisible = true;

                }

                zTip.setStyles({ top:e.page.y +10, left:e.page.x + 10 });

            } else {

                /* xx-leave event:  move content back to the main body*/
                this.adopt( inner.firstChild );
                isVisible = false;
            }

            zTip.ifClass( isVisible, "in" );

        }

        $$(elements).addEvents({
            touchmove: tipEvent,
            mousemove: tipEvent,
            touchend: tipEvent,
            mouseleave: tipEvent
        });

};
