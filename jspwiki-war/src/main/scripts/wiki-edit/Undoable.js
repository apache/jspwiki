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
Class: UndoRedo
    Abstract class implements a simple undo/redo stack to save and restore
    the state of an "undo-able" object.
    The object needs to provide a {{getState()}} and a {{putState(obj)}} methods.
    Prior to any changes, it should fire a "beforeChange" event.
    The undo and redo event trigger the corresponding actions.

Options:
    maxundo - integer , maximal size of the undo and redo stack (default 20)
    redo - (optional) DOM element, will get a click handler to the redo() function
    undo - (optional) DOM element, will get a click handler to the undo() function

Example:
(start code)
    x = new Class({
        Implement: [Options, Undoable],
        initialize:function(...){
          ..
          this.initializeUndoable( btns, 10 );
          ..
        },
        ..
            this.fireEvent("beforeChange");
            this.fireEvent("undo");
            this.fireEvent("redo");
        ..
        getState: function(){ ... return state-object; },
        putState: function( state-object ){ ... }
(end)
*/

!(function(){

// Helper function to change the css style of the undo/redo buttons.
function btnStyle( self ){

    var btns = self.btns, disabled = "disabled";

    if( btns.undo ){ btns.undo.ifClass( !self.undo[0], disabled); }
    if( btns.redo ){ btns.redo.ifClass( !self.redo[0], disabled); }

}

// Swap the state of an undoable object.
function swap(from, to){

    from = this[from]; to = this[to];

    if( from[0] ){

        to[to.length] = this.getState(); //current state
        this.putState( from.pop() ); //new state

    }

    btnStyle( this );  //repaint buttons

}


this.Undoable = new Class({

    initializeUndoable: function(btns, maxundo){

        var self = this,
            undo = self.undo = [];

        self.redo = [];
        self.btns = btns;
        btnStyle(self); //{ redo:btns.redo, undo:btns.undo };
        maxundo = maxundo || 40;

        self.addEvents({

            //Persist the current state of the undo-able object.
            //Calls the {{this.getState()}} to retrieve the state info.
            //  state - (optional) state object to be persisted. If not present,
            //          the state will be retrieved via {{this.getState()}} function.
            beforeChange: function( state ){

                //console.log("Undoable beforeChange:", undo.length);

                undo[undo.length]= state || self.getState();
                self.redo = [];

                if( undo[maxundo] ){ undo.shift(); }

                btnStyle(self);

            },
            undo: swap.pass(["undo", "redo"], self),
            redo: swap.pass(["redo", "undo"], self)
        });

    }

});

})();
