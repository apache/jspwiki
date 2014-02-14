/*!
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*jslint forin: true, onevar: true, nomen: true, plusplus: true, immed: true */

/*
Function: toISOString
    Return the current date in ISO8601 format 'yyyy-mm-dd'.
    (ref. EcmaScript 5)

Example:
> alert( new Date().toISOString() ); // alerts 2009-05-21
> alert( new Date().toISOString() ); // alerts 2009-05-21T16:06:05.000TZ
*/
Date.extend({
    toISOString: function(){
        var d = this,
            dd = d.getDate(),
            mm = d.getMonth()+1;

        return d.getFullYear() + '-' + (mm<10 ?'0':'') + mm + '-' + (dd<10 ?'0':'') + dd;
    }
});



/*
Class: WikiEdit
    The WikiEdit class implements the JSPWiki's specific editor, with support
    for JSPWIki's markup, suggestion popups, ajax based page preview, etc...

    It uses an enhanced textarea based on the [SnipEditor] class.
*/

//Wiki.Edit = { ... }
var WikiEdit = {

    initialize: function(){

        //ensure the main Wiki class is initialized. Seems not to be guaranteed on ie.
        Wiki.initialize();

        var txta = $('wikiText'),
            self = this,
            snipe,
            config,
            livepreviewFn = self.livepreview.bind(self),
            orientationFn = self.orientation.bind(self),
            configFn = self.config.bind(self),
            //URL?section=0..n
            section = location.search.match(/[&?]section=(\d+)/),
            prefs = Wiki.prefs;

        if( txta ){

            self.onbeforeunload( txta );

            snipe = self.snipEditor = new SnipEditor(txta, {

                container: txta.getParent('form'),
                snippets: self.tabSnippets,
                directsnips: self.directSnippets,
                //buttons: ' .toolbar .cmd',

                // section = 'all' or 0..n (section# - first section is 0)
                sectionCursor: section ? section[1].toInt() : 'all',
                sectionParser: (Wiki.Context =='edit') ? self.sectionParser : null,

                // snippet events
                onChange: livepreviewFn, //or periodical ??
                onConfig: configFn,
                onOrientation: orientationFn

            });

            //initconfig also triggers the change event, and run the first livepreview ?
            self.initconfig( prefs );

            //livepreviewFn('livepreview');  //periodical(3000);
            orientationFn(); //initialization of the preview orientation

            self.makeResizable( $('resize-handle'), $(snipe), $('previewarea'), prefs );

        }

    },

    /*
    Function: initconfig
        Initialize the configuration checkboxes from the toolbar.
        Set the checkbox according to the wiki-prefs (cookie)
        and configure the snipEditor.
    */
    initconfig: function( prefs ){

        var config = this.config, el;

        ['tabcompletion','smartpairs','autosuggest','livepreview'].each( function(cmd){

            if( el = $(cmd) ){
                el.checked = !!prefs.get(cmd);
                config( cmd );
            }

        })

    },

    /*
    Function: config
        Change the configuration of the snip-editor, and store it
        in the wiki-prefs. (cookie)
        The configuration is read from a DOM Checkbox element.
        The name of the DOM checkboxes correponds with the cookie names,
        and the cookienames corresponds with the snip-editor state attribute.

        - invoked by initconfig, to initialize checkboxes with cookie values.
        - invoked when the config cmd checkboxes are clicked (ref. snippet commands)

    Argument:
        cmd - which configuration command has been triggered / needs to be initialized.
    */
    config: function( cmd ){

        //console.log("config "+cmd);
        var el = $(cmd), snipEditor = this.snipEditor;

        if( el ){
            el = el.checked;
            Wiki.prefs.set(cmd, el);
            snipEditor.set(cmd, el);
        }
        snipEditor.fireEvent('change');
    },

    /*
    Function: orientation
        Change the orientation of the wiki editor textarea and preview area.
        Uses oocss classes 'size1of1' or 'size1of2' to generate a vertical
        or horizontal orientation.

        Driven throught the cmd "tile-vertical" or "tile-horizontal".

    Argument:
        cmd : (optional) string 'tile-horizontal' or 'tile-vertical'

    */
    orientation: function( cmd ){

        var cookie = 'orientation', //used to be 'previewlayout'
            horizontal = 'tile-horizontal',
            css = ['size1of2', 'size1of1'];

        if( !cmd ){ cmd = Wiki.prefs.get(cookie) || horizontal; }
        Wiki.prefs.set(cookie, cmd);
        //console.log('config orientation '+cmd);

        //orientation buttons have the .orientation css class
        $$('.'+cookie).each( function(el){
            el.ifClass( el.get('text') == cmd, 'disabled' );
        });

        //change actual orientation : swap oocss classes 'size1of1' or 'size1of2'
        if( cmd == horizontal ){ css = css.reverse(); }
        $(this.snipEditor).getElements("! ."+css[0]).swapClass(css[0],css[1]);

    },

    /*
    Function: onbeforeunload
        Install an onbeforeunload handler, which is called ''before'' the page unloads.
        The user gets a warning in case the textarea was changed, without saving.

        The onbeforeunload handler is automatically removed on regular exit of the page.
    */
    onbeforeunload: function(txta){

        window.onbeforeunload = function(){
            if( txta.value != txta.defaultValue ){ return "edit.areyousure".localize(); }
        };

        txta.getParent('form').addEvent('submit', function(){
            window.onbeforeunload = null;
        });
    },

    /*
    Function: makeResizable
        Activate the resize handle.
        While dragging the textarea, also update the size of the
        preview area. Store the new height in the 'EditorSize' prefs cookie.

    Arguments:
        resizeHandle - draggable resize handle (DOM element)
        txta - resizable textarea (DOM element)
        preview - preview (DOM element)
        prefs - wiki user preferences (Hash.Cookie)
    */
    makeResizable: function(resizeHandle, txta, preview, prefs){

        var cookie = 'Editorcookie',
            height = 'height',
            size = prefs.get(cookie),
            y;

        if( resizeHandle ){

            if( size ){    $$(txta, preview).setStyle(height, size); }

            txta.makeResizable({
                handle: resizeHandle,
                modifiers: { x:null },
                onDrag: function(){
                    y = this.value.now.y;
                    preview.setStyle(height, y);
                    prefs.set(cookie, y);
                }
            });

        }
    },

    /*
    Function: directSnippets
        DirectSnippet definitions for JSPWiki, aka ''smartpairs''.
        These snippets are directly expanded on keypress.
    */
    directSnippets: {
        '"' : '"',
        '(' : ')',
        '[' : ']',
        '{' : '}',
        '%%' : ' /%',
        "'" : {
            snippet:"'",
            scope:{
                "[{":"}]"  //plugin parameters
            }
        }
    },

    /*
    FIXME
    Function: autoCorrect
        These snippets are directly expanded on keypress.
        - single chars, are extended with the auto-correct snippet
        - multi chars, are replaced by the auto-correct snippet
    */
    autoCorrect: {
        "(" : ")",
        '[' : ']',
        '{' : '}',
        '%%' : '%% /%',

        "teh":"the",
        "Teh":"The",
        "(c)":"&copy;",
        "(tm)":"&trademark;"
    },

    /*
    Function: tabSnippets
        Definitions for the JSPWiki editor commands.

        Some commands are predefined by the snipe editor:
        - find : toggles the find and replace dialog
        - sections : toggle sections dropdown dialog, which allows to switch
            between certain sections of the document or the whole document
        - undo : undo last command, set the editor to the previous stable state
        - redo : revert last undo command

        A command consists of triggers, attributes, snippets, events and dialogs.

        Triggers : click events, suggestion dialogs, TAB-completion and Ctrl-keys.

        Click events are attached to DOM elements with a .cmd css-class.
        If the DOM elements also contains a .pop css-class, a dialog will be opened.

        TAB-completion can be turned on/off via the 'tabcompletion' flag.

        The 'keyup' event can trigger a suggestion dialog:
        - the suggest(txta,caret) function validates the suggestion context
          It returns true/false and can modify the snippet with
             - snip.start : begin offset of the matched prefix
             - snip.match : matched prefix (string)
             - snip.tail: (optional) replaceable tail

        Attributes:
        - initialize: function(cmd, snip) called once at initialization of snipe
        - key: shortcut key  (ctrl-key)
        - scope: set to TRUE when the cmd is valid
        - nscope: set to TRUE when the cmd is not valid
        - cmdId: wysiwyg mode only (commandIdentifier)

        Snippet:
        The snippet contains the inserted or replaced text.
        - static snippet: "some string"
        - snippet with parameters in {} brackets: "some {dialog1} string"
          A {.} will be replaced by the selected text.
          A {dialog1} opens a dialog, and inserts the caputered info.
        - dynamic snippet: javascript function.
          Example:
              snippet: function(){
                  this.dialogs.exec( dialog-name, ...
                      onChange: function(value){  }
                  )
              }

        Event:
        Fires an event back to the invoking Object (WikiEdit in this case)
          Example:
            smartpairs: { event: 'config' }

        Dialogs:
        Note: use unique names across all snippets
        - <dialog-name>: [ AnyDialogClass, {dialog-parameters, event-handlers} ]
        - <dialog-name>: "dialog initialization string"
          This is a short-cut for [Selection, "dialog initialization string"]
        The Dialog Classes are subclass of Dialog. (eg. Dialog.Selection)


    Examples:

     acl: {
         nscope: { "[{" : "}]" },
         snippet: "[\\{ ALLOW {permission} {principal(s)}  }]"

        permission: "view|edit|delete",
         "principals(s)": [Dialog.Selection, {
                 onOpen: function(){ this.setBody( AJAX-retrieval of suggestions )   }
         ]
     }

     link: {
         suggest: function(){
             //match [, but not [[ or [{
             //defines .start, .selection, .trail ??
         }
         snippet: "{wikiLink}",
         //or snippet: function(){ this.dialogs.exec('wikiLink'); },
         wikiLink: [Dialog.Link, {
            onOpen: function(){
                AJAX-retrieval of suggestions
            }
         }]
     }

     color: {
        nscope: {"%%(":")"},
        action: "%%(color:#000000; background:#ffffff;) {.} \%",
     }
     colorsuggestion: {
        scope: {"%%(":")"},
        suggest: function(){
            //match #cccccc
        }
        snippet: "{color}",
        color: [ dialog.Color, {
            //parms
        }]
     }


    */
    tabSnippets: {

        find: {
            key: "f"
            //predefined find dialog triggered via Ctrl-f or a toolbar 'find' button
        },

        //sections:
        //predefined section dialog triggered via a toolbar 'sections' button
        //add shortcut key ??
        //TODO: embed it into the LINK dialog

        undo: {
            //event: "undo", //predefined snipe event
            action: function(){ this.undoredo.onUndo(); },
            key: "z"
        },

        redo: {
            //event: "redo", //predefined snipe event
            action: function(){ this.undoredo.onRedo(); },
            key: "y"
        },

        smartpairs: { event: 'config' },
        livepreview: { event: 'config' },
        autosuggest: { event: 'config' },
        tabcompletion: { event: 'config' },
        'tile-vertical': { event: 'orientation' },
        'tile-horizontal': { event: 'orientation' },

        br: {
            key: "shift+Enter",
            snippet: "\\\\\n"
        },

        hr: "\n----\n",

        h1: "\n!!!{Heading 1 title}\n",
        h2: "\n!!{Heading 2 title}\n",
        h3: "\n!{Heading 3 title}\n",

        h: {
            suggest: function(txta,caret){
                var c,result=txta.slice(0,caret.start).match( /(?:^|[\n\r])(!{1,3}[^\n\r]*)$/ );

                if( result ){
                    c = result[1];
                    result = {
                        start: caret.start - c.length,
                        match: c + txta.slice(caret.start).match( /[^\n\r]*/ )||''  //entire line
                    };
                }
                return result;
            },
            h: [Dialog.Selection, {
                onOpen: function(){
                    var value = (this.getValue().replace(/^!+\s?/,'')||'Title'), //remove !markup
                        val = value.trim().trunc(20),
                        k = ['!!! '+value,'!! '+value,'! '+value],
                        v = ['<h2>'+val+'</h2>','<h3>'+val+'</h3>','<h4>'+val+'</h4>'];

                    this.setBody( v.associate( k ) );
                }
            }]
        },

        font: {
            nScope: {
                "%%(":")",
                "font-family:":";"
            },
            /*
            suggest: function(txta,caret){
                //match /%%(:?.*)font-family:([^;\)]+)/

            },*/
            snippet: "%%(font-family:{font};) {.}/% ",
            font: [Dialog.Font, {}]
        },

        color: {
            nscope: { '%%(': ')' },
            snippet: "%%(color:{#000000}; background:{#ffffff};) {.} ",
            suggest: function(txta, caret){
                //match "#cccccc;" pattern
                var c,d, result = txta.slice(0,caret.end).match( /#[0-9a-f]{0,6}$/i );

                if( result ){
                    c = result[0];
                    d = txta.slice( caret.end ).match( /^[0-9a-f]+/i )||'';
                    result = {
                        start: caret.end - c.length, //position of # char
                        match: (c+d).slice(0,7)
                    };
                }
                return result;
            },
            color: [ Dialog.Color, {
                colorImage:'./test-dialog-assets/circle-256.png'
            }]
         },

        symbol: { synonym:"chars" },
        chars: {
            nScope: { "%%(":")" },
            snippet: "{&entity;}",
            suggest: function(txta, caret){
                //match &xxx;
                var c,result = txta.slice(0,caret.end).match( /&[\da-zA-Z]*;?$/ );

                if( result ){
                    c = result[0];
                    result = {
                        start: caret.end - c.length,
                        match: c,
                        tail: txta.slice( caret.end ).match( /^[\da-zA-Z]*;?/ )||''
                    }
                }
                return result;

            },
            chars: [Dialog.Chars, {caption:"Special Chars".localize()}]
        },

        style: { synonym:"css"},
        css: {
            nScope: { "%%(":")" },
            snippet: "%%{css} {.} /% ",
            suggest: function(txta, caret){
                //match %%(.w+)
                var c, result = txta.slice(0,caret.end).match(/%%[\da-zA-Z(:#;)]*$/);

                if(result){
                    c = result[0].slice(2); //drop the %% prefix
                    result = {
                        start: caret.end - c.length,
                        match: c + txta.slice( caret.end ).match( /^[\da-zA-Z(:#;)]*/ )||''
                    };
                }
                return result;
            },
            css: {
                "(css:value;)":"any css definitions",
                information:"information",
                warning:"warning",
                error:"error",
                commentbox:"commentbox",
                quote:"quoted paragraph",
                sub:"sub-script<span class='sub'>2</span>",
                sup:"super-script<span class='sup'>2</span>",
                strike:"<span class='strike'>strikethrough</span>",
                xflow:"wide content with scroll bars"
            }
        },

        //simple tab completion commands
        sub: "%%sub {subscript text}/% ",
        sup: "%%sup {superscript text}/% ",
        strike: "%%strike {strikethrough text}/% ",
        xflow: "\n%%xflow\n{wide content}\n/%\n ",
        quote: "\n%%quote \n{quoted text}\n/%\n",
        dl: "\n;{term}:{definition-text} ",
        pre: "\n\\{\\{\\{\n{some preformatted block}\n}}}\n",
        code: "\n%%prettify \n\\{\\{\\{\n{/* some code block */}\n}}}\n/%\n",
        mono: "\\{\\{{monospaced text}}} ",

        link: {
            key:'l',
            commandIdentifier:'createlink',
            suggest: function(txta, caret){

                //match [link] or [link,  do not match [{, [[
                //match '[' + 'any char except \n, [, { or ]' at end of the string
                var result = txta.getFromStart().match( /\[([^\[\{\]\n\r]*)$/ ),
                    link;

                if( result ){
                    link = result[1].split('|').getLast(); //exclude "text|" prefix
                    result = {
                        start: caret.start - link.length ,
                        //if no input yet, then get list attachments of this wikipage
                        match: link,
                        tail: txta.slice( caret.start ).search( /[\n\r\]]/ )
                    };
                }
                return result;
            },

            //snippet: "[{display text}|{pagename or url}|{attributes}] ",
            snippet: "[{link}] ",
            //attributes: "accesskey='X'|title='description'|target='_blank'
            //    'accesskey', 'charset', 'class', 'hreflang', 'id', 'lang', 'dir',
            //  'rel', 'rev', 'style', 'tabindex', 'target', 'title', 'type'
            //    display-text
            //    wiki-page or url -- allow to validate the url ; preview the page/url
            //    title: descriptive text
            //- target: _blank --new-- window yes or no

            //link: [ Dialog.Link, { ...
            link: [ Dialog.Selection, {

                onOpen: function(dialog){

                    var dialog = this, key = dialog.getValue();
                    if(key=='') key = Wiki.PageName + '/';

                    console.log('json lookup for '+key);
                    //Wiki.jsonrpc('search.getSuggestions', [key,30], function(result,exception){
                    //offline testing:
                    var exception, result = {list:['AJAX1', 'AJAX1111', 'AJAWZZZ', 'result very longerlongerlongerlonger 3', 'results moremore']};

                    if( result.list && result.list.length /*length!=0*/ ){
                        dialog.setBody( result.list );
                    } else {
                        dialog.hide(); //ie. fireEvent onClose on the dialog
                    }

                    //}); //end of json callback
                }

            }]


        },

        bold: {
            key:'b',
            snippet:"__{bold text}__ "
        },

        italic: {
            key:'i',
            snippet: "''{italic text}'' "
        },

        allow: { synonym: "acl" },
        acl: {
            snippet: "\n[\\{ALLOW {permission} {principal(,principal)} \\}]\n",
            permission: "view|edit|modify|comment|rename|upload|delete",
            //permission:[Dialog.Selection, {body:"view|edit|modify|comment|rename|upload|delete"}]
            "principal(,principal)": function(){
                return "Anonymous|Asserted|Authenticated|All";
                //FIXME: retrieve list of available wiki user groups through ajax call
            }
        },

        img: {
            snippet:"\n[\\{Image src='{img.jpg}' width='{400px}' height='{300px}' align='{text-align}' style='{css-style}' class='{css-class}' }]\n ",
            'text-align':'left|center|right'
        },

        plugin: {
            snippet: "\n[\\{{plugin}}]\n",
            suggest: function(txta, xcaret){
                //match [{
            },
            plugin: {
                "Set a page variable":"SET name='value'",
                "Get a page variable":"$varname",
                "Test a page variable":"If name='value' page='pagename' exists='true' contains='regexp'\n\nbody\n",
                "Insert Page":"InsertPage page='pagename'",
                "Table Of Contents [toc]":"TableOfContents",
                "Make a Page Alias":"SET alias='{pagename}'",
                "Current Time":"CurrentTimePlugin format='yyyy mmm-dd'",
                "Incoming Links":"ReferredPagesPlugin page='pagename' type='local|external|attachment' depth='1..8' include='regexp' exclude='regexp'",
                "Outgoing Links":"ReferringPagesPlugin page='pagename' separator=',' include='regexp' exclude='regexp'",
                "Search":"Search query='Janne' max='10'",
                "Display weblog posts":"WeblogPlugin page='pagename' startDate='300604' days='30' maxEntries='30' allowComments='false'",
                "New weblog entry":"WeblogEntryPlugin"
            }
        },

        tab: {
            nScope: {
                "%%(":")",
                "%%tabbedSection":"/%"
            },
            snippet:"%%tabbedSection \n%%tab-{tabTitle1}\n{tab content 1}\n/%\n%%tab-{tabTitle2}\n{tab content 2}\n/%\n/%\n "
        },

        alias: {
            nScope: { "[{":"}]" },
            snippet:"\n[\\{SET alias='{pagename}' }]\n"
        },

        toc: {
            nScope: { "[{":"}]" },
            snippet:"\n[\\{TableOfContents }]\n"
        },

        table: "\n||heading-1||heading-2\n| cell11   | cell12\n| cell21   | cell22\n",

        me: { alias: 'sign'},
        sign: function(){
            var name = Wiki.UserName || 'UserName';
            return "\\\\--" + name + ", "+ new Date().toISOString() + "\n";
        },

        date: function(k) {
            return new Date().toISOString()+' ';
            //return "[{Date value='" + d.toISOString() + "' }]"
            //return "[{Date " + d.toISOString() + " }]"
        },

        abra: {
            suggest:"abra",
            snippet:"cadabra"
        },
        abrar: {
            suggest:"abrar",
            snippet:"acurix"
        },
        lorem: "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n",
        Lorem: { synonym: "lorem" }

    },


    /*
    Function: sectionParser
        Convert a jspwiki-markup page to an array of page sections.
        Sections are marked with a JSPWiki header line. ( !, !! !!! )

        This function is a callback function for the [SnipEditor].
        It is called by [snipeditor.buildToc] every time the textarea of the
        snipeditor is being changed.

    Returns:
        This function returns a array of objects [{title, start, depth}]
        title - (string) plain title of the section (no wiki markup)
        start - (number) offset within the text string where this section starts
        depth - (number) nesting level of the section 0,1...n
    */
    sectionParser: function( text ){

        var result = [],
            DELIM = '\u00a4',

            tt = text
                // mask any header markup inside a {{{ ... }}} but keep length of the text unchanged!
                .replace(/\{\{\{([\s\S]*?)\}\}\}/g, function(match){
                    return match.replace( /^!/mg, ' ' );
                })
                // break string up into array of headers and section-bodies :
                // [0] : text prior to the first header
                // [1,odd] : header markup !, !! or !!!
                // [2,even] : remainder of the section, starting with header title
                .replace( /^([!]{1,3})/mg, DELIM+"$1"+DELIM )
                .split(DELIM),

            pos = tt.shift().length,  //get length of the first element, prior to first section
            count = tt.length,
            i, hlen, title;

        for( i=0; i<count; i=i+2 ){

            hlen = tt[i].length;
            //take first line
            title = tt[i+1].split(/[\r\n]/)[0]
                //remove unescaped(~) inline wiki markup __,'',{{,}}, %%(*), /%
                .replace(/(^|[^~])(__|''|\{\{|\}\}|%%\([^\)]+\)|%%\S+\s|%%\([^\)]+\)|\/%)/g,'$1')
                //and remove wiki-markup escape chars ~
                .replace(/~([^~])/g, '$1');

            //depth: convert length of header markup (!!!,!!,!) into #depth-level:  3,2,1 => 0,1,2
            result.push({ title:title, start:pos, depth:3-hlen });
            pos += hlen + tt[i+1].length;
        }

        return result;
    },

    /*
    Function: livepreview
        Linked as onChange handler to the SnipEditor.
        Make AJAX call to the backend to convert the contents of the textarea
        (wiki markup) to HTML.
        FIXME: should work bothways. wysiwyg <-> wikimarkup

    */
    livepreview: function(){

        var self = this,
            page = Wiki.PageName,
            text = $(self.snipEditor).value,
            preview = $('previewarea'),
            loading = preview.getPrevious(); //loading... block

        if( !$('livepreview').checked ){

            //clean preview area
            if( self.previewcache ){
                preview.empty();
                self.previewcache = null;
            }

        } else if( self.previewcache != text.length ){

            self.previewcache = text.length;
            return preview.set('html',preview.get('html')+' Lorem ipsum'); //test code

            new Request.HTML({
                url: Wiki.BaseUrl + "Edit.action?ajaxPreview&page=" + page,
                data: 'wikiText=' + encodeURIComponent( text ),
                update: preview,
                onRequest: function(){ loading.show(); },
                onComplete: function(){ loading.hide(); Wiki.renderPage(preview, page); }
            }).send();

        }
    }
};


/*
Global: load/domready
    Initialize the WikiEdit class on page load.
*/
window.addEvent('domready', function(){ WikiEdit.initialize(); } );



/*
Class: SnipEditor
    The SnipEditor class enriches a TEXTAREA object with capabilities such as
    section editing, tab-autocompletion, auto-indentation,
    smart typing pairs, suggestion popups, toolbars, undo and redo functionality,
    find & replace etc.
    The snip-editor can be configured with a set of snippet commands.
    See [getSnippet] for more info on how to define snippets.

Credit:
    The SnipEditor was inspired by postEditor (by Daniel Mota aka IceBeat,
    http://icebeat.bitacoras.com ) and ''textMate'' (http://macromates.com/).
    It has been written to fit as wiki markup editor for the JSPWIKI project.

    Dirk Frederickx, Oct-Dec 2008 - Dec 2010

Arguments:
    el - textarea element
    options - optional, see below

Options:
    tab - (string) number of spaces used to insert/remove a tab in the textarea;
        default is 4
    snippets - (snippet-object) set of snippets, which will be expanded when
        clicking a button or pressing the TAB key. See [getSnippet], [tabSnippet]
    tabcompletion - (boolean, default false) when set to true,
        the tabSnippet keywords will be expanded
        when pressing the TAB key.  See also [tabSnippet]
    directsnips - (snippet-object) set of snippets which are directly expanded
        on key-down. See [getSnippet], [directSnippet]
    smartpairs - (boolean, default false) when set to true,
        the direct snip (aka smart pairs) will be expanded on keypress.
        See also [directSnippet]
    suggestsnips - (snippet-object) set of snippets which are triggered at
        key-up or mouse click events. Typically suggestsnips are used to generate
        help dialog popups.
    buttons - (array of Elements), each button elemnet will bind its click-event
        with [onButtonClick}. When the click event fires, the {{rel}} attribute
        or the text of the element will be used as snippet keyword.
        See also [tabSnippet].
    dialogs - set of dialogs, consisting of either a Dialog object,
        or a set of {dialog-options} for the predefined
        dialogs suchs as Font, Color and Special.
        See property [initializeDialogs] and [openDialog]
    findForm - (object) list of form-controls. See [onFindAndReplace] handler.
    next - (Element), when pressing Shift-Enter, the textarea will ''blur'' and
        this ''next'' element will ge the focus.
        This compensates the overwritting default TAB handling of the browser.
    onresize - (function, optional), when present, a textarea resize bar
        with css class {{resize-bar}} is added after the textarea,
        allowing to resize the heigth of the textarea.
        This onresize callback function is called whenever
        the height of the textarea is changed.

Dependencies:
    [Textarea]
    [UndoRedo]
    [SnipEditor.Tools]

Example:
    (start code)
    <script>
    new SnipEditor( "mainTextarea", {
        snippets: { bold:"**{bold text}**", italic:"''{italic text}''" },
        tabcompletion:true,
        directsnips: { '(':')', '[' : ']' },
        buttons: $$('a.tool'),
        next:'nextInputField'
    });
    </script>
    (end)

*/
var SnipEditor = new Class({

    Implements: [Options, Events],

    Binds: ['sync','parseSections','updateSection','shortcut','keystroke','suggest'],

    options: {
        tab: "    ", //default tab = 4 spaces
        //autosuggest:false, //todo
        //tabcompletion:false,
        //autocompletion:false,  => smartpairs
        //autocorrect:false  => todo
        snippets: {},
        directsnips: {}, //smartpairs
        //container: null,   //DOM element, container for .cmd elements (eg toolbar buttons)
        sectionCursor: 'all',
        sectionParser: function(){ return {}; }
    },

    initialize: function(el, options){

        options = this.setOptions(options).options;

        var self = this,

            /*
            The textarea is cloned into a main and work area.
            The workarea is visible and used for the actual editing.
            It contains the full document or a particular section.
            The mainarea is hidden and always contains the full document.
            On submit, the mainarea is send back to the server.
            */
            main = self.mainarea = $(el),
            work = main.clone().erase('name') //.clone(true,false), dont copy ID and name
                .inject( main.hide(), 'before' ),

            // Augment the textarea element with some extra capabilities
            // Make sure the content of Main is always in sync with work
            textarea = self.textarea = new Textarea( work );

        self.undoredo = new UndoRedo( self, {undo:'.tUNDO', redo:'.tREDO'} );

        //The Commands class processes all commands
        //   entered via tab-completion, button clicks, dialog or suggestion dialog
        //   Valid commands are given back to the SnipEditor via the onAction event.
        self.commands = new SnipEditor.Commands({

            container: options.container,
            //textarea: textarea,

            onOpen: function(/*cmd, eventHdl*/){ work.focus(); },

            onClose: function(){ work.focus(); },

            onAction: function(cmd){ self.action(cmd, Array.slice(arguments,1) ); },

            //add predefined dialogs, which are closely linked with the internals of the snipeditor
            dialogs: {
                find: [Dialog.Find,{
                    dialog: options.container.getElement('.find'),
                    data: {
                        //feed the find dialog with searchable content: selection or all
                        get: function(){
                            var s = textarea.getSelection();
                            return (s=='') ? work.value : s;
                        },
                        set: function(v){
                            var s = textarea.getSelectionRange();
                            self.undoredo.onChange();
                            s.thin ? work.value = v : textarea.setSelection(v);
                        }
                    }
                }],
                sections : new SnipEditor.Sections({
                    main: main,
                    work: work,
                    selected: options.sectionCursor,
                    parser: options.sectionParser
                })
            }
        });

        self.initSnippets( options.snippets );
        self.clearContext();

        work.addEvents({
            keydown: self.keystroke,
            keypress: self.keystroke,
            //fixme: any click outside the suggestion block should clear the active snip -- blur ?
            //blur: self.clearContext.bind(self), //(and hide any open dialogs)
            keyup: self.suggest,
            click: self.suggest
        });

        //catch shortcut keys when focus on toolbar or textarea
        options.container.addEvent( 'keypress', self.shortcut);

    },

    /*
    Function: initSnippets
        Initialize the snippets and collect all shortcut keys and suggestion snips
    */
    initSnippets: function( snips ){

        var self = this, cmd, snip, key,
            dialogs={};

        self.keys = {};
        self.suggestions = {};

        for( cmd in snips ){
            snip = snips[cmd];

            if( typeOf(snip)=='string' ){ snip = {snippet:snip}; }
            //snip.cmd = cmd;  NOK -- snip.cmd is the default suggestion dialog definition !!

            Function.from( snip.initialize )(cmd, snip);

            key = snip.key;
            if( key ){ self.keys[key] = cmd; }

            if( typeOf(snip.suggest)=='function' ){ self.suggestions[cmd] = snip; }

            //check for default snip dialogs --EG:  find:{find:[Dialog.Find,{options}] }
            if( snip[cmd] ){ dialogs[cmd] = snip[cmd]; }

        }

        self.commands.addDialogs(dialogs, self.textarea);
        //console.log(self.commands.dlgs);
    },


    /*
    Function: toElement
        Retrieve textarea DOM element;

    Example:
    >    var snipe = new SnipEditor('textarea-element');
    >    $('textarea-element') == snipe.toElement();
    >    $('textarea-element') == $(snipe);

    */
    toElement: function(){
        return $(this.textarea);
    },

    /*
    Function: get
        Retrieve some of the public properties of the snip-editor.

    Arguments:
        item - textarea|snippets|tabcompletion|directsnips|smartpairs|suggestsnips
    */
    get: function(item){

        return( /textarea|snippets|tabcompletion|directsnips|smartpairs/.test(item) ? this[item] : null );

    },

    /*
    Function: set
        Set/Reset some of the public options of the snip-editor.

    Arguments:
        item - snippets|tabcompletion|directsnips|smartpairs|suggestsnips
        value - new value
    Returns
        this SnipEditor object
    */
    set: function(item, value){

        if( /tabsnip|tabcompletion|directsnips|smartpairs/.test(item) ){
            this.options[item] = value;
        }
        return this;
    },

    /*
    Function: shortcut.
        Handle shortcut keys: Ctrl+shortcut key.
        This is a "Keypress" event handler connected to the container element
        of the snip editor.
    Note:
        Safari seems to choke on Cmd+b and Cmd+i. All other Cmd+keys are fine. !?
        It seems in those cases, the event is fired on document level only.
    */
    shortcut: function(e){

        var keycmd = this.keys[e.key];

        if ( (Browser.Platform.mac ? e.meta : e.control) && keycmd){
            e.stop();
            this.commands.action( keycmd );
        }

    },

    /*
    Function: keystroke
        This is a cross-browser keystroke handler for keyPress and keyDown
        events on the textarea.

    Note:
        The KeyPress is used to accept regular character keys.

        The KeyDown event captures all special keys, such as Enter, Del, Backspace, Esc, ...
        To work around some browser incompatibilities, a hack with the {{event.which}}
        attribute is used to grab the actual special chars.

        Ref. keyboard event paper by Jan Wolter, http://unixpapa.com/js/key.html

        Todo: check on Opera

    Arguments:
        e - (event) keypress or keydown event.
    */
    keystroke: function(e){

        //console.log(e.code + " keystroke "+e.shift+" "+e.type+"+meta="+e.meta+" +ctrl="+e.control );

        if( e.type=='keydown' ){

            //Exit if this is a normal key; only process special chars with the keydown event
            if( !Event.Keys[e.key] ){ return; }

        } else { // e.type=='keypress'

            //Only process regular character keys via keypress event
            //Note: cross-browser hack with 'which' attribute for special chars
            if( !e.event.which /*which==0*/ ){ return; }

            //Reset faulty 'special char' treatment by mootools
            e.key = String.fromCharCode(e.code).toLowerCase();

        }

        var self = this,
            txta = self.textarea,
            el = $(txta),
            key = e.key,
            caret = txta.getSelectionRange(),
            scroll = el.getScroll();

        el.focus();

        if( /up|down|esc/.test(key) ){

            self.clearContext();

        } else if( /tab|enter|delete|backspace/.test(key) ){

            self[key](e, txta, caret);

        } else {

            self.directSnippet(e, txta, caret);

        }

        el.scrollTo(scroll);

    },

    /*
    Function: enter
        When the Enter key is pressed, the next line will be ''auto-indented''
        or space-aligned with the previous line.
        Except if the Enter was pressed on an empty line.

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    enter: function(e, txta, caret) {

        //if( this.hasContext() ){
            //fixme
            //how to 'continue previous snippet ??
            //eg '\n* {unordered list item}' followed by TAB or ENTER
            //snippet should always start with \n;
            //snippet should have a 'continue on enter' flag ?
        //}

        this.clearContext();

        if( caret.thin ){

            var prevline = txta.getFromStart().split(/\r?\n/).pop(),
                indent = prevline.match( /^\s+/ );

            if( indent && (indent != prevline) ){
                e.stop();
                txta.insertAfter( '\n' + indent[0] );
            }

        }
    },

    /*
    Function: backspace
        Remove single-character directsnips such as {{ (), [], {} }}

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    backspace: function(e, txta, caret) {

        if( caret.thin  && (caret.start > 0) ){

            var key = txta.getValue().charAt(caret.start-1),
                snip = this.getSnippet( this.options.directsnips, key );

            if( snip && (snip.snippet == txta.getValue().charAt(caret.start)) ){

                /* remove the closing pair character */
                txta.setSelectionRange( caret.start, caret.start+1 )
                    .setSelection('');

            }
        }
    },

    /*
    Function: delete
        Removes the next TAB (4spaces) if matched

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    "delete": function(e, txta, caret) {

        var tab = this.options.tab;

        if( caret.thin && !txta.getTillEnd().indexOf(tab) /*index==0*/ ){

            e.stop();
            txta.setSelectionRange(caret.start, caret.start + tab.length)
                .setSelection('');

        }
    },

    /*
    Function: tab
        Perform tab-completion function.
        Pressing a tab can lead to :
        - expansion of a snippet command cmd and selection of the first parameter
        - selection of the next snippet parameter (if active snippet)
        - otherwise, expansion to set of spaces (4)


    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection

    */
    tab: function(e, txta, caret){

        var self = this,
            snips = self.options.snippets,
            fromStart = txta.getFromStart(),
            len = fromStart.length,
            cmd, cmdlen; // ok = false;

        e.stop();

        if( self.options.tabcompletion ){

            if( self.hasContext() ){

                return self.nextAction(txta, caret);

            }

            if( caret.thin ){

                //lookup the command backwards from the text preceeding the caret
                for( cmd in snips ){

                    cmdlen = cmd.length;

                    if( (len >= cmdlen) && (cmd == fromStart.slice( - cmdlen )) ){

                        //first remove the command
                        txta.setSelectionRange(caret.start - cmdlen, caret.start)
                            .setSelection('');

                        return self.commands.action( cmd );

                    }
                }
            }

        }

        //if you are still here, convert the tab into spaces
        self.convertTabToSpaces(e, txta, caret);

    },

    /*
    Function: convertTabToSpaces
        Convert tabs to spaces. When no snippets are detected, the default
        treatment of the TAB key is to insert a number of spaces.
        Indentation is also applied in case of multi-line selections.

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    convertTabToSpaces: function(e, txta, caret){

        var tab = this.options.tab,
            selection = txta.getSelection(),
            fromStart = txta.getFromStart();
            isCaretAtStart = txta.isCaretAtStartOfLine();

        //handle multi-line selection
        if( selection.indexOf('\n') > -1 ){

            if( isCaretAtStart ){ selection = '\n' + selection; }

            if( e.shift ){

                //shift-tab: remove leading tab space-block
                selection = selection.replace(RegExp('\n'+tab,'g'),'\n');

            } else {

                //tab: auto-indent by inserting a tab space-block
                selection = selection.replace(/\n/g,'\n'+tab);

            }

            txta.setSelection( isCaretAtStart ? selection.slice(1) : selection );

        } else {

            if( e.shift ){

                //shift-tab: remove 'backward' tab space-block
                if( fromStart.test( tab + '$' ) ){

                    txta.setSelectionRange( caret.start - tab.length, caret.start )
                        .setSelection('');

                }

            } else {

                //tab: insert a tab space-block
                txta.setSelection( tab )
                    .setSelectionRange( caret.start + tab.length );

            }

        }
    },

    /*
    Function: setContext
        Store the active snip. (state)
        EG, subsequent handling of dialogs.
        As long as a snippet is active, the textarea gets the css class {{.activeSnip}}.

    Arguments:
        snip - snippet object to make active
    */
    hasContext: function(){
        return !!this.context.snip;
    },

    setContext: function( snip, suggest ){

        this.context = {snip:snip, suggest:suggest};
        $(this).addClass('activeSnip');

    },

    /*
    Function: clearContext
        Clear the context object, and remove the css class from the textarea.
        Also make sure that no dialogs are left open.
    */
    clearContext: function(){

        this.context = {};
        this.commands.close();
        $(this).removeClass('activeSnip').focus();

    },

    /*
    Function: getSnippet
        Retrieve and validate the snippet. Returns false when the snippet is not
        found or not in scope.

    About snippets:
    In the simplest case, you can use snippets to insert plain text that you do not
    want to type again and again. The snippet is expanded when hitting
    the Tab key: the ''snippet'' is replaced by ''snippet expansion text''.

    (start code)
    var tabSnippets = {
        <snippet1> : <snippet expansion text>,
        <snippet2> : <snippet expansion text>
    }
    (end)

    See also [DirectSnippets].

    For example, following snippet will expand the ''toc'' text into the
    TableOfContents wiki plugin call. Don't forget to escape '{' and '}'
    with a backslash, because they have a special meaning. (see below)
    Use the '\n' charater to define multi-line snippets. Start the snippet
    with '\n' to make sure the snippet starts on a new line.

    (start code)
    "toc": "\n[\{TableOfContents \}]\n"
    (end)

    After tab-completion, the caret is placed just after the expanded snippet.

    Snippet parameters:
    If you want, you can put ''{parameters}'' inside the snippet. Pressing the tab
    will jump to the next parameter. If you are ok with the default value,
    just tab over it. If not, start typing to overwrite it.

    (start code)
    "bold": "__{some bold text}__"
    (end)

    You can have multiple ''{parameters}'' too. Pressing more tabs will get you there.

    (start code)
    "link": "[{link text}|{pagename}]"
    (end)

    Extended snippet syntax:
    So far we discussed the simple snippet syntax. In order to unlock more advanced
    snippet features, you'll need to use the extended snippet syntax.

    (start code)
    "toc": {
        snippet : "\n[\{TableOfContents \}]\n"
    }
    (end)

    which is actually the same as

    (start code)
    "toc": "\n[\{TableOfContents \}]\n"
    (end)

    Snippet synonyms:
    Instead of defining the snippet text, you can also refer to another snippet.
    This allows you to create synonyms.

    (start code)
    "allow": {
        synonym: "acl"
    }
    (end)

    Dynamic snippets:
    Next to static snippet texts, you can also dynamically generate
    the snippet text through a javascript function. For example, you could
    use ajax calls to populate the snippet on the fly. The function should return
    either the string (simple snippet syntax) or a snippet object.
    (eg return {{ { snippet:"..." } }} )

    (start code)
    "date": function(e, textarea){
        return new Date().toLocaleString();
    }
    (end)

    or

    (start code)
    "date": function(e, textarea){
        var d = new Date().toLocaleString();
        return { 'snippet': d };
    }
    (end)

    Snippet scope:
    See [inScope] to see how to restrict the scope of a snippet.

    Parameter dialog boxes:
    To help the entry of parameters, you can specify a predefined set of choices
    for a ''{parameter}'', as a string (with | separator), js array or js object.
    A parameter dialog box will be displayed to provide easy selection of
    one of the choices.  See [Dialog.Selection].

    Example of parameter suggestion-list:

    (start code)
    "acl": {
        snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
        permission: "view|edit|modify|comment|rename|upload|delete",
        "principal(,principal)": "Anonymous|Asserted|Authenticated|All"
        }
    }
    "acl": {
        snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
        permission: [view,edit,modify]
        }
    }
    "acl": {
        snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
        permission: {'Only read access':'view','Read and write access':'edit','R/W, rename, delete access':'modify' }
        }
    }
    (end)


    Arguments:
        snips - snippet collection object for lookup of the key
        key - snippet key. If not present, retreive the key from
            the textarea just to the left of the caret. (i.e. tab-completion)

    Returns:
        Return a snippet object or false.
        (start code)
        returned_object = false || {
                key: "snippet-key",
                snippet: " snippet-string ",
                text: " converted snippet-string, no-parameter braces, auto-indented ",
                parms: [parm1, parm2, "last-snippet-string" ]
            }
        (end)
    */
    getSnippet: function( snips, cmd ){

        var self = this,
            txta = self.textarea,
            fromStart = txta.getFromStart(),
            snip = snips[cmd],
            tab = this.options.tab,
            parms = [],
            s,last;

        if( snip && snip.synonym ){ snip = snips[snip.synonym]; }

        snip = Function.from(snip)(self, [cmd]);

        if( typeOf(snip) == 'string' ){ snip = { snippet:snip }; }

        if( !snip || !self.inScope(snip, fromStart) ){ return false; }

        s = snip.snippet || '';

        //parse snippet and build the parms[] array with all {parameters}
        s = s.replace( /\\?\{([^{}]+)\}/g, function(match, name){

            if( match.charAt(0) == '\\' ){ return match.slice(1); }
            parms.push(name);
            return name;

        }).replace( /\\\{/g, '{' );
        //and finally, replace the escaped '\{' by real '{' chars

        //also push the last piece of the snippet onto the parms[] array
        last = parms.getLast();
        if(last){ parms.push( s.slice(s.lastIndexOf(last) + last.length) ); }

        //collapse \n of previous line if the snippet starts with \n
        if( s.test(/^\n/) && ( fromStart.test( /(^|[\n\r]\s*)$/ ) ) ) {
            s = s.slice(1);
            //console.log("remove leading \\n");
        }

        //collapse \n of subsequent line when the snippet ends with a \n
        if( s.test(/\n$/) && ( txta.getTillEnd().test( /^\s*[\n\r]/ ) ) ) {
            s = s.slice(0,-1);
            //console.log("remove trailing \\n");
        }

        //auto-indent the snippet's internal newlines \n
        var prevline = fromStart.split(/\r?\n/).pop(),
            indent = prevline.match(/^\s+/);
        if( indent ){ s = s.replace( /\n/g, '\n' + indent[0] ); }

        //complete the snip object
        snip.text = s;
        snip.parms = parms;

        return snip;
    },

    /*
    Function: inScope
        Sometimes it is useful to restrict the scope of a snippet, and only allow
        the snippet expansion in specific parts of the text. The scope parameter allows
        you to do that by defining start and end delimiting strings.
        For example, the following "fn" snippet will only expands when it appears
        inside the scope of a script tag.

        (start code)
        "fn": {
            snippet: "function( {args} )\{ \n    {body}\n\}\n",
            scope: {"<script":"</script"} //should be inside this bracket
        }
        (end)

        The opposite is possible too. Use the 'nScope' or not-in-scope parameter
        to make sure the snippet is only inserted when not in scope.

        (start code)
        "special": {
            snippet: "{special}",
            nScope: { "%%(":")" } //should not be inside this bracket
        },
        (end)

    Arguments:
        snip - Snippet Object
        text - (string) used to check for open scope items

    Returns:
        True when the snippet is in scope, false otherwise.
    */
    inScope: function(snip, text){

        var pattern, pos, scope=snip.scope, nscope=snip.nscope;

        if( scope ){

            if( typeOf(scope)=='function' ){

                return scope( this.textarea );

            } else {

                for( pattern in scope ){

                    pos = text.lastIndexOf(pattern);
                    if( (pos > -1) && (text.indexOf( scope[pattern], pos ) == -1) ){ return 1 /*true*/; }

                }
                return false;
            }
        }

        if( nscope ){

            for( pattern in nscope ){

                pos = text.lastIndexOf(pattern);
                if( (pos > -1) && (text.indexOf( nscope[pattern], pos ) == -1) ){ return !1 /*false*/; }

            }

        }
        return 1 /*true*/;
    },


    /*
    Function: directSnippet
        Direct snippet are invoked immediately when the key is pressed
        as opposed to a [tabSnippet] which are expanded after pressing the Tab key.

        Direct snippets are typically used for smart typing pairs,
        such as {{ (), [] or {}. }}
        Direct snippets can also be defined through javascript functions
        or restricted to a certain scope. (ref. [getSnippet], [inScope] )

        First, the snippet is retrieved based on the entered character.
        Then, the opening- and closing- chars are inserted around the selection.

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection

    Example:
    (start code)
    directSnippets: {
        '"' : '"',
        '(' : ')',
        '{' : '}',
        "<" : ">",
        "'" : {
            snippet:"'",
            scope:{
                "<javascript":"</javascript",
                "<code":"</code",
                "<html":"</html"
            }
        }
    }
    (end)

    */
    directSnippet: function(e, txta, caret){

        var self = this,
            options = self.options,

            snip = self.getSnippet( options.directsnips, e.key );

        if( snip && options.smartpairs ){

            e.stop();

            txta.setSelection( e.key, txta.getSelection(), snip.snippet )
                .setSelectionRange( caret.start+1, caret.end+1 );

        }

    },

    /*
    Function: action
        This function executes the proper action.
        The command can be given throug TAB-completion or by pressing a button.

        It looks up the snippet and inserts its value in the textarea.

        When text was selected prior to the click event, the selection will
        be injected in one of the snippet {parameter}.

        Additionally, when the snippet only contains one {parameter},
        the snippet will toggle: i.e. remove the snippet when already present,
        otherwise insert the snippet.

        TODO:
        Prior to the insertion of the snippet, the caret will be moved to the beginning of the line.
        Prior to the insertion of the snippet, the caret will be moved to the beginning of the next line.

    Arguments:
        e - (event) keypress or keydown event.
    */
    action: function( cmd, args ){

        var self = this,
            txta = self.textarea,
            caret = txta.getSelectionRange(),
            snip = self.context.snip || self.getSnippet(self.options.snippets, cmd),
            s;

        //console.log("Action: "+cmd+" ("+args+") text=["+snip.text+"] parms=["+snip.parms+"] "+!!snip);

        if( snip ){

            s = snip.text;

            if( snip.action ){    //eg undo, redo

                return snip.action.call(self, cmd, snip, args );

            }

            self.undoredo.onChange();

            if( snip.event ){

                return self.fireEvent(snip.event, [cmd, args]);

            }


            $(txta).focus();

            if( self.context.suggest ){

                return self.suggestAction( cmd, args );

            }


            if( !caret.thin && (snip.parms.length==2) ){
                s = self.toggleSnip(txta, snip, caret);
                //console.log("toggle snippet: "+s+" parms:"+snip.parms);
            }

            //inject args into the snippet parms
            if( args ){
                args.each( function(arg){
                    if(snip.parms.length > 1){
                        s = s.replace( snip.parms.shift(), arg );
                    }
                });
                //console.log("inject args: "+s+" "+snip.parms);
            }

            //inject selected text into first snippet parm
            if( !caret.thin && (snip.parms[1] /*length>1*/) ){
                s = s.replace( snip.parms.shift(), txta.getSelection() );
                //console.log("inject selection: "+s+" "+snip.parms);
            }

            //now insert the snippet text
            txta.setSelection( s );

            if( !snip.parms.length/*length==0*/ ){

                //when no selection, move caret after the inserted snippet,
                //otherwise leave the selection unchanged
                if( caret.thin ){ txta.setSelectionRange( caret.start + s.length ); }
                //console.log("action:: should we clear this ? " + self.hasContext() );
                self.clearContext();

            } else {

                //this snippet has one or more parameters
                //store the active snip and process the next {parameter}
                //checkme !!
                if( !self.hasContext() ){ self.setContext( snip ); }
                caret = txta.getSelectionRange(); //update new caret
                self.nextAction(txta, caret);

            }

        }

    },

    /*
    Function: toggleSnip
        Toggle the prefix and suffix of a snippet.
        Eg. toggle between {{__text__}} and {{text}}.
        The selection will be matched against the snippet.

    Precondition:
        - the selection is not empty (caret.thin = false)
        - the snippet has exatly one {parameter}

    Arguments:
        txta - Textarea object
        snip - Snippet object
        caret - Caret object {start, end, thin}

    Returns:
        - (string) replacement string for the selection.
            By default, returns snip.text
        - the snip.parms will be set to [] is toggle was executed successfully
        Eventually the selection will be extended if the
        prefix and suffix were just outside the selection.
    */
    toggleSnip: function(txta, snip, caret){

        var s = snip.text,
            //get the first and last textual parts of the snippet
            arr = s.trim().split( snip.parms[0] ),
            fst = arr[0],
            lst = arr[1],
            re = new RegExp( '^\\s*' + fst.trim().escapeRegExp() + '\\s*(.*)\\s*' + lst.trim().escapeRegExp() + '\\s*$' );

        if( (fst+lst)!='' ){

            s = txta.getSelection();
            snip.parms = [];

            // if pfx & sfx (with optional whitespace) are matched: remove them
            if( s.test(re) ){

                s = s.replace( re, '$1' );

            // if pfx/sfx are matched just outside the selection: extend selection
            } else if( txta.getFromStart().test(fst+'$') && txta.getTillEnd().test('^'+lst) ){

                txta.setSelectionRange(caret.start-fst.length, caret.end+lst.length);

            // otherwise, insert snippet and set caret between pfx and sfx
            } else {

                txta.setSelection( fst+lst ).setSelectionRange( caret.start + fst.length );
            }
        }
        return s;
    },

    /*
    Method: suggest
        Suggestion snippets are dialog-boxes appearing as you type.
        When clicking items in the suggest dialogs, content is inserted
        in the textarea.

    */
    suggest: function(){

        var self = this,
            txta = self.textarea,
            caret = txta.getSelectionRange(),
            fromStart = txta.getFromStart(),
            suggestions = self.suggestions,
            cmd, suggest, snip;

        for( cmd in suggestions ){

            snip = suggestions[cmd];
            suggest = snip.suggest(txta, caret);

            if( suggest /*&& self.inScope(snip, fromStart)*/ ){

                if(!suggest.tail) suggest.tail = 0; //ensure default value

                //console.log( "Suggest: "+ cmd + " [" + JSON.encode(suggest)+"]" );
                self.setContext( snip, suggest );
                return self.commands.action(cmd, suggest.match);

            }
        }

        //if you got here, no suggestions
        this.clearContext();

    },

    /*
    Method: suggestAction
        <todo>
        suggest = { start: start-position , match:'string', tail: length }
    */
    suggestAction: function( cmd, value ){

        var self = this,
            txta = self.textarea,
            suggest = self.context.suggest,
            end = suggest.start + suggest.match.length + suggest.tail;


        //console.log('SuggestAction: '+ cmd+' (' +value + ') [' + JSON.encode(suggest) + ']');

        //set the selection to the replaceable text, and inject the new value
        txta.setSelectionRange( suggest.start, end ).setSelection( value );

        //if tail, set the selection on the tail --why ??
        if( suggest.tail>0 ){
            txta.setSelectionRange( end - suggest.tail, txta.getSelectionRange().end );
        }

        self.clearContext();
        return self.suggest();

    },

    /*
    Function: nextAction
        Process the next ''{parameter}'' of the active snippet as you tab along
        or after you clicked a button or closed a dialog.

    Arguments:
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    nextAction: function(txta, caret){

        var self = this,
            snip = self.context.snip,
            parms = snip.parms,
            dialog,
            pos;

        while( parms[0] /*.length > 0*/ ){

            dialog = parms.shift();
            pos = txta.getValue().indexOf(dialog, caret.start);

            //console.log("next action: "+dialog+ " pos:" + pos + " parms: "+parms+" caret:"+caret.start);

            //found the next {dialog} or possibly the end of the snippet
            if( (dialog !='') && (pos > -1) ){

                if( parms[0] /*.length > 0*/ ){

                    // select the next {dialog}
                    txta.setSelectionRange( pos, pos + dialog.length );

                    //invoke the new dialog
                    //console.log('next action: invoke '+dialog+" "+snip[dialog])
                    self.commands.action( dialog, snip[dialog] );

                    //remember every selected snippet dialog
                    self.undoredo.onChange();

                    return; // and retain the context snip for subsequent {dialogs}

                } else {

                    // no more {dialogs}, move the caret after the end of the snippet
                    txta.setSelectionRange( pos + dialog.length );

                }
            }
        }

        self.clearContext();
    },

    /*
    Function: getState
        Get the current state of the SnipEditor which consist of
        the content and selection of the textarea.
        It implements the ''Undoable'' interface called from the
        [UndoRedo] class.
    */
    getState: function(){

        var txta = this.textarea,
            el = $(txta);

        return {
            main: this.mainarea.value,
            value: el.get('value'),
            cursor: txta.getSelectionRange(),
            scrollTop: el.scrollTop,
            scrollLeft: el.scrollLeft
        };
    },

    /*
    Function: putState
        Set a state of the SnipEditor.
        This works in conjunction with the [UndoRedo] class.
    Argument:
        o - object originally created by the getState funcion
    */
    putState: function(o){

        var self = this,
            txta = self.textarea,
            el = $(txta);

        self.clearContext();
        self.mainarea.value = o.main;
        el.value = o.value;
        el.scrollTop = o.scrollTop;
        el.scrollLeft = o.scrollLeft;
        txta.setSelectionRange( o.cursor.start, o.cursor.end )
            .fireEvent('change');
    }

});


