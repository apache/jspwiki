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
/*jslint forin: true, onevar: true, nomen: true, plusplus: true, immed: true */

/*
Class: Dialog
	Implementation of a simple Dialog box.
	Acts as a base class for other dialog classes.
	It is asumes mootools v1.2.4 core & more; incl. Drag.

Events:
	action - user input: click a button, make selection, ...
	open - show the dialog panel
	close - hide the dialog panel
	resize - called when dialogbox is resized

Arguments:
	options - optional, see options below

Options:
	dialog - (optional) predefined dialog DOM element/s
		When present, it overrules the caption and body elements.
	caption - (optional) DOM element
	body - (optional) DOM element
	cssClass - css class for dialog box, default is 'dialog'
	style - (optional) additional css style for the dialog box
	relativeTo - DOM element to position the dialog box
	resize - resize callback, default null which implies no resize.
	showNow - (default true) show the dialogbox at initialisation time
	draggable - (default true) make the dialogbox draggable

Events:
	onOpen- fires when the dialog is shown
	onClose - fires when the dialog is hidden
	onResize - fires when the dialog is resized

DOM Structure:
	(start code)
	<div class='dialog'>
		<div class='caption'> ... </div>
		<a class='close'>&#215<a>
		<div class='body'> ... dialog main body ... </div>
		<div class='buttons'> ... dialog buttons ... </div>
	</div>
	(end)

Example:
	(start code)
	var dialog = new Dialog({
		caption:self.ApplicationName || 'JSPWiki',
		showNow:false,
		relativeTo:$('query')
	});
	dialog.setBody( $('some-dialog') ).show();

	(end)


	(start code)
	var button = $('colorButton');
	var cd = new Dialog.Color({
		relativeTo:button,
		onChange:function(color){ $('target').setStyle('background',color);}
	});
	button.addEvent('click', cd.toggle.bind(cd));

	cd.dispose();
	(end)
*/
var Dialog = new Class({

	Implements: [Events, Options],

	options:{
		cssShow: 'show',
		styles: {},
		//dialog: DOM-element
		//caption: ''
		//body: 'innerHTML' or DOM-element
		//autoClose: false,
		//showNow: false,
		//draggable: false
		//onShow: function(){},
		//onHide: function(){},
		relativeTo: document.body
	},

	initialize:function(options){

		var self = this, el;

        //options.cssClass = '.dialog'+(options.cssClass||'');
        this.setClass('.dialog',options);
        //console.log(options.cssClass);

		this.setOptions(options);
		console.log("DIALOG Initialize ",this.options.cssClass);

		options = self.options;

		el = self.element = options.dialog || self.build(options);

		el.getElements('.close').addEvent('click', self.hide.bind(self) );

		//make dialog draggable; only possible when el is position absolute
		if( (el.getStyle('position')=='absolute') && options.draggable ){

			new Drag(el,{
				handle: (self.get('.caption') || el).setStyle('cursor','move')
			});

		}

		//CHECKME: self.resetPosition();
		self[ options.showNow ? "show": "hide"]();
	},

	toElement: function(){
		return this.element;
	},
	/*
	Function: get
		Retrieve DOM elements inside the dialog container, based on a css selector.
	Example:
	>	this.get('.body');
	>	this.get('.body li');
	*/
	get: function(selector){
		return this.element.getElement(selector);
	},

    setClass: function(clazz,options){
        options.cssClass = clazz+(options.cssClass||'');
    },

	destroy: function(){
		this.element.destroy();
	},

	show: function(){
	
	console.log("DIALOG show",this.options.cssClass,this.element.className);
		this.fireEvent('beforeOpen', this)
			.setPosition()
			.element.addClass(this.options.cssShow);
		return this.fireEvent('open', this);
	},

	hide: function(){
		this.fireEvent('beforeClose', this)
			.element.removeClass(this.options.cssShow);
		return this.fireEvent('close', this);
	},

	isVisible: function(){
		return this.element.hasClass(this.options.cssShow);
	},

	toggle: function(){
		return this[this.isVisible() ? 'hide' : 'show']();
	},

	/*
	Function: action
		Fires the ''action'' event.
		When the autoClose option is set, the dialog will also be hidden.
	*/
	action: function(value){
		//console.log('action: '+value+" close:"+self.options.autoClose);
		this.fireEvent('action', value);
		if( this.options.autoClose ){ this.hide(); }
	},

	/*
	Function: build
		Build new dialog frame based on caption and body options.
	*/
	build: function( options ){

        //console.log("DIALOG build ",options.cssClass);

		var element = this.element = [
		    'div'+options.cssClass, {styles:options.styles}, [
			    'a.close',{ html:'&#215;'},
			    'div.body'
			]
		].slick().inject(document.body);

        if( options.relativeTo ){
			//make sure to inject the dialog close to the relativeTo element
			//so that any relative positioned parent doesn't intervene
			element.inject($(options.relativeTo), 'before');
		}
		
console.log("DIALOG build ",this.element.className);
		this.setBody( options.body );
		if( options.caption ) this.setCaption( options.caption );

console.log("DIALOG build ",this.element.className);

		return element;
	},

	/*
	Function: setBody
		Set the body of the dialog box
	Arguments:
		content - string or DOM element
	Example:
		> setBody( "this is a new dialog content");
		> setBody( new Element('span',{'class','error'}).set('html','Error encountered') );
	*/
	setBody: function(content){

		var body = this.get('.body') || this.element,
		    type = typeOf(content);

		body.empty();

		if( type=='string') body.set('html',content);
		if( type=='element') body.adopt(content);
		if( type=='elements') body.adopt(content);

		return this;
	},


	setCaption: function(caption){

		var cptn = this.get('.caption') ||'div.caption'.slick().inject(this.element,'top');
			type = typeOf(caption);

		cptn.empty();

		if( type=='string') cptn.set('html',caption);
		if( type=='element') cptn.adopt(caption);

		return this;
	},

	setValue: function(value){

		console.log('DIALOG  '+value);
		return this.setBody(value);

	},

	/*
	Function: setPosition
		Moves the dialog to a specific screen position: x/y coordinates,
		relative to another DOM element or in the center of the window.
		Only works when the element is visible.

	Arguments:
		relativeTo: (optional) DOM element or an object with a getCoordinates() method
			Defaults to this.options.relativeTo or to the center of the window.
	*/
	setPosition: function( relativeTo ){

		var w = window, ws, x, y, pos,
			el = this.element;

		if(!relativeTo){ relativeTo = this.options.relativeTo; }
		pos = (relativeTo && 'getCoordinates' in relativeTo) ? relativeTo : document.id(relativeTo);
		
		if( el.getStyle('position') == 'absolute' ){

			if( pos ){

				pos = pos.getCoordinates();
				//console.log(JSON.encode(pos));
				x = pos.left; y = pos.bottom; //align at the bottom of the relativeTo element

			} else {	// center dialog box

				//todo: should be adjusted everytime the screen is resized or scrolled ?
				ws = w.getScroll();
				w = w.getSize();
				pos = el.getCoordinates();
				x = ws.x + w.x/2 - pos.width/2;
				y = ws.y + w.y/2 - pos.height/2;

			}

			el.setPosition({x:x,y:y});
			//el.morph({left: x, top:y}); -- move this to css/transition

		}
		return this;
	}

});

