/**
 ** Javascript routines to support the BrushedTemplate
 ** Dirk Frederickx
 ** Nov 06-Mar 07: aligned with MooTools ##
 **
 ** 050 URL Handling
 ** 100 Wiki object (page parms, UserPrefs and setting focus) ##
 ** 110 WikiSlimbox : attachment viewer ##
 ** 114 Reflection (adds reflection to images) ##
 ** 120 QuickLinks object ##
 ** 130 TabbedSection object
 ** 132 Accordion object ##
 ** 140 SearchBox object: remember 10 most recent search topics
 ** 150 Colors, GraphBar object: e.g. used on the findpage
 ** 160 URL
 **
 ** 200 Collapsable list items
 ** 220 RoundedCorners ffs
 ** 230 Sortable (clever table-sort) ##
 ** 240 Table-filter (excel like table filters ##
 ** 250 Categories: turn wikipage link into AJAXed popup ##
 ** 260 WikiTips ##
 ** 270 WikiColumns ##
 ** 280 ZebraTable: color odd/even row of a table ##
 ** 290 HighlightWord: refactored
 ** 295 Typography
 **
 **/


/* extend mootools */

String.extend({
	repeat: function( size ){
		if( size <=1 ) return this;
		var a = new Array( size );
		for( var i=0; i < size; i++ ) { a[i] = this; }
		return( a.join("") );
	},
	deCamelize: function(){
		return this.replace(/([a-z])([A-Z])/g,'$1 $2');
	}
});


// get text of a dhtml node
function $getText(el) {
	return el.innerText || el.textContent || '';
}
Element.extend({
	getText: function() {
		return this.innerText || this.textContent || '';
	},
	/* wrapper = new Element('div').injectWrapper(node); */
	injectWrapper: function(el){
		while( el.firstChild ) this.appendChild( el.firstChild );
		el.appendChild( this ) ;
		return this;
	},
	visible: function() {
		return( this.style.display != 'none' );
	},
	hide: function() {
		this.style.display = 'none';
		return this;
	},
	show: function() {
		this.style.display = '';
		return this;
	},	
	toggle: function() {
		return this.visible() ? this.hide(): this.show();
	},
	/*
	scrollTo: function() {
		window.scrollTo(this.x||this.offsetLeft,this.y||this.offsetTop);
		return this;
	},
	*/
	getPositionedOffset: function(what) {
		what = "offset"+what.capitalize();
		var el=this, offset=0;
		do {
			offset += el[what] || 0;
			el = el.offsetParent;
			if (el && /relative|absolute/.test($(el).getStyle('position'))) break;
		} while (el);
		return offset;
	}
});


/* Observable class: observe any form element for changes */
Element.extend({
	observe: function(fn, options){
		return new Observer(this, fn, options);
	}
});

