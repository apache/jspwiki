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
Class: UndoRedo
    The UndoRedo class implements a simple undo/redo stack to save and restore
    the state of an 'undo-able' object.
    The object needs to provide a {{getState()}} and a {{putState(obj)}} methods.
    Whenever the object changes, it should call the UndoRedo onChange() handler.
    Optionally, event-handlers can be attached for undo() and redo() functions.

Arguments:
    obj - the undo-able object
    options - optional, see options below

Options:
    maxundo - integer , maximal size of the undo and redo stack (default 20)
    redo - (optional) DOM element, will get a click handler to the redo() function
    undo - (optional) DOM element, will get a click handler to the undo() function

Example:
(start code)
        var undoredo = new UndoRedo(this, {
            redoElement:'redoID',
            undoElement:'undoID'
        });

        //when a change occurs on the calling object which needs to be persisted
        undoredo.onChange( );
(end)
*/
var UndoRedo = new Class({

    Implements: Options,

    options: {
        //redo : redo button selector
        //undo : undo button selector
        maxundo:40
    },
    initialize: function(obj, options){

        var self = this,
            btn = this.btn = { redo:options.redo, undo:options.undo };

        self.setOptions(options);
        self.obj = obj;
        self.redo = [];
        self.undo = [];

        self.btnStyle();
    },

    /*
    Function: onChange
        Call the onChange function to persist the current state of the undo-able object.
        The UndoRedo class will call the {{obj.getState()}} to retrieve the state info.

    Arguments:
        state - (optional) state object to be persisted. If not present,
            the state will be retrieved via a call to the {{obj.getState()}} function.
    */
    onChange: function(state){

        var self = this;

        self.undo.push( state || self.obj.getState() );
        self.redo = [];

        if(self.undo[self.options.maxundo]){ 
            self.undo.shift(); 
        }
        self.btnStyle();

    },

    /*
    Function: onUndo
        Click event-handler to recall the state of the object
    */
    onUndo: function(e){

        var self = this;

        if(e){ e.stop(); }

        //if(self.undo.length > 0){
        if(self.undo[0] /*length>0*/){

            self.redo.push( self.obj.getState() );
            self.obj.putState( self.undo.pop() );

        }
        self.btnStyle();

    },

    /*
    Function: onRedo
        Click event-handler to recall the state of the object after a previous undo action.
        The state will be reset by means of the {{obj.putState()}} method
    */
    onRedo: function(e){

        var self = this;

        if(e){ e.stop(); }

        //if(self.redo.length > 0){
        if(self.redo[0] /*.length > 0*/){

            self.undo.push( self.obj.getState() );
            self.obj.putState( self.redo.pop() );

        }
        self.btnStyle();

    },

    /*
    Function: btnStyle
        Helper function to change the css style of the undo/redo buttons.
    */
    btnStyle: function(){

        var self = this, btn = self.btn;

        if(btn.undo){ btn.undo.ifClass( !self.undo[0] /*length==0*/, 'disabled'); }
        if(btn.redo){ btn.redo.ifClass( !self.redo[0] /*length==0*/, 'disabled'); }

    }

});
