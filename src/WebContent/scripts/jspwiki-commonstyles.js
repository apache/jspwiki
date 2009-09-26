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
Script: jspwiki-commonstyles.js
	Contains additional Dynmic Styles

	* [Reflection]: add reflection to images
	* [Accordion]: horizontal and vertical accordions
	* [RoundedCorners]: ==> move to jspwiki-extra.js
	* [Tips]: mouse-hover Tips
	* [Columns]: side-by-side columns
	* [Prettify]: syntax highlighting

License:
	http://www.apache.org/licenses/LICENSE-2.0

Depends:
	Depends on mootools-more, v1.2.3.1.
	Includes Fx.Accordion, Drag, Tips, Hash.Cookie

*/

/*
Script: Reflection
	Add reflection effect to images.
	Uses the ''reflection.js'' by Christophe Bleys

Arguments:

Options:
	height - (optional) 1..100. Height value of the reflection image,
	  in percent of the height of the reflected image (default = 30%)
	opacity - (optional) 1..100. Opacity or transparency value of the
	  reflection image (default = 50%, 100 means not transparent)

Example:
> 	%%reflection  [some-image.jpg] /%
> 	%%reflection-30-50  [some-image.jpg] /%

*/
Wiki.registerPlugin( function(page, name){

	page.getElements('div[class^=reflection]').each( function(r){
		var parms = r.className.split('-');
		r.getElements('img').reflect({ height:parms[1]/100, width:parms[2]/100 });
	});

});

/*
Element: reflect
	Extend the base Element class with a reflect and unreflect methods.

Credits:
>	reflection.js for mootools v1.42
>	(c) 2006-2008 Christophe Beyls, http://www.digitalia.be
>	MIT-style license.
*/
Element.implement({
	reflect: function(options) {

		var img = this,
			oHeight = options.height || 0.33,
			oOpacity = options.opacity || 0.5;

		if (img.get("tag") == "img") {

			img.unreflect();

			function doReflect() {
				var reflection, reflectionHeight = Math.floor(img.height * oHeight), wrapper, context, gradient;

				if (Browser.Engine.trident) {
					reflection = new Element("img", {src: img.src, styles: {
						width: img.width,
						height: img.height,
						marginBottom: -img.height + reflectionHeight,
						filter: "flipv progid:DXImageTransform.Microsoft.Alpha(opacity=" + (oOpacity * 100) + ",style=1,finishOpacity=0,startx=0,starty=0,finishx=0,finishy=" + (oHeight * 100) + ")"
					}});
				} else {
					reflection = new Element("canvas");
					if (!reflection.getContext) return;
					try {
						context = reflection.setProperties({width: img.width, height: reflectionHeight}).getContext("2d");
						context.save();
						context.translate(0, img.height-1);
						context.scale(1, -1);
						context.drawImage(img, 0, 0, img.width, img.height);
						context.restore();
						context.globalCompositeOperation = "destination-out";

						gradient = context.createLinearGradient(0, 0, 0, reflectionHeight);
						gradient.addColorStop(0, "rgba(255,255,255," + (1 - oOpacity) + ")");
						gradient.addColorStop(1, "rgba(255,255,255,1.0)");
						context.fillStyle = gradient;
						context.rect(0, 0, img.width, reflectionHeight);
						context.fill();
					} catch (e) {
						return;
					}
				}
				reflection.setStyles({display: "block", border: 0});

				wrapper = new Element(($(img.parentNode).get("tag") == "a") ? "span" : "div").injectAfter(img).adopt(img, reflection);
				wrapper.className = img.className;
				img.store("reflected", wrapper.style.cssText = img.style.cssText);
				wrapper.setStyles({width: img.width, height: img.height + reflectionHeight, overflow: "hidden"});
				img.style.cssText = "display:block;border:0";
				img.className = "reflected";
			}

			if (img.complete) doReflect();
			else img.onload = doReflect;
		}

		return img;
	},

	unreflect: function() {
		var img = this, reflected = this.retrieve("reflected"), wrapper;
		img.onload = $empty;

		if (reflected !== null) {
			wrapper = img.parentNode;
			img.className = wrapper.className;
			img.style.cssText = reflected;
			img.store("reflected", null);
			wrapper.parentNode.replaceChild(img, wrapper);
		}

		return img;
	}
});


