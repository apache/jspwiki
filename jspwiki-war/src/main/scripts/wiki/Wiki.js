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


/*eslint-env browser*/
/*global $, $$, Form, Hash, Behavior, HighlightQuery, Accesskey */
/*exported  Wiki */

/*
Script: wiki.js
    Javascript routines to support JSPWiki, a JSP-based WikiWiki clone.

Dependencies:
    Based on http://mootools.net/
    * mootools-core.js : v1.5.1 excluding the Compatibility module
    * mootools-more.js : v1.5.1 including...
        Class.Binds, Hash, Element.Shortcuts, Fx.Elements, Fx.Accordion, Drag,
        Drag.Move, Hash.Cookie, Swiff

Depends on :
    moo-extend/Behaviour.js
    moo-extend/Element.Extend.js
    moo-extend/Array.Extend.js
    moo-extend/String.js
    moo-extend/HighlightQuery.js
    moo-extend/Acceskey.js
    moo-extend/Form.File.js
    moo-extend/Form.MultipleFile.js
    moo-extend/Request.js
    wiki/Navigate.js
    wiki/Recents.js
    wiki/FindPages.js
    wiki/Search.js
    wiki/Prefs.js
*/

//"use strict";

/*
Class: Wiki
    Javascript support functions for jspwiki.  (singleton)
*/
var Wiki = {

    version: "haddock03",  //used to validate compatible preference cookies

    initialize: function(){

        var wiki = this,
            behavior = new Behavior();

        wiki.add = behavior.add.bind(behavior);
        wiki.once = behavior.once.bind(behavior);
        wiki.update = behavior.update.bind(behavior);


        // add core jspwiki behaviors; needed to support the default template jsp's
        wiki.add( "body", wiki.caniuse )

            .add( "[accesskey]", Accesskey )

            //toggle effect:  toggle .active class on this element when clicking toggle element
            //.add("[data-toggle]", "onToggle", {attr:"data-toggle"})
            .add( "[data-toggle]", function(element){

                element.onToggle( element.get("data-toggle"), function(isActive){
                    var pref = element.get("data-toggle-pref");
                    if( pref ){
                        //console.log(pref, isActive);
                        wiki.prefs.set(pref, isActive ? "active" : "");
                    }
                });
            })

            //generate modal confirmation boxes, eg prompting to execute
            //an unrecoverable action such as deleting a page or attachment
            //.add("[data-modal]", "onModal", {attr:"data-modal"})
            .add( "[data-modal]", function(element){
                element.onModal( element.get("data-modal") );
            })

            //hover effects: show/hide this element when hovering over the parent element
            //.add("[data-toggle]", "onHover", {attr:"data-hover-parent"})
            .add( "[data-hover-parent]", function(element){
                element.onHover( element.get("data-hover-parent") );
            })

            //resize the "data-resize" elements when dragging this element
            //.add( "[data-resize]", wiki.resizer.bind(wiki) )
            .add( "[data-resize]", function(element){
                wiki.resizer(element, $$(element.get("data-resize")) );
            })

            //add header scroll-up/down effect
            .add( ".fixed-header > .header", wiki.yoyo)

            //highlight previous search query retreived from a cookie or referrer page
            .add( ".page-content", function(element){

                var previousQuery = "PrevQuery";

                HighlightQuery( element, wiki.prefs.get(previousQuery) );
                wiki.prefs.erase(previousQuery);

            })

            //activate quick navigation searchbox
            .add( ".searchbox .dropdown-menu", function(element){

                var recentSearch = "RecentSearch", prefs = wiki.prefs;

                //activate Recent Searches functionality
                new wiki.Recents( element, {
                    items: prefs.get(recentSearch),
                    onChange: function( items ){
                        items ? prefs.set(recentSearch, items) : prefs.erase(recentSearch);
                    }
                });

                //activate Quick Navigation functionality
                new wiki.Findpages(element, {
                    rpc: function(value, callback){
                        wiki.jsonrpc("/search/pages", [value, 16], callback);
                    },
                    toUrl: wiki.toUrl.bind(wiki),
                    allowClone: function(){
                        return /view|preview|info|attach/.test( wiki.Context );
                    }
                });
            })

            //activate ajax search routines on Search.jsp
            .add( "#searchform2", function(form){

                wiki.search = new wiki.Search( form, {
                    xhrURL: wiki.XHRSearch,
                    onComplete: function(){
                        //console.log(form.query.get("value"));
                        wiki.prefs.set("PrevQuery", form.query.get("value"));
                    }
                });
            })

            //activate attachment upload routines
            .add( "#files", Form.File, {
                max: 8,
                rpc: function(progressid, callback){
                    //console.log("progress", progressid);
                    wiki.jsonrpc("/progressTracker", [progressid], callback);
                }
            });

        window.addEvents({
            popstate: wiki.popstate,
            domready: wiki.domready.bind(wiki)
        });



    },


    caniuse: function( body ){

        //support for flexbox is broken in IE, do it the hard-way - ugh.

        var isIE11 = !(window.ActiveXObject) && "ActiveXObject" in window;
        var isIE9or10 = "ActiveXObject" in window;

        body.ifClass( !( isIE11 || isIE9or10 ) , "can-flex");

    },

    /*
    Function: domready
        After the DOM is fully loaded:
        - initialize the meta data wiki properties
        - initialize the section Links
        - when the "referrer" url (previous page) contains a "section=" parameter,
          scroll the wiki page to the right section
    */
    domready: function(){

        var wiki = this;

        wiki.dropdowns();

        wiki.meta();

        wiki.prefs = new Hash.Cookie("JSPWikiUserPrefs", {
            path: wiki.BasePath,
            duration: 20
        });

        //Object.each(wiki.prefs.hash, function(item,key){ console.log("PREFS  ",key,"=>",item); });

        if( wiki.version != wiki.prefs.get("version") ){
            wiki.prefs.empty();
            wiki.prefs.set("version", wiki.version);
        }


        //wiki.url = null;  //CHECK:  why this is needed?
        //console.log( wiki.prefs.get("SectionEditing") , wiki.EditPermission ,wiki.Context );
        if( wiki.prefs.get("SectionEditing") && wiki.EditPermission && (wiki.Context != "preview") ){

            wiki.addEditLinks( wiki.toUrl( wiki.PageName, true ) );

        }

        //console.log( "section", document.referrer, document.referrer.match( /\&section=(\d+)$/ ) );
        wiki.scrollTo( ( document.referrer.match( /\&section=(\d+)$/ ) || [,-1])[1] );

        // initialize all registered behaviors
        wiki.update();

        //on page-load, also read the #hash and fire popstate events
        wiki.popstate();

        wiki.autofocus();

    },

    /*
    Function: yoyo ( header )
        Add a yoyo effect to the header:  hide it on scroll down, show it again on scroll up.

    Inspired by: https://github.com/WickyNilliams/headroom.js

    DOM Structure:
    (start code)
        div[style='padding-top:nn']    => nn==height of header;  push content down
        div.header.yoyo[.scroll-down]  => css: position=fixed
    (end)

    */
    yoyo: function( header ){

        var height = header.offsetHeight,
            semaphore,
            scrollY,
            lastScrollY = 0;

        //add spacer just infront of fixed element, adjust height == header (fixed elements do not take space in the dom)
        "div".slick({styles: { paddingTop: height } }).inject(header,"before");

        window.addEvent("scroll", function(){ semaphore = true; });

        setInterval( function(){

            if( semaphore ){

                semaphore = false;
                scrollY = window.getScroll().y;

                // Limit scroll top to counteract iOS / OSX bounce.
        		scrollY = scrollY.limit(0, window.getScrollSize().y - window.getSize().y);

                if( Math.abs(lastScrollY - scrollY) > 5 /* minimum difference */ ){

                    header.ifClass( scrollY > lastScrollY && scrollY > height, "scrolling-down" );
                    //console.log(scrollY, lastScrollY, height);


                    lastScrollY = scrollY;
                }
            }

        }, 250);

    },


    /*
    Function: popstate
        When pressing the back-button, the "popstate" event is fired.
        This popstate function will fire a internal 'popstate' event
        on the target DOM element.

        Behaviors (such as Tabs or Accordions) can push the ID of their
        visible content on the window.location hash.
        This ID can be retrieved when the back-button is pressed.

        When clicking a link referring to hidden content (tab, accordion), the
        popstate event is 'bubbled' up the DOM tree.

    */
    popstate: function(){

        var target = $(location.hash.slice(1)),
            events,
            popstate = "popstate";

        //console.log( popstate, location.hash, target );

        //only send popstate events to targets within the main page; eg not sidebar
        if( target && target.getParent(".page-content") ){

            while( !target.hasClass("page-content") ){

                events = target.retrieve("events"); //mootools specific - to read registered events on elements

                if( events && events[popstate] ){

                    target.fireEvent(popstate);

                }

                target = target.getParent();

            }
        }
    },

    autofocus: function(){

        var els, element;

        if( !("autofocus" in document.createElement("input") ) ){
            // editor/plain.jsp  textarea#wikiText
            // login.jsp         input#j_username
            // prefs/prefs       input#assertedName
            // find              input#query2
            els = $$("input[autofocus=autofocus], textarea[autofocus=autofocus]");
            while( els[0] ){
                element = els.shift();
                //console.log("autofocus", element, element.autofocus, element.isVisible(), element.offsetWidth, element.offsetHeight, "$", element.getStyle("display"), "$");
                if( element.isVisible() ){
                    element.focus();
                    return;
                }
            }
        }

    },

    /*
    Function: meta
        Read all the "meta" dom elements, prefixed with "wiki",
        and add them as properties to the wiki object.
        EG  <meta name="wikiContext">  becomes  wiki.Context
        * wikiContext : jspwiki requestcontext variable (view, edit, info, ...)
        * wikiBaseUrl
        * wikiPageUrl: page url template with dummy pagename "%23%24%25"
        * wikiEditUrl : edit page url
        * wikiJsonUrl : JSON-RPC / AJAX url
        * wikiPageName : pagename without blanks
        * wikiUserName
        * wikiTemplateUrl : path of the jsp template
        * wikiApplicationName
        * wikiEditPermission
    */
    meta: function(){

        var url,
            wiki = this,
            host = location.host;

        $$("meta[name^=wiki]").each( function(el){
            wiki[el.get("name").slice(4)] = el.get("content") || "";
        });

        // BasePath: if JSPWiki is installed in the root, then we have to make sure that
        // the cookie-cutter works properly here.
        url = wiki.BaseUrl;
        url = url ? url.slice( url.indexOf(host) + host.length, -1 ) : "";
        wiki.BasePath = ( url /*===""*/ ) ? url : "/";
        console.log(url, host, wiki.BaseUrl + " basepath: " + wiki.BasePath);

    },

    /*
    Function: dropdowns
        Parse special wikipages such ase MoreMenu, HomeMenu
        and format them as bootstrap compatible dropdown menus.
    */
    dropdowns: function(){

        $$( "ul.dropdown-menu > li > ul" ).each( function(ul){

            var li, parentLi = ul.getParent();

            while( (li = ul.getFirst("li")) ){

                if( li.innerHTML.trim() == "----" ){

                    li.addClass("divider");

                } else if( !li.getFirst() || !li.getFirst("a") ){

                    li.addClass("dropdown-header");

                }
                li.inject(parentLi, "before");

            }
            ul.dispose();

        });

        /* (deprecated) "pre-HADDOCK" moremenu style
              Consists of a list of links, with \\ delimitters
              Each <p> becomes a set of <li>, one for each link
              The block is terminated with a divider, if more <p>'s are coming
        */
        $$( "ul.dropdown-menu > li.more-menu > p" ).each( function(element){

            var parentLi = element.getParent();

            element.getElements('a').each( function(link){
                ["li",[link]].slick().inject(parentLi, "before");
            });
            if( element.getNext("p *,hr") ){
                "li.divider".slick().inject(parentLi, "before") ;
            }
            element.dispose();

        });

    },

    /*
    Function: getSections
        Returns the list of all section headers, excluding the header of the Table Of Contents.
    */
    getSections: function(){

        return $$(".page-content [id^=section]:not(#section-TOC)");

    },

    /*
    Function: scrollTo
        Scrolls the page to the section previously being edited - if any
        Section counting starts at 1??
    */
    scrollTo: function( index ){

        //console.log("Scroll to section ", index, ", Number of sections:", this.getSections().length );
        var element = this.getSections()[index];

        if( element ){
            location.replace( "#" + element.get("id") );
        }

    },

    /*
    Property: toUrl
        Convert a wiki pagename to a full wiki-url.
        Use the correct url template: view(default), edit-url or clone-url
    */
    toUrl: function(pagename, isEdit, isClone){

        var urlTemplate = isClone ? this.CloneUrl : isEdit ? this.EditUrl : this.PageUrl;
        return urlTemplate.replace(/%23%24%25/, this.cleanPageName(pagename) );

    },

    /*
    Property: toPageName
        Parse a wiki-url and return the corresponding wiki pagename
    */
    toPageName: function(url){

        var s = this.PageUrl.escapeRegExp().replace(/%23%24%25/, "(.+)");
        return ( url.match( RegExp(s) ) || [0, false] )[1];

    },

    /*
    Property: cleanPageName
        Remove all not-allowed chars from a pagename.
        Trim all whitespace, allow letters, digits and punctuation chars: ()&+, -=._$
        Mirror of org.apache.wiki.parser.MarkupParser.cleanPageName()
    */
    cleanPageName: function( pagename ){

        //\w is short for [A-Z_a-z0-9_]
        return pagename.clean().replace(/[^\w\u00C0-\u1FFF\u2800-\uFFFD\(\)&\+,\-=\.\$ ]/g, "");

    },

    /*
    Function: addEditLinks
        Add to each Section title (h2/h3/h4) a quick edit link.
        FFS: should better move server side
        FFS: add section #hash to automatically go back to the section being edited
    */
    addEditLinks: function( url ){

        var description = "quick.edit".localize();

        url = url + (url.contains("?") ? "&" : "?") + "section=";

        this.getSections().each( function(element, index){

            element.grab("a.editsection".slick({ html: description, href: url + index }));

        });

    },

    /*
    Function: configPrefs  (sofar only used in edit mode)
        Initialize the configuration checkboxes from the wiki prefs cookie.
        Save any change to the checkboxes back into the wiki prefs cookie.
        Also take care of switching between different editor types, saving the
        new editor type into the wiki prefs cookie.

        EG: tabcompletion, smartpairs, autosuggest, livepreview, previewcolumn. editor-type
    */
    configPrefs: function( form, onChangeFn ){

        var wiki = this;

        function onCheck(){

            var cmd = this.getAttribute("data-cmd"),
                isChecked = this.checked;

            wiki.toggleLivePreview(form, cmd, isChecked);
            wiki.prefs.set(cmd, isChecked);  //persist in the pref cookie
            if( onChangeFn ){ onChangeFn(cmd, isChecked); }

        }

        //Handle all configuration checkboxes
        form.getElements("[type=checkbox][data-cmd]").each( function( el ){

            el.checked = !!wiki.prefs.get(el.getAttribute("data-cmd"));
            el.addEvent("click", onCheck );
            onCheck.apply(el);

        });

        //Persist the selected editor type in the pref cookie
        form.getElements("a.editor-type").addEvent("click", function(){

            wiki.prefs.set("editor", this.get("text"));

        });

    },


    toggleLivePreview: function( container, cmd, state ){

        if( cmd.test( /livepreview|previewcolumn/ ) ){

            var previewcontainer = container.getElement(".edit-area").ifClass(state, cmd),
                ajaxpreview = container.getElement(".ajaxpreview");

            if( cmd == "livepreview" ){

                //disable the previewcolumn toolbar cmd checkbox
                container.getElement("[data-cmd=previewcolumn]").disabled = !state;

            } else {

                /* Toggle the position of the preview-area in the dom

                1. HORIZONTAL SIDE BY SIDE VIEW
                div.snip
                    div.toolbar
                    div.edit-area.livepreview.previewcolumn
                        div.col-50
                        div.col-50.ajaxpreview
                    div.resizer

                2. VERTICAL VIEW
                div.snip
                    div.toolbar
                    div.edit-area.livepreview
                        div.col-50
                    div.resizer
                    div.col-50.ajaxpreview
                */

                if( !state ){ previewcontainer = previewcontainer.getParent(); }
                previewcontainer.grab(ajaxpreview);

            }
        }
    },

    getXHRPreview: function( getContent, previewElement ){

        var wiki = this,
            loading = "loading",
            preview = function(p){ previewElement.removeClass(loading).set("text", p);};

        return (function(){

            previewElement.addClass(loading);

            new Request({
                url: wiki.XHRHtml2Markup,
                data: {
                    htmlPageText: getContent()
                },
                onSuccess: function(responseText){
                    preview( responseText.trim() );
                },
                onFailure: function(e){
                    preview( "Sorry, HTML to Wiki Markup conversion failed :=() " + e );
                }
            }).send();

        }).debounce();

    },

    /*
    Behavior: resizer
        Resize the target element, by dragging a .resizer handle.
        Multiple elements can be resized via the callback.
        The .resizer element can specify a prefs cookie to retrieve/store the height.
        Used by the plain and wysiwyg editor.

    Arguments:
        target - DOM element to be resized
        callback - function, to allow resizing of more elements

    Globals:
        wiki - main wiki object, to get/set preference fields
        textarea - resizable textarea (DOM element)
        preview - preview (DOM element)
    */
    /*
    wiki.add(".resizer",function(element){...}


    [data-resize] : resize target,  can be multiple elements

    div.resizer[data-resize=".pagecontent"] => for add-comment sections
    div.resizer[data-resize=".ajaxpreview,.snipeable"][data-pref=editorHeight]
    */
    resizer: function( handle, targets, dragCallback ){

        var pref = handle.get("data-pref"),
            prefs = this.prefs,
            target;

        function showDragState(add){ handle.ifClass(add, "dragging"); }

        if( !targets[0] ){ return; }

        //set the initial size of the targets
        if( pref ){
            targets.setStyle("height", prefs.get(pref) || 300 );
        }

        target = targets.pop();

        target.makeResizable({
            handle: handle,
            modifiers: { x: null },
            onDrag: function(){
                var h = this.value.now.y;
                if( pref ){ prefs.set(pref, h); }
                if( targets ){ targets.setStyle("height", h); }
                if( dragCallback ){ dragCallback(h); }
            },
            onBeforeStart: showDragState.pass(true),
            onComplete: showDragState.pass(false),
            onCancel: showDragState.pass(false)
        });

    },


    pageDialog: function( caption, method ){

        var wiki = this;

        return [ Dialog.Selection, {

            caption: caption,

            onOpen: function( dialog ){

                var key = dialog.getValue();

                //if empty link, than fetch list of attachments of the open page
                if( !key || (key.trim()=='') ){

                    key = wiki.PageName + "/";

                }

                wiki.jsonrpc( method, [key, 30], function( result ){

                    //console.log("jsonrpc result", result, !!result[0] );
                    if( result[0] /* length > 0 */ ){

                        dialog.setBody( result );

                    } else {

                        dialog.hide();

                    }
                });
            }
        }];

    },

    /*
    Function: jsonrpc
        Generic json-rpc routines to talk to the backend jspwiki-engine.
    Note:
        Uses the JsonUrl which is read from the meta element "WikiJsonUrl"
        {{{ <meta name="wikiJsonUrl" content="/JSPWiki-pipo/JSON-RPC" /> }}}

    Supported rpc calls:
        - {{search.findPages}} gets the list of pagenames with partial match
        - {{progressTracker.getProgress}} get a progress indicator of attachment upload
        - {{search.getSuggestions}} gets the list of pagenames with partial match

    Example:
        (start code)
        //Wiki.ajaxJsonCall('/search/pages,[Janne,20]', function(result){
        Wiki.jsonrpc("search.findPages", ["Janne", 20], function(result){
            //do something with the resulting json object
        });
        (end)
    */
    jsonrpc: function(method, params, callback){

        if( this.JsonUrl ){

            //console.log(method, JSON.stringify(params) );

            //NOTE:  this is half a JSON rpc ... only responseText is JSON formatted
            new Request({
                url: this.JsonUrl + method,
                //method:"post"     //defaults to "POST"
                //urlEncoded: true, //content-type header = www-form-urlencoded + encoding
                //encoding: "utf-8",
                //encoding: "ISO-8859-1",
        		headers: {
		        	//'X-Requested-With': 'XMLHttpRequest',
			        //'Accept': 'text/javascript, text/html, application/xml, text/xml, */*'
        			'Accept': 'application/json',
		        	'X-Request': 'JSON'
		        },
                onSuccess: function( responseText ){

                    //console.log(responseText, JSON.parse( responseText ), responseText.charCodeAt(8),responseText.codePointAt(8), (encodeURIComponent(responseText)), encodeURIComponent("ä"), encodeURIComponent("Ã")  );
                    callback( JSON.parse( responseText ) );
                    //callback( responseText );

                },
                onError: function(error){
                    //console.log(error);
                    callback( null );
                    throw new Error("Wiki rpc error: " + error);
                }

            }).send( "params=" + params );

        }

    }

};

Wiki.initialize();
