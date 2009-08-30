/*!
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
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
Script: jspwiki-common.js
	Javascript routines to support JSPWiki, a JSP-based WikiWiki clone.

License:
	http://www.apache.org/licenses/LICENSE-2.0

Since:
	v.2.6.0

Dependencies:
	Based on http://mootools.net/ v1.11
	*	Core, Class,  Native, Element(ex. Dimensions), Window,
	*	Effects(ex. Scroll), Drag(Base), Remote, Plugins(Hash.Cookie, Tips, Accordion)

Core JS Routines:
	*	[Wiki] object (page parms, UserPrefs and setting focus)
	*	[WikiSlimbox]
	*	[SearchBox]: remember 10 most recent search topics
	*	[HighlightWords]

Core Wiki plugins, implementing JSPWiki Dynamic Styles:
	*	Wiki.registerPlugin( <js-plugin: function(dom-element, page-name){...} >)
	*	Wiki.renderPage(page-element, page-name)

	*	[WikiSlimbox] (attachment viewer)
	*	[TabbedSection] including all accordion variants
	*	[Colors], [GraphBar]
	*	[Collapsible] list and box
	*	[TableAdds] with %%sortable, %%tablefilter (excel like column filters) and %%zebra-table
	* 	[Popup] DOM based popup, replacing alert(), prompt(), confirm()

*/

/*
Global: $getText
	Global functions to get the text of a dom node
*/
function $getText(el) {
	return el.innerText || el.textContent || '';
}

/*
Global: mootools-extensions
*/
String.extend({

	/*
	Function: deCamelize
		Convert camelCase string to space-separated set of words.

	Example:
	> "CamelCase".deCamelize(); /returns "Camel Case"
	*/
	deCamelize: function(){
		return this.replace(/([a-z])([A-Z])/g,"$1 $2");
	},

	/*
	Function: trunc
		Truncate a string to a maximum length

	Arguments:
		size - maximum length of the string
		elips - (optional) the elips indicates when the string was truncacted (defaults to '...')

	Example:
	> "this is a long string".trunc(7); //returns "this is..."
	*/
	trunc: function(size, elips){

		return this.slice(0,size) + ((this.length<size) ? '' : (elips||'...'));

	},

	/*
	Function: stripScipts
		Strips the String of its <script> tags and anything in between them.

	Examples:
	> var myString = "<script>alert('Hello')</script>Hello, World.";
	> myString.stripScripts(); //Returns "Hello, World."
	*/
	stripScripts: function(){
		return this.replace(/<script[^>]*>([\s\S]*?)<\/script>/gi, '');
	}
});

/*
Function: getLast
	Returns the last item from the array.
	(credit: mootools v1.2, bugfix doesn't work when last element is '')

Examples:
> ['Cow', 'Pig', 'Dog', 'Cat'].getLast(); //returns 'Cat'
*/
//Array.extend seems not to overwrite the native getLast function
Array.prototype.getLast = function(){
	return (this.length) ? this[this.length - 1] : null;
}


Element.extend({

	/*
	Function: wraps
		This method moves this Element around its target.
		The Element is moved to the position of the passed element and becomes the parent.
		(credit: mootools v1.2)

	Arguments:
		el - (mixed) The id of an element or an Element.

	Returns:
		(element) This Element.

	Examples:
	HTML
	>	<div id="myFirstElement"></div>
	JavaScript
	>	var mySecondElement = new Element('div', {id: 'mySecondElement'});
	>	mySecondElement.wraps($('myFirstElement'));
	Resulting HTML
	>	<div id="mySecondElement">
    >		<div id="myFirstElement"></div>
	>	</div>
	*/
	wraps: function(el){
		while( el.firstChild ) this.appendChild( el.firstChild );
		el.appendChild( this ) ;
		return this;
	},

	/*
	Function: visible
		Check if the current element and all its parents are visible.

	Retuns:
		(boolean) - true when element is visible.

	Examples:
	>	$('thisElement').visible; //returns true when visible
	*/
	visible: function() {
		var el = this;
		while( $type(el)=='element' ){
			if((el.getStyle('visibility') == 'hidden') || (el.getStyle('display') == 'none' )) return false;
			el = el.getParent();
		}
		return true;
	},

	/*
	Function: hide
		Hide the element: set 'display' style to 'none'.

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').hide();
	*/
 	hide: function() {
		return this.setStyle('display','none');
	},

	/*
	Function: show
		Show the element: set 'display' style to '' (default display style)

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').show();
	*/
	show: function() {
		return this.setStyle('display','');
	},

	/*
	Function: toggle
		Toggle the 'display' style of the element.

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').toggle();
	*/
	toggle: function() {
		return this.visible() ? this.hide() : this.show();
	},

	/*
	Function: addHover
		Shortcut function to add 'hover' css class to an element.
		This allow to support hover effects on all elements, also in IE.

	Arguments
		clazz - (optional) hover class-name, default is {{hover}}

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').addHover();
	*/
	addHover: function(clazz){
		if(!clazz) clazz = 'hover';
		return this.addEvents({
			'mouseenter': function(){ this.addClass(clazz) },
			'mouseleave': function(){ this.removeClass(clazz) }
		});
	},

	/*
	Function: getDefaultValue
		Returns the default value of a form element.
		Inspired by getValue() of mootools, v1.1

	Returns:
		(element) - This Element

	Examples:
	> $('thisElement').getDefaultValue();
	*/
	getDefaultValue: function(){
		switch(this.getTag()){
			case 'select':
				var values = [];
				$each(this.options, function(option){
					if( option.defaultSelected ) values.push($pick(option.value, option.text));
				});
				return (this.multiple) ? values : values[0];
			case 'input': if (!(this.defaultChecked && ['checkbox', 'radio'].contains(this.type)) && !['hidden', 'text', 'password'].contains(this.type)) break;
			case 'textarea': return this.defaultValue;
		}
		return false;
	},

	/*
	Property: getPosition
		Returns the real offsets of the element.

	Arguments:
		overflown - optional, an array of nested scrolling containers for
		  scroll offset calculation, use this if your element is inside
		  any element containing scrollbars

	Example:
	> $('element').getPosition();

	Returns:
	> {x: 100, y:500}
	*/
	getPosition: function(overflown){
		overflown = overflown || [];
		var el = this, left = 0, top = 0;

		do {
			left += el.offsetLeft || 0;
			top += el.offsetTop || 0;
			//if( el.getStyle('position')=='relative') alert( 'found relative\n'+el.innerHTML.slice(0,40) );
			el = el.offsetParent;
		} while( el && ($(el).getStyle('position')=='static') );

		overflown.each(function(element){
			left -= element.scrollLeft || 0;
			top -= element.scrollTop || 0;
		});

		return {'x': left, 'y': top};
	}

});


/*
Class: Observer
	Class to observe a dom element for changes.

Example:
>	$(formInput).observe(function(){
>		alert('my value changed to '+this.getValue() );
>	});

*/
var Observer = new Class({
	options: {
		event: 'keyup',
		delay: 300
	},
	initialize: function(el, fn, options){
		var self = this;
		self.setOptions(options);
		self.element = el = $(el);
		self.callback = fn;
		self.timeout = null;
		self.listener = self.fired.bind(self);
		self.value = el.getValue();
		el.set({autocomplete:'off'}).addEvent(self.options.event, self.listener);
	},
	fired: function(){
		var self = this,
			el = self.element,
			value = el.value;

		if( self.value == value ) return;
		self.clear();
		self.value = value;
		self.timeout = self.callback.delay(self.options.delay, null, [el]);
	},
	clear: function(){
		this.timeout = $clear(this.timeout);
	},
	stop: function(){
		this.element.removeEvent(this.options.event, this.listener);
		this.clear();
	}
});
Observer.implement(new Options);

Element.extend({
	observe: function(fn, options){
		return new Observer(this, fn, options);
	}
});


/*
Global: LocalizedString
	The __LocalizedStrings__ object is the store of all {{name:value}} pairs
	with the localisation values.
	The name of each entry starts with the prefix {{javascript}}.
	The value of each entry may contain parameters like this: {{ {0},{1},... }}

Examples:
	(start code)
	LocalizedStrings = {
		"javascript.some.resource.key":"localised resource key {0}" ,
		"javascript.moreInfo":"More",
		"javascript.imageInfo":"Image {0} of {1}"
	}
	(end)

*/
var LocalizedStrings = LocalizedStrings || [];

/*
Function: localize
	Localize a string; with or without parameters.
	Uses the [LocalideString] global hash.

Examples:
>	"moreInfo".localize() =='More';
>	"imageInfo".localize(2,4); => "Image {0} of {1}" becomes "Image 2 of 4

*/
String.extend({
	localize: function(){
		var s = LocalizedStrings["javascript."+this],
			args = arguments;

		if(!s) return("???" + this + "???");

		return s.replace(/\{(\d)\}/g, function(m){
			return args[m.charAt(1)] || "???"+m.charAt(1)+"???";
		});
	}
});


