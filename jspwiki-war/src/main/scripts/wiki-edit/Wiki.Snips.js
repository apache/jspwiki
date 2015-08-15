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
Function: snippets

        Definitions for the JSPWiki editor commands.


        A command consists of triggers, attributes, snippets, events and dialogs.

        Following commands are predefined by the snipe editor:
        - find : toggles the find and replace dialog
        - sections : toggle sections dropdown dialog, which allows to switch
            between certain sections of the document or the whole document
        - undo : undo last command, set the editor to the previous stable state
        - redo : revert last undo command


Snippet Triggers :

        Triggers can be click events, TAB-key, suggestion dialogs and shortcut-keys (ctrl+/meta+).

        CLICK event:
        Click events are attached to DOM elements with a {{data-cmd="cmd"}} attribute.

        TAB key:
        Enter a command followed by the TAB key.
        TAB-completion can be turned on/off via the 'tabcompletion' flag.

        KEYUP event, may trigger a suggestion dialog:
        Suggestion dialogs are opened when the cursor is located 'inside' a command keyword.
        Match function determines a valid suggestion command.

        - the suggest(txta,caret) function validates the suggestion context
          It returns true/false and can modify the snippet with
             - snip.start : begin offset of the matched prefix
             - snip.match : matched prefix (string)
             - snip.tail: (optional) replaceable tail


Snippet Attributes :
        - initialize: function(cmd, snip){ return snippet } called once during initialisation
        - key: shortcut key  (ctrl-key or meta-key)
        - scope: returns TRUE when the command appears inside certain start and end pattern
        - nscope: set to TRUE when the command is not inside certain start and end pattern
        - cmdId: (wysiwyg mode only) corresponding commandIdentifier
        - synonym:

Snippet actions
        Text, Event, Dialog (will be opened prior to insertion of the text)

Snippet Text:
        The snippet text contains the text to be inserted or replaced.
        Add '\n' at the start or end of the snippet, if it need to appear on a new line.
        - a {.description} (start with dot) will be replaced by the selected text, if any
        - a {parameter} (not dot) will become the selected text AFTER insertion of the snippet
        The snippet text can also be replace by a javascript function, so any manipulation
        of the textarea is possible.

Snippet Event :

        Fires an event back to the invoking Object (Wiki.Edit in our case)
        Example:
            smartpairs: { event: 'config' }

Snippet dialog:

        Snippet dialog carry the name of the command.
        (btw -- you do use unique names, do you?)
        - <dialog-name>: [ Dialog.SubClass, {dialog-parameters, event-handlers} ]
        - <dialog-name>: "dialog initialization string"
          This is a short notation for Dialog.Selection, or..
          [Selection, "put here your dialog initialization string"]

        The Dialog Classes are subclass of Dialog. (eg. Dialog.Selection)


