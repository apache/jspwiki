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
    def: { alias: "dl" },
    pre:    "\n{{{\n{some preformatted block}\n}}}\n",
    code:   "\n%%prettify \n{{{\n{/* some code block */}\n}}}\n/%\n",
    table:  "\n||{heading-1} ||heading-2\n|cell11     |cell12\n|cell21     |cell22\n",

    me: { alias: "sign"},
    sign: function(){
        var name = Wiki.UserName || 'UserName';
        return "\n%%signature\n" + name + ", "+ new Date().toISOString() + "\n/%\n";
    },

    now: { alias: "date" },
    date: function( ){

        //FIXME: better use the date&time preference
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
        suggest: { lback:"src='([^']*)'?$", match: "^([^']*)" },
        imgSrcDlg: Wiki.pageDialog("Image", "/search/suggestions")
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
        suggest: {
        lback: "font-family:([^;\\)\\n\\r]*)$",
        match:"^([^;\\)\\n\\r]*)"
        },
        fontDlg: [Dialog.Font, {}]
    },

    color: "%%(color:{#000000}; background:#ffffff;) ${some text} /%",

    colorDlg: {
        scope: { '%%(': ')' },
        //  /\B#(?:[0-9a-f]{3}){1,2}\b/i
        suggest:"\B#(?:[0-9a-f]{3}){1,2}\b",
        colorDlg: [ Dialog.Color , {} ]
     },

    symbol: { alias: "chars" },
    chars: "&entity;",

    charsDlg: {
        suggest: {
        lback: /&\w*;?$/,
        match: /^&\w*;?/
        },
        dialog: [ Dialog.Chars, {
            caption:"dialog.character.entities".localize()
        }]
    },

    icon: "%%icon-{!search} /%",
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
        suggest: {
        lback: /%%text-(\w*)$/,
        //match: "^default|success|info|warning|danger|capitalize|lowercase|uppercase|smallcaps"
        match: /^\w*/
        },
        contextText: [Dialog.Selection, {
        cssClass:".dialog-horizontal",
        body:{
            primary:"<span class='text-primary'>primary</span>",
            success:"<span class='text-success'>success</span>",
            info:"<span class='text-info'>info</span>",
            warning:"<span class='text-warning'>warning</span>",
            danger:"<span class='text-danger'>danger</span>",
            "":"",
            capitalize:"<span class='text-capitalize'>capitalize</span>",
            lowercase:"<span class='text-lowercase'>lowercase</span>",
            uppercase:"<span class='text-uppercase'>uppercase</span>",
            smallcaps:"<span class='text-smallcaps'>Small Caps</span>"
        }
        }]
    },

    contextBG: {
        scope: { "%%":"/%" },
        suggest: {
        //lback: /%%(default|success|info|warning|error)$/,
        //match: /^\w+/
        lback: /%%(\w*)$/,
        match: /^(default|success|info|warning|error)/
        },
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
        suggest: {
        lback: /%%label-(\w*)$/,
        //match: "^default|success|info|warning|danger"
        match: /^\w*/
        },
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
        suggest: {lback: "list-(?:[\\w-]+-)?(\\w*)$", match: "^\\w*" },
        listDlg: [Dialog.Selection, {
        cssClass:".dialog-horizontal",
        body: "nostyle|unstyled|hover|group"
        }]
    },

    tableDlg: {
        scope: { "%%table-":"/%" },
        suggest: {lback: "table-(?:[\\w-]+-)?(\\w*)$", match: "^\\w*" },
        tableDlg: [Dialog.Selection, {
        cssClass:".dialog-horizontal",
        body: "sort|filter|striped|bordered|hover|condensed|fit"
        }]
    },

    cssDlg: {
        scope: { "%%":"/%" },
        suggest: {lback:"%%([\\da-zA-Z-_]*)$", match:"^[\\da-zA-Z-_]+"},
        cssDlg: [Dialog.Selection, {
        caption: "dialog.styles".localize(),
        cssClass: ".dialog-filtered",
        body: {
        "(css:value;)":"any css definitions",
        "default":"Contextual <span class='info'>backgrounds</span>",
        "text-{default}":"Contextual colors and other text styles",
        "label-{default}":"<span class='label label-success'>Contextual labels</span>",
        "badge":"Badges <span class='badge'>007</span>",
        //"btn-default":"<span class='btn btn-xs btn-default'>Buttons</span>",
        "collapse":"<b class='bullet'></b>Collapsible lists",
        "list-{nostyle}":"List styles",
        "table-{fit}":"Table styles",
        "":"",
        "add-css":"Add CSS",
        alert: "Alert Box",
        "accordion\n!Tab1\n{body1}\n!Tab2\nbody2\n": "Accordion",  //leftAccordion, rightAccordion, pillsAccordion, accordion-primary...
        category: "<span class='category-link'>Category Link</span>",
        carousel: "Carousel viewer",
        columns: "Multi-column layout",
        commentbox: "Comment Box",
        graphBar: "Graph Bars",
        lead:"<span class='lead-item'>LEAD text</span>",
        "pills\n!Tab1\n{body1}\n!Tab2\nbody2\n":"Pills",
        prettify: "Prettify syntax highlighter",
        progress:"Progress Bars",
        quote: "<div class='quote-item'>Quoted paragraph</div>",
        scrollable: "Scrollable <span style='font-family:monospace; white-space:pre;'>preformatted</span> block",
        "scrollable-image": "Scrollable Wide Images",
        //reflection: "Image with reflection",
        "under-construction":"<span class='under-construction small' style='display:inline-block;height:auto;margin-bottom:0'/>",
        slimbox: "Slimbox viewer <span class='icon-slimbox'></span>",
        "tabs\n!Tab1\n{body1}\n!Tab2\nbody2\n":"Tabs",
        viewer: "Media viewer"

        //"drop-caps":"Drop Caps",
        //xflow:"wide content with scroll bars"
        }
        }]
    },

    link: {
        key:'l',
        wysiwyg:'createlink',
        snippet: "[description|{pagename or url}|link-attributes] "
    },


    linkPart3:{
        suggest: {
        lback: "\\[(?:[^\\|\\]]+\\|[^\\|\\]]+\\|)([^\\|\\[\\]\\n\\r]*)$",
        match: "^[^\\|\\]\\n\\r]*"
        },
        linkPart3: [ Dialog.Selection, {

        caption: "dialog.link.attributes".localize(),
        body: {
            "link-attributes": "<i>no attributes</i>",
            //"class='category'": "<span class='category-link'>Category Link</span>",
            "class='viewer'": "Embedded Viewer",
            "class='slimbox'": "Add a Slimbox Link <span class='icon-slimbox'/> ",
            "class='slimbox-link'": "Change to Slimbox Link <span class='icon-slimbox'/> ",
            "divider1": "",
            "class='btn btn-primary'": "Button style (normal)",
            "class='btn btn-xs btn-primary'": "Button style (small)",
            "divider2": "",
            "target='_blank'": "Open link in new tab"
        }
        }]

    },

    linkDlg: {
        //match [link],  do not match [{, [[
        //do not include the [ in the matched string
        suggest: {
        lback: /\[([^\|\[{\]\n\r]*)$/,
        match: /^([^\|\[{\]\n\r]*)(?:[\]\n\r])/
        },
        linkDlg: Wiki.pageDialog("Wiki Link", "/search/suggestions")

    },

    linkPart2: {
        //match [description|link], [description|link|...
        //do not match [{, [[
        //do not include the [ in the matched string
        suggest: {
        lback: /\[(?:[^\|\]]+\|)([^\|\[{\]\n\r]*)$/,
        match: /^([^\|\[{\]\n\r]*)(?:[\]\|\n\r])/
        },
        linkPart2: Wiki.pageDialog("Wiki Link", "/search/suggestions")
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
        suggest: { lback:"ALLOW (\\w*)$", match:"^\\w+" },
        permission: [Dialog.Selection, {
            caption: "dialog.permission".localize(),
            cssClass:".dialog-horizontal",
            body:"view|edit|modify|comment|rename|upload|delete"
        }]
    },
    principal: {
        scope:{ '[{ALLOW':'}]'},
        suggest: { lback:"ALLOW \\w+ (?:[\\w,]+,)?(\\w*)$", match:"^\\w*" },

        principal: [ Dialog.Selection, {

        caption: "dialog.principal".localize(),
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
            caption: "dialog.toc.options".localize(),
            body:{
            " title='{Page contents}' ":"title",
            " numbered='true' ":"numbered",
            " prefix='{Chap. }' ":"chapter prefix"
            }
        }]
    },

    plugin: "\n[{{plugin} \n{=body}\n}]\n",

    pluginDlg: {
        //match [{plugin}]  do not match [[{
        //match '[{' + 'any char except \n, or }]' at end of the string
        //note: do not include the [ in the matched string
        suggest: {
            lback: "(^|[^\\[])\\[{(\\w*)(?:\\|\\])?$",
            //lback: "(^|[^\\[])\\[{([^\\[\\]\\n\\r]*)(?:\\|\\])?$",
            match: "^([^\\[\\]\\n\\r]*)\\}\\]"
        },
        pluginDlg: [ Dialog.Selection, {
        caption: "dialog.plugin".localize(),
        body: {
        "ALLOW {permission} principal ": "Page Access Rights <span class='icon-unlock-alt' />",
        "SET {name}='value'":"Set a Wiki variable",
        "${varname}":"Get a Wiki variable",
        "If name='{value}' page='pagename' exists='true' contains='regexp'\n\nbody\n":"IF plugin",
        "SET alias='{pagename}'":"Set Page Alias",
        "SET page-styles='prettify-nonum table-condensed-fit'":"Set Page Styles",
        "SET sidebar='off'":"Hide Sidebar",
        //"Table":"Advanced Tables",
        //"Groups":"View all Wiki Groups",
        "":"",
        "Counter":"Insert a simple counter",
        "PageViewPlugin":"Count Views of this page",
        "CurrentTimePlugin format='yyyy mmm-dd'":"Insert Current Time",
        "Denounce":"Denounce a link",
        "Image src='{image.jpg}'":"Insert an Image <span class='icon-picture'></span>",
        "IndexPlugin":"Index of all pages",

        "InsertPage page='{pagename}'":"Insert another Page",
        "ListLocksPlugin":"List page locks",
        "RecentChangesPlugin":"Displays the recent changed pages",
        "ReferringPagesPlugin page='{pagename}' separator=',' include='regexp' exclude='regexp'":"Incoming Links (referring pages)",
        "ReferredPagesPlugin page='{pagename}' type='local|external|attachment' depth='1..8' include='regexp' exclude='regexp'":"Outgoing Links (referred pages)",
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


    lipstick: {

        key: "control+enter",
        snippet: "{format}",
        nscope: { "[":"]" },

        suggest: function(textarea, caret /*, fromStart*/){

            return caret.thin ? null : { lback: "", match: textarea.getSelection() };

        },

        lipstick: [Dialog.Selection, {

        cssClass: ".dialog-horizontal",

        onBeforeOpen: function( dialog ){

            var body = {},
            textarea = dialog.options.relativeTo,
            caret = textarea.getSelectionRange();

            if( textarea.isCaretAtStartOfLine() ){
                Object.append(body, {
                    "\n{!!!}": "<span title='header'>H1</span>",
                    "\n{!!}": "<span title='title'>H2</span>",
                    "\n{!}": "<span title='sub-title'>H3</span>",
                    "\n* {list item}": "<span class='icon-list-ul'/>",
                    "\n# {list-item}": "<span class='icon-list-ol'/>",
                    "divider-sol": ""
                });
            }

            Object.append(body, {
                "__{bold}__": "<span style='font-family:serif;'><b>B</b></span>",
                "''{italic}''": "<span style='font-family:serif;'><i>I</i></span>",
                "{{{monospaced}}} ": "<tt>&lt;/&gt;</tt>",
                "{{{{code}}}}": "<span class='small' style='font-family:monospace;'>code</span>",
                "divider1": "",
                "[{link}]": "<span class='icon-link'/>",
                //"[description|{link}|options]": "<span class='icon-link'/>",
                "[{Image src='{image.jpg}'}]": "<span class='icon-picture'/>",
                "[{{plugin}}]": "<span class='icon-puzzle-piece'></span>",
                "%%style {body} /%":"<span style='font-family:monospace;'>%%</span>",
                "divider2": "",
                "%%(font-family:{font};) body /%":"<span style='font-family:serif;'>A</span><span style='font-family:sans-serif'>a</span>",
                "&{entity};" : "<span style='font-family:cursive;'>&amp;</span>",
                //"%%sub {subscript}/% ": "a<span class='sub'>n</span>",
                //"%%sup {superscript}/% ": "a<span class='sup'>m</span>",
                //"%%strike {strikethrough}/% ":"<span class='strike'>S</span>",
                //"divider3": "",
                "[{ALLOW {permission} principal }]":"<span class='icon-unlock-alt'></span>",
                "\\\\\n":"<b>&para;</b>"
            });

            if( textarea.isCaretAtStartOfLine()
            &&  textarea.isCaretAtEndOfLine()
            &&  textarea.slice(caret.start,caret.end-1).indexOf("\n") > -1 ){
                Object.append(body, {
                "divider-code": "",
                    "\n{{{\n/* code block */\n{code block}\n}}}\n": "<span class='small' style='font-family:monospace;'>&lt;/&gt;</span>",
                    "\n%%prettify\n{{{\n{pretiffied code block}\n}}}/%\n": "<span class='small' style='font-family:monospace;color:green;'>&lt;/&gt;</span>",
                    "\n%%scrollable\n{{{\n{code block}\n}}}/%\n": "&darr;&uarr;"
                });
            }

            dialog.setBody(body);

        }
        }]
    }
}