/*
Class: Wiki
	The main javascript class to support basic jspwiki functions.
*/
var Wiki = {

	/*
	Function: initialize
		Initialize main Wiki properties.

	It reads all the "meta" dom elements, prefixed with "wiki",
	such as
	* wikiContext : jspwiki requestcontext variable (view, edit, info, ...)
	* wikiBaseUrl
	* wikiPageUrl: page url template with dummy pagename "%23%24%25"
	* wikiEditUrl : edit page url
	* wikiJsonUrl : JSON-RPC url
	* wikiPageName : pagename without blanks
	* wikiUserName
	* wikiTemplateUrl : path of the jsp template
	* wikiApplicationName

	It parses the {{JSPWikiUserPrefs}} cookie.

	All registered js plugins are invoked for both the main page and
	the favorites block.

	When the 'referrer' url (previous page) contains a "section=" parameter,
	the wiki page will be scrolled to the right section.

	*/
	initialize: function(){

		var self = this,
			host = location.host;

		if(self.prefs) return; //already initialised

		// read all meta elements starting with wiki
		$$('meta').each( function(el){
			var n = el.getProperty('name') || '';
			if( n.indexOf('wiki') == 0 ) this[n.substr(4)] = el.getProperty('content');
		}, self);

		self.BasePath = (self.BaseUrl) ?
			self.BaseUrl.slice(self.BaseUrl.indexOf(host)+host.length,-1) : '';

		// if JSPWiki is installed in the root, then we have to make sure that
		// the cookie-cutter works properly here.
		if(self.BasePath == '') self.BasePath = '/';

		self.prefs = new Hash.Cookie('JSPWikiUserPrefs', {path:Wiki.BasePath, duration:20});

		self.allowEdit = !!$E('a.edit'); //deduct permission level
		self.url = null;
		self.parseHash.periodical(500);

		/* reusable dialog object for alert, prompt, confirm */
		//FIXME: lazy creation ???
		self.dialog = new Dialog({
			caption:self.ApplicationName || 'JSPWiki',
			showNow:false,
			relativeTo:$('query')
		});

		//FIXME
		self.makeMenuFx('morebutton', 'morepopup');
		self.addEditLinks();

		self.renderPage( $('page'), Wiki.PageName);
		self.renderPage( $('favorites'), "Favorites");

		//self.addCollapsableFavs();
		self.splitbar();

		//jump back to the section previously being edited
		if( document.referrer.test( /\&section=(\d+)$/ ) ){
			var section = RegExp.$1.toInt(),
				els = this.getSections();
			if( els && els[section] ){
				var el = els[section];
				window.scrollTo( el.getLeft(), el.getTop() );
			}
		}

		SearchBox.initialize();
		HighlightWord.initialize();
		self.setFocus();
	},

	/*
	Function: getSections
		Returns the list of all section headers, excluding the header of the
		Table Of Contents.
	*/
	getSections: function(){
		return $$('#pagecontent *[id^=section]').filter(
			function(item){ return(item.id != 'section-TOC') }
		);
	},

	/*
	Function: alert
		Show the alert popup. Any html fragment can be displayed.

	Arguments:
		msg - html text fragment

	Example:
	> Wiki.alert( "alert message");
	*/
	alert: function(msg){
		//return alert(msg); //standard js

		this.dialog
			.setBody( new Element('p').setHTML(msg) )
			.setButtons({ Ok:Class.empty })
			.show();
	},

	/*
	Function: confirm
		Replaces the standard confirm dialog, supporting any html fragment.
		If the user clicks "OK", the box returns true.
		If the user clicks "Cancel", the box returns false.
		The return value (true/false) is handled via the callback function.

	Example:
	> Wiki.confirm("sometext", callback-function(true/false) );
	*/
	confirm: function(msg, callack){
		//return callback( confirm(msg) ); //standard js

		this.dialog
			.setBody( new Element('p').setHTML(msg) )
			.setButtons({
				Ok:function(){ callback(true); },
				Cancel:function(){ callback(false); }
			})
			.show();
	},

	/*
	Function: prompt
		Show the prompt prompt, with standard 'Ok' and 'Cancel' buttons.
		Replaces the standard prompt handling, supporting any html fragment.
		The return value is handled via the callback function.

	Arguments:
		msg - html text fragment
		defaultreply - (string) default answer
		callback - (function) invoke when pressing the Ok button

	Example:
		> Wiki.prompt("sometext","defaultvalue", callback-function(result) );
	*/
	prompt: function(msg, defaultreply, callback){
		//return callback( prompt( msg, defaultreply ) ); //standard js

		var input;

		this.dialog.setBody([
				new Element('p').setHTML(msg),
				new Element('form').adopt(
					input = new Element('input',{
						name:'prompt',
						type:'text',
						value: defaultreply,
						size: 24
					})
				)
			])
			.setButtons({
				Ok:function(){ callback( input.getValue() ) },
				Cancel:Class.empty
			})
			.show();
		input.focus();
	},

	/*
	Function: registerPlugin
		Register a jspwiki javascript plugin.
	*/
	registerPlugin: function(fn){
		if(!this.plugins) this.plugins = [];
		this.plugins.push(fn);
	},

	/*
	Function: renderPage
		Invoke all registered wiki js plugins.

	Arguments:
		page - dom element
		name - wiki page of the page (pagename, or 'Favorites)
	*/
	renderPage: function(page, name){
		if( page ){
			this.plugins.each( function(obj){
				//obj(page, name);
				($type(obj)=='function') ? obj(page, name) : obj.render(page, name);
			});
		}
	},

	/*
	Function: setFocus
		Set the focus of certain form elements, depending on the context of the page.
		Protect agains IE6: you can't set the focus on invisible elements.
	*/
	setFocus: function(){
		/* plain.jsp,   login.jsp,   prefs/profile, prefs/prefs, find */
		['editorarea','j_username','loginname','assertedName','query2'].some(function(el){
			el = $(el);
			return el && el.visible() ? !!el.focus() : false;
		});
	},

	/*
	Property: toUrl
		Turn a wiki pagename into a full wiki-url
	*/
	toUrl: function(pagename){

		return this.PageUrl.replace(/%23%24%25/, pagename);

	},

	/*
	Property: toPageName
		Parse a wiki-url and return the corresponding wiki pagename
	*/
	toPageName: function(url){

		var s = this.PageUrl.escapeRegExp().replace(/%23%24%25/, '(.+)'),
			res = url.match( new RegExp(s) );
		return (res ? res[1] : false);

	},

	/*
	Property: cleanPageName
		Remove all not-allowed chars from a *candidate* pagename.
		Trim repeated whitespace, allow letters, digits and
		following punctuation chars: ()&+,-=._$
		Ref com.ecyrd.jspwiki.parser.MarkupParser.cleanPageName()
	*/
	cleanPageName: function(p){

		return p.trim().replace(/\s+/g,' ')
				.replace(/[^A-Za-z0-9()&+,-=._$ ]/g, '');

	},

	/*
	Function: makeMenuFx
		Make hover menu with fade effect.
	*/
	makeMenuFx: function(btn, menu){
		btn = $(btn);
		menu = $(menu);
		if(!btn || !menu) return;

		var	popfx = menu.effect('opacity', {wait:false}).set(0);
		btn.adopt(menu).set({
			href:'#',
			events:{
				'mouseout': function(){ popfx.start(0) },
				'mouseover': function(){ Wiki.locatemenu(btn,menu); popfx.start(0.9) }
			}
		});
	},

	/*
	Function: locatemenu
		TODO
	*/
	locatemenu: function(base,el){

		var win = {'x': window.getWidth(), 'y': window.getHeight()},
			scroll = {'x': window.getScrollLeft(), 'y': window.getScrollTop()},
			corner = base.getPosition(),
			offset = {'x': base.offsetWidth-el.offsetWidth, 'y': base.offsetHeight },
			popup = {'x': el.offsetWidth, 'y': el.offsetHeight},
			prop = {'x': 'left', 'y': 'top'},
			z, pos;

		for( z in prop ){
			pos = corner[z] + offset[z]; /*top-left corner of base */
			if ((pos + popup[z] - scroll[z]) > win[z]) pos = win[z] - popup[z] + scroll[z];
			el.setStyle(prop[z], pos);
		};
	},

	/*
	Function: parseHash
		TODO: periodic screening of the #hash to ensure all screen sections are displayed properly

	FIXME:
		add handling of BACK button for tabs ??
	*/
	parseHash: function(){

		if(this.url && this.url == location.href ) return;
		this.url = location.href;
		var h = location.hash;
		if( !h || h=='' ) return;
		h = $( h.slice(1) );

		while( $type( h ) == 'element' ){

			if( h.hasClass('hidetab') ){

				TabbedSection.click.apply($('menu-' + h.id));

			} else if( h.hasClass('tab') ){

				/* accordion -- need to find accordion toggle object */
				h.fireEvent('onShow');

			} else if( h.hasClass('collapsebody') ){

				/* collapsible box -- need to find the toggle button */

			} else if( h.hasClass('collapsebox') ){

				var bullet = h.getFirst();
				if(bullet) bullet.fireEvent('click');

			} else if( !h.visible() ){
				//alert('not visible'+el.id);
				//fixme need to find the correct toggler
				//el.show(); //eg collapsedBoxes: fixme
			}
			h = h.getParent();
		}

		location = location.href; /* now jump to the #hash */
	},

	/*
	Function: SubmitOnce
		Disable all form buttons to avoid double submits.
		At the start, overwrite any {{onbeforeunload}} handler installed by
		eg. the WikiEdit class.

	Fixme:
		Replaced by stripes in v3.0 ??
	*/
	submitOnce: function(form){

		window.onbeforeunload = null; /* regular exit of this page -- see jspwiki-edit.js */
		(function(){
			$A(form.elements).each(function(e){
				if( (/submit|button/i).test(e.type)) e.disabled = true;
			});
		}).delay(10);

		return true;
	},

	/*
	Function: submitUpload
		Support for the upload progressbar.
		Attached to the attachment upload submit form.

	Returns:
		Returns via the submitOnce() function.
	*/
	submitUpload: function(form, progress){

		var self = this,
			bar = $('progressbar').setStyle('visibility','visible');

		self.progressbar = self.jsonrpc.periodical(
			1000,
			self,
			["progressTracker.getProgress",[progress],function(result){
				result = result.stripScripts(); //xss vulnerability
				if(!result.code) bar.getFirst().setStyle('width',result+'%').setHTML(result+'%');
			}]
		);
		return self.submitOnce(form);
	},

	/*
	Property: splitbar
		Add a toggle bar next to the main page content block, to
		show/hide the favorites block with a click of the mouse.
		The open/close status is saved in the Wiki.prefs cookie.
		When the user hovers the mouse over the toggle bar, an arrow
		image is shown to indicate the collapsable effect.

	Note:
		The toggle bar has css-id 'collapseFavs'.
		The toggle bar gets a .hover class when the mouse hovers over it.
		The mouse-pointer image has css-id 'collapseFavsPointer'.
		The DOM body gets a .fav-slide class when the favorites are collapsed (hidden)


	DOM structure:
	(start code)
	<div id='content'>
		<div id='page'>
			<div class='tabmenu'> ... </div>
			<div class='tabs'>
				<div class='splitbar'></div> <== injected splitbar
				<div id='pagecontent'> ... </div>
				<div id='attach'> ... </div>
				<div id='info'> ... </div>
			</div>
		</div>
		<div id='favorites'> ... </div>
	</div>
	(end)
	*/
	splitbar: function(){

		//inject toggle block :: can be done at server already
		var splitbar = 'splitbar',
			pref = 'hidefav',
			body = $E('body'),
			pointer = new Element('div', {'id':'splitPointer'}).inject(body),
			pointerFn = function(e){
				this.addClass('hover');
				pointer.setStyles({ left:$('page').getPosition().x, top: (e.pageY || e.clientY) }).show();
			};

		// The cookie is not saved to the server's Preferences automatically, (HTTP GET)
		// so the body class will not be set yet.
		// Should better move server side, for faster rendering. wf-stripes
		body.addClass( Wiki.prefs.get( pref )||'' );

		new Element('div',{
			'class':splitbar,
			'events':{
				'click': function(){
					body.toggleClass( pref );
					Wiki.prefs.set( pref, body.hasClass(pref) ? pref:'' );
				},
				'mouseenter': pointerFn,
				'mousemove': pointerFn,
				'mouseleave': function(){ this.removeClass('hover'); pointer.hide() }
			}
		}).injectTop( $('page') );

	},

	addCollapsableFavs: function(){

		var body = $('wikibody'),
			$pref = 'fav-slide',
			pref = Wiki.prefs.get('ToggleFav'),
			tabs = $E('#page .tabs');

		if( !tabs ) return;

		// The cookie is not saved to the server's Preferences automatically,
		// so the body class will not be set yet.
		// Should better move server side, for faster rendering. wf-stripes
		(pref==$pref) ? body.addClass($pref) : body.removeClass($pref);

		/* Be careful.
		   The .tabs block can not be made relative, cause then the .tabmenu doesn't
		   stack properly with the .tabs block.
		   Therefore, insert a relative DIV container to contain the clickable vertical bar.
		   TODO: needs probably another IE hack ;-)
		 */

		tabs = new Element('div', {
			'styles': {
				'position':'relative',
				'padding': tabs.getStyle('padding') // take over padding from original .tabs
			}
		}).wraps(tabs.setStyle('padding','0'));

		var pointer = new Element('div', {'id':'collapseFavsPointer'}).hide().inject(body),
			movePointer = function(e){
				this.addClass('hover');
				pointer.setStyles({ left:this.getPosition().x, top: (e.pageY || e.clientY) }).show();
			};

		new Element('div', {
			'id':'collapseFavs',
			'events': {
				'click': function(){
					body.toggleClass($pref);
					Wiki.prefs.set('ToggleFav', body.hasClass($pref) ? $pref:'' );
				},
				'mouseenter': movePointer,
				'mousemove': movePointer,
				'mouseleave': function(){
					this.removeClass('hover');
					pointer.hide();
				}
			}
		}).injectTop(tabs);

	},

	// fixme: should move server side
	addEditLinks: function(){


//fixme: SectionEditing is not properly save when updating userprefs !
//alert(this.prefs.keys()+"\n"+this.prefs.values());

		if( $("previewcontent") || !this.allowEdit || this.prefs.get('SectionEditing') != 'on') return;

		var url = this.EditUrl;
		url = url + (url.contains('?') ? '&' : '?') + 'section=';

		var aa = new Element('a',{'class':'editsection'}).setHTML('quick.edit'.localize()),
			i = 0;

		this.getSections().each( function(el){
			el.adopt(aa.set({'href':url + i++ }).clone());
		});

	},

	/*
	Function: ajax
		todo
		FIXME: to be refactored based on Stripes approach to AJAX.

	Arguments:

	Options:
		action - stripes event name
		params - DOM-element or js-object. Will be converted to a query-string

	Example:
		(start code)
		Wiki.ajax('Search.jsp', {
			action:'ajaxSearch',
			params:{query:'some-text', maxItems:20},
			update:$('dom-element'),
			onComplete:function(){
				alert('ajax - done');
			}
		});
		(end)
	*/
	ajax: function(url, options){

		//FIXME: under contstruction
		var params = options.action+'=&'+options.params.toQueryString();

  		new Ajax( url, {
			postBody: params,
			method: 'post',
			update: options.update,
			onComplete: function( result ){
				options.onComplete( result );
			}
		}).request();

	},

	/*
	Function: jsonrpc
		Generic json-rpc routines to talk to the jspwiki-engine backend.
		FIXME: to be refactored based on Stripes approach to AJAX.

	Note:
		Uses the JsonUrl which is read from the page as meta tag
		{{{ <meta name="wikiJsonUrl" content='/JSPWiki-pipo/JSON-RPC' /> }}}

	Supported rpc calls:
		- {{search.findPages}} gets the list of pagenames with partial match
		- {{progressTracker.getProgress}}
		- {{search.getSuggestions}} gets the list of pagenames with partial match

	Example:
		(start code)
		Wiki.jsonrpc('search.findPages', ['Janne',20], function(result){
			//do something with the result json object
		});

		//stripes approach
		Wiki.jsonrpc('Search.jsp', {
			action: 'findPages',
			params: js-object,
			onComplete: function(result){
			...json result object...
		});

		(end)
	*/
	$jsonid : 10000,
	jsonrpc: function(method, params, fn){

		if(this.JsonUrl){

			new Ajax(this.JsonUrl, {
				postBody: Json.toString({
					'id':this.$jsonid++,
					'method':method,
					'params':params
				}),
				method: 'post',
				onComplete: function(result){
					var r = Json.evaluate(result,true);
					fn(r.result || r.error || null);
					/*if( r ){
						if(r.result){ fn(r.result) }
						else if(r.error){ fn(r.error) }
					}*/
				}
			}).request();

		}
	}

}



