/*
Script: spec-jspwiki-common.js

License:
	http://www.apache.org/licenses/LICENSE-2.0
*/

describe('Basic Javascript specs', function(){

	it('shoud convert to lower-case', function(){
		expect('Hello'.toLowerCase()).toEqual('hello');
	});
    it('should concatenate two strings', function(){
		expect("Hello " + "World").toEqual("Hello World");
	});
	it('should add two numbers', function(){
		expect(1 + 2).toEqual(3);
	});
	it('should match array literals', function(){
		expect([1,2,3]).toEqual([1,2,3]);
	});
	it('should difference of array literals', function(){
		expect([1,2,3]).toNotEqual([4,5,6]);
	});
	it('should match date literals', function(){
		expect(new Date(1979,03,27)).toEqual(new Date(1979,03,27));
	});
	it('should retrieve the DOM firstChild', function(){
		expect(document.body.firstChild).toEqual(document.body.firstChild);
	});
});

describe('Common mootools extensions', function(){

	it('should convert camel-case into space separated sentences', function(){

		expect('thisIsCamelCase'.deCamelize()).toEqual('this Is Camel Case');
		expect('thisIsACamelCase'.deCamelize()).toEqual('this Is ACamel Case');

	});

	it('should truncate a string to a maximum size', function(){

		expect('test'.trunc(6)).toEqual('test');
		expect('test a longer string'.trunc(6)).toEqual('test a...');
		expect('test a longer string'.trunc(6,"---")).toEqual('test a---');

	});

	it('should return the text value of a DOM element', function(){

		var a = new Element('div');
		a.innerHTML= "test string";

		expect( $getText(a) ).toEqual('test string');

	});

	it('should return the last item in the array', function(){

    	expect([].getLast()).toEqual(null);
    	expect([3].getLast()).toEqual(3);
	    expect([1,2,3,0,0,6].getLast()).toEqual(6);

	});
 
	it('should put an wrapper around a set of DOM elements', function(){
		var html = "<div>some text</div>Some plain text<span>more text</span><p>And more text</p>";
		var div = new Element('div').set('html',html);
		var wrap = new Element('p').wrapContent(div);

		expect(wrap.innerHTML).toEqual(html);
		expect(div.innerHTML).toEqual("<p>"+html+"</p>");

	});

	it('should return the visibility of an DOM element, depending on the visibility of one of its parents', function(){

		var span = new Element('span').set('html','Inside text');
		var div = new Element('div',{'styles':{'display':'none'}}).adopt(span);
		expect(span.visible()).toEqual(false);

		div.setStyle('display','block');
		expect(span.visible()).toEqual(true);
	});

	it('should return the default value of any form element', function(){

		var form = new Element('form').set('html',[
			"<input name='input' type='checkbox' checked='checked'></input>",
			"<select name='select'>",
			"<option value='volvo'>Volvo</option>",
			"<option value='saab' selected='true'>Saab</option>",
			"</select>",
			"<textarea name='textarea'>textarea-value</textarea>"
		].join(''));
		expect( form.input.getDefaultValue() ).toBeFalsy();
		expect( form.select.getDefaultValue() ).toEqual('saab');
		expect( form.textarea.getDefaultValue() ).toEqual('textarea-value');

		//now change the value of the form elements
		form.input.checked = true;
		form.select.selectedIndex = 0;
		form.textarea.value = "new textarea-value";

		expect( form.input.get('value') ).toEqual('on'); //strange return value FIXME
		expect( form.select.get('value') ).toEqual('volvo');
		expect( form.textarea.get('value') ).toEqual('new textarea-value');

		//the default values remain unchanged
		expect( form.input.getDefaultValue() ).toBeFalsy();
		expect( form.select.getDefaultValue() ).toEqual('saab');
		expect( form.textarea.getDefaultValue() ).toEqual('textarea-value');
	});

	it('should return a localized string', function(){

		LocalizedStrings = {
			'javascript.moreInfo' : 'More',
			'javascript.imageInfo' : 'Image {0} of {1}'
		};

		expect( "moreInfo".localize() ).toEqual('More');
		expect( "imageInfo".localize(2) ).toEqual('Image 2 of ???1???');
		expect( "imageInfo".localize(2,4) ).toEqual('Image 2 of 4');
		expect( "imageInfo".localize(2,4,6) ).toEqual('Image 2 of 4');
		expect( "funny string".localize() ).toEqual('???funny string???');
	});

});




