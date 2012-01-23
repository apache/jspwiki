/*
Script: spec-dialog.js
	Test following classes: SnipEditor, Textarea, UndoRedo

License:
	xxx

*/

describe('Dialog & descendent Class specs', function(){

	var tst, dialog;

	beforeEach( function() {
		$$('body')[0].adopt( tst = new Element('p') );
		tst.set('html',"empty");
		dialog = null;
	});
	afterEach( function(){

		dialog = null; //fixme: remove Elements created by dialogs
		tst.dispose();
	});

	//BASE DIALOG CLASS : test TODO


	//SELECTION DIALOG
	it('should turn a string into a selection dialog', function() {

		var dialog = new SelectionDialog({
			body:"left|center|right",
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements('li'), el = els[0];

		expect(els.length).toEqual(3);
		expect(el.get('text')).toEqual('left');
		expect(el.get('title')).toEqual('left');

		dialog.fireEvent('onSelect','newvalue');
		expect(tst.get('text')).toEqual('newvalue');
	});

	it('should turn a array into a selection dialog', function() {
		var dialog = new SelectionDialog({
			body:['left','center'],
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements('li'), el = els[0];

		expect(els.length).toEqual(2);
		expect(el.get('text')).toEqual('left');
		expect(el.get('title')).toEqual('left');
	});

	it('should turn an object into a selection dialog', function() {
		var dialog= new SelectionDialog({
			body:{Left:"left",Center:"center",Right:"right"},
			caption:"select",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements('li'), el = els[2];

		expect(els.length).toEqual(3);
		expect(el.get('text')).toEqual('Right');
		expect(el.get('title')).toEqual('right');
	});

	it('should hide the dialog when the autoClose flag is set', function(){

		//the mockup Event object allows testing with keyboard and mouse events
		Event.implement({
			initialize: function(e){
				//alert(JSON.encode(e));
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
			onSelect:function(value){ tst.set('html', value ); }
		});

		expect(dialog.element.getStyle('display')).toEqual('none');

		dialog.toggle();
		expect(dialog.element.getStyle('display')).toNotEqual('none');

		var el = dialog.body.getElements('li')[1];
		dialog.onSelect({ 'type':'mousedown', target:el });
		expect(tst.get('text')).toEqual('center');

		expect(dialog.element.getStyle('display')).toEqual('none');
	});

	it('should make centered selection dialog', function(){
		//fixme : getCoordinates seems not to run under JSSpec properly
		var dialog= new SelectionDialog({
			body:"left|center|right",
			caption:"select",
			relativeTo:tst, //should remove this line
			autoClose:true,
			onSelect:function(value){ tst.set('html', value ); }
		});

		//todo validate position on the dialog
	});


	//FONT DIALOG
	it('should generate a standard Font Dialog', function() {

		var dialog= new FontDialog({
			caption:"font",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements('li'), el = els[1];

		expect(els.length).toEqual(9);
		expect(el.get('text')).toEqual('Comic Sans');
		expect(el.get('title')).toEqual('comic sans ms');

		//grnch -- funny browser 'dependency'
		if( Browser.Engine.webkit ){
			expect(el.getStyle('font-family')).toEqual("'comic sans ms'");
		} else {
			expect(el.getStyle('font-family')).toEqual("comic sans ms");
		}
		dialog.fireEvent('onSelect','newvalue');
		expect(tst.get('text')).toEqual('newvalue');
	});

	it('should generate a redefined Font Dialog', function() {

		var dialog= new FontDialog({
			fonts:{'Font name1':'font1', 'Font name2':'font2'},
			caption:"font",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements('li'), el = els[1];

		expect(els.length).toEqual(2);
		expect(el.get('text')).toEqual('Font name2');
		expect(el.get('title')).toEqual('font2');
		expect(el.getStyle('font-family')).toEqual('font2');

	});

	//CHARS DIALOG
	it('should generate a standard Chars Dialog', function() {

		var dialog= new CharsDialog({
			caption:"special chars",
			relativeTo:tst,
			onSelect:function(value){ tst.set('html', value ); }
		});

		var els = dialog.body.getElements("td"), el = els[11];

		expect(els.length).toEqual(9*11);
		//expect(el.get('text')).toEqual('&deg;');
		expect(el.get('title')).toEqual('&deg;');

		dialog.fireEvent('onSelect','newvalue');
		expect(tst.get('text')).toEqual('newvalue');

	});

	//COLOR DIALOG

	it('should generate a standard Color Dialog', function() {

		var dialog= new ColorDialog({
			relativeTo:tst,
			onChange:function(value){ tst.set('html', value ); }
		});

		var img = dialog.body.getElements("img");

		expect(img.length).toEqual(1);
		expect(img[0].get('src')).toEqual('images/circle-256.png');
		expect(dialog.color.get('text')).toEqual('#ffffff');
		expect(dialog.cursor).toNotEqual( null );
		expect(dialog.hsv).toEqual([0,0,100]);

		dialog.fireEvent('onChange','newvalue');
		expect(tst.get('text')).toEqual('newvalue');
	});

});