/*
Plugin: WikiSlimbox

Credits:
	Inspired by Slimbox by Christophe Bleys. (see http://www.digitalia.be/software/slimbox)

Example:
	> %%slimbox [...] %%
	> %%slimbox-img  [some-image.jpg] %%
	> %%slimbox-ajax [some-page links] %%
*/
Wiki.registerPlugin( function(page,name){
	var i = 0,
		lnk = new Element('a',{'class':'slimbox'}).setHTML('&raquo;');

	$ES('*[class^=slimbox]',page).each(function(slim){
		var group = 'lightbox'+ i++,
			parm = slim.className.split('-')[1] || 'img ajax',
			filter = [];
		if(parm.test('img')) filter.extend(['img.inline', 'a.attachment']);
		if(parm.test('ajax')) filter.extend(['a.wikipage', 'a.external']);

		$ES(filter.join(','),slim).each(function(el){
			var href = el.src||el.href,
				rel = (el.className.test('inline|attachment')) ? 'img' : 'ajax';

			if((rel=='img') && !href.test('(.bmp|.gif|.png|.jpg|.jpeg)(\\?.*)?$','i')) return;

			lnk.clone().setProperties({
				'href':href,
				'rel':group+' '+rel,
				'title':el.alt||el.getText()
			}).injectAfter(el);//.injectBefore(el);

			if(el.src) el.replaceWith(new Element('a',{
				'class':'attachment',
				'href':el.src
			}).setHTML(el.alt||el.getText()));
		});
	});
	if(i) Lightbox.init();
	//new Asset.javascript(Wiki.TemplateUrl+'scripts/slimbox.js');
})


/*
Class: Slimbox
	Slimbox v1.31 - The ultimate lightweight Lightbox clone
	by Christophe Beyls (http://www.digitalia.be) - MIT-style license.
	Inspired by the original Lightbox v2 by Lokesh Dhakar.

	Updated by Dirk Frederickx to fit JSPWiki needs
	- minimum size of image canvas
	- add maximum size of image w.r.t window size
	- CLOSE icon -> close x text iso icon
	- {{prev, next}} links added in visible part of screen
	- add size of picture to info window
	- spacebar, down arrow, enter : next image
	- up arrow : prev image
	- allow the same picture occuring several times
	- add support for external page links  => slimbox_ex
*/
var Lightbox = {

	init: function(options){
		this.options = $extend({
			resizeDuration: 400,
			resizeTransition: false, /*Fx.Transitions.sineInOut,*/
			initialWidth: 250,
			initialHeight: 250,
			animateCaption: true,
			errorMessage: "slimbox.error".localize()
		}, options || {});

		this.anchors=[];
		$each(document.links, function(el){
			if (el.rel && el.rel.test(/^lightbox/i)){
				el.onclick = this.click.pass(el, this);
				this.anchors.push(el);
			}
		}, this);
		this.eventKeyDown = this.keyboardListener.bindAsEventListener(this);
		this.eventPosition = this.position.bind(this);

		/*	Build float panel
			<div id="lbOverlay"></div>
			<div id="lbCenter">
				<div id="lbImage">
					<!-- img or iframe element is inserted here -->
				</div>
			</div>
			<div id="lbBottomContainer">
				<div id="lbBottom">
					<div id="lbCaption">
					<div id="lbNumber">
					<a id="lbCloseLink"></a>
					<div style="clear:both;"></div>
				</div>
			</div>
		*/
		this.overlay = new Element('div', {'id': 'lbOverlay'}).inject(document.body);

		this.center = new Element('div', {'id': 'lbCenter', 'styles': {'width': this.options.initialWidth, 'height': this.options.initialHeight, 'marginLeft': -(this.options.initialWidth/2), 'display': 'none'}}).inject(document.body);
		new Element('a', {'id': 'lbCloseLink', 'href':'#', 'title':'slimbox.close.title'.localize()}).inject(this.center).onclick = this.overlay.onclick = this.close.bind(this);
		this.image = new Element('div', {'id': 'lbImage'}).inject(this.center);

		this.bottomContainer = new Element('div', {'id': 'lbBottomContainer', 'styles': {'display': 'none'}}).inject(document.body);
		this.bottom = new Element('div', {'id': 'lbBottom'}).inject(this.bottomContainer);
		//new Element('a', {'id': 'lbCloseLink', 'href': '#', 'title':'slimbox.close.title'.localize()}).setHTML('slimbox.close'.localize()).inject(this.bottom).onclick = this.overlay.onclick = this.close.bind(this);
		this.caption = new Element('div', {'id': 'lbCaption'}).inject(this.bottom);

		var info = new Element('div').inject(this.bottom);
		this.prevLink = new Element('a', {'id': 'lbPrevLink', 'href': '#', 'styles': {'display': 'none'}}).setHTML("slimbox.previous".localize()).inject(info);
		this.number = new Element('span', {'id': 'lbNumber'}).inject(info);
		this.nextLink = this.prevLink.clone().setProperties({'id': 'lbNextLink' }).setHTML("slimbox.next".localize()).inject(info);
		this.prevLink.onclick = this.previous.bind(this);
		this.nextLink.onclick = this.next.bind(this);

 		this.error = new Element('div').setProperty('id', 'lbError').setHTML(this.options.errorMessage);
		new Element('div', {'styles': {'clear': 'both'}}).inject(this.bottom);

		var nextEffect = this.nextEffect.bind(this);
		this.fx = {
			overlay: this.overlay.effect('opacity', {duration: 500}).hide(),
			resize: this.center.effects($extend({duration: this.options.resizeDuration, onComplete: nextEffect}, this.options.resizeTransition ? {transition: this.options.resizeTransition} : {})),
			image: this.image.effect('opacity', {duration: 500, onComplete: nextEffect}),
			bottom: this.bottom.effect('margin-top', {duration: 400, onComplete: nextEffect})
		};

		this.fxs = new Fx.Elements([this.center, this.image], $extend({duration: this.options.resizeDuration, onComplete: nextEffect}, this.options.resizeTransition ? {transition: this.options.resizeTransition} : {}));

		this.preloadPrev = new Image();
		this.preloadNext = new Image();
	},

	click: function(link){
		var rel = link.rel.split(' ');
		if (rel[0].length == 8) return this.open([[url, title, rel[1]]], 0);

		var imageNum=0, images = [];
		this.anchors.each(function(el){
			var elRel = el.rel.split(' ');
			if (elRel[0]!=rel[0]) return;
			if((el.href==link.href) && (el.title==link.title)) imageNum = images.length;
			images.push([el.href, el.title, elRel[1]]);
		});
		return this.open(images, imageNum);
	},

	open: function(images, imageNum){
		this.images = images;
		this.position();
		this.setup(true);
		this.top = window.getScrollTop() + (window.getHeight() / 15);
		this.center.setStyles({top: this.top, display: ''});
		this.fx.overlay.start(0.7);
		return this.changeImage(imageNum);
	},

	position: function(){
		this.overlay.setStyles({top: window.getScrollTop(), height: window.getHeight()});
	},

	setup: function(open){
		var elements = $A(document.getElementsByTagName('object'));
		elements.extend(document.getElementsByTagName(window.ie ? 'select' : 'embed'));
		elements.each(function(el){
			if (open) el.lbBackupStyle = el.style.visibility;
			el.style.visibility = open ? 'hidden' : el.lbBackupStyle;
		});
		var fn = open ? 'addEvent' : 'removeEvent';
		window[fn]('scroll', this.eventPosition)[fn]('resize', this.eventPosition);
		document[fn]('keydown', this.eventKeyDown);
		this.step = 0;
	},

	keyboardListener: function(event){
		switch (event.keyCode){
			case 27: case 88: case 67: this.close(); break;
			case 37: case 38: case 80: this.previous(); break;
			case 13: case 32: case 39: case 40: case 78: this.next(); break;
			default: return;
		}
		new Event(event).stop();
	},

	previous: function(){
		return this.changeImage(this.activeImage-1);
	},

	next: function(){
		return this.changeImage(this.activeImage+1);
	},

	changeImage: function(imageNum){
		if (this.step || (imageNum < 0) || (imageNum >= this.images.length)) return false;
		this.step = 1;
		this.activeImage = imageNum;

		this.center.style.backgroundColor = '';
		this.bottomContainer.style.display = this.prevLink.style.display = this.nextLink.style.display = 'none';
		this.fx.image.hide();
		this.center.className = 'lbLoading';

		this.preload = new Image();
		this.image.empty().setStyle('overflow','hidden');
		if( this.images[imageNum][2] == 'img' ){
			this.preload.onload = this.nextEffect.bind(this);
			this.preload.src = this.images[imageNum][0];
		} else if( this.images[imageNum][2] == 'element' ){
			/**/
			this.so = this.images[imageNum][0];
			this.so.setProperties({
				width: '120px',
				height: '120px'
			});
			this.so.inject(this.image);
			this.nextEffect();
		} else {
			this.iframeId = "lbFrame_"+new Date().getTime();	// Safari would not update iframe content that has static id.
			this.so = new Element('iframe').setProperties({
				id: this.iframeId,
//				width: this.contentsWidth,
//				height: this.contentsHeight,
				frameBorder:0,
				scrolling:'auto',
				src:this.images[imageNum][0]
			}).inject(this.image);
			this.nextEffect();	//asynchronous loading?
		}
		return false;
	},

	ajaxFailure: function (){
		this.ajaxFailed = true;
		this.image.setHTML('').adopt(this.error.clone());
		this.nextEffect();
	},

	nextEffect: function(){
		switch (this.step++){
		case 1:
			this.center.className = '';
			this.caption.empty().adopt(new Element('a', {
					'href':this.images[this.activeImage][0],
					'title':"slimbox.directLink".localize()
				}).setHTML(this.images[this.activeImage][1] || ''));

			var type = (this.images[this.activeImage][2]=='img') ? "slimbox.info" : "slimbox.remoteRequest";
			this.number.setHTML((this.images.length == 1) ? '' : type.localize(this.activeImage+1, this.images.length));
			this.image.style.backgroundImage = 'none';

			var w = Math.max(this.options.initialWidth,this.preload.width),
				h = Math.max(this.options.initialHeight,this.preload.height),
				ww = Window.getWidth()-10,
				wh = Window.getHeight()-120;
			//if(this.images[this.activeImage][2]!='img' &&!this.ajaxFailed){ w = 6000; h = 3000; }
			if(w > ww) { h = Math.round(h * ww/w); w = ww; }
			if(h > wh) { w = Math.round(w * wh/h); h = wh; }

			this.image.style.width = this.bottom.style.width = w+'px';
			this.image.style.height = /*this.prevLink.style.height = this.nextLink.style.height = */ h+'px';

			if( this.images[this.activeImage][2]=='img') {
				this.image.style.backgroundImage = 'url('+this.images[this.activeImage][0]+')';

				if (this.activeImage) this.preloadPrev.src = this.images[this.activeImage-1][0];
				if (this.activeImage != (this.images.length - 1)) this.preloadNext.src = this.images[this.activeImage+1][0];

				this.number.setHTML(this.number.innerHTML+'&nbsp;&nbsp;['+this.preload.width+'&#215;'+this.preload.height+']');
			} else {
				this.so.style.width=w+'px';
				this.so.style.height=h+'px';
			}

			if (this.options.animateCaption) this.bottomContainer.setStyles({height: '0px', display: ''});

			this.fxs.start({
				'0': { height: [this.image.offsetHeight], width: [this.image.offsetWidth], marginLeft: [-this.image.offsetWidth/2] },
				'1': { opacity: [1] }
			});

			break;
		case 2:
			//this.center.style.backgroundColor = '#000';
			this.image.setStyle('overflow','auto');
			this.bottomContainer.setStyles({ top: (this.top + this.center.clientHeight)+'px', marginLeft: this.center.style.marginLeft });
			if (this.options.animateCaption){
				this.fx.bottom.set(-this.bottom.offsetHeight);
				this.bottomContainer.style.height = '';
				this.fx.bottom.start(0);
				break;
			}
			this.bottomContainer.style.height = '';
		case 3:
			if (this.activeImage) this.prevLink.style.display = '';
			if (this.activeImage != (this.images.length - 1)) this.nextLink.style.display = '';
			this.step = 0;
		}
	},

	close: function(){
		if (this.step < 0) return;
		this.step = -1;
		if (this.preload){
			this.preload.onload = Class.empty;
			this.preload = null;
		}
		for (var f in this.fx) this.fx[f].stop();
		this.center.style.display = this.bottomContainer.style.display = 'none';
		this.fx.overlay.chain(this.setup.pass(false, this)).start(0);
		this.image.empty();
		return false;
	}
};


