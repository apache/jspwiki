/*
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
Javascript routines to support JSPWiki
Since v.2.6.0

Uses mootools v1.1, with following components:
*	Core, Class,  Native, Element(ex. Dimensions), Window,
*	Effects(ex. Scroll), Drag(Base), Remote, Plugins(Hash.Cookie, Tips, Accordion)

Core JS Routine
*	100 Wiki object (page parms, UserPrefs and setting focus)
*	140 SearchBox object: remember 10 most recent search topics
*	290 HighlightWords in the page-content

Core Dynamic Styles
	Wiki.addPageRender( XYZ )
	Wiki.renderPage(page-element, page-name)

*	110 WikiSlimbox (attachment viewer): dynamic style
*	130 TabbedSection object: dynamic style
*	150 Colors, GraphBar object: dynamic style
*	200 Collapsible list items: dynamic style
*	230 Sortable: dynamic style
*	240 Table-filter (excel like column filters): dynamic style
*	280 ZebraTable (color odd/even rows): dynmaic style

Complementary Dynamic Styles (see jspwiki-commonstyles.js)
*	114 Reflection (adds reflection to images): dynamic style
*	116 WikiCoverflow (based on MooFlow) : dynamic style
*	118 Google Chart: dynamic style
*	132 Accordion object: dynamic style
*	220 RoundedCorners: dynamic style
*	260 WikiTips: dynamic style
*	270 WikiColumns: dynamic style
*	300 Prettify: dynamic style

*/

/* extend mootools */
String.extend({
	deCamelize: function(){
		return this.replace(/([a-z])([A-Z])/g,"$1 $2");
	},
	trunc: function(size,elips){
		if( !elips ) elips="...";
		return (this.length<size) ? this : this.substring(0,size)+elips;
	},
	stripScripts: function(){
		var text = this.replace(/<script[^>]*>([\s\S]*?)<\/script>/gi, function(){
			return '';
		});
		return text;
	}
})

// get text of a dhtml node
function $getText(el) {
	return el.innerText || el.textContent || '';
}
Element.extend({

	/* wrapper = new Element('div').injectWrapper(node); */
	wrapChildren: function(el){
		while( el.firstChild ) this.appendChild( el.firstChild );
		el.appendChild( this ) ;
		return this;
	},

	visible: function() {
		var el = this;
		while($type(el)=='element'){
			if(el.getStyle('visibility') == 'hidden') return false;
			if(el.getStyle('display') == 'none' ) return false;
			el = el.getParent();
		}
		return true;
	},

	hide: function() {
		return this.setStyle('display','none');
	},

	show: function() {
		return this.setStyle('display','');
	},

	toggle: function() {
		return this.visible() ? this.hide() : this.show();
	},

	scrollTo: function(x, y){
		this.scrollLeft = x;
		this.scrollTop = y;
	},

	/* dimensions.js */
	getPosition: function(overflown){
		overflown = overflown || [];
		var el = this, left = 0, top = 0;
		do {
			left += el.offsetLeft || 0;
			top += el.offsetTop || 0;
			el = el.offsetParent;
		} while (el);
		overflown.each(function(element){
			left -= element.scrollLeft || 0;
			top -= element.scrollTop || 0;
		});
		return {'x': left, 'y': top};
	},

	getDefaultValue: function(){
		switch(this.getTag()){
			case 'select':
				var values = [];
				$each(this.options, function(option){
					if (option.defaultSelected) values.push($pick(option.value, option.text));
				});
				return (this.multiple) ? values : values[0];
			case 'input': if (!(this.defaultChecked && ['checkbox', 'radio'].contains(this.type)) && !['hidden', 'text', 'password'].contains(this.type)) break;
			case 'textarea': return this.defaultValue;
		}
		return false;
	}

});

var Observer = new Class({
	initialize: function(el, fn, options){
		this.options = Object.extend({
	   	    event: 'keyup',
			delay: 300
		}, options || {});
		this.element = $(el);
		this.callback = fn;
		this.timeout = null;
		this.listener = this.fired.bind(this);
		this.value = this.element.getValue();
		this.element.setProperty('autocomplete','off').addEvent(this.options.event, this.listener);
	},
	fired: function() {
		if (this.value == this.element.value) return;
		this.clear();
		this.value = this.element.value;
		this.timeout = this.callback.delay(this.options.delay, null, [this.element]);
	},
	clear: function() {
		this.timeout = $clear(this.timeout);
	},
	stop: function() {
		this.element.removeEvent(this.options.event, this.listener);
		this.clear();
	}
});

/* Observable class: observe any form element for changes */
Element.extend({
	observe: function(fn, options){
		return new Observer(this, fn, options);
	}
});


/* I18N Support
 * LocalizedStrings takes form { "javascript.some.resource.key":"localised resource key {0}" }
 * Examples:
 * "moreInfo".localize();
 * "imageInfo".localize(2,4); => "Image {0} of {1}" becomes "Image 2 of 4
 */
var LocalizedStrings = LocalizedStrings || []; //defensive
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

