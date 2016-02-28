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
/*global Class, Dialog  */
/*exported Dialog.Buttons */
/*

*/
Dialog.Buttons = new Class({

	Extends: Dialog,
	options: {
		buttons:[], //list of button labels
		//onAction: function(button-label){},
		autoClose: true
	},
	initialize: function(options){

        this.setClass('.buttons',options);
        //console.log("Dialog.Buttons",options);
		this.parent(options);
		this.setButtons( this.options.buttons );

	},
	/*
	Function: setButtons

	Arguments:
		buttons - objects with key:value as button-label:callback function

	Example:
		(start code)
			myDialog.setButtons(['Ok','Cancel']);

			//FIXME???
			myDialog.setButtons({
				Ok:function(){ callback( input.get('value') ); },
				Cancel:function(){}
			});
		(end)
	*/
	setButtons: function( buttons ){

        var self = this,
            btns = self.get('.btn-group') || 'div.btn-group'.slick().inject(self.element);

		btns.empty().adopt( buttons.map(function(b){

			return 'a.btn.btn-default.btn-sm'.slick({
				html: b.localize(),
				events:{ click: self.action.bind(self,b) }
			});
		}) );

		return self;
	}
})
