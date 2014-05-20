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
Class: Dialog.Selection
	A simple selection dialog, with a list of selectable items.

Arguments:
	options - see [Dialog] object

Options:
	body - list of selectable items, defined as a string ,array or object.
	onSelect - callback function when an item is clicked
	autoClose - (default true) hide the dialog when an iten is clicked

Inherits from:
	[Dialog]

Example:
	(start code)
	new Dialog.Selection({
		body:"a|b|c|d",
		caption:"Snippet Dialog",
		autoClose:true,
		onClick:function(v){ alert('clicked '+v) }
	});
	new Dialog.Selection({
		body:[a,b,c,d],
		caption:"Snippet Dialog",
		onClick:function(v){ alert('clicked '+v) }
	});
	new Dialog.Selection({
		body:{'avalue':'a','bvalue':'b','cvalue':'c'},
		caption:"Snippet Dialog",
		onClick:function(v){ alert('clicked '+v) }
	});
	(end code)
*/
Dialog.Selection = new Class({

	Extends: Dialog,
	
	options: {
		//onAction: function(value){},
		//selected: <some value>
		cssClass: 'dialog selection',
		autoClose: true,
		items: '.body li'
	},

	initialize:function( options ){

        this.setClass('.selection',options);
        this.selected = options.selected || '';
		this.parent( options );

        console.log('Dialog.Selection ', this.element.className);
	},

	setBody: function(content){

		var self = this;
console.log('Dialog.Selection body ',content);
		if(!content){ content = self.options.body; }

		//turn 'multi|value|string' into [array]
		if( typeOf(content) == 'string' ){ content = content.split('|'); }

		//turn [array] into {object} with name:value pairs
		if( typeOf(content) == 'array' ){ content = content.associate(content); }

		//turn {object} in DOM elements (ul/li collection)
		if( typeOf(content) == 'object' ){
		
			content = 'ul'.slick({ 
				html: Object.keys(content).map( function(key){
					return '<li title="'+key+'">'+content[key]/*.trunc(36)*/+'</li>';
				})
			});
			
		}

		if( typeOf(content) == 'element' ){

			//first move the content elements into the body and highlight the selected item
			self.parent( content ).setValue( self.selected );

			//then add the click & hover event handlers
			self.getItems().addEvents({
				//click: self.action.bind(self),
				click: function(){ self.action( this); },
				mouseleave: function(){ this.removeClass('hover'); },
				mouseenter: function(){ this.addClass('hover'); }
			});
		}
		
		return self;
	},
	
	/*
	Function: setValue
		Store the selected value. (this.selected).
		And highlight the selected item (if any) 
	*/
	setValue: function(value){

		var selected = "selected", els;

		//remove previous selected item, if any 
		els = this.get('.'+selected); 
		if(els){ els.removeClass(selected); } 

		els = this.getItems('[title^='+value+']');
		if(els[0]){ els[0].addClass(selected); }

		this[selected] = value;

		return this;	
	},

	getValue: function(){
		return this.selected;	
	},

	action: function(item){
		var value = item.get('title');
		this.setValue(value).parent(value);
	},


	getItems: function( filter ){
		return this.element.getElements(this.options.items+(filter||''));
	}

});
