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
Class: Collapsible

Options:
    options - (object, optional)

    bullet - (string) css selector to create collapsible bullets, default is "b.bullet", //"b.bullet[html=&bull;]"
    open - (string) css class of expanded "bullet" and "target" elements
    close - (string) css class of collapsed "bullet" and "target" elements
    hint - (object) hint titles for the open en closed bullet, will be localized

    nested - (optional) css selector of nested container elements, example "li",
    target - (optional) css selector of the target element which will expand/collapse, eg "ul,ol"
        The target is a descendent of the main element, default target is the main element itself.

    collapsed - (optional) css selector to match element which should be collapsed at initialization (eg "ol")
        The initial state will be overruled by the Cookie.Flags, if any.
    cookie - (optional) Cookie.Flags instance, persistent store of the collapsible state of the targets

    fx - (optional, default = "height") Fx animation parameter - the css style to be animated
    fxy - (optional, default = "y") Fx animation parameter
    fxReset - (optional, default = "auto") Fx animation parameter - end value of Fx animated style.
        At the end of the animation, "fx" is reset to this "fxReset" value. ("auto" or fixed value)

Depends on:
    String.Extend: xsubs()
    Element.Extend: ifClass()

DOM structure:
    (start code)
    div.collapsible
        ul
            li
                b.bullet.xpand|xpand[onclick="..."]
                Toggle-text
                ul.xpand|xpand
                    li .. collapsible content ..
    (end)

Example:
    (start code)
    ...
    (end)
