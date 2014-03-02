/*
Class: Dialog.Font
	The Dialog.Font is a Dialog.Selection object, to selecting a font.
	Each selectable item is redered in its proper font.

Arguments:
	options - optional, see options below

Options:
	fonts - (object) set of font definitions with name/value
	others - see Dialog.Selection options

Inherits from:
	[Dialog.Selection]

Example
	(start code)
	dialog= new Dialog.Font({
		fonts:{'Font name1':'font1', 'Font name2':'font2'},
		caption:"Select a Font",
		onSelect:function(value){ alert( value ); }
	});
	(end)
*/
Dialog.Font = new Class({

	Extends:Dialog.Selection,
	
	options: {
		fonts: {
			'arial':'Arial',
			'comic sans ms':'Comic Sans',
			'courier new':'Courier New',
			'garamond':'Garamond',
			'georgia':'Georgia',
			'helvetica':'Helvetica',
			'impact':'Impact',
			'times new roman':'Times',
			'tahoma':'Tahoma',
			'trebuchet ms':'Trebuchet',
			'verdana':'Verdana'
		}
	},

	initialize:function(options){

		var self = this, fonts = options.fonts;

        //options.cssClass = '.font'+(options.cssClass||'')
        this.setClass('.font',options);
		options.body = fonts ? fonts : self.options.fonts;

		self.parent(options);

		self.getItems().each(function(li){
			li.setStyle('font-family', li.get('title') );
		});

	}

});