describe('Main WIKI class',function(){

	it('should convert a pagename into a wiki url', function(){

		Wiki.PageUrl = '/JSPWiki-pipo/Wiki.jsp?page=%23%24%25';

		expect(Wiki.toUrl('test')).toEqual('/JSPWiki-pipo/Wiki.jsp?page=test');
	});

	it('should convert a wiki url into a pagename', function(){

		Wiki.PageUrl = '/JSPWiki-pipo/Wiki.jsp?page=%23%24%25';

		expect(Wiki.toPageName('/JSPWiki-pipo/Wiki.jsp?page=test')).toEqual('test');
		expect(Wiki.toPageName('test')).toBeFalsy();
	});

	it('should remove funny chars to make a valid wiki pagename', function(){

		expect(Wiki.cleanPageName('  ab    cd  ')).toEqual('ab cd');
		expect(Wiki.cleanPageName('a1b2c3()&+,-=._$d4e5f6')).toEqual('a1b2c3()&+,-=._$d4e5f6');
		expect(Wiki.cleanPageName('ab%@!\\?cd')).toEqual('abcd');
	});

});


describe('WikiSlimbox class',function(){
});

describe('TabbedSection class',function(){
});

describe('Searchbox class',function(){
});

describe('Color class',function(){

	it('hex constructor - should have the correct rgb', function(){
		var myColor = new Color('#a2c240');
		var myColor2 = new Color([162, 194, 64]);

		//expect(myColor).toEqual([162, 194, 64]);
		expect(myColor).toEqual(myColor2);
	});

	it('hex constructor - should have the correct hex', function(){
		var myColor = new Color('#a2c240');
		expect(myColor.hex).toEqual('#a2c240');

		myColor = new Color('#666');  //short notation
		expect(myColor.hex).toEqual('#666666');
	});

	it('rgb constructor - should have the correct rgb', function(){
		var myColor = new Color([162, 194, 64],'rgb');
		var myColor2 = new Color([162, 194, 64]);

		expect(myColor).toEqual(myColor2);
	});

	it('rgb constructor - should have the correct hex', function(){
		myColor = new Color([162, 194, 64],'rgb');
		expect(myColor.hex).toEqual('#a2c240');

		myColor = new Color([162, 194, 64]);
		expect(myColor.hex).toEqual('#a2c240');
	});

	it('html-name constructor - should have the correct hex', function(){
		var myColor = new Color('lime');

		expect(myColor.hex).toEqual('#00ff00');
		expect(myColor).toEqual(new Color([0, 255, 0]));
	});

	it('should mix two colors', function(){
		var myColor = new Color('#000').mix('#fff', [255, 0, 255], 10);
		expect(myColor.hex).toEqual('#311731');
	});

	it('should invert a color', function(){
		var white = new Color('white');
		var black = new Color('black');

		expect(white.invert().hex).toEqual( black.hex );
	});
});


