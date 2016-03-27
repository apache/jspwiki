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
/*global Wiki, Dialog, Request  */

/*
DirectSnippet definitions for JSPWiki, aka ''smartpairs''.
These snippets are directly expanded on keypress.
*/
Wiki.DirectSnips = {
    '"' : '"',
    '(' : ')',
    '[' : ']',
    '{' : '}',
    "'" : {
        snippet: "'",
        scope: {
            "[{" : "}]"  //plugin parameters
          }
    }
};

/*
Function: snippets

        Definitions for the JSPWiki editor commands.
*/

Wiki.Snips = {

        // Snipe predefined commands
        find: { key: "f" },
        undo: { event: "undo" },
        redo: { event: "redo" },

        // Configuration commands
        wysiwyg: { event: 'config' },
        smartpairs: { event: 'config' },
        livepreview: { event: 'config' },
        autosuggest: { event: 'config' },
        tabcompletion: { event: 'config' },
        previewcolumn: { event: 'config' },


        // Simple shortcuts
        br: {
            key: "shift+enter",
            snippet: "\\\\\n"
        },
        hr: "\n----\n",
        lorem: "This is just some sample. Don’t even bother reading it; you will just waste your time. Why do you keep reading? Do I have to use Lorem Ipsum to stop you? OK, here goes: Lorem ipsum dolor sit amet, consectetur adipi sicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Still reading? Gosh, you’re impossible. I’ll stop here to spare you.",
        Lorem: { alias: "lorem" },


        // simple inline tab completion commands
        bold:   { key: "b", snippet: "__{bold}__" },
        italic: { key: "i", snippet: "''{italic}''" },

        mono:   { key: "m", snippet: "{{{monospaced text}}} " },
        sub:    "%%sub {subscript text}/% ",
        sup:    "%%sup {superscript text}/% ",
        strike: "%%strike {strikethrough text}/% ",

        // simple block tab completion commands
        quote:  "\n%%quote\n{Quoted text}\n/%\n",
        dl:     "\n;{term}:definition-text ",
        pre:    "\n{{{\n{some preformatted block}\n}}}\n",
        code:   "\n%%prettify \n{{{\n{/* some code block */}\n}}}\n/%\n",
        table:  "\n||{heading-1} ||heading-2\n|cell11     |cell12\n|cell21     |cell22\n",
        t: { alias: "table" },

        me: { alias: "sign"},
        sign: function(){
            var name = Wiki.UserName || 'UserName';
            return "\n\\\\ &mdash;" + name + ", "+ new Date().toISOString() + "\\\\ \n";
        },

        hDlg: {
            suggest: { pfx:"(^|\n)([!]{1,3})$", match:"^([!]{1,3})(?:[^!])"},
            hDlg: [Dialog.Selection, {
                match: "=",  //exact match
                body: {
                    "!!!": "<span style='font-size:30px;xline-height:1;'>Header</span>",
                    "!!": "<span style='font-size:24px;xline-height:30px;'>Title</span>",
                    "!": "<span style='font-size:18px;xline-height:30px;'>Sub-title</span>",
                    "{text}": "Normal Paragraph"
                }
            }]
        },

        now: { alias: "date" },
        date: function( ){
            return new Date().toISOString()+' ';
            //return "[{Date value='" + d.toISOString() + "' }]"
            //return "[{Date " + d.toISOString() + " }]"
        },

        tabs: {
            nScope: {
                "%%(":")",
                "%%tabbedSection":"/%"
            },
            snippet:"%%tabbedSection \n%%tab-{tabTitle1}\ntab content 1\n/%\n%%tab-tabTitle2\ntab content 2\n/%\n/%\n "
        },

        img: "\n[{Image src='{img.jpg}' width='400px' height='300px' align='left' }]\n ",

        imgSrcDlg:{
            scope: { "[{Image":"}]" },
            suggest: { pfx:"src='([^']*)'?$", match: "^([^']*)" },
            imgSrcDlg: [ Dialog.Selection, {

                caption: "Image Source",
                onOpen: function( dialog ){

                    var key = dialog.getValue();

                    if( !key || (key.trim()=='') ){ key = Wiki.PageName + '/'; }

                    Wiki.jsonrpc("/search/suggestions", [key, 30], function( result ){

                        //console.log('jsonrpc result', result );
                        if( result[1] /*length>1*/ ){

                            dialog.setBody( result );

                        } else {

                            dialog.hide();

                        }
                    });
                }
            }]
        },

        imgAlignDlg: {
            scope: { "[{Image":"}]" },
            suggest: "align='\\w+'",
            imgAlignDlg: "left|center|right"
        },

        font: {
            nScope: { "%%(":")" },
            snippet: "%%(font-family:{font};) body /% "
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

        symbol: { alias: "chars" },
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
                    error:"<span class='error'>error</span>"
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
            suggest: {pfx:"%%([\\da-zA-Z-_]*)$", match:"^[\\da-zA-Z-_]+"},
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

//block styles
//                quote:"<div class='quote'>Quoted paragraph</div>",
//                lead:"<span class='lead'>LEAD text</span>",
//                "drop-caps":"Drop Caps",
                //xflow:"wide content with scroll bars"
            }
        },

        link: {
            key:'l',
            wysiwyg:'createlink',
            snippet: "[description|{pagename or url}|options] "
        },


        linkPart3:{
            suggest: {
                pfx: "\\[(?:[^\\|\\]]+\\|[^\\|\\]]+\\|)([^\\|\\[\\]\\n\\r]*)$",
                match: "^[^\\|\\]\\n\\r]*"
            },
            linkPart3: {
                //"class='category'": "Category link",
                "class='viewer'": "Embedded Viewer",
                "class='slimbox'": "Add a Slimbox Link <span class='icon-slimbox'/> ",
                "class='slimbox-link'": "Change to Slimbox Link <span class='icon-slimbox'/> ",
                "divide1": "",
                "class='btn btn-primary'": "Button style (normal)",
                "class='btn btn-xs btn-primary'": "Button style (small)",
                "divide2": "",
                "target='_blank'": "Open link in new tab"
            }

        },

        linkDlg: {

            //match [description|link], [link] or [link,  do not match [{, [[
            //match '[' + 'any char except \n, [, { or ]' at end of the string
            //note: do not include the [ in the matched string
            suggest: {
                pfx: "\\[(?:[^\\|\\]]+\\|)?([^\\|\\[{\\]\\n\\r]*)$",
                match: "^([^\\|\\[{\\]\\n\\r]*)(?:\\]|\\|)"
            },

            linkDlg: [ Dialog.Selection, {

                caption: "Wiki Link",
                onOpen: function( dialog ){

                    var //dialog = this,
                        key = dialog.getValue();

                    //if empty link, than fetch list of attachments of this page
                    if( !key || (key.trim()=='') ){ key = Wiki.PageName + "/"; }

                    Wiki.jsonrpc("/search/suggestions", [key, 30], function( result ){

                        //console.log("jsonrpc result", result );
                        if( result[0] /* length > 0 */ ){

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
        allow: { alias: "acl" },
        acl: "\n[{ALLOW {permission} principal }]\n",

        permission: {
            scope:{ '[{ALLOW':'}]'},
            suggest: { pfx:"ALLOW (\\w*)$", match:"^\\w+" },
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
            snippet:"\n[~{TableOfContents }]\n"
        },

        tocParams: {
            scope:{ '[{TableOfContents':'}]'},
            suggest: "\\s",
            tocParams: [Dialog.Selection, {
                caption: "TOC options",
                body:{
                " title='{Page contents}' ":"title",
                " numbered='true' ":"numbered",
                " prefix='{Chap. }' ":"chapter prefix"
                }
            }]
        },

        plugin: "\n[{{plugin}}]\n",

        pluginDlg: {
            //match [{plugin}]  do not match [[{
            //match '[{' + 'any char except \n, or }]' at end of the string
            //note: do not include the [ in the matched string
            suggest: {
                pfx: "(^|[^\\[])\\[{(\\w*)(?:\\|\\])?$",
                //pfx: "(^|[^\\[])\\[{([^\\[\\]\\n\\r]*)(?:\\|\\])?$",
                match: "^([^\\[\\]\\n\\r]*)\\}\\]"
            },
            pluginDlg: [ Dialog.Selection, {
                caption: "Plugin",
                body: {
                "ALLOW {permission} principal ": "Page Access Rights <span class='icon-unlock-alt' />",
                "SET {name}='value'":"Set a Wiki variable",
                "${varname}":"Get a Wiki variable",
                "If name='{value}' page='pagename' exists='true' contains='regexp'\n\nbody\n":"IF plugin",
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
                "Search query='{Janne}' max='10'":"Insert a Search query",
                "TableOfContents ":"Table Of Contents ",
                "UndefinedPagesPlugin":"List pages that are missing",
                "UnusedPagesPlugin":"List pages that have been orphaned",
                "WeblogArchivePlugin":"Displays a list of older weblog entries",
                "WeblogEntryPlugin":"Makes a new weblog entry",
                "WeblogPlugin page='{pagename}' startDate='300604' days='30' maxEntries='30' allowComments='false'":"Builds a weblog"
                }
            }]

        },

        selectBlock: {
            suggest: function(workarea, caret /*, fromStart*/){

                var selection = workarea.getSelection();

                if( !caret.thin
                  && workarea.isCaretAtStartOfLine()
                  && workarea.isCaretAtEndOfLine()
                  && selection.slice(0,-1).indexOf("\n") > -1 ){

                     //console.log("got block selection" );
                     return { pfx:"", match:workarea.getSelection() }
                }
            },

            selectBlock: [Dialog.Selection, {
                cssClass: ".dialog-horizontal",
                body:{
                    "\n{{{\n{code block}\n}}}\n": "<span style='font-family:monospace;'>code</span>",
                    "\n%%scrollable\n{{{\n{code block}\n}}}/%\n": "<span style='font-family:monospace;'>scrollable-code</span>",
                    "\n%%prettify\n{{{\n{pretiffied code block}\n}}}/%\n": "<span class='pun' style='font-family:monospace;'>prettify</span>"
                }
            }]
        },

        selectStartOfLine: {
            suggest: function(workarea, caret/*, fromStart*/ ){

                var selection = workarea.getSelection();

                if( !caret.thin
                  && workarea.isCaretAtStartOfLine()
                  && workarea.isCaretAtEndOfLine() ){

                     //console.log("got start of line selection", caret);
                     return { pfx:"", match:selection }
                }
            },

            selectStartOfLine: [Dialog.Selection, {
                cssClass: ".dialog-horizontal",
                body:{
                    "\n!!!{header}": "H1",
                    "\n!!{header}": "H2",
                    "\n!{header}": "H3",
                    "__{bold}__": "<b>bold</b>",
                    "''{italic}''": "<i>italic</i>",
                    "{{{monospaced text}}} ": "<tt>mono</tt>",
                    "{{{{code}}}}\n": "<span style='font-family:monospace;'>code</span>",
                    "[description|{link}|options]": "<span class='icon-link'/>",
                    "[{Image src='${image.jpg}'}]": "<span class='icon-picture'/>",
                    "\n[{{plugin}}]\n": "<span class='icon-puzzle-piece'></span>"
                }
            }]
        },
        //Commands triggered by the selection of substrings:
        //    lowest priority vs. other snippets
        selectInline: {
            suggest: function(workarea, caret/*, fromStart*/ ){

                if(!caret.thin){
                     //console.log("got selection", caret);
                     return { pfx:"", match:workarea.getSelection() }
                }
            },

            selectInline: [Dialog.Selection, {
                cssClass: ".dialog-horizontal",
                body:{
                    "__{bold}__":"<b>bold</b>",
                    "''{italic}''":"<i>italic</i>",
                    "{{{monospaced text}}}":"<tt>mono</tt>",
                    "{{{{code}}}}\n": "<span style='font-family:monospace;'>code</span>",
                    "[description|{pagename or url}|options]":"<span class='icon-link'/>",
                    "[{Image src='{image.jpg}'}]":"<span class='icon-picture'/>"
                }
            }]
        }

}

