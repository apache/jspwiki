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
/*



*/

/*
DirectSnippet definitions for JSPWiki, aka ''smartpairs''.
These snippets are directly expanded on keypress.
*/
Wiki.DirectSnips = {

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
};

/*
    Function: tabSnippets
    
        Definitions for the JSPWiki editor commands.

        Following commands are predefined by the snipe editor:
        - find : toggles the find and replace dialog
        - sections : toggle sections dropdown dialog, which allows to switch
            between certain sections of the document or the whole document
        - undo : undo last command, set the editor to the previous stable state
        - redo : revert last undo command

        A command consists of triggers, attributes, snippets, events and dialogs.

        Triggers : 
        ========
        click events, suggestion dialogs, TAB-completion and Ctrl-keys.

        Click events are attached to DOM elements with a .cmd css-class.
        If the DOM element also contains a .pop css-class, a dialog will be opened.

        TAB-completion can be turned on/off via the 'tabcompletion' flag.

        The 'keyup' event can trigger a suggestion dialog:
        - the suggest(txta,caret) function validates the suggestion context
          It returns true/false and can modify the snippet with
             - snip.start : begin offset of the matched prefix
             - snip.match : matched prefix (string)
             - snip.tail: (optional) replaceable tail

        Attributes :
        ==========
        - initialize: function(cmd, snip) called once at initialization
        - key: shortcut key  (ctrl-key)
        - scope: set to TRUE when the cmd is valid
        - nscope: set to TRUE when the cmd is not valid
        - cmdId: wysiwyg mode only (commandIdentifier)
        
        Snippet :
        =======
        The snippet contains the inserted or replaced text.
        - static snippet: "some string"
        - snippet with parameters in {} brackets: "some {dialog1} string"
          A {.} will be replaced by the selected text.
          A {dialog-1} opens a dialog, and inserts the returned info (eg color, selection...)
        - dynamic snippet: javascript function.
          Example:
              snippet: function(){
                  this.dialogs.exec( dialog-name, ...
                      onChange: function(value){  }
                  )
              }

        Event :
        =====
        Fires an event back to the invoking Object (Wiki.Edit in our case)
        Example:
            smartpairs: { event: 'config' }

        Dialogs :
        =======
        (btw -- you do use unique names, do you?)
        - <dialog-name>: [ Dialog.SubClass, {dialog-parameters, event-handlers} ]
        - <dialog-name>: "dialog initialization string"
          This is a short notation for Dialog.Selection, or..
          [Selection, "put here your dialog initialization string"]

        The Dialog Classes are subclass of Dialog. (eg. Dialog.Selection)


    Examples:

     acl: {
         nscope: { "[{" : "}]" },
         snippet: "[\\{ ALLOW {permission} {principal(s)}  }]"

         permission: "view|edit|delete",
         "principals(s)": [Dialog.Selection, {
             onOpen: function(){ this.setBody( AJAX-request list of principals ); }
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
                AJAX-retrieval of link suggestions
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

Wiki.Snips = {

        find: {
            key: "f"
            //predefined find dialog triggered via Ctrl-f or a toolbar 'find' button
        },

        //sections:
        //predefined section dialog triggered via a toolbar 'sections' button
        //TODO: turn it into a suggestion menu for header lines

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
        previewcolumn: { event: 'config' },

        br: {
            key: "shift+enter",
            snippet: "\\\\\n"
        },

        hr: "\n----\n",


        h: {
            xxxsuggest: function(txta,caret){
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
                //colorImage:'./test-dialog-assets/circle-256.png'
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
                var c, result = txta.slice(0,caret.end).match(/%%[\da-zA-Z(\-\_:#;)]*$/);

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
                "text-success":"text-success",
                "text-information":"text-information",
                "text-warning":"text-warning",
                "text-error":"text-error",
                success:"success",
                information:"information",
                warning:"warning",
                error:"error",
                commentbox:"commentbox",
                quote:"quoted paragraph",
                sub:"sub-script<span class='sub'>2</span>",
                sup:"super-script<span class='sup'>2</span>",
                strike:"<span class='strike'>strikethrough</span>",
                pretify:"prettify code block",
                reflection:"image reflection"
                //xflow:"wide content with scroll bars"
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


//console.log("****"+dialog.getValue()+"****", Wiki.PageName)
                    var dialog = this, 
                        key = dialog.getValue();
                        
                    if( !key || (key.trim()=='')) key = Wiki.PageName + '/';

                    //console.log('json lookup for '+key);
             	   	Wiki.ajaxJsonCall("/search/suggestions",[key,30], function(result) {
                       if( result && result.size()>0 ){
                           dialog.setBody( result );
                       } else {
                           dialog.hide();
                       }
             	   	});
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
                "TableOfContents title='Page contents' numbered='true' prefix='Chap. '":"Table Of Contents (toc)",
                "If name='value' page='pagename' exists='true' contains='regexp'\n\nbody\n":"Test a page variable",
                "SET alias='{pagename}'":"Make a Page Alias",
                "SET name='value'":"Set a page variable",
                "$varname":"Get a page variable",
                "InsertPage page='pagename'":"Insert Page",
                "CurrentTimePlugin format='yyyy mmm-dd'":"Current Time",
                "Search query='Janne' max='10'":"Search query",
                "ReferredPagesPlugin page='pagename' type='local|external|attachment' depth='1..8' include='regexp' exclude='regexp'":"Incoming Links (aka referred pages)",
                "ReferringPagesPlugin page='pagename' separator=',' include='regexp' exclude='regexp'":"Outgoing Links (aka referring pages)",
                "WeblogPlugin page='pagename' startDate='300604' days='30' maxEntries='30' allowComments='false'":"Display weblog posts",
                "WeblogEntryPlugin":"New weblog entry"
            }
        },

        tab: {
            nScope: {
                "%%(":")",
                "%%tabbedSection":"/%"
            },
            snippet:"%%tabbedSection \n%%tab-{tabTitle1}\n{tab content 1}\n/%\n%%tab-{tabTitle2}\n{tab content 2}\n/%\n/%\n "
        },

        toc: {
            nScope: { "[{":"}]" },
            snippet:"\n[\\{TableOfContents }]\n"
        },

        table: "\n||heading-1||heading-2\n| cell11   | cell12\n| cell21   | cell22\n",

        me: { alias: 'sign'},
        sign: function(){
            var name = Wiki.UserName || 'UserName';
            return "\\\\\n--" + name + ", "+ new Date().toISOString() + "\\\\\n";
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
        lorem: "This is is just some sample. Don’t even bother reading it; you will just waste your time. Why do you keep reading? Do I have to use Lorem Ipsum to stop you? OK, here goes: Lorem ipsum dolor sit amet, consectetur adipi sicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Still reading? Gosh, you’re impossible. I’ll stop here to spare you.",
        Lorem: { synonym: "lorem" }


}