var Observer = new Class({
	initialize: function(el, fn, options){
		this.options = Object.extend({
       	    event: 'keyup',
		    delay: 1000
		}, options || {});
		this.element = $(el);
		this.callback = fn;
		this.timeout = null;
		this.listener = this.fired.bind(this);
		this.value = this.element.value;  //CHECK: use getValue() ?
		this.element.addEvent(this.options.event, this.listener);
	},
	fired: function() {
		this.clear();
		if (this.value == this.element.value) return;
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

//ref http://forum.mootools.net/topic.php?id=1038 --not yet used
/*
var QueryString = new Class({
    initialize: function(){
        $A(window.location.search.replace(/^?/,'').split('&')).each(function(s){
            this[s.split('=')[0]] = unescape(s.split('=')[1]);
        }.bind(this));
    }
});
*/

/* Cookie: based on mootools, uses encode/decode stuff iso escape */
Cookie = {

	set: function(key, value, path, expires, domain, secure){
			var c = [], date = new Date();
			date.setTime(date.getTime() + ((expires || 365)*86400000));
			c.push(key + "=" + encodeURIComponent(value));
			//c.push(key + "=" + escape(value));
			c.push("expires=" + date.toGMTString());
			c.push("path=" + (path ? path: "/"));
			if(domain) c.push("domain="+domain);
			if(secure) c.push("secure");
			document.cookie = c.join("; "); 
	},
	get: function(key){
		var value = document.cookie.match('(?:^|;)\\s*'+key+'=([^;]*)');
		//return value ? unescape(value[1]): false;
		return value ? decodeURIComponent(value[1]): false;
	},
	remove: function(key){
			this.set(key, '', -1);
	}
}; 


// see http://forum.mootools.net/topic.php?id=959 ...
var Color = new Class({

	initialize: function(color){
			if (color.blend && color.mix) return color;
			var rgb = (color.push) ? color: color.hexToRgb(true);
			return Object.extend(rgb, Color.prototype);
	},

	blend: function(alpha, color){
			color = new Color(color);
			var rgb = [];
			for (var i = 0; i < 3; i++) rgb.push(Math.round((this[i] / 100 * alpha) + (color[i] / 100 * (100 - alpha))));
			return new Color(rgb);
	},

	mix: function(){
			var mixed;
			$each(arguments, function(color){
					mixed = this.blend(50, color);
			}, this);
			return mixed;
	},

	invert: function(){
			var rgb = [];
			for (var i = 0; i < 3; i++) rgb.push(255 - this[i]);
			return new Color(rgb);
	}
});
function $C(color){
	return new Color(color);
};

Color.implement({
	HTMLColors :
	{ black  :"000000", green :"008000", silver :"C0C0C0", lime  :"00FF00",
	  gray   :"808080", olive :"808000", white  :"FFFFFF", yellow:"FFFF00",
	  maroon :"800000", navy  :"000080", red    :"FF0000", blue  :"0000FF",
	  purple :"800080", teal  :"008080", fuchsia:"FF00FF", aqua  :"00FFFF" },

	initialize: function(color){
			if (color.blend && color.mix) return color;
			var rgb = (color.push) ? color : (this.HTMLColors[color] || color).hexToRgb(true);
			return Object.extend(rgb, Color.prototype);
	}
});




/* I18N Support
 * LocalizedStrings takes form { "javascript.resource key":"localised resource key {0}" }
 * Parameters {0..9}
 * Use: "resource key".localize(parameters)
 *
 * Examples:
 * "moreInfo".localize();
 * "imageInfo".localize(2,4); => expects "Image {0} of {1}"
 */
String.extend({

	localize: function(){
		var s = LocalizedStrings["javascript."+this], args = arguments;
		if(!s) return("§§§" + this + "§§§");
        return s.replace(/\{(\d)\}/g, function(m){ 
        	return args[m.charAt(1)] || "§§§"+m.charAt(1)+"§§§"});
	}
});
/* LocalizedStrings is found in commonheader.jsp */


/* FIXME */
/* parse number anywhere inside a string */
Number.REparsefloat = new RegExp( "([+-]?\\d+(:?\\.\\d+)?(:?e[-+]?\\d+)?)", "i");
/* parse a date anywhere inside a string */
/* todo */


/** TABLE stuff **/
function $T(el) {
	var t = $(el ); 
	return (t && t.tBodies[0]) ? $(t.tBodies[0]) : t;
};

/* FIXME */
// find first ancestor element with tagName
function getAncestorByTagName( node, tagName )
{
	if( !node) return null;
	if( node.nodeType == 1 && (node.tagName.toLowerCase() == tagName.toLowerCase()))
	{ return node; }
	else
	{ return getAncestorByTagName( node.parentNode, tagName ); }
}


/* 050  URL handling
 * - onPageLoad/interal 500ms: make sure URL#HASH is visible
 * - parseParameters: read parameter from the URL
 * FIXME mootools
 **/
var URL = {

	parseParameters: function(aParm){  //FIXME: use mootools foo
		var u = document.URL.split( '&' );
		for( var i=1; i < u.length; i++ ){
			var item = u[i].split('=');
			if( item.length == 1 ) continue;
			if( item[0] == aParm ) return( item[1] );
		}
		return(0);
	},

	onPageLoad: function(){
		URL.makeVisible.periodical(500);
	},

	CurrentURL: null,  //current URL#HASH value
	makeVisible: function(){
		if( this.CurrentURL && (this.CurrentURL == location.href) ) return;
		this.CurrentURL = location.href;

		var h = location.hash; if( h=="" ) return;
		h = h.substring(1); //drop leading #
		var node = $(h);

		var needToScroll = false;
		while( $type(node)=='element' ){
			if ( node.hasClass('hidetab')){
				needToScroll = true;
				TabbedSection.onclick(node); 
			} else if (node.hasClass('tab')){
				needToScroll = true;
				//fixme need to jump to the correct toggler
				//alert(node.className+' '+node.getPrevious().className);
				//node.getPrevious().fireEvent('onClick');
			} else if ( /*(node.style) &&*/ node.visible()){
				needToScroll = true;
				//fixme need to find the correct toggler
				node.show(); //eg collapsedBoxes: fixme
			}
			node = node.getParent();
		}
		if( needToScroll ) $(h).scrollTo();
	}
}


/** 100 Wiki functions **/
var Wiki = {

	init: function(props){
		Object.extend(Wiki,props || {'DELIM':'\u00A4'}); 
		this.BasePath = this.BaseURL.slice( this.BaseURL.indexOf( location.host )
											+ location.host.length, -1 );
        //this.Language = navigator.language ? navigator.language : navigator.userLanguage;
		//this.ClientTimezone = new Date().getTimezoneOffset()/60;
	},
	getPageElement: function(){ 
		return($('page') || $E('.page'));
	},
	onPageLoad: function(){
		this.PermissioneEdit = ( $E('.actionEdit') != undefined ); //deduct permission level

		// put focus on the first form element within a page
		var f = $('workarea')	// plain.jsp
			|| $('editorarea')	// plain.jsp
			|| $('editor')		// Comment.jsp
			|| $('j_username')	// Login.jsp
			|| $('prefSkin')	// UserPreference.jsp
			|| $('query2');		// Search.jsp
		if(f && f.visible()) f.focus();	//IE chokes when focus on invisible element

		return;
		/* visual sugar: turn More... select-box into hover dropdown */
		/* TODO - css is not yet ok*/
		var more = $('actionsMorePopupItems'),
			hover = more.getParent().effect('opacity', {wait:false}).set(0),
			selection = $('actionsMore');
			
		//selection.getParent().setProperty('visibility','hidden');//hide();
		more.getParent().getParent().show()
		 	.addEvent('mouseout',(function(){ hover.start(0) }).bind(this))
			.addEvent('mouseover',(function(){ hover.start(0.9) }).bind(this));
		
		$A(selection.options).each(function(o){
			new Element('a').setProperty('href',o.value).addClass(o.className).setHTML(o.text)
				.injectInside( new Element('li').injectInside(more) );
		});
		//$('actionsmenu').remove();
	},

	/* SubmitOnce: hide the real submit button, add proxy buttons which get disabled on submit */
	submitOnce: function(form){
		$A(form.elements).each(function(e){
			if( e.type.toLowerCase() == 'button') e.disabled = true;
		});
		return true;
	}
}


/** 110 WikiSlimbox
 ** Inspired http://www.digitalia.be/software/slimbox by Christophe Bleys
 ** Dirk Frederickx, Mar 2007
 ** 	%%slimbox [...] %%
 ** 	%%slimbox-img  [some-image.jpg] %%
 ** 	%%slimbox-ajax [some-page links] %%
 **/
var WikiSlimbox = {

	onPageLoad: function(){
		var i=0,
			lnk=new Element('a').addClass('slimbox').setHTML('&raquo;');
			
		$$('*[class^=slimbox]').each(function(slim){
			var group = 'lightbox'+ i++,
				parm = slim.className.split('-')[1] || 'img ajax',
				filter = [];
			if(parm.test('img')) filter.extend(['img.inline', 'a.attachment']); 
			if(parm.test('ajax')) filter.extend(['a.wikipage', 'a.external']); 
			$ES(filter.join(','),slim).each(function(el){
				var href = el.src||el.href;
				var rel = (el.className.test('inline|attachment')) ? 'img' : 'ajax';
				if((rel=='img') && !href.test('(.bmp|.gif|.png|.jpg|.jpeg)(\\?.*)?$')) return;
				lnk.clone().setProperties({'href':href, 'rel':group+' '+rel,'title':el.alt||el.getText()})
				lnk.clone().setProperties({'href':href, 'rel':group+' '+rel,'title':el.alt||el.getText()})
					.injectBefore(el);
				if(el.src) 
					el.replaceWith(new Element('a').addClass('attachment').setProperty('href',el.src).setHTML(el.alt||el.getText()));
			});
		});
		if(i) Lightbox.init({errorMessage:"slimbox.error".localize()});
		//new Asset.javascript(Wiki.TemplateDir+'scripts/slimbox.js');
	}
}

/*
	Slimbox v1.3 - The ultimate lightweight Lightbox clone
	by Christophe Beyls (http://www.digitalia.be) - MIT-style license.
	Inspired by the original Lightbox v2 by Lokesh Dhakar.

	Updated by Dirk Frederickx ./ JSPWiki
	- minimum size of image canvas DONE
	- add maximum size of image w.r.t window size DONE
	- CLOSE icon -> close x text iso icon DONE
	- <<prev, next>> links added in visible part of screen DONE
	- add size of picture to info window DONE
	- spacebor, down arrow, enter : next image DONE
	- up arrow : prev image DONE
	- allow the same picture occuring several times DONE
	- add support for external page links  => slimbox_ex DONE
	- FFS make resizable, & draggable 
*/
var Lightbox = {

	init: function(options){
		this.options = Object.extend({
			resizeDuration: 400,
			resizeTransition: Fx.Transitions.sineInOut,
			initialWidth: 250,
			initialHeight: 250,
			animateCaption: true,
			errorMessage: '<h2>Error</h2>There was a problem with your request.<br />Please try again.'
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

		this.overlay = new Element('div').setProperty('id', 'lbOverlay').injectInside(document.body);

		this.center = new Element('div').setProperty('id', 'lbCenter').setStyles({width: this.options.initialWidth+'px', height: this.options.initialHeight+'px', marginLeft: '-'+(this.options.initialWidth/2)+'px', display: 'none'}).injectInside(document.body);
		this.image = new Element('div').setProperty('id', 'lbImage').injectInside(this.center);
		this.prevLink = new Element('a').setProperties({id: 'lbPrevLink', href: '#', title:'Previous [up arrow] [<-left arrow]'}).setStyle('display', 'none').setHTML('&laquo; Prev').injectInside(this.center);
		this.nextLink = this.prevLink.clone().setProperties({id:'lbNextLink', title:'Next [space] [down arrow] [right arrow->]'}).setHTML('Next &raquo;').injectInside(this.center);
		this.prevLink.onclick = this.previous.bind(this);
		this.nextLink.onclick = this.next.bind(this);

		this.bottomContainer = new Element('div').setProperty('id', 'lbBottomContainer').setStyle('display', 'none').injectInside(document.body);
		this.bottom = new Element('div').setProperty('id', 'lbBottom').injectInside(this.bottomContainer);
		new Element('a').setProperties({id: 'lbCloseLink', href: '#', title:'Close [Esc]'}).setHTML('Close &#215;').injectInside(this.bottom).onclick = this.overlay.onclick = this.close.bind(this);
		this.caption = new Element('div').setProperty('id', 'lbCaption').injectInside(this.bottom);
		this.number = new Element('div').setProperty('id', 'lbNumber').injectInside(this.bottom);
		this.error = new Element('div').setProperty('id', 'lbError').setHTML(this.options.errorMessage);
		
		new Element('div').setStyle('clear', 'both').injectInside(this.bottom);

		var nextEffect = this.nextEffect.bind(this);
		this.fx = {
			overlay: this.overlay.effect('opacity', {duration: 500}).hide(),
			resize: this.center.effects({duration: this.options.resizeDuration, transition: this.options.resizeTransition, onComplete: nextEffect}),
			image: this.image.effect('opacity', {duration: 500, onComplete: nextEffect}),
			bottom: this.bottom.effect('margin-top', {duration: 400, onComplete: nextEffect})
		};

		this.fxs = new Fx.Elements([this.center, this.image], {duration: this.options.resizeDuration, transition: this.options.resizeTransition, onComplete: nextEffect});
		
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
		this.center.setStyles({top: this.top+'px', display: ''});
		this.fx.overlay.start(0.7);
		return this.changeImage(imageNum);
	},

	position: function(){
		this.overlay.setStyles({top: window.getScrollTop()+'px', height: window.getHeight()+'px'});
	},

	setup: function(open){
		var elements = $A(document.getElementsByTagName('object'));
		if (window.ie) elements.extend(document.getElementsByTagName('select'));
		elements.each(function(el){ el.style.visibility = open ? 'hidden' : ''; });
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

		this.bottomContainer.style.display = this.prevLink.style.display = this.nextLink.style.display = 'none';
		this.fx.image.hide();
		this.center.className = 'lbLoading';

		this.preload = new Image();
		this.image.setHTML('').setStyle('overflow','hidden');
        if( this.images[imageNum][2] == 'img' ){
			this.preload.onload = this.nextEffect.bind(this);
			this.preload.src = this.images[imageNum][0];
		} else {			
			//FIXME -- better to work with iFrame
			this.ajaxFailed = false;
			var nextEffect = this.nextEffect.bind(this);
			var ajaxFailure = this.ajaxFailure.bind(this);
			var ajaxOptions = {
				method: 	'get',
				update: 	this.image, 
//				evalScripts: true,
//				evalResponse: true,
				onComplete: this.nextEffect.bind(this), 
				onFailure:  this.ajaxFailure.bind(this)
			};
			this.ajaxRequest = new Ajax(this.images[imageNum][0], ajaxOptions).request();
		}
		return false;
	},

	ajaxFailure: function (){
		this.ajaxFailed = true;
		this.image.setHTML('').adopt(this.error.clone());
		this.nextEffect();
//		this.center.setStyle('cursor', 'pointer');
//		this.bottom.setStyle('cursor', 'pointer');
//		this.center.onclick = this.bottom.onclick = this.close.bind(this);		
	},
	

	nextEffect: function(){
		switch (this.step++){
		case 1:
			this.center.className = '';
			this.caption.setHTML('').adopt(new Element('a')
				.setProperties({
					'href':this.images[this.activeImage][0],
					'title':"slimbox.directLink".localize()
				}).setHTML(this.images[this.activeImage][1] || ''));
			var type = (this.images[this.activeImage][2]=='img') ? "slimbox.info" : "slimbox.remote Request";
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
			this.image.style.height = this.prevLink.style.height = this.nextLink.style.height = h+'px';
			
		if( this.images[this.activeImage][2]=='img') {
			this.image.style.backgroundImage = 'url('+this.images[this.activeImage][0]+')';

			if (this.activeImage) this.preloadPrev.src = this.images[this.activeImage-1][0];
			if (this.activeImage != (this.images.length - 1)) this.preloadNext.src = this.images[this.activeImage+1][0];
			
			this.number.setHTML(this.number.innerHTML+'&nbsp;&nbsp;['+this.preload.width+'&#215;'+this.preload.height+']');
			}
			this.step++;

		case 2:
			this.step++;

		case 3:
			if (this.options.animateCaption) this.bottomContainer.setStyles({height: '0px', display: ''});

			this.fxs.start({
				'0': { height: [this.image.offsetHeight], width: [this.image.offsetWidth], marginLeft: [-this.image.offsetWidth/2] },
				'1': { opacity: [1] }
			});	

			break;
		case 4:
			this.image.setStyle('overflow','auto');
			this.bottomContainer.setStyles({ top: (this.top + this.center.clientHeight)+'px', marginLeft: this.center.style.marginLeft });
			if (this.options.animateCaption){
				this.fx.bottom.set(-this.bottom.offsetHeight);
				this.bottomContainer.style.height = '';
				this.fx.bottom.start(0);
				break;
			}
			this.bottomContainer.style.height = '';
		case 5:
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
		return false;
	}
};


/** 114 Reflection
 ** Inspired by Reflection.js at http://cow.neondragon.net/stuff/reflection/
 ** Freely distributable under MIT-style license.
 ** Adapted for JSPWiki/BrushedTemplate, D.Frederickx, Sep 06
 ** Use:
 ** 	%%Reflection-height-opacity  [some-image.jpg] %%
 **/
var WikiReflection = {

	onPageLoad: function(){
		$$('*[class^=reflection]').each( function(w){
			var parms = w.className.split('-');
			$ES('img',w).each(function(img){
				Reflection.add(img, parms[1], parms[2]);
			}); 
		});
	}
}
/* FIXME : add delayed loading of reflection library */
var Reflection = {

	defaultHeight: 0.3,
	defaultOpacity: 0.5,

	add: function(img, height, opacity)
	{
		//Reflection.remove(image); FIXME - is this still needed?
		height  = ( height  ) ? height/100 : this.defaultHeight;
		opacity = ( opacity ) ? opacity/100: this.defaultOpacity;

		try
		{
			var div  = document.createElement('div'); //umbrella div
			var imgW = imgWX = img.width; imgWX +='px'; 
			var imgH = img.height;
			var rH   = Math.floor(imgH * height); //reflection height

			div.style.cssText =
			img.style.cssText = 'vertical-align:bottom';

			div.className = img.className.replace(/\bReflection\b/, "");
			//div.style.width = imgWX;
			div.style.width = "100%";
			div.style.maxWidth = imgWX;
			div.style.height = Math.floor(imgH * (1+height) ) + 'px';

			img.className = 'inline reflected';  //FIXME: is this still needed ??
			img.parentNode.replaceChild(div, img);
			div.appendChild(img);

			if( document.all && !window.opera ){  //ie
				var r = new Element('img');
				div.appendChild(r);
				r.src = img.src;
				r.style.width = imgWX;
				r.style.marginBottom = "-" + (imgH - rH) + 'px';
				r.style.filter = 'flipv progid:DXImageTransform.Microsoft.Alpha(opacity='+(opacity*100)+', style=1, finishOpacity=0, startx=0, starty=0, finishx=0, finishy='+(height*100)+')';
			} else {
				var r = new Element('canvas');
				if( !r.getContext ) return;

				div.appendChild(r);
				r.width  = imgW;  r.style.width  = imgWX;
				r.height = rH;    r.style.height = rH + 'px';

				var ctx = r.getContext("2d");
				ctx.save();
				ctx.translate(0, imgH-1);
				ctx.scale(1, -1);
				ctx.drawImage(img, 0, 0, imgW, imgH);
				ctx.restore();
				ctx.globalCompositeOperation = "destination-out";

				var g = ctx.createLinearGradient(0, 0, 0, rH);
				g.addColorStop( 0, "rgba(255, 255, 255, " + (1 - opacity) + ")" );
				g.addColorStop( 1, "rgba(255, 255, 255, 1.0)" );
				ctx.fillStyle = g;
				ctx.rect( 0, 0, imgW, rH );
				ctx.fill(); 
			}
		}
		catch (e) { }
	}
}
 
/** 120 brushed quick links **/
var QuickLinks = {

	onPageLoad: function(){
		if( $("previewcontent") || !Wiki.PrefShowQuickLinks ) return;

		var sections = $$('#pagecontent *[id^=section]');
		if(sections.length==0) return;
		
		var ql,qlPrev,qlEdit,qlNext;
		function qql(clazz,title,text,href){
			var a = new Element('a').setProperty('title',title.localize()).setHTML(text);
			if(href) a.setProperty('href',href);
			ql.adopt(new Element('span').addClass(clazz).adopt(a));
			return(a);
		}
		ql = new Element('div').addClass('quicklinks');
		qql('quick2Top','quick.top','&laquo;','#wikibody');
		qlPrev = qql('quick2Prev','quick.previous','&lsaquo;');

		if(Wiki.PermissioneEdit) qlEdit = qql('quick2Edit','quick.edit','&bull;');

		qlNext = qql('quick2Next','quick.next','&rsaquo;');
		qql('quick2Bottom','quick.bottom','&raquo;','#footer');
				
		var last = sections.length-1;
		sections.each(function(s,i){
			qlNext.setProperty('href',(i==last) ? '#footer' : '#'+sections[i+1].id);
			qlPrev.setProperty('href',(i==0) ? '#wikibody' : '#'+sections[i-1].id);
			if(qlEdit) qlEdit.setProperty('href','Edit.jsp?page='+Wiki.PageName+'&section='+(i+1));
			s.adopt(ql.clone());
		});
	}
}

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

	onPageLoad: function(){
		$$('.tabbedSection').each( function(tt){
			tt.addClass('tabs'); //css styling is on tabs
			var tabmenu = new Element('div').addClass('tabmenu').injectBefore(tt);

			tt.getChildren().each(function(tab,i) {
				if( !tab.className.test('^tab-') ) return;

				if( !tab.id || (tab.id=="") ) tab.id = tab.className;
				var title = tab.className.substr(4).deCamelize();

				//use class to make tabs visible during printing !
				(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

				var span = new Element('span').injectInside(tabmenu);
				var menu = new Element('a')
					.setProperties({'id':'menu-'+tab.id, 'href':'javascript:void(0)'})
					.addEvent('click',function(){ TabbedSection.onclick(tab.id); })
					.appendText(title)
					.injectInside(span);
				if( i==0 ) menu.addClass('activetab');        
			});
		}, this);
	},

	onclick: function(tabID){
		var tab = $(tabID), 
			tabs = $(tab.parentNode),
			menu = $('menu-'+tab.id);

		for(var t=tabs.getFirst(); t; t=t.getNext()){
			if( !t.id ) continue;
			var m = $('menu-'+t.id);
			if( !m || !m.hasClass('activetab') ) continue;

			if( t.id == tabID ) break; //already active
			m.removeClass('activetab');      
			menu.addClass('activetab');
			t.addClass('hidetab');
			tab.removeClass('hidetab').show();
			break;
		}
	}
}

/** 132 Accordion for Tabs, Accordeons, CollapseBoxes
 **
 ** Following markup:
 ** <div class="accordion">
 **		<div class="tab-FirstTab">...<div>
 **		<div class="tab-SecondTab">...<div>
 ** </div>
 **
 **	is changed into
 **	<div class="accordion">
 **		<div class="toggle active">First Tab</div>
 **		<div class="tab-FirstTab tab active">...</div>
 **		<div class="toggle">Second Tab</div>
 **		<div class="tab-SecondTab">...</div>
 **	</div>
 **/
var WikiAccordion = {

	onPageLoad: function(){
		$$('.accordion, .tabbedAccordion').each( function(tt){
			
			var toggles=[], contents=[], togglemenu=false;
			if(tt.hasClass('tabbedAccordion')) togglemenu = new Element('div').addClass('togglemenu').injectBefore(tt);
			
			tt.getChildren().each(function(tab) {
				if( !tab.className.test('^tab-') ) return;

				//FIXME use class to make tabs visible during printing 
				//(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

				var title = tab.className.substr(4).deCamelize();
				if(togglemenu) {
					toggles.push(new Element('div').addClass('toggle').injectInside(togglemenu).appendText(title));
				} else {
					toggles.push(new Element('div').addClass('toggle').injectBefore(tab).appendText(title));
				}        
				contents.push(tab.addClass('tab'));
			});

			new Accordion(toggles, contents, {     
				alwaysHide: !togglemenu,
				onComplete: function(){
							var el = this.elements[this.previous];
							if (el.offsetHeight > 0) el.setStyle('height', 'auto');  
				},
				onActive: function(toggle,content){                          
							toggle.addClass('active'); content.addClass('active'); },
				onBackground: function(toggle,content){ 
							toggle.removeClass('active'); content.removeClass('active');} 
			});
		});
	}
}


/* 140 SearchBox
 * Remember 10 most recent search topics
 * Uses a cookie to store to 10 most recent search topics
 * Extensions for quick links for view, edit and clone (ref. idea of Ron Howard - Nov 05)
 * Refactored for mootools, April 07
 * Add AJAX and hover effect.
 */
var SearchBox = {

	Max:     9, //max recent search items
	
	onPageLoad: function(){
		this.form = $('searchForm'); if( !this.form ) return;
		this.form.addEvent('submit',this.submit.bindAsEventListener(this))
				 .addEvent('mouseout',(function(){ this.hover.start(0) }).bind(this))
				 .addEvent('mouseover',(function(){ this.hover.start(0.9) }).bind(this));
		$('recentClear').addEvent('click', this.clear.bindAsEventListener(this));
		$('recentSearches').show(); 

		this.hover = $('searchboxMenu').setProperty('visibility','visible')
			.effect('opacity', {wait:false}).set(0);
    
	    this.query = $('query'); this.query.observe(this.compactSearch.bind(this) ); 

		this.recent = (Cookie.get('JSPWikiSearchBox') || "").split(Wiki.DELIM);
		if( this.recent.length == 0 ) return;

		var ul = $('recentItems'), go = $('advancedSearch'); q = this.form.query;
		this.recent.each(function(el){
			new Element('a').setProperty('href','#').setHTML(el)
				.addEvent('click',function(){ q.value = el; q.focus(); })
				.injectInside( new Element('li').injectInside(ul) );
		});
	},

	submit: function(){ 
		var v = this.form.query.value;
		if( !this.recent.test(v) ){
			if(this.recent.length > this.Max) this.recent.pop();
			this.recent.unshift(v);
			Cookie.set(this.Cookie, this.recent.join(Wiki.DELIM), Wiki.BasePath);
		}
		if(v.trim() != '') location.href = this.form.action + '?query=' + v;
		return false;
	},

	clear: function(){
		Cookie.remove(this.Cookie);
		this.recent = [];
		$('recentSearches').hide();
	},

	//TODO add spinner
    compactSearch: function(){ 
      var qv = this.query.value ;
      if( (qv==null) || (qv.trim()=="") || (qv==this.query.defaultValue) ) return;

	  new Ajax( Wiki.TemplateDir+'AjaxSearch.jsp', {
	  	update: 'searchResult', 
	  	postBody: $('searchForm').toQueryString(), 
	  	method: 'post'
	  }).request();
    },

	/* navigate to url, after smart pagename handling */
	navigate: function(url, promptText, clone, search){
		var p = Wiki.PageName, s = this.query.value;
		if(s == this.query.defaultValue) s = '';

		if(s == ''){
			s = prompt(promptText, (clone) ? p+'sbox.clone.suffix'.localize() : p);
			if( !s || (s == '') ) return false;
		}
		//if(!search) s = s.replace(/[^A-Za-z0-9._\/]/g, ''); //valid pagename FIXME
		if( clone && (s != p) )  s += '&clone=' + p;

		if(s == '') return false; //dont exec the click
		location.href = url.replace('__PAGEHERE__', s);
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
var WikiColors =
{
	HTMLColors :
	{ black  :"000000", green :"008000", silver :"C0C0C0", lime  :"00FF00",
	  gray   :"808080", olive :"808000", white  :"FFFFFF", yellow:"FFFF00",
	  maroon :"800000", navy  :"000080", red    :"FF0000", blue  :"0000FF",
	  purple :"800080", teal  :"008080", fuchsia:"FF00FF", aqua  :"00FFFF" },


	gradient: function( percent, from, to )
	{
		if( percent < 0 ) percent = 0;
		if( percent > 1 ) percent = 1;

		var r = to[0]-from[0], g = to[1]-from[1], b = to[2]-from[2];

		return this.asHTML( [from[0]+r*percent, from[1]+g*percent, from[2]+b*percent] );
	},

	invert: function( color )
	{
		color = this.parse( color );

		return( [ 255-color[0], 255-color[1], 255-color[2] ] );
	},

	/* Convert html-code to Array(r,g,b)  EG: 00FFFF = Array(0,255,255) */
	parse: function( c )
	{
		if(c instanceof Array) return( c ); //already done??
		c = c.toLowerCase();
		if( WikiColors.HTMLColors[c] ) c = WikiColors.HTMLColors[c];

		return c.hexToRgb(true);
	},

	/* Convert Array(r,g,b) to string  EG: [0,255,255]    = #00FFFF */
	/* Convert rgb(r,g,b)   to string  EG: rgb(0,255,255) = #00FFFF */
	/* Convert ffff00       to string  EG: '00FFFF'       = #00FFFF */
	REparseColor:  new RegExp( "^[0-9a-fA-F]+$" ),
	RErgbFormat: new RegExp( "rgb\\((\\d+),\\s*(\\d+),\\s*(\\d+)\\)","i"),
	_array2hex: function( arr )
	{
		s=[];
		for( i = 0; i<arr.length; i++ )
		{
			var v = Math.round( arr[i] ).toString( 16 );
			if( v.length < 2)  s.push('0');
			s.push(v);
		}
		return s.join('');
	},
	asHTML: function ( c )
	{
		var result = '';

		if( c instanceof Array)
		{
			result = this._array2hex( c.slice(0,3) );
		}
		else if( this.RErgbFormat.test( c ) )
		{
			result = this._array2hex( [RegExp.$1, RegExp.$2, RegExp.$3] );
		}
		else
		{
			result = c;
		}
		if( this.REparseColor.test(result) ) result = '#' + result;
		return result;
	}
}

var GraphBar =
{
	REclassName: new RegExp( "(?:^| )graphBars([^-]*)(-\\S+)?" ),

	onPageLoad : function(){
		$$('*[class^=graphBars]').each( function(g){
			/* parse parameters */
			var lbound = 20,	//max - lowerbound size of bar
				ubound = 320,	//min - upperbound size of bar
				color1 = null,	// bar color
				color2 = null,	// 2nd bar color used depending on bar-type
				isHorizontal = true,	// orientation horizontal or vertical
				isProgress   = false,	// progress bar
				isGauge      = false;	// gauge bar

			GraphBar.REclassName.test(g.className);
			var barName = RegExp.$1;
			var parms = RegExp.$2.toLowerCase().split('-');
			
			for(var pi=0; pi < parms.length; pi++){
				p = parms[pi];
				if(p == "vertical")	{ isHorizontal = false; }
				else if(p == "progress")	{ isProgress = true;  }
				else if(p == "gauge")	{ isGauge = true; }
				else if(p.indexOf("min") == 0)	{ lbound = parseInt( p.substr(3), 10 ); }
				else if(p.indexOf("max") == 0)	{ ubound = parseInt( p.substr(3), 10 ); }
				else {
					p = WikiColors.parse( p );
					if(!color1) color1 = p; 
					else if(!color2) color2 = p;
				}
			}
			if( lbound > ubound ) { var m = ubound; ubound=lbound; ubound=m;  }
			if( isProgress && !color1 ) color2 = "transparent";
			if( (isGauge || isProgress)  && color1 && !color2 ) color2 = WikiColors.invert( color1 );
			if( color1 && !color2 ) color2 = color1;

			/* collect all gBar elements */
			var bars = $ES('.gBar'+barName, g);
			//alert(barName+' '+bars+' '+bars.length);
			if( !bars && barName && (barName!="")){  // check for matching table columns or rows
				bars = this.getTableValues( g, barName );
			}
			if( !bars ) return;

			/* parse bar data and scale according to lbound an ubound */
			var barData = this.parseBarData( bars, lbound, ubound );

			/* modify DOM for each bar element */
			var e = new Element( "span" ).addClass('graphBar').setHTML('&nbsp;');

			//alert( color1+" " +color2);
			bars.each(function(b,j){
				//var b  = bars[j];     //gBar element
				var pb = b.getParent(); //parent of gBar element
				var n  = e.clone();     //principal bar to be inserted
				var nn = null;          //2nd bar to be inserted ico progress
				var c  = null;          //principal bar color

				if(color1){
					var percent = ( j / (bars.length-1) );
					if( isGauge ) percent = (barData[j]-lbound) / (ubound-lbound);
					c = WikiColors.gradient( percent, color1, color2 ); //colorTable[gg]; //get color value
				}
				if(isHorizontal){
					n.setStyles({'borderLeftStyle':'solid', 'borderLeftWidth':barData[j]+'px'});

					if(isProgress){
						nn = n.clone();
						n.setStyles({'paddingRight':'0', 'borderLeftWidth':(ubound-barData[j])+'px','marginLeft':'-1ex'});
						if( color1 ) nn.setStyle('borderLeftColor', WikiColors.asHTML(color1));
						if( color2 ) n.setStyle('borderLeftColor', WikiColors.asHTML(color2));
					} else {
						if( c ) n.setStyle('borderLeftColor',c);
					}
				}
				else /* vertical bars */
				{
					n.setStyles({'borderBottomStyle':'solid', 'borderBottomWidth':barData[j]+'px'});
					b.setStyle('position', 'relative'); //needed for inserted spans ;-)) hehe

					// behaviour of relative on a TD is undefined - so insert a help div block
					// see http://www.w3.org/TR/CSS21/visuren.html#propdef-position ;-(
					if( pb.nodeName == 'TD' ) { pb = new Element('div').injectWrapper( pb );  }

					var fontsize = 20;  //can we read this info somewhere ? nono
					pb.setStyles({'height':(ubound+fontsize)+'px', 'position':'relative'});

					if( !isProgress ) { b.setStyle('top', (ubound - barData[j])+'px'); }

					n.setStyles({'position':'absolute', 'width':'1em', 'bottom':'0'});

					if(isProgress){
						if( color1 ) n.style.borderBottomColor = WikiColors.asHTML( color1 );

						nn = n.clone().setStyle('borderBottomWidth', ubound+'px'); //background bar with color
						if( color2) nn.setStyle('borderBottomColor', WikiColors.asHTML(color2));
					} else {
						if( c ) n.setStyle('borderBottomColor', c);
					}
				}
				if( nn ) pb.insertBefore( nn, b );
				pb.insertBefore( n, b );

			},this);
			e = null;
		},this);
	},

	parseBarData: function( nodes, lbound, ubound )
	{
		var barData = [], count = nodes.length; maxValue = Number.MIN_VALUE, minValue = Number.MAX_VALUE;
		var span = ubound - lbound;

		var isnum = true, isdate = true;

		for(var j=0; j < count; j++){
			var s = $getText( nodes[j] );
			barData[j] = s;
			if( isnum  ) isnum  = !isNaN( parseFloat( s.match( Number.REparsefloat ) ) );
			if( isdate ) isdate = !isNaN( Date.parse( s ) );
		}

		for(var j=0; j < count; j++){
			var k = barData[j];
			if( isdate )     { k = new Date( Date.parse( k ) ).valueOf();  }
			else if( isnum ) { k = parseFloat( k.match( Number.REparsefloat ) ); }

			maxValue = Math.max( maxValue, k );
			minValue = Math.min( minValue, k );
			barData[j] = k;
		}

		for( var j=0; j < count; j++ ){ /* scale values */
			barData[j] = parseInt( (span * (barData[j]-minValue) / (maxValue-minValue) ) + lbound ) ;
		}
		return barData;
	},

	/*
	 * Fetch set of gBar values from a table
	 * Check first-row to match field-name: return array with col values
	 * Check first-column to match field-name: return array with row values
	 * insert SPANs qs place-holder of the missing gBars
	 */
	getTableValues: function( node, fieldName )
	{
		//alert(node +' '+fieldName);
		var table = $E('table', node); if(!table) return null;
		var tlen = table.rows.length;

		if( tlen > 1 ) /* check for COLUMN based table */
		{
			var r = table.rows[0];
			for( var h=0; h < r.cells.length; h++ )
			{
				if( $getText( r.cells[h] ).trim() == fieldName )
				{
					var result = [];
					for( var i=1; i< tlen; i++)
						result.push( new Element('span').injectWrapper(table.rows[i].cells[h]) );
					return result;
				}
			}
		}

		for( var h=0; h < tlen; h++ )  /* check for ROW based table */
		{
			var r = table.rows[h];
			if( $getText( r.cells[0] ).trim() == fieldName )
			{
				var result = [];
				for( var i=1; i< r.cells.length; i++)
					result.push( new Element('span').injectWrapper(r.cells[i]) );
				return result;
			}
		}
		return null;
	}
}

/**
 ** 200 Collapsable list items
 **
 ** See also David Lindquist <first name><at><last name><dot><net>
 ** See: http://www.gazingus.org/html/DOM-Scripted_Lists_Revisited.html
 **
 ** Add stuff to support collabsable boxes, Nov 05, D.Frederickx
 **
 **/
var Collapsable =
{
	tmpcookie   : null,
	cookies     : [],
	cookieNames : [],

	ClassName       : "collapse",
	ClassNameBox    : "collapsebox",
	REClassNameBox  : new RegExp( "(?:^| )collapsebox(-closed)?" ),
	ClassNameBody   : "collapsebody",
	CollapseID      : "clps", //prefix for unique IDs of inserted DOM nodes
	MarkerOpen      : "O",    //cookie state chars
	MarkerClose     : "C",
	CookiePrefix    : "JSPWikiCollapse",

	onPageLoad: function(){
		this.OpenTip  = "collapse".localize();
		this.CloseTip = "expand".localize();

		this.bullet = new Element('div').addClass('collapseBullet').setHTML('&bull;');

		this.initialise( "favorites",   this.CookiePrefix + "Favorites" );
		this.initialise( "pagecontent", this.CookiePrefix + Wiki.PageName );
	},

	initialise: function(domID, cookieName){
		var page = $(domID); if(!page) return;
		this.tmpcookie = Cookie.get(cookieName);
		this.cookies.push( "" ) ; //initialise new empty collapse cookie
		this.cookieNames.push( cookieName );

		$ES('.collapse', page).each(function(el){ Collapsable.collapseNode(el); });   
		$ES('*[class^=collapsebox]', page).each(function(el){ Collapsable.collapseBox(el); });
	},

	collapseBox: function(node){
		var title = node.getFirst(); if( !title ) return;
		/*while( (title != null) && (!this.REboxtitle.test( title.nodeName.toLowerCase() )) )
		{
			title = title.nextSibling;
		}
		if( !title || !title.nextSibling ) return;
		*/

		/*put wrapper around all remaining siblings*/
		var body = new Element('div').addClass('collapsebody');
		while(title.nextSibling) body.appendChild(title.nextSibling);
		node.appendChild(body);
		
		var isClosed = node.hasClass('collapsebox-closed');
		node.className = node.className.replace( this.REClassNameBox, this.ClassNameBox ) ;

		var bullet  = this.bullet.clone();//.injectAfter(title);
		this.initBullet( bullet, body, (isClosed) ? this.MarkerClose : this.MarkerOpen );
		title.appendChild( bullet ).addClass('collapseboxtitle');
	},

	// Modifies the list such that sublists can be hidden/shown by clicking the listitem   bullet
	// The listitem bullet is a node inserted into the DOM tree as the first child of the
	// listitem containing the sublist.
	collapseNode: function(node){
		var items = node.getElementsByTagName("li");
		for( i=0; i < items.length; i++ )
		{
			var nodeLI = items[i];
			var nodeXL = ( nodeLI.getElementsByTagName("ul")[0] ||
										 nodeLI.getElementsByTagName("ol")[0] );

			//dont insert bullet when LI is "empty" -- iow it has no text or no non ulol tags inside
			//eg. * a listitem
			//    *** a nested list item - intermediate level is empty
			var emptyLI = true;
			for( var n = nodeLI.firstChild; n ; n = n.nextSibling )
			{
				if((n.nodeType == 3 ) && ( n.nodeValue.trim() == "" ) ) continue; //keep searching
				if((n.nodeName == "UL") || (n.nodeName == "OL")) break; //seems like an empty li
				emptyLI = false;
				break;
			}
			if( emptyLI ) continue; //do not insert a bullet

			var bullet = this.bullet.clone();

			if( nodeXL )
			{
				var defaultState = (nodeXL.nodeName == "UL") ? this.MarkerOpen: this.MarkerClose ;
				this.initBullet( bullet, nodeXL, defaultState );
			}
			nodeLI.insertBefore( bullet, nodeLI.firstChild );
		}
	},


	// initialise bullet according to parser settings
	initBullet: function( bullet, body, defaultState )
	{
		var collapseState = this.parseCookie( defaultState );
		bullet.onclick = this.toggleBullet;
		bullet.id = this.CollapseID + "." + (this.cookies.length-1) +
												          "." + (this.cookies.getLast().length-1);
		this.setOpenOrClose( bullet, ( collapseState == this.MarkerOpen ), body );
	},

	setOpenOrClose: function( bullet, setToOpen, body )
	{ 
		var fx = $(body).setStyle('overflow','hidden')
						.effect('height', { wait:false,
							onComplete:function(){ if(setToOpen) $(body).setStyle('height','auto'); } 
						});
		if(setToOpen){
			$(bullet).setProperties({'title':this.OpenTip, 'class':'collapseOpen'}).setHTML('&raquo;');
			fx.start(body.scrollHeight);
		} else {
			$(bullet).setProperties({'title':this.CloseTip, 'class':'collapseClose'}).setHTML('&laquo;');
			fx.start(body.scrollHeight, 0);
		} 
	},

	// parse cookie
	// this.tmpcookie contains cookie being validated agains the document
	// this.cookies.last contains actual cookie being constructed
	//    this cookie is stored in the cookies[]
	//    and only persisted when the user opens/closes something
	// returns collapseState MarkerOpen, MarkerClose
	parseCookie: function( token )
	{
		var currentcookie = this.cookies.getLast();
		var cookieToken = token; //default value

		if( (this.tmpcookie) && (this.tmpcookie.length > currentcookie.length) )
		{
			cookieToken = this.tmpcookie.charAt( currentcookie.length );
			if(  ( (token == this.MarkerOpen) && (cookieToken == this.MarkerClose) )
				|| ( (token == this.MarkerClose) && (cookieToken == this.MarkerOpen) ) ) //##fixed
					token = cookieToken ;
			if( token != cookieToken )  //mismatch between tmpcookie and expected token
					this.tmpcookie = null;
		}
		this.cookies[this.cookies.length - 1] += token; //append and save currentcookie

		return( token );
	},

	// toggle bullet and update corresponding cookie
	// format of ID of bullet = "collapse.<cookies-index>.<cookie-charAt>"
	toggleBullet: function(){
		var ctx = Collapsable; //avoid confusion with this == clicked bullet

		var idARR  = this.id.split(".");  if( idARR.length != 3 ) return;
		var cookie = ctx.cookies[idARR[1]]; // index in cookies array

		var body = $(this.parentNode);
		if( body.getTag().test('h2|h3|h') ){
			body = body.getNext();
		} else {
			body = $E('ul,ol', body);
		}
		if( !body ) return;

		//ctx.setOpenOrClose( this, (body.style.display == "none"), body );
		ctx.setOpenOrClose( this, (body.offsetHeight == 0), body );

		var i = parseInt(idARR[2]); // position inside cookie
		var c = ( cookie.charAt(i) == ctx.MarkerOpen ) ? ctx.MarkerClose: ctx.MarkerOpen;
		cookie = cookie.substring(0,i) + c + cookie.substring(i+1) ;

		Cookie.set(ctx.cookieNames[idARR[1]], cookie, Wiki.BasePath);
		ctx.cookies[idARR[1]] = cookie;

		return false;
	}
}

/**
 ** 220 RoundedCorners --experimental
 ** based on Nifty corners by Allesandro Fulciniti
 ** www.pro.html.it
 ** Refactored for JSPWiki
 **
 ** JSPWiki syntax:
 **
 **  %%roundedCorners-<corners>-<color>-<borderColor>
 **  %%
 **
 **  roundedCorners-yyyy-ffc5ff-c0c0c0
 **
 **  corners: "yyyy" where first y: top-left,    2nd y: top-right,
 **                           3rd y: bottom-left; 4th y: bottom-right
 **     value can be: "y": Normal rounded corner (lowercase y)
 **                    "s": Small rounded corner (lowercase s)
 **                    "n": Normal square corner
 **
 **/

var RoundedCorners =
{
	/** Definition of CORNER dimensions
	 ** Normal    Normal+Border  Small  Small+Border
	 ** .....+++  .....BBB       ..+++  ..BBB
	 ** ...+++++  ...BB+++       .++++  .B+++
	 ** ..++++++  ..B+++++       +++++  B++++
	 ** .+++++++  .B++++++
	 ** .+++++++  .B++++++
	 ** ++++++++  B+++++++
	 **
	 ** legend: . background, B border, + forground color
	 **/
	NormalTop :
		 [ { margin: "5px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "3px", height: "1px", borderSide: "2px" }
		 , { margin: "2px", height: "1px", borderSide: "1px" }
		 , { margin: "1px", height: "2px", borderSide: "1px" }
		 ] ,
	SmallTop :
		 [ { margin: "2px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "1px", height: "1px", borderSide: "1px" }
		 ] ,
	//NormalBottom: see onPageLoad()
	//SmallBottom: see onPageLoad()

	/**
	 ** Usage:
	 ** RoundedCorners.register( "#header", ['yyyy', '00f000', '32cd32'] );
	 **/
	registry: {},
	register: function( selector, parameters )
	{
		this.registry[selector] = parameters;
		return this;
	},

	onPageLoad: function()
	{
		/* make reverse copies for bottom definitions */
		this.NormalBottom = this.NormalTop.slice(0).reverse();
		this.SmallBottom  = this.SmallTop.slice(0).reverse();

		for( selector in this.registry )  // CHECK NEEDED
		{
			var n = $$(selector); 
			var parms = this.registry[selector];
			this.exec( n, parms[0], parms[1], parms[2], parms[3] );
		}

		$$('#pagecontent *[class^=roundedCorners]').each(function(el){ 
			var parms = el.className.split('-');
			if( parms.length < 2 ) return;
			this.exec( [el], parms[1], parms[2], parms[3], parms[4] );
		},this);
	},

	exec: function( nodes, corners, color, borderColor, background )
	{
		corners = ( corners ? corners+"nnnn": "yyyy" );
		color   = ( color ? WikiColors.asHTML(color): "transparent" );
		if( borderColor ) borderColor = WikiColors.asHTML( borderColor );
		if( background  ) background = WikiColors.asHTML( background );

		var c = corners.split('');
		/* [0]=top-left; [1]=top-right; [2]=bottom-left; [3]=bottom-right; */

		var nodeTop = null;
		var nodeBottom = null;

		if( c[0]+c[1] != "nn" )  //add top rounded corners
		{
			nodeTop = document.createElement("b") ;
			nodeTop.className = "roundedCorners" ;

			if( (c[0] == "y") || (c[1] == "y") )
			{
				this.addCorner( nodeTop, this.NormalTop, c[0], c[1], color, borderColor );
			}
			else if( (c[0] == "s") || (c[1] == "s") )
			{
				this.addCorner( nodeTop, this.SmallTop, c[0], c[1], color, borderColor );
			}
		}

		if( c[2]+c[3] != "nn" ) //add bottom rounded corners
		{
			nodeBottom = document.createElement("b");
			nodeBottom.className = "roundedCorners";

			if( (c[2] == "y") || (c[3] == "y") )
			{
				this.addCorner( nodeBottom, this.NormalBottom, c[2], c[3], color, borderColor );
			}
			else if( (c[2] == "s") || (c[3] == "s") )
			{
				this.addCorner( nodeBottom, this.SmallBottom, c[2], c[3], color, borderColor );
			}
		}

		if( (!nodeTop) && (!borderColor) && (!nodeBottom) ) return;

		for( var i=0; i<nodes.length; i++)
		{
			if( !nodes[i] ) continue;
			this.addBody( nodes[i], color, borderColor );
			if( nodeTop     )  nodes[i].insertBefore( nodeTop.cloneNode(true), nodes[i].firstChild );
			if( nodeBottom  )  nodes[i].appendChild( nodeBottom.cloneNode(true) );
		}
	},

	addCorner: function( node, arr, left, right, color, borderColor )
	{
		for( var i=0; i< arr.length; i++ )
		{
			var n =  document.createElement("div");
			n.style.height = arr[i].height;
			n.style.overflow = "hidden";
			n.style.borderWidth = "0";
			n.style.backgroundColor = color;

			if( borderColor )
			{
				n.style.borderColor = borderColor;
				n.style.borderStyle = "solid";
				if(arr[i].borderTop)
				{
					n.style.borderTopWidth = arr[i].borderTop;
					n.style.height = "0";
				}
			}

			if( left != 'n' ) n.style.marginLeft = arr[i].margin;
			if( right != 'n' ) n.style.marginRight = arr[i].margin;
			if( borderColor )
			{
				n.style.borderLeftWidth  = ( left  == 'n' ) ? "1px": arr[i].borderSide;
				n.style.borderRightWidth = ( right == 'n' ) ? "1px": arr[i].borderSide;
			}
			node.appendChild( n );
		}
	},

	// move all children of the node inside a DIV and set color and bordercolor
	addBody: function( node, color, borderColor)
	{
		if( node.passed ) return;

		var container = new Element('div').injectWrapper(node);

		container.style.padding = "0 4px";
		container.style.backgroundColor = color;
		if( borderColor )
		{
			container.style.borderLeft  = "1px solid " + borderColor;
			container.style.borderRight = "1px solid " + borderColor;
		}

		node.passed=true;
	}
}


/** 230 Sortable -- Sort tables **/
//TODO cache table ok, cache datatype for each column
var Sortable =
{
	onPageLoad: function(){
		this.DefaultTitle = "sort.click".localize();
		this.AscendingTitle = "sort.ascending".localize();
		this.DescendingTitle = "sort.descending".localize();
		
		$$('.sortable table').each(function(table){
			if( table.rows.length < 2 ) return;

			$A(table.rows[0].cells).each(function(th){
				th=$(th);
				if( th.getTag() != 'th' ) return;
				th.addEvent('click', function(){ Sortable.sort(th); })
					.addClass('sort')
					.title=Sortable.DefaultTitle;
			});
		},this);
	},

	sort: function(th){
		var table = getAncestorByTagName(th, "table" );
		var filter = (table.filterStack),
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
		if(th.hasClass('sort')) rows.sort( Sortable.createCompare( colidx, datatype ) )
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
		var num=date=ip4=euro=true;
		rows.each(function(r,i){
			var v = $getText(r.cells[colidx]).clean().toLowerCase();
			if(num)  num  = !isNaN(parseFloat(v));
			if(date) date = !isNaN(Date.parse(v));
			if(ip4)  ip4  = v.test("(?:\\d{1,3}\\.){3}\\d{1,3}");
			if(euro) euro = v.test("^[£$€][0-9.,]+");
		});
		return (euro) ? 'euro': (ip4) ? 'ip4': (date) ? 'date': (num) ? 'num': 'string';
	},

	convert: function(val, datatype){
		switch(datatype){
			case "num" : return parseFloat( val.match( Number.REparsefloat ) );
			case "euro": return parseFloat( val.replace(/[^0-9.,]/g,'') );
			case "date": return new Date( Date.parse( val ) );
			case "ip4" : 
				var octet = val.split( "." );
				return parseInt(octet[0]) * 1000000000 + parseInt(octet[1]) * 1000000 + parseInt(octet[2]) * 1000 + parseInt(octet[3]);
			default    : return val.toString().toLowerCase();
		}
	},

	createCompare: function(i, datatype) {
		return function(row1, row2) {
			var val1 = Sortable.convert( $getText(row1.cells[i]), datatype );
			var val2 = Sortable.convert( $getText(row2.cells[i]), datatype );

			if     ( val1 < val2 ) return -1
			else if( val1 > val2 ) return 1
			else return 0;
		}
	}
}

/** 240 table-filters 
 ** inspired by http://www.codeproject.com/jscript/filter.asp
 **/
var TableFilter =
{
	onPageLoad: function(){
		this.All = "filter.all".localize();
		this.FilterRow = 1; //row number of filter dropdowns
        
		$$('.table-filter table').each( function(table){
			if( table.rows.length < 2 ) return;

			var r = $(table.insertRow(TableFilter.FilterRow)).addClass('filterrow');
			for(var j=0; j < table.rows[0].cells.length; j++ ){
				var s = new Element('select');
				s.onchange = TableFilter.filter;
				s.fCol     = j; //store index
				new Element('th').adopt(s).injectInside(r);
			}
			table.filterStack = [];
			TableFilter.buildEmptyFilters(table);
		});
	},

	buildEmptyFilters: function(table){
		for(var i=0; i < table.rows[0].cells.length; i++){
			if(!TableFilter.isFiltered(table, i)) TableFilter.buildFilter(table, i);
		}
		if(table.zebra) table.zebra();
			
	},

	isFiltered: function(table, col){
        return table.filterStack.some(function(f){ return f.fCol==col });
	},

	// this function initialises a column dropdown filter
	buildFilter: function(table, col, selectedValue){
		// Get a reference to the dropdownbox.
		var select = table.rows[TableFilter.FilterRow].cells[col].firstChild;
		select.options.length = 0;

        var rows=[];
		$A(table.rows).each(function(r,i){
			if((i==0) || (i==TableFilter.FilterRow)) return;
			if(r.style.display == 'none') return;
			rows.push( r );
		});
		rows.sort(Sortable.createCompare(col, Sortable.guessDataType(rows,col)));

		//add only unique strings to the dropdownbox
		select.options[0]= new Option(TableFilter.All, TableFilter.All);
		var value;
        rows.each(function(r,i){
        	var v = $getText(r.cells[col]).clean().toLowerCase();
        	if(v == value) return;
        	value = v;
        	select.options[select.options.length] = new Option(v, value);
        });
		if(selectedValue != undefined) select.value = selectedValue;
		else select.options[0].selected = true;
	},

	filter: function(){ //onchange handler of filter dropdowns
		var col   = this.fCol,
			value = this.value,
			table = getAncestorByTagName(this, 'table');
		if( !table || table.style.display == 'none') return;

		// First check if the column is allready in the filter.
		if(table.filterStack.every(function(f,i){
			if(f.fCol != col) return true;
			if(value == TableFilter.All) table.filterStack.splice(i, 1);
			else f.fValue = value;
			return false;
		}) ) table.filterStack.push( {fValue:value, fCol:col} );

        $A(table.rows).each(function(r,i){ //show all
        	r.style.display='';
        });

        table.filterStack.each(function(f){ //now filter the right rows
            var v = f.fValue, c = f.fCol;
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


/** 250 Categories: turn wikipage link into AJAXed popup **/
var Categories =
{
	onPageLoad: function (){
		this.jsp = Wiki.TemplateDir + '/AJAXCategories.jsp';

		$$('.category a.wikipage').each(function(link){
			var page = Wiki.href2pagename(link.href );

			var wrap = new Element('span').injectBefore(link).adopt(link);
			var popup = new Element('div').addClass('categoryPopup').injectInside(wrap);
			var popeff = popup.effect('opacity',{wait:false}).set(0);

			link.addClass('categoryLink')
				.setProperties({ href:'#', title: 'Click to show category [' + page + '] ...' })
				.addEvent('click', function(e){
				new Event(e).stop();  //dont jump to top of page ;-)
				new Ajax( Categories.jsp, { 
					postBody: '&page=' + page,
					update: popup,
					onComplete: function(){
						link.setProperty('title', '').removeEvent('click');
						wrap.addEvent('mouseover', function(e) { popeff.start(0.9); })
							.addEvent('mouseout', function(e) { popeff.start(0); });
						popup.setStyle('left', link.getPositionedOffset('left') + 'px');
						popeff.start(0.9); }
				}).request();
			});
		});
	} 
}

/* FIXME Convert url to wiki-pagename */
Wiki.href2pagename = function(href){
	if( href.test(/\?page=([^&]+)/) ) return RegExp.$1;
	return href;
}

/**
 ** 260 Wiki Tips: 
 **/
var WikiTips =
{
	onPageLoad: function() {    
		var tips = [];
		$$('*[class^=tip]').each( function(t){
			var parms = t.className.split('-');
			if( parms.length<=0 || parms[0] != 'tip' ) return;
			t.className="tip";

			var body = new Element('span').injectWrapper(t).hide();
			var caption = (parms[1]) ? parms[1].deCamelize(): "tip.default.title".localize();
			var tt = new Element('span').setHTML(caption).addClass('tip-anchor')
				.injectInside(t);
			tt.title  = caption+'::';
			tt.$.myBody = body;
			tips.push(tt);
		});
		if( tips.length>0 ) new Tips( tips , {'className':'tip'} );
	}
}

/*overwrite Tips start funtion to support DOM bodies*/
Tips.implement({
	start: function(el){		
		this.wrapper.empty();
		if (el.$.myTitle){
			this.title = new Element('span').inject(
				new Element('div', {'class': this.options.className + '-title'}).inject(this.wrapper)
			).setHTML(el.$.myTitle);
		}
		if (el.$.myText){
			this.text = new Element('span').inject(
				new Element('div', {'class': this.options.className + '-text'}).inject(this.wrapper)
			).setHTML(el.$.myText);
		}
		/* BrushedTemplate extension */
		if (el.$.myBody){
			el.$.myBody.clone().injectInside(
				new Element('div').addClass(this.options.className+'-text').injectInside(this.wrapper)
			).show();
		}

		$clear(this.timer);
		this.timer = this.show.delay(this.options.showDelay, this);
	}
});


/**
 ** 270 Wiki Columns
 ** Dirk Frederickx, Mar 07
 **/
var WikiColumns =
{
	onPageLoad: function() {    
		var tips = [];
		$$('*[class^=columns]').each( function(t){
			var parms = t.className.split('-');
			t.className='columns';
			WikiColumns.buildColumns(t, parms[1] || 'auto');
		});
	},

	buildColumns: function( el, width){
		var breaks = $ES('hr',el);
		if(!breaks || breaks.length==0) return;

        var colCount = breaks.length+1;
        width = (width=='auto') ? 99/colCount+'%' : width/colCount+'px';

		var colDef = new Element('div').setStyle('width',width).addClass('col'),
			col = colDef.clone().injectBefore(el.getFirst()),
        	n;
        while(n = col.nextSibling){
        	if(n.tagName && n.tagName.toLowerCase() == 'hr'){
				col = colDef.clone();
				$(n).replaceWith(col);
				continue;
			}
			col.appendChild(n);
        }
        new Element('div').setStyle('clear','both').injectInside(el);
	}
}

/**
 ** 280 ZebraTable
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
 **
 **/
var ZebraTable = {

	onPageLoad: function(){
		$$('*[class^=zebra]').each(function(z){
			var parms = z.className.split('-'), 
				isDefault = parms[1].test('table'),
				c1 = $C(parms[1]||''), 
				c2 = $C(parms[2]||'');
			$ES('table',z).each(function(t){
				t.zebra = this.zebrafy.pass([isDefault, c1,c2],t);
				t.zebra();
			},this);
		},this);
	},
	zebrafy: function(isDefault, c1,c2){
		var j=0;
		$A($T(this).rows).each(function(r,i){
			if(i==0 || !$(r).visible()) return;
			if(isDefault) (j++ % 2 == 0) ? $(r).addClass('odd') : $(r).removeClass('odd');
			else $(r).setStyle('background-color', (j++ % 2 == 0) ? c1 : c2 );
		});
	}
}


/**
 **
 Highlight Word
 **
 ** Inspired by http://www.kryogenix.org/code/browser/searchhi/
 ** Modified 21006 to fix query string parsing and add case insensitivity
 ** Modified 20030227 by sgala@hisitech.com to skip words
 **                   with "-" and cut %2B (+) preceding pages
 ** Refactored for JSPWiki -- now based on regexp, by D.Frederickx. Nov 2005
 **
 **/
var HighlightWord =
{
	ClassName     : "searchword" ,
	ClassNameMatch: "<span class='searchword' >$1</span>" ,
	ReQuery: new RegExp( "(?:\\?|&)(?:q|query)=([^&]*)", "g" ) ,

	onPageLoad: function ()
	{
		//FIXME: AJAX find doesnt use the &query= parm - so need another way to check the referrer
		if( !this.ReQuery.test( document.referrer ) ) return;

		var words = decodeURIComponent(RegExp.$1);
		words = words.replace( /\+/g, " " );
		words = words.replace( /\s+-\S+/g, "" );
		words = words.replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ); //escape metachars
		words = words.trim().split(/\s+/).join("|");
		this.reMatch = new RegExp( "(" + words + ")" , "gi");

		this.walkDomTree( $("pagecontent") );
	},

	// recursive tree walk matching all text nodes
	walkDomTree: function( node )
	{
		if( !node ) return; /* bugfix */
		var nn = null;
		for( var n = node.firstChild; n ; n = nn )
		{
			nn = n. nextSibling; /* prefetch nextSibling cause the tree will be modified */
			this.walkDomTree( n );
		}

		// continue on text-nodes, not yet highlighted, with a word match
		if( node.nodeType != 3 ) return;
		if( node.parentNode.className == this.ClassName ) return;
		var s = node.innerText || node.textContent || '';
		if( !this.reMatch.test( s ) ) return;
		var tmp = new Element('span').setHTML(s.replace(this.reMatch,this.ClassNameMatch));

		var f = document.createDocumentFragment();
		while( tmp.firstChild ) f.appendChild( tmp.firstChild );

		node.parentNode.replaceChild( f, node );
	}
}





//window.addEvent('domready', function(){
window.addEvent('load', function(){

	Wiki.onPageLoad();
	//WikiReflection.onPageLoad(); //before accordion cause impacts height!
	WikiAccordion.onPageLoad();
	TabbedSection.onPageLoad(); //after coordion or safari
	QuickLinks.onPageLoad();
	Collapsable.onPageLoad();
	SearchBox.onPageLoad();
	Sortable.onPageLoad();
	TableFilter.onPageLoad();
	RoundedCorners.onPageLoad();
	ZebraTable.onPageLoad();
	HighlightWord.onPageLoad();
	GraphBar.onPageLoad();
	Categories.onPageLoad();
	URL.onPageLoad();
	//EditTools.onPageLoad();

	WikiSlimbox.onPageLoad();
	WikiTips.onPageLoad();
	WikiColumns.onPageLoad();
});