/* FIXME parse number anywhere inside a string */
Number.REparsefloat = new RegExp( "([+-]?\\d+(:?\\.\\d+)?(:?e[-+]?\\d+)?)", "i");

/** TABLE stuff **/
function $T(el) {
	var t = $(el);
	return (t && t.tBodies[0]) ? $(t.tBodies[0]) : t;
};

/* FIXME */
// find first ancestor element with tagName
function getAncestorByTagName( node, tagName ) {
	if( !node) return null;
	if( node.nodeType == 1 && (node.tagName.toLowerCase() == tagName.toLowerCase())){
		return node;
	} else {
		return getAncestorByTagName( node.parentNode, tagName );
	}
}

/** AJAX Requests as per http://javapapers.com/ajax/getting-started-with-ajax-using-java/ **/
/*
 * creates a new XMLHttpRequest object which is the backbone of AJAX,
 * or returns false if the browser doesn't support it
 */
function getXMLHttpRequest() {
	var xmlHttpReq = false;
	// to create XMLHttpRequest object in non-Microsoft browsers
	if (window.XMLHttpRequest) {
		xmlHttpReq = new XMLHttpRequest();
	} else if (window.ActiveXObject) {
		try {
			// to create XMLHttpRequest object in later versions
			// of Internet Explorer
			xmlHttpReq = new ActiveXObject("Msxml2.XMLHTTP");
		} catch (exp1) {
			try {
				// to create XMLHttpRequest object in older versions
				// of Internet Explorer
				xmlHttpReq = new ActiveXObject("Microsoft.XMLHTTP");
			} catch (exp2) {
				xmlHttpReq = false;
			}
		}
	}
	return xmlHttpReq;
}

/*
 * Returns a function that waits for the state change in XMLHttpRequest
 */
function getReadyStateHandler(xmlHttpRequest,responseId,loading,callback) {
	// an anonymous function returned
	// it listens to the XMLHttpRequest instance
	return function() {
		if (xmlHttpRequest.readyState >=1 && xmlHttpRequest.readyState <4) {
			if (responseId && document.getElementById(responseId) != null) {
				document.getElementById(responseId).innerHTML = loading;
			}
		}
		if (xmlHttpRequest.readyState == 4) {
			if (xmlHttpRequest.status == 200) {
				if (responseId && document.getElementById(responseId) != null) {
					document.getElementById(responseId).innerHTML = xmlHttpRequest.responseText;
				} else {
					// Javascript function JSON.parse to parse JSON data
					var jsonResponse = xmlHttpRequest.responseText;
					if (jsonResponse && jsonResponse.length>0) {
			        	jsonResponse = JSON.parse(jsonResponse);
					}
			        callback(jsonResponse);
				}
			} else {
				var errorMsg = "HTTP error " + xmlHttpRequest.status + ": " + xmlHttpRequest.statusText;
				if (responseId && document.getElementById(responseId) != null) {
					document.getElementById(responseId).innerHTML(errorMsg);
				} else {
					console.log(errorMsg);
					callback(errorMsg);
				}
			}
		}
	};
}
/** End AJAX Requests **/


