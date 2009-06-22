/*
Script: jspwiki-commonSpecs.js

License:
	http://www.apache.org/licenses/LICENSE-2.0
*/

describe('Basic Javascript specs', {

	'toLowerCase()': function() {
		value_of('Hello'.toLowerCase()).should_be('hello');
	},
	'Match array literals': function() {
		value_of([1,2,3]).should_be([1,2,3]);
	},
	'Difference of array literals': function() {
		value_of([1,2,3]).should_not_be([4,5,6]);
	},
	'Match date literals': function() {
		value_of(new Date(1979,03,27)).should_be(new Date(1979,03,27));
	},
	'DOM firstChild operator': function() {
		value_of(document.body.firstChild).should_be(document.body.firstChild);
	}
});

describe('Common mootools extensions',{

	'should convert camel-case into space separated sentences': function(){

		value_of('thisIsCamelCase'.deCamelize()).should_be('this Is Camel Case');
		value_of('thisIsACamelCase'.deCamelize()).should_be('this Is ACamel Case');

	},
	
	'should truncate a string to a maximum size': function(){
	
		value_of('test'.trunc(6)).should_be('test');
		value_of('test a longer string'.trunc(6)).should_be('test a...');
		value_of('test a longer string'.trunc(6,"---")).should_be('test a---');
	
	},
	
	'should return the text value of a DOM element': function(){
	
		var a = new Element('div');
		a.innerHTML= "test string";

		value_of( $getText(a) ).should_be('test string');	
	
	},

	'should return the last item in the array': function(){

    	value_of([].getLast()).should_be(null);
    	value_of([3].getLast()).should_be(3);
	    value_of([1,2,3,0,0,6].getLast()).should_be(6);

	},
 
	'should put an wrapper around a set of DOM elements': function(){
		var html = "<div>some text</div>Some plain text<span>more text</span><p>And more text</p>";
		var div = new Element('div').setHTML(html);
		var wrap = new Element('p').wraps(div);
		
		value_of(wrap.innerHTML).should_be(html);
		value_of(div.innerHTML).should_be("<p>"+html+"</p>");

	},
	
	'should return the visibility of an DOM element, depending on the visibility of one of its parents': function() {

		var span = new Element('span').setHTML('Inside text');
		var div = new Element('div',{'styles':{'display':'none'}}).adopt(span);		
		value_of(span.visible()).should_be(false);

		div.setStyle('display','block');
		value_of(span.visible()).should_be(true);
	},

	'should return the default value of any form element': function(){

		var form = new Element('form').setHTML([
			"<input name='input' type='checkbox' checked='checked'></input>",
			"<select name='select'>",
			"<option value='volvo'>Volvo</option>",
			"<option value='saab' selected='true'>Saab</option>",
			"</select>",
			"<textarea name='textarea'>textarea-value</textarea>"
		].join(''));

		value_of( form.input.getDefaultValue() ).should_be(false);
		value_of( form.select.getDefaultValue() ).should_be('saab');
		value_of( form.textarea.getDefaultValue() ).should_be('textarea-value');

		//now change the value of the form elements
		form.input.checked = true;
		form.select.selectedIndex = 0;
		form.textarea.value = "new textarea-value";

		value_of( form.input.getValue() ).should_be('on'); //strange return value FIXME
		value_of( form.select.getValue() ).should_be('volvo');
		value_of( form.textarea.getValue() ).should_be('new textarea-value');

		//the default values remain unchanged
		value_of( form.input.getDefaultValue() ).should_be(false);
		value_of( form.select.getDefaultValue() ).should_be('saab');
		value_of( form.textarea.getDefaultValue() ).should_be('textarea-value');
	},
	
	'should return a localized string': function(){

		LocalizedStrings = {
			'javascript.moreInfo' : 'More',
			'javascript.imageInfo' : 'Image {0} of {1}'
		};
		
		value_of( "moreInfo".localize() ).should_be('More');
		value_of( "imageInfo".localize(2) ).should_be('Image 2 of ???1???');
		value_of( "imageInfo".localize(2,4) ).should_be('Image 2 of 4');
		value_of( "imageInfo".localize(2,4,6) ).should_be('Image 2 of 4');
		value_of( "funny string".localize() ).should_be('???funny string???');
	}

});


