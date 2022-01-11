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
Plugin: Viewer.Slimbox

    Slimbox clone, refactored for JSPWiki.
    Added support for iframes, flash video.
    Todo: html5 video.

Credits:
    Inspired by Slimbox by Christophe Bleys, (see http://www.digitalia.be/software/slimbox)
    the mediaboxAdvanced by John Einselen (see http://iaian7.com/webcode/mediaboxAdvanced)
    and the diabox by Mike Nelson. (see https://github.com/mnelson/diabox)

DOM structure:
    DOM structure of the JSPWiki Slimbox viewer.
    (start code)
    div#slmbx
        div.modal               //semi transparent screen overlay
        div.viewport(.loading)  //img, object or iframe element is inserted here
            a.controls.caption
            a.controls.next
            a.controls.prev
            a.controls.close
    (end)
*/
Viewer.Slimbox = new Class({

    Implements: Options,
    Binds: ["attach", "key", "update", "resize"],

    options: {
        loop: true,  // (boolean) affects next/prev at last/first element
        width: 800,  // (int px) default width of the box
        height: 600, // (int px) default height of the box
        hints: {     // default controls
            btn: "Click to view {0}",
            close: "&times;",
            next: "&gt;",  //&#171;  Next
            prev: "&lt;",  //&#187;  Previous
            nofm: "[{0}/{1}]&nbsp;",     //[page/#pages]
            size: "Size: {0}px x {1}px"  // (height x width)
        },
        keys: {
            close: ["esc", "x", "c"],
            prev: ["left", "up", "p"],
            next: ["enter", "space", "right", "down", "n"]
        }
    },

    initialize: function(options){

        var self = this.setOptions(options),
            hints = self.options.hints,
            controls = "a.controls.";

        //helper function
        function clickFn(){
            if( this.matches(".next")){ self.update(1); }
            else if( this.matches(".prev")){ self.update(-1); }
            else { self.attach( /*O=close*/ ); }
        }

        $(document.body).grab([
            "div.slmbx", { attach: self }, [
                "div.slmodal", { events: { click: clickFn } },
                "div.viewport", { attach: [self, "viewport"], events: { "click:relay(a)": clickFn } }, [
                    //insert viewable iframe/object/img ...
                    controls + "caption.external",
                    controls + "next", { html: hints.next },
                    controls + "prev", { html: hints.prev },
                    controls + "close", { html: hints.close }
                ]
            ]
        ].slick());

    },

    /*
    Function: get
        Retrieve DOM elements inside the slimbox container, based on a css selector.
    Example:
    >    this.get("a.next");
    */
    get: function( selector ){

        return this.element.getElement(selector);

    },

    /*
    Function: match
        Check if the URL is recognized as a viewable object/image/html...
    */
    match: function(url){

        return Viewer.match(url, this.options);

    },

    /*
    Function: watch
        Install click handlers on a group of images/objects/media links,
        and optionally add slimbox click-buttons.

    Arguments:
        elements - set of DOM elements
        btn - (optional) slick selector to insert a slimbox button after each viewable link
    Returns
        set of DOM elements viewable via Slimbox
    Example
    >   TheSlimbox.watch( $$(".slimbox a","a.slimbox-btn");

    */
    watch: function(elements, btn){

        var self = this, caption;

        elements = $$(elements).filter( function(el){
            return self.match( el.src || el.href );
        });

        return elements.each( function(el, idx){

            caption = el.textContent || el.title || el.alt;

            if( btn ){
                el = btn.slick({
                    href: el.src || el.href,
                    title: self.options.hints.btn.xsubs(caption)
                }).inject(el, "after");
            }

            el.addEvent("click", function(ev){ ev.stop(); self.show(elements, idx); });

        });

    },


    /*
    Function: show
        Start the image/media viewer for a set of elements.
    Arguments
        elements - set of DOM elements to be viewed
        cursor - index of first items to be viewed
    */
    show: function( elements, cursor ){

        var self = this;
        self.elements = elements;
        self.cursor = cursor;
        self.attach( 1 /*true*/ );
        self.update( 0 );

    },

    /*
    Function: attach
        Attach or de-tach eventhandlers from the slimbox dialogs.
        Display or hide the modal and viewport. (css class .show)
    */
    attach: function( open ){

        var self = this,
            fn = open ? "addEvent" : "removeEvent";

        ["object", Browser.ie6 ? "select" : "embed"].each(function(tag) {
            $$(tag).each( function(el){
                if( open ){ el._slimbox = el.style.visibility; }
                el.style.visibility = open ? "hidden" : el._slimbox;
            });
        });

        self.element.ifClass(open, "active");
        self.reset();

        document[fn]("keydown", self.key); //checkme: support arrow keys, etc.

    },

    reset: function(){

        this.viewport.getElements(":not(.controls)").destroy();
        this.preload = null;

    },

    /*
    Function: key
        Handle keystrokes.
    */
    key: function( event ){

        var self = this,
            keys = self.options.keys,
            key = event.key;

        //console.log("keydown ", key);
        keys.close.contains(key) ? self.attach(/*O=close*/) :
            keys.next.contains(key) ? self.update(1) :
                keys.prev.contains(key) ? self.update(-1) :
                    /*otherwise*/ key = 0;

        if(key){ event.stop(); }

    },

    /*
    Function: update
        Updates the viewport and the controls with caption, next and previous links.
        Implements cursor loop-around logic.

    Arguments:
        increment - move the cursor by increment, and display the new content
    */
    update: function( increment ){

        var self = this,
            options = self.options,
            hints = options.hints,
            elements = self.elements,

            max = elements.length,
            many = max > 1,

            incr = function(num){
                return options.loop ? (num >= max) ? 0 : (num < 0) ? max - 1 : num : num;
            },
            cursor = incr( self.cursor + increment ).limit( 0, max - 1 ), //new cursor value

            el, url;

        if( increment != 0 && (cursor == self.cursor)){ return; }

        self.cursor = cursor;
        self.get(".prev")[ (many && (incr(cursor - 1) >= 0 )) ? "show" : "hide"]();
        self.get(".next")[ (many && (incr(cursor + 1) < max)) ? "show" : "hide"]();

        el = elements[cursor];
        url = el.href || el.src; //url = encodeURIComponent(url);

        self.get(".caption").set({
            href: url,
            html: ( many ? hints.nofm.xsubs( cursor + 1, max)  : "" ) +
                    (el.title || el.alt || el.textContent || ""  ).escapeHtml()

        });

        self.viewport.addClass("loading");
        //if( self.preload ){ self.preload.destroy(); self.preload = null;}
        self.reset();
        //alert("wait");
        Viewer.preload( url, options, self.resize );

    },

    /*
    Function: resize
        Completes the resizing of the viewport, after loading the img, iframe or object.
    */
    resize: function( preload ){

        var self = this,
            isImage = preload.matches("img"),
            viewport = self.viewport,

            wSize = window.getSize(),
            width = preload.width.toInt().limit( 240, 0.9 * wSize.x ).toInt(),
            height = preload.height.toInt().min( 0.9 * wSize.y ).toInt(),
            caption = self.get(".caption");

        self.preload = preload;

        //caption.set("html", caption.get("html") + self.options.hints.size.xsubs(width, height) );
        caption.set("title", self.options.hints.size.xsubs(width, height) );

        // viewport has css set to { top:50%, left:50% } for automatic centered positioning
        viewport
            .removeClass("loading")
            .setStyles({
                backgroundImage: isImage ? "url(" + preload.src + ")" : "none",
                //rely on css3 transitions... iso mootools morph
                width: width, height: height, marginTop: -height / 2, marginLeft: -width / 2
            });
            //mootools transition, in case css3 would not be available
            //.morph({ width:width, height:height, marginTop:-height/2, marginLeft:-width/2 });

        if( !isImage ){ viewport.grab( preload ); }//now include the viewer in the dom

    }

});
