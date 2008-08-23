/* 
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
Adapted for JSPWiki/BrushedTemplate, D.Frederickx, Sep 06

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
				menu = new Element('div',{'class':'menu'}).injectBefore(tt);
			}
			else if(tt.hasClass('leftAccordion')){
				menu = new Element('div',{'class':'sidemenu left'}).injectBefore(tt);
			}
			else if(tt.hasClass('rightAccordion')){
				menu = new Element('div',{'class':'sidemenu right'}).injectBefore(tt);
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
		bullet=toggle=null;
	}
}
Wiki.addPageRender(WikiAccordion);


/** 220 RoundedCorners --experimental
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

		$ES('*[class^=roundedCorners]',page).each(function(el){ 
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
		} }).wrapChildren(n);

		if(border.hex){
			//n.setStyles('border','');
			var st = "1px solid " + border.hex
			container.setStyles({'border-left':st, 'border-right': st });
		}
	}
}
Wiki.addPageRender(RoundedCorners);


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
 ** Dirk Frederickx, Mar 07
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
