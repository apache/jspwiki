/*
Plugin: WikiSlimbox
	Slimbox clone, refactored for JSPWiki.

	Modified for inclusion in JSPWiki
	- DONE minimum size of image canvas
	- DONE add maximum size of image w.r.t window size TODO
	- DONE spacebor, down arrow, enter : next image
	- DONE up arrow : prev image done
	- DONE add support for external page links  => slimbox_ex
	- DONE add support for youtube, ...
	- add support for native <video> playback in the browser (not ie)

Credits:
	Inspired by Slimbox by Christophe Bleys.
	(see http://www.digitalia.be/software/slimbox)
	and the mediaboxAdvanced by John Einselen
	(see http://iaian7.com/webcode/mediaboxAdvanced)

DOM structure:
	DOM structure of the JSPWiki Slimbox viewer.
	(start code)
	<div id="lbOverlay"></div>
	<div id="lbCenter" class="or spin">
		<div id="lbImage">
			<!-- img or iframe element is inserted here -->
		</div>
	</div>
	<div id="lbBottomContainer">
		<div id="lbBottom">
			<a id="lbCloseLink"/>
			<a id="lbNextLink"/>
			<a id="lbPrevLink"/>
			<div id="lbCaption">
			<div id="lbNumber">
		</div>
	</div>
	(end)
*/

