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
/*global $, Class, Options, Events, Undoable, Textarea, Dialog  */
/*eslint-disable no-console */

/*
Class: Snipe
    The Snipe class decorates a TEXTAREA object with extra capabilities such as
    section editing, tab-completion, auto-indentation,
    smart typing pairs, suggestion popups, toolbars, undo and redo functionality,
    advanced find & replace capabilities etc.
    The snip-editor can be configured with a set of snippet commands.
    See [getSnippet] for more info on how to define snippets.

Credit:
    Snipe (short for Snip-Editor) was inspired by postEditor (by Daniel Mota aka IceBeat,
    http://icebeat.bitacoras.com ) and ""textMate"" (http://macromates.com/).
    It has been written to fit as wiki markup editor for the JSPWIKI project.

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
    buttons - (array of Elements), each button elemnet will bind its click-event
        with [onButtonClick}. When the click event fires, the {{rel}} attribute
        or the text of the element will be used as snippet keyword.
        See also [tabSnippet].
    dialogs - set of dialogs, consisting of either a Dialog object,
        or a set of {dialog-options} for the predefined
        dialogs suchs as Font, Color and Special.
        See property [initializeDialogs] and [openDialog]
    findForm - (object) list of form-controls. See [onFindAndReplace] handler.
    onresize - (function, optional), when present, a textarea resize bar
        with css class {{resize-bar}} is added after the textarea,
        allowing to resize the heigth of the textarea.
        This onresize callback function is called whenever
        the height of the textarea is changed.

Dependencies:
    [Textarea]
    [Undoable]
    [Snipe.Snips]
    [Snipe.Commands]

Example:
(start code)
    new Snipe( "mainTextarea", {
        tabcompletion: true,
        snippets: { bold:"**{bold text}**", italic:"\"\"{italic text}\"\"" },
        directsnips: { "(":")", "[" : "]" }
    });
(end)

*/
var Snipe = new Class({

    Implements: [Options, Events, Undoable],

    Binds: ["sync","shortcut","keystroke","suggest","action"],

    options: {
        tab: "    ", //default tab = 4 spaces
        //autosuggest:false,
        //tabcompletion:false,
        //autocompletion:false,
        snippets: {},
        directsnips: {},
        //container: null,   //DOM element, container for toolbar buttons
        sectionCursor: "all",
        sectionParser: function(){ return {}; }
    },

    initialize: function(element, options){

        options = this.setOptions(options).options;

        this.initializeUndoable(options.undoBtns);

        var self = this,

            /*
            The textarea is cloned into a main and work textarea.
            The work texarea remains visible and is used for the actual editing.
            It contains either the full document or a particular section.
            The main textarea is hidden and contains always the complete document.
            On submit, the mainarea is send back to the server.
            */
            //main = self.mainarea = $(el),
            //work = main.clone().erase("name").inject( main.hide(), "before" ).addClass("snipe-work"),
            container = options.container || $(element).form,

            // Augment the textarea element with extra capabilities
            // Make sure the content of the mainarea is always in sync with the workarea
            //textarea = self.textarea = new Textarea( work );
            textarea = self.textarea = new Textarea( element );


        self.directsnips = new Snipe.Snips( textarea, options.directsnips );
        self.snippets    = new Snipe.Snips( textarea, options.snippets );

        self.snippets.dialogs.find = [ Dialog.Find, {
            data: {
                //feed the find dialog with searchable content
                selection: function(){
                    return textarea.getSelection();
                },
                get: function(){
                    var selection = textarea.getSelection();
                    return (selection=="") ? element.value : selection;
                },
                set: function(v){
                    var s = textarea.getSelectionRange();
                    self.fireEvent("beforeChange"); //make undoable
                    //textarea[ s.thin ? "setValue" : "setSelection" ](v);
                    s.thin ? element.value = v : textarea.setSelection(v);
                    self.fireEvent("change");
                }
            }
        }];

        //Snipe.Commands takes care of capturing commands.
        //Commands are entered via tab-completion, button clicks, or a dialog.
        //Snipe.Commands ensures that at most one dialog is open at the same time.
        self.commands = new Snipe.Commands( container, {
            onOpen: function( /*command*/ ){ element.focus(); },
            onClose: function( /*command*/ ){ element.focus(); },
            onAction: self.action,
            dialogs: self.snippets.dialogs,
            relativeTo: textarea
        });


        //add drag&drop handler: drag or paste data into the textarea
        if( options.dragAndDrop ){

            textarea.onDragAndDrop(options.dragAndDrop, function(){
                self.fireEvent("change")
            });
        }


        self.reset();

        element.addEvents({

            keydown: self.keystroke,
            keypress: self.keystroke,

            //fixme: any click outside the suggestion block should clear the context -- blur ?
            //blur: self.reset.bind(self),
            keyup: self.suggest.debounce(), //(250, true)
            click: self.suggest.debounce(),

            input: (function( ){
                //console.log("change event on input");
                self.fireEvent("change");
            }).debounce()

        });

        //catch shortcut keys when focus anywhere on the snipe editor (toolbar, textarea..)
        container.addEvent("keydown", self.shortcut);

    },

    /*
    Function: toElement
        Retrieve textarea DOM element;

    Example:
    >    var snipe = new Snipe("textarea-element");
    >    $("textarea-element") == snipe.toElement();
    >    $("textarea-element") == $(snipe);

    */
    toElement: function(){
        return this.textarea.toElement();
    },

    /*
    Function: get
        Retrieve some of the public properties or options of the snip-editor.

    Arguments:
        item - mainarea|textarea|snippets|directsnips|autosuggest|tabcompletion|smartpairs
    */
    get: function(item){

        return( "value" == item ? this.textarea.getValue() :
                /mainarea|textarea/.test(item) ? this[item] :
                /snippets|directsnips|autosuggest|tabcompletion|smartpairs/.test(item) ? this.options[item] :
                null );

    },

    /*
    Function: set
        Set/Reset some of the options of the snip-editor.
        Also use to set/reset the content of the textarea. (eg section editing)

    Arguments:
        item - snippets|directsnips|autosuggest|tabcompletion|smartpair
        value - new value
    Returns
        this Snipe object
    */
    set: function(item, value){

        if( item == "value" ){

            this.textarea.setValue( value );
            this.fireEvent("beforeChange"); //make undoable
            this.textarea.focus();

        } else if( /snippets|directsnips|autosuggest|tabcompletion|smartpairs/.test(item) ){

            this.options[item] = value;

        }

        return this.fireEvent("change");
    },

    /*
    Function: shortcut.
        Handle shortcut keys: Ctrl/Meta+shortcut key.
        This is a "Keypress" event handler connected to the container element
        of the snip editor.
    Note:
        Safari seems to choke on Cmd+b and Cmd+i. All other Cmd+keys are fine. !?
        It seems in those cases, the event is fired on document level only.

        More info http://unixpapa.com/js/key.html
    */
    shortcut: function(e){

        var key, cmd;

        if( e.shift || e.control || e.meta || e.alt ){

            key = (e.shift ? "shift+":"") +
                    (e.control ? "control+":"") +
                      (e.meta ? "meta+":"") +
                        (e.alt ? "alt+":"") +
                          e.key,

            console.log("shortcut ",key);
            cmd = this.snippets.keys[key];

            if ( cmd ){

                //console.log("Snipe shortcut", key, cmd, se.code);
                e.stop();
                this.commands.action( cmd );

            }
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

        //console.log(e.type, e.key, e.code, "shift:"+e.shift,"meta:"+e.meta,"ctrl:"+e.control );

        if( e.type == "keydown" ){

            //Exit if this is a normal key; process special chars with the keydown event
            if( e.key.length == 1 ){ return; }

        } else { // e.type == "keypress"

            //CHECKME
            //Only process regular character keys via keypress event
            //Note: cross-browser hack with "which" attribute for special chars
            if( !e.event.which /*which==0*/ ){ return; }

            //CHECKME: Reset faulty "special char" treatment by mootools
            //console.log( e.key, String.fromCharCode(e.code).toLowerCase());

            e.key = String.fromCharCode(e.code).toLowerCase();

        }

        var self = this,
            txta = self.textarea,
            key = e.key,
            caret = txta.getSelectionRange();

        txta.focus();

        if( /up|down|esc/.test(key) ){

            self.reset();

        } else if( /tab|enter|delete|backspace/.test(key) ){

            self[key](e, txta, caret);

        } else {

            self.smartPairs(e, txta, caret);

        }

    },

    /*
    Function: enter
        When the Enter key is pressed, the next line will be auto-indented
        with the previous line.
        Except if the Enter was pressed on an empty line.

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    enter: function(e, txta /*, caret*/ ) {

        this.reset();

        var prevline = txta.getFromStart().match( /(?:^|\r?\n)([ \t]+)(.*)$/ );
        //prevline[1]=sequence of spaces (indentation string)
        //prevline[2]=non-blank tail of the prevline

        if( !e.shift && prevline && ( prevline[2] != "" ) ){

            e.stop();
            //console.log("enter key - autoindent", prevline);
            txta.insertAfter( "\n" + prevline[1] );

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
                snip = this.directsnips.get( key );

            if( snip && (snip.snippet == txta.getValue().charAt(caret.start)) ){

                // remove the closing pair character
                txta.setSelectionRange( caret.start, caret.start+1 )
                    .setSelection("");

            }
        }
    },

    /*
    Function: delete
        Removes the previous TAB (4spaces) if matched

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    "delete": function(e, txta, caret) {

        var tab = this.options.tab;
        //console.log("delete key");

        if( caret.thin && !txta.getTillEnd().indexOf(tab) /*index==0*/ ){

            e.stop();
            txta.setSelectionRange(caret.start, caret.start + tab.length)
                .setSelection("");

        }
    },

    /*
    Function: tab
        Perform tab-completion function.
        Pressing a tab can lead to :
        - expansion of a snippet command cmd and selection of the first parameter
        - otherwise, expansion to set of spaces (4)


    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection

    */
    tab: function(e, txta, caret){

        var self = this, cmd;

        e.stop();

        if( self.options.tabcompletion ){

            if( caret.thin && ( cmd = self.snippets.match() ) ){

                //remove the command
                txta.setSelectionRange(caret.start - cmd.length, caret.start)
                    .setSelection( "" );

                return self.commands.action( cmd );
            }

        }

        //if you are still here, convert the tab into spaces
        self.tab2spaces(e, txta, caret);

    },

    /*
    Function: tab2spaces
        Convert tabs to spaces. When no snippets are detected, the default
        treatment of the TAB key is to insert a number of spaces.
        Indentation is also applied in case of multi-line selections.

    Arguments:
        e - event
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    tab2spaces: function(e, txta, caret){

        var tab = this.options.tab,
            selection = txta.getSelection(),
            fromStart = txta.getFromStart(),
            isCaretAtStart = txta.isCaretAtStartOfLine();

        //handle multi-line selection
        if( selection.indexOf("\n") > -1 ){

            if( isCaretAtStart ){ selection = "\n" + selection; }

            if( e.shift ){

                //shift-tab: remove leading tab space-block
                selection = selection.replace(RegExp("\n"+tab,"g"),"\n");

            } else {

                //tab: auto-indent by inserting a tab space-block
                selection = selection.replace(/\n/g,"\n"+tab);

            }

            txta.setSelection( isCaretAtStart ? selection.slice(1) : selection );

        } else {

            if( e.shift ){

                //shift-tab: remove "backward" tab space-block
                if( fromStart.test( tab + "$" ) ){

                    txta.setSelectionRange( caret.start - tab.length, caret.start )
                        .setSelection("");

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

    Arguments:
        snip - snippet object to make active
    */
    hasContext: function(){

        return !!this.context.snip;

    },

    setContext: function( snip, suggest ){

        //console.log("Snipe.setContext",snip,suggest);
        this.context = { snip:snip, suggest:suggest };

    },

    /*
    Function: reset
        Clear the context object, and remove the css class from the textarea.
        Also make sure that no dialogs are left open.
    */
    reset: function(){

        console.log("Snipe:reset", this.context);
        this.context = null;
        this.textarea.focus();
        this.commands.close();

    },


    /*
    Function: smartPairs
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
        """ : """,
        "(" : ")",
        "{" : "}",
        "<" : ">",
        """ : {
            snippet:""",
            scope:{
                "<javascript":"</javascript",
                "<code":"</code",
                "<html":"</html"
            }
        }
    }
    (end)

    */
    smartPairs: function(e, workarea, caret){

        var snip, key = e.key;

        if( this.options.smartpairs && (snip = this.directsnips.get(key)) ){

            e.stop();

            //insert the keystroke, retain the selection and insert the snippet outcome
            //and keep the original selection (caret)
            workarea.setSelection( key, workarea.getSelection(), snip.snippet )
                    .setSelectionRange( caret.start + 1, caret.end + 1 );

        }

    },


    /*
    Method: suggest
        Suggestion snippets are dialog-boxes appearing as you type.
        When clicking items in the suggest dialogs, content is inserted
        in the textarea.

    */
    suggest: function( /*event*/ ){

        var suggest;

        if( this.options.autosuggest ){

            if ( (suggest = this.snippets.matchSuggest()) ){

                console.log( "Snipe.suggest ",suggest );
                this.setContext( null/*snip*/, suggest );
                return this.commands.action( suggest.cmd , suggest.lback );

            }
            this.reset();
        }
    },


    /*
    Function: action
        This function executes the command action.
        It will coordinate to get the snippet inserted and move the
        caret to the proper position after insertion.

        Actions can be triggered via:
        - tab-completion
        - click-event from the [data-cmd] DOM element
        - short-cut key
        - matched suggestion (with look-back and match strings)

    Arguments:
        cmd - (string) used to loopup the snippet
        args - (optional) additional snippet arguments (eg value passed via a dialog)
    */
    action: function( cmd /*, more command arguments */ ){

        var self = this,
            args = Array.slice(arguments, 1),
            snip = self.snippets.get( cmd ),
            txta = self.textarea,
            caret,
            snipXL,
            snippet = snip.suggest ? args.join() : snip.snippet,
            suggest = snip.suggest && self.context && self.context.suggest;

        function unesc(s){ return s.replace(/~\{/g, "{"); }

        console.log("Snipe:action ", cmd, "snippet=",snippet, "args=",args, "suggest=",suggest );

        if( !snip ) return;

        if( snip.event ){
            //console.log("Snipe:action Event: ",snip.event);
            self.fireEvent(snip.event, arguments);

        } else {

            self.fireEvent("beforeChange"); //make action undoable

            if( suggest ){

                snippet = self.suggestAction( txta, snippet, suggest.lback, suggest.match);

            }


            /*
            Match "pfx{=copy-selection}sfx" and replace by "pfx<selection>sfx"
            snippet = snippet.replace(/..../, function(match, target){
                if(selection){
                    target = selection; selection = "";
                }
                return target;  //alse removing the {= and }
            });
            */


            /*
            Match "pfx{selection}sfx"

            snippet = snippet.replace( /regexp/, selection || RegExp.$1 );

            and also handle toggle !!?
            */


            // match "pfx{selection}sfx" into ["pfx","selection","sfx"]
            // do not match "pfx~{do-not-match}sfx"
            if(( snipXL = snippet.match( /(^|[\S\s]*[^~])\{([^!{}][^{}]*)\}([\S\s]*)/ ) )){
            //if( snipXL = snippet.match( /(^|[\S\s]*[^~])\{([^\{\}]+)\}([\S\s]*)/ ) ){

                //console.log("Snipe:action complex snippet 'pfx{selection}sfx' ", pfx, sel, sfx, caret.thin );
                self.injectXL( txta,
                               snippet,
                               unesc( snipXL[1] ), //pfx
                               snipXL[2],          //sel
                               unesc( snipXL[3] )  //sfx
                );

            } else {

                //if no selection, just insert and move caret after inserted snippet
                caret = txta.getSelectionRange();
                snippet = unesc(snippet);

                //console.log("Snipe:action simple snippet", caret.thin, snippet );
                self.inject( txta,
                             snippet,
                             caret.start + (caret.thin ? snippet.length : 0),
                             caret.thin ? 0 : snippet.length );
            }

        }

        self.reset();
        self.fireEvent("change");
        self.suggest.delay(1, self); //allow some time to finish any actions, before opening a new suggestion dialog

    },

    /*
    Function: suggestAction
        Adjust the snippet and the caret based on the suggestion context.
        The selection is set to the matched suggestion string, to prepare for the later
        snippet replacement.
        When the snippet starts with the look-back string; only the last part
        of the matched suggestion string should be replaced by the snippet.
        Returns a adjusted snippet.

    Example:  ($==caret position)
        [te$st] => lback = "te", match = "test",  snippet ="wiki-page"
            Selection will become "test",  and snippet will become "wiki-page"
        [te$st] => lback = "te", match = "test",  snippet ="team-page"
            Selection will become "st",  and snippet will become "am-page"
    */
    suggestAction: function( txta, snippet, lback, match){

        var start = txta.getSelectionRange().start,
            len = match.length;

        //console.log("Snipe:suggestAction ",snippet,lback,match );
        if( snippet.startsWith( lback ) ){

            //remove the look-back part of the snippet
            snippet = snippet.slice( lback.length );
            len -= lback.length;

        } else {

            //move the cursor to the start of the match
            start -= lback.length;

        }

        txta.setSelectionRange(start, start + len); //move the cursor

        return snippet;

    },

    /*
    Function: injectXL
        Inject a complex snippet.
        Complex snippet match this pattern: "pfx{selection}sfx".
        The part inside the "{..}" should be replaced by the selection.
        If the selection has already the pfx/sfx strings,  they should be toggled.
    Example
        snippet: "__{bold}__", no selection
            Snippet "__bold__" will be inserted, selection will become "bold"
        snippet: "__{bold}__", selection:"pipo"
            Snippet "__pipo__" will be inserted, selection will become "pipo"
        snippet: "__{bold}__", selection:__"pipo"__
            Snippet "pipo" will replace "__pipo__", selection will become "pipo"
        snippet: "__{bold}__", selection:"__pipo__"
            Snippet "pipo" will replace selection, selection will become "pipo"
    */
    injectXL: function( txta, snippet, pfx, sel, sfx){

        var caret = txta.getSelectionRange(),
            start = caret.start;

        if( !caret.thin ) {

            sel = txta.getSelection();
            console.log("injectXL: ", sel, pfx, sfx);

            if( sel.startsWith(pfx) && sel.endsWith(sfx) ){

                console.log("TOGGLE: pfx/sfx matched inside the selection",caret.start,caret.end);
                sel = sel.slice( pfx.length, -sfx.length );
                pfx = sfx = "";

            } else if( txta.getFromStart().endsWith(pfx)
                    && txta.getTillEnd().startsWith(sfx) ){

                console.log("TOGGLE: pfx/sfx matched outside the selection",caret.start,caret.end);
                start -= pfx.length;
                txta.setSelectionRange(start , caret.end + sfx.length);
                pfx = sfx = "";

            }
        }

        this.inject( txta, pfx + sel + sfx, start + pfx.length, sel.length);

    },

    /*
    Function: inject
        Replace the selection by the snippet and set a new selection.
        Collapse leading and trailing \n characters
        Autoindent the (multi-line) snippet.
    */
    inject: function( txta, snippet, start, selectionLen ){

        var fromStart = txta.getFromStart(),
            prevline = fromStart.split(/\r?\n/).pop(),
            indent = prevline.match(/^\s+/);

        console.log("inject: ", snippet, start, selectionLen );

        if( snippet.test(/^\n/) && ( fromStart.test( /(^|[\n\r]\s*)$/ ) ) ) {
            console.log("collapse leading \\n", snippet);
            snippet = snippet.slice( 1 );
            start--;
        }

        if( snippet.test(/\n$/) && ( txta.getTillEnd().test( /^\s*[\n\r]/ ) ) ) {
            console.log("collapse trailing \\n", snippet);
            snippet = snippet.slice(0, -1);
            start--;
        }

        if( indent ){
            //console.log("auto-indent internal newlines \n");
            snippet = snippet.replace( /\n/g, "\n" + indent[0] );
        }

        txta.setSelection( snippet )
            .setSelectionRange( start, start + selectionLen );

    },


    /*
    Interface: Undoable
        Implemenent the "undoable" interface.

    Function: getState
        State contains the value of the main and work area, the cursor and scroll position
    */
    getState: function(){

        var txta = this.textarea,
            el = txta.toElement();

        return {
            //main: this.mainarea.value,
            value: el.get("value"),
            cursor: txta.getSelectionRange(),
            scrollTop: el.scrollTop,
            scrollLeft: el.scrollLeft
        };
    },

    /*
    Function: putState
        Set a state of the Snip editor. This works in conjunction with the getState function.

    Argument:
        state - object originally created by the getState function
    */
    putState: function(state){

        var self = this,
            txta = self.textarea,
            el = txta.toElement();

        self.reset();
        //self.mainarea.value = state.main;
        el.value = state.value;
        el.scrollTop = state.scrollTop;
        el.scrollLeft = state.scrollLeft;
        txta.setSelectionRange( state.cursor.start, state.cursor.end );

        self.fireEvent("change");
        self.suggest();
    }

});
