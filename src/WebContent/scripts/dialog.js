
/*
Class: SelectionDialog
	A selection dialog generates a dialog box, with a set
	of selectable/clickable items. 

Arguments:
	options - see [Dialog] object

Options:
	body - set of selectable items, defined as a string ,array or object.
	onSelect - callback function when an item is clicked
	autoClose - (default false) hide the dialog when an iten is clicked

Inherits from:
	[Dialog]
	
Example:
	(start code)
	new SelectionDialog({
		body:"a|b|c|d",
		caption:"Snippet Dialog",
		autoClose:true,
		onClick:function(v){ alert('clicked '+v) }
	});
	new SelectionDialog({
		body:[a,b,c,d],
		caption:"Snippet Dialog",
		onClick:function(v){ alert('clicked '+v) }
	});
	new SelectionDialog({
		body:{'a':'avalue','b':'bvalue','c':'cvalue'},
		caption:"Snippet Dialog",
		onClick:function(v){ alert('clicked '+v) }
	});
	(end code)
*/
var SelectionDialog = Dialog.extend({

	options: { 
		//onSelect: function(value){},
		//autoClose: false
	},	

	initialize:function( options ){

		this.parent(options);
		this.element.addClass('selectionDialog');
		this.setBody(options.body)
			.resetPosition();
	},

	setBody: function(content){

		//turn 'multi|value|string' into [array]
		if( $type(content) == 'string' ) content = content.split('|');

		//turn [array] into {object} with name:value pairs
		if( $type(content) == 'array' ) content = content.associate(content);

		//turn {object} in DOM ul/li clickable selection
		if( $type(content) == 'object' ){

			var els = [], onselect = this.onSelect.bind(this), i;
			for( i in content ){
				els.push( new Element('li',{ 
					'title': content[i],
					'events': {
						'click': onselect,
						'mouseout': function(){ this.removeClass('hover') },
						'mouseover': function(){ this.addClass('hover') }
					}
				}).setHTML( i/*.trunc(36)*/ ) );
			};
			
			this.parent( new Element('ul').adopt(els) );
			//this.body.empty().adopt( new Element('ul').adopt(els) );
		}
		
		return this;
	},
	/*
	Function: onSelect
		Click event handler for selectable items. 
		When the autoClose option is set, the dialog will be hidden.
		Fires the 'onSelect' event.
	*/
	onSelect: function(event){

		if( event ){ event = new Event(event).stop() }; 
		if( this.options.autoClose ) this.hide();		
				
		this.fireEvent('onSelect', event.target.getProperty('title')); 
	}

});

/*
Class: FontDialog
	The FontDialog is a SelectionDialog object, to selecting a font.
	Each selectable item is redered in its proper font.

Arguments:
	options - optional, see options below

Options:
	fonts - (object) set of font definitions with name/value
	others - see SelectionDialog options

Inherits from:
	[SelectionDialog]
	
Example
	(start code)
	dialog= new FontDialog({
		fonts:{'Font name1':'font1', 'Font name2':'font2'},
		caption:"Select a Font",
		onSelect:function(value){ alert( value ); }
	});
	(end)
*/
var FontDialog = SelectionDialog.extend({

	options: { 
		fonts: {
			'Arial':'arial',
			'Comic Sans':'comic sans ms',
			'Courier New':'courier new',
			'Georgia':'georgia', 
			'Helvetica':'helvetica', 
			'Impact':'impact', 
			'Times':'times new roman', 
			'Trebuchet':'trebuchet ms', 
			'Verdana':'verdana'
		}
	},	

	initialize:function(options){

		options.body = options.fonts ? options.fonts : this.options.fonts;
		this.parent(options);
		this.element.addClass('fontDialog');
		$ES('li',this.body).each(function(li){
			li.setStyle('font-family', li.getProperty('title') );
		});
	}
	
});

