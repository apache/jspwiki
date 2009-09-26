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
String.implement({

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
	}

});

Element.implement({

	/*
	Function: wrapContent
		This method moves this Element around its content.(child-nodes)
		The Element is moved to the position of the passed element and becomes the parent.
		All child-nodes are moved to the new element.

	Arguments:
		el - (mixed) The id of an element or an Element.

	Returns:
		(element) This Element.

	Examples:
	HTML
	>	<div id="myFirstElement"></div>
	JavaScript
	>	var mySecondElement = new Element('div', {id: 'mySecondElement'});
	>	mySecondElement.wrapContent($('myFirstElement'));
	Resulting HTML
	>	<div id="mySecondElement">
    >		<div id="myFirstElement"></div>
	>	</div>
	*/
	wrapContent : function(el){
		while( el.firstChild ) this.appendChild( el.firstChild );
		el.appendChild( this ) ;
		return this;
	},

	/*
	FIXME 123!
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

		//isDisplayed: function(){
		//		return this.getStyle('display') != 'none';
		//	},
		//
		//isVisible: function(){
		//		var w = this.offsetWidth, h = this.offsetHeight;
		//		return (w == 0 && h == 0) ? false : (w > 0 && h > 0) ? true : this.isDisplayed();
		//  },
	},

	/*
	Function: hide
		Hide the element: set 'display' style to 'none'.
		Ref. mootools.1.2.3

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').hide();
	*/
 	hide: function() {
		//return this.setStyle('display','none');
		var d;
		try {
			// IE fails here if the element is not in the dom
			if ((d = this.getStyle('display')) == 'none') d = null;
		} catch(e){}
 
		return this.store('originalDisplay', d || 'block').setStyle('display', 'none');
	},

	/*
	Function: show
		Show the element: set 'display' style to '' (default display style)
		Ref. mootools.1.2.3

	Returns:
		(element) - This Element

	Examples:
	>	$('thisElement').show();
	*/
	show: function(display) {
		//return this.setStyle('display','');
		return this.setStyle('display', display || this.retrieve('originalDisplay') || 'block');
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
		//return this[this.isDisplayed() ? 'hide' : 'show']();
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
		Inspired by get('value') of mootools, v1.1

	Note:
		Checkboxes treat default-values in a different way.
		Compare the {{checked}} property vs the {{defaultChecked}} property.
		When equal, return the defaultValue (which btw equals to the value).
		Otherwise, return false.  (FFS: or may be better undefined ?)

	Returns:
		(element) - the default value of the element; or false if not applicable.

	Examples:
	> $('thisElement').getDefaultValue();
	*/
	getDefaultValue: function(){

	    var self = this,
	    	type = self.get('type'),
	    	values = [];

		switch( self.get('tag') ){

			case 'select':

				$each(self.options, function(option){
					if( option.defaultSelected ) values.push($pick(option.value, option.text));
				});
				return (self.multiple) ? values : values[0];

			case 'input':

				if( ('checkbox'==type) && (self.checked != self.defaultChecked)) break;

				if(	!'checkbox|radio|hidden|text|password'.test(type) ) break;

			case 'textarea':

				return self.defaultValue;

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
Shortcuts:
	$E : synonym for document.getElement(css-selector)
*/
var $E = document.getElement.bind(document);
//var $ = document.id;

/*
Class: Observer
	Class to observe a dom element for changes.

Example:
>	$(formInput).observe(function(){
>		alert('my value changed to '+this.get('value') );
>	});

*/
var Observer = new Class({

	Implements: Options,

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
		self.value = el.get('value');
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

Element.implement({
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
>	"moreInfo".localize() =="More";
>	"imageInfo".localize(2,4); => "Image {0} of {1}" becomes "Image 2 of 4"

*/
String.implement({
	localize: function(){
		var s = LocalizedStrings["javascript."+this] || "???"+this+"???",
			args = arguments; /* propagate to the closure function */

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

		/* FIXME: ID on top tabs, should move server side */
		if( $E('div.tabs') ){
			$E('div.tabs').id="toptab";
			$E('div.tabmenu').id="toptabmenu";
        }

		// read all meta elements starting with wiki
		$$('meta').each( function(el){
			var n = el.get('name') || '';
			if( n.indexOf('wiki') == 0 ) this[n.substr(4)] = el.get('content');
		}, self);

		self.BasePath = (self.BaseUrl) ?
			self.BaseUrl.slice(self.BaseUrl.indexOf(host)+host.length,-1) : '';
		// if JSPWiki is installed in the root, then we have to make sure that
		// the cookie-cutter works properly here.
		if(self.BasePath == '') self.BasePath = '/';

		self.prefs = new Hash.Cookie('JSPWikiUserPrefs', {path:self.BasePath, duration:20});

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

		self.renderPage( $('page'), self.PageName);
		self.renderPage( $('favorites'), "Favorites");

		self.splitbar(); //add splitbar between favorites and page content

		//jump back to the section previously being edited
		if( document.referrer.test( /\&section=(\d+)$/ ) ){
			var section = RegExp.$1.toInt(),
				els = self.getSections();
			if( els && els[section] ){
				var el = els[section];
				window.scrollTo( el.getLeft(), el.getTop() );
			}
		}

		//fixme: new SearchBox($('xxx'));
		SearchBox.initialize();

		//fixme;
		// HighlightWord( $('pagecontent'), self.prefs.get('PrevQuery') )
		// self.prefs.set('PrevQuery','');
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
			.setBody( new Element('p').set('html',msg) )
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
			.setBody( new Element('p').set('html',msg) )
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
				new Element('p').set('html',msg),
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
				Ok:function(){ callback( input.get('value') ) },
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

		//var	popfx = menu.effect('opacity', {wait:false}).set(0);
		menu.fade('hide');
		btn.adopt(menu).set({
			href:'#',
			events:{
				'mouseout': function(){ menu.fade(0) /*popfx.start(0)*/ },
				'mouseover': function(){ Wiki.locatemenu(btn,menu); menu.fade(0.9) /*popfx.start(0.9)*/ }
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
				if(!result.code) bar.getFirst().setStyle('width',result+'%').set('html',result+'%');
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
		}).wrapContent(tabs.setStyle('padding','0'));

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


//fixme: SectionEditing is not properly saved when updating userprefs !
//alert(this.prefs.keys()+"\n"+this.prefs.values());

		if( $("previewcontent") || !this.allowEdit || this.prefs.get('SectionEditing') != 'on') return;

		var url = this.EditUrl;
		url = url + (url.contains('?') ? '&' : '?') + 'section=';

		var aa = new Element('a',{'class':'editsection'}).set('html','quick.edit'.localize()),
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

  		new Request.HTML({
  			url:url,
			data: params,
			method: 'post',
			update: options.update,
			onComplete: function( result ){
				options.onComplete( result );
			}
		}).send();

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

			new Request({
				url: this.JsonUrl,
				data: JSON.encode({
					'id':this.$jsonid++,
					'method':method,
					'params':params
				}),
				method: 'post',
				onComplete: function(result){
					var r = JSON.decode(result,true);
					fn(r.result || r.error || null);
					/*if( r ){
						if(r.result){ fn(r.result) }
						else if(r.error){ fn(r.error) }
					}*/
				}
			}).send();
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
		lnk = new Element('a',{'class':'slimbox'}).set('html','&raquo;');


	page.getElements('*[class^=slimbox]').each(function(slim){
		var group = 'lightbox'+ i++,
			parm = slim.className.split('-')[1] || 'img ajax',
			filter = [];
		if(parm.test('img')) filter.extend(['img.inline', 'a.attachment']);
		if(parm.test('ajax')) filter.extend(['a.wikipage', 'a.external']);

		slim.getElements(filter.join(',')).each(function(el){
			var href = el.src||el.href,
				rel = (el.className.test('inline|attachment')) ? 'img' : 'ajax';

			if((rel=='img') && !href.test('(.bmp|.gif|.png|.jpg|.jpeg)(\\?.*)?$','i')) return;

			lnk.clone().setProperties({
				'href':href,
				'rel':group+' '+rel,
				'title':el.alt||el.get('text')
			}).injectAfter(el);//.injectBefore(el);

			//if(el.src) el.replaces(new Element('a',{
			//	'class':'attachment',
			//	'href':el.src
			//}).set('html',el.alt||el.get('text')));

			if(el.src) new Element('a',{
				'class':'attachment',
				href:el.src,
				html:el.alt||el.get('text')
			}).replaces(el);

		});
	});

return; //FIXME
	//if(i) Lightbox.init();
	/* TODO: new slimbox invocation code, based on Slimbox v1.7 */
	$$(document.links).filter(function(el) {
		return el.rel && el.rel.test(/^lightbox/i);
	}).slimbox({/* Put custom options here */}, null, function(el) {
		return (this == el) || ((this.rel.length > 8) && (this.rel == el.rel));
	});


})



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
		page.getElements('.tabmenu a').each(function(el){
			if(!el.href) el.addEvent('click', this.click);
		},this);

		// convert tabbedSections into tabmenu's with click handlers
		page.getElements('.tabbedSection').each( function(tt){
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

		//this.hover = $('searchboxMenu').setProperty('visibility','visible')
		//.effect('opacity', {wait:false}).set(0);
		var menu = $('searchboxMenu').set('visibility','visible').fade('hide');

		$(q.form).addEvent('submit',this.submit.bind(this))
			//FIXME .addEvent('blur',function(){ this.hasfocus=false; this.hover.start(0) }.bind(this))
			//FIXME .addEvent('focus',function(){ this.hasfocus=true; this.hover.start(0.9) }.bind(this))
			  .addEvent('mouseout',function(){ menu.fade(0) /*this.hover.start(0)*/ }.bind(this))
			  .addEvent('mouseover',function(){
			  	Wiki.locatemenu(this.query, $('searchboxMenu') );
			  	menu.fade(0.9) /*this.hover.start(0.9)*/ }.bind(this));

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
					}).set('html',el).inject( new Element('li').inject(ul) );
			});
		//}
	},

	onPageLoadFullSearch : function(){

		var q2 = $("query2"); if( !q2 ) return;
		this.query2 = q2;

		var changescope = function(){
			var qq = this.query2.value.replace(/^(?:author:|name:|contents:|attachment:)/,'');
			this.query2.value = $('scope').get('value') + qq;
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
				//var x = $E('#searchResult2',$('searchResult2'));
				var res = $('searchResult2');
			console.log(res.innerHTML);
				res.replaces( res );
				//res.replaces( res.getElement('#searchResult2') );
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
		$('searchTarget').set('html','('+qv+') :');
		$('searchSpin').show();

		Wiki.jsonrpc('search.findPages', [qv,20], function(result){
				$('searchSpin').hide();
				if(!result.list) return;
				var frag = new Element('ul');

				result.list.each(function(el){
					new Element('li').adopt(
						new Element('a',{'href':Wiki.toUrl(el.map.page) }).set('html',el.map.page),
						new Element('span',{'class':'small'}).set('html'," ("+el.map.score+")")
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
var Color = (function(){

var htmlColors = {
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
};
return new Class({

	initialize: function(color, type){
		if (arguments.length >= 3){
			type = 'rgb'; color = Array.slice(arguments, 0, 3);
		} else if (typeof color == 'string'){
			if (color.match(/rgb/)) color = color.rgbToHex().hexToRgb(true);
			//else if (color.match(/hsb/)) color = color.hsbToRgb();
			else color = htmlColors[color.trim().toLowerCase()] || color.hexToRgb(true);
		}
		type = type || 'rgb';
		/*
		switch (type){
			case 'hsb':
				var old = color;
				color = color.hsbToRgb();
				color.hsb = old;
			break;
			case 'hex': color = color.hexToRgb(true); break;
		}
		*/
		color.rgb = color.slice(0, 3);
		//color.hsb = color.hsb || color.rgbToHsb();
		color.hex = color.rgbToHex();
		return $extend(color, this);
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
			rgb = $A(this),
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

})()

/*
Class: GraphBar
	Generate horizontal or vertical bars, without using any images.
	Support any color, gradient bars, progress and gauge bars.
	The length of the bars can be based on numbers or dates.
	Allow to specify maximum and minimum values.

>	%%graphBars
>		%%gBar 25 /%
>	/%

	Graphbar parameters can be passed in class constructor (options)
	or as call-name parameters.

>	%%graphBars-min50-max3000-progress-lime-0f0f0f
>		%%gBar 25 /%
>	/%

	Other example of wiki-markup
> %%graphBars-e0e0e0 ... %%  use color #e0e0e0, default size 120
> %%graphBars-blue-red ... %%  blend colors from blue to red
> %%graphBars-red-40 ... %%  use color red, maxsize 40 chars
> %%graphBars-vertical ... %%  vertical bars
> %%graphBars-progress ... %%  progress bars in 2 colors
> %%graphBars-gauge ... %%  gauge bars in gradient colors

Options:
	classPrefix - CSS classname of parent element (default = graphBars)
	classBar - CSS classname of the bar value elements (default = gBar)
	lowerbound - lowerbound of bar values (default:20px)
	upperbound - upperbound of bar values (default:320px)
	vwidth - vertical bar width in px(default:20px)
	isHorizontal - horizontal or vertical bars (default:true)
	isProgress - progress bar show 2 bars, always summing up to 100%
	isGauge - gauge bars have colour gradient related to the size/value of the bar


DOM-structure:
	Original DOM-structure
>	<span class="gBar">100 </span>

	Is converted to following horizontal bar
>	<span class="graphBar" style="border-left-width: 20px;">x</span>
>	<span class="gBar">100 </span>

	or is converted to following vertical bar
>   <div style="height: 77px; position: relative;">
>       <span class="graphBar"
>             style="border-color: rgb(255, 0, 0);
>                    border-bottom-width: 20px;
>                    position: absolute;
>                    width: 20px;
>                    bottom: 0px;"/>
>       <span style="position: relative; top: 40px;"> 20 </span>
>    </div>

	or is converted to following progress bar
>	<span class="graphBar" style="border-color: rgb(0, 128, 0); border-left-width: 20px;">x</span>
>	<span class="graphBar" style="border-color: rgb(0, 255, 0); border-left-width: 300px; margin-left: -1ex;">x</span>
>	<span class="gBar">100 </span>


Examples:
>	new GraphBar( dom-element, { options });

*/
Wiki.registerPlugin( function(page,name){

	page.getElements('div[class^=graphBars]').each( function(el){
		new GraphBar(el);
	});

});

var GraphBar = new Class({

	Implements: Options,

	options: {
		classPrefix:"graphBars",
		classBar:"gBar",
		lowerbound:20,
		upperbound:320,
		vwidth:20, //vertical bar width
		isHorizontal:true,
		isProgress:false,
		isGauge:false
	},

	initialize: function(el, options){

		this.setOptions(options);
		this.parseParameters( el );

		var self = this,
			options = self.options,
			bars = el.getElements('.'+ options.classBar + options.barName), //collect all gBar elements
			color1 = self.color1,
			color2 = self.color2,
			border = (options.isHorizontal ? 'borderLeft' : 'borderBottom'),
			isProgress = options.isProgress,
			isGauge = options.isGauge,

			tmp = options.upperbound,
			ubound = Math.max(tmp, options.lowerbound),
			lbound = (tmp == ubound) ? options.lowerbound : tmp;

		if( !color2 && color1)
			color2 = (isGauge || isProgress) ? color1.invert() : color1;

		if( bars.length == 0 ) bars = self.getTableValues(el, options.barName);

		if( bars ){

			var barData = self.parseBarData( bars, lbound, ubound-lbound );

			bars.each( function(el, index){

				var bar = {},
					progressbar = {},
					value = barData[index],
					barEL = new Element('span',{'class':'graphBar'}),
					pb = el.getParent(); // parent of gBar element

				bar[ border+'Width' ] = value;

				if( options.isHorizontal ){

					barEL.set('html','x');

					if( isProgress ){
						$extend( progressbar, bar );
						bar[border+'Width'] = ubound - value;
						bar.marginLeft='-1ex';
					}

				} else { // isVertical

					if( pb.get('tag')=='td' ) pb = new Element('div').wrapContent(pb);

					pb.setStyles({
						height: ubound + el.getStyle('lineHeight').toInt(),
						position: 'relative'
					});
					el.setStyle('position', 'relative'); //needed for inserted spans ;-)) hehe
					if( !isProgress ) el.setStyle('top', ubound - value );

					$extend( bar, {position:'absolute', width:options.vwidth, bottom:0} );

					if( isProgress ) $extend(progressbar,bar)[border+'Width'] = ubound;

				}

				if( isProgress ){

					if( color1 ) bar.borderColor = color1.hex;

					if( color2 ){ progressbar.borderColor = color2.hex }
					else        { bar.borderColor = 'transparent' }

				} else if( color1 ){

					var percent = isGauge ? (value-lbound)/(ubound-lbound) : index/(bars.length-1);
					bar.borderColor = color1.mix(color2, 100 * percent).hex;

				}

				if( isProgress ) barEL.clone().setStyles(progressbar).injectBefore(el);
				barEL.setStyles(bar).injectBefore(el);


			});
		}

	},

	parseParameters: function( el ){

		var self = this,
			options = self.options,
			parms = el.className.slice( options.classPrefix.length ).split('-');

		options.barName = parms.shift(), //first param is optional barName

		parms.each( function( p ){
			p = p.toLowerCase();
			if(p == "vertical") { options.isHorizontal = false; }
			else if(p == "progress") { options.isProgress = true;  }
			else if(p == "gauge") { options.isGauge = true; }
			else if(p.indexOf("min") == 0) { options.lowerbound = p.slice(3).toInt(); }
			else if(p.indexOf("max") == 0) { options.upperbound = p.slice(3).toInt(); }

			else if(p != "") {
				p = new Color(p,'hex'); if(!p.hex) return;
				if( !self.color1 ) self.color1 = p;
				else if( !self.color2 ) self.color2 = p;
			}
		});

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

		nodes.each(function( n ){
			var v = n.get('text');
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

		var table = node.getElement('table');
		if(!table) return false;
		var tlen = table.rows.length, h, l, r, result, i;

		if( tlen > 1 ){ /* check for COLUMN based table */
			r = table.rows[0];
			for( h=0, l=r.cells.length; h<l; h++ ){
				if( $getText( r.cells[h] ).trim() == fieldName ){
					result = [];
					for( i=1; i< tlen; i++)
						result.push( new Element('span').wrapContent(table.rows[i].cells[h]) );
					return result;
				}
			}
		}
		for( h=0; h < tlen; h++ ){  /* check for ROW based table */
			r = table.rows[h];
			if( $getText( r.cells[0] ).trim() == fieldName ){
				result = [];
				for( i=1,l=r.cells.length; i<l ; i++)
					result.push( new Element('span').wrapContent(r.cells[i]) );
				return result;
			}
		}
		return false;
	}
});


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
			this.bullet = new Element('div',{'class':'collapseBullet'}).set('html','&bull;');
		}

		this.pims.push({
			'name':cookie,
			'value':'',
			'initial': (cookie ? Cookie.read(cookie) : "")
		});
		page.getElements('.collapse').each(function(el){
			//CHECK: if(!$-E('.collapseBullet',el)) this.collapseNode(el); // no nesting
			if(!el.getElement('.collapseBullet')) this.collapseNode(el); // no nesting
		}, this);
		page.getElements('.collapsebox,.collapsebox-closed').each(function(el){
			this.collapseBox(el);
		}, this);
	},

	collapseBox: function(el){
		//CHECK: if($-E('.collapsetitle',el)) return; //been here before
		if(el.getElement('.collapsetitle')) return; //been here before
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
		node.getElements('li').each(function(li){
			//CHECK: var ulol = $-E('ul',li) || $-E('ol',li);
			//var ulol = li.getElement('ul') || li.getElement('ol');
			var ulol = li.getElement('ul,ol');

			//dont insert bullet when LI is 'empty': no text or no non-ul/ol tags
			var emptyLI = true;
			for( var n = li.firstChild; n ; n = n.nextSibling ) {
				if((n.nodeType == 3 ) && ( n.nodeValue.trim() == "" ) ) continue; //keep searching
				if((n.nodeName == "UL") || (n.nodeName == "OL")) break; //seems like an empty li
				emptyLI = false;
				break;
			}
			if( emptyLI ) return;

			new Element('div',{'class':'collapsebody'}).wrapContent(li);
			var bullet = this.bullet.clone().injectTop(li);
			if(ulol) this.newBullet(bullet, ulol, (ulol.get('tag')=='ul'));
		},this);
	},

	newBullet: function(bullet, body, isopen, clicktarget){
		var ck = this.pims.getLast(); //read cookie
		isopen = this.parseCookie(isopen);
		if(!clicktarget) clicktarget = bullet;

		/*
		var bodyfx = body.setStyle('overflow','hidden')
			.effect('height', {
				wait:false,
				onStart:this.renderBullet.bind(bullet),
				onComplete:function(){ if(bullet.hasClass('collapseOpen')) body.setStyle('height','auto'); }
			});
		*/
		body.setStyle('overflow','hidden');
		var bodyfx = new Fx.Tween( body, {
				//wait:false,
				onStart:this.renderBullet.bind(bullet),
				onComplete:function(){
					if(bullet.hasClass('collapseOpen')) body.setStyle('height','auto');
				}
			});

		bullet.className = (isopen ? 'collapseClose' : 'collapseOpen'); //ready for rendering
		clicktarget.addEvent('click', this.clickBullet.bindWithEvent(bullet, [ck, ck.value.length-1, bodyfx]))
			.addHover();

		bodyfx.fireEvent('onStart');
		if(!isopen) bodyfx.set('height',0);
	},

	renderBullet: function(){
		if(this.hasClass('collapseClose')){
			this.setProperties({'title':'collapse'.localize(), 'class':'collapseOpen'}).set('html','-'); /* &raquo; */
		} else {
			this.setProperties({'title':'expand'.localize(), 'class':'collapseClose'}).set('html','+'); /* &laquo; */
		}
	},

	clickBullet: function(event, ck, bulletidx, bodyfx){
		var collapse = this.hasClass('collapseOpen'),
			bodyHeight = bodyfx.element.scrollHeight; //CHECKME: does this work -- refactor
			//FIXME: better use the morph style ???

		if(event.target==this){ /* don't handle clicks on nested elements */

			if(collapse) bodyfx.start('height', bodyHeight, 0); else bodyfx.start('height',bodyHeight);

			ck.value = ck.value.slice(0,bulletidx) + (collapse ? 'c' : 'o') + ck.value.slice(bulletidx+1);
			if(ck.name) Cookie.write(ck.name, ck.value, {path:Wiki.BasePath, duration:20});

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

	page.getElements('div[class^=commentbox]').each(function(el){
		var legend = el.className.split('-')[1];
  		if( legend ){
  			new Element('fieldset',{'class':'commentbox'}).adopt(
  				new Element('legend').set('html', legend.deCamelize() )
  			).wrapContent(el).injectBefore(el);
  			el.dispose();
  		}
	});


});


/*
Class: TableAdds
	Add support for sorting, filtering and zebra-striping of tables.
	TODO: add support for row-grouping
	TODO: add support for check-box filtering (ref excel like)

Credit:
	Filters inspired by http://www.codeproject.com/jscript/filter.asp
*/
var TableAdds = new Class({

	Implements: Options,

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
							return el.get('tag')=='th';
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

		//var zebra = this.zebra;
		//if( zebra ) zebra();
		$try( this.zebra );
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
			value = filtervalue || select.get('value'),
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

		if( !this.rows ) this.rows = $A(this.table.rows).slice(1);
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
		Empty rows will sort based on the title attribute of the cells.

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

		var num=true, flt = num, ddd=num, ip4=num, euro=num, kmgt=num, empty=num;

		rows.each( function( r,iii ){

			var v = r.cells[column];
			if( v ){
				v = v.getAttribute('jspwiki:sortvalue') || $getText(v);
				v = v.clean().toLowerCase();

				if( !r.data ) r.data=[];
				r.data[column] = v;

				num &= v.test(/\d+/);
				flt &= !isNaN(v.toFloat());
				ddd &= !isNaN(Date.parse(v));
				ip4 &= v.test(/(?:\d{1,3}\.){3}\d{1,3}/); //169.169.0.1
				euro &= v.test(/^[£$€][\d.,]+/);
				kmgt &= v.test(/(?:[\d.,]+)\s*(?:[kmgt])b/); //2 MB, 4GB, 1.2kb, 8Tb
				empty &= (v=='');
			}
		});

		//now convert all cells to sortable values according to the datatype
		rows.each( function( r ){

			var val = r.data[column], z;

			if( kmgt ){

				val = val.match(/([\d.,]+)\s*([kmgt])b/) || [0,0,''];
				z = {m:3, g:6, t:9}[ val[2] ] || 0;
				val = val[1].replace(/,/g,'').toFloat() * Math.pow(10, z);

			} else if( euro ){

				val = val.replace(/[^\d.]/g,'').toFloat();

			} else if( ip4 ){

				val = val.split( '.' );
				val = ((val[0].toInt() * 256 + val[1].toInt()) * 256 + val[2].toInt()) * 256 + val[3].toInt();

			} else if( ddd ){

				val = Date.parse( val );

			} else if( flt ){

				val = val.match(/([+-]?\d+(:?\.\d+)?(:?e[-+]?\d+)?)/)[0].toFloat();

			} else if( num ){

				val = val.match(/\d+/)[0].toInt();

			}

			r.data[column] = empty ? r.cells[column].get('title') : val;

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

	page.getElements('.sortable table').each( function(table){
		new TableAdds(table, {sort:true, title: title});
	});

	page.getElements('.table-filter table').each( function(table){
		new TableAdds(table, {filter:true, title: title});
	});

	page.getElements('*[class^=zebra]').each( function(el){
		var parms = el.className.split('-').splice(1);
		el.getElements('table').each(function(table){
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

		page.getElements('.category a.wikipage').each(function(link){

			var page = Wiki.toPageName(link.href);
			if(!page) return;

			var wrap = new Element('span').injectBefore(link).adopt(link),
				popup = new Element('div',{'class':'categoryPopup'}).inject(wrap).fade('hide');

			link.addClass('categoryLink')
				.set({ href:'#', title: "category.title".localize(page) })
				.addEvent('click', function(event){

				event.stop();  //dont jump to top of page ;-)
				new Request.HTML({
					url:Wiki.TemplateUrl + 'AJAXCategories.jsp',
					data: '&page=' + page,
					update: popup,
					onComplete: function(){
						link.set('title', '')
							.removeEvent('click');
						wrap.addEvents({
							'mouseover': function(){ popup.fade(0.9) },
							'mouseout': function(){ popup.fade(0) }
						});
						popup.setStyles({
							left: link.getPosition().x,
							top: link.getPosition().y+16
						}).fade(0.9).getElements('li,div.categoryTitle').addHover();
					}
				}).send();

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
			var tmp = new Element('span',{
				'html': s.replace(this.reMatch,"<span class='searchword'>$1</span>")
				}),
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
	It is based on mootools v1.2.3, depending on the Drag class.

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

	Implements: [Events, Options],

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
				}).set('html','&#215;')
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
			new Drag(el);
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
		> setBody( new Element('span',{'class','error'}).set('html','Error encountered') );
	*/
	setBody: function(content){

		var body = this.body.empty(),
			type = $type(content);

		if( type=='string'){
			body.set('html',content)
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
						new Element('span').set('html',btn.localize())
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
				ws = window.getScroll(),
				pos = this.element.getCoordinates();

			pos = {
				left: ws.x + w.x/2 - pos.width/2,
				top: ws.y + w.y/2 - pos.height/2
			}

		}

		this.element.setStyles(pos);
		//fixme: centering the dialog box does not yet work
		//alert(JSON.encode(pos));
		//alert(JSON.encode(this.element.getCoordinates()) );
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


/*
Global: domready
*/
window.addEvent('domready', function(){ Wiki.initialize() });
