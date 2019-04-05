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
Plugin: Viewer.Carousel
    Viewer plugin for automatic cycling through elements.
    Inspired on bootstrap"s implementation, relying on css3 transitions;
    rewritten for mootools.

Credit:
    bootstrap-carousel.js v2.2.2 http://twitter.github.com/bootstrap/javascript.html#carousel

Depends on:
    Viewer

DOM structure:

    (start code)
    div.carousel
        a.xxx
        a.xxx
    (end)

    becomes
    (start code)
    div.carousel[.slide]

      ol.carousel-indicators
        li.active[data-target="#myCarousel"][data-slide-to="0"]
        li[data-target="#myCarousel"][data-slide-to="1"]
        li[data-target="#myCarousel"][data-slide-to="2"]

      <!-- Carousel items -->
      div.carousel-inner
        div.item.active
          img | object | iframe
        div.item
          img | object | iframe
        ...

      <!-- Carousel nav -->
      a.controls.prev[href="#myCarousel"][data-slide="prev"] &lt;
      a.controls.next[href="#myCarousel"][data-slide="next"] &gt;
    (end)

Example:
>    new Carousel(container, [el1, el2], {container:..., ...} );

*/
Viewer.Carousel = new Class({

    Binds: ["build", "cycle", "stop", "next", "prev", "slid"],
    Implements: [Events, Options],

    options: {
        cycle: 1e4, //=> when set, the carousel automatically cycles through all items
        width: 400, // Default width of the carousel (in pixels)
        height: 300 // Default height of the carousel (in pixels)
    },

    initialize: function(elements, options){

        var self = this,
            t = "transitionend";

        options = self.setOptions(options).options;
        //console.log("CAROUSEL Options: ", options);

        self.css3 = Element.Events[t] ? t : null;
        self.element = options.container;

        Viewer.preloads( $$(elements), options, self.build );

    },

    build: function(elements, width, height){

        var self = this,
            items = [], indicators = [],
            cycle = self.options.cycle,
            NOP = function(){};



        $$(elements).each( function(el){

            items.push("div.item", [
                el, { styles: {
                    //add padding to center the item inside its container, but still fill 100% of the available space
                    padding: Number(height - el.height).limit(0, height) / 2 + "px " +
                             Number(width - el.width).limit(0, width) / 2 + "px"
                }},
                "div.carousel-caption", {
                    html: (el.title || el.alt || el.textContent)
                }
            ]);
            indicators.push("li");
        });
        items[0] += ".active";
        indicators[0] += ".active";

        self.element.empty()
            .set({
                "class": "carousel",
                //maxHeight,maxHeight will auto-scale the images if they are too big to fit
                styles: { maxWidth: width, maxHeight: height },
                events: {
                    "click:relay(li)": function(){ self.to(this.getAllPrevious().length); },
                    mouseenter: cycle ? self.stop : NOP,
                    mouseleave: cycle ? self.cycle : NOP
                }
            })
            .adopt([
                "div.carousel-progress", /*ffs {"transitionDuration":self.cycle/1e3+"s"},*/
                "ol.carousel-indicators",
                    indicators,
                "div.carousel-inner",
                    items,
                "a.controls.prev[html=&lt;]", {events: { click: self.prev }},
                "a.controls.next[html=&gt;]", {events: { click: self.next }}
                ].slick()
            );

        //self.cycle();  only start cycling after a first mouseenter , next()


    },

    get: function( selector ){
        return this.element.getElements(selector);
    },

    /*
    Function: cycle
        Cycle through the carousel items.
        - invoked on initialization
        - invoked on mouseleave
        - invokde at end of the sliding operations
    */
    cycle: function(/*event*/){

        var self = this,
            cycle = self.options.cycle;

        if( cycle && !self.sliding ){
            self.stop(); // make sure to first clear the tid
            self.tid = self.next.delay( cycle );
            self.element.addClass("sliding");
        }

    },

    /*
    Function: stop
        Stop the autocycle mechanism.
        - invoked on mouseenter
        - invoked at start of the sliding operation
    */
    stop: function( /*event*/ ){

        //console.log("stop ", this.tid, this.sliding, arguments);
        clearTimeout( this.tid );
        this.tid = null;
        this.element.removeClass("sliding");

    },

    /*
    Function: to
        Slide directly to a specific carousel item.
        Not yet used.
    */
    to: function( pos ){

        var self = this,
            items = self.get(".item"),
            item = items[pos],
            active = items.indexOf( items.filter(".active")[0] );

        if ( !item ){ return; }

        if( self.sliding ){

            //console.log("concurrency betweeen slide() and pos() - wf "slid" event to occur");
            self.element.addEvent("slid", self.to.pass(pos) );

        } else if ( !item.match(".active") ){

            self.slide( pos > active ? "next" : "prev", item );

        }

    },

    next: function(){
        if( !this.sliding ){ this.slide("next"); }
    },

    prev: function () {
        if( !this.sliding ){ this.slide("prev"); }
    },


    /*
    Function:slide
        Move the carousel to the next item.
        It fully relies on css3 transition.
        If not supported, no animation-effects are applied (lazy me)

    stable =>
    >    .active  => left:0;
    >    .next    => left:100%;
    >    .prev    => left:-100%;

    slide-type = next => Slide to left:
    >    item.active.left   => left:-100%;
    >    item.next.left     => left:0;

    slide-type = prev => Slide to right:
    >    item.active.right  => left:100%;
    >    item.prev.right    => left:0;

    Arguments
        - type : "next","prev"
        - next : element to be shown at end of the "slide" ( array )

    */
    slide: function(type, next){

        var self = this,
            active = self.get(".item.active")[0],
            gonext = (type == "next"),
            css3 = self.css3,
            slid = self.slid;

        self.sliding = true;
        self.stop();

        next = next ||
            active[ gonext ? "getNext" : "getPrevious" ]() ||
            active.getParent()[ gonext ? "getFirst" : "getLast" ]();

        if( next.match(".active") ){ return; }

        next.addClass( type ); //.next or .prev

        self.fireEvent("slide");

        if( css3 ){

            //console.log("transition: "+css3)
            next.offsetWidth; // force reflow -- is this really needed ?
            $$(active, next).addClass( gonext ? "left" : "right" );
            self.element.addEvent( css3, slid );

        } else {

            slid();

        }

    },

    slid: function(){

        var self = this,
            items = self.get(".item"),
            newActive = self.get(".item.next,.item.prev")[0];

        if( newActive ){

            items.set("class", "item");  //wipe out .active, .next, .prev, .left, .right
            newActive.addClass("active");

            self.get("li").set("class", "")[items.indexOf(newActive)].addClass("active");

            self.sliding = false;
            self.cycle();
            self.fireEvent("slid", 0/*dummy*/, 1/*delay 1ms*/);

        }

    }

});


/*
Extension : css3 native events
    Extend mootools to support css3 native events
    (needed by Viewer.Carousel)

Credits:
    Inspired by  https://github.com/amadeus/CSSEvents/

Example:
>    $(element).addEvent("transitionend",function(event){ ...})

*/
!(function(css3){

var B = Browser,
    NativeEvents = Element.NativeEvents,
    pfx = B.cssprefix = (B.safari || B.chrome || B.platform.ios) ? "webkit" : (B.opera) ? "o" : (B.ie) ? "ms" : "";

    for ( style in css3 ){

        var eventType = css3[style],
            type = eventType.toLowerCase(),
            aType = pfx ? pfx + eventType : type,
            aTest = pfx ? pfx + style.capitalize() : style;

        if( document.createElement("div").style[ aTest ] != null ){

            NativeEvents[type] = NativeEvents[aType] = 2;
            Element.Events[type] = { base: aType };

        }

        //console.log(Element.NativeEvents, Element.Events);

    }

})({transition: "TransitionEnd"});
//})({transition:"TransitionStart",transition:"TransitionEnd",animation:"AnimationStart",animation:"AnimationIteration",animation:"AnimationEnd");
