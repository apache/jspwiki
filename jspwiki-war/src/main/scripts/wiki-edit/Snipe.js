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

    initialize: function(el, options){

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
            main = self.mainarea = $(el),
            work = main.clone().erase("name").inject( main.hide(), "before" ),
            container = options.container || work.form,

            // Augment the textarea element with extra capabilities
            // Make sure the content of the mainarea is always in sync with the workarea
            textarea = self.textarea = new Textarea( work );

        self.directsnips = new Snipe.Snips( textarea, options.directsnips );
        self.snippets    = new Snipe.Snips( textarea, options.snippets );

        self.snippets.dialogs.find = [ Dialog.Find, {
            data: {
                //feed the find dialog with searchable content
                get: function(){
                    var selection = textarea.getSelection();
                    return (selection=="") ? work.value : selection;
                    //return (selection=="") ? textarea.getValue() : selection;
                },
                set: function(v){
                    var s = textarea.getSelectionRange();
                    //self.fireEvent("beforeChange");
                    //textarea[ s.thin ? "setValue" : "setSelection" ](v);
                    s.thin ? work.value = v : textarea.setSelection(v);
                    self.fireEvent("change");
                }
            }
        }];

        //Snipe.Commands takes care of capturing commands.
        //Commands are entered via tab-completion, button clicks, or a dialog.
        //Snipe.Commands ensures that at most one dialog is open at the same time.
        self.commands = new Snipe.Commands( container, {
            //onOpen: function( /*command*/ ){ work.focus(); },
            onClose: function( /*command*/ ){ work.focus(); },
            onAction: self.action,
            dialogs: self.snippets.dialogs,
            relativeTo: textarea
        });


        self.reset();

        work.addEvents({

            keydown: self.keystroke,
            keypress: self.keystroke,

            //fixme: any click outside the suggestion block should clear the context -- blur ?
            //blur: self.reset.bind(self),
            keyup: self.suggest.debounce(), //(250, true)
            click: self.suggest.debounce(),

            input: (function( ){
                console.log("***Snipe: dom textarea input :");
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
        return $(this.textarea);
    },

    /*
    Function: get
        Retrieve some of the public properties or options of the snip-editor.

    Arguments:
        item - mainarea|textarea|snippets|directsnips|autosuggest|tabcompletion|smartpairs
    */
    get: function(item){

        return( /mainarea|textarea/.test(item) ? this[item] :
                /snippets|directsnips|autosuggest|tabcompletion|smartpairs/.test(item) ? this.options[item] :
                null );

    },

    /*
    Function: set
        Set/Reset some of the options of the snip-editor.

    Arguments:
        item - snippets|directsnips|autosuggest|tabcompletion|smartpair
        value - new value
    Returns
        this Snipe object
    */
    set: function(item, value){

        if( /snippets|directsnips|autosuggest|tabcompletion|smartpairs/.test(item) ){
            this.options[item] = value;
        }
        return this;
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

        var key, keycmd;

        if( e.shift || e.control || e.meta || e.alt ){

            key = (e.shift ? "shift+":"") +
                    (e.control ? "control+":"") +
                      (e.meta ? "meta+":"") +
                        (e.alt ? "alt+":"") +
                          e.key,

            //console.log("shortcut ",key);
            keycmd = this.snippets.keys[key];

            if ( keycmd ){

                //console.log("Snipe shortcut",key,keycmd,e.code);
                e.stop();
                this.commands.action( keycmd );

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
            //el = txta.toElement(),
            key = e.key,
            caret = txta.getSelectionRange();

        txta.toElement().focus();

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
        As long as a snippet is active, the textarea gets the css class {{.activeSnip}}.

    Arguments:
        snip - snippet object to make active
    */
    hasContext: function(){

        return !!this.context.snip;

    },

    setContext: function( snip, suggest ){

        //console.log("Snipe.setContext",snip,suggest);
        this.context = { snip:snip, suggest:suggest };
        this.toElement().addClass("activeSnip");

    },

    /*
    Function: reset
        Clear the context object, and remove the css class from the textarea.
        Also make sure that no dialogs are left open.
    */
    reset: function(){

        //console.log("Snipe:reset", this.context);
        this.context = {};
        this.toElement().removeClass("activeSnip").focus();
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
                return this.commands.action( suggest.cmd , suggest.pfx );

            }

            this.commands.close();
        }
    },


    /*
    Function: action
        This function executes the command action.
        It will insert the snippet and update the selection and caret.

        It looks up and processes the snippet.
        - insert the snippet text at the caret in the textarea.
        - when text was selected (prior to the click or keyup event):
            - the snippet text will replace the selection
            - the selection will be passed as a parameter into the snippet
            - if the snippet has ONE parameter, the snippet text will be toggled:
              i.e. remove the snippet when already present, otherwise insert the snippet

    Arguments:
        cmd - (string) used to loopup the snippet
        args - (optional) additional snippet arguments (eg value passed via a dialog)
    */
    action: function( cmd ){

        var self = this,
            args = Array.slice(arguments, 1),
            snip = self.snippets.get( cmd ),
            //snippet = snip.snippet,
            s = snip.snippet,
            suggest = snip.suggest && self.context.suggest,

            txta = self.textarea,
            caret = txta.getSelectionRange(),
            hasCaret = !caret.thin,
            caretStart = caret.start,

            pfx, sel, sfx, snipArr, selectionLength,
            /* match "pfx{selection}sfx" into ["pfx","selection","sfx"] */
            /* do not match "pfx~{do-not-match}sfx"  */
            snipSelection = /(^|[\S\s]*[^~])\{([^\{\}]+)\}([\S\s]*)/,
            snipEscapeChar = /~\{/g;

        function setCaret(start,end){ txta.setSelectionRange(start, end); }
        function removeEscapeChar(s){ return s.replace(snipEscapeChar, "{"); }

        console.log("Snipe:action ", snip, s, cmd, args,suggest );

        if( snip ){

            if( snip.event ){

                //console.log("Snipe:action() fire-event: ",snip.event);
                return self.fireEvent(snip.event, arguments);

            }

            this.fireEvent("beforeChange"); //CHECKME

            //adjust caret based on suggestion context
            if( suggest ){

                s = args.join();  //result of the suggest dialog
                selectionLength = suggest.match.length;

                if( s.startsWith(suggest.pfx) ){

                    s = s.slice(suggest.pfx.length);
                    selectionLength -= suggest.pfx.length;

                } else {

                    caretStart -= suggest.pfx.length;

                }

                //move the caret according to suggest pfx and match
                setCaret( caretStart, caretStart +  selectionLength );
                hasCaret = selectionLength > 0;
            }

            if( (snipArr = s.match( snipSelection )) ){

                pfx = removeEscapeChar( snipArr[1] );
                sel = hasCaret ?  txta.getSelection() : snipArr[2] ;
                sfx = removeEscapeChar( snipArr[3] );

                console.log("found a 'pfx{selection}sfx' snippet", snipArr, pfx, sel, sfx,hasCaret );

                if( hasCaret ) {

                    if( sel.startsWith(pfx) && sel.endsWith(sfx) ){

                        console.log("TOGGLE: pfx/sfx matched inside the selection",caret.start,caret.end);
                        sel = sel.slice( pfx.length, -sfx.length );
                        pfx = sfx = "";

                    } else if( txta.getFromStart().endsWith(pfx)
                            && txta.getTillEnd().startsWith(sfx) ){

                        console.log("TOGGLE: pfx/sfx matched outside the selection",caret.start,caret.end);
                        caretStart -= pfx.length;
                        setCaret( caretStart, caret.end + sfx.length); //enlarge the selection
                        pfx = sfx = "";

                    }
                }

                s = pfx + sel + sfx;
                caretStart += pfx.length;
                selectionLength = sel.length;

            } else {

                console.log("plain text snippet", hasCaret,s );
                s = removeEscapeChar( s );
                caretStart += hasCaret ? 0 : s.length;
                selectionLength = hasCaret ? s.length : 0;

            }

            self.inject(s, caretStart, selectionLength);
            self.reset();

            self.fireEvent("change");
            //allow to finish the actions, before opening a new suggestion dialog
            self.suggest.delay(10, self);

        }

    },

    inject: function( snippet, start, selectionLength ){

        var self = this,
            txta = self.textarea,
            fromStart = txta.getFromStart(),
            prevline = fromStart.split(/\r?\n/).pop(),
            indentation = prevline.match(/^\s+/);

        //console.log(snippet,start,selectionLength);

        //process whitespace before and after the snippet
        //collapse \n of previous line if the snippet starts with \n
        if( snippet.test(/^\n/) && ( fromStart.test( /(^|[\n\r]\s*)$/ ) ) ) {
            //console.log("remove leading \\n", snippet);
            snippet = snippet.slice( 1 );
            start--;
        }

        //collapse \n of the next line when the snippet ends with a \n
        if( snippet.test(/\n$/) && ( txta.getTillEnd().test( /^\s*[\n\r]/ ) ) ) {
            //console.log("remove trailing \\n", snippet);
            snippet = snippet.slice(0, -1);
        }

        //finally auto-indent the snippets internal newlines \n
        if( indentation ){
            snippet = snippet.replace( /\n/g, "\n" + indentation[0] );
        }

        txta.setSelection( snippet )
            .setSelectionRange( start, start + selectionLength );

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
            main: this.mainarea.value,
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
        self.mainarea.value = state.main;
        el.value = state.value;
        el.scrollTop = state.scrollTop;
        el.scrollLeft = state.scrollLeft;
        txta.setSelectionRange( state.cursor.start, state.cursor.end );

        self.fireEvent("change");
        self.suggest();
    }

});