/*
Class: TabbedSection
	Creates tabs, based on some css-class information
	Use in jspwiki:
	> %%tabbedSection
	> 	%%tab-FirstTab .. %%
	> /%

	Alternative syntax based on header markup
	> %%tabbedSection
	> 	!# First tab title ..
	> 	!# Second tab title ..
	> /%


Example:
	(start code)
	<div class="tabbedSection">
		<div class="tab-FirstTab">..<div>
		<div class="tab-SecondTab">..<div>
	</div>
	(end)
	is changed into
	(start code)
	<div class="tabmenu"><span><a activetab>..</a></span>..</div>
	<div class="tabbedSection tabs">
		<div class="tab-firstTab ">
		<div class="tab-SecondTab hidetab">
	</div>
	(end)
*/
var TabbedSection = {

	render: function(page, name){

		// add click handlers to existing tabmenu's, generated by <wiki:tabbedSection>
		$ES('.tabmenu a',page).each(function(el){
			if(!el.href) el.addEvent('click', this.click);
		},this);

		// convert tabbedSections into tabmenu's with click handlers
		$ES('.tabbedSection',page).each( function(tt){
			if(tt.hasClass('tabs')) return;
			tt.addClass('tabs'); //css styling on tabs

			var menu = new Element('div',{'class':'tabmenu'}).injectBefore(tt);

			/*
			var menuItems = this.getTabs(tt);
			for( var m in menuItems ){
				new Element('a', {
					//'id':'menu-'+tab.id,
					'class':(i==0) ? 'activetab' : '',  //fixme
					'events':{
						'click': this.click(this,m)
					}
				}).appendText( menuItems[m] ).inject(menu);
			}
			*/
			tt.getChildren().each(function(tab,i) {
				//find nested %%tab-XXX
				var clazz = tab.className;
				if( !clazz.test('^tab-') ) return;

				if( !tab.id || (tab.id=="") ) tab.id = clazz; //unique id

				(i==0) ? tab.removeClass('hidetab') : tab.addClass('hidetab');

				new Element('div',{'class':'clearbox'}).inject(tab);

				var title = clazz.substr(4).deCamelize(); //drop 'tab-' prefix
				new Element('a', {
					'id':'menu-'+tab.id,
					'class':(i==0) ? 'activetab' : '',
					'events':{ 'click': this.click }
				}).appendText(title).inject(menu);

			},this);
		}, this);
	},

	/*
	Function: getTabs
		Helper function for TabbedSection. Also used by Accordion.
		It will scan the
		modifies DOM to ensure each TAB section is conform <div class="tab">...</div>
		return a set of menu-items : {id:'menu1-title', id:'menu2-title'}

		(1) %%tab-tabName : <div class="tab">...</div>
		(2) !# tab name: <div class="tab"><h2>tab name</h2> ... </div>

	Arguments:
		el - DOM content container

	Returns:
		{id:'menu1 title', id:'menu2 title', ...} or null
	*/
	$id: 1000,
	getTabs : function( tt ){
		var tabs = [], titles=[];

		var isTabClass = true, isTabHeads = true;


		el.getChildren().each(function(tab) {

			if( !tab.className.test('^tab-') ) return;
			var clazz = tab.className;
			if( !clazz.test('^tab-') ) return;

			if( !tab.id || (tab.id=="") ) tab.id = clazz; //unique id

			(i==0) ? tab.removeClass('hidetab') : tab.addClass('hidetab');

			new Element('div',{'class':'clearbox'}).inject(tab);

			var title = clazz.substr(4).deCamelize(); //drop 'tab-' prefix

			//FIXME use class to make tabs visible during printing
			//(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

			var title = tab.className.substr(4).deCamelize(),
				t = toggle.clone().appendText(title);
			if(togglemenu) {
				toggles.push(t.inject(togglemenu));
			} else {
				toggles.push(t.adopt(bullet.clone()).injectBefore(tab));
			}
			titles.push(title);
			tabs.push(tab.addClass('tab'));
		});

		return {'titles':titles,'tabs':tabs};


/**tabbed-section*******/
			if(tt.hasClass('tabs')) return;

			el.addClass('tabs'); //css styling on tabs contents

			var tt = this.getTabs(el);
			var menu = new Element('div',{'class':'tabmenu'}).injectBefore(tt);
			var toggle = new Element('a',{'events':{ 'click': this.click }});

			tt.titles.each(function(title,i) {
				var t = toggle.clone().appendText(title).inject(menu);
				if(i==0) t.addClass('activetab');
				t.id='menu-'+tt.tabs[i].id;
			},this);


/**accordion*******/

			var toggles=[], menu=false;

			/*if(tt.hasCalss('accordion')){ no separate menu block needed } */
			if(tt.hasClass('tabbedAccordion')){ menu = 'togglemenu'; }
			else if(tt.hasClass('leftAccordion')){ menu = 'sidemenu left'; }
			else if(tt.hasClass('rightAccordion')){	menu = 'sidemenu right'; }

			if(menu) menu = new (Element('div'),{'class':menu }).injectBefore(tt);

			var tt = this.getTabs(el);
			/*build togglemenu*/
			tt.titles.each( function(title,i){
				var t = toggle.clone().appendText(title);
				menu ? t.inject(menu) : t.adopt(bullet.clone()).injectBefore(tt.tabs[i]);
				toggles.push(t);
			});
			new Accordion(toggles, tt.tabs, { /* ... */});

	},

	click: function(){
		var menu = $(this).getParent(),
			tabs = menu.getNext();

		menu.getChildren().removeClass('activetab');
		this.addClass('activetab');

		//skip possible relative wrapper element
		var rel = tabs.getFirst();
		if(rel.getStyle('position')=='relative') tabs = rel;

		tabs.getChildren().addClass('hidetab');

		//fixme: id needs to be unique , should not be the TAB name
		tabs.getElementById( this.id.slice(5) ).removeClass('hidetab');
	}

}
Wiki.registerPlugin( TabbedSection );
//FIXME: convert to class
//Wiki.registerPlugin( function(page,name){