/*
Class: SnipEditor.Commands
    This class preprocesses all command triggers, such as
    suggestion commands, button clicks or tab-completion commands.

    It will make sure that only one dialog is open at the same time.
    It intializes and caches the dialogs, handles the show/hide/toggle
    of dialogs, and passes action events back to the SnipEditor.

    Dialogs can be opened by means of
    - external command triggers: exec
    - click events on command buttons (.cmd.pop)
    - suggestion trigger (exec ???)

    FYI - all DIALOGs are created as descendants of the Dialog class.
    - Dialog : floating dialog panel
        - FormDialog : predef content with open/close handlers ???
        - Dialog.Selection : selectable list of items
            - Dialog.Font : selection list of font items
            - Dialog.Section : selection list of textarea sections
        - Dialog.Chars : selection matrix of special character entities
        - Dialog.Color : color-wheel
        - Dialog.Find : find and replace dialog

Options:
    container: DOM element  => contains commands(.cmd) and dialogs(.pop)
    dialogs - predefined set of dialog initialisators
    // **event handlers**
    onOpen - invoked after opening a DIALOG
    onClose - invoked after closing a DIALOG
    onAction - action call-back action(cmd,arguments)

Properties
    - buttons : collection of DOM-elements with click handlers to either
        action() or toggle()
    - dialogs : collection of dialog definitions [Dialog-class, {dialog parameters}]

DOM structure:
    <div class="cmd tICON"><i>action command</i></div>
    <div class="cmd pop tICON"><i>dialog</i></div>

    <div class="dialog fixed"> ... dialog content </div>
*/
SnipEditor.Commands = new Class({

    Implements: [Events, Options],

    options: {
        //onAction:function()...s
        //container: container DOM element, contains .cmd and .pop dialogs
        btns:'.cmd', //toolbar buttons
        dlgs:'.pop',
        dialogs:{}
    },
    btns: {},
    dlgs: {},
    allDlgs: {},

    initialize: function( options ){

        var self = this;
        options = self.setOptions(options).options;

        options.container.getElements(options.btns).each( function(el){
            var cmd = self.getCmd(el), dlg=$(cmd);
            self.btns[cmd] = el;

            //match the element against the dlgs css selector and check if a dialog DOM element exists
            if( el.match(options.dlgs) && dlg ){
                //console.log("Predefined DOM dialog "+cmd);
                //self.addDialogs({cmd:dlg});
                self.addDialogs( [dlg].associate([cmd]) );
                //options.dialogs[cmd]=dlg;
            }
            el.addEvent('click', function(){ self.click(cmd); } );
        });

        self.addDialogs(options.dialogs);
    },

    getCmd: function( el ){
        //console.log( el.get('id') || el.get('name') || el.get('text') );
        return el.get('id') || el.get('name') || el.get('text');
    },

    /*
    Funciton: addDialog
        Add a new dialog.
        The dialog is only created when invoking the command.
        This happens through a button click or through the action() method.

    Arguments:
        dialogs: {cmd1:dialog, cmd2:dialog-def...}
        (dialog-def : array[Dialog-Class, {dialog parameters}]
    */
    addDialogs: function(dialogs, relativeTo){

        Object.each(dialogs, function( dialog, cmd ){

            //additional dialogs will overwrite any existing
            if(this.allDlgs && this.allDlgs[cmd]) console.log("AddDialogs - warning: double registration of => " + cmd);

            this.allDlgs[cmd] = dialog;  //repo of all dialogs
            if( instanceOf(dialog, Dialog) ){ this.attachDialog(cmd,dialog); }
            if( relativeTo ){ this.btns[cmd] = relativeTo; }

        },this);
        //console.log('allDialogs: '+Object.keys(this.allDlgs) );
    },

    attachDialog: function(cmd,dialog){

        var    self = this,
            actionHdl = function(v){ self.fireEvent('action', [cmd,v]); };

        //console.log('attachDialog: '+cmd);

        return self.dlgs[cmd] = dialog.addEvents({
            onOpen: self.openDialog.bind(self, cmd),
            onClose: self.closeDialog.bind(self, cmd),
            onAction: actionHdl,
            onDrag: actionHdl
        });
    },

    click: function( cmd ){

        var d = this.dlgs[cmd];
        d ? d.toggle() : this.action( cmd );

    },

    /*
    Function: action
        Action handler for a simple command. Pass the 'action' event
        up to the SnipEditor.

    Arguments:
        cmd : command name
        value : (optional) initial value of the dialog
    */
    action: function( cmd, value ){

        var self = this, btn = self.btns[cmd], dlg = self.dlgs[cmd];

        //console.log("Commands.action "+cmd+" value:"+value+" btn="+btn+ " dlg="+dlg);

        if( btn && $(btn).hasClass('disabled') ){

            //nothing to do here

        //} else if( dlg ){
        } else if( self.allDlgs[cmd] ){

            if( !dlg ){ dlg = self.createDialog(cmd) }
            if( value ){ dlg.setValue( value ); }
            dlg.show();

        } else {

            if( btn ){ $(btn).addClass('active'); }
            self.fireEvent('action', [cmd, value] );
            if( btn ){ $(btn).removeClass('active'); }

        }

    },

    /*
    Function: createDialog
        Create a new dialog.
        The name of the cmd determines the type (or class) of Dialog to be created
        - cmd: Dialog[cmd] (eg cmd='font' : Dialog.Font)
        - the name of the dialog equals the DOM ID of a predefined HTML dialog

        - DOM Element: predefined DOM dialog
        - [ Dialog-class, { dialog parameters } ]
        - { dialog parameters } : the cmd determines the type of Dialog to create
        - "string" : create a selection dialog Dialog.Selection


    Arguments
        cmd - xx

        The open/close handlers will make sure only one dialog is open at the
        same time. The open dialog is stored in {{this.activeCmd}}.

        The key events 'action', 'drag', 'open' and 'close' are propagated upwards.

    Returns:
        The created dialog, which is also stored in this.dlgs[] repository.

    */
    createDialog: function( cmd ){

        var self = this,
            dlg,
            btn = self.btns[cmd],
            factory = Function.from( self.allDlgs[cmd] )(),
            factoryType = typeOf(factory);

        //console.log('Commands.createDialog() '+cmd+' '+ ' btn='+btn +" "+factoryType);

        //expect factory to be [Dialog class,  {dialog options object}]
        if( factoryType != 'array' || factory.length != 2 ){

            factory = ( factoryType == 'element' ) ?
                [Dialog, {dialog:factory}] : [Dialog.Selection, {body:factory}];
        }

        dlg = new factory[0]( Object.append( factory[1],{
            cssClass: 'dialog float',
            autoClose: false, //suggestion dialog should not be autoclosed
            relativeTo: btn //button or textareaa
            //draggable: true
        }) );

        //Make sure that this.dlgs[cmd] gets initialized prior to calling show()
        return self.attachDialog(cmd,dlg);
    },

    /*
    Function: openDialog
        Opens a dialog. If already another dialog was open, that one will first be closed.
        When a toolbar button exists, it will get the css class '.active'.

        Note: make sure that this.dlgs[cmd] is initialized prior to calling show() !

    Argument:
        cmd - dialog to be opened
        preOpen - ...
    */
    openDialog: function(cmd, dialog){

        var self = this, activeCmd = self.activeCmd, tmp;

        //console.log('Commands.openDialog() ' + cmd + ' ' + self.activeCmd );

        if( activeCmd && (activeCmd != cmd) ){
            tmp = self.dlgs[activeCmd]; if(tmp){ tmp.hide(); }
            //toobar button will be deactivated by closeDialog()
        }

        self.activeCmd = cmd;
        tmp = self.btns[cmd]; if(tmp){ $(tmp).addClass('active'); }

        self.fireEvent('open', cmd, dialog);
    },

    /*
    Function: closeDialog

    Arguments:
        cmd - (mandatory) dialog to be closed
    */
    closeDialog: function(cmd, dialog){

        var self = this, btn = self.btns[cmd];

        //console.log('Commands.closeDialog() ' + cmd + ' ' + self.activeCmd )

        if( cmd == self.activeCmd ){ self.activeCmd = null; }
        if( btn ){ $(btn).removeClass('active'); }

        self.fireEvent('close', cmd, dialog);
    },

    /*
    Function: close
        Close the active dialog, if any.
    */
    close: function(){

        var activeCmd = this.activeCmd;
        if(activeCmd){ this.dlgs[activeCmd].hide(); }

    }

});



