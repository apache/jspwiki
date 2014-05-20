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
    The WikiEdit class implements the JSPWiki's specific editor, with support
    for JSPWIki's markup, suggestion popups, ajax based page preview, etc...

    It uses an enhanced textarea based on the [SnipEditor] class.
*/


!function( wiki ){

var container,
    textarea, 
    snipe, 
    preview, previewcache,
    sectionDropDown;
    
wiki.add('#editform', function( element ){

    container = element;
    textarea = container.getElement('.editor');
    preview  = container.getElement('.ajaxpreview');
    sectionDropDown = container.getElement('[data-sections]');
    
    onbeforeunload( textarea );
    
    snipe = new Snipe( textarea, {

        snippets: wiki.Snips,
        directsnips: wiki.DirectSnips,

        onChange: preview ? livepreview : null, 
        onConfig: config

    });


    if( wiki.Context == 'edit' && sectionDropDown ){ 
        new Snipe.Sections( sectionDropDown, {
           snipe: snipe, 
           parser: sectionParser
        });
    }


    resizer( element.getElement('.resizer'), 'EditorCookie' );
 
    /*
    Initialize the configuration checkboxes:
    set the checkbox according to the wiki-prefs (cookie) and configure the snip-editor.
    */
    ['tabcompletion','smartpairs','autosuggest','livepreview','previewcolumn'].each( function(cmd){

        var el = container.getElement('[data-cmd='+cmd+']');
        if( el ){
            //console.log('init config ',cmd);
            el.checked = !!wiki.get(cmd);
            config( cmd );
        }

    })
    
});


    /*
    Function: onbeforeunload
        Install an onbeforeunload handler, which is called ''before'' the page unloads.
        The user gets a warning in case the textarea was changed, without saving.

        The onbeforeunload handler is automatically removed on regular exit of the page.
    */
    function onbeforeunload( textarea ){

        window.onbeforeunload = function(){
            if( textarea.value != textarea.defaultValue ){ return "edit.areyousure".localize(); }
        };

        textarea.getParent('form').addEvent('submit', function(){
            window.onbeforeunload = null;
        });
    }

    /*
    Function: makeResizable
        Activate the resize handle.
        While dragging the textarea, also update the size of the
        preview area. Store the new height in the 'EditorSize' prefs cookie.

    Arguments:
        element - draggable resize handle (DOM element)
        options - { cookie: name of the cookie to persist the editor size across pageloads }

    Globals:
        wiki - main wiki object, to get/set preference fields
        textarea - resizable textarea (DOM element)
        preview - preview (DOM element)
    */
    function resizer( handle, cookie ){

        var height = 'height',
            textarea = snipe.toElement(),
            size = wiki.get(cookie),
            y;

        function dragging(add){ handle.ifClass(add,'dragging'); }

        if( size ){ 
            textarea.setStyle(height, size); 
            preview.setStyle(height, size); 
        }

        if( handle ){ 
            //console.log("resizer ",textarea,preview);
            textarea.makeResizable({
                handle: handle,
                modifiers: { x:null },
                onDrag: function(){
                    y = this.value.now.y;
                    preview.setStyle(height, y);
                    wiki.set(cookie, y);
                },
                onBeforeStart: dragging.pass(true),
                onComplete: dragging.pass(false),
                onCancel: dragging.pass(false)

            });
        }
    }


    /*
    Function: livepreview
        Linked as onChange handler to the SnipEditor.
        Make AJAX call to the backend to convert the contents of the textarea
        (wiki markup) to HTML.
        FIXME: should work bothways. wysiwyg <-> wikimarkup

    */
    function livepreview(v){

        var text = snipe.toElement().get('value');

        if( !$('livepreview').checked ){

            //clean preview area
            if( previewcache ){
                preview.empty();
                previewcache = null;
            }

        } else if( previewcache != text.length ){

            previewcache = text.length;
            //return preview.set('html',preview.get('html')+' Lorem ipsum'); //test code

            new Request.HTML({
                url: wiki.XHRPreview,
                data: {
                    page: wiki.PageName,
                    wikimarkup: text
                },
                update: preview,
                onRequest: function(){ preview.addClass('loading'); },
                onComplete: function(){ 
                    preview.removeClass('loading'); 
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

        var el = container.getElement('[data-cmd='+cmd+']'),
            state, editarea; 

        if( el ){
        
            wiki.set(cmd, state = el.checked);

            if( cmd.test(/livepreview|previewcolumn/) ){

                editarea = container.getElement('.edit-area').ifClass(state,cmd);

                if( cmd == 'livepreview' ){

                    container.getElement('[data-cmd=previewcolumn]').disabled = !state;

                } else {    //cmd == 'previewcolumn'

                    if(state){ 
                        editarea.adopt(preview); 
                    } else {
                        preview.inject( container.getElement('.resizer'), 'after'); 
                    }
                }
            }
        
            snipe.set(cmd, state).fireEvent('change');

        }
    }

        
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
    function sectionParser( text ){

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
    }


}(Wiki);
