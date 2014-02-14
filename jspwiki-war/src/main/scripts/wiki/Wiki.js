/*!
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
/*global HighlightQuery, HighlightAccesskey, Behavior, FileUpload, TabbedSection */
/*exported  Wiki */


/*
Script: wiki.js
    Javascript routines to support JSPWiki, a JSP-based WikiWiki clone.

License:
    http://www.apache.org/licenses/LICENSE-2.0

Since:
    v.2.9.0

Dependencies:
    Based on http://mootools.net/ v1.4.5
    * mootools-core.js : v1.4.5 excluding the Compatibility module
    * mootools-more.js : v1.4.0.1 including...
        Class.Binds, Element.Shortcuts, Fx.Accordion, Drag, Drag.Move, Hash.Cookie, Tips


Core Wiki Routines:
    *    [Wiki] object (page parms, UserPrefs and setting focus)
    *    [WikiSlimbox]
    *    [SearchBox]: remember 10 most recent search topics

Depends on :

    mooxtend.js
    behavior.js
    color.js
    localize.js

    cookie.flag.js
    collapsible.js

    file-upload.js
    graph-bar.js
    Highlight.Query.js
    Highlight.Accesskey.js
    observer.js
    placeholder.js
    reflect.js
    viewer.js
    viewer.slimbox.js
    viewer.carousel.js
    tablextend.js  => TableStuff  (sortable, filters, zebra-stripes,  ?select&calculate)
    tabs.js => TabbedSection => Tabs


    wiki.js
    wiki.category.js
    wiki.d-styles
    wiki.navigate.js
    wiki.recent-search.js
    wiki.search-box.js
    wiki.search.js

    wiki.prefs.js
    wiki.admin.js

    wiki.edit
        dialog.js
        snip-editor.js
        textarea.js
        undoable.js
*/

//"use strict";

