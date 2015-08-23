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
 * jspwiki-commonstyles.js
 * Contains additional Dynmic Styles
 *
 *	114 Reflection (adds reflection to images): dynamic style
 *	132 Accordion object: dynamic style
 *	220 RoundedCorners: dynamic style
 *	260 WikiTips: dynamic style
 *	270 WikiColumns: dynamic style
 *	300 Prettify: dynamic style
 */


/*
Dynamic Style: Reflection  (114)

Inspired by Reflection.js at http://cow.neondragon.net/stuff/reflection/
Freely distributable under MIT-style license.
Adapted for JSPWiki/BrushedTemplate, Sep 06

Use:
 	%%reflection-height-opacity  [some-image.jpg] %%
 */
var WikiReflection = {

	render: function(page,name){
		$ES('*[class^=reflection]',page).each( function(w){
			var parms = w.className.split('-');
			$ES('img',w).each(function(img){
				Reflection.add(img, parms[1], parms[2]);
			});
		});
	}
}
Wiki.addPageRender(WikiReflection);

/* FIXME : add delayed loading of reflection library */
var Reflection = {

	options: { height: 0.33, opacity: 0.5 },

	add: function(img, height, opacity) {
		//TODO Reflection.remove(image); --is this still needed?
		height  = (height ) ? height/100 : this.options.height;
		opacity = (opacity) ? opacity/100: this.options.opacity;

		var div = new Element('div').injectAfter(img).adopt(img),
			imgW = img.width,
			imgH = img.height,
			rH   = Math.floor(imgH * height); //reflection height

		div.className = img.className.replace(/\breflection\b/, "");
		div.style.cssText = img.backupStyle = img.style.cssText;
		//div.setStyles({'width':img.width, 'height':imgH +rH, "maxWidth": imgW });
		div.setStyles({'width':img.width, 'height':imgH +rH });
		img.style.cssText = 'vertical-align: bottom';
		//img.className = 'inline reflected';  //FIXME: is this still needed ??

		if( window.ie ){
			new Element('img', {'src': img.src, 'styles': {
				'width': imgW,
				'marginBottom': "-" + (imgH - rH) + 'px',
				'filter': 'flipv progid:DXImageTransform.Microsoft.Alpha(opacity='+(opacity*100)+', style=1, finishOpacity=0, startx=0, starty=0, finishx=0, finishy='+(height*100)+')'
			}}).inject(div);
		} else {
			var r = new Element('canvas', {'width':imgW, 'height':rH, 'styles': {'width':imgW, 'height': rH}}).inject(div);
			if( !r.getContext ) return;

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

	render: function(page,name){

		var toggle = new Element('div',{'class':'toggle'}),
			bullet = new Element('div',{'class':'collapseBullet'});

		$ES('.accordion, .tabbedAccordion, .leftAccordion, .rightAccordion',page).each( function(tt){

			var toggles=[], contents=[], menu=false;
			if(tt.hasClass('tabbedAccordion')){
				menu = new Element('div',{'class':'menu top'}).injectBefore(tt);
			}
			else if(tt.hasClass('leftAccordion')){
				menu = new Element('div',{'class':'menu left'}).injectBefore(tt);
			}
			else if(tt.hasClass('rightAccordion')){
				menu = new Element('div',{'class':'menu right'}).injectBefore(tt);
			}

			tt.getChildren().each(function(tab) {
				if( !tab.className.test('^tab-') ) return;

				//FIXME use class to make tabs visible during printing
				//(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

				var title = tab.className.substr(4).deCamelize(),
					t = toggle.clone().appendText(title);
				menu ? t.inject(menu) : bullet.clone().injectTop(t.injectBefore(tab));

				toggles.push(t);
				contents.push(tab.addClass('tab'));
			});

			new Accordion(toggles, contents, {
				height: true,
				alwaysHide: !menu,
				onComplete: function(){
					var el = $(this.elements[this.previous]);
					if (el.offsetHeight > 0) el.setStyle('height', 'auto');
				},
				onActive: function(toggle,content){
					toggle.addClass('active');
					var b = toggle.getFirst();/*bullet*/
					if(b) b.setProperties({'title':'collapse'.localize(), 'class':'collapseOpen'}).setHTML('-'); /* &raquo; */
					content.addClass('active');//.removeClass('xhidetab');
				},
				onBackground: function(toggle,content){
					content.setStyle('height', content['offsetHeight']);
					toggle.removeClass('active');
					var b = toggle.getFirst();/*bullet*/
					if(b) b.setProperties({'title':'expand'.localize(), 'class':'collapseClose'}).setHTML('+'); /* &laquo; */
					content.removeClass('active');//.addClass('xhidetab');
				}
			});
		});
		bullet=toggle=null; //avoid memory leaks
	}
}
Wiki.addPageRender(WikiAccordion);



/**
 ** 260 Wiki Tips:
 **/
var WikiTips =
{
	render: function(page,name) {
		var tips = [];
		$ES('*[class^=tip]',page).each( function(t){
			var parms = t.className.split('-');
			if( parms.length<=0 || parms[0] != 'tip' ) return;
			t.className = "tip";

			var body = new Element('span').wrapChildren(t).hide(),
				caption = (parms[1]) ? parms[1].deCamelize(): "tip.default.title".localize();

			tips.push(
				new Element('span',{
					'class': 'tip-anchor',
					'title': caption + '::' + body.innerHTML
				}).setHTML(caption).inject(t)
			);
		});
		if( tips.length>0 ) new Tips( tips , {'className':'tip', 'Xfixed':true} );
	}
}
Wiki.addPageRender(WikiTips);


/**
 ** 270 Wiki Columns
 ** Mar 07
 **/
var WikiColumns =
{
	render: function(page,name) {
		var tips = [];
		$ES('*[class^=columns]',page).each( function(t){
			var parms = t.className.split('-');
			t.className='columns';
			WikiColumns.buildColumns(t, parms[1] || 'auto');
		});
	},

	buildColumns: function( el, width){
		var breaks = $ES('hr',el);
		if(!breaks || breaks.length==0) return;

		var colCount = breaks.length+1;
		width = (width=='auto') ? 98/colCount+'%' : width/colCount+'px';

		var colDef = new Element('div',{'class':'col','styles':{'width':width}}),
			col = colDef.clone().injectTop(el),
			n;
		while(n = col.nextSibling){
			if(n.tagName && n.tagName.toLowerCase() == 'hr'){
				col = colDef.clone();
				$(n).replaceWith(col);
				continue;
			}
			col.appendChild(n);
		}
		new Element('div',{'styles':{'clear':'both'}}).inject(el);
	}
}
Wiki.addPageRender(WikiColumns);


/* 300 Javascript Code Prettifier
 * based on http://google-code-prettify.googlecode.com/svn/trunk/README.html
 */
var WikiPrettify = {
	render: function(page,name){
		var els = $ES('.prettify pre, .prettify code',page);
		if(!els || els.length==0) return;
		els.addClass('prettyprint');

		//TODO: load assets .css and .js
		//PRETTIFY: patch added to avoid processing of the same element
		prettyPrint(page);
	}
}
Wiki.addPageRender(WikiPrettify);
