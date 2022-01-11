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
    find: { },
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
            "%%tabs":"/%"
        },
        snippet:"%%tabs\n!tab-1\ntab-1 content 1\n!tab-2\ntab-2 content \n/%\n "
    },

    insertPageDlg:{
        scope: { "[{InsertPage":"}]" },
        suggest: { lback:"page='([^']*)'?$", match: "^([^']*)" },
        insertPageDlg: Wiki.pageDialog("Insert Page", "/search/suggestions")
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
        cssClass:".dialog-horizontal.icons",
        body: ( function(icons){
                    var obj = {};
                    icons.split(",").forEach( function(item){
                        item = "icon-"+item;
                        obj[item]="<div class='"+item+"'></div>";
                    })
                    return obj;
                }
              )( "search,user,home,refresh,repeat,bookmark,tint,plus,external-link,signin,signout,rss,wrench,filter,link,paper-clip,undo,euro,tag,star,star-o,heart,trash-o,ellipsis-v,pie-chart,location,info,warning,error,flash,smile,frown,meh,slimbox,picture,columns"
              )
        }]
    },

    textDlg: {
        scope: { "%%":" " },
        suggest: {
            lback: /(:?%%|\.)text-(\w*)$/,
            //match: "^default|success|info|warning|danger|capitalize|lowercase|uppercase|smallcaps"
            match: /^\w*/
        },
        textDlg: [Dialog.Selection, {
        cssClass:".dialog-horizontal.text-styles",
        body:{
            primary:"<span class='text-primary'>primary</span>",
            success:"<span class='text-success'>success</span>",
            info:"<span class='text-info'>info</span>",
            warning:"<span class='text-warning'>warning</span>",
            danger:"<span class='text-danger'>danger</span>",

            white:"<span class='text-white'>white</span>",
            //"white.shadow":"<span class='bg-black'><span class='text-white shadow'>shadow</span></span>",
            black:"<span class='text-black'>black</span>",
            //"black.shadow":"<span class='text-black shadow'>shadow</span>",

            divider2:"",
            left:"<span class='icon-align-left'></span>",
            center:"<span class='icon-align-center'></span>",
            right:"<span class='icon-align-right'></span>",
            justify:"<span class='icon-align-justify'></span>",

            capitalize:"<span class='text-capitalize'>Aa</span>",
            lowercase:"<span class='text-lowercase'>aa</span>",
            uppercase:"<span class='text-uppercase'>AA</span>",
            smallcaps:"<span class='text-smallcaps'>Aa</span>"

        }
        }]
    },

    contextBG: {
        scope: { "%%":" " },
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

    bgColorDlg: {
        scope: { "%%":" " },
        suggest: {
        lback: /(:?%%|\.)bg-(\w*)$/,
        match: /^\w*/
        },
        bgColorDlg: [Dialog.Selection, {
        cssClass:".dialog-horizontal.bg-colors",
        body:{
            primary:"<span class='bg-primary' title='primary'>&para;</span>",
            success:"<span class='bg-success' titlte='success'>&para;</span>",
            info:"<span class='bg-info' title='info'>&para;</span>",
            warning:"<span class='bg-warning' title='warning'>&para;</span>",
            danger:"<span class='bg-danger' title='danger'>&para;</span>",

            aqua:"<span class='bg-aqua' title='aqua'>&para;</span>",
            blue:"<span class='bg-blue' title='blue'>&para;</span>",
            navy:"<span class='bg-navy' title='navy'>&para;</span>",
            teal:"<span class='bg-teal' title='teal'>&para;</span>",
            green:"<span class='bg-green' title='green'>&para;</span>",
            olive:"<span class='bg-olive' title='olive'>&para;</span>",
            lime:"<span class='bg-lime' title='lime'>&para;</span>",

            yellow:"<span class='bg-yellow' title='yellow'>&para;</span>",
            orange:"<span class='bg-orange' title='orange'>&para;</span>",
            red:"<span class='bg-red' title='red'>&para;</span>",
            fuchsia:"<span class='bg-fuchsia' title='fuchsia'>&para;</span>",
            purple:"<span class='bg-purple' title='purple'>&para;</span>",
            maroon:"<span class='bg-maroon' title='maroon'>&para;</span>",

            white:"<span class='bg-white' title='white'>&para;</span>",
            silver:"<span class='bg-silver' title='silver'>&para;</span>",
            gray:"<span class='bg-gray' title='gray'>&para;</span>",
            black:"<span class='bg-black' title='black'>&para;</span>"
        }
        }]
    },

    bgDlg: {
        scope: { "%%bg.":" " },
        suggest: {
        lback: /(:%%bg\.|\.?)(\w*)$/,
        //match: "^default|success|info|warning|danger"
        match: /^\w*/
        },
        bgDlg: [Dialog.Selection, {
        //caption:"Background Image",
        cssClass:".dialog-horizontal",
        body:{
            "top":"&uarr;",
            "right":"&rarr;",
            "bottom":"&darr;",
            "left":"&larr;",
            "divider1":"",
            "contain": "Contain",
            "cover": "Cover",
            "fixed":"Fixed",
            "divider2":"",
            "dark":"Dark",
            "light":"Light",
            "kenburns":"Animated"
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
        body: "sort|filter|striped|bordered|noborder|hover|condensed|fit"
        }]
    },

    cssDlg: {
        scope: { "%%":" " },
        suggest: {lback:"(:?%%|\\.)([\\da-zA-Z-_]*)$", match:"^[\\da-zA-Z-_]*"},
        cssDlg: [Dialog.Selection, {
        caption: "dialog.styles".localize(),
        cssClass: ".dialog-filtered",
        body: {
        "(css:value;)":"any css definitions",
        "bg-":"Background colors",
        "text-{default}":"Text colors and other styles",
        "default":"<span class='default'>Contextual boxes</span>",
        "label-{default}":"<span class='label label-default'>Contextual labels</span>",
        "badge":"Badges <span class='badge'>007</span>",
        //"btn-default":"<span class='btn btn-xs btn-default'>Buttons</span>",
        "collapse":"Collapsible lists <b class='bullet'></b>",
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
        lback: /\[([^|[{\]\n\r]*)$/,
        match: /^([^|[{\]\n\r]*)(?:[\]\n\r])/
        },
        linkDlg: Wiki.pageDialog("Wiki Link", "/search/suggestions")

    },

    linkPart2: {
        //match [description|link], [description|link|...
        //do not match [{, [[
        //do not include the [ in the matched string
        suggest: {
        lback: /\[(?:[^|\]]+\|)([^|[{\]\n\r]*)$/,
        match: /^([^|[{\]\n\r]*)(?:[\]|\n\r])/
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
        "SET keywords={keyword1, keyword2}":"Set Page Keywords",
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
                    "\n{!!!}": "<span title='header'>H<span class='sub'>1</span></span>",
                    "\n{!!}": "<span title='title'>H<span class='sub'>2</span></span>",
                    "\n{!}": "<span title='sub-title'>H<span class='sub'>3</span></span>",
                    "\n* {list item}": "<span class='icon-list-ul'/>",
                    "\n# {list-item}": "<span class='icon-list-ol'/>",
                    "divider-sol": ""
                });
            }

            Object.append(body, {
                "__{bold}__": "<span style='font-family:serif;'><b>B</b></span>",
                "''{italic}''": "<span style='font-family:serif;'><i>I</i></span>",
                "{{{monospaced}}} ": "<tt title='inline monospaced'>&lt;&gt;</tt>",
                "{{{{code}}}}": "<span title='code' class='small' style='font-family:monospace;'>code</span>",
                "divider1": "",
                "[{link}]": "<span class='icon-link' title='Insert a link'/>",
                //"[description|{link}|options]": "<span class='icon-link'/>",
                "[{Image src='{image.jpg}'}]": "<span class='icon-picture' title='Insert an image'/>",
                "[{{plugin}}]": "<span class='icon-puzzle-piece' title='Insert a Plugin'></span>",
                "%%style {body} /%":"<span style='font-family:monospace;letter-spacing:-.2em;' title='Add a Style'>%%</span>",
                "divider2": "",
                "%%(font-family:{font};) body /%":"<span title='Change the Font'><span style='font-family:serif;'>A</span><span style='font-family:sans-serif'>a</span></span>",
                "&{entity};" : "<span style='font-family:cursive;' title='Insert a Special Character'>&amp;</span>",
                //"%%sub {subscript}/% ": "a<span class='sub'>n</span>",
                //"%%sup {superscript}/% ": "a<span class='sup'>m</span>",
                //"%%strike {strikethrough}/% ":"<span class='strike'>S</span>",
                //"divider3": "",
                "[{ALLOW {permission} principal }]":"<span class='icon-unlock-alt' title='Add a page ACL'></span>",
                "\\\\\n":"<b title='Insert a New Line'>&para;</b>"
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