var Slimbox = (function() {

	// Global variables, accessible to Slimbox only
	var win = window, ie6 = Browser.Engine.trident4, options, images, activeImage = -1, activeURL, prevImage, nextImage, compatibleOverlay, middle, centerWidth, centerHeight,

	// Preload images
	preload = {}, preloadPrev = new Image(), preloadNext = new Image(),

	// DOM elements
	overlay, center, image,/* sizer,*/ closeLink, prevLink, nextLink, bottomContainer, bottom, caption, number,

	// MEDIA parameters
	media, isimage,

	// Effects
	fxOverlay, fxResize, fxImage, fxBottom;

	/*
		Initialization
	*/

	win.addEvent("domready", function() {

		// Append the Slimbox HTML code at the bottom of the document
		$(document.body).adopt(
			$$(
				overlay = new Element("div", {id: "lbOverlay", events: {click: close}}),
				center = new Element("div", {id: "lbCenter"}),
				bottomContainer = new Element("div", {id: "lbBottomContainer"})
			).setStyle("display", "none")
		);

		//image = new Element("div", {id: "lbImage"}).injectInside(center).adopt(
		image = new Element("div", {id: "lbImage"}).inject(center)/*.adopt(
			sizer = new Element("div", {styles: {position: "relative"}}).adopt(
				prevLink = new Element("a", {id: "lbPrevLink", href: "#", events: {click: previous}}),
				nextLink = new Element("a", {id: "lbNextLink", href: "#", events: {click: next}})
			)
		)*/;

		//bottom = new Element("div", {id: "lbBottom"}).injectInside(bottomContainer).adopt(
		bottom = new Element("div", {id: "lbBottom"}).inject(bottomContainer).adopt(
			caption = new Element("div", {id: "lbCaption"}),
			number = new Element("div", {id: "lbNumber"}),
			closeLink = new Element("a", {id: "lbCloseLink", events: {click: close}}),
			nextLink = new Element("a", {id: "lbNextLink", events: {click: next}}),
			prevLink = new Element("a", {id: "lbPrevLink", events: {click: previous}})
		);
	});


	/*
		Internal functions
	*/

	function position() {
		var scroll = win.getScroll(), size = win.getSize();
		$$(center, bottomContainer).setStyle("left", scroll.x + (size.x / 2));
		if (compatibleOverlay) overlay.setStyles({left: scroll.x, top: scroll.y, width: size.x, height: size.y});
	}

	function setup(open) {
		["object", ie6 ? "select" : "embed"].forEach(function(tag) {
			Array.forEach(document.getElementsByTagName(tag), function(el) {
				if (open) el._slimbox = el.style.visibility;
				el.style.visibility = open ? "hidden" : el._slimbox;
			});
		});

		overlay.style.display = open ? "" : "none";

		var fn = open ? "addEvent" : "removeEvent";
		win[fn]("scroll", position)[fn]("resize", position);
		document[fn]("keydown", keyDown);
	}

	function keyDown(event) {
		var code = event.code;
		// Prevent default keyboard action (like navigating inside the page)
		return options.closeKeys.contains(code) ? close()
			: options.nextKeys.contains(code) ? next()
			: options.previousKeys.contains(code) ? previous()
			: false;
	}

	function previous() {
		return changeImage(prevImage);
	}

	function next() {
		return changeImage(nextImage);
	}

	function changeImage(imageIndex) {

		if (imageIndex >= 0) {

			activeImage = imageIndex;
			activeURL = images[imageIndex][0];
			//activeURL = encodeURIComponent(images[imageIndex][0]);

			prevImage = (activeImage || (options.loop ? images.length : 0)) - 1;
			nextImage = ((activeImage + 1) % images.length) || (options.loop ? 0 : -1);

			stop();
			center.className = "spin";
			image.empty();

			//preload = Slimbox.loadImage( activeURL, animateBox, options );

			if( isimage = Slimbox.isImage(activeURL) ){

				preload = new Image()
				preload.onload = animateBox;
				preload.src = activeURL;

			} else if( preload = Slimbox.isMedia(activeURL) ){

				//preload now contains item from the .Media repository
				//extend it with default swiff parameters
				preload = $extend({
					width: options.initialWidth,
					height: options.initialHeight,
					params: {
						wmode: 'opaque',
						bgcolor: '#000000',
						allowfullscreen: 'true',
						allowscriptaccess: 'true'
					}
				}, preload[1](activeURL) );

				new Swiff(preload.url, preload).inject(image);
				animateBox();

			} else {

				preload = new IFrame({
					src:activeURL,
					width:800,
					height:600,
					//frameborder:0,
					events:{
						load:animateBox
					}
				}).inject(image);

			}

		}
		return false;
	}

	function animateBox() {

		center.className = "";

		fxImage.set(0);

		var size = win.getSize();
		//image.setStyles({backgroundImage: "url(" + activeURL + ")", display: ""});
		//if( !isimage ) image.adopt(preload);
		image.setStyles({
			backgroundImage: (isimage) ? 'url(' + activeURL + ')' : 'none',
			display:'',
			width:preload.width.toInt().limit(options.initialWidth, 0.8*size.x),
			height:preload.height.toInt().limit(options.initialHeight, 0.9*size.y)
		});
		//sizer.setStyle("width", preload.width);
		//$$(sizer, prevLink, nextLink).setStyle("height", preload.height);

		caption.set("html", images[activeImage][1] || "");
		number.set("html", (((images.length > 1) && options.counterText) || "").replace(/{x}/, activeImage + 1).replace(/{y}/, images.length));

		//if (prevImage >= 0) preloadPrev.src = images[prevImage][0];
		//if (nextImage >= 0) preloadNext.src = images[nextImage][0];
		if (prevImage >= 0 && Slimbox.isImage(images[prevImage][0])) preloadPrev.src = images[prevImage][0];
		if (nextImage >= 0 && Slimbox.isImage(images[nextImage][0])) preloadNext.src = images[nextImage][0];

		centerWidth = image.offsetWidth;
		centerHeight = image.offsetHeight;
		var top = Math.max(0, middle - (centerHeight / 2)), check = 0, fn;
		if (center.offsetHeight != centerHeight) {
			check = fxResize.start({height: centerHeight, top: top});
		}
		if (center.offsetWidth != centerWidth) {
			check = fxResize.start({width: centerWidth, marginLeft: -centerWidth/2});
		}
		fn = function() {
			//bottomContainer.setStyles({width: centerWidth, top: top + centerHeight, marginLeft: -centerWidth/2, /*visibility: "hidden",*/ display: ""});
			bottomContainer.setStyles({width: centerWidth, top: top + centerHeight, marginLeft: -centerWidth/2, display: ""});
			fxImage.start(1);
		};
		if (check) {
			fxResize.chain(fn);
		}
		else {
			fn();
		}
	}

	function animateCaption() {
		//if (prevImage >= 0) prevLink.style.display = "";
		//if (nextImage >= 0) nextLink.style.display = "";
		if(prevImage >= 0) prevLink.set({title:images[prevImage][1] || "", styles:{display:""}});
		if(nextImage >= 0) nextLink.set({title:images[nextImage][1] || "", styles:{display:""}});

		//fxBottom.set(-bottom.offsetHeight).start(0);
		fxBottom.start(1);
		//move to animateBox
		//bottomContainer.style.visibility = "";
	}

	function stop() {
		preload.onload = $empty;
		//not needed: image.empty() will stop loading, and avoid "refiring" of animateBox()
		//preload.src = preloadPrev.src = preloadNext.src = activeURL;
		fxResize.cancel();
		fxImage.cancel();
		//fxBottom.cancel();
		fxBottom.cancel().set(0);
		$$(prevLink, nextLink, image, bottomContainer).setStyle("display", "none");
	}

	function close() {
		if (activeImage >= 0) {
			stop();
			activeImage = prevImage = nextImage = -1;
			center.style.display = "none";
			fxOverlay.cancel().chain(setup).start(0);
		}

		return false;
	}


	/*
		API
	*/

	/* not used
	Element.implement({
		slimbox: function(_options, linkMapper) {
			// The processing of a single element is similar to the processing of a collection with a single element
			$$(this).slimbox(_options, linkMapper);

			return this;
		}
	});
	*/

		/*
			options:	Optional options object, see Slimbox.open()
			linkMapper:	Optional function taking a link DOM element and an index as arguments and returning an array containing 2 elements:
					the image URL and the image caption (may contain HTML)
			linksFilter:	Optional function taking a link DOM element and an index as arguments and returning true if the element is part of
					the image collection that will be shown on click, false if not. "this" refers to the element that was clicked.
					This function must always return true when the DOM element argument is "this".
		*/
	/* not used
	Elements.implement({
		slimbox: function(_options, linkMapper, linksFilter) {
			linkMapper = linkMapper || function(el) {
				return [el.href, el.title];
			};

			linksFilter = linksFilter || function() {
				return true;
			};

			var links = this;

			links.removeEvents("click").addEvent("click", function() {
				// Build the list of images that will be displayed
				var filteredLinks = links.filter(linksFilter, this);
				return Slimbox.open(filteredLinks.map(linkMapper), filteredLinks.indexOf(this), _options);
			});

			return links;
		}
	});
	*/
	return {
		open: function(_images, startImage, _options) {
			options = $extend({
				loop: true, // Allows to navigate between first and last images
				overlayOpacity: 0.4, // 1 is opaque, 0 is completely transparent (change the color in the CSS file)
				overlayFadeDuration: 300, // Duration of the overlay fade-in and fade-out animations (in milliseconds)
				resizeDuration: 240, // Duration of each of the box resize animations (in milliseconds)
				resizeTransition: false, // false uses the mootools default transition
				initialWidth: 400, // Initial width of the box (in pixels)
				initialHeight: 200, // Initial height of the box (in pixels)
				imageFadeDuration: 300, // Duration of the image fade-in animation (in milliseconds)
				captionAnimationDuration: 300, // Duration of the caption animation (in milliseconds)
				closeText: "Close",
				nextText: "Next",
				prevText: "Previous",
				counterText: "({x} of {y})", // Translate or change as you wish, or set it to false to disable counter text for image groups
				closeKeys: [27, 88, 67], // Array of keycodes to close Slimbox, default: Esc (27), 'x' (88), 'c' (67)
				previousKeys: [37, 38, 80], // Array of keycodes to navigate to the previous image, default: Left arrow (37), Up arrow(38), 'p' (80)
				nextKeys: [13, 32, 39, 40, 78] // Array of keycodes to navigate to the next image, default: Enter(13), Space(32), Right arrow (39), Down arrow(40), 'n' (78)
			}, _options || {});

			// Setup bottom labels
			prevLink.set('html',options.prevText);
			nextLink.set('html',options.nextText);
			closeLink.set('html',options.closeText);

			// Setup effects
			fxOverlay = new Fx.Tween(overlay, {property: "opacity", duration: options.overlayFadeDuration});
			fxResize = new Fx.Morph(center, $extend({duration: options.resizeDuration, link: "chain"}, options.resizeTransition ? {transition: options.resizeTransition} : {}));
			fxImage = new Fx.Tween(image, {property: "opacity", duration: options.imageFadeDuration, onComplete: animateCaption});
			//fxBottom = new Fx.Tween(bottom, {property: "margin-top", duration: options.captionAnimationDuration});
			fxBottom = new Fx.Tween(bottom, {property: "opacity", duration: options.captionAnimationDuration}).set(0)

			// The function is called for a single image, with URL and Title as first two arguments
			if (typeof _images == "string") {
				_images = [[_images, startImage]];
				startImage = 0;
			}

			middle = win.getScrollTop() + (win.getHeight() / 2);
			centerWidth = options.initialWidth;
			centerHeight = options.initialHeight;
			center.setStyles({top: Math.max(0, middle - (centerHeight / 2)), width: centerWidth, height: centerHeight, marginLeft: -centerWidth/2, display: ""});
			compatibleOverlay = ie6 || (overlay.currentStyle && (overlay.currentStyle.position != "fixed"));
			//solved in css _position
			//if (compatibleOverlay) overlay.style.position = "absolute";
			fxOverlay.set(0).start(options.overlayOpacity);
			position();
			setup(1);

			images = _images;
			options.loop = options.loop && (images.length > 1);
			return changeImage(startImage);
		},

		loadImage: function( url, callbackFn, options ){

			var preload;

			if( isimage = this.isImage( url ) ){

				preload = new Image()
				preload.onload = callbackFn;
				preload.src = url;

			} else if( preload = this.isMedia( url ) ){

				//preload now contains item from the .Media repository
				//extend it with default swiff parameters
				preload = $extend({
					width: options.initialWidth,
					height: options.initialHeight,
					params: {
						wmode: 'opaque',
						bgcolor: '#000000',
						allowfullscreen: 'true',
						allowscriptaccess: 'true'
					}
				}, preload[1]( url ) );

				new Swiff(preload.url, preload);
				callbackFn();

			} else {

				preload = new IFrame({
					src:activeURL,
					width:800,
					height:600,
					//frameborder:0,
					events:{
						load:callbackFn
					}
				});

			}

			return preload;
		},

		Image: "(\.bmp|\.gif|\.png|\.jpg|\.jpeg)$",
		isImage: function( url ){
			return url.test(this.Image,'i')
		},

		Media: [],
		isMedia: function(url){

			for( var media = this.Media, i=0, l=media.length; i<l; i++){
				if( url.test( media[i][0].escapeRegExp(),'i') ) return media[i];
			}
			return null;
		}

	};

})();