/*
Class: SnipEditor.Sections
    This dialog displays the list of page sections.
    A page section includes the header

    (all) - allows to select all sections (auto generated)
    start-of-page - only present when first section starts on an offset > 0
    section1..n - section titles, with indentation level depending on their weight

    The set of sections is generated by the parseSections() callback handler.
    This parser returns an array of section 'descriptors':
>    [ {title:text, start:char-offset, indent:indentation-level}, ... ]

    Clicking an entry triggers the updateSections() callback handler.
    FIXME: why not fire an onAction event (similar to other dialogs)

Depends:
    Dialog.Selection

*/
SnipEditor.Sections = new Class({
    Extends:Dialog.Selection,
    options: {
        //main:textarea
        //work:textarea
        //selected: selected item: all,s-1,s0..sn
        //parser: function
        all: "( all )".localize(),
        startOfPage: "Start of Page".localize()
    },

    initialize: function(options){

        var self = this,
            onChange=self.onChange.bind(self);

        options.cssClass = 'dialog float sections'; //CHECK: <dialog float> is generic dialog cssclass
        self.parent(options);

        //console.log(self.options);

        self.parse();
        self.action();

        //can be hooked up by the SnipEditor.Commands
        /*self.dialogDefinition = [Dialog.Selection, {
            selected: self.options.selected,
            onOpen: self.onOpen,
            onAction: self.onAction
        }];*/

        //FIXME !! keyup
        self.options.work.addEvents({ change:onChange, keyup:onChange });

    },

    /*
    Function: parse
        Invoke the external parser on the contents of the main textarea.
        This external parser should return an array with an entry for each section:
        [ {title:text, start:char-offset, depth:nesting level}, ... ]

        >        0 : ( all )                       => title=All => cursor=NaN
        >        1 : start-of-page (if applicable) => title=s-1 => cursor=-1
        >        2..n : page sections              => title=s0..sn => cursor=0..n

    */
    parse: function(){
        this.sections = this.options.parser( this.options.main.value );
    },

    /*
    Function: onOpen
        UPDATE/RFEFRESH the textarea section dialog.
    */
    //onOpen: function( dialog ){
    show: function( ){

        //console.log("Sections: show");
        var options = this.options,
            data = { all: options.all },
            sections = this.sections;

        if( sections[0] /*.length > 0*/ ){
            if( sections[0].start>0 ){ data['s-1'] = options.startOfPage }
            sections.each( function(item, idx){ data['s'+idx] = item.title.trunc(36); });
        }

        //dialog.setBody( data ).getItems().each(function(el){
        this.setBody( data ).getItems().each(function(el){
            var t = el.title.slice(1).toInt();
            if( t>=0 ){ el.setStyle('padding-left',(.5+sections[t].depth)+'em'); }
        });

        this.parent(); //invoke parent's show
    },

    /*
    Function: onChange
        Make sure that changes to the work textarea are propagated to the main textarea.
        This functions handles the correct insertion of the changed section into the main
        textarea.
    */
    onChange: function(){

        //console.log("Sections: onChange");
        var self = this,
            main = self.options.main,
            work = self.options.work.value,
            sections = self.sections,
            s, linefeed;

        s = main.value;
        //insert \n to ensure the next section always starts on a new line.
        linefeed = (work.slice(-1) != '\n')  ? '\n' : '';

        //console.log('change txta: from='+sections.begin+ ' end='+sections.end);
        main.value = s.slice(0, self.begin) + work + linefeed + s.slice(self.end);
        self.end = self.begin + work.length;

        self.parse();
    },

    /*
    Function: onAction
        This function copies the selected section from the main to the work textarea.
        It is invoked at initialization and through the dialog onAction click handler.

    Arguments:
        cursor - index of selected section: all, -1, 0..n
    */
    //onAction:function( item ){

    /*
    setValue: function(value){
    },
    action: function(item){
        var value = item.get('title');
        this.setValue(value).parent(value);
    },
    */
    action:function( item ){

        var self = this,
            main = self.options.main.value,
            sections = self.sections,
            begin = 0,
            end = main.length;

        if( item ){

            item = item.replace(/^s/,'').toInt();

            if( item == -1 ){

                //show the Start Of Page, prior to the first real section
                end = sections[0].start;

            } else if(item >= 0  && sections[item] /*item < sections.length*/ ){

                begin = sections[item].start;
                //if( item+1 < sections.length ){ end = sections[item+1].start; }
                if( sections[item+1] ){ end = sections[item+1].start; }

            }
        }

        self.options.work.value = main.slice(begin, end);
        self.begin = begin;
        self.end = end;
        //console.log("Section: onAction " + item + " "+self.begin+" "+self.end);

        //checkme
        if( item ) self.parent(item);
    }
});


