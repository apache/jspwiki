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
Class: Color
    Class for creating and manipulating colors in JavaScript.
    This is a minimized variant of the Color class, based Mootools.More,
    written for jspwiki.
    It adds supports for html color names. (ref. http://en.wikipedia.org/wiki/Web_colors)

Arguments:
    color - (mixed) A string or an array representation of a color.

Color:
    There are 4 representations of color: #hex (3 or 6 chars) , vga color name, X11 color names, RGB (array).
    For String representation see Element:setStyle for more information.

Returns:
    (array) A new Color instance.

Examples:
    > var black = new Color('#000');
    > var black = new Color('rgb(0,0,0)');
    > var purple = new Color([255,0,255]);
    > var purple = new Color(255,0,255);
    > var red = new Color('red'); //support 16 standard vga color names
    > var azure = new Color('azure'); //support all 130 additional X11 color names

*/

!(function(){

var c0l0r = 'i'.slick(),
    Color = this.Color = new Type('Color', function(color){

    if (arguments.length >= 3){

        color = Array.slice(arguments, 0, 3);

    } else if (typeof color == 'string'){

        c0l0r.inject(document.body);
        color = ( color.test(/^[\da-f]{3,6}$/i) ?  ("#" + color) :
                 c0l0r.setStyle('color',color).getComputedStyle('color').rgbToHex() ).hexToRgb(true);  //[r,g,b]
        c0l0r.dispose(); //remove element from the dom

    }
    if(!color){ return null; }
    color.rgb = color.slice(0, 3);
    color.hex = color.rgbToHex();

    return Object.append(color, this);
});

Color.implement({

    mix: function(){

        var colors = Array.slice(arguments),
            alpha = ( (typeOf(colors.getLast()) == 'number') ? colors.pop() : 50 )/100,
            alphaI = 1 - alpha,
            rgb = this.slice(), i, color;

        while( colors[0] ){

            color = new Color( colors.shift() );
            for (i = 0; i < 3; i++){
                rgb[i] = ((rgb[i] * alphaI) + (color[i] * alpha)).round();
            }

        }
        return new Color(rgb);

    },

    invert: function(){

        return new Color(255-this[0], 255-this[1], 255-this[2]);

    }

});

})();