/*
Class: SearchBox
 * FIXME: remember 10 most recent search topics (cookie based)
 * Extended with quick links for view, edit and clone (ref. idea of Ron Howard - Nov 05)
 * Refactored for mootools, April 07
*/
var SearchBox = {

	initialize: function(){
		this.onPageLoadQuickSearch();
		this.onPageLoadFullSearch();
	},

	onPageLoadQuickSearch : function(){
		var q = $('query'); if( !q ) return;
		this.query = q;
		q.observe(this.ajaxQuickSearch.bind(this) );

		this.hover = $('searchboxMenu').setProperty('visibility','visible')
			.effect('opacity', {wait:false}).set(0);

		$(q.form).addEvent('submit',this.submit.bind(this))
			//FIXME .addEvent('blur',function(){ this.hasfocus=false; this.hover.start(0) }.bind(this))
			//FIXME .addEvent('focus',function(){ this.hasfocus=true; this.hover.start(0.9) }.bind(this))
			  .addEvent('mouseout',function(){ this.hover.start(0) }.bind(this))
			  .addEvent('mouseover',function(){ Wiki.locatemenu(this.query, $('searchboxMenu') ); this.hover.start(0.9) }.bind(this));

		/* use advanced search-input on safari - experimental */
		//if(window.webkit){
		//	q.setProperties({type:"search",autosave:q.form.action,results:"9",placeholder:q.defaultValue});
		//} else {
			$('recentClear').addEvent('click', this.clear.bind(this));

			this.recent = Wiki.prefs.get('RecentSearch'); if(!this.recent) return;

			var ul = new Element('ul',{'id':'recentItems'}).inject($('recentSearches').show());
			this.recent.each(function(el){
				// xss vulnerability JSPWIKI-384
				el = el.stripScripts();
				new Element('a',{
					'href':'#',
					'events': {'click':function(){ q.value = el; q.form.submit(); }}
					}).setHTML(el).inject( new Element('li').inject(ul) );
			});
		//}
	},

	onPageLoadFullSearch : function(){
		var q2 = $("query2"); if( !q2 ) return;
		this.query2 = q2;

		var changescope = function(){
			var qq = this.query2.value.replace(/^(?:author:|name:|contents:|attachment:)/,'');
			this.query2.value = $('scope').getValue() + qq;
			this.runfullsearch();
		}.bind(this);

		q2.observe( this.runfullsearch0.bind(this) );

		$('scope').addEvent('change', changescope);
		$('details').addEvent('click', this.runfullsearch.bind(this));

		if(location.hash){
			/* hash contains query:pagination(-1=all,0,1,2...) */
			var s = decodeURIComponent(location.hash.substr(1)).match(/(.*):(-?\d+)$/);
			if(s && s.length==3){
				q2.value = s[1];
				$('start').value = s[2];
				changescope();
			}
		}
	},

	/* reset the start page before rerunning the ajax search */
	runfullsearch0: function(){
		$('start').value='0';
		this.runfullsearch();
	},

	runfullsearch: function(e){
		var q2 = this.query2.value;
		if( !q2 || (q2.trim()=='')) {
			$('searchResult2').empty();
			return;
		}
		$('spin').show();

		var scope = $('scope'),
			match= q2.match(/^(?:author:|name:|contents:|attachment:)/) ||"";

		$each(scope.options, function(option){
			if (option.value == match) option.selected = true;
		});

		Wiki.ajax('Search.jsp',{
			action:'ajaxSearch',
			params:$('searchform2'),
			update: 'searchResult2',
			onComplete: function() {
				$('spin').hide();
				//FIXME: stripes generates a whole web-page iso of page fragment with searchresults.
				var x = $E('#searchResult2',$('searchResult2'));
				$('searchResult2').replaceWith( x );
				GraphBar.render($('searchResult2'));
				Wiki.prefs.set('PrevQuery', q2);
			}
		});
		location.hash = '#'+q2+":"+$('start').value;  /* push the query into the url history */

	},

	submit: function(){
		var v = this.query.value.stripScripts(); //xss vulnerability
		if( v == this.query.defaultValue) this.query.value = '';
		if( !this.recent ) this.recent=[];
		if( !this.recent.test(v) ){
			if(this.recent.length > 9) this.recent.pop();
			this.recent.unshift(v);
			Wiki.prefs.set('RecentSearch', this.recent);
		}
	},

	clear: function(){
		this.recent = [];
		Wiki.prefs.remove('RecentSearch');
		$('recentSearches','recentClear').hide();
	},

	ajaxQuickSearch: function(){
		var qv = this.query.value.stripScripts() ;
		if( (qv==null) || (qv.trim()=="") || (qv==this.query.defaultValue) ) {
			$('searchOutput').empty();
			return;
		}
		$('searchTarget').setHTML('('+qv+') :');
		$('searchSpin').show();

		Wiki.jsonrpc('search.findPages', [qv,20], function(result){
				$('searchSpin').hide();
				if(!result.list) return;
				var frag = new Element('ul');

				result.list.each(function(el){
					new Element('li').adopt(
						new Element('a',{'href':Wiki.toUrl(el.map.page) }).setHTML(el.map.page),
						new Element('span',{'class':'small'}).setHTML(" ("+el.map.score+")")
					).inject(frag);
				});
				$('searchOutput').empty().adopt(frag);
				Wiki.locatemenu( $('query'), $('searchboxMenu') );
		});
	} ,

	/* navigate to url, after smart pagename handling */
	navigate: function(url, promptText, clone, search){
		var p = Wiki.PageName,
			defaultResult = (clone) ? p+'sbox.clone.suffix'.localize() : p,
			s = this.query.value;
		if(s == this.query.defaultValue) s = '';

		var handleResult = function(s){
			if(s == '') return;
			if(!search)	s = Wiki.cleanPageName(s);//remove invalid chars from the pagename

			p=encodeURIComponent(p);
			s=encodeURIComponent(s);
			if(clone && (s != p)) s += '&clone=' + p;

			location.href = url.replace('__PAGEHERE__', s );
		};

		if(s!='') {
			handleResult(s);
		} else {
			Wiki.prompt(promptText, defaultResult, handleResult.bind(this));
		}
	}
}


/*
Class: Color
	Class for creating and manipulating colors in JavaScript.
	Minimal variant of the Color class, inspired by mootools
 
Syntax:
	>var myColor = new Color(color[, type]);

Arguments:
	# color - (mixed) A string or an array representation of a color.
	# type - (string, optional) A string representing the type of the color to create.

Color:
	There are 2 representations of color: String, RGB.
	For String representation see Element:setStyle for more information.

Examples:
	String representation:
	> '#fff'
	RGB representation:
	> [255, 255, 255]
	Or
	> [255, 255, 255, 1] //(For transparency.)

Returns:
	(array) A new Color instance.

Examples:
	> var black = new Color('#000');
	> var purple = new Color([255,0,255]);
	> var azure = new Color('azure');

Credit:
  mootools 1.11
*/
var Color = new Class({

	colors: {
		aqua:[0,255,255],
		azure:[240,255,255],
		beige:[245,245,220],
		black:[0,0,0],
		blue:[0,0,255],
		brown:[165,42,42],
		cyan:[0,255,255],
		darkblue:[0,0,139],
		darkcyan:[0,139,139],
		darkgrey:[169,169,169],
		darkgreen:[0,100,0],
		darkkhaki:[189,183,107],
		darkmagenta:[139,0,139],
		darkolivegreen:[85,107,47],
		darkorange:[255,140,0],
		darkorchid:[153,50,204],
		darkred:[139,0,0],
		darksalmon:[233,150,122],
		darkviolet:[148,0,211],
		fuchsia:[255,0,255],
		gold:[255,215,0],
		green:[0,128,0],
		indigo:[75,0,130],
		khaki:[240,230,140],
		lightblue:[173,216,230],
		lightcyan:[224,255,255],
		lightgreen:[144,238,144],
		lightgrey:[211,211,211],
		lightpink:[255,182,193],
		lightyellow:[255,255,224],
		lime:[0,255,0],
		magenta:[255,0,255],
		maroon:[128,0,0],
		navy:[0,0,128],
		olive:[128,128,0],
		orange:[255,165,0],
		pink:[255,192,203],
		purple:[128,0,128],
		violet:[128,0,128],
		red:[255,0,0],
		silver:[192,192,192],
		white:[255,255,255],
		yellow:[255,255,0]
	},

	initialize: function(color, type){

		if(!color) return false;
		type = type || (color.push ? 'rgb' : 'hex');
		var rgb = (type=='rgb') ? color : this.colors[color.trim().toLowerCase()] || color.toString().hexToRgb(true);
		if(!rgb) return false;
		rgb.hex = rgb.rgbToHex();
		return $extend(rgb, Color.prototype);

	},


	/*
	Method: mix
		Mixes two or more colors with the Color.

	Syntax:
		> var myMix = myColor.mix(color[, color2[, color3[, ...][, alpha]);

	Arguments:
		# color - (mixed) A single or many colors, in hex or rgb representation, to mix with this Color.
		# alpha - (number, optional) If the last argument is a number, it will be treated as the amount of the color to mix.

	Returns:
		(array) A new Color instance.

	Examples:
	(code)
	// mix black with white and purple, each time at 10% of the new color
	var darkpurple = new Color('#000').mix('#fff', [255, 0, 255], 10);
 
	$('myDiv').setStyle('background-color', darkpurple);
	(end)
	*/
	mix: function(){
		var colors = $A(arguments),
			rgb = this.copy(),
			alpha = (($type(colors.getLast()) == 'number') ? colors.pop() : 50)/100,
			alphaI = 1-alpha;

		colors.each(function(color){
			color = new Color(color);
			for (var i = 0; i < 3; i++) rgb[i] = Math.round((rgb[i] * alphaI) + (color[i] * alpha));
		});
		return new Color(rgb, 'rgb');
	},

	/*
	Method: invert
		Inverts the Color.

	Syntax:
		> var myInvert = myColor.invert();

	Returns:
		* (array) A new Color instance.

	Examples:
		(code)
		var white = new Color('#fff');
		var black = white.invert();
		(end)
	*/
	invert: function(){
		return new Color( this.map( function(value){
			return 255 - value;
		}));
	}
});

/*
Class: GraphBar
	Object: also used on the findpage
 ** %%graphBars ... %%
 ** convert numbers inside %%gBar ... %% tags to graphic horizontal bars
 ** no img needed.

 ** supported parameters: bar-color and bar-maxsize

Examples:
> %%graphBars-e0e0e0 ... %%  use color #e0e0e0, default size 120
> %%graphBars-blue-red ... %%  blend colors from blue to red
> %%graphBars-red-40 ... %%  use color red, maxsize 40 chars
> %%graphBars-vertical ... %%  vertical bars
> %%graphBars-progress ... %%  progress bars in 2 colors
> %%graphBars-gauge ... %%  gauge bars in gradient colors
*/