/*
Class: Textarea
    The textarea class enriches a TEXTAREA element, and provides cross browser
    support to handle text selection: get and set the selected text,
    changing the selection, etc.
    It also provide support to retrieve and validate the caret/cursor position.

Example:
    (start code)
    <script>
        var txta = new Textarea( "mainTextarea" );
    </script>
    (end)
*/
var Textarea = new Class({

    Implements: [Options,Events],

    //options: { onChange:function(e){} );

    initialize: function(el,options){

        var self = this,
            txta = self.ta = $(el),

            lastValue,
            lastLength = -1,
            changeFn = function(e){
                var v = txta.value;
                if( v.length != lastLength || v !== lastValue ){
                    self.fireEvent('change', e);
                    lastLength = v.length;
                    lastValue = v;
                }
            };

        self.setOptions(options);

        txta.addEvents({ change:changeFn, keyup:changeFn });

        //Create shadow div to support pixel measurement of the caret in the textarea
        //self.taShadow = new Element('div',{
        //    styles: { position:'absolute', visibility:'hidden', overflow:'auto'/*,top:0, left:0, zIndex:1, white-space:pre-wrap*/ }
        //})
        self.taShadow = new Element('div[style=position:absolute;visibility:hidden;overflow:auto]')
          .inject(txta,'before')
          .setStyles( txta.getStyles(
            'font-family0font-size0line-height0text-indent0padding-top0padding-right0padding-bottom0padding-left0border-left-width0border-right-width0border-left-style0border-right-style0white-space0word-wrap'
            .split(0)
        ));

        return this;
    },

    /*
    Function: toElement
        Return the DOM textarea element.
        This allows the dollar function to return
        the element when passed an instance of the class. (mootools 1.2.x)

    Example:
    >    var txta = new Textarea('textarea-element');
    >    $('textarea-element') == txta.toElement();
    >    $('textarea-element') == $(txta); //mootools 1.2.x
    */
    toElement: function(){
        return this.ta;
    },

    /*
    Function: getValue
        Returns the value (text content) of the textarea.
    */
    getValue: function(){
        return this.ta.value;
    },
    /*
    Function: slice
        Mimics the string slice function on the value (text content) of the textarea.
    Arguments:
        Ref. javascript slice function
    */
    slice: function(start,end){
        return this.ta.value.slice(start,end);
    },


    /*
    Function: getFromStart
        Returns the first not selected part of the textarea, till the start of the selection.
    */
    getFromStart: function(){
        return this.slice( 0, this.getSelectionRange().start );
    },

    /*
    Function: getTillEnd
        Returns the last not selected part of the textarea, starting from the end of the selection.
    */
    getTillEnd: function(){
        return this.slice( this.getSelectionRange().end );
    },

    /*
    Function: getSelection
        Returns the selected text as a string

    Note:
        IE fixme: this may return any selection, not only selected text in this textarea
            //if(Browser.Engine.trident) return document.selection.createRange().text;
    */
    getSelection: function(){

        var cur = this.getSelectionRange();
        return this.slice(cur.start, cur.end);

    },

    /*
    Function: setSelectionRange
        Selects the selection range of the textarea from start to end

    Arguments:
        start - start position of the selection
        end - (optional) end position of the seletion (default == start)

    Returns:
        Textarea object
    */
    setSelectionRange: function(start, end){

        var txta = this.ta,
            value,diff,range;

        if(!end){ end = start; }

        if( txta.setSelectionRange ){

            txta.setSelectionRange(start, end);

        } else {

            value = txta.value;
            diff = value.slice(start, end - start).replace(/\r/g, '').length;
            start = value.slice(0, start).replace(/\r/g, '').length;

            range = txta.createTextRange();
            range.collapse(1 /*true*/);
            range.moveEnd('character', start + diff);
            range.moveStart('character', start);
            range.select();
            //textarea.scrollTop = scrollPosition;
            //textarea.focus();

        }
        return this;
    },

    /*
    Function: getSelectionRange
        Returns an object describing the textarea selection range.

    Returns:
        {{ { 'start':number, 'end':number, 'thin':boolean } }}
        start - coordinate of the selection
        end - coordinate of the selection
        thin - boolean indicates whether selection is empty (start==end)
    */

/* ffs
    getIERanges: function(){
        this.ta.focus();
        var txta = this.ta,
            range = document.selection.createRange(),
            re = this.createTextRange(),
            dupe = re.duplicate();
        re.moveToBookmark(range.getBookmark());
        dupe.setEndPoint('EndToStart', re);
        return { start: dupe.text.length, end: dupe.text.length + range.text.length, length: range.text.length, text: range.text };
    },
*/
    getSelectionRange: function(){

        var txta = this.ta,
            caret = { start: 0, end: 0 /*, thin: true*/ },
            range, dup, value, offset;

        if( txta.selectionStart!=null ){

            caret = { start: txta.selectionStart, end: txta.selectionEnd };

        } else {

            range = document.selection.createRange();
            //if (!range || range.parentElement() != txta){ return caret; }
            if ( range && range.parentElement() == txta ){
                dup = range.duplicate();
                value = txta.value;
                offset = value.length - value.match(/[\n\r]*$/)[0].length;

                dup.moveToElementText(txta);
                dup.setEndPoint('StartToEnd', range);
                caret.end = offset - dup.text.length;
                dup.setEndPoint('StartToStart', range);
                caret.start = offset - dup.text.length;
            }
        }

        caret.thin = (caret.start==caret.end);

        return caret;
    },

    /*
    Function: setSelection
        Replaces the selection with a new value (concatenation of arguments).
        On return, the selection is set to the replaced text string.

    Arguments:
        string - string to be inserted in the textarea.
            If multiple arguments are passed, all strings will be concatenated.

    Returns:
        Textarea object, with a new selection

    Example:
        > txta.setSelection("new", " value"); //replace selection by 'new value'
    */
    setSelection: function(){

        var value = Array.from(arguments).join('').replace(/\r/g, ''),
            txta = this.ta,
            scrollTop = txta.scrollTop, //cache top
            start,end,v,range;

        if( txta.selectionStart!=null ){

            start = txta.selectionStart;
            end = txta.selectionEnd;
            v = txta.value;
            //txta.value = v.substr(0, start) + value + v.substr(end);
            txta.value = v.slice(0, start) + value + v.substr(end);
            txta.selectionStart = start;
            txta.selectionEnd = start + value.length;

        } else {

            txta.focus();
            range = document.selection.createRange();
            range.text = value;
            range.collapse(1 /*true*/);
            range.moveStart("character", -value.length);
            range.select();

        }
        txta.focus();
        txta.scrollTop = scrollTop;
        txta.fireEvent('change');
        return this;

    },

    /*
    Function: insertAfter
        Inserts the arguments after the selection, and puts caret after inserted value

    Arguments:
        string( one or more) - string to be inserted in the textarea.

    Returns:
        Textarea object
    */
    insertAfter: function(){

        var value = Array.from(arguments).join('');

        return this.setSelection( value )
            .setSelectionRange( this.getSelectionRange().start + value.length );

    },

    /*
    Function: isCaretAtStartOfLine
        Returns boolean indicating whether caret is at the start of a line.
    */
    isCaretAtStartOfLine: function(){

        var i = this.getSelectionRange().start;
        return( (i<1) || ( this.ta.value.charAt( i-1 ).test( /[\n\r]/ ) ) );

    },

    /*
    Function: getCoordinates
        Returns the absolute coordinates (px) of the character at a certain offset in the textarea.
        Default returns pixel coordinates of the selection.

    Credits:
        Inspired by http://github.com/sergeche/tx-content-assist.

    Arguments:
        offset - character index
            If omitted, the pixel position of the caret is returned.

    Returns:
        {{ { top, left, width, height, right, bottom } }}
     */
    getCoordinates: function( offset ){

        var txta = this.ta,
            taShadow = this.taShadow,
            delta = 0,
            el,css,style,t,l,w,h;

        //prepare taShadow
        css = txta.getStyles(['padding-left','padding-right','border-left-width','border-right-width']);
        for(style in css){ delta +=css[style].toInt() }

        //default offset is the position of the caret
        if( !offset ){ offset = this.getSelectionRange().end; }

        el = taShadow.set({
            styles: {
                width: txta.offsetWidth - delta,
                height: txta.getStyle('height')  //ensure proper handling of scrollbars - if any
            },
            //FIXME: should we put the full selection inside the <i></i> bracket ? (iso a single char)
            html: txta.value.slice(0, offset) + '<i>A</i>' + txta.value.slice(offset+1)
        }).getElement('i');

        t = txta.offsetTop + el.offsetTop - txta.scrollTop;
        l = txta.offsetLeft + el.offsetLeft - txta.scrollLeft;
        w = el.offsetWidth;
        h = el.offsetHeight;
        return {top:t, left:l, width:w, height:h, right:l+w, bottom:t+h}

    }

});


