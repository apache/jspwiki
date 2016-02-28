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
/*global Class, Dialog, Color, Drag  */
/*exported Dialog.Color */
/*
Class: Dialog.Color
    The Dialog.Color is a [Dialog] which allow visual entry of hexadecimal color
    values.

Inspiration:
    - http://www.colorjack.com/software/dhtml+color+sphere.html
    - Chris Esler, http://www.chrisesler.com/mootools

Inherits from:
    [Dialog]

Example:
    Dialog.Color with toggle button
    (start code)
    <script>
        var cd = new Dialog.Color( {
            relativeTo: $("colorButton"),
            wheelImage:"circle.png",
            onChange:function(color){ $("mytarget").setStyle("background",color); }
        });
        $("colorButton").addEvent("click", cd.toggle.bind(cd));
    </script>
    (end code)
*/
Dialog.Color = new Class({

    Extends: Dialog, //Dialog.Buttons,
    options: {

        //onAction: function(color){},
        //onDrag: function(color){},
        //onResize: function(){},
        color:"#ffffff", //initial color
        //buttons:["Done"],
        //colors: [],  //todo: colors array with predef pallette
        //colorImage: "scripts-dev/main/src/styles/generated-css/images/circle-256.png",
        resize:{x:[96,256]}  //min/max limits for the resizer
    },

    initialize: function(options){

        var self = this,
            cursor,
            showNow = options.showNow,
            setHSV = self.setHSV.bind(this);

        this.setClass(".color",options);

        options.caption = "span.color".slick();
        options.body = [
            "div.cursor",
            "div.zone" //img = "img".slick({"src":options.colorImage||self.options.colorImage})
        ].slick();

        cursor = self.cursor = options.body[0];

        options.showNow =false;
        self.parent(options);

/*        body = self.get(".body").makeResizable({
            handle: "div.resize".slick().inject(self.element),
            limit: self.options.resize,
            onDrag: function(){ self.resize(this.value.now.x); }
        });
*/

        new Drag(cursor,{
            handle:cursor.getNext(),
            style:false,
            snap:0,
            //also update the wheel on mouse-down
            onStart:setHSV,
            onDrag:setHSV
        });

        self.setValue();
        if(showNow){ self.show(); }
    },

    setValue:function( color ){

        color = (color||this.options.color).hexToRgb(true) || [255,255,255];
        this.hsv = color.rgb2hsv();
        //console.log("Dialog.Color setValue() ",color+" "+this.hsv, this.hsv.hsv2rgb() );
        return this.moveCursor();

    },

    /*
    Function: resize
        Resize the dialog body and fire the "resize" event.
        Only use one dimension to ensure a square color box

    Arguments:
        width,height - new dimension of the body in px
    */
    /*
    resize: function( width ){

        this.get(".body").setStyles({height:width, width:width});
        this.moveCursor( );
        this.fireEvent("resize");

    },
    */

    /*
    Function: setHSV
        Recalculate the HSV-color based on x/y mouse coordinates.
        After recalculation, the color-wheel cursor is repositioned
        and the "onChange" event is fired.

    Arguments:
        cursor - DOM element
        e - mouse drag event"s
    */
    setHSV: function( cursor, e ){

        var self = this,
            body = self.get(".body").getCoordinates(),
            v = [e.page.x - body.left, e.page.y - body.top],
            W = body.width,
            W2 = W/2,
            W3 = W2/2,
            x = v[0]-W2,
            y = W-v[1]-W2,
            sat = Math.sqrt(Math.pow(x,2)+Math.pow(y,2)),
            hue = Math.atan2(x,y)/(Math.PI*2);

        self.hsv = [
                hue > 0 ? (hue*360) : ((hue*360)+360),
                sat < W3 ? (sat/W3)*100 : 100,
                sat >= W3 ? Math.max(0,1-((sat-W3)/(W3)))*100 : 100
            ];

        self.moveCursor( );
        self.fireEvent("drag", self.getHex() );

    },

    getHex: function(){
        return this.hsv.hsv2rgb().rgbToHex();
    },

    action: function( /*value*/ ){
        //only one button to press: Done/Ok, ...
        this.parent( this.getHex() );
    },

    show: function(){
        return this.parent().moveCursor( );
    },

    /*
    Function: moveCursor
        Reposition the cursor based on the width argument
        Note: only works when the dialog is visible.

    */
    moveCursor: function( ){

        var self = this,
            hsv = self.hsv,
            hex = self.getHex(),
            w2 = self.get(".body").getSize().x/2,
            radius = (hsv[0] / 360) * (Math.PI*2),
            hypothenuse = (hsv[1] + (100-hsv[2])) / 100*(w2/2);

        self.get(".color").set({
            html: hex,
            styles: {
                color: new Color(hex).invert().hex,
                background: hex
            }
        });

        self.cursor.setStyles({
            left:  Math.abs( Math.sin(radius) * hypothenuse + w2 ),
            top:  Math.abs( Math.cos(radius) * hypothenuse - w2 )
        });

        return self;

    }

});