/** 100 Wiki functions **/
var Wiki = {

	onPageLoad: function(){
		if(this.prefs) return; //already initialised
		//read all meta elements starting with wiki
		$$('meta').each(function(el){
			var n = el.getProperty('name') || '';
			if( n.indexOf('wiki') == 0 ) this[n.substr(4)] = el.getProperty('content');
		},this);

		var h = location.host;
		this.BasePath = this.BaseUrl.slice(this.BaseUrl.indexOf(h)+h.length,-1);

		// If JSPWiki is installed in the root, then we have to make sure that
		// the cookie-cutter works properly here.

		if( this.BasePath == '' ) this.BasePath = '/';

		this.prefs = new Hash.Cookie('JSPWikiUserPrefs', {path:Wiki.BasePath, duration:20});

		this.PermissionEdit = !!$$('a.edit')[0]; //deduct permission level
		this.url = null;
		this.parseLocationHash.periodical(500);

		this.makeMenuFx('morebutton', 'morepopup');
		this.addEditLinks();

		var p = $('page'); if(p) this.renderPage(p, Wiki.PageName);
		var f = $('favorites'); if(f) this.renderPage(f, "Favorites");
	},
	/* show popup alert, which allows any html msg to be displayed */
	alert: function(msg){
		return alert(msg); //standard js

	},
	/* show popup prompt, which allows any html msg to be displayed and replied to */
	prompt: function(msg, defaultreply, callback){
		return callback( prompt(msg,defaultreply) ); //standard js

	},

	renderPage: function(page, name){
		this.$pageHandlers.each(function(obj){
			obj.render(page, name)
		});
	},
	addPageRender: function(fn){
		if(!this.$pageHandlers) this.$pageHandlers = [];
		this.$pageHandlers.push(fn);
	},

	setFocus: function(){
		/* plain.jsp,   login.jsp,   prefs/profile, prefs/prefs, find */
		['editorarea','j_username','loginname','assertedName','query2'].some(function(el){
			el = $(el);
			if(el && el.visible()) { el.focus(); return true; }
			return false;
		});
	},

	getUrl: function(pagename){
		return this.PageUrl.replace(/%23%24%25/, pagename);
	},

	/* retrieve pagename from any wikipage url format */
	getPageName: function(url){
		var s = this.PageUrl.escapeRegExp().replace(/%23%24%25/, '(.+)'),
			res = url.match(new RegExp(s));
		return (res ? res[1] : false);
	},

	//ref org.apache.wiki.parser.MarkupParser.cleanLink()
	//trim repeated whitespace
	//allow letters, digits and punctuation chars: ()&+,-=._$
	cleanLink: function(p){
		return p.trim().replace(/\s+/g,' ')
                .replace(/[^\w\u00C0-\u1FFF\u2800-\uFFFD\(\)&\+,\-=\.\$ ]/g, "");

	},

	changeOrientation: function(){
		var fav = $('prefOrientation').getValue();
		$('wikibody')
			.removeClass('fav-left').removeClass('fav-right')
			.addClass(fav);
		//$('collapseFavs').fireEvent('click').fireEvent('click'); //refresh sliding favorites
	},

	/* make hover menu with fade effect */
	makeMenuFx: function(btn, menu){
		var btn = $(btn), menu = $(menu);
		if(!btn || !menu) return;

		var	popfx = menu.effect('opacity', {wait:false}).set(0);
		btn.adopt(menu).set({
			'href':'#',
			'events':{
				'mouseout': function(){ popfx.start(0) },
				'mouseover': function(){ Wiki.locatemenu(btn,menu); popfx.start(0.9) }
			}
		});
	},

	//FIXME
	locatemenu: function(base,el){
		var win = {'x': window.getWidth(), 'y': window.getHeight()},
			scroll = {'x': window.getScrollLeft(), 'y': window.getScrollTop()},
			corner = base.getPosition(),
			offset = {'x': base.offsetWidth-el.offsetWidth, 'y': base.offsetHeight },
			popup = {'x': el.offsetWidth, 'y': el.offsetHeight},
			prop = {'x': 'left', 'y': 'top'};

		for (var z in prop){
			var pos = corner[z] + offset[z]; /*top-left corner of base */
			if ((pos + popup[z] - scroll[z]) > win[z]) pos = win[z] - popup[z] + scroll[z];
			el.setStyle(prop[z], pos);
		};
	},

	parseLocationHash: function(){
		if(this.url && this.url == location.href ) return;
		this.url = location.href;
		var h = location.hash;
		if( h=="" ) return;
		h = h.replace(/^#/,'');

		var el = $(h);
		while( $type(el) == 'element' ){
			if( el.hasClass('hidetab') ){
				TabbedSection.click.apply($('menu-'+el.id));
			} else if( el.hasClass('tab') ){
				/* accordion -- need to find accordion toggle object */
			} else if( el.hasClass('collapsebody') ){
				/* collapsible box -- need to find the toggle button */
			} else if(!el.visible() ){
				//alert('not visible'+el.id);
				//fixme need to find the correct toggler
				//el.show(); //eg collapsedBoxes: fixme
			}
			el = el.getParent();
		}

		location = location.href; /* now jump to the #hash */
	},

	/* SubmitOnce: disable all buttons to avoid double submit */
	submitOnce: function(form){
		window.onbeforeunload = null; /* regular exit of this page -- see jspwiki-edit.js */
		(function(){
			$A(form.elements).each(function(e){
				if( (/submit|button/i).test(e.type)) e.disabled = true;
			});
		}).delay(10);
		return true;
	},

	submitUpload: function(form, progress){
		$('progressbar').setStyle('visibility','visible');
		this.progressbar =
			Wiki.ajaxJsonCall.periodical(500, this, ["/progressTracker",[progress],function(result){
			if(result) {
				$('progressbar').getFirst().setStyle('width',result+'%').setHTML(result+'%');
			}
		}]);
		return Wiki.submitOnce(form);
	},

	addEditLinks: function(){
		if( $("previewcontent") || !this.PermissionEdit || this.prefs.get('SectionEditing') != 'on') return;

		var aa = new Element('a',{'class':'editsection'}).setHTML('quick.edit'.localize()),
			i = 0,
			url = this.EditUrl;

		url = url + (url.contains('?') ? '&' : '?') + 'section=';

		this.getSections().each( function(el){
			el.adopt(aa.set({'href':url + i++ }).clone());
		});

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
	 * AJAX call starts with these functions which rely on the Mootools Request.HTML and Request.JSON
	 * http://mootools.net/core/docs/1.5.1/Request/Request.JSON
	 */
	/** Mootools version
	ajaxHtmlCall:function(url, params, responseId, loading){
		var update = document.getElementById(responseId);
		if (update){ update.innerHTML = loading||'Loading...'; }

		new Request.HTML({
			url: this.JsonUrl + url,
			method:'post',  // defaults to 'POST'
			update: update
		}).send({
	        params: params
	    });
	},
	ajaxJsonCall: function(url, params, callback){
		//the Request.JSON does all encoding and decoding of the JSON automatically
		new Request.JSON({
			url: this.JsonURL + url,
			method:'post',
			onSuccess: function(response){
			    if(response.error){
			        console.log(response.error);
			        callback(null);
			    } else {
			        callback(response.result)
			    }
			},
			onError: function(response){
			        console.log(response.error);
			        callback(null);
			}
		}).send({
	        params: params
	    });
	}
	 */

	ajaxHtmlCall: function (url, params, responseId, loading) {
		url = Wiki.JsonUrl + url;
		if (!loading) {
			loading = "Loading...";
		}
		var xmlHttpRequest = getXMLHttpRequest();
		xmlHttpRequest.onreadystatechange = getReadyStateHandler(xmlHttpRequest,responseId,loading);
		xmlHttpRequest.open('post', url, true);
		xmlHttpRequest.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		xmlHttpRequest.send("params="+params);
	},

	ajaxJsonCall: function (url, params, callback) {
		url = Wiki.JsonUrl + url;
		var xmlHttpRequest = getXMLHttpRequest();
		xmlHttpRequest.onreadystatechange = getReadyStateHandler(xmlHttpRequest,null,null,callback);
		xmlHttpRequest.open('post', url, true);
		xmlHttpRequest.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
		xmlHttpRequest.send("params="+params);
	}
}


/** 110 WikiSlimbox
 ** Inspired http://www.digitalia.be/software/slimbox by Christophe Bleys
 ** 	%%slimbox [...] %%
 ** 	%%slimbox-img  [some-image.jpg] %%
 ** 	%%slimbox-ajax [some-page links] %%
 **/
var WikiSlimbox = {

	render: function(page, name){
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
				}).injectBefore(el);

				if(el.src) el.replaceWith(new Element('a',{
					'class':'attachment',
					'href':el.src
				}).setHTML(el.alt||el.getText()));
			});
		});
		if(i) Lightbox.init();
		//new Asset.javascript(Wiki.TemplateUrl+'scripts/slimbox.js');
	}
}
Wiki.addPageRender(WikiSlimbox);