describe('Main WIKI class',{

	'should convert a pagename into a wiki url':function(){

		Wiki.PageUrl = '/JSPWiki-pipo/Wiki.jsp?page=%23%24%25';
		
		value_of(Wiki.toUrl('test')).should_be('/JSPWiki-pipo/Wiki.jsp?page=test');		
	},
	
	'should convert a wiki url into a pagename':function(){

		Wiki.PageUrl = '/JSPWiki-pipo/Wiki.jsp?page=%23%24%25';
		
		value_of(Wiki.toPageName('/JSPWiki-pipo/Wiki.jsp?page=test')).should_be('test');
		value_of(Wiki.toPageName('test')).should_be(false);
	},
	
	'should remove funny chars to make a valid wiki pagename':function(){
		
		value_of(Wiki.cleanPageName('  ab    cd  ')).should_be('ab cd');
		value_of(Wiki.cleanPageName('a1b2c3()&+,-=._$d4e5f6')).should_be('a1b2c3()&+,-=._$d4e5f6');
		value_of(Wiki.cleanPageName('ab%@!\\?cd')).should_be('abcd');	
	}

});


describe('WikiSlimbox class',{
});

describe('TabbedSection class',{
});

describe('Searchbox class',{
});

describe('Color class',{

	'hex constructor - should have the correct rgb': function(){
		var myColor = new Color('#a2c240');
		value_of(myColor).should_be([162, 194, 64]);
	},
	
	'hex constructor - should have the correct hex': function(){
		var myColor = new Color('#a2c240');
		value_of(myColor.hex).should_be('#a2c240');

		myColor = new Color('#666');  //short notation
		value_of(myColor.hex).should_be('#666666');
	},

	'rgb constructor - should have the correct rgb': function(){
		//var myColor = new Color('rgb(162, 194, 64)');
		//value_of(myColor).should_be([162, 194, 64]);

		var myColor = new Color([162, 194, 64]);
		value_of(myColor).should_be([162, 194, 64]);
	},

	'rgb constructor - should have the correct hex': function(){
		//var myColor = new Color('rgb(162, 194, 64)');
		//value_of(myColor.hex).should_be('#a2c240');

		myColor = new Color([162, 194, 64]);
		value_of(myColor.hex).should_be('#a2c240');
	},

	'html-name constructor - should have the correct hex': function(){
		var myColor = new Color('lime');
		value_of(myColor.hex).should_be('#00ff00');
		value_of(myColor).should_be([0, 255, 0]);
	},
	
	'should mix two colors': function(){
		var myColor = new Color('#000').mix('#fff', [255, 0, 255], 10);
		value_of(myColor.hex).should_be('#311731');
	},
	
	'should invert a color': function(){
		var white = new Color('white');
		var black = new Color('black');

		value_of(white.invert().hex).should_be( black.hex );		
	}
});


describe('GraphBar class',{
});

describe('Collapsible class',{
});