Slimbox.Media.extend([

	//fixme
	['.swf', function(url){
		return { url:url }
	}],
	['facebook.com', function(url){
  		url = 'http://www.facebook.com/v/' + url.split('v=')[1].split('&')[0];
  		return {
  			url:url,
			movie:url,
			width:320,
			height:240,
			classid: 'clsid:D27CDB6E-AE6D-11cf-96B8-444553540000'
		}
	}],
	['flickr.com', function(url){
		url = url.split('/')[5];
		return {
			url: 'http://www.flickr.com/apps/video/stewart.swf',
			classid: 'clsid:D27CDB6E-AE6D-11cf-96B8-444553540000',
			width:500,
			height:375,
			params: {flashvars: 'photo_id='+url+'&amp;show_info_box=true' }
		}
	}],
	['google.com/videoplay', function(url){
		url = url.split('=')[1];
		return {
			url:'http://video.google.com/googleplayer.swf?docId='+url/*+'&autoplay=0'*/,
			width:400,
			height:326
		}
	}],
	['youtube.com/watch', function(url){
		url = url.split('v=')[1];
		var parms =
			(url.test(/fmt=18/i)) ? ['&ap=%2526fmt%3D18',560,345] :
			(url.test(/fmt=22/i)) ? ['&ap=%2526fmt%3D22',640,385] :
			/*else*/ ['&ap=%2526fmt%3D18',425,344];

		return {
			url:'http://www.youtube.com/v/'+url
			+'&autoplay=0&fs=1'+parms[0]+'&border=0&rel=0&showinfo=1&showsearch=0&feature=player_embedded',
			width:parms[1],
			height:parms[2]
		}
	}],
	['youtube.com/view', function(url){
		url = url.split('p=')[1];
		return{
			url:'http://www.youtube.com/p/'+url
				+'&autoplay='+options.autoplayNum+'&fs='+options.fullscreenNum+mediaFmt
				+'&border='+options.ytBorder+'&color1=0x'+options.ytColor1+'&color2=0x'+options.ytColor2
				+'&rel='+options.ytRel+'&showinfo='+options.ytInfo+'&showsearch='+options.ytSearch,
			width:480,
			height:385
		};
	}],
	['metacafe.com/watch', function(url){
		url = url.split('/')[4];
		return {
			url: 'http://www.metacafe.com/fplayer/'+url+'/.swf?playerVars=autoPlay=1',
			width: 400,
			height: 345
		}
	}],
	["megavideo.com", function(url){
		url = url.split('=')[1];
		return {
			url:'http://wwwstatic.megavideo.com/mv_player.swf?v='+url,
			width:640,
			height:360
		}
	}],
	['vimeo.com', function(url){
		url = url.split('/')[3];
		return{
			url:'http://www.vimeo.com/moogaloop.swf?clip_id='+url
			 	+'&amp;server=www.vimeo.com&amp;fullscreen=1&amp;autoplay=0'
			 	+'&amp;show_title=1&amp;show_byline=1&amp;show_portrait=1&amp;color=ffffff',
			width:640,
			height:360
		};
	}]

]);

