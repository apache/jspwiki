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
		$ES('.accordion, .tabbedAccordion',page).each( function(tt){
			
			var toggles=[], contents=[], togglemenu=false;
			if(tt.hasClass('tabbedAccordion')) togglemenu = new Element('div',{'class':'togglemenu'}).injectBefore(tt);
			
			tt.getChildren().each(function(tab) {
				if( !tab.className.test('^tab-') ) return;

				//FIXME use class to make tabs visible during printing 
				//(i==0) ? tab.removeClass('hidetab'): tab.addClass('hidetab');

				var title = tab.className.substr(4).deCamelize();
				if(togglemenu) {
					toggles.push(new Element('div',{'class':'toggle'}).inject(togglemenu).appendText(title));
				} else {
					toggles.push(new Element('div',{'class':'toggle'}).injectBefore(tab).appendText(title));
				}        
				contents.push(tab.addClass('tab'));
			});
			new Accordion(toggles, contents, {     
				alwaysHide: !togglemenu,
				onComplete: function(){
					var el = $(this.elements[this.previous]);
					if (el.offsetHeight > 0) el.setStyle('height', 'auto');  
				},
				onActive: function(toggle,content){                          
					toggle.addClass('active'); 
					content.addClass('active').removeClass('xhidetab'); 
				},
				onBackground: function(toggle,content){ 
					content.setStyle('height', content['offsetHeight']);
					toggle.removeClass('active'); 
					content.removeClass('active').addClass('xhidetab');
				} 
			});
		});
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

	render: function(page,name){
		/* make reverse copies for bottom definitions */
		this.NormalBottom = this.NormalTop.slice(0).reverse();
		this.SmallBottom  = this.SmallTop.slice(0).reverse();

		for(selector in this.registry ){  // CHECK NEEDED
			var n = $$(selector), 
				parms = this.registry[selector];
			this.exec( n, parms[0], parms[1], parms[2], parms[3] );
		}

		$ES('#pagecontent *[class^=roundedCorners]',page).each(function(el){ 
			var parms = el.className.split('-');
			if( parms.length < 2 ) return;
			this.exec( [el], parms[1], parms[2], parms[3], parms[4] );
		},this);
	},

	exec: function( nodes, corners, color, borderColor, background ){

		corners = ( corners ? corners+"nnnn": "yyyy" );
		color   = new Color(color,'hex') || 'transparent';
		if(borderColor) borderColor = new Color(borderColor);
		if(background)  background  = new Color(background);

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
			if( !nodes[i] || nodes[i].passed ) continue;
			
			this.addBody(nodes[i], color, borderColor);
			if(nodeTop   ) nodes[i].insertBefore(nodeTop.cloneNode(true), nodes[i].firstChild);
			if(nodeBottom) nodes[i].appendChild(nodeBottom.cloneNode(true));
			
			nodes[i].passed=true;
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
			n.style.backgroundColor = color.hex;

			if( borderColor )
			{
				n.style.borderColor = borderColor.hex;
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
		var container = new Element('div').wrapChildren(node);

		container.style.padding = "0 4px";
		container.style.backgroundColor = color.hex;
		if( borderColor )
		{
			container.style.borderLeft  = "1px solid " + borderColor.hex;
			container.style.borderRight = "1px solid " + borderColor.hex;
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
