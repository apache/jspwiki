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
Function: hsv2rgb
    Convert HSV values into RGB values
    Credits: www.easyrgb.com/math.php?MATH=M21#text21
*/
Array.implement({

    hsv2rgb: function(){

        var self = this,
            r,A,B,C,F,
            hue = self[0]/360, //hue - degrees from 0 to 360
            sat = self[1]/100, //saturation - %
            val = self[2]/100; //value - %

        if( !sat/*sat==0*/ ){

            r=[val,val,val];

        } else {

            hue = (hue>1) ? 0 : 6 * hue;
            F = hue|0;  //Math.floor(hue);
            A = val * (1 - sat);
            B = val * (1 - sat * (hue-F) );
            C = val + A - B; //val * (1 - sat * ( 1 - (hue-F)) );
            //val = Math.round(val);

            r = (F==0) ? [val, C, A] :
                (F==1) ? [B, val, A] :
                (F==2) ? [A, val ,C] :
                (F==3) ? [A, B, val] :
                (F==4) ? [C, A, val] : [val, A, B];

        }

        return r.map( function(x){ return(.5 + x*255)|0; }); ///Math.round()
        //return [ (.5+r[0]*255)|0, (.5+r[1]*255)|0, (.5+r[2]*255)|0 ];

    },

    rgb2hsv: function(){

        var self = this,
            hue = 0, //hue - degrees from 0 to 360
            sat = 0, //saturation - %
            tmp = 255,
            r = self[0]/tmp, dR,
            g = self[1]/tmp, dG,
            b = self[2]/tmp, dB,
            maxRGB = [r,g,b].max(),         //Math.max(r, g, b),
            deltaRGB = maxRGB - [r,g,b].min(); //Math.min(r, g, b);

        if( deltaRGB ){    //if deltaRGB==0 : this is a gray, no chroma; otherwise chromatic data

            sat = deltaRGB / maxRGB;

            tmp = deltaRGB / 2;
            dR = (((maxRGB - r) / 6) + tmp) / deltaRGB;
            dG = (((maxRGB - g) / 6) + tmp) / deltaRGB;
            dB = (((maxRGB - b) / 6) + tmp) / deltaRGB;

            hue = (r == maxRGB) ? dB - dG
                : (g == maxRGB) ? (1/3) + dR - dB
                : /*b == maxRGB*/ (2/3) + dG - dR;

            if (hue < 0) { hue += 1; }
            if (hue > 1) { hue -= 1; }

        }

        return [ hue*360, sat*100, maxRGB*100 ];

    }
});