/*
Class: Accordion
	Add accordion effect for Tabs, Accordeons, CollapseBoxes


Following markup
{{{
	<div class="accordion">
		<div class="tab-FirstTab">...<div>
		<div class="tab-SecondTab">...<div>
	</div>
}}}
is changed into
{{{
	<div class="accordion">
		<div class="toggle active">First Tab</div>
		<div class="tab-FirstTab tab active">...</div>
		<div class="toggle">Second Tab</div>
		<div class="tab-SecondTab">...</div>
	</div>
}}}
*/
var WikiAccordion = {

	render: function(page,name){

		var toggle = new Element('div',{'class':'toggle'}),
			bullet = new Element('div',{'class':'collapseBullet'});

		page.getElements('div.accordion, div.tabbedAccordion, div.leftAccordion, div.rightAccordion').each( function(tt){

			var toggles=[], contents=[], accordion=null, menu=false;
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
				var i = toggles.length-1;
				contents.push(tab
					.addClass('tab')
					.addEvent('onShow', function(){	accordion.display(i); })
				);
			});

			accordion = new Accordion(toggles, contents, {
				height: true,
				alwaysHide: !menu,
				onComplete: function(){
					var el = $(this.elements[this.previous]);
					if (el.offsetHeight > 0) el.setStyle('height', 'auto');
				},
				onActive: function(toggle,content){
					toggle.addClass('active');
					var b = toggle.getFirst();/*bullet*/
					if(b) b.setProperties({'title':'collapse'.localize(), 'class':'collapseOpen'}).set('html','-'); /* &raquo; */
					content.addClass('active');//.removeClass('xhidetab');
				},
				onBackground: function(toggle,content){
					content.setStyle('height', content['offsetHeight']);
					toggle.removeClass('active');
					var b = toggle.getFirst();/*bullet*/
					if(b) b.setProperties({'title':'expand'.localize(), 'class':'collapseClose'}).set('html','+'); /* &laquo; */
					content.removeClass('active');//.addClass('xhidetab');
				}
			});
		});
		bullet=toggle=null; //avoid memory leaks
	}
}
Wiki.registerPlugin( WikiAccordion );


