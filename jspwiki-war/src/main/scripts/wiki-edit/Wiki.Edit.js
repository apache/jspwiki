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

    It uses an enhanced textarea based on the [Snipe] class.
*/


!function( wiki ){

var editform,
    textarea,
    snipe,
    preview,
    previewcache,
    sectionsDropDown;

wiki.add("#editform", function( element ){

    editform = element;
    textarea = getFormElement(".editor");
    preview = getFormElement(".ajaxpreview");

    onbeforeunload( );

    snipe = new Snipe( textarea, {

        container: editform,
        undoBtns: {
            undo: getFormElement("[data-cmd=undo]"),
            redo: getFormElement("[data-cmd=redo]")
        },
        snippets: wiki.Snips,
        directsnips: wiki.DirectSnips,

/*
        onChange: ( function(){
            console.log("change event");
            if( preview ){ livepreview(); }
        } ).debounce(),
*/
        onChange: livepreview.debounce(1000),

        onConfig: config  //configuration callback

    });

    if( wiki.Context == "edit"
    && ( sectionsDropDown = getFormElement(".sections") ) ){

        new Snipe.Sections( sectionsDropDown, {
           snipe: snipe,
           parser: jspwikiSectionParser  //jspwiki section parser callback
        });

    }

    resizer( snipe.toElement(), getFormElement(".resizer"), "EditorCookie" );

    //Initialize the configuration checkboxes
    //Read the wiki-prefs cookie values.
    //EG: tabcompletion, smartpairs, autosuggest, livepreview, previewcolumn..
    editform.getElements(".config [data-cmd]").each( function( configElement ){

        var cmd = configElement.getAttribute("data-cmd");

        configElement.checked = !!wiki.prefs.get(cmd);  //read wiki preferences cookie

        configElement.getParent().ifClass(configElement.checked, 'active');

        //wiki.Snips[ cmd ] = { event: 'config' };
        //console.log(cmd, configElement.checked);
        config(cmd);

    });

    //livepreview();
    //livepreview.periodical(5000);


});


    /*
    Function: getFormElement
        Helper function : lookup first matching descendant DOM element from the editform
    */
    function getFormElement( selector ){

        return editform.getElement( selector );

    }

    /*
    Function: onbeforeunload
        Install an onbeforeunload handler, which is called ""before"" the page unloads.
        The user gets a warning in case the textarea was changed, without saving.

        The onbeforeunload handler is automatically removed on regular exit of the page.
    */
    function onbeforeunload( ){

        window.onbeforeunload = function(){
            if( textarea.value != textarea.defaultValue ){
                return "edit.areyousure".localize();
            }
        };

        editform.addEvent("submit", function(){ window.onbeforeunload = null; });
    }


    /*
    Function: resizer
        Activate the resize handle of the input textarea.
        While dragging the resize handle, update the textarea and the
        preview area. Store the new height in the "EditorSize" prefs cookie.

    Arguments:
        element - draggable resize handle (DOM element)
        options - { cookie: name of the cookie to persist the editor size across pageloads }

    Globals:
        wiki - main wiki object, to get/set preference fields
        textarea - resizable textarea (DOM element)
        preview - preview (DOM element)
    */
    function resizer( resizableTextarea, handle, cookie ){

        var height = "height",
            size = wiki.prefs.get(cookie),
            y;

        function helpdragging(add){ handle.ifClass(add, "dragging"); }

        if( size ){

            resizableTextarea.setStyle(height, size);
            preview.setStyle(height, size);

        }

        if( handle ){

            //console.log("resizer ",textarea,preview);
            resizableTextarea.makeResizable({
                handle: handle,
                modifiers: { x: null },
                onDrag: function(){
                    y = this.value.now.y;
                    preview.setStyle(height, y);
                    wiki.prefs.set(cookie, y);
                },
                onBeforeStart: helpdragging.pass(true),
                onComplete: helpdragging.pass(false),
                onCancel: helpdragging.pass(false)

            });

        }
    }


    /*
    Function: livepreview
        Linked as onChange handler to the SnipEditor.
        Make AJAX call to the backend to convert the contents of the textarea
        (wiki markup) to HTML.
        TODO: should work bothways. wysiwyg <-> wikimarkup

    */
    function livepreview( ){

        var text = snipe.toElement().get("value"),
            loading = "loading";

    console.log("**** change event");
        if( !(getFormElement("[data-cmd=livepreview]") || {}).checked ){

            //cleanup the preview area
            if( previewcache ){
                preview.empty();
                previewcache = null;
            }

        } else if( previewcache != text.length ){

            previewcache = text.length;
            //return preview.set("html",preview.get("html")+" Lorem ipsum"); //test code

            console.log("**** invoke Request.HTML ",wiki.XHRPreview)
            new Request.HTML({
                url: wiki.XHRPreview,
                data: {
                    page: wiki.PageName,
                    wikimarkup: text
                },
                update: preview,
                onRequest: function(){ preview.addClass(loading); },
                onComplete: function(){

                    preview.removeClass(loading);
                    wiki.update();

                }
            }).send();

        }
    }

    /*
    Function: config
        Change the configuration of the snip-editor, and store it
        in the wiki-prefs. (cookie)
        The configuration is read from DOM checkbox elements.
        The name of the DOM checkboxes correponds with the cookie names,
        and the cookienames correspond with the snip-editor state attribute, if applicable.

        - invoked by initconfig, to initialize checkboxes with cookie values.
        - invoked when the config cmd checkboxes are clicked (ref. snippet commands)

    Argument:
        cmd - which configuration command has been triggered or needs to be initialized.
    */
    function config( cmd ){

        var el = getFormElement("[data-cmd=" + cmd + "]"),
            state,
            previewcontainer;

        //console.log("CONFIG EVENT", cmd, arguments);

        if( el ){

            state = el.checked;
            wiki.prefs.set(cmd, state);  //persist in the pref cookie

            if( cmd.test( /livepreview|previewcolumn/ ) ){

                previewcontainer = getFormElement(".edit-area").ifClass(state, cmd);

                if( cmd == "livepreview" ){

                    //disable the previewcolumn toolbar cmd
                    getFormElement("[data-cmd=previewcolumn]").disabled = !state;

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
                    previewcontainer.grab(preview);

                }
            }

            snipe.set(cmd, state).fireEvent("change");

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
            result.push({ title: title, start: pos, depth: 3 - hlen });
            pos += hlen + tt[i + 1].length;

        }

        return result;

    }


}( Wiki );