/*
Class: Wiki
    Javascript support functions for jspwiki.  (singleton)

*/
var Wiki = {

    initialize: function(){

        var behavior = new Behavior(),
            wiki = this,
            prefs = new Hash.Cookie('JSPWikiUserPrefs', {path:wiki.BasePath, duration:20});

        wiki.add = behavior.add.bind(behavior);
        wiki.once = behavior.once.bind(behavior);
        wiki.update = behavior.update.bind(behavior);


        //wiki.get = function(name){ return wiki.prefs.get(name); };
        wiki.get = prefs.get.bind(prefs);
        //wiki.set = function(name,value){ return wiki.prefs.set(name,value); };
        wiki.set = prefs.set.bind(prefs);
        wiki.erase = prefs.erase.bind(prefs);

        //FIXME : link dom elements with behaviors
        //wiki.add('input[placeholder]', function(element){ element.placeholderX(); })

            //.add('input[autofocus],textarea[autofocus]', function(element){ element.focus(); })
            //wiki.add('input:autofocus,textarea:autofocus', function(element){
            //    console.log('autofocus', element);
            //    element.focus();
            //})

        if( !('autofocus' in document.createElement('input') ) ){
            wiki.add('input[autofocus=autofocus], textarea[autofocus=autofocus]', function(element){
                console.log('autofocus', element);
                //CHECKME
                //    plain.jsp            'wikiText' OK 
                //    login.jsp            'j_username' OK
                //    prefs/profile        'loginname' 
                //    prefs/prefs        'assertedName' OK
                //    find                 'query2'  OK
                return this.isVisible() && element.focus();
            });
        }


        wiki.add( '*[accesskey]', Accesskey )

            .add('*[data-toggle]', function(element){
                element.onToggle( element.get('data-toggle') /*, 'active'*/ );
            })

            .add('*[data-hover-parent]', function(element){
                element.onHover( element.get('data-hover-parent') /*, 'active'*/ );
            })

            .add('.searchbox .dropdown-menu', function(element){

                var recentsCookie = 'RecentSearch';
                
                //activate Recent Searches functionality
                new wiki.Recents( element, {
                    items: wiki.get(recentsCookie),
                    onChange: function( items ){
                        items ? wiki.set(recentsCookie, items) : wiki.erase(recentsCookie);
                    }
                });

                //activate Quick Navigation functionality
                new wiki.Findpages(element,{
                    rpc: function(value, callback){
                        wiki.jsonrpc('search.findPages', [value,16], callback); 
                    },
                    toUrl: wiki.toUrl.bind(wiki)
                });

            })

            .add('#searchform2', function( form ){

                wiki.search = new wiki.Search( form, {
                    xhrURL: wiki.XHRSearch,
                    onComplete: function(){ 
                        //console.log(form.query.get('value')); 
                        wiki.set('PrevQuery', form.query.get('value')); 
                    }
                });

            })

            //.add('#uploadForm input[type=file]', Form.Upload, { 
            .add('#files', Form.File, { 
                max:1,  //CHECK: jspwiki v.2.10.x seems to only support 1 upload-file at a time ?? 
                rpc: function(progressid, callback){
                    wiki.jsonrpc('progressTracker.getProgress', [progressid], callback); 
                },
            });

        window.addEvent('domready', wiki.domready.bind(wiki) );

    },

    /*
    Function: domready
        After the DOM is fully loaded,
        - initialize the main wiki properties (meta data, prefs cookie, ...)
        - once all behavior's are defined, call the update() to initiate them all
          (activation of all dynamic styles)
        - final actions:
            - HighLight words in case the referrer page was a search query
            - when the 'referrer' url (previous page) contains a "section=" parameter,
              scroll the wiki page to the right section
            - invoke periodical url hash parser

    */
    domready: function(){

        var wiki = this;

        wiki.getMeta();

        wiki.url = null;  //??check why this is needed

        if ( wiki.Context!='preview' && wiki.EditPermission && (wiki.get('SectionEditing:checked')) ){

            wiki.addEditLinks( wiki.EditUrl );

        }

        wiki.scrollToSection( (document.referrer.match( /\&section=(\d+)$/ )||[-1])[0] );

        // Highlight previous search-query (in cookie) or referrer page's search query
        HighlightQuery( $('pagecontent'), wiki.get('PrevQuery') );
        wiki.erase('PrevQuery');

        // initialize all the element behaviors
        wiki.update();

        //todo -- check bootstrap router concept
        //wiki.parseHash.periodical(500);

    },

    /*
    Function: getMeta
        Read all the "meta" dom elements, prefixed with "wiki",
        and add them as properties to the wiki object.
        EG  <meta name="wikiContext">  becomes  wiki.Context
 
        * wikiContext : jspwiki requestcontext variable (view, edit, info, ...)
        * wikiBaseUrl
        * wikiPageUrl: page url template with dummy pagename "%23%24%25"
        * wikiEditUrl : edit page url
        * wikiJsonUrl : JSON-RPC url
        * wikiPageName : pagename without blanks
        * wikiUserName
        * wikiTemplateUrl : path of the jsp template
        * wikiApplicationName
        * wikiEditPermission

    */
    getMeta: function(){

        var url,
            wiki = this,
            host = location.host;

        $$('meta[name^=wiki]').each( function(el){
            wiki[el.get('name').slice(4)] = el.get('content')||'';
        });
        
        // BasePath: if JSPWiki is installed in the root, then we have to make sure that
        // the cookie-cutter works properly here.
        url = wiki.BaseUrl;
        url = url ? url.slice( url.indexOf(host) + host.length, -1 ) : '';
        wiki.BasePath = ( url /*===''*/ ) ? url : '/';
        //console.log("basepath: " + wiki.BasePath);

    },

    /*
    Function: getSections
        Returns a list of all section headers, excluding the header of the Table Of Contents.

    */
    getSections: function(){

        return $$('.page-content *[id^=section]:not(#section-TOC)');

    },

    /*
    Function: scrollToSection
        Scrolls the page to the section previously being edited - if any
        Section counting starts at 1??
    */
    scrollToSection:function( index ){

        //console.log("SCROLL to section", index, ", Number of sections:",this.getSections().length );

        var element = this.getSections()[index], pos;

        if( element ){

            pos = element.getPosition();
            window.scrollTo(pos.x, pos.y);

        }

    },

    /*
    Property: toUrl
        Turn a wiki pagename into a full wiki-url
    */
    toUrl: function(pagename, isEdit, isClone){

        var url = isClone ? this.CloneUrl : isEdit ? this.EditUrl : this.PageUrl;

        return url.replace(/%23%24%25/, this.cleanPageName(pagename) );

    },

    /*
    Property: toPageName
        Parse a wiki-url and return the corresponding wiki pagename
    */
    toPageName: function(url){

        var s = this.PageUrl.escapeRegExp().replace(/%23%24%25/, '(.+)');
        return ( url.match( RegExp(s) ) || [,false] )[1];

    },

    /*
    Property: cleanPageName
        Remove all not-allowed chars from a *candidate* pagename.
        Trim repeated whitespace, allow letters, digits and punctuation chars: ()&+,-=._$
        Ref. org.apache.wiki.parser.MarkupParser.cleanPageName()
    */
    cleanPageName: function(p){

        //return p.clean().replace(/[^0-9A-Za-z\u00C0-\u1FFF\u2800-\uFFFD()&+,-=._$ ]/g, '');
        //\w is short for [A-Z_a-z0-9_]
        return p.clean().replace(/[^\w\u00C0-\u1FFF\u2800-\uFFFD()&+,=.$ ]/g, '');

    },

    /*
    Function: parseHash
        Periodic validation of #hash to ensure hidden screen sections are displayed.
        (eg tabs, accordions, ...)

    FIXME:
        Add handling of BACK button for tabs ??
        Use concept of ROUTER from backbone ??
    */
    parseHash: function(){

        var h = location.hash;

        if(this.url && this.url == location.href ){ return; }
        this.url = location.href;

        if( !h /*|| h===''*/ ){ return; }
        h = $( h.slice(1) );


        while( typeOf( h ) == 'element' ){

            if( h.hasClass('hidetab') ){

                TabbedSection.click.apply($('menu-' + h.id));

            } else if( h.hasClass('tab') ){

                /* accordion -- need to find accordion toggle object */
                h.fireEvent('onShow');

/*            } else if( !h.isVisible() ){
                //alert('not visible'+el.id);
                //fixme need to find the correct toggler
                el.show(); //eg collapsedBoxes: fixme
*/
            }
            h = h.getParent();
        }

        location = location.href; /* now jump to the #hash */
    },


  /*
    Function: addEditLinks
        Inject Section Edit links.
        Todo: should better move server side
    */
    addEditLinks: function( url ){

        var description = 'quick.edit'.localize();

        url = url + (url.contains('?') ? '&' : '?') + 'section=';

        this.getSections().each( function(element, index){

            element.grab('a.edit-section'.slick({ html:description, href:url+index }));

        });

    },


    /*
    Function: jsonrpc
        Generic json-rpc routines to talk to the backend jspwiki-engine.

    Note:
        Uses the JsonUrl which is read from the meta element "WikiJsonUrl"
        {{{ <meta name="wikiJsonUrl" content='/JSPWiki-pipo/JSON-RPC' /> }}}

    Supported rpc calls:
        - {{search.findPages}} gets the list of pagenames with partial match
        - {{progressTracker.getProgress}} get a progress indicator of attachment upload
        - {{search.getSuggestions}} gets the list of pagenames with partial match

    Example:
        (start code)
        Wiki.jsonrpc('search.findPages', ['Janne',20], function(result){
            //do something with the resulting json object
        });
        (end)
        
    */
    jsonid : 1e4, //seed
    jsonrpc: function(method, params, callback){

        if(this.JsonUrl){

            new Request.JSON({
                url: this.JsonUrl,
                data: JSON.encode({ 
                    //jsonrpc:'2.0', //CHECK
                    id: this.jsonid++, 
                    method: method, 
                    params: params 
                }),
                method: 'post',
                onSuccess: function(r){ 
                    if(r.error) console.log(r.error);
                    callback(r.result); 
                },
                onError: function(e){
                    console.log(e); 
                    callback(null);
                }
            }).send();

        }
    }

};

Wiki.initialize();