describe('GraphBar class',function(){

	var graph;

	//helperfunction to build graphbars.
	var buildTable = function(clazzname, markup){

		var html = markup.map(function(row){
			row = row.replace( /\|.[^|]*/g, function(match){
				return (match.charAt(1)=='|') ? '<th>'+match.slice(2)+'</th>' : '<td>'+match.slice(1)+'</td>';
			});
			return '<tr>'+row+'</tr>';
		});

		graph.empty().removeClass('graphBars').addClass(clazzname)
			.adopt( new Element('table',{'html':html}) );

		new GraphBar(graph);
	};

	beforeEach( function(){

		document.body.adopt( graph = new Element('div',{'class':'graphBars'}) );

	});

	afterEach( function(){

		graph.dispose();

	});

	it('should generate inline horizontal bars',function(){
//		%%graphBars
//		* This is the 1st bar: %%gBar 100 /%
//		* This is the 2nd bar: %%gBar 120 /%
//		* This is the 3rd bar: %%gBar 140 /%
//		/%
		graph.empty().adopt(
			new Element('ul').adopt(
				new Element('li',{html:"This is the 1st bar: <span class='gBar'>100</span>"}),
				new Element('li',{html:"This is the 2nd bar: <span class='gBar'>120</span>"}),
				new Element('li',{html:"This is the 3rd bar: <span class='gBar'>140</span>"})
			)
		);
		//run render graphbar :
		new GraphBar(graph);

		expect( $$('span.graphBar').length).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");

	});

	it('should parse data in tables, with %%gBar',function(){
//		%%graphBars
//		| apples  | 20 kg
//		| pears   | 40 kg
//		| bananas | 60 kg
//		/%
		buildTable("graphBars", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");

	});

	it('should parse column data in tables, with names',function(){
//		%%graphBarsWeight
//		|| Name   ||Weight
//		| apples  | 20 kg
//		| pears   | 40 kg
//		| bananas | 60 kg
//		/%
		buildTable('graphBarsWeight', [
			"|| Name   || Weight ",
			"| apples  | 20 kg ",
			"| pears   | 40 kg ",
			"| bananas | 60 kg "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");

	});

	it('should parse row data in tables, with names',function(){
//		%%graphBarsWeight
//		||Name    | apples | pears | bananas
//		||Weight  | 20 kg  | 40 kg | 60
//		/%
		buildTable('graphBarsWeight', [
			"||Name    | apples | pears | bananas",
			"||Weight  | 20 kg  | 40 kg | 60 "
		]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");


	});

	it('should do vertical bars',function(){
//		%%graphBars-vertical
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%

		buildTable('graphBars-vertical', [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> " ]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-bottom-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("position") ).toEqual("absolute");
		expect( $$('span.graphBar')[0].getStyle("width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("bottom") ).toEqual("0px");
		expect( $$('span.graphBar')[0].getParent().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[0].getNext().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[0].getNext().getStyle("top") ).toEqual("300px");


		expect( $$('span.graphBar')[1].getStyle("border-bottom-width") ).toEqual("170px");
		expect( $$('span.graphBar')[1].getStyle("position") ).toEqual("absolute");
		expect( $$('span.graphBar')[1].getStyle("width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("bottom") ).toEqual("0px");
		expect( $$('span.graphBar')[1].getParent().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[1].getNext().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[1].getNext().getStyle("top") ).toEqual("150px");

		expect( $$('span.graphBar')[2].getStyle("border-bottom-width") ).toEqual("320px");
		expect( $$('span.graphBar')[2].getStyle("position") ).toEqual("absolute");
		expect( $$('span.graphBar')[2].getStyle("width") ).toEqual("20px");
		expect( $$('span.graphBar')[2].getStyle("bottom") ).toEqual("0px");
		expect( $$('span.graphBar')[2].getParent().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[2].getNext().getStyle("position") ).toEqual("relative");
		expect( $$('span.graphBar')[2].getNext().getStyle("top") ).toEqual("0px");

	});

	it('should generate date&time graph-bars',function(){
//		%%graphBars
//		|| Name   ||Weight
//		| apples  | %%gBar 1 Jan 2006 /%
//		| pears   | %%gBar 15 Feb 2006 /%
//		| bananas | %%gBar 1 Apr 2006 /%
//		/%
		buildTable("graphBars", [
			"| apples  | <span class='gBar'>1 Jan 2006 </span> ",
			"| pears   | <span class='gBar'>20 Feb 2006</span> ",
			"| bananas | <span class='gBar'>1 Apr 2006 </span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("186px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");

	});

	it('should accept single color input',function(){
//		%%graphBars-fuchsia
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-fuchsia", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("border-left-color") ).toEqual("#ff00ff");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[1].getStyle("border-left-color") ).toEqual("#ff00ff");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");
		expect( $$('span.graphBar')[2].getStyle("border-left-color") ).toEqual("#ff00ff");

	});

	it('should accept double color input to generate gradient colors for all bars',function(){
//		%%graphBars-ffff00-669900
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-ffff00-669900", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("border-left-color") ).toEqual("#ffff00");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[1].getStyle("border-left-color") ).toEqual("#b3cc00");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");
		expect( $$('span.graphBar')[2].getStyle("border-left-color") ).toEqual("#669900");

	});

	it('should invert color if progress bars specify only one color',function(){
//		%%graphBars-00ffff-progress
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-red-progress", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(6);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("border-left-color") ).toEqual("#00ffff");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("300px");
		expect( $$('span.graphBar')[1].getStyle("border-left-color") ).toEqual("#ff0000");
		expect( $$('span.graphBar')[1].getStyle("margin-left") ).toEqual("-1ex");

		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-color") ).toEqual("#00ffff");
		expect( $$('span.graphBar')[3].getStyle("border-left-width") ).toEqual("150px");
		expect( $$('span.graphBar')[3].getStyle("border-left-color") ).toEqual("#ff0000");
		expect( $$('span.graphBar')[3].getStyle("margin-left") ).toEqual("-1ex");

		expect( $$('span.graphBar')[4].getStyle("border-left-width") ).toEqual("320px");
		expect( $$('span.graphBar')[4].getStyle("border-left-color") ).toEqual("#00ffff");
		expect( $$('span.graphBar')[5].getStyle("border-left-width") ).toEqual("0px");
		expect( $$('span.graphBar')[5].getStyle("border-left-color") ).toEqual("#ff0000");
		expect( $$('span.graphBar')[5].getStyle("margin-left") ).toEqual("-1ex");

	});

	it('should accept min and max bar size',function(){
//		%%graphBars-min48-max256
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-min48-max256", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("48px");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("152px");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("256px");

	});

	it('should do progress bars, with 2 complementary bars reaching 100%',function(){
//		%%graphBars-fff00-669900-progress
//		|| Name   ||Weight
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 40 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-ffff00-669900-progress", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>40 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(6);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("border-left-color") ).toEqual("#669900");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("300px");
		expect( $$('span.graphBar')[1].getStyle("border-left-color") ).toEqual("#ffff00");
		expect( $$('span.graphBar')[1].getStyle("margin-left") ).toEqual("-1ex");

		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("170px");
		expect( $$('span.graphBar')[2].getStyle("border-left-color") ).toEqual("#669900");
		expect( $$('span.graphBar')[3].getStyle("border-left-width") ).toEqual("150px");
		expect( $$('span.graphBar')[3].getStyle("border-left-color") ).toEqual("#ffff00");
		expect( $$('span.graphBar')[3].getStyle("margin-left") ).toEqual("-1ex");

		expect( $$('span.graphBar')[4].getStyle("border-left-width") ).toEqual("320px");
		expect( $$('span.graphBar')[4].getStyle("border-left-color") ).toEqual("#669900");
		expect( $$('span.graphBar')[5].getStyle("border-left-width") ).toEqual("0px");
		expect( $$('span.graphBar')[5].getStyle("border-left-color") ).toEqual("#ffff00");
		expect( $$('span.graphBar')[5].getStyle("margin-left") ).toEqual("-1ex");

	});

	it('should do gauge bars, with colors relative to the values',function(){
//		%%graphBars-fff00-669900-gauge
//		|| Name   ||Weight
//		| apples  | %%gBar 20 kg /%
//		| pears   | %%gBar 50 kg /%
//		| bananas | %%gBar 60 kg /%
//		/%
		buildTable("graphBars-ffff00-669900-gauge", [
			"| apples  | <span class='gBar'>20 kg</span> ",
			"| pears   | <span class='gBar'>50 kg</span> ",
			"| bananas | <span class='gBar'>60 kg</span> "]);

		expect( $$('span.graphBar').length ).toEqual(3);
		expect( $$('span.graphBar')[0].getStyle("border-left-width") ).toEqual("20px");
		expect( $$('span.graphBar')[0].getStyle("border-left-color") ).toEqual("#ffff00");
		expect( $$('span.graphBar')[1].getStyle("border-left-width") ).toEqual("245px");
		expect( $$('span.graphBar')[1].getStyle("border-left-color") ).toEqual("#8cb300");
		expect( $$('span.graphBar')[2].getStyle("border-left-width") ).toEqual("320px");
		expect( $$('span.graphBar')[2].getStyle("border-left-color") ).toEqual("#669900");

	});

});

describe('Collapsible class',function(){
});

describe('TableAdds class',function(){

	var myTable,row,getcol,s;

	beforeEach( function(){
		document.body.adopt( myTable = new Element('table') );

		//rowbuilder help function
		row = function(r){
			return ("<tr><td>"+r.split('|').join('</td><td>')+"</td></tr>");
		};
		//return column as js-array of values
		getcol = function(column){
			var result = [];
			$A( myTable.rows ).each( function(r,i){
				if( (i>0) && (r.style.display != 'none')) result.push( $(r.cells[column]).get('text').trim() );
			});
			return result;
		};

		var s = '';
		s+= row('Title  | Author  | Published    | Edition  | Some IP@         | Expenses  | Disk Memory|Float').replace(/td>/g,"th>");
		s+= row('book1  |  zappy  |  25 Feb 2005 |  5       |  100.100.100.100 |  €500     |  2Gb       |0.01');
		s+= row('book2  |  happy  |  25 Jan 2005 |  19      |  256.100.100.100 |  €1500    |  4Kb       |10e5');
		s+= row('book3  |  pappy  |  23 Mar 2005 |  06      |  10.100.100.100  |  €50      |  1.23TB    |-1e3');
		s+= row('book4  |  dappy  |  21 Apr 2005 |  199     |  1.100.100.100   |  €0.500   |  2.73kb    |0.443e-12');
		s+= row('book5  |  pappy  |  25 Jul 2005 |  017     |  1.100.25.100    |  €5500    |  0.4Tb     |0.1e-99');

		myTable.set('html',s);
	});

	afterEach( function(){
		myTable.dispose();
	});

	it('should add default color zebra stripes to a table', function(){
		new TableAdds(myTable, {zebra:['table']});

		var rows = myTable.rows;
		expect( rows[1].hasClass('odd') ).toBeFalsy();
		expect( rows[2].hasClass('odd') ).toBeTruthy();
		expect( rows[3].hasClass('odd') ).toBeFalsy();

	});
	it('should add one color zebra stripes to a table', function(){
		new TableAdds(myTable, {zebra:['#fffff']});

		var rows = myTable.rows;
		expect( rows[1].getStyle('background-color') ).toEqual('transparent');
		expect( rows[2].getStyle('background-color') ).toEqual('#ffffff');
		expect( rows[3].getStyle('background-color') ).toEqual('transparent');

	});

	it('should add 2 color zebra stripes to a table', function(){
		new TableAdds(myTable, {zebra:['#fffff','lime']});

		var rows = myTable.rows;
		expect( rows[1].getStyle('background-color') ).toEqual('#00ff00');
		expect( rows[2].getStyle('background-color') ).toEqual('#ffffff');
		expect( rows[3].getStyle('background-color') ).toEqual('#00ff00');

	});

	it('should sort integer numbers', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 3, data=[];

		data = getcol(column);
		expect(data).toEqual( ['5', '19', '06', '199', '017'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['5', '06', '017', '19', '199'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['199', '19', '017', '06', '5' ] );

	});

	it('should sort dates', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 2, data=[];

		data = getcol(column);
		expect(data).toEqual( ['25 Feb 2005', '25 Jan 2005', '23 Mar 2005', '21 Apr 2005', '25 Jul 2005'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['25 Jan 2005',  '25 Feb 2005', '23 Mar 2005', '21 Apr 2005', '25 Jul 2005'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['25 Jul 2005', '21 Apr 2005', '23 Mar 2005', '25 Feb 2005', '25 Jan 2005'] );

	});
	it('should sort currency', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 5, data=[];

		data = getcol(column);
		expect(data).toEqual( ['€500', '€1500', '€50', '€0.500', '€5500'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['€0.500', '€50', '€500', '€1500', '€5500'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['€5500', '€1500', '€500', '€50', '€0.500'] );

	});
	it('should sort ip@', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 4, data=[];

		data = getcol(column);
		expect(data).toEqual( ['100.100.100.100', '256.100.100.100', '10.100.100.100', '1.100.100.100', '1.100.25.100'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['1.100.25.100', '1.100.100.100', '10.100.100.100', '100.100.100.100', '256.100.100.100'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['256.100.100.100', '100.100.100.100', '10.100.100.100', '1.100.100.100', '1.100.25.100'] );

	});
	it('should sort Tb, Gb, Mb, Kb memory values', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 6, data=[];

		data = getcol(column);
		expect(data).toEqual( ['2Gb', '4Kb', '1.23TB', '2.73kb', '0.4Tb'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['2.73kb', '4Kb', '2Gb', '0.4Tb', '1.23TB'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['1.23TB', '0.4Tb', '2Gb', '4Kb', '2.73kb'] );

	});

	it('should sort floats', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 7, data=[];

		data = getcol(column);
		expect(data).toEqual( ['0.01', '10e5', '-1e3', '0.443e-12', '0.1e-99'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['-1e3', '0.1e-99', '0.443e-12', '0.01', '10e5'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['10e5', '0.01', '0.443e-12', '0.1e-99', '-1e3' ] );

	});

	it('should sort strings', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 1, data=[];

		data = getcol(column);
		expect(data).toEqual( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toEqual( ['dappy', 'happy', 'pappy', 'pappy', 'zappy'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toEqual( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );

	});

	it('should sort with "sortvalue" attributes', function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 3, data=[];
		var rows = myTable.rows;
		rows[1].cells[column].setAttribute('jspwiki:sortvalue',0);
		rows[2].cells[column].setAttribute('jspwiki:sortvalue',1);
		rows[3].cells[column].setAttribute('jspwiki:sortvalue',2);
		rows[4].cells[column].setAttribute('jspwiki:sortvalue',3);
		rows[5].cells[column].setAttribute('jspwiki:sortvalue',4);


		data = getcol(column);
		expect(data).toEqual( ['5', '19', '06', '199', '017'] );

		tt.sort(column); //ascending
		data = getcol(column);
		expect(data).toNotEqual( ['5', '06', '017', '19', '199'] );
		expect(data).toEqual( ['5', '19', '06', '199', '017'] );

		tt.sort(column); //descending
		data = getcol(column);
		expect(data).toNotEqual( ['199', '19', '017', '06', '5' ] );
		expect(data).toEqual( ['017', '199', '06', '19', '5'] );

	});

	it('should filter a columns', function(){

		var tt = new TableAdds(myTable, {filter:true});
		var column = 3, data=[];

		data = getcol(column);
		expect(data).toEqual( ['5', '19', '06', '199', '017'] );

		tt.filter(column, 6);
		data = getcol(column);
		expect(data).toEqual( ['06'] );


	});

	it('should filter a columns with more then 1 unique element', function(){

		var tt = new TableAdds(myTable, {filter:true});
		var tt = new TableAdds(myTable, {sort:true});
		var column = 1, data=[];


		data = getcol(column);
		expect(data).toEqual( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );

		tt.filter(column, 'pappy');
		data = getcol(column);
		expect(data).toEqual( ['pappy','pappy'] );

		data = getcol(0);
		expect(data).toEqual( ['book3','book5'] );
	});

	it('should combine sorting and filtering to a table', function(){

		var t1 = new TableAdds(myTable, {filter:true});

		expect(t1.filters).toNotEqual(undefined);
		expect(t1.sorted).toEqual(undefined);

		var t2 = new TableAdds(myTable, {sort:true});
		expect(t1).toEqual( t2 );
		expect(t1.filters).toNotEqual(undefined);
		expect(t1.sorted).toBeTruthy();

		var column = 1, data=[];
		data = getcol(column);
		expect(data).toEqual( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );

		t1.sort(column);
		t1.sort(column);
		data = getcol(column);
		expect(data).toEqual( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );

		t1.filter(column, 'pappy');
		data = getcol(column);
		expect(data).toEqual( ['pappy','pappy'] );

		data = getcol(0);
		expect(data).toEqual( ['book5','book3'] );


	});

	it('should combine sorting, filtering and zebra-stripes to a table', function(){

		var t1 = new TableAdds(myTable, {filter:true, sort:true, zebra:['red'] });

		expect(t1.filters).toNotEqual(undefined);
		expect(t1.sorted).toBeTruthy();
		expect(t1.zebra).toNotEqual(undefined);

		var column = 1, data=[];
		data = getcol(column);
		expect(data).toEqual( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );

		t1.sort(column);
		t1.sort(column);
		data = getcol(column);
		expect(data).toEqual( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );

		t1.filter(column, 'pappy');
		data = getcol(column);
		expect(data).toEqual( ['pappy','pappy'] );
		data = getcol(0);
		expect(data).toEqual( ['book5','book3'] );

		var rows = myTable.rows;
		expect( rows[0].getStyle('background-color') ).toEqual('transparent');
		expect( rows[2].getStyle('background-color') ).toEqual('transparent');
		expect( rows[3].getStyle('background-color') ).toEqual('#ff0000');

	});

});

describe('Categories class',function(){
});

describe('HighlightWord class',function(){
});


