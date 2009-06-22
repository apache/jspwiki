/*
Script: SnipEditorSpecs.js
	Test following classes: SnipEditor, Textarea, UndoRedo

License:
	xxx

*/

describe('Dialog & descendent Class specs', {

	before_each : function() {
		$$('body')[0].adopt( tst = new Element('p') );
		tst.setHTML("empty");
		dialog = null;	
	},
	after_each: function(){
	
		dialog = null;
		tst.remove();
	},
	
	//BASE DIALOG CLASS : test TODO


	//SELECTION DIALOG
	'should turn a string into a selection dialog': function() {

		var dialog= new SelectionDialog({
			body:"left|center|right",
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("li",dialog.body), el = els[0];

		value_of(els.length).should_be(3);
		value_of(el.getText()).should_be('left');
		value_of(el.getProperty('title')).should_be('left');

		dialog.fireEvent('onSelect','newvalue');
		value_of(tst.getText()).should_be('newvalue');		
	},

	'should turn a array into a selection dialog': function() {
		var dialog = new SelectionDialog({
			body:['left','center'],
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("li",dialog.body), el = els[0];

		value_of(els.length).should_be(2);
		value_of(el.getText()).should_be('left');
		value_of(el.getProperty('title')).should_be('left');
	},

	'should turn an object into a selection dialog': function() {
		var dialog= new SelectionDialog({
			body:{Left:"left",Center:"center",Right:"right"},
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("li",dialog.body), el = els[2];

		value_of(els.length).should_be(3);
		value_of(el.getText()).should_be('Right');
		value_of(el.getProperty('title')).should_be('right');
	},
	
	'should hide the dialog when the autoClose flag is set': function(){

		//the mockup Event object allows testing with keyboard and mouse events
		Event.implement({ 
			initialize: function(e){ 
				//alert(Json.toString(e));
				for(var ee in e ){ this[ee] = e[ee]; }
				//this.event=''; //used by snipeditor to check the event.which flag
				return this; 
			},
			stop: function(){ return this; }
		});

		var dialog= new SelectionDialog({
			body:"left|center|right",
			caption:"select",
			relativeTo:tst,
			showNow:false,
			autoClose:true,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		value_of(dialog.element.getStyle('display')).should_be('none'); 
		
		dialog.toggle();
		value_of(dialog.element.getStyle('display')).should_not_be('none'); 

		var el = $ES("li",dialog.body)[1];
		dialog.onSelect({ 'type':'mousedown', target:el });
		value_of(tst.getText()).should_be('center');

		value_of(dialog.element.getStyle('display')).should_be('none');		
	},

	'should make centered selection dialog': function(){
		//fixme : getCoordinates seems not to run under JSSpec properly
		var dialog= new SelectionDialog({
			body:"left|center|right",
			caption:"select",
			relativeTo:tst, //should remove this line
			autoClose:true,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		//todo validate position on the dialog
	},


	//FONT DIALOG		
	'should generate a standard Font Dialog': function() {

		var dialog= new FontDialog({
			caption:"font",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("li",dialog.body), el = els[1];

		value_of(els.length).should_be(9);
		value_of(el.getText()).should_be('Comic Sans');
		value_of(el.getProperty('title')).should_be('comic sans ms');

		//funny browser 'dependency'
		if(window.webkit){
			value_of(el.getStyle('font-family')).should_be("'comic sans ms'");
		} else {
			value_of(el.getStyle('font-family')).should_be("comic sans ms");
		}
		dialog.fireEvent('onSelect','newvalue');
		value_of(tst.getText()).should_be('newvalue');
	},

	'should generate a redefined Font Dialog': function() {

		var dialog= new FontDialog({
			fonts:{'Font name1':'font1', 'Font name2':'font2'},
			caption:"font",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("li",dialog.body), el = els[1];

		value_of(els.length).should_be(2);
		value_of(el.getText()).should_be('Font name2');
		value_of(el.getProperty('title')).should_be('font2');
		value_of(el.getStyle('font-family')).should_be('font2');

	},

	//CHARS DIALOG
	'should generate a standard Chars Dialog': function() {

		var dialog= new CharsDialog({
			caption:"special chars",
			relativeTo:tst,
			onSelect:function(value){ tst.setHTML( value ); }
		});

		var els = $ES("td",dialog.body), el = els[11];

		value_of(els.length).should_be(9*11);
		//value_of(el.getText()).should_be('&deg;');
		value_of(el.getProperty('title')).should_be('&deg;');

		dialog.fireEvent('onSelect','newvalue');
		value_of(tst.getText()).should_be('newvalue');
		
	},
	
	//COLOR DIALOG

	'should generate a standard Color Dialog': function() {

		var dialog= new ColorDialog({
			relativeTo:tst,
			onChange:function(value){ tst.setHTML( value ); }
		});

		var img = $ES("img",dialog.body);

		value_of(img.length).should_be(1);
		value_of(img[0].getProperty('src')).should_be('images/circle-256.png');
		value_of(dialog.color.getText()).should_be('#ffffff');
		value_of(dialog.cursor).should_not_be( null );
		value_of(dialog.hsv).should_be([0,0,100]);

		dialog.fireEvent('onChange','newvalue');
		value_of(tst.getText()).should_be('newvalue');
	}

});

