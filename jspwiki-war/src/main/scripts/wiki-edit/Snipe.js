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
    next - (Element), when pressing Shift-Enter, the textarea will ""blur"" and
        this ""next"" element will ge the focus.
        This compensates the overwritting default TAB handling of the browser.
    onresize - (function, optional), when present, a textarea resize bar
        with css class {{resize-bar}} is added after the textarea,
        allowing to resize the heigth of the textarea.
        This onresize callback function is called whenever
        the height of the textarea is changed.

Dependencies:
    [Textarea]
    [UndoRedo]
    [Snipe.Commands]

Example:
(start code)
    new Snipe( "mainTextarea", {
        snippets: { bold:"**{bold text}**", italic:"""{italic text}""" },
        tabcompletion:true,
        directsnips: { "(":")", "[" : "]" },
        buttons: $$("a.tool"),
        next:"nextInputField"
    });
(end)

*/
var Snipe = new Class({

    Implements: [Options, Events, Undoable],

    Binds: ["sync","shortcut","keystroke","suggest","select","action"],

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
            The work texarea is visible and used for the actual editing.
            It contains either the full document or a particular section.
            The main textarea is hidden and is kept always up to date.
            On submit, the mainarea is send back to the server.
            */
            main = self.mainarea = $(el),
            work = main.clone().erase("name").inject( main.hide(), "before" ),
            container = options.container || work.form,

            // Augment the textarea element with extra capabilities
            // Make sure the content of the mainarea is always in sync with the workarea
            textarea = self.textarea = new Textarea( work );

        //console.log("**** JUST CREATED THE SNIPE WORK EDITAEREA!  Huray!! ****");

        self.directsnips = new Snipe.Snips( textarea, options.directsnips );
        self.snippets    = new Snipe.Snips( textarea, options.snippets );

        self.snippets.dialogs.find = [ Dialog.Find, {
            data: {
                //feed the find dialog with searchable content
                get: function(){
                    var selection = textarea.getSelection();
                    return (selection=="") ? work.value : selection;
                },
                set: function(v){
                    var s = textarea.getSelectionRange();
                    self.fireEvent("beforeChange");
                    s.thin ? work.value = v : textarea.setSelection(v);
                }
            }
        }];

        //console.log("snip dialogs",JSON.encode(Object.keys(self.snippets.dialogs)));

        //Snipe.Commands takes care of capturing commands.
        //Commands are entered via tab-completion, button clicks, or a dialog.
        //Snipe.Commands also ensures that at most one dialog is open at the same time.
        self.commands = new Snipe.Commands( container, {
            //onOpen: function( /*command*/ ){ work.focus(); },
            onClose: function( /*command*/ ){ work.focus(); },
            onAction: self.action,

            dialogs: self.snippets.dialogs,
            relativeTo: textarea
        });


        self.reset();

        //Activate snipe trigger events on the work textarea
        work.addEvents({

            keydown: self.keystroke,
            keypress: self.keystroke,

            //fixme: any click outside the suggestion block should clear the context -- blur ?
            //blur: self.reset.bind(self), //(and hide any open dialogs; and update the preview area...)
            keyup: self.suggest.debounce(), //(250, true)
            click: self.suggest.debounce(),
            //select: self.select.debounce(),
            change: function( parm ){
                //console.log("bubble change event from textarea");
                self.fireEvent("change",parm);
            }

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
        item - textarea|snippets|tabcompletion|directsnips|smartpairs|autosuggest
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

                console.log("Snipe shortcut",key,keycmd,e.code);
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

        //console.log(e.key, e.code + " keystroke "+e.shift+" "+e.type+"+meta="+e.meta+" +ctrl="+e.control );

        if( e.type=="keydown" ){

            //Exit if this is a normal key; process special chars with the keydown event
            if( e.key.length==1 ) return;

        } else { // e.type=="keypress"

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
            el = txta.toElement(),
            key = e.key,
            caret = txta.getSelectionRange();

        el.focus();

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
    enter: function(e, txta, caret) {

        this.reset();

        var prevline = txta.getFromStart().match( /(?:^|\r?\n)([ \t]+)(.*)$/ );
        //prevline[1]=sequence of spaces (indentation string)
        //prevline[2]=non-blank tail of the prevline

        if( !e.shift && prevline && (prevline[2]!="") ){

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
                //snip = this.getSnippet( this.options.directsnips, key );

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
                    .setSelection("");

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
            fromStart = txta.getFromStart();
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

        //console.log("Snipe:reset");
        this.context = {};
        this.commands.close();
        this.toElement().removeClass("activeSnip").focus();

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
            workarea.setSelection( key, workarea.getSelection(), snip.text )
                    .setSelectionRange( caret.start+1, caret.end+1 );

        }

    },


    /*
    Method: suggest
        Suggestion snippets are dialog-boxes appearing as you type.
        When clicking items in the suggest dialogs, content is inserted
        in the textarea.

        snip.suggest => {
            cmd:  <cmd>,
            start: <pos>,
            value: <match-string-before-caret>,
            xxxtail:  <length-match-after-caret>
        }

    */
    suggest: function(e){

        var self = this, suggest;

        if( this.options.autosuggest ){

            if ( suggest = this.snippets.matchSuggest() ){

                console.log( "Snipe.suggest ",suggest );
                this.setContext( null/*snip*/, suggest );
                return this.commands.action( suggest.cmd , suggest.pfx );

            }
            //close suggest dialog if one is still open
            //is this ok??

            this.reset();
        }

        self.fireEvent("change"); //Potential change of the textarea, to be debounced by receiver -- CHECKME

    },


    /*
    Method: select
        Selection snippets are dialog-boxes appearing when you select some text.
        When clicking items in the SELECTION dialogs, content is inserted
        in the textarea.

        Selection commands (lowest priority vs. other snippets)
        - selectInline: "bold|italic|mono|link",
        - selectBlock:  "code|prettify"
        - selectStartOfLine: "!!!h1|!!h2|!h3|bold|italic|mono|link|plugin"

    */
    select: function(e){

        var self = this,
            txta = self.textarea,
            selection = txta.getSelectionRange(),
            cmd = "selectInline";

        if( !txta.getSelectionRange().thin ){

            if( txta.isCaretAtStartOfLine() ){

                cmd = txta.isCaretAtEndOfLine() ? "selectBlock" : "selectStartOfLine";

            }

            console.log("SELECT COMMAND", cmd );
            //return this.commands.action( cmd );

        }

    },


    /*
    Function: action
        This function executes the command action.
        The trigger of a command can be
        - "keydown/keypress" TAB key , via snipe.commands to close any dialogs...
        - "keydown" with CTRL/META key, via snipe.command to close any dialogs...
        - "click" event on a button , captured by snipe.commands
        - "keyup"/"click" on the workarea -- suggestion snip


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
            args = Array.slice(arguments,1),
            snip, // = self.context.snip || self.getSnippet(self.options.snippets, cmd),
            txta = self.textarea,
            caret = txta.getSelectionRange(),
            s, p;

        //console.log("Snipe:action() "+cmd+" ["+args+"]");

        if( snip = self.snippets.get(cmd) ){

            if( snip.event ){
                console.log("Snipe:action() fire-event: ",snip.event);
                return self.fireEvent(snip.event, arguments);
            }

            this.fireEvent("beforeChange");

            //$(txta).focus();  CHECKME

            if( snip.suggest ){

                return self.suggestAction( cmd, args );
                //todo -- bring that here inline ...
                //snip.text = ...

            }

            s = snip.text;

            if( !caret.thin && (snip.parms.length==2) ){

                s = self.toggleSnip(txta, snip, caret);
                //console.log("toggle snippet: "+s+" parms:"+snip.parms);

            }

            //inject args into the snippet parms
            while( args && args[0] ){

                if( snip.parms[0] ){
                    s = s.replace( snip.parms.shift(), args.shift() );
                }

            }

            //replace the first parm by the selected text
            if( snip.parms[1] ){

                if( !caret.thin ){
                    p = snip.parms.shift();
                        s = s.replace( p, txta.getSelection() );
                    }

            }

            txta.setSelection( s );

            //next action
            if( false && snip.parms[1] ){

                //this snippet has one or more parameters left
                //store the active snip and process the next {parameter}
                //checkme !!

                if( !self.hasContext() ){ self.setContext( snip ); }

                caret = txta.getSelectionRange(); //update new caret

                self.nextAction( txta, caret );


            } else {

                //when no selection, move caret after the inserted snippet,
                //otherwise leave the selection unchanged
                if( caret.thin ){

                    txta.setSelectionRange( caret.start + s.length );

                }

                self.reset();

            }

        }

    },

    /*
    Function: toggleSnip
        Toggle the prefix and suffix of a snippet.
        Eg. toggle between {{__text__}} and {{text}}.
        The selection will be matched against the snippet.

    Precondition:
        - the selection is not empty (caret.thin = false)
        - the snippet has exatly one {parameter}

    Arguments:
        txta - Textarea object
        snip - Snippet object
        caret - Caret object {start, end, thin}

    Returns:
        - (string) replacement string for the selection.
            By default, returns snip.text
        - the snip.parms will be set to [] is toggle was executed successfully
        Eventually the selection will be extended if the
        prefix and suffix were just outside the selection.
    */
    toggleSnip: function(txta, snip, caret){

        var s = snip.text,
            //get the first and last textual parts of the snippet
            arr = s.trim().split( snip.parms[0] ),
            fst = arr[0],
            lst = arr[1],
            re = new RegExp( "^\\s*" + fst.trim().escapeRegExp() + "\\s*(.*)\\s*" + lst.trim().escapeRegExp() + "\\s*$" );

        if( (fst+lst)!="" ){

            s = txta.getSelection();
            snip.parms = [];

            // if pfx & sfx (with optional whitespace) are matched: remove them
            if( s.test(re) ){

                s = s.replace( re, "$1" );

            // if pfx/sfx are matched just outside the selection: extend selection
            } else if( txta.getFromStart().test(fst.escapeRegExp()+"$") && txta.getTillEnd().test("^"+lst.escapeRegExp()) ){

                txta.setSelectionRange(caret.start-fst.length, caret.end+lst.length);

            // otherwise, insert snippet and set caret between pfx and sfx
            } else {

                txta.setSelection( fst+lst ).setSelectionRange( caret.start + fst.length );
            }
        }
        return s;
    },

    /*
    Method: suggestAction
        <todo>
        suggest = { pfx: "prefix-string" , match:"full-string" }
    */
    suggestAction: function( cmd, valueArr ){

console.log(this.context.suggest);
        var suggest = this.context.suggest,
            workarea = this.textarea,
            start = workarea.getSelectionRange().start - suggest.pfx.length;

        //jump to the matched string
        start = workarea.indexOf( suggest.match, start );

        if( start >= 0 ){

            workarea.setSelectionRange( start, start + suggest.match.length )
                    .setSelection( valueArr )
                    .setSelectionRange( workarea.getSelectionRange().end );

        }
        return this.suggest();

    },

    /*
    CHECKME : remove..
    Function: nextAction
        Process the next ""{parameter}"" of the active snippet as you tab along
        or after you clicked a button or closed a dialog.

    Arguments:
        txta - Textarea object
        caret - caret object, indicating the start/end of the textarea selection
    */
    nextAction: function(txta, caret){

        var self = this,
            snip = self.context.snip,
            parms = snip.parms,
            dialog,
            pos;

        while( parms[0] ){

            dialog = parms.shift();
            pos = txta.getValue().indexOf(dialog, caret.start);

            //console.log("next action: "+dialog+ " pos:" + pos + " parms: "+parms+" caret:"+caret.start);

            //found the next {dialog} or possibly the end of the snippet
            if( (dialog !="") && (pos > -1) ){

                if( parms[0] ){

                    // select the next {dialog}
                    txta.setSelectionRange( pos, pos + dialog.length );

                    //invoke the new dialog
                    //console.log("next action: invoke "+dialog+" "+snip[dialog])
                    self.commands.action( dialog, snip[dialog] );

                    //remember every selected snippet dialog
                    //self.undoredo.onChange();
                    //self.fireEvent("change");

                    return; // and retain the context snip for subsequent {dialogs}

                } else {

                    // no more {dialogs}, move the caret after the end of the snippet
                    txta.setSelectionRange( pos + dialog.length );

                }
            }
        }

        self.reset();
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
    }

});