*/
!function(){

var TCollapsible = this.Collapsible = new Class({

    Implements: Options,

    options: {
        bullet: "b.bullet", //clickable bullet
        hint: { open:"collapse", close:"expand" },
        open: "xpand",
        close: "clpse",

        //cookie: null,    //Cookie.Flags - persist the state of the targets
        //target: "ul,ol", //the elements which will expand/collapse
        //nested: "li",    //(optional) css selector of nested container elements
        //collapsed: "ol", //css selector to check if default state is collapsed

        fx: "height",    //style attribute to animate on collapse
        fxy: "y",        //scroll direction to animate on collapse,
        fxReset: "auto"    //end value after animation is complete on expanded element: "auto" or fixed width
    },

    initialize: function(element, options){

        var self = this;

        self.element = element = document.getElement(element);
        //note: setOptions() makes a copy of all objects, so first copy the cookie!
        self.cookie = options && options.cookie;
        options = self.setOptions(options).options;

        if( options.nested ){

            element.getElements( options.nested ).each( self.build, self );

        } else {

            self.build( element );

        }

        element.addEvent(
            //EG: "click:relay(b.bullet.xpand,b.bullet.clpse)"
            "click:relay({0}.{1},{0}.{2})".xsubs(options.bullet,options.open,options.close),
            function(event){ event.stop(); self.toggle(this); }
        );

    },

    build: function( element ){

        var self = this,
            options = self.options,
            bullet = options.bullet,
            target;

        if( !self.skip(element) ){

            bullet = element.getElement(bullet) || bullet.slick().inject(element,"top");
            target = element.getElement(options.target);

            if( target && (target.get("text").trim()!="") ){

                //console.log("FX tween",bullet,target,self.initState(element,target));
                if( options.fx ){
                    target.set("tween",{
                        property: options.fx,
                        onComplete: function(){ self.fxReset( this.element ); }
                    });
                }

                self.update(bullet, target, self.initState(element,target), true);
            }
        }
    },

    //dummy skip function, can be overwritten by descendent classes
    skip: function( /*element*/ ){
        return false;
    },

    //function initState: returns true:expanded; false:collapsed
    //cookies always overwrite the initial state
    initState:function( element, target ){

        var cookie = this.cookie,
            isCollapsed = this.options.collapsed;

        isCollapsed = !(isCollapsed && target.match(isCollapsed) );

        return cookie ? cookie.get(target, isCollapsed) : isCollapsed;
    },

    //function getState: returns true:expanded, false:collapsed
    getState: function( target ){

        return target.hasClass(this.options.open);

    },

    toggle: function(bullet){

        var self = this,
            cookie = self.cookie,
            options = self.options,
            nested = options.nested,
            element = nested ? bullet.getParent(nested) : self.element,
            target, state;

        if( element ){
            target = element.getElement(options.target);

            if( target ){
                state = !self.getState(target);
                self.update( bullet, target, state );
                if( cookie ){ cookie.write(target, state); }
            }
        }
    },

    update: function( bullet, target, expand, force ){

        var options = this.options, open=options.open, close=options.close;

        if( bullet ){

            bullet.ifClass(expand, open, close)
                  .set( "title", options.hint[expand ? "open" : "close"].localize() );

        }
        if( target ){

            this.animate( target.ifClass(expand, open, close), expand, force );

        }

    },

    animate: function( element, expand, force ){

        var fx = element.get("tween"),
            fxReset = this.options.fxReset,
            max = (fxReset!="auto") ? fxReset : element.getScrollSize()[this.options.fxy];

        if( this.options.fx ){

            if( force ){
                fx.set( expand ? fxReset : 0);
            } else {
                fx.start( expand ? max : [max,0] );
            }

        }

    },

    fxReset: function(element){

        var options = this.options;

        if( options.fx && this.getState(element) ){

            element.setStyle(options.fx, options.fxReset);

        }

    }

});


/*
Class: Collapsible.List
    Converts ul/ol lists into collapsible trees.
    Converts every nested ul/ol into a collasible item.
    By default, OL elements are collapsed.

DOM Structure:
    (start code)
    div.collapsible
        ul
            li
                b.bullet.xpand|xpand[onclick="..."]
                Toggle-text
                ul.xpand|xpand
                    li ... collapsible content ...
    (end)
*/
TCollapsible/*this.Collapsible*/.List = new Class({

    Extends:TCollapsible,

    initialize: function(element,options){

        this.parent( element, Object.merge({
            target:   "> ul, > ol",
            nested:   "li",
            collapsed:"ol"
        },options));

    },

    // SKIP empty LI elements  (so, do not insert collapse-bullets)
    // LI element is not-empty when is has
    // - a child-node different from ul/ol
    // - a non-empty #text-nodes
    // Otherwise, it is considered
    skip: function(element){

        var n = element.firstChild,isTextNode, re=/ul|ol/i;

        while( n ){

            isTextNode = (n.nodeType==3);

            if( ( !isTextNode && ( !re.test(n.tagName) ) )
             || (  isTextNode && ( n.nodeValue.trim()!="") ) ){

                     return false;
            }
            n=n.nextSibling;
        }

        return true; //skip this element

    }

});

/*
Class: Collapsible.Box
    Makes a collapsible box.
    - the first element becomes the visible title, which gets a bullet inserted
    - all other child elements are wrapped into a collapsible element

Options:


DOM Structure:
    (start code)
    div.collapsebox.panel.panel-default
        div.panel-heading
            b.bullet.xpand|clpse[onclick="..."]
            h4.panel-title title
        div.panel-body.xpand|clpse
            .. collapsible content ..
    (end)

*/
TCollapsible/*this.Collapsible*/.Box = new Class({

    Extends:TCollapsible,

    initialize:function(element,options){

        //FFS: how to protect against empty boxes..
        //if( element.getChildren().length >= 2 ){      //we don"t do empty boxes

            options.collapsed = options.collapsed ? "div":""; // T/F converted to matching css selector
            options.target = options.target || "!^"; //or  "> :last-child" or "> .panel-body"

            this.parent( element, options );
        //}
    },

    build: function( element ){

        var options = this.options, heading, body, next
            panelCSS = "panel".fetchContext(element);

        //we don"t do double invocations
        if( !element.getElement( options.bullet ) ){

            //build bootstrap panel layout
            element.className += " "+panelCSS;

            heading = ["div.panel-heading",[options.bullet]].slick().wraps(
                element.getFirst().addClass("panel-title")
            );

            body = "div.panel-body".slick();
                while(next = heading.nextSibling) body.appendChild( next );

            //if( body.get("text").trim()!="" ) this-is-and-empty-box !!

            this.parent( element.grab( "div".slick().grab(body) ) );

        }
    }

});

}();