/*
Class: UndoRedo
    The UndoRedo class implements a simple undo/redo stack to save and restore
    the state of an 'undo-able' object.
    The object needs to provide a {{getState()}} and a {{putState(obj)}} methods.
    Whenever the object changes, it should call the UndoRedo onChange() handler.
    Optionally, event-handlers can be attached for undo() and redo() functions.

Arguments:
    obj - the undo-able object
    options - optional, see options below

Options:
    maxundo - integer , maximal size of the undo and redo stack (default 20)
    redo - (optional) DOM element, will get a click handler to the redo() function
    undo - (optional) DOM element, will get a click handler to the undo() function

Example:
    (start code)
    <script>
        var undoredo = new UndoRedo(this, {
            redoElement:'redoID',
            undoElement:'undoID'
        });

        //when a change occurs on the calling object which needs to be persisted
        undoredo.onChange( );
    </script>
    (end)
*/
var UndoRedo = new Class({

    Implements: Options,

    options: {
        //redo : redo button selector
        //undo : undo button selector
        maxundo:40
    },
    initialize: function(obj, options){

        var self = this,
            btn = this.btn = {};

        self.setOptions(options);
        self.obj = obj;
        self.redo = [];
        self.undo = [];

        btn.redo = $$(self.options.redo);
        btn.undo = $$(self.options.undo);

        self.btnStyle();
    },

    /*
    Function: onChange
        Call the onChange function to persist the current state of the undo-able object.
        The UndoRedo class will call the {{obj.getState()}} to retrieve the state info.

    Arguments:
        state - (optional) state object to be persisted. If not present,
            the state will be retrieved via a call to the {{obj.getState()}} function.
    */
    onChange: function(state){

        var self = this;

        self.undo.push( state || self.obj.getState() );
        self.redo = [];
        //if(self.undo.length > self.options.maxundo){ self.undo.shift(); }
        if(self.undo[self.options.maxundo]){ self.undo.shift(); }
        self.btnStyle();

    },

    /*
    Function: onUndo
        Click event-handler to recall the state of the object
    */
    onUndo: function(e){

        var self = this;

        if(e){ e.stop(); }

        //if(self.undo.length > 0){
        if(self.undo[Ø] /*length>0*/){
            self.redo.push( self.obj.getState() );
            self.obj.putState( self.undo.pop() );
        }
        self.btnStyle();

    },

    /*
    Function: onRedo
        Click event-handler to recall the state of the object after a previous undo action.
        The state will be reset by means of the {{obj.putState()}} method
    */
    onRedo: function(e){

        var self = this;

        if(e){ e.stop(); }

        //if(self.redo.length > 0){
        if(self.redo[0] /*.length > 0*/){
            self.undo.push( self.obj.getState() );
            self.obj.putState( self.redo.pop() );
        }
        self.btnStyle();

    },

    /*
    Function: btnStyle
        Helper function to change the css style of the undo/redo buttons.
    */
    btnStyle: function(){

        var self = this, btn = self.btn;

        if(btn.undo){ btn.undo.ifClass( !self.undo.length /*length==0*/, 'disabled'); }
        if(btn.redo){ btn.redo.ifClass( !self.redo.length /*length==0*/, 'disabled'); }

    }

});