var GraphBar =
{
	render: function(page, name){
		$ES('*[class^=graphBars]',page).each( function(g){
			var lbound = 20,	//max - lowerbound size of bar
				ubound = 320,	//min - upperbound size of bar
				vwidth = 20,	//vertical bar width
				color1 = null,	// bar color
				color2 = null,	// 2nd bar color used depending on bar-type
				isGauge = false,	// gauge bar
				isProgress = false,	// progress bar
				isHorizontal = true,// horizontal or vertical orientation
				parms = g.className.substr(9).split('-'),
				barName = parms.shift(); //first param is optional barName

			parms.each(function(p){
				p = p.toLowerCase();
				if(p == "vertical") { isHorizontal = false; }
				else if(p == "progress") { isProgress = true;  }
				else if(p == "gauge") { isGauge = true; }
				else if(p.indexOf("min") == 0) { lbound = p.substr(3).toInt(); }
				else if(p.indexOf("max") == 0) { ubound = p.substr(3).toInt(); }
				else if(p != "") {
					p = new Color(p,'hex'); if(!p.hex) return;
					if(!color1) color1 = p;
					else if(!color2) color2 = p;
				}
			});
			if( !color2 && color1) color2 = (isGauge || isProgress) ? color1.invert() : color1;

			if( lbound > ubound ) { var m = ubound; ubound=lbound; ubound=m; }
			var size = ubound-lbound;

			var bars = $ES('.gBar'+barName, g); //collect all gBar elements
			if( (bars.length==0) && barName && (barName!="")){  // check table data
				bars = this.getTableValues(g, barName);
			}
			if( !bars ) return;

			var barData = this.parseBarData( bars, lbound, size ),
				border = (isHorizontal ? 'borderLeft' : 'borderBottom');

			bars.each(function(b,j){
				var bar1 = $H().set(border+'Width',barData[j]),
					bar2 = $H(), // 2nd bar only valid ico 'progress'
					barEL = new Element('span',{'class':'graphBar'}),
					pb = b.getParent(); // parent of gBar element

				if(isHorizontal){
					barEL.setHTML('x');
					if(isProgress){
						bar2.extend(bar1.obj);
						bar1.set(border+'Width',ubound-barData[j]).set('marginLeft','-1ex');
					}
				} else { // isVertical
					if(pb.getTag()=='td') { pb = new Element('div').wraps(pb); }

					pb.setStyles({'height':ubound+b.getStyle('lineHeight').toInt(), 'position':'relative'});
					b.setStyle('position', 'relative'); //needed for inserted spans ;-)) hehe
					if( !isProgress ) { b.setStyle('top', (ubound-barData[j])); }

					bar1.extend({'position':'absolute', 'width':vwidth, 'bottom':'0'});
					if(isProgress){
						bar2.extend(bar1.obj).set(border+'Width', ubound);
					}
				}
				if(isProgress){
					if(color1){ bar1.set('borderColor', color1.hex); }
					if(color2){
						bar2.set('borderColor', color2.hex);
					} else {
						bar1.set('borderColor', 'transparent');
					}
				} else if(color1){
					var percent = isGauge ? (barData[j]-lbound)/size : j/(bars.length-1);
					bar1.set('borderColor', color1.mix(color2, 100 * percent).hex);
				}

				if(bar2.length > 0){ barEL.clone().setStyles(bar2.obj).injectBefore(b); };
				if(bar1.length > 0){ barEL.setStyles(bar1.obj).injectBefore(b); };

			},this);

		},this);
	},

	/*
	Function: parseBarData
		Parse bar data types and scale according to lbound and size
	*/
	parseBarData: function(nodes, lbound, size){
		var barData=[],
			maxValue=Number.MIN_VALUE,
			minValue=Number.MAX_VALUE,
			num=true,
			ddd=num;

		nodes.each(function(n,i){
			var v = n.getText();
			barData.push(v);
			num &= !isNaN(v.toFloat());
			ddd &= !isNaN(Date.parse(v));
		});
		barData = barData.map(function(b){
			if( ddd ){ b = new Date(Date.parse(b) ).valueOf(); }
			else if( num ){ b = b.match(/([+-]?\d+(:?\.\d+)?(:?e[-+]?\d+)?)/)[0].toFloat(); }

			maxValue = Math.max(maxValue, b);
			minValue = Math.min(minValue, b);
			return b;
		});

		if(maxValue==minValue) maxValue=minValue+1; /* avoid div by 0 */
		size = size/(maxValue-minValue);
		return barData.map(function(b){
			return ( (size*(b-minValue)) + lbound).toInt();
		});
	},

	/*
	Function: getTableValues
		Fetch set of gBar values from a table
		* check first-row to match field-name: return array with col values
		* check first-column to match field-name: return array with row values
		* insert SPANs as place-holder of the missing gBars
	*/
	getTableValues: function(node, fieldName){
		var table = $E('table', node); if(!table) return false;
		var tlen = table.rows.length, h, r, result, i;

		if( tlen > 1 ){ /* check for COLUMN based table */
			r = table.rows[0];
			for( h=0; h < r.cells.length; h++ ){
				if( $getText( r.cells[h] ).trim() == fieldName ){
					result = [];
					for( i=1; i< tlen; i++)
						result.push( new Element('span').wraps(table.rows[i].cells[h]) );
					return result;
				}
			}
		}
		for( h=0; h < tlen; h++ ){  /* check for ROW based table */
			r = table.rows[h];
			if( $getText( r.cells[0] ).trim() == fieldName ){
				result = [];
				for( i=1; i< r.cells.length; i++)
					result.push( new Element('span').wraps(r.cells[i]) );
				return result;
			}
		}
		return false;
	}
}
Wiki.registerPlugin(GraphBar);
//FIXME:convert to class


/*
Class: Collapsible
	Provides support for collapsible list and boxes.
	The collapse status is stored in a browser cookie.
*/
var Collapsible =
{
	pims : [], // all me cookies

	render: function(page, name){
		page = $(page); if(!page) return;

		var cookie = Wiki.Context.test(/view|edit|comment/) ? "JSPWiki"+name : "";
		//var cookie = "";  //activate this line if you want to deactivatie cookie handling

		if(!this.bullet) {
			this.bullet = new Element('div',{'class':'collapseBullet'}).setHTML('&bull;');
		}

		this.pims.push({
			'name':cookie,
			'value':'',
			'initial': (cookie ? Cookie.get(cookie) : "")
		});
		$ES('.collapse', page).each(function(el){
			if(!$E('.collapseBullet',el)) this.collapseNode(el); // no nesting
		}, this);
		$ES('.collapsebox,.collapsebox-closed', page).each(function(el){
			this.collapseBox(el);
		}, this);
	},

	collapseBox: function(el){
		if($E('.collapsetitle',el)) return; //been here before
		var title = el.getFirst(); if( !title ) return;

		var body = new Element('div', {'class':'collapsebody'}),
			bullet  = this.bullet.clone(),
			isclosed = el.hasClass('collapsebox-closed');

		while(title.nextSibling) body.appendChild(title.nextSibling); // wrap other siblings
		el.appendChild(body);

		if(isclosed) el.removeClass('collapsebox-closed').addClass('collapsebox');
		bullet.injectTop( title.addClass('collapsetitle') );
		this.newBullet(bullet, body, !isclosed, title );
	},

	/*
	Function: collapseNode
		Modifies the list such that sublists can be hidden/shown by clicking
		the listitem bullet. The listitem bullet is a node inserted into the
		DOM tree as the first child of the listitem contained by the sublist.
	*/
	collapseNode: function(node){
		$ES('li',node).each(function(li){
			var ulol = $E('ul',li) || $E('ol',li);

			//dont insert bullet when LI is 'empty': no text or no non-ul/ol tags
			var emptyLI = true;
			for( var n = li.firstChild; n ; n = n.nextSibling ) {
				if((n.nodeType == 3 ) && ( n.nodeValue.trim() == "" ) ) continue; //keep searching
				if((n.nodeName == "UL") || (n.nodeName == "OL")) break; //seems like an empty li
				emptyLI = false;
				break;
			}
			if( emptyLI ) return;

			new Element('div',{'class':'collapsebody'}).wraps(li);
			var bullet = this.bullet.clone().injectTop(li);
			if(ulol) this.newBullet(bullet, ulol, (ulol.getTag()=='ul'));
		},this);
	},

	newBullet: function(bullet, body, isopen, clicktarget){
		var ck = this.pims.getLast(); //read cookie
		isopen = this.parseCookie(isopen);
		if(!clicktarget) clicktarget = bullet;

		var bodyfx = body.setStyle('overflow','hidden')
			.effect('height', {
				wait:false,
				onStart:this.renderBullet.bind(bullet),
				onComplete:function(){ if(bullet.hasClass('collapseOpen')) body.setStyle('height','auto'); }
			});

		bullet.className = (isopen ? 'collapseClose' : 'collapseOpen'); //ready for rendering
		clicktarget.addEvent('click', this.clickBullet.bindWithEvent(bullet, [ck, ck.value.length-1, bodyfx]))
			.addHover();

		bodyfx.fireEvent('onStart');
		if(!isopen) bodyfx.set(0);
	},

	renderBullet: function(){
		if(this.hasClass('collapseClose')){
			this.setProperties({'title':'collapse'.localize(), 'class':'collapseOpen'}).setHTML('-'); /* &raquo; */
		} else {
			this.setProperties({'title':'expand'.localize(), 'class':'collapseClose'}).setHTML('+'); /* &laquo; */
		}
	},

	clickBullet: function(event, ck, bulletidx, bodyfx){
		var collapse = this.hasClass('collapseOpen'),
			bodyHeight = bodyfx.element.scrollHeight;

		if(event.target==this){ /* don't handle clicks on nested elements */

			if(collapse) bodyfx.start(bodyHeight, 0); else bodyfx.start(bodyHeight);

			ck.value = ck.value.slice(0,bulletidx) + (collapse ? 'c' : 'o') + ck.value.slice(bulletidx+1);
			if(ck.name) Cookie.set(ck.name, ck.value, {path:Wiki.BasePath, duration:20});

		}
	},

	/*
	Function: parseCookie
		Parse the initial cookie versus actual document
	Returns:
		true if collapse status is open
	*/
	parseCookie: function( isopen ){
		var ck = this.pims.getLast(),
			cursor = ck.value.length,
			token = (isopen ? 'o' : 'c');

		if(ck.initial && (ck.initial.length > cursor)){
			var cookieToken = ck.initial.charAt( cursor );

			if(  ( isopen && (cookieToken == 'c') )
			  || ( !isopen && (cookieToken == 'o') ) ) token = cookieToken ;

			if(token != cookieToken) ck.initial = null; //mismatch with initial cookie
		}
		ck.value += token; //append and save currentcookie

		return(token == 'o');
	}
};
Wiki.registerPlugin(Collapsible);
//Wiki.registerPlugin( Collapsible.render );