describe('TableAdds class',{

	before_each : function() {
		document.body.adopt( myTable = new Element('table') );
		
		//rowbuilder help function
		row = function(r){
			return ("<tr><td>"+r.split('|').join('</td><td>')+"</td></tr>");
		};
		//return column as js-array of values
		getcol = function(column){
			var result = [];
			$A( myTable.rows ).each( function(r,i){
				if( (i>0) && (r.style.display != 'none')) result.push( $(r.cells[column]).getText().trim() );
			});
			return result;
		};

		var s = '';
		s+= row('Title  | Author  | Published    | Edition  | Some IP@         | Expenses  | Disk Memory').replace(/td>/g,"th>");
		s+= row('book1  |  zappy  |  25 Feb 2005 |  5       |  100.100.100.100 |  €500     |  2Gb ');
		s+= row('book2  |  happy  |  25 Jan 2005 |  19      |  256.100.100.100 |  €1500    |  4Kb');
		s+= row('book3  |  pappy  |  23 Mar 2005 |  06      |  10.100.100.100  |  €50      |  1.23TB');
		s+= row('book4  |  dappy  |  21 Apr 2005 |  199     |  1.100.100.100   |  €0.500   |  2.73kb');
		s+= row('book5  |  pappy  |  25 Jul 2005 |  017     |  1.100.25.100    |  €5500    |  0.4Tb');

		myTable.setHTML(s);		
	},
	after_each: function(){
		myTable.remove();
	},

	'should add default color zebra stripes to a table': function(){
		new TableAdds(myTable, {zebra:['table']});

		var rows = myTable.rows;
		value_of( rows[1].hasClass('odd') ).should_be(false);
		value_of( rows[2].hasClass('odd') ).should_be(true);
		value_of( rows[3].hasClass('odd') ).should_be(false);
		
	},
	'should add one color zebra stripes to a table': function(){
		new TableAdds(myTable, {zebra:['#fffff']});

		var rows = myTable.rows;
		value_of( rows[1].getStyle('background-color') ).should_be('transparent');
		value_of( rows[2].getStyle('background-color') ).should_be('#ffffff');
		value_of( rows[3].getStyle('background-color') ).should_be('transparent');
		
	},

	'should add 2 color zebra stripes to a table': function(){
		new TableAdds(myTable, {zebra:['#fffff','lime']});

		var rows = myTable.rows;
		value_of( rows[1].getStyle('background-color') ).should_be('#00ff00');
		value_of( rows[2].getStyle('background-color') ).should_be('#ffffff');
		value_of( rows[3].getStyle('background-color') ).should_be('#00ff00');
		
	},

	'should sort numbers': function(){
	
		var tt = new TableAdds(myTable, {sort:true});
		var column = 3, data=[];

		data = getcol(column);
		value_of(data).should_be( ['5', '19', '06', '199', '017'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['5', '06', '017', '19', '199'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['199', '19', '017', '06', '5' ] );		
	
	},
	'should sort dates': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 2, data=[];

		data = getcol(column);
		value_of(data).should_be( ['25 Feb 2005', '25 Jan 2005', '23 Mar 2005', '21 Apr 2005', '25 Jul 2005'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['25 Jan 2005',  '25 Feb 2005', '23 Mar 2005', '21 Apr 2005', '25 Jul 2005'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['25 Jul 2005', '21 Apr 2005', '23 Mar 2005', '25 Feb 2005', '25 Jan 2005'] );		
	
	},
	'should sort currency': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 5, data=[];

		data = getcol(column);
		value_of(data).should_be( ['€500', '€1500', '€50', '€0.500', '€5500'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['€0.500', '€50', '€500', '€1500', '€5500'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['€5500', '€1500', '€500', '€50', '€0.500'] );
	
	},
	'should sort ip@': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 4, data=[];

		data = getcol(column);
		value_of(data).should_be( ['100.100.100.100', '256.100.100.100', '10.100.100.100', '1.100.100.100', '1.100.25.100'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['1.100.25.100', '1.100.100.100', '10.100.100.100', '100.100.100.100', '256.100.100.100'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['256.100.100.100', '100.100.100.100', '10.100.100.100', '1.100.100.100', '1.100.25.100'] );
	
	},
	'should sort Tb, Gb, Mb, Kb memory values': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 6, data=[];

		data = getcol(column);
		value_of(data).should_be( ['2Gb', '4Kb', '1.23TB', '2.73kb', '0.4Tb'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['2.73kb', '4Kb', '2Gb', '0.4Tb', '1.23TB'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['1.23TB', '0.4Tb', '2Gb', '4Kb', '2.73kb'] );
	
	},
	'should sort strings': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 1, data=[];

		data = getcol(column);
		value_of(data).should_be( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_be( ['dappy', 'happy', 'pappy', 'pappy', 'zappy'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_be( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );
	
	},
	'should sort with "sortvalue" attributes': function(){

		var tt = new TableAdds(myTable, {sort:true});
		var column = 3, data=[];
		var rows = myTable.rows;
		rows[1].cells[column].setAttribute('jspwiki:sortvalue',0);
		rows[2].cells[column].setAttribute('jspwiki:sortvalue',1);
		rows[3].cells[column].setAttribute('jspwiki:sortvalue',2);
		rows[4].cells[column].setAttribute('jspwiki:sortvalue',3);
		rows[5].cells[column].setAttribute('jspwiki:sortvalue',4);


		data = getcol(column);
		value_of(data).should_be( ['5', '19', '06', '199', '017'] );
		
		tt.sort(column); //ascending
		data = getcol(column);
		value_of(data).should_not_be( ['5', '06', '017', '19', '199'] );
		value_of(data).should_be( ['5', '19', '06', '199', '017'] );

		tt.sort(column); //descending
		data = getcol(column);
		value_of(data).should_not_be( ['199', '19', '017', '06', '5' ] );		
		value_of(data).should_be( ['017', '199', '06', '19', '5'] );
	
	},
	'should filter a columns': function(){

		var tt = new TableAdds(myTable, {filter:true});
		var column = 3, data=[];

		data = getcol(column);
		value_of(data).should_be( ['5', '19', '06', '199', '017'] );
		
		tt.filter(column, 6);
		data = getcol(column);
		value_of(data).should_be( ['06'] );


	},
	'should filter a columns with more then 1 unique element': function(){

		var tt = new TableAdds(myTable, {filter:true});
		var tt = new TableAdds(myTable, {sort:true});
		var column = 1, data=[];


		data = getcol(column);
		value_of(data).should_be( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );
		
		tt.filter(column, 'pappy');
		data = getcol(column);
		value_of(data).should_be( ['pappy','pappy'] );
	
		data = getcol(0);
		value_of(data).should_be( ['book3','book5'] );
	},
	'should combine sorting and filtering to a table': function(){

		var t1 = new TableAdds(myTable, {filter:true});

		value_of(t1.filters).should_not_be(undefined);
		value_of(t1.sorted).should_be(undefined);

		var t2 = new TableAdds(myTable, {sort:true});
		value_of(t1).should_be( t2 );
		value_of(t1.filters).should_not_be(undefined);
		value_of(t1.sorted).should_be(true);		

		var column = 1, data=[];
		data = getcol(column);
		value_of(data).should_be( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );
		
		t1.sort(column);
		t1.sort(column);
		data = getcol(column);
		value_of(data).should_be( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );

		t1.filter(column, 'pappy');
		data = getcol(column);
		value_of(data).should_be( ['pappy','pappy'] );
	
		data = getcol(0);
		value_of(data).should_be( ['book5','book3'] );


	},
	'should combine sorting, filtering and zebra-stripes to a table': function(){

		var t1 = new TableAdds(myTable, {filter:true, sort:true, zebra:['red'] });

		value_of(t1.filters).should_not_be(undefined);
		value_of(t1.sorted).should_be(true);		
		value_of(t1.zebra).should_not_be(undefined);
		
		var column = 1, data=[];
		data = getcol(column);
		value_of(data).should_be( ['zappy', 'happy', 'pappy', 'dappy', 'pappy'] );
		
		t1.sort(column);
		t1.sort(column);
		data = getcol(column);
		value_of(data).should_be( ['zappy', 'pappy', 'pappy', 'happy', 'dappy'] );

		t1.filter(column, 'pappy');
		data = getcol(column);
		value_of(data).should_be( ['pappy','pappy'] );	
		data = getcol(0);
		value_of(data).should_be( ['book5','book3'] );

		var rows = myTable.rows;
		value_of( rows[0].getStyle('background-color') ).should_be('transparent');
		value_of( rows[2].getStyle('background-color') ).should_be('transparent');
		value_of( rows[3].getStyle('background-color') ).should_be('#ff0000');

	}

});

describe('Categories class',{
});

describe('HighlightWord class',{
});