/*
Class: RoundedCorners

--experimental
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

*/
var RoundedCorners =
{
	/** Definition of CORNER dimensions
	 ** Normal    Normal+Border  Small  Small+Border   Big
	 ** .....+++  .....BBB       ..+++  ..BBB          ........+++
	 ** ...+++++  ...BB+++       .++++  .B+++          .....++++++
	 ** ..++++++  ..B+++++       +++++  B++++          ...++++++++
	 ** .+++++++  .B++++++                             ..+++++++++
	 ** .+++++++  .B++++++                             .++++++++++
	 ** ++++++++  B+++++++                             .++++++++++
	 **                                                .++++++++++
	 **                                                +++++++++++
	 **
	 ** legend: . background, B border, + forground color
	 **/
	$Top: {
		'y' : /* normal */
		 [ { margin: "5px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "3px", height: "1px", borderSide: "2px" }
		 , { margin: "2px", height: "1px", borderSide: "1px" }
		 , { margin: "1px", height: "2px", borderSide: "1px" }
		 ] ,
		's' : /* small */
		 [ { margin: "2px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "1px", height: "1px", borderSide: "1px" }
		 ] ,
		'b' : /* big */
		 [ { margin: "8px", height: "1px", borderSide: "0", borderTop: "1px" }
		 , { margin: "6px", height: "1px", borderSide: "2px" }
		 , { margin: "4px", height: "1px", borderSide: "1px" }
		 , { margin: "3px", height: "1px", borderSide: "1px" }
		 , { margin: "2px", height: "1px", borderSide: "1px" }
		 , { margin: "1px", height: "3px", borderSide: "1px" }
		 ]
	},

	/**
	 ** Usage:
	 ** RoundedCorners.register( "#header", ['yyyy', '00f000', '32cd32'] );
	 **/
	$registry: {},
	register: function(selector, parms){
		this.$registry[selector] = parms;
		return this;
	},

	render: function(page,name){
		/* make reverse copies for bottom definitions */

		this.$Bottom = {};
		for(var i in this.$Top){
			this.$Bottom[i] = this.$Top[i].slice(0).reverse();
		}

		for(var selector in this.$registry){  // CHECK NEEDED
			var n = $$(selector),
				p = this.$registry[selector];
			this.exec(n, p[0], p[1], p[2], p[3]);
		}

		page.getElements('div[class^=roundedCorners]').each(function(el){
			var p = el.className.split('-');
			if(p.length >= 2) this.exec([el], p[1], p[2], p[3], p[4] );
		},this);
	},

	exec: function(nodes, corners, color, borderColor, background){
		corners = (corners || "yyyy") + 'nnnn';
		color = new Color(color) || 'transparent';
		borderColor = new Color(borderColor);
		background  = new Color(background);

		var c = corners.split('');
		/* c[0]=top-left; c[1]=top-right; c[2]=bottom-left; c[3]=bottom-right; */

		nodes.each(function(n){
			if( n.$passed ) return;

			var top = this.addCorner(this.$Top, c[0], c[1], color, borderColor, n),
				bottom = this.addCorner(this.$Bottom, c[2], c[3], color, borderColor, n);

			if(top || bottom) {
				this.addBody(n, color, borderColor);

				if(top){
					var p = n.getStyle('padding-top').toInt();
					top.setStyle('margin-top', p-top.getChildren().length);
					n.setStyle('padding-top',0);
					top.injectTop(n);
				}

				if(bottom){
					var p = n.getStyle('padding-bottom').toInt();
					bottom.setStyle('margin-bottom', p-bottom.getChildren().length);
					n.setStyle('padding-bottom',0);
					n.adopt(bottom);
				}
			}
			if(borderColor) n.setStyle('border','none');
			n.$passed=true;
		},this);
		top=bottom=null;
	},

	getTemplate: function(template, corners){
		var t = false;
		if(corners != 'nn') for(var item in template){
			if(corners.contains(item)){
				t = template[item];
				break;
			}
		}
		return t;
	},

	addCorner: function(corner, left, right, color, border, n){

		corner = this.getTemplate(corner, left+right);
		if(!corner) return false;

		var padl = n.getStyle('padding-left').toInt(),
			padr = n.getStyle('padding-right').toInt();
		var node = new Element('b',{'class':'roundedCorners','styles':{
			'display':'block',
			'margin-left':-1*padl,
			'margin-right':-1*padr
		} });

		corner.each(function(line){
			var el = new Element('div', {'styles': {
				'height':line.height,
				'overflow':'hidden',
				'border-width':'0',
				'background-color':color.hex
			} });

			if(border.hex){
				el.setStyles({'border-color':border.hex,'border-style':'solid'});

				if(line.borderTop){
					el.setStyles({'border-top-width':line.borderTop,'height':'0'});
				}
			}
			if(left != 'n') el.setStyle('margin-left', line.margin);
			if(right != 'n') el.setStyle('margin-right', line.margin);

			if(border.hex){
				el.setStyles({
					'border-left-width': (left  == 'n') ? '1px': line.borderSide,
					'border-right-width': (right == 'n') ? '1px': line.borderSide
				});
			}
			node.adopt(el);
		});
		return node;
	},

	// move all children of the node inside a DIV and set color and bordercolor
	addBody: function(n, color, border){

		var padl = n.getStyle('padding-left').toInt(),
			padr = n.getStyle('padding-right').toInt();

		var container = new Element('div',{'styles':{
			'overflow':'hidden',
			'margin-left':-1*padl,
			'margin-right':-1*padr,
			'padding-left':(padl==0) ? 4 : padl,
			'padding-right':(padr==0) ? 4 : padr,
			'background-color':color.hex
		} }).wrapContent(n);

		if(border.hex){
			//n.setStyles('border','');
			var st = "1px solid " + border.hex
			container.setStyles({'border-left':st, 'border-right': st });
		}
	}
}
Wiki.registerPlugin( RoundedCorners );


/*
Script: Tips
	Add mouse-hover Tips to your pages.
	Depends on Mootools Tips plugin.
	{{ %%tip-<tip-caption>   <tip body>  /% }}

Argument:
	caption - (optional)

DOM Structure:
(start code)
<div class="tip-TipCaption"> ...tip-body... </div>

<span class="tip-anchor">Tip Caption</span>
<div class="tip" display:none>
	<span> ... </span>
</div>

<div class="tip-anchor">Tip Caption</div>

At the bottom of the page:
<div class="tip" style="...show/hide...">
	<div class="tip-top">
	<div class="tip">
		<div class="tip-title"> Tip Caption </div>
		<div class="tip-text"> ...tip-body... </div>
	</div>
	<div class="tip-bottom">
</div>
(end)

Example:
>  %%tip-ClickHere some tip text /%

*/
Wiki.registerPlugin( function(page, name){

	var tips = [];

	page.getElements('div[class^=tip],span[class^=tip]').each( function(t){

		var parms = t.hide().className.split('-');
		if( parms.length > 1 || parms[0] == 'tip' ){

			var body = new Element('span').wrapContent(t),
				caption = parms[1] ? parms[1].deCamelize() : "tip.default.title".localize();

			tips.push(
				new Element('span',{ 'class': 'tip-anchor',	html: caption })
				.store('tip:title',caption)
				.store('tip:text',body)
				.inject(t,'before')
			);
		}
	});

	new Tips( tips );

});


/*
Script: Columns
	Format the page content side by side, in columns, like in a newspaper.
	HR elements (in wiki {{----}} markup) are used to separate the columns.
	Column widths are equal and automatically calculated.
	Optionally, you can specify the width in pixel(px) for the columns.

Arguments:
	width - (optional) column width in pixel(px)

Example:
>	%%columms-300
>		column-text1 ...
>		----
>		column-text1 ...
>	/%
*/
Wiki.registerPlugin( function(page, name){

	page.getElements('div[class^=columns]').each( function(block){

		var parms = block.className.split('-'),
			columnBreaks = block.getElements('hr');

		if( columnBreaks && columnBreaks.length>0 ){

			var columnCount = columnBreaks.length + 1,
				width = ( parms[1] ) ? parms[1]/columnCount+'px' : 95/columnCount+'%',
				wrapper = new Element('div', { 'class':'col', styles:{ width:width } }),
				col = wrapper.clone().injectTop(block),
				n;

			block.className='columns';

			//use native DOM to also copy text-elements.
			while( n = col.nextSibling ){

				if( n.tagName && n.tagName.toLowerCase() == 'hr' ){
					col = wrapper.clone().replaces(n);
				} else {
					col.appendChild( n );
				}

			}

			new Element('div',{ style:'clear:both'}).inject( block );
			//wrapper.empty(); //memory leak
		}
	});

});

/*
Script: Code-Prettifier

Credits:
	Based on http://google-code-prettify.googlecode.com/svn/trunk/README.html

	Prettify has been modified slightly to avoid processing of the same element.
	See http://code.google.com/p/google-code-prettify/issues/detail?id=40

Future extension:
	Add option to overrule the choice of language:
>	"bsh", "c", "cc", "cpp", "cs", "csh", "cyc", "cv", "htm", "html",
>Ê Ê "java", "js", "m", "mxml", "perl", "pl", "pm", "py", "rb", "sh",
>Ê Ê "xhtml", "xml", "xsl"

Example:
>	%%prettify {{{
>		some code snippet here ...
>	}}} /%

*/
Wiki.registerPlugin( function(page, name){

	var els = page.getElements('div.prettify pre, div.prettify code');

	if( els && els.length>0 ){

		els.addClass('prettyprint');
		prettyPrint(page);

	}

});