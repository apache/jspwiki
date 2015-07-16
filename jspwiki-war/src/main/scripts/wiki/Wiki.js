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


/*jshint forin:false, noarg:true, noempty:true, undef:true, unused:true, plusplus:false, immed:false, browser:true, mootools:true */
/*global HighlightQuery, Behavior */
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

    initialize: function(){

        var wiki = this,
            behavior = new Behavior();

        wiki.add = behavior.add.bind(behavior);
        wiki.once = behavior.once.bind(behavior);
        wiki.update = behavior.update.bind(behavior);


        // add core jspwiki behaviors; needed to support the default template jsp's
        wiki.add( "[accesskey]", Accesskey )

            //.add("input[placeholder]", function(element){ element.placeholderX(); })

            //toggle effect:  toggle .active class on this element when clicking toggle element
            //.add("[data-toggle]", "onToggle", {attr:"data-toggle"})
            .add( "[data-toggle]", function(element){
                element.onToggle( element.get("data-toggle") );
            })

            //generate modal confirmation boxes, eg prompting to execute
            //unrecoverable actions such as deleting a page or attachment
            //.add("[data-toggle]", "onModal", {attr:"data-modal"})
            .add( "[data-modal]", function(element){
                element.onModal( element.get("data-modal") );
            })

            //hover effects: show/hide this element when hovering over its parent element
            //.add("[data-toggle]", "onHover", {attr:"data-hover-parent"})
            .add( "[data-hover-parent]", function(element){
                element.onHover( element.get("data-hover-parent") );
            })

            //make navigation bar sticky (simulate position:sticky; )
            //.add("[data-toggle]", "onSticky" )
            .add( ".sticky", function( element ){
                element.onSticky();
            })

            //highlight previous search query in cookie or referrer page search query
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
                    toUrl: wiki.toUrl.bind( wiki ),
                    allowClone: function(){
                        return /view|preview|info|attach/.test( wiki.Context );
                    }
                });
            })

            //activate ajax search routines on Search.jsp
            .add( "#searchform2", function( form ){

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

                //TODO: jspwiki v.2.10.x seems now to only support 1 upload-file at a time
                max: 1,
                rpc: function(progressid, callback){
                    wiki.jsonrpc("/progressTracker", [progressid], callback);
                }
            });


        window.addEvents({
            popstate: wiki.popstate,
            domready: wiki.domready.bind(wiki)
        });

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

        //wiki.url = null;  //CHECK:  why this is needed?
        if( wiki.prefs.get("SectionEditing") && wiki.EditPermission && (wiki.Context != "preview") ){

            wiki.addEditLinks( wiki.toUrl( wiki.PageName, true ) );

        }

        //console.log( "section", document.referrer, document.referrer.match( /\&section=(\d+)$/ ) );
        //FFS : refactor -- why not use the standard #section-ID funtionalities
        wiki.srcollTo( ( document.referrer.match( /\&section=(\d+)$/ ) || [,-1])[1] );

        // initialize all registered behaviors
        wiki.update();

        //on page-load, also read the #hash and fire popstate events
        wiki.popstate();

        wiki.autofocus();

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
        if( target && target.getParent(".page") ){

            while( !target.hasClass("page") ){

                events = target.retrieve("events"); //mootools specific

                if( events && events[ popstate ] ){

                    target.fireEvent( popstate );

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
        //console.log("basepath: " + wiki.BasePath);

    },

    /*
    Function: dropdowns
        Adapt dropdown extensions suchs as moremenu,  logomenu, ...
        to fit to the bootstrap .dropdown-menu class structure
    */
    //fixthosebootstrapdropdownmenus
    //ftbddm
    dropdowns: function(){

        $$( "ul.dropdown-menu > li > ul" ).each( function(ul){

            var li, parentLi = ul.getParent();

            while( li = ul.getFirst("li") ){

                if( li.innerHTML.trim() == "----" ){

                    li.addClass( "divider" );

                } else if( !li.getFirst() || !li.getFirst("a") ){

                    li.addClass( "dropdown-header" );

                }
                li.inject(parentLi, "before");

            }
            ul.dispose();

        });

        /* (deprecated) "pre-HADDOCK" moremenu style
              Consists of a list of links, with \\ delimitters
              Each <p> becomes a set of li, one for each link
              The block is terminated with a divider, if more <p's> are coming
        */
        $$( "ul.dropdown-menu > li.more-menu > p" ).each( function(element){

            var parentLi = element.getParent();

            element.getElements('a').each( function(link){
                ['li',[link]].slick().inject(parentLi, "before");
            });
            if( element.getNext('p *,hr') ){
                'li.divider'.slick().inject(parentLi, "before") ;
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
    Function: srcollTo
        Scrolls the page to the section previously being edited - if any
        Section counting starts at 1??
    */
    srcollTo: function( index ){

        //console.log("Scroll to section ", index, ", Number of sections:", this.getSections().length );
        var element = this.getSections()[index], pos;

        if( element ){
            pos = element.getPosition();
            //console.log("SCROLL TO ", element,pos );
            window.scrollTo(pos.x, pos.y);
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
    //jsonid: 1e4, //seed -- not used anymore
    jsonrpc: function(method, params, callback){

        if( this.JsonUrl ){

            console.log(method, JSON.stringify(params) );

    		//NOTE:  this is half a JSON rpc ... responseText is JSON formatted
            new Request({
    			url: this.JsonUrl + method,
	    		//method:"post"     //defaults to "POST"
                //urlEncoded: true, //content-type header = www-form-urlencoded + encoding
                //encoding: utf-8,
                onSuccess: function( responseText ){

                    console.log(responseText, JSON.decode( responseText ) );
                    callback( JSON.parse( responseText ) )

                },
                onError: function(error){
                    //console.log(error);
                    throw new Error("Wiki rpc error: " + error);
                    callback( null );
                }

    		}).send( "params=" + params );

            /* obsolete
            new Request.JSON({
                //url: this.JsonUrl,
                url: this.JsonUrl + method,
                data: JSON.encode({     //FFS ECMASCript5; JSON.stringify() ok >IE8
                    //jsonrpc:'2.0', //CHECK
                    id: this.jsonid++,
                    method: method,
                    params: params
                }),
                method: "post",
                onSuccess: function( response ){
                    if( response.error ){
                        throw new Error("Wiki servier rpc error: " + response.error);
                        callback(null);
                    } else {
                        callback( response.result );
                    }
                },
                onError: function(error){
                    //console.log(error);
                    throw new Error("Wiki rpc error: "+error);
                    callback(null);

                }
            }).send();
            */

        }

    }

};

Wiki.initialize();