/*
Class: CharsDialog
	The CharsDialog is a Dialog object, to support selection of special
	character. 

Arguments:
	options - optional, see options below

Options:
	others - see Dialog options

Inherits from:
	[Dialog]
*/
var CharsDialog = Dialog.extend({

	options: { 
		//onChange: Class.empty, 
		//autoClose: false,
		chars: [
			'&nbsp;','&iexcl;','&cent;','&pound;','&yen;','&sect;','&uml;','&copy;','&laquo;','&not;','&reg;',
			'&deg;','&plusmn;','&acute;','&micro;','&para;','&middot;','&cedil;','&raquo;','&iquest;','&Agrave;','&Aacute;',
			'&Acirc;','&Atilde;','&Auml;','&Aring;','&AElig;','&Ccedil;','&Egrave;','&Eacute;','&Ecirc;','&Euml;','&Igrave;',
			'&Iacute;','&Icirc;','&Iuml;','&Ntilde;','&Ograve;','&Oacute;','&Ocirc;','&Otilde;','&Ouml;','&Oslash;','&Ugrave;',
			'&Uacute;','&Ucirc;','&Uuml;','&szlig;','&agrave;','&aacute;','&acirc;','&atilde;','&auml;','&aring;','&aelig;',
			'&ccedil;','&egrave;','&eacute;','&ecirc;','&euml;','&igrave;','&iacute;','&icirc;','&iuml;','&ntilde;','&ograve;',
			'&oacute;','&ocirc;','&otilde;','&ouml;','&divide;','&oslash;','&ugrave;','&uacute;','&ucirc','&uuml','&yuml;',
			'&#8218;','&#402;','&#8222;','&#8230;','&#8224;','&#8225;','&#710;','&#8240;','&#8249;','&#338;','&#8216;',
			'&#8217;','&#8220;','&#8221;','&#8226;','&#8211;','&#8212;','&#732;','&#8482;','&#8250;','&#339;','&#376;'
		]
	},	

	initialize:function(options){
		this.parent(options);
		this.element.addClass('charsDialog');

		/* inspired by smarkup */
		var arr = [], 
			chars = this.options.chars,
			rowCount = chars.length / 11, //fixme: fixed width of table !!
			onselect = this.onSelect.bind(this);
		
		for (var i = 0; i < rowCount; i++) {
			arr.push( '<tr>' );
			for (var j = i * 11; j < (i * 11 + 11); j++) {
				arr.extend( ['<td title="', chars[j].replace('&','&amp;'), '" >', chars[j], '</td>' ] );
			}
			arr.push( '</tr>' );
		}
		
		this.body.adopt( new Element('table',{
			'class':'charsDialog',
			'events':{ 'click': onselect }
		}).setHTML( '<tbody>', arr.join(''), '</tbody>' )
		);
		
		this.resetPosition();

	},

	onSelect: function(e){
		if( this.options.autoClose ) this.hide();
		this.fireEvent('onSelect', e.target.getProperty('title')); 
	}

});


