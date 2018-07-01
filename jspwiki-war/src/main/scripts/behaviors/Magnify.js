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
Dynamic Style: Magnifier
    Add a magnifying overlay glass to images on mouseover.

Inspired by: https://github.com/marcaube/bootstrap-magnify

*/

function Magnify( images ){

    //only one magnifier can be visible at the same time
    var maggy = "div.magnifier".slick().inject(document.body);

    function move_maggy( event ){

        var img = event.target,
            isVisible = /move/.test(event.type);

        if( isVisible ){

            var box = img.getCoordinates(),
                mouseLeft = event.page.x,
                mouseTop = event.page.y,
                radius = maggy.offsetWidth / 2,  //maggy radius : width==height

                bgX = Math.round( radius - (mouseLeft - box.left) / box.width * img.naturalWidth ),
                bgY = Math.round( radius - (mouseTop - box.top) / box.height * img.naturalHeight );

            //console.log(box.height, img.naturalWidth, img.naturalHeight, img.naturalHeight/box.height, bgX+"px "+bgY+"px" );

            maggy.setStyles({
                left: mouseLeft - radius,
                top: mouseTop - radius,
                backgroundImage: "url('" + img.src + "')",
                backgroundPosition: bgX + "px " + bgY + "px"
            });

            /*avoid that the whole screen starts to scroll around when touch-down*/
            if( /touch/.test(event.type) ){ event.preventDefault(); }

        }

        maggy.ifClass( isVisible , "show" );

    }

    $$( images ).addEvents({
        mousedown: function(event){ event.stop(); },  //avoid dragging/selecting the img
        mousemove: move_maggy,
        touchmove: move_maggy,
        mouseleave: move_maggy,
        touchend: move_maggy
    });

}
