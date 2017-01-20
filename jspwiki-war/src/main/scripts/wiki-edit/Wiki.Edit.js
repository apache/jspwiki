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
Class: Wiki.Edit
    Wiki.Edit implements the JSPWiki plain editor, with support
    for JSPWIki's markup, suggestion popups, ajax based page preview, etc...

    It uses [Snipe] to enhance the plain textarea.
*/

/*eslint-env browser*/
/*global Wiki, Snipe, Request */

!function( wiki ){


wiki.add("textarea#editorarea", function( main ){

    var form = main.form, snipe, preview;

    function getFormElem( selector ){  return form.getElement( selector );  }

    onbeforeunload( window, main );

    if( snipe = getFormElem("textarea.snipeable") ){

        snipe = new Snipe( snipe, {
            container: form,
            undoBtns: {
                undo: getFormElem("[data-cmd=undo]"),
                redo: getFormElem("[data-cmd=redo]")
            },
            snippets: wiki.Snips,
            directsnips: wiki.DirectSnips
        });

        wiki.configPrefs(form, function(cmd, isChecked){
            snipe.set(cmd, isChecked);
        });

        if( preview = getFormElem(".ajaxpreview") ){

            snipe.addEvent("change", livepreview.pass([
                preview,
                getFormElem("[data-cmd=livepreview]") || {}
            ], snipe));

        }

        new Snipe.Sections( snipe, {
            main: main,
            menu: getFormElem(".sections > ul"),
            parser: jspwikiSectionParser
        });

    }

});


/*
Function: onbeforeunload
    Install an onbeforeunload handler, which is called prior to unloading the page.
    The user will get a warning in case the textarea was changed, without saving.

    The onbeforeunload handler then gets removed on regular exit of the page.

*/
function onbeforeunload( window, main ){

    window.onbeforeunload = function(){

        if( main.value != main.defaultValue ){

            return "edit.areyousure".localize();

        }
    };

    main.form.addEvent("submit", function(){

        window.onbeforeunload = null;

    });
}


   /*
Function: livepreview
    Linked as onChange handler to Snipe.
    Make AJAX call to the wiki server to convert the contents of the textarea
    (wiki markup) to HTML.
*/

function livepreview(preview, previewToggle){

    var content = this.get("value").trim(),
        isEmpty = content == "",
        name, link;

    //console.log("**** change event", new Date().getSeconds() );

    function updateWiki( hasBeenUpdated ){

        preview.ifClass(!hasBeenUpdated, "loading");
        if( hasBeenUpdated ){ wiki.update(); }

    }

    if( !previewToggle.checked ){

        //reset the preview area
        if( preview.cache ){
            preview.empty();
            preview.cache = null;
        }

    } else if( preview.cache != content ){

        preview.cache = content;

        preview.ifClass( isEmpty, "empty" );

        if( isEmpty ){

            preview.innerHTML =  "preview.zone".localize();
            return;

        }

        if( wiki.Context == "comment" ){

            name = $("authorname").value || wiki.UserName || "AnonymousCoward";
            link = $("link").value;
            if( link ){ name = "[{0}|{1}]".xsubs(name, link); }

            //add the comment signature to the preview;  simulating Comment.jsp
            content += "\n\n%%signature\n{0}, [\\{CurrentTimePlugin}]\n/%\n".xsubs( name );

        }

        //return preview.set("html",preview.get("html")+" Lorem ipsum"); //test code

        //console.log("**** invoke Request.HTML ",previewcache, wiki.XHRPreview)
        new Request.HTML({
            url: wiki.XHRPreview,
            data: {
                page: wiki.PageName,
                wikimarkup: content
            },
            update: preview,
            onRequest: updateWiki,
            onComplete: updateWiki.pass(true)

        }).send();

    }
}

/*
Function: jspwikiSectionParser
    Convert a jspwiki-markup page into an array of page sections.
    Sections are marked by jspwiki headers:  !, !!  or !!!

    This function is used as a callback for [Snip.Sections]

Returns:
    This function returns a array of objects [{title, start, depth}]
    title - (string) plain title of the section (no wiki markup)
    start - (number) offset within the text string where this section starts
    depth - (number) nesting level of the section 0,1...n
*/
function jspwikiSectionParser( text ){

    var result = [],
        DELIM = "\u00a4",

        tt = text

            // mask confusing header markup inside a {{{ ... }}} but keep length of the text unchanged!
            .replace(/\{\{\{([\s\S]*?)\}\}\}/g, function(match){
                return match.replace( /^!/mg, " " );
            })

            // break string up into array of headers and section-bodies :
            // [0] : text prior to the first header
            // [1,odd] : header markup !, !! or !!!
            // [2,even] : remainder of the section, starting with header title
            .replace( /^([!]{1,3})/mg, DELIM + "$1" + DELIM )

            .split(DELIM),

        pos = tt.shift().length,  //get length of the first element, prior to first section
        count = tt.length,
        i, hlen, title;

    for( i = 0; i < count; i = i + 2 ){

        hlen = tt[i].length;
        //take first line
        title = tt[i + 1].split(/[\r\n]/)[0]

            //remove unescaped(~) inline wiki markup __,"",{{,}}, %%(*), /%
            .replace(/(^|[^~])(__|""|\{\{|\}\}|%%\([^\)]+\)|%%\S+\s|%%\([^\)]+\)|\/%)/g, "$1")

            //and remove wiki-markup escape chars ~
            .replace(/~([^~])/g, "$1");

        //depth: convert length of header markup (!!!,!!,!) into #depth-level:  3,2,1 => 0,1,2
        result[ i/2 ] = { title: title, start: pos, depth: 3 - hlen };
        pos += hlen + tt[i + 1].length;

    }

    return result;

}

}( Wiki );