/*
Class: ColorDialog
	The ColorDialog is a [Dialog] which allow visual entry of hexadecimal color 
	values.

Inspiration:
	- http://www.colorjack.com/software/dhtml+color+sphere.html
	- Chris Esler, http://www.chrisesler.com/mootools

Inherits from:
	[Dialog]

Example:
	ColorDialog with toggle button
	(start code)
	<script>
		var cd = new ColorDialog( {
			relativeTo: $('colorButton'), 
			wheelImage:'circle.png',
			onChange:function(color){ $('mytarget').setStyle('background',color); }
		});
		$('colorButton').addEvent('click', cd.toggle.bind(cd));
	</script>
	(end code)
*/
var ColorDialog = Dialog.extend({

	options: {
		//onChange: function(color){},
		colorImage: 'images/circle-256.png',
		resize:{x:[96,400],y:[96,400]}  //min/max limits for the resizer
	},	

	initialize: function(options){
	
		var self = this;
		self.parent(options);	
		self.element.addClass('colorDialog');
		self.hsv = [0,0,100];//starting color.
		self.color = new Element('span').setHTML('#ffffff').injectTop(self.caption);
		self.cursor = new Element('div',{
			'class':'cursor',
			'styles':{'top':86,'left':68}
			//funny calc -- checkout the dialog css defs 
			// 86=64+32-8 (-8=offset circle)
			// 68=64+10-5-1 (5=half cursor size, -1=offset circle)
		});
		self.body.adopt(
			self.cursor,
			new Element('img',{'src':self.options.colorImage})
		);

		self.resize( 128 ); //default size of the body/wheel
		
		new Drag.Base(self.cursor,{
			handle:self.body,
			snap:0,
			//also update the wheel on mouse-down
			onStart:function(){ self.setHSV( this.mouse.start ) },
			onDrag:function(){ self.setHSV( this.mouse.now ) }
		});

		self.resetPosition();
	},

	/*
	Function: resize
	*/
	resize: function( bodywidth ){
		this.moveCursor( bodywidth );
		this.parent( bodywidth );
	},

	/*
	Function: setHSV
		Recalculate the HSV-color based on x/y mouse coordinates.
		After recalculation, the color-wheel cursor is repositioned
		and the 'onChange' event is fired.
		
	Arguments:
		page - object with {{ {x:.., y:.. } }} coordinates
	*/
	setHSV: function( page ){

		var body = this.body.getCoordinates(), 
			v = [page.x - body.left + 5, page.y - body.top +28 ],
			W = body.width,
			W2 = W/2, 
			W3 = W2/2, 

			x = v[0]-W2-3, 
			y = W-v[1]-W2+21,
			SV = Math.sqrt(Math.pow(x,2)+Math.pow(y,2)),
			hue = Math.atan2(x,y)/(Math.PI*2);
			
		this.hsv = [
				hue>0?(hue*360):((hue*360)+360),
				SV<W3?(SV/W3)*100:100, 
				SV>=W3?Math.max(0,1-((SV-W3)/(W3)))*100:100
			];

		var hexVal = this.hsv.hsv2rgb().rgbToHex(); 
		this.color.setHTML( hexVal ).setStyles({
			'color': new Color(hexVal).invert().hex,
			'background-color': hexVal 
		}); 
		this.moveCursor( W );
		this.fireEvent('onChange', hexVal);

	},

	/*
	Function: moveCursor
		Reposition the cursor based on the width argument
		
	Argument
		width - in px
	*/
	moveCursor: function( width ){

		var hsv = this.hsv,
			W2 = width/2, //half of the width
			rad = (hsv[0]/360) * (Math.PI*2), //radius
			hyp = (hsv[1] + (100-hsv[2])) / 100*(W2/2); //hypothenuse

		this.cursor.setStyles({
			left: Math.round(Math.abs(Math.round(Math.sin(rad)*hyp) + W2 + 3)),  //+1
			top: Math.round(Math.abs(Math.round(Math.cos(rad)*hyp) - W2 - 21))  //-18
		});	

	}
		
});
ColorDialog.implement(new Options); //mootools v1.1

/*
Function: hsv2rgb
	Convert HSV values into RGB values
*/
Array.extend({
	hsv2rgb: function(){ 
		// easyrgb.com/math.php?MATH=M21#text21
	    var R,G,A,B,C,F,
	    	S=this[1]/100,
	    	V=this[2]/100,
	    	H=this[0]/360;

	    if( S>0 ){ 
	    
	    	if(H>=1) H=0;
	        H=6*H; 
	        F=H-Math.floor(H);
	        A=Math.round(255*V*(1-S));
	        B=Math.round(255*V*(1-(S*F)));
	        C=Math.round(255*V*(1-(S*(1-F))));
	        V=Math.round(255*V); 

	        switch(Math.floor(H)) {
	            case 0: R=V; G=C; B=A; break;
	            case 1: R=B; G=V; B=A; break;
	            case 2: R=A; G=V; B=C; break;
	            case 3: R=A; G=B; B=V; break;
	            case 4: R=C; G=A; B=V; break;
	            case 5: R=V; G=A; B=B; break;
	        }
	        return([R?R:0,G?G:0,B?B:0]);
	    }
	    else return([(V=Math.round(V*255)),V,V]);
	}
})


/*
Class: FormDialog
	The FormDialog is a dialog 

Example:
	(start code)
	FormDialog({
		body: dom-element,
		buttons: { ok:'OK-label', cancel:'Cancel-label' }
		options:{
			onSubmit:function( queryString ){ //process-results
		}
	(end)
*/
var FormDialog = Dialog.extend({
	//todo
});


/*
Class: TableDialog
	The TableDialog is a simple wiki table editor based on the Dialog class.

	- click cell and get small input-textarea to enter markup
	- allow to toggle header/normal-cell flag per cell
	- hover column header (A,B,C...) and click to insert/delete
	- hover row header (1,2,3,...) and click to insert/delete
	- allow to extend colums
	- allow to extend rows
	- automatically converts wiki to table and vice-versa
	- FFS: support [{Table plugin syntax to merge cells 
	  This requires to select multiple cells:  join/unjoin cells
	
Example:
	(start code)
	WikiTableDialog( wiki-markup ?? textarea,{
			buttons: { ok:'OK-label', cancel:'Cancel-label' }
			onChange:function()
		}
	})
	(end)
*/
var TableDialog = Dialog.extend({
	//todo
});