/*
	Slimbox v1.31 - The ultimate lightweight Lightbox clone
	by Christophe Beyls (http://www.digitalia.be) - MIT-style license.
	Inspired by the original Lightbox v2 by Lokesh Dhakar.

	Updated by Dirk Frederickx to fit JSPWiki needs
	- minimum size of image canvas DONE
	- add maximum size of image w.r.t window size DONE
	- CLOSE icon -> close x text iso icon DONE
	- <<prev, next>> links added in visible part of screen DONE
	- add size of picture to info window DONE
	- spacebor, down arrow, enter : next image DONE
	- up arrow : prev image DONE
	- allow the same picture occuring several times DONE
	- add support for external page links  => slimbox_ex DONE
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
			if(this.images[this.activeImage][2]!='img' &&!this.ajaxFailed){ w = 6000; h = 3000; }
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


/** Class: Tabbed Section (130)
	Creates tabs, based on some css-class information
	Use in jspwiki: %%tabbedSection  %%tab-FirstTab .. %% %%

	Following markup:
	<div class="tabbedSection">
		<div class="tab-FirstTab">..<div>
		<div class="tab-SecondTab">..<div>
	</div>

	is changed into
	<div class="tabmenu"><span><a activetab>..</a></span>..</div>
	<div class="tabbedSection tabs">
		<div class="tab-firstTab ">
		<div class="tab-SecondTab hidetab">
	</div>
 **/
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

	click: function(){
		var menu = $(this).getParent(),
			tabs = menu.getNext();

		menu.getChildren().removeClass('activetab');
		this.addClass('activetab');

		tabs.getChildren().addClass('hidetab');
		tabs.getElement( '#'+ this.id.substr(5)).removeClass('hidetab');
	}

}
Wiki.addPageRender(TabbedSection);



/* 140 SearchBox
 * FIXME: remember 10 most recent search topics (cookie based)
 * Extended with quick links for view, edit and clone (ref. idea of Ron Howard - Nov 05)
 * Refactored for mootools, April 07
 */
