/*
Class: SnipEditor.Commands
    This class preprocesses all command triggers, such as
    suggestion commands, button clicks or tab-completion commands.

    It will make sure that only one dialog is open at the same time.
    It intializes and caches the dialogs, handles the show/hide/toggle
    of dialogs, and passes action events back to the SnipEditor.

    Dialogs can be opened by means of
    - external command triggers: exec
    - click events on command buttons (.cmd.pop)
    - suggestion trigger (exec ???)

    FYI - all DIALOGs are created as descendants of the Dialog class.
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
    dialogs - predefined set of dialog initialisators
    // **event handlers**
    onOpen - invoked after opening a DIALOG
    onClose - invoked after closing a DIALOG
    onAction - action call-back action(cmd,arguments)

Properties
    - buttons : collection of DOM-elements with click handlers to either
        action() or toggle()
    - dialogs : collection of dialog definitions [Dialog-class, {dialog parameters}]

DOM structure:
    <div class="cmd tICON"><i>action command</i></div>
    <div class="cmd pop tICON"><i>dialog</i></div>

    <div class="dialog fixed"> ... dialog content </div>
*/
Snipe.Commands = new Class({

    Implements: [Events, Options],

    options: {
        //onAction:function()...s
        cmds:'cmd' //toolbar button data attribute
        //dialogs:{ cmd1:dialog1, cmd2:dialog2, ...}
    },
    btns: {},     //all cmd:buttons (dom element)
    dlgs: {},     //all cmd:instantiated dialogs  (lazy activation)
    dialogs: {},  //all cmd:dialogs

    initialize: function( container, options ){

        var self = this.setOptions(options), 
            attr = 'data-' + self.options.cmds,
            command, 
            dialog,
            dialogs = options.dialogs||{};

        container.getElements('['+attr+']').each( function(el){

            command = el.get(attr);

            self.btns[command] = el.addEvent('click', self.click.pass(command,self));

            if( dialog = container.getElement('.dialog.' + command) ){
            
                if( dialogs[command] ){

                    dialogs[command][1].dialog = dialog;  

                } else {

                    dialogs[command] = dialog;

                }
            }
        });

        self.addDialogs( dialogs );
    },

    /*
    Funciton: addDialog
        Add a new dialog.
        The dialog is only created when invoking the command.
        This happens through a button click or through the action() method.

    Arguments:
        dialogs: {cmd1:dialog, cmd2:dialog-def...}
        (dialog-def : array[Dialog-Class, {dialog parameters}]
        relativeTo: create a dialog relative to a positioned object (eg. button, textarea-location)
    */
    addDialogs: function(newdialogs, relativeTo){

        var self = this,
            dialog,
            command
            dialogs = self.dialogs;
        
        for( command in newdialogs ){

            if( dialogs && dialogs[command] ){
                console.log("AddDialogs - warning: double registration of => " + command);
            }

            dialog = dialogs[command] = newdialogs[command];  //array of all dialogs
            if( instanceOf( dialog, Dialog ) ){ self.attach(command,dialog); }

            //checkme ...
            if( relativeTo ){ self.btns[ command ] = relativeTo; }

        };
        //console.log('allDialogs: '+Object.keys(self.dialogs) );
    },

    attach: function(command, dialog){

        var self = this,
            actionHdl = function(v){ self.fireEvent('action', [command,v]); };

        //console.log('attachDialog: '+command);

        return self.dlgs[command] = dialog.addEvents({
            onOpen: self.openDialog.bind(self, command),
            onClose: self.closeDialog.bind(self, command),
            onAction: actionHdl,
            onDrag: actionHdl
        });
    },

    click: function( command ){

        var dialog = this.dlgs[ command ];
        dialog ? dialog.toggle() : this.action( command );

    },

    /*
    Function: action
        Action handler for a simple command. Pass the 'action' event
        up to the Snipe Editor.

    Arguments:
        command : command name
        value : (optional) initial value of the dialog
    */
    action: function( command, value ){

        var self = this, 
            active = 'active',
            button = self.btns[command], 
            dialog = self.dlgs[command];

        //console.log("Commands.action "+command+" value:"+value+" btn="+button+ " dlg="+dialog);
        if( button ) button = document.id( button);

        if( button && button.match('.disabled') ){

            //nothing to do here

        } else if( self.dialogs[command] ){

            if( !dialog ){ dialog = self.createDialog(command) }
            if( value ){ dialog.setValue( value ); }
            dialog.show();

        } else {

            if( button ){ button.addClass(active); }
            self.fireEvent('action', [command, value] );
            if( button ){ button.removeClass(active); }

        }

    },

    /*
    Function: createDialog
        Create a new dialog.
        The name of the cmd determines the type (or class) of Dialog to be created
        - cmd: Dialog[cmd] (eg cmd='font' : Dialog.Font)
        - the name of the dialog equals the DOM ID of a predefined HTML dialog

        - DOM Element: predefined DOM dialog
        - [ Dialog-class, { dialog parameters } ]
        - { dialog parameters } : the cmd determines the type of Dialog to create
        - "string" : create a Dialog.Selection dialog


    Arguments
        cmd - (string) command

        The open/close handlers will make sure only one dialog is open at the
        same time. The open dialog is stored in {{this.activeCmd}}.

        The key events 'action', 'drag', 'open' and 'close' are propagated upwards.

    Returns:
        The created dialog, which is also stored in this.dlgs[] repository.

    */
    createDialog: function( cmd ){

        var self = this,
            dlg,
            btn = self.btns[cmd],
            factory = Function.from( self.dialogs[cmd] )(),
            type = typeOf(factory);

        //console.log('Commands.createDialog() '+cmd+' '+ ' btn='+btn +" "+type);

        //expect factory to be [Dialog class,  {dialog options object}]
        if( type != 'array' || factory.length != 2 ){

            factory = ( type == 'element' ) ?
                [Dialog, {dialog:factory}] : [Dialog.Selection, {body:factory}];
        }

        dlg = new factory[0]( Object.append( factory[1],{
            //cssClass: 'dialog float',
            autoClose: false, //fixme: suggestion dialog should not be autoclosed
            relativeTo: btn   //button or textareaa
            //draggable: true
        }) );

        //Make sure that this.dlgs[cmd] gets initialized prior to calling show()
        return self.attach(cmd, dlg);
    },

    /*
    Function: openDialog
        Opens a dialog. If already another dialog was open, that one will first be closed.
        When a toolbar button exists, it will get the css class '.active'.

        Note: make sure that this.dlgs[cmd] is initialized prior to calling show() !

    Argument:
        command - dialog to be opened
        preOpen - ...
    */
    openDialog: function(command, dialog){

        var self = this, 
            current = self.activeCmd, 
            tmp;

        //console.log('Commands.openDialog() ' + command + ' ' + self.activeCmd );

        if( ( current!=command ) && ( tmp = self.dlgs[current] ) ){ tmp.hide(); }
        //toobar button will be deactivated by closeDialog()

        self.activeCmd = command;
        if( tmp = self.btns[command] ){ $(tmp).addClass('active'); }

        self.fireEvent('open', command, dialog);
    },

    /*
    Function: closeDialog

    Arguments:
        cmd - (mandatory) dialog to be closed
    */
    closeDialog: function(cmd, dialog){

        var self = this, 
            btn = self.btns[cmd];

        //console.log('Commands.closeDialog() ' + cmd + ' ' + self.activeCmd )

        if( cmd == self.activeCmd ){ self.activeCmd = null; }
        if( btn ){ $(btn).removeClass('active'); }

        self.fireEvent('close', cmd, dialog);
    },

    /*
    Function: close
        Close the active dialog, if any.
    */
    close: function(){

        var activeCmd = this.activeCmd;
        if( activeCmd ){ this.dlgs[activeCmd].hide(); }

    }

});


