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
/*global typeOf, instanceOf, Class, Options, Events, Snipe, Dialog  */
/*
Class: SnipEditor.Commands
    This class preprocesses all command triggers, such as
    suggestion commands, button clicks or tab-completion commands.

    It will make sure that only one dialog is open at the same time.
    It intializes and caches the dialogs, handles the show/hide/toggle
    of dialogs, and passes action events back to the Snipe Editor.

    Dialogs can be opened by means of
    - external command triggers: exec
    - click events on command buttons (.cmd.pop)
    - suggestion trigger (exec ???)

    DIALOGs are created as descendants of the Dialog class.
    - Dialog : floating dialog panel
        - FormDialog : predef content with open/close handlers ???
        - Dialog.Selection : selectable list of items
            - Dialog.Font : selection list of font items
            - Dialog.Section : selection list of textarea sections
        - Dialog.Chars : selection matrix of special character entities
        - Dialog.Color : color-wheel
        - Dialog.Find : find and replace dialog

Options:
    container: DOM element  => contains commands([data-cmd])
    dialogs - predefined set of dialog definitions
    relativeTo - relative position of the dialog (default is document.body)
    // **event handlers**
    onOpen - fired after opening any DIALOG
    onClose - fired after closing any DIALOG
    onAction - action call-back action(cmd, arguments)

Properties
    - buttons : collection of DOM-elements with click handlers to either
        action() or toggle()
    - dialogs : collection of dialog definitions [Dialog-class, {options}]

DOM structure:
    <div class="cmd tICON"><i>action command</i></div>
    <div class="cmd pop tICON"><i>dialog</i></div>

    <div class="dialog fixed"> ... dialog content </div>
*/
Snipe.Commands = new Class({

    Implements: [Events, Options],

    options: {
        //onAction:function(command,value){ .. },
        cmds: "data-cmd" //toolbar button data attribute
        //relativeTo: document.body //default position of a dialog
        //dialogs:{ cmd1:dialog1, cmd2:dialog2, ...}
    },
    dlgs: {},  //all cmd:instantiated dialogs  (lazy activation)
    btns: {},  //all button DOM elements
    dialogs: {},  //all cmd:dialogs  definitions

    initialize: function( container, options ){

        var self = this.setOptions(options),
            dataCmd = self.options.cmds,
            command,
            dialog,
            dialogs = options.dialogs || {};

        //add click buttons and dialogs
        container.addEvent("click:relay([" + dataCmd + "])", function(event){

            var cmd = this.get( dataCmd ),
                dlg = self.dlgs[ cmd ];

            dlg ? dlg.toggle() : self.action( cmd );

            // input fields (eg checkboxes) keep the default behaviour; other click events are disabled
            if( !this.matches("input") ){ event.stop(); }


        });

        //see if there are any dialogs linked to a button. Eg: "div.dialog.<command>"
        container.getElements("[" + dataCmd + "]").each( function(button){

            command = button.get(dataCmd);
            self.btns[command] = button;

            if( (dialog = container.getElement(".dialog." + command)) ){

                if( !dialogs[command] ){ dialogs[command] = [Dialog, {}]; }

                options = dialogs[command][1];
                //register the DOM dialog element, and move to top of DOM for proper absolute positioning
                options.dialog = dialog.inject(document.body);
                options.relativeTo = button;  //register the dialog positioning info

            }
        });

        self.addDialogs( dialogs );

    },

    /*
    Function: addDialog
        Add a new dialogs.

    Arguments:
        newdialogs: {cmd:[Dialog-Class, {options}]..}
    */
    addDialogs: function( newdialogs ){

        var dialog,
            command,
            dialogs = this.dialogs;

        for( command in newdialogs ){

            if( dialogs[command] ){
                console.log("Snipe.Commands addDialogs() - warning: double registration of => " + command);
            }

            dialog = dialogs[command] = newdialogs[command];

            //note: make sure to initialize this.dialogs[command] prior to calling show()
            if( instanceOf( dialog, Dialog ) ){ this.attach(dialog, command); }

        }
        //console.log("allDialogs: " + Object.keys(this.dialogs) );
    },

    /*
    Function: attach
        Attach event-handlers to a dialog
    */
    attach: function(dialog, command){

        var self = this,
            //fire ACTION event back to the invoker of the Snipe.Commands
            actionHdl = function(value){ self.fireEvent("action", [command, value]); };

        //console.log("Snipe.Commands: attachDialog() ", command, dialog);

        return self.dlgs[command] = dialog.addEvents({
            open: self.openDialog.bind(self, command),
            close: self.closeDialog.bind(self, command),
            action: actionHdl,
            drag: actionHdl
        });
    },

    /*
    Function: action
        Action handler for a simple command.
        Send the "action" event back to the Snipe Editor.

    Arguments:
        command : command name
        value : (optional) initial value of the dialog
    */
    action: function( command, value ){

        var self = this,
            active = "active",
            button = self.btns[command],
            dialog;

        //console.log("Commands.action ", command, " value:", value, " btn=", button, " dlg=", dialog);
        //if( button ) button = document.id( button);

        if( button && button.matches(".disabled") ){

            //nothing to be done here

        } else if( self.dialogs[command] ){

            dialog = self.dlgs[command] || self.createDialog(command);
            if( value != null ){ dialog.setValue( value ); }
            dialog.show();

        } else {

            if( button ){ button.addClass(active); }
            self.fireEvent("action", [command, value] );
            if( button ){ button.removeClass(active); }

        }

    },

    /*
    Function: createDialog
        Create a new dialog, based on dialog creation parameters in this.dlgs :
        - [ Dialog-class, { options } ]
        - otherwise convert to Dialog.Selection dialog


    Arguments
        command - (string) command

        The open/close handlers will make sure only one dialog is open at the
        same time. The open dialog is stored in {{this.activecommand}}.

        The key events "action", "drag", "open" and "close" are propagated upwards.

    Returns:
        The created dialog, which is also stored in this.dlgs[] repository.

    */
    createDialog: function( command ){

        var dialog = $.toFunction( this.dialogs[command] )();

        //console.log("Snipe.Commands: createDialog() " + command + " ",dialog );

        if( typeOf(dialog) != "array" ){

            dialog = [ Dialog.Selection, { body: dialog } ];

        }

        if( !dialog[1].relativeTo ){

            dialog[1].relativeTo = this.options.relativeTo || document.body;

        }

        dialog[1].autoClose = false;

        //note: make sure to initialize this.dialogs[command] prior to calling show()
        return this.attach( new dialog[0]( dialog[1] ), command);
    },

    /*
    Function: openDialog
        Opens a dialog. If already another dialog was open, that one will first be closed.
        When a toolbar button exists, it will get the css class ".active".

        Note: make sure that this.dlgs[cmd] is initialized prior to calling show() !

    Argument:
        command - dialog to be opened
    */
    openDialog: function(command){

        var self = this,
            activeDlg = self.activeDlg,
            newDlg = self.dlgs[command],
            button = self.btns[command];

        //console.log("Snipe.Commands: openDialog() " + command + " " + activeDlg);
        if( activeDlg && (activeDlg != newDlg) ){ activeDlg.hide(); }
        self.activeDlg = self.dlgs[command];

        if( button ){ button.addClass("active"); }

        self.fireEvent("open", command);

    },

    /*
    Function: closeDialog

    Arguments:
        command - (mandatory) dialog to be closed
    */
    closeDialog: function(command /*, dialog*/){

        var self = this,
            button = self.btns[command];

        //console.log("Snipe.Commands: closeDialog() " + command )
        if( self.dlgs[command] == self.activeDlg ){ self.activeDlg = null; }

        if( button ){ button.removeClass("active"); }

        self.fireEvent("close", command);

    },

    /*
    Function: close
        Close any active dialog.
    */
    close: function(){

        var activeDlg = this.activeDlg;
        if( activeDlg ){ activeDlg.hide(); }

    }

});