var SearchBox = {

	onPageLoad: function(){
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

		new Ajax(Wiki.TemplateUrl+'AJAXSearch.jsp', {
			data: $('searchform2').toQueryString(),
			update: 'searchResult2',
			method: 'get', // use "get" to avoid mootools bug on XHR header "CONNECTION:CLOSE"
			onComplete: function() {
				$('spin').hide();
				GraphBar.render($('searchResult2'));
				Wiki.prefs.set('PrevQuery', q2);
			}
		}).request();

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

	   Wiki.ajaxJsonCall("/search/pages",[qv,'20'], function(result) {
			$('searchSpin').hide();
			if(!result) return;
			var frag = new Element('ul');

			result.each(function(el){
				new Element('li').adopt(
					new Element('a',{'href':Wiki.getUrl(el.page) }).setHTML(el.page),
					new Element('span',{'class':'small'}).setHTML(" ("+el.score+")")
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
			if(!search)	s = Wiki.cleanLink(s);//remove invalid chars from the pagename

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


/**
 ** 150 GraphBar Object: also used on the findpage
 ** %%graphBars ... %%
 ** convert numbers inside %%gBar ... %% tags to graphic horizontal bars
 ** no img needed.
 ** supported parameters: bar-color and bar-maxsize
 ** e.g. %%graphBars-e0e0e0 ... %%  use color #e0e0e0, default size 120
 ** e.g. %%graphBars-blue-red ... %%  blend colors from blue to red
 ** e.g. %%graphBars-red-40 ... %%  use color red, maxsize 40 chars
 ** e.g. %%graphBars-vertical ... %%  vertical bars
 ** e.g. %%graphBars-progress ... %%  progress bars in 2 colors
 ** e.g. %%graphBars-gauge ... %%  gauge bars in gradient colors
 **/

/* minimal variant of the Color class, inspired by mootools */
var Color = new Class({

	_HTMLColors: {
		black  :"000000", green :"008000", silver :"c0c0c0", lime  :"00ff00",
		gray   :"808080", olive :"808000", white  :"ffffff", yellow:"ffff00",
		maroon :"800000", navy  :"000080", red    :"ff0000", blue  :"0000ff",
		purple :"800080", teal  :"008080", fuchsia:"ff00ff", aqua  :"00ffff"
	},

	initialize: function(color, type){
		if(!color) return false;
		type = type || (color.push ? 'rgb' : 'hex');
		if(this._HTMLColors[color]) color = this._HTMLColors[color];
		var rgb = (type=='rgb') ? color : color.toString().hexToRgb(true);
		if(!rgb) return false;
		rgb.hex = rgb.rgbToHex();
		return $extend(rgb, Color.prototype);
	},

	mix: function(){
		var colors = $A(arguments),
			rgb = this.copy(),
			alpha = (($type(colors[colors.length - 1]) == 'number') ? colors.pop() : 50)/100,
			alphaI = 1-alpha;

		colors.each(function(color){
			color = new Color(color);
			for (var i = 0; i < 3; i++) rgb[i] = Math.round((rgb[i] * alphaI) + (color[i] * alpha));
		});
		return new Color(rgb, 'rgb');
	},

	invert: function(){
		return new Color(this.map(function(value){
			return 255 - value;
		}));
	}

});

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
				barName = parms.shift(), //first param is optional barName
				size,bars,barData,border;

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
			size = ubound-lbound;

			bars = $ES('.gBar'+barName, g); //collect all gBar elements
			if( (bars.length==0) && barName && (barName!="")){  // check table data
				bars = this.getTableValues(g, barName);
			}
			if( !bars ) return;

			barData = this.parseBarData( bars, lbound, size );
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
					if(pb.getTag()=='td') { pb = new Element('div').wrapChildren(pb); }

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

	// parse bar data types and scale according to lbound and size
	parseBarData: function(nodes, lbound, size){
		var barData=[],
			maxValue=Number.MIN_VALUE,
			minValue=Number.MAX_VALUE,
			num=date=true;

		nodes.each(function(n,i){
			var s = n.getText();
			barData.push(s);
			num &= !isNaN(s.toFloat());
			/* chrome accepts numbers as valid Dates !! */
			date &= !isNaN(Date.parse(s)) && s.test(/[^\d]/);
		});

		barData = barData.map(function(b){
			if(date){ b = new Date(Date.parse(b) ).valueOf();  }
			else if(num){ b = parseFloat( b.match(Number.REparsefloat) ); }

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

	/* Fetch set of gBar values from a table
	 * Check first-row to match field-name: return array with col values
	 * Check first-column to match field-name: return array with row values
	 * insert SPANs as place-holder of the missing gBars
	 */
	getTableValues: function(node, fieldName){
		var table = $E('table', node); if(!table) return false;
		var tlen = table.rows.length;

		if( tlen > 1 ){ /* check for COLUMN based table */
			var r = table.rows[0];
			for( var h=0; h < r.cells.length; h++ ){
				if( $getText( r.cells[h] ).trim() == fieldName ){
					var result = [];
					for( var i=1; i< tlen; i++)
						result.push( new Element('span').wrapChildren(table.rows[i].cells[h]) );
					return result;
				}
			}
		}
		for( var h=0; h < tlen; h++ ){  /* check for ROW based table */
			var r = table.rows[h];
			if( $getText( r.cells[0] ).trim() == fieldName ){
				var result = [];
				for( var i=1; i< r.cells.length; i++)
					result.push( new Element('span').wrapChildren(r.cells[i]) );
				return result;
			}
		}
		return false;
	}
}
Wiki.addPageRender(GraphBar);


/** 200 Collapsible list and boxes **/
var Collapsible =
{
	pims : [], // all me cookies

	render: function(page, name){
		page = $(page); if(!page) return;

		var cookie = Wiki.Context.test(/view|edit|comment/) ? "JSPWikiCollapse"+ name: "";

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

	// Modifies the list such that sublists can be hidden/shown by clicking the listitem bullet
	// The listitem bullet is a node inserted into the DOM tree as the first child of the
	// listitem containing the sublist.
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

			new Element('div',{'class':'collapsebody'}).wrapChildren(li);
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
			.addEvent('mouseenter', function(){ clicktarget.addClass('hover')} )
			.addEvent('mouseleave', function(){ clicktarget.removeClass('hover')} );

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

	clickBullet: function( event, ck, bulletidx, bodyfx){
		var collapse = this.hasClass('collapseOpen'),
			bodyHeight = bodyfx.element.scrollHeight;

		if(event.target==this){ /* don't handle clicks on nested elements */

			if(collapse) bodyfx.start(bodyHeight, 0); else bodyfx.start(bodyHeight);

			ck.value = ck.value.slice(0,bulletidx) + (collapse ? 'c' : 'o') + ck.value.slice(bulletidx+1);
			if(ck.name) Cookie.set(ck.name, ck.value, {path:Wiki.BasePath, duration:20});

		}
	},

	// parse initial cookie versus actual document
	// returns true if collapse status is open
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
Wiki.addPageRender(Collapsible);


/** 230 Sortable -- Sort tables **/
//TODO cache table ok, cache datatype for each column
var Sortable =
{
	render: function(page,name){
		this.DefaultTitle = "sort.click".localize();
		this.AscendingTitle = "sort.ascending".localize();
		this.DescendingTitle = "sort.descending".localize();

		$ES('.sortable table',page).each(function(table){
			if( table.rows.length <= 2 ) return;

			$A(table.rows[0].cells).each(function(th){
				th=$(th);
				if(th.getTag()=='th'){
					th.addEvent('click', this.sort.bind(this,th) )
						.addClass('sort')
						.title=this.DefaultTitle;
				}
			},this);
		},this);
	},

	sort: function(th){
		var table = getAncestorByTagName(th, "table" ),
			filter = (table.filterStack),
			rows = (table.sortCache || []),
			colidx = 0, //target column to sort
			body = $T(table);
		th = $(th);

		//todo add spinner while sorting
		//validate header row
		$A(body.rows[0].cells).each(function(thi, i){
			if(thi.getTag() != 'th') return;
			if(th == thi) { colidx=i; return; }
			thi.removeClass('sortAscending').removeClass('sortDescending')
				.addClass('sort').title = Sortable.DefaultTitle;
		});

		if(rows.length == 0){  //if data not yet cached
			$A(body.rows).each(function(r,i){
				if((i==0) || ((i==1) && (filter))) return;
				rows.push( r );
			});
		};
		var datatype = Sortable.guessDataType(rows,colidx);

		//do the actual sorting
		if(th.hasClass('sort')){
			rows.sort( Sortable.createCompare(colidx, datatype) )
		}
		else rows.reverse();

		var fl=th.hasClass('sortDescending');
		th.removeClass('sort').removeClass('sortAscending').removeClass('sortDescending');
		th.addClass(fl ? 'sortAscending': 'sortDescending')
			.title= fl ? Sortable.DescendingTitle: Sortable.AscendingTitle;

		var frag = document.createDocumentFragment();
		rows.each( function(r,i){ frag.appendChild(r); });
		body.appendChild(frag);
		table.sortCache = rows;
		if(table.zebra) table.zebra();
	},

	guessDataType: function(rows, colidx){

		var num=date=ip4=euro=kmgt=true;

		rows.each(function(r,i){

			var v = r.cells[colidx];

			v = v.getAttribute('jspwiki:sortvalue') || $getText(v);
			v = v.clean().toLowerCase();

			if(num)  num  = !isNaN(parseFloat(v));
			/* chrome accepts numbers as valid Dates !! */
			if(date) date = !isNaN(Date.parse(v)) && v.test(/[^\d]/);
			if(ip4)  ip4  = v.test(/(?:\\d{1,3}\\.){3}\\d{1,3}/); //169.169.0.1
			if(euro) euro = v.test(/^[Â£$â‚¬][0-9.,]+/);
			if(kmgt) kmgt = v.test(/(?:[0-9.,]+)\s*(?:[kmgt])b/);  //2 MB, 4GB, 1.2kb, 8Tb

		});

		return (kmgt) ? 'kmgt': (euro) ? 'euro': (ip4) ? 'ip4': (date) ? 'date': (num) ? 'num': 'string';

	},

	convert: function(val, datatype){

		switch( datatype ){

			case "num" :
				return parseFloat( val.match( Number.REparsefloat ) );

			case "euro":
				return parseFloat( val.replace(/[^0-9.,]/g,'') );

			case "date":
				return new Date( Date.parse( val ) );

			case "ip4" :
				var octet = val.split( "." );
				return parseInt(octet[0]) * 1000000000 + parseInt(octet[1]) * 1000000 + parseInt(octet[2]) * 1000 + parseInt(octet[3]);

			case "kmgt":
				var v = val.toString().toLowerCase().match(/([0-9.,]+)\s*([kmgt])b/);
				if(!v) return 0;
				var z=v[2];
				z = (z=='m') ? 3 : (z=='g') ? 6 : (z=='t') ? 9 : 0;
				return v[1].toFloat()*Math.pow(10,z);

			default:
				return val.toString().toLowerCase();

		}

	},

	createCompare: function( i, datatype ){

		return function( row1, row2 ){

			//fixme: should cache the converted sortable values
			var v1 = row1.cells[i],
				v2 = row2.cells[i],
				val1 = Sortable.convert( v1.getAttribute('jspwiki:sortvalue') || $getText(v1), datatype ),
				val2 = Sortable.convert( v2.getAttribute('jspwiki:sortvalue') || $getText(v2), datatype );

			return (val1<val2) ? -1 : (val1>val2) ? 1 : 0;

		}
	}
}
Wiki.addPageRender(Sortable);

/** 240 table-filters
 ** inspired by http://www.codeproject.com/jscript/filter.asp
 **/
var TableFilter =
{
	render: function(page,name){
		this.All = "filter.all".localize();
		this.FilterRow = 1; //row number of filter dropdowns

		$ES('.table-filter table',page).each( function(table){
			if( table.rows.length < 2 ) return;

			/*
			$A(table.rows[0].cells).each(function(e,i){
				var s = new Element('select',{
					'events': {
						'click':function(event){ event.stop(); }.bindWithEvent(),
						'change':TableFilter.filter
					}
				});
				s.fcol = i; //store index
				e.adopt(s);
			},this);
			*/

			var r = $(table.insertRow(TableFilter.FilterRow)).addClass('filterrow');
			for(var j=0; j < table.rows[0].cells.length; j++ ){
				var s = new Element('select',{
					'events': {
						'change':TableFilter.filter
					}
				});
				s.fcol = j; //store index

				new Element('th').adopt(s).inject(r);
			}
			table.filterStack = [];
			TableFilter.buildEmptyFilters(table);
		});
	},

	buildEmptyFilters: function(table){
		for(var i=0; i < table.rows[0].cells.length; i++){
			var ff = table.filterStack.some(function(f){ return f.fcol==i });
			if(!ff) TableFilter.buildFilter(table, i);
		}
		if(table.zebra) table.zebra();
	},

	// this function initialises a column dropdown filter
	buildFilter: function(table, col, selectedValue){
		// Get a reference to the dropdownbox.
		var select = table.rows[TableFilter.FilterRow].cells[col].firstChild;
		//var select = $(table.rows[0].cells[col]).getLast();
		if(!select) return; //empty dropdown
		select.options.length = 0;

		var rows=[];
		$A(table.rows).each(function(r,i){
			if((i==0) || (i==TableFilter.FilterRow)) return;
			if(r.style.display == 'none') return;
			rows.push( r );
		});
		rows.sort(Sortable.createCompare(col, Sortable.guessDataType(rows,col)));

		//add only unique strings to the dropdownbox
		select.options[0]= new Option(this.All, this.All);
		var value;
		rows.each(function(r,i){
			var v = $getText(r.cells[col]).clean().toLowerCase();
			if(v == value) return;
			value = v;
			//if(v.length > 32) v = v.substr(0,32)+ "...";
			//select.options[select.options.length] = new Option(v, value);
			select.options[select.options.length] = new Option(v.trunc(32), value);
		});
		(select.options.length <= 2) ? select.hide() : select.show();
		if(selectedValue != undefined) {
			select.value = selectedValue;
		} else {
			select.options[0].selected = true;
		}
	},

	filter: function(){ //onchange handler of filter dropdowns
		var col   = this.fcol,
			value = this.value,
			table = getAncestorByTagName(this, 'table');
		if( !table || table.style.display == 'none') return;

		// First check if the column is allready in the filter.
		if(table.filterStack.every(function(f,i){
			if(f.fcol != col) return true;
			if(value == TableFilter.All) table.filterStack.splice(i, 1);
			else f.fValue = value;
			return false;
		}) ) table.filterStack.push( {fValue:value, fcol:col} );

		$A(table.rows).each(function(r,i){ //show all
			r.style.display='';
		});

		table.filterStack.each(function(f){ //now filter the right rows
			var v = f.fValue, c = f.fcol;
			TableFilter.buildFilter(table, c, v);

			var j=0;
			$A(table.rows).each(function(r,i){
				if((i==0) || (i==TableFilter.FilterRow)) return;
				if(v != $getText(r.cells[c]).clean().toLowerCase()) r.style.display = 'none';
			});
		});
		TableFilter.buildEmptyFilters(table); //fill remaining dropdowns
	}
}
Wiki.addPageRender(TableFilter);


/** 250 Categories: turn wikipage link into AJAXed popup **/
var Categories =
{
	render: function (page,name){

		$ES('.category a.wikipage',page).each(function(link){
			var page = Wiki.getPageName(link.href);
			if(!page) return;
			var wrap = new Element('span').injectBefore(link).adopt(link),
				popup = new Element('div',{'class':'categoryPopup'}).inject(wrap),
				popfx = popup.effect('opacity',{wait:false}).set(0);

			link.addClass('categoryLink')
				.setProperties({ href:'#', title: "category.title".localize(page) })
				.addEvent('click', function(e){
				new Event(e).stop();  //dont jump to top of page ;-)

				new Ajax( Wiki.TemplateUrl + 'AJAXCategories.jsp', {
				    method:"get", //use "get" to avoid mootools bug on XHR header "CONNECTION:CLOSE"
					data: '&page=' + page,
					update: popup,
					onComplete: function(){
						link.setProperty('title', '').removeEvent('click');
						wrap.addEvent('mouseover', function(e){ popfx.start(0.9); })
							.addEvent('mouseout', function(e){ popfx.start(0); });
						popup.setStyle('left', link.getPosition().x);
						popup.setStyle('top', link.getPosition().y+16);
						popfx.start(0.9);

						$ES('li,div.categoryTitle',popup).each(function(el){
							el.addEvent('mouseout',function(){ this.removeClass('hover')})
							  .addEvent('mouseover',function(){ this.addClass('hover')});
						});


					}
				}).request();
			});
		});
	}
}
Wiki.addPageRender(Categories);


/** 280 ZebraTable
 ** Color odd/even rows of table differently
 ** 1) odd rows get css class odd (ref. jspwiki.css )
 **   %%zebra-table ... %%
 **
 ** 2) odd rows get css style='background=<color>'
 ** %%zebra-<odd-color> ... %%
 **
 ** 3) odd rows get odd-color, even rows get even-color
 ** %%zebra-<odd-color>-<even-color> ... %%
 **
 ** colors are specified in HEX (without #) format or html color names (red, lime, ...)
 **/
var ZebraTable = {
	render: function(page,name){
		$ES('*[class^=zebra]',page).each(function(z){
			var parms = z.className.split('-'),
				isDefault = parms[1].test('table'),
				c1 = '',
				c2 = '';
			if(parms[1]) c1= new Color(parms[1],'hex');
			if(parms[2]) c2= new Color(parms[2],'hex');
			$ES('table',z).each(function(t){
				t.zebra = this.zebrafy.pass([isDefault, c1,c2],t);
				t.zebra();
			},this);
		},this);
	},
	zebrafy: function(isDefault, c1, c2){
		var j=0;
		$A($T(this).rows).each(function(r,i){
			if(i==0 || (r.style.display=='none')) return;
			if(isDefault) (j++ % 2) ? $(r).addClass('odd') : $(r).removeClass('odd');
			else $(r).setStyle('background-color', (j++ % 2) ? c1 : c2 );
		});
	}
}
Wiki.addPageRender(ZebraTable);

/** Highlight Word
 ** Inspired by http://www.kryogenix.org/code/browser/searchhi/
 ** Modified 21006 to fix query string parsing and add case insensitivity
 ** Modified 20030227 by sgala@hisitech.com to skip words
 **                   with "-" and cut %2B (+) preceding pages
 ** Refactored for JSPWiki -- now based on regexp
 **/
var HighlightWord =
{
	onPageLoad: function (){
		var q = Wiki.prefs.get('PrevQuery');
		Wiki.prefs.set('PrevQuery', '');
		if( !q && document.referrer.test("(?:\\?|&)(?:q|query)=([^&]*)","g") ) q = RegExp.$1;
		if( !q ) return;

		var words = decodeURIComponent(q).stripScripts(); //xss vulnerability
		words = words.replace( /\+/g, " " );
		words = words.replace( /\s+-\S+/g, "" );
		words = words.replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ); //escape metachars
		words = words.trim().split(/\s+/).join("|");
		this.reMatch = new RegExp( "(" + words + ")" , "gi");

		this.walkDomTree( $("pagecontent") );
	},

	// recursive tree walk matching all text nodes
	walkDomTree: function(node){
		if( !node ) return;
		for(var nn=null, n = node.firstChild; n ; n = nn) {
			nn = n. nextSibling; /* prefetch nextSibling cause the tree will be modified */
			this.walkDomTree(n);
		}
		// continue on text-nodes, not yet highlighted, with a word match
		if( node.nodeType != 3 ) return;
		if( node.parentNode.className == "searchword" ) return;
		var s = node.innerText || node.textContent || '';

		s = s.replace(/</g,'&lt;'); /* pre text elements may contain <xml> element */

		if( !this.reMatch.test( s ) ) return;
		var tmp = new Element('span').setHTML(s.replace(this.reMatch,"<span class='searchword'>$1</span>"));

		var f = document.createDocumentFragment();
		while( tmp.firstChild ) f.appendChild( tmp.firstChild );

		node.parentNode.replaceChild( f, node );
	}
}

window.addEvent('load', function(){
	Wiki.onPageLoad();

	SearchBox.onPageLoad();
	HighlightWord.onPageLoad();
	Wiki.setFocus();
	//console.profile();
	//console.profileEnd();
});