/*
Plugin: commentbox
	This wiki-plugin supports the commentbox dynamic style.

Example:
>  %%commentbox ... /% : floating box to the right
>  %%commentbox-LegendTitle .... /% : make it a legend box

DOM structure
>	<div class="commentbox"> ... </div>
>	<fieldset class="commentbox">
>		<legend>LegendTitle</legend>
>		....
>	</fieldset>

*/
Wiki.registerPlugin( function(page,name){

	$ES('*[class^=commentbox]',page).each(function(el){
		var legend = el.className.split('-')[1];
  		if( legend ){
  			new Element('fieldset',{'class':'commentbox'}).adopt(
  				new Element('legend').setHTML( legend.deCamelize() )
  			).wraps(el).injectBefore(el);
  			el.remove();
  		}
	});
});


/*
Class: TableAdds
	Add support for sorting, filtering and zebra-striping of tables.
	TODO: add support for row-grouping

Credit:
	Filters inspired by http://www.codeproject.com/jscript/filter.asp
*/
var TableAdds = new Class({

	options: {
		//sort: true,
		//filter: true,
		//zebra: [color1, color2],
		title : {
			all: "(All)",
			sort: "Click to sort",
			ascending: "Ascending sort. Click to reverse",
			descending: "Descending sort. Click to reverse"
		}
	},


	initialize: function( table, options ){

		if( table.rows.length < 3 ) return null;
		table = $(table);

		var self = table.TableAdds;  //max one TableAdds instance per table
		if( !self) {
			this.table = table;
			this.head = $A(table.rows[0].cells).filter(function(el){
							return el.getTag()=='th';
						});
			table.TableAdds = self = this;
		}
		self.setOptions(options);
		options = self.options;
		var head = self.head;

		if(!self.sorted && options.sort){
			head.each( function(th,i){
				th.set({
					'class': 'sort',
					title: options.title.sort,
					events: {
						'click': self.sort.bind(self,[i])
					}
				});
			});
			self.sorted = true;
		};

		if( !self.filters && options.filter){
			head.each( function(th,i){
				th.adopt( new Element('select',{
					'class':'filter',
					events: {
						'click': function(e){ new Event(e).stopPropagation() },
						'change': self.filter.bind(self,[i])
					}
				}) );
			});
			self.filters = [];
			self.buildFilters();
		}

		if( !self.zebra && options.zebra ){
			(self.zebra = self.zebrafy.bind(self,options.zebra) )();
		}
		return self;
	},

	/*
	Function: sort
		This is a ''click'' event handler to sort tables by a certain column.
		Css styling is applied
		to change the appearance of the sortAscending/sortDescending clickable
		controls inside the table header.
		The data-type of the column is auto-recognized to avoid extensive
		parameterisation.

		# Copy the table body rows into a javascript array and guess
		  the data-type of the column to be sorted.
		# Do the actual sort or reverse sort
		# Apply css format to the header cells.
		# Put the sorted array back into the DOM tree of the document

		The [guessDataType] and [doSort] are helper functions for data-type
		conversion and caching.

		Following CSS selectors can be changed if needed ..
		* clickable column headers, not yet sorted: css class = ''.sort''
		* column headers, sorted ascending: css class = ''.sortAscending''
		* column headers, sorted descending: css clas = ''.sortDescending''


	Credits:
		The implementation was inspired by the excellent javascript created by
		Erik Arvidsson. See http://webfx.eae.net/dhtml/sortabletable/sortabletable.html.

	Arguments:
		column - index of the column to be used as sort key
	*/
	sort: function( column ){

		//todo: add spinner while sorting
		var th = this.head[column],
			rows = this.getRows(); //table.sortCache,

		this.guessDataType(rows, column);

		th.hasClass('sort') ? this.doSort(rows, column) : rows.reverse();

		// format the header cell
		var isdesc = th.hasClass('sortDescending'),
			title = this.options.title;

		new Elements(this.head).addClass('sort').removeClass('sortAscending').removeClass('sortDescending');

		th.removeClass('sort')
			.addClass( isdesc ? 'sortAscending' : 'sortDescending')
			.title = isdesc ? title.descending : title.ascending;

		//put sorted rows back into the table
		var frag = document.createDocumentFragment();
		rows.each( function(r){ frag.appendChild(r); });
		this.table.appendChild(frag);

		var zebra = this.zebra;
		if( zebra ) zebra();
	},

	/*
	Function: filter
		Filter the table based on the filter column and (selected) filter value.
		This function is also an onChange event handler linked with a 'select' element.

	Arguments
		column - index of the column to be used as sort key
		filtervalue - (optional) the value to be filtered, if not present,
			take the selected value of the dropdown filter
	*/
	filter: function( column, filtervalue ){

		var rows = this.getRows(),
			select = this.head[column].getLast(), //get select element
			value = filtervalue || select.getValue(),
			isAll = (value == this.options.title.all),
			filters = this.filters;

		// First check if the column is allready in the filters stack.
		// If so, store the new filter-value in the filters stack.
		// Otherwise, add a new entry at the end of the filters stack.
		if( filters.every( function( filter ,i ){

			if( filter.column != column ) return true;
			isAll ? filters.splice(i, 1) : filter.value = value;
			return false;

		}) ) filters.push( {value:value, column:column} );

		//reset visibility of all rows, and then apply the filters
		rows.each( function(r){ r.style.display=''; });

		filters.each( function(filter){

			var value = filter.value,
				column = filter.column;

			this.buildFilter(column, value);

			rows.each( function(r){
				if( value != r.data[column] ) r.style.display = 'none';
			});

		},this);

		this.buildFilters(); //fill remaining dropdowns
	},

	/*
	Function: zebrafy
		Add odd/even coloring to the table.

	Arguments:
		color1 - color specified in hex(without #) or as html color name.
		color1 - color specified in hex(without #) or as html color name.

		When the first color == 'table' of '' the predefined css class ''.odd''
		is used to color the alternative rows.
	*/
	zebrafy: function(color1, color2){

		var j=0,
			isDefault = color1.test('table') || color1=='';
		color1 = (!isDefault && color1) ? (new Color(color1,'hex')) : '';
		color2 = (!isDefault && color2) ? (new Color(color2,'hex')) : '';

		this.getRows().each( function(r){
			if( r.style.display!='none' ){
				if( isDefault ) $(r)[ (j++ % 2) ? 'addClass' : 'removeClass']('odd');
				else $(r).setStyle('background-color', (j++ % 2) ? color1 : color2 );
			}
		});

	},

	/*
	Function: getRows
		Retrieve the set of data rows of the table.
		Cache the result for subsequent calls.

	Returns:
		Array with all data rows of the table. (excluding the header row)
	*/
	getRows: function(){

		if( !this.rows ) this.rows = $A(this.table.rows).copy(1);
		return this.rows;

	},


	/*
	Function:  buildFilters
		Build for each unfitered column a new filter dropdown.
	*/
	buildFilters: function( ){

		this.head.each( function(th, column){

			var filtered = this.filters.some( function(f){ return f.column==column });
			if( !filtered ) this.buildFilter( column );

		},this);

		var zebra = this.zebra;
		if( zebra ) zebra();
	},

	/*
	Function: buildFilter
		Build a single column dropdown filter. Only the column values of the
		visible rows will be part of the filter dropdown.

	Arguments:
		table - table
		col - column index
		filterValue - normalised value of the selected item (optional)
	*/
	buildFilter: function( column, filterValue ){

		var select = this.head[column].getLast();
		if(!select) return; //empty dropdown ????

		var dropdown = select.options,
			rows = this.getRows(),
			all = this.options.title.all,
			rr = [],
			unique;

		this.guessDataType(rows, column);

		//collect only the visible rows
		rows.each( function(r){
			if( r.style.display !='none' ) rr.push(r);
		});

		this.doSort(rr, column);

		//build dropdown with all unique column values
		dropdown.length = 0;
		dropdown[0]= new Option(all, all);

		rr.each( function(r){
			var d = r.data[column];
			if( d && d != unique ){
				unique = d;
				dropdown[dropdown.length] = new Option( $getText(r.cells[column]).clean().trunc(32), d);
			}
		});
		select.value = filterValue || dropdown[0].value;

		// disable the dropdown if only one value
		select.disabled = (dropdown.length <= 2);
	},

	/*
	Function: guessDataType
		Parse the column and guess its data-type.
		Then convert all values according to that data-type.
		The result is cached in rows~[n].data.

	Supported data-types:
		numeric - numeric value, with . as decimal separator
		date - dates as supported by javascript Date.parse
		  See https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/parse
		ip4 - ip addresses (like 169.169.0.1)
		euro - currency values (like £10.4, $50, €0.5)
		kmgt - storage values (like 2 MB, 4GB, 1.2kb, 8Tb)

	Arguments:
		rows - array of rows each pointing to a DOM tr element
			and caching previously ''guessed'' converted data.
		column - index (0..n) of the processed column
	*/
	guessDataType: function(rows, column){

		//check cached table data
		if( rows[0].data && rows[0].data[column] ) return;

		var num=true, ddd=num, ip4=num, euro=num, kmgt=num;

		rows.each( function( r,iii ){

			var v = r.cells[column];
			if( v ){
				v = v.getAttribute('jspwiki:sortvalue') || $getText(v);
				v = v.clean().toLowerCase();

				if( !r.data ) r.data=[];
				r.data[column] = v;

				num &= !isNaN(v.toFloat());
				ddd &= !isNaN(Date.parse(v));
				ip4 &= v.test(/(?:\d{1,3}\.){3}\d{1,3}/); //169.169.0.1
				euro &= v.test(/^[£$€][0-9.,]+/);
				kmgt &= v.test(/(?:[0-9.,]+)\s*(?:[kmgt])b/); //2 MB, 4GB, 1.2kb, 8Tb
			}
		});

		//now convert all cells to sortable values according to the datatype
		rows.each( function( r ){

			var val = r.data[column], z;

			if( kmgt ){

				val = val.match(/([0-9.,]+)\s*([kmgt])b/) || [0,0,''];
				z = {m:3, g:6, t:9}[ val[2] ] || 0;
				val = val[1].replace(/,/g,'').toFloat() * Math.pow(10, z);

			} else if( euro ){

				val = val.replace(/[^0-9.]/g,'').toFloat();

			} else if( ip4 ){

				val = val.split( '.' );
				val = ((val[0].toInt() * 256 + val[1].toInt()) * 256 + val[2].toInt()) * 256 + val[3].toInt();

			} else if( ddd ){

				val = Date.parse( val );

			} else if( num ){

				val = val.match(/([+-]?\d+(:?\.\d+)?(:?e[-+]?\d+)?)/)[0].toFloat();

			}

			r.data[column] = val;

		});

	},

	/*
	Function: doSort
		Helper function to sort an array, previoulsy prepared by
		[guessDataType]

	Arguments:
		array - target array to be sorted
		column - column index to use a sorting key
	*/
	doSort: function( array, column){

		array.sort( function(a, b){
			a = a.data[column];
			b = b.data[column];
			return (a<b) ? -1 : (a>b) ? 1 : 0;
		});
	}

});
TableAdds.implement(new Options);

/*
Script: TableAdds
	Register a wiki page renderer, invoking the TableAdds class
	where needed.

Table sorting:
	All tables inside a JSPWiki {{%%sortable}} style are retrieved and processed.
	An onclick() handler is added to each column header which points to the
	heart of the javascript: the [TableAdds.sort] function.

Table filtering:
	Add excel like filter dropdowns to all tables inside a JSPWiki {{%%filtertable}} style.


Odd/Even coloring of tables (zebra style):
	- odd rows get css class odd (ref. jspwiki.css )
	> %%zebra-table ... /%
	- odd rows get css style='background=<color>'
	> %%zebra-<odd-color> ... /%
	- odd rows get odd-color, even rows get even-color
	> %%zebra-<odd-color>-<even-color> ... /%

*/
Wiki.registerPlugin( function(page, name){

	var title = {
		all: "filter.all".localize(),
		sort: "sort.click".localize(),
		ascending: "sort.ascending".localize(),
		descending: "sort.descending".localize()
	};

	$ES('.sortable table',page).each( function(table){
		new TableAdds(table, {sort:true, title: title});
	});

	$ES('.table-filter table',page).each( function(table){
		new TableAdds(table, {filter:true, title: title});
	});

	$ES('*[class^=zebra]',page).each( function(el){
		var parms = el.className.split('-').splice(1);
		$ES('table',el).each(function(table){
			new TableAdds(table, {zebra: parms});
		});
	});

});


/*
Class: Categories
	Turn wikipage links into AJAXed popups.
*/
var Categories =
{
	render: function(page, name){

		$ES('.category a.wikipage',page).each(function(link){

			var page = Wiki.toPageName(link.href);
			if(!page) return;
			var wrap = new Element('span').injectBefore(link).adopt(link),
				popup = new Element('div',{'class':'categoryPopup'}).inject(wrap),
				popfx = popup.effect('opacity',{wait:false}).set(0);

			link.addClass('categoryLink')
				.setProperties({ href:'#', title: "category.title".localize(page) })
				.addEvent('click', function(e){
				new Event(e).stop();  //dont jump to top of page ;-)

				new Ajax( Wiki.TemplateUrl + 'AJAXCategories.jsp', {
					postBody: '&page=' + page,
					update: popup,
					onComplete: function(){
						link.setProperty('title', '').removeEvent('click');
						wrap.addEvents({
							'mouseover': function(e){ popfx.start(0.9); },
							'mouseout': function(e){ popfx.start(0); }
						});
						popup.setStyles({
							'left': link.getPosition().x,
							'top': link.getPosition().y+16
						});
						popfx.start(0.9);

						$ES('li,div.categoryTitle',popup).addHover();
					}
				}).request();
			});
		});
	}
}
Wiki.registerPlugin( Categories );


/*
Class: HighlightWord

Credit:
	Inspired by http://www.kryogenix.org/code/browser/searchhi/

History:
	- Modified 21006 to fix query string parsing and add case insensitivity
	- Modified 20030227 by sgala@hisitech.com to skip words
	  with "-" and cut %2B (+) preceding pages
	- Refactored for JSPWiki -- now based on regexp
*/
var HighlightWord =
{
	initialize: function (){
		var q = Wiki.prefs.get('PrevQuery');
		Wiki.prefs.set('PrevQuery', '');
		if( !q && document.referrer.test("(?:\\?|&)(?:q|query)=([^&]*)","g") ) q = RegExp.$1;

		if( q ){
			var words = decodeURIComponent(q).stripScripts(); //xss vulnerability
			words = words.replace( /\+/g, " " );
			words = words.replace( /\s+-\S+/g, "" );
			words = words.replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ); //escape metachars
			words = words.trim().split(/\s+/).join("|");

			this.reMatch = new RegExp( "(" + words + ")" , "gi");

			this.walkDomTree( $("pagecontent") );
		}
	},

	/*
	Function: walkDomTree
		Recursive tree walk to match all text nodes
	*/
	walkDomTree: function( node ){

		if( !node ) return;

		for( var nn=null, n = node.firstChild; n ; n = nn ){
			// prefetch nextSibling cause the tree will be modified
			nn = n. nextSibling;
			this.walkDomTree(n);
		}

		// continue on text-nodes, not yet highlighted, with a word match
		if( node.nodeType != 3 ) return;
		if( node.parentNode.className == "searchword" ) return;
		var s = node.innerText || node.textContent || '';

		s = s.replace(/</g,'&lt;'); // pre text elements may contain <xml> element

		if( this.reMatch.test( s ) ){
			var tmp = new Element('span').setHTML(s.replace(this.reMatch,"<span class='searchword'>$1</span>")),
				f = document.createDocumentFragment();

			while( tmp.firstChild ) f.appendChild( tmp.firstChild );

			node.parentNode.replaceChild( f, node );
		}
	}
}

/*
Class: Dialog
	Simplified implementation of a Dialog box. Acts as a base class
	for other dialog classes.
	It is based on mootools v1.11, depending on the Drag.Base class.

Arguments:
	options - optional, see options below

Options:
	className - css class for dialog box, default is 'dialog'
	style - (optional) additional css style for the dialog box
	relativeTo - DOM element to position the dialog box
	modal - ffs
	resize - resize callback, default null which implies no resize.
	showNow - (default true) show the dialogbox at initialisation time
	draggable - (default true) make the dialogbox draggable
	buttons - (array) set of DOM elements will get a click handler attached
	onShow - (eventhandler) called when dialogbox is displayed
	onHide - (eventhandler) called when dialogbox is hidden
	onResize - (eventhandler) called when dialogbox is resized

Events:
	onShow - fires when the dialog is shown
	onHide - fires when the dialog is hidden
	onResize - fires when the dialog is resized

Example:
	(start code)
	<script>
	var button = $('colorButton');
	var cd = new ColorDialog({
		relativeTo:button,
		onChange:function(color){ $('target').setStyle('background',color);}
	});
	button.addEvent('click', cd.toggle.bind(cd));
	//button.addEvent('click', function(){cd.toggle()});

	</script>
	(end code)
*/
var Dialog = new Class({

	options:{
		className: 'dialog',
		//style:{ dialog-box overrule css styles}
		relativeTo: document.body,
		//modal: false,
		//resize: false, //true or {x:[min,max],y:[min,max]}
		showNow: true,
		draggable: true
		//buttons: { 'button-label': callbackfn-when-clicked }
		//onShow: Class.empty,
		//onHide: Class.empty,
		//onResize: Class.empty
	},

	initialize:function(options){

		this.setOptions(options);

		var caption, body, self = this;
		options = self.options;

		var el = this.element = new Element('div',{
			'class':'dialog',
			'styles': $extend({display:'none'},options.style||{})
		}).adopt(
			caption = new Element('div',{'class':'caption'}).adopt(
				new Element('a',{
					'class':'closer',
					'events':{ 'click': this.hide.bind(this) }
				}).setHTML('&#215;')
			),
			body = new Element('div',{'class':'body'}),
			buttons = new Element('div',{'class':'buttons'})
		);

		self.caption = caption;
		self.body = body;
		self.buttons = buttons;

		if( options.caption ) caption.appendText(options.caption);
		self.setBody( options.body );
		self.setButtons( options.buttons );

		if( options.draggable ){
			new Drag.Base(el);
			el.setStyle('cursor','move');
		}

		if( options.resize ){
			var resize = new Element('div',{'class':'resize'}).inject(el);
			body.makeResizable({
				handle:resize,
				limit:options.resize,
				onDrag: function(){	self.resize(this.value.now.x) }
			})
		}

		el.injectInside(document.body);
		self.resetPosition();
		if( options.showNow ) self.show();
	},

	/*
	Function: toElement
	*/
	toElement: function(){
		return this.element;
	},

	/*
	Function: show
		Show the dialog box and fire the 'onShow' event.
	*/
	show: function(){
		this.element.setStyle('display','');
		this.fireEvent('onShow', [this]);
		return this;
	},
	/*
	Function: hide
		Hide the dialog box and fire the 'onHide' event.
	*/
	hide: function(){
		this.element.setStyle('display','none');
		this.fireEvent('onHide');
		return this;
	},
	/*
	Function: toggle
		Toggle the visibility of the dialog box.
	*/
	toggle: function(){
		var isVisible = this.element.getStyle('display')!='none';
		return this[isVisible ? 'hide' : 'show']();
	},
	/*
	Function: resetPosition
		Set the position of the dialog box back to its initialization value.
	*/
	resetPosition: function(){
		this.setPosition(this.options.relativeTo);
	},
	/*
	Function: setBody
		Set the body of the dialog box
	Arguments:
		content - string or DOM element
	Example:
		> setBody( "this is a new dialog content");
		> setBody( new Element('span',{'class','error'}).setHTML('Error encountered') );
	*/
	setBody: function(content){

		var body = this.body.empty(),
			type = $type(content);

		if( type=='string'){
			body.setHTML(content)
		} else if( type=='element'){
			body.adopt(content.show());
		};

		return this;
	},
	/*
	Function: setButtons
		this.buttons.empty();
		if( buttons ){

		}
	*/
	setButtons: function(buttons){

		var self = this,
			buttonDiv = this.buttons.empty();

		if( buttons ){

			for(var btn in buttons){
				new Element('a',{
					'class':'btn',
					'events':{
						click: buttons[btn],
						mouseup: function(){self.hide();}
					}
				}).adopt(
					new Element('span').adopt(
						new Element('span').setHTML(btn.localize())
					)
				).inject(buttonDiv);
			}
		}
		return this;
	},
	/*
	Function: setPosition
		Position the dialog box. (absolute positioning)

	Arguments:
		relativeTo: (optional) DOM element. Default is the center of the window.
	*/
	setPosition: function( relativeTo ){

		if( relativeTo ){

			//new Element('span',{'styles':{'position':'relative'}}).injectAfter(relativeTo).adopt(relativeTo,el);

			if( $type(relativeTo) == 'element' ) relativeTo = relativeTo.getCoordinates();
			var pos = {left:relativeTo.left, top:relativeTo.top + relativeTo.height}

		} else {

			//center dialog box
			var w = window.getSize(),
				pos = this.element.getCoordinates();
			pos = {
				left: w.scroll.x + w.size.x/2 - pos.width/2,
				top: w.scroll.y + w.size.y/2 - pos.height/2
			}

		}

		this.element.setStyles(pos);
		//fixme: centering the dialog box does not yet work
		//alert(Json.toString(pos));
		//alert(Json.toString(this.element.getCoordinates()) );
	},
	/*
	Function: resize
		Resize the dialog body and fire the 'resize' event.

	Arguments:
		bodywidth - width in px of the body
	*/
	resize: function( bodywidth ){

		//fixme: this resize only works for square color-boxes
		this.element.setStyles({ 'height': bodywidth+28, 'width': bodywidth+20 });
		this.body.setStyles({ 'height': bodywidth, 'width':bodywidth });
		this.fireEvent('onResize');
		return this;

	}

});
Dialog.implement(new Options, new Events); //mootools v1.1


/*
Global: domready
*/
window.addEvent('domready', function(){ Wiki.initialize() });