Examples:

    bold: '__{.bold}__'
    bold: { snippet: '__{.bold}__' }

    toc: { snippet: '\n[{TableOfContents}]\n' }

    newline: {
        key:'shift+enter',
        snippet: '\\\\\n'
    }
    br: { synonym: 'newline' }


    acl: {
        nscope: { "[{" : "}]" },
        snippet: "[{ALLOW {permission} {principal}  }]"
     },
     permission: {
        scope: { "[{ALLOW" : "}]" },
        suggest: 'ALLOW\s+(\w+)',
        permission: "view|edit|delete"  //selection dialog
     },
     principal: {
        scope: { "[{ALLOW" : "}]" },
        suggest: 'ALLOW\s+\w+\s+(\w+)',
        principals: [Dialog.Selection, {
             onOpen: function(){ this.setBody( AJAX-request list of principals ); }
        ]
     },

    link: {
        key:'l',
        scope: { '[':']' , '[':'\n' },
        nscope: { '[{':'\n', '[[','\n' },
        snippet: '[{.description} | {pagenameOrUrl} | linkAttributes ] ",
        commandIdentifier:'createlink'
    },
    linkDlg: {
        scope: { '[':']' , '[':'\n' },
        //match [link] or [link,  do not match [{, [[
        //match '[' + 'any char except \n, [, { or ]' at end of the string
        suggest: '|?([^\\n\\|\\]\\[\\{]+)',
        linkDlg: [Dialog.Link, {
            onOpen: function(){
                AJAX-retrieval of link suggestions
            }
         }]
        ****
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
        ****
    }
    linkAttributes: {
        scope: { '|':']'},
        suggest: ...,
        linkAttributes: 'class='', newpage ....'
    }

*/

Wiki.Snips = {

        // Snipe predefined commands
        find: { key: "f" },
        undo: { key: "z", event: "undo" },
        redo: { key: "y", event: "redo" },

        // Configuration commands
        smartpairs: { event: 'config' },
        livepreview: { event: 'config' },
        autosuggest: { event: 'config' },
        tabcompletion: { event: 'config' },
        previewcolumn: { event: 'config' },


        // Simple shortcuts
        br: {
            key: "shift+enter",
            snippet: "\\\\ "
        },
        hr: "\n----\n",
        lorem: "This is is just some sample. Don’t even bother reading it; you will just waste your time. Why do you keep reading? Do I have to use Lorem Ipsum to stop you? OK, here goes: Lorem ipsum dolor sit amet, consectetur adipi sicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Still reading? Gosh, you’re impossible. I’ll stop here to spare you.",
        Lorem: { synonym: "lorem" },


        // simple inline tab completion commands
        bold:   { key:'b', snippet: "__{bold}__ " },
        italic: { key:'i', snippet: "''{italic}'' " },

        mono:   "\\{\\{{monospaced text}}} ",
        sub:    "%%sub {subscript text}/% ",
        sup:    "%%sup {superscript text}/% ",
        strike: "%%strike {strikethrough text}/% ",

        // simple block tab completion commands
        quote:  "\n%%quote \n{.}\n/%\n",
        dl:     "\n;{term}:{definition-text} ",
        pre:    "\n\\{\\{\\{\n{some preformatted block}\n}}}\n",
        code:   "\n%%prettify \n\\{\\{\\{\n{/* some code block */}\n}}}\n/%\n",
        table:  "\n||heading-1||heading-2\n| cell11   | cell12\n| cell21   | cell22\n",
        //xflow:  "\n%%xflow\n{wide content}\n/%\n ",

        me: { alias: 'sign'},
        sign: function(){
            var name = Wiki.UserName || 'UserName';
            return "\n\\\\ &mdash;" + name + ", "+ new Date().toISOString() + "\\\\ \n";
        },

        date: function(k) {
            return new Date().toISOString()+' ';
            //return "[{Date value='" + d.toISOString() + "' }]"
            //return "[{Date " + d.toISOString() + " }]"
        },

        tabs: {
            nScope: {
                "%%(":")",
                "%%tabbedSection":"/%"
            },
            snippet:"%%tabbedSection \n%%tab-{tabTitle1}\n{tab content 1}\n/%\n%%tab-{tabTitle2}\n{tab content 2}\n/%\n/%\n "
        },



        img: "\n[{Image src='img.jpg' width='400px' height='300px' align='{left}' }]\n ",

        imgSrcDlg:{
            scope: { "[{Image":"}]" },
            suggest: { pfx:"src='([^']*)'?$", match: "^([^']*)" },
            imgSrcDlg: [ Dialog.Selection, {

                caption: "Image Source",
                onOpen: function( dialog ){

                    var //dialog = this,
                        key = dialog.getValue();

                    if( !key || (key.trim()=='') ){ key = Wiki.PageName + '/'; }

                    //console.log('json lookup for '+key);
             	   	//Wiki.ajaxJsonCall("/search/suggestions",[key,30], function(result) {
                    Wiki.jsonrpc("/search/suggestions", [key, 30], function( result ){

                        console.log('jsonrpc result', result );
                        if( result[1] /*length>1*/ ){

                            dialog.setBody( result );

                        } else {

                            dialog.hide();

                        }
                    });
                }
            }]
        },

/*
        imgAlignDlg: {
            scope: { "[{Image":"}]" },
            suggest: "align='\\w+'",
            imgAlignDlg: "left|center|right"
        },
*/

        font: {
            nScope: { "%%(":")", },
            snippet: "%%(font-family:{font};) body /% ",
        },
        fontDlg: {
            scope: { "%%(":")" },
            suggest: { pfx: "font-family:([^;\\)\\n\\r]*)$", match:"^([^;\\)\\n\\r]*)" },
            fontDlg: [Dialog.Font, {}]
        },

        color: "%%(color:{#000000}; background:#ffffff;) {some text} /%",

        colorDlg: {
            scope: { '%%(': ')' },
            suggest:"#[0-9a-fA-F]{0,6}",
            colorDlg: [ Dialog.Color , {} ]
         },

        symbol: { synonym: "chars" },
        chars: "&entity;",

        charsDlg: {
            suggest: '&\\w+;?',
            charsDlg: [ Dialog.Chars, { caption:"Special Chars".localize() }]
        },


        icon: "%%icon-{}search /%",
        iconDlg: {
            scope: { "%%":"/%" },
            suggest: "icon-\\S*",
            iconDlg: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body:{
                    "icon-search":"<div class='icon-search'></div>",
                    "icon-user":"<div class='icon-user'></div>",
                    "icon-home":"<div class='icon-home'></div>",
                    "icon-refresh":"<div class='icon-refresh'></div>",
                    "icon-repeat":"<div class='icon-repeat'></div>",
                    "icon-bookmark":"<div class='icon-bookmark'></div>",
                    "icon-tint":"<div class='icon-tint'></div>",
                    "icon-plus":"<div class='icon-plus'></div>",
                    "icon-external-link":"<div class='icon-external-link'></div>",

                    "icon-signin":"<div class='icon-signin'></div>",
                    "icon-signout":"<div class='icon-signout'></div>",
                    "icon-rss":"<div class='icon-rss'></div>",
                    "icon-wrench":"<div class='icon-wrench'></div>",
                    "icon-filter":"<div class='icon-filter'></div>",
                    "icon-link":"<div class='icon-link'></div>",
                    "icon-paper-clip":"<div class='icon-paper-clip'></div>",
                    "icon-undo":"<div class='icon-undo'></div>",
                    "icon-euro":"<div class='icon-euro'></div>",
                    "icon-slimbox":"<div class='icon-slimbox'></div>",
                    "icon-picture":"<div class='icon-picture'></div>",
                    "icon-columns":"<div class='icon-columns'></div>"
                }
            }]
        },

        contextText: {
            scope: { "%%":"/%" },
            suggest: {pfx: "%%text-(\\w*)$", match: "^default|success|info|warning|danger" },
            contextText: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body:{
                    primary:"<span class='text-primary'>primary</span>",
                    success:"<span class='text-success'>success</span>",
                    info:"<span class='text-info'>info</span>",
                    warning:"<span class='text-warning'>warning</span>",
                    danger:"<span class='text-danger'>danger</span>"
                }
            }]
        },

        contextBG: {
            scope: { "%%":"/%" },
            suggest: {pfx:"%%(default|success|info|warning|error)$", match:"^default|success|info|warning|error"},
            contextBG: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body:{
                    "default":"<span class='default'>default</span>",
                    success:"<span class='success'>success</span>",
                    info:"<span class='info'>info</span>",
                    warning:"<span class='warning'>warning</span>",
                    error:"<span class='error'>error</span>",
                }
            }]
        },

        labelDlg: {
            scope: { "%%":"/%" },
            suggest: {pfx: "%%label-(\\w*)$", match: "^default|success|info|warning|danger" },
            labelDlg: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body:{
                    "default":"<span class='label label-default'>default</span>",
                    primary:"<span class='label label-primary'>primary</span>",
                    success:"<span class='label label-success'>success</span>",
                    info:"<span class='label label-info'>info</span>",
                    warning:"<span class='label label-warning'>warning</span>",
                    danger:"<span class='label label-danger'>danger</span>"
                }
            }]
        },

        listDlg: {
            scope: { "%%list-":"/%" },
            suggest: {pfx: "list-(?:[\\w-]+-)?(\\w*)$", match: "^\\w*" },
            listDlg: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body: "nostyle|unstyled|hover|group"
            }]
        },

        tableDlg: {
            scope: { "%%table-":"/%" },
            suggest: {pfx: "table-(?:[\\w-]+-)?(\\w*)$", match: "^\\w*" },
            tableDlg: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body: "sort|filter|striped|bordered|hover|condensed|fit"
            }]
        },


        cssDlg: {
            scope: { "%%":"/%" },
            suggest: {pfx:"%%([\\da-zA-Z-_]*)$", match:"^[\\da-zA-Z-_]*"},
            cssDlg: {
                "(css:value;)":"any css definitions",
                "default":"contextual backgrounds",
                "text-default":"contextual text color",
                "label-default":"<span class='label label-default'>contextual labels</span>",
                "badge":"badges <span class='badge'>13</span>",
                //"btn-default":"<span class='btn btn-xs btn-default'>Buttons</span>",
                "collapse":"collapsable lists",
                "list-nostyle":"list styles",
                //progress:"Progress Bars",
                "table-fit":"table styles",
                "":"",
                "add-css":"Add CSS",
                alert: "Alert Box",
                accordion: "Accordion",  //leftAccordion, rightAccordion, pillsAccordion, accordion-primary...
                category: "Category Links",
                carousel: "Carousel",
                columns: "Multi-column layout",
                commentbox: "Comment Box",
                //graphBar
                pills:"Pills",
                prettify: "Prettify syntax highlighter",
                scrollable: "Scrollable Preformatted block",
                "scrollable-image": "Scrollable Wide Images",
                //reflection: "Image with reflection",
                slimbox: "Slimbox Viewer <span class='icon-slimbox'></span>",
                //"under-construction": "<div class='under-construction'> </div>",
                tabs:"Tabs",
                viewer: "Media Viewer"

//inline styles
                //bold
                //italic
//                small:"<span class='small'>Smaller</span> text",
//                sub:"2<span class='sub'>8</span> Sub-Script",
//                sup:"2<span class='sup'>3</span> Super-Script",
//                strike:"<span class='strike'>strikethrough</span>",
//block styles
//                quote:"<div class='quote'>Quoted paragraph</div>",
//                lead:"<span class='lead'>LEAD text</span>",
//                "drop-caps":"Drop Caps",
                //xflow:"wide content with scroll bars"
            }
        },

        link: {
            key:'l',
            commandIdentifier:'createlink',
            //snippet: "[{description|}{pagename or url}|{attributes}] ",
            snippet: "[{pagename or url}] "
        },


        linkPart3:{
            suggest: {
                pfx: "\\[(?:[^\\|\\]]+\\|[^\\|\\]]+\\|)([^\\|\\[\\]\\n\\r]*)$",
                match: "^[^\\|\\]\\n\\r]*"
            },
            linkPart3: {
                //"class='category'": "Category link",
                "class='viewer'": "View Linked content",
                "class='slimbox'": "Add Slimbox link <span class='icon-slimbox'/> ",
                "class='slimbox-link'": "Replace by Slimbox Link <span class='icon-slimbox'/> ",
                "divide1": "",
                "class='btn btn-primary'": "Button Style",
                "class='btn btn-xs btn-primary'": "Small Button Style",
                "divide2": "",
                "target='_blank'": "Open link in new tab"
            }

        },

        linkDlg: {

            //match [description|link], [link] or [link,  do not match [{, [[
            //match '[' + 'any char except \n, [, { or ]' at end of the string
            //note: do not include the [ in the matched string
            suggest: {
                pfx: "\\[(?:[^\\|\\]]+\\|)?([^\\|\\[\\{\\]\\n\\r]*)$",
                match: "^([^\\|\\[\\{\\]\\n\\r]*)(?:\\]|\\|)"
            },

            linkDlg: [ Dialog.Selection, {

                caption: "Wiki Link",
                onOpen: function( dialog ){


                    var //dialog = this,
                        key = dialog.getValue();

                    if( !key || (key.trim()=='') ){ key = Wiki.PageName + '/'; }

                    //console.log('json lookup for '+key);
                    Wiki.jsonrpc("/search/suggestions", [key, 30], function( result ){

                        console.log("jsonrpc result", result );
                        if( result[1] /*length>1*/ ){

                            dialog.setBody( result );

                        } else {

                            dialog.hide();

                        }
                    });
                }
            }]
        },

        variableDlg: {
            scope:{ '[{$':'}]'},
            suggest: "\\w+",
            variableDlg: "applicationname|baseurl|encoding|inlinedimages|interwikilinks|jspwikiversion|loginstatus|uptime|pagename|pageprovider|pageproviderdescription|page-styles|requestcontext|totalpages|username"
        },


        // Page access rights
        allow: { synonym: "acl" },
        acl: "\n[{ALLOW {permission} {principal} }]\n",

        permission: {
            scope:{ '[{ALLOW':'}]'},
            suggest: { pfx:"ALLOW (\\w+)$", match:"^\\w+" },
            permission: [Dialog.Selection, {
                cssClass:".dialog-horizontal",
                body:"view|edit|modify|comment|rename|upload|delete"
            }]
        },
        principal: {
            scope:{ '[{ALLOW':'}]'},
            suggest: { pfx:"ALLOW \\w+ (?:[\\w,]+,)?(\\w*)$", match:"^\\w*" },

            principal: [ Dialog.Selection, {

                caption: "Roles, Groups or Users",
                onOpen: function( dialog ){

                    new Request({
                        url: Wiki.XHRPreview,
                        data: { page: Wiki.PageName, wikimarkup: "[{Groups}]" },
                        onSuccess: function(responseText){

                            var body = "Anonymous|Asserted|Authenticated|All";
                            responseText = responseText.replace( /<[^>]+>/g,'').replace(/\s*,\s*/g,'|' ).trim();
                            if(responseText != ""){ body = body + '||' + responseText; }

                            dialog.setBody(body);

                        }
                    }).send();

                }
            }]



        },

        toc: {
            nScope: { "[{":"}]" },
            snippet:"\n[\\{TableOfContents }]\n"
        },

        tocParams: {
            scope:{ '[{TableOfContents':'}]'},
            suggest: "\\s",
            tocParams: [Dialog.Selection, {
                caption: "TOC additional parameters",
                body:{
                " title='Page contents' ":"title",
                " numbered='true' ":"numbered",
                " prefix='Chap. ' ":"chapter prefix"
                }
            }]
        },


        plugin: "\n[{{plugin}}]\n",

        pluginDlg: {
            //match [{plugin}]  do not match [[{
            //match '[{' + 'any char except \n, or }]' at end of the string
            //note: do not include the [ in the matched string
            //snippet: "\n[{{plugin}}]\n",
            suggest: {
                pfx: "(^|[^\\[])\\[\\{([^\\[\\]\\n\\r]*)(?:\\|\\])?$",
                match: "^([^\\[\\]\\n\\r]*)\\}\\]"
            },
            pluginDlg: [ Dialog.Selection, {
                caption: "Plugin",
                body: {
                "ALLOW permission principal ": "Page Access Rights <span class='icon-unlock-alt' />",
                "SET name='value'":"Set a Wiki variable",
                "$varname":"Get a Wiki variable",
                "If name='value' page='pagename' exists='true' contains='regexp'\n\nbody\n":"IF plugin",
                "SET alias='${pagename}'":"Page Alias",
                "SET sidebar='off'":"Collapse Sidebar",
                //"Table":"Advanced Tables",
                //"Groups":"View all Wiki Groups",
                "":"",
                "Counter":"Insert a simple counter",
                "CurrentTimePlugin format='yyyy mmm-dd'":"Insert Current Time",
                "Denounce":"Denounce a link",
                "Image src='${image.jpg}'":"Insert an Image <span class='icon-picture'></span>",
                "IndexPlugin":"Index of all pages",

                "InsertPage page='${pagename}'":"Insert another Page",
                "SET page-styles='prettify-nonum table-condensed-fit'":"Insert Page Styles",
                "ListLocksPlugin":"List page locks",
                "RecentChangesPlugin":"Displays the recent changed pages",
                "ReferredPagesPlugin page='{pagename}' type='local|external|attachment' depth='1..8' include='regexp' exclude='regexp'":"Incoming Links (referred pages)",
                "ReferringPagesPlugin page='{pagename}' separator=',' include='regexp' exclude='regexp'":"Outgoing Links (referring pages)",
                "Search query='Janne' max='10'":"Insert a Search query",
                "TableOfContents ":"Table Of Contents ",
                "UndefinedPagesPlugin":"List pages that are missing",
                "UnusedPagesPlugin":"List pages that have been orphaned",
                "WeblogArchivePlugin":"Displays a list of older weblog entries",
                "WeblogEntryPlugin":"Makes a new weblog entry",
                "WeblogPlugin page='{pagename}' startDate='300604' days='30' maxEntries='30' allowComments='false'":"Builds a weblog"
                }
            }]

        },

        //FIXME
        //Commands triggered by the selection of substrings:
        //    lowest priority vs. other snippets
        selectInline: {
            suggest: function(workarea,caret,fromStart){
                if(!caret.thin){
                     console.log("got selection", caret);
                     return { pfx:"xx", match:workarea.getSelection() }
                }
            },

            selectInline: [Dialog.Selection, {
                cssClass: ".dialog-horizontal",
                body:{
                    "__bold__":"<b>bold</b>",
                    "''italic''":"<i>italic</i>",
                    "{{mono}}":"<tt>mono</tt>",
                    "[description|{link}|options]":"<span class='icon-link'/>",
                    "[{Image src='${image.jpg}'}]":"<span class='icon-picture'/>"
                }
            }]
        },
        selectBlock:  "code|prettify",
        selectStartOfLine: "!!!h1|!!h2|!h3|bold|italic|mono|link|plugin"

}

