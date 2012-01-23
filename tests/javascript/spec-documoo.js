/*
Script: spec-documoo.js
	Test following classes: Jsdocs

License:
	xxx

*/

describe('Documoo Class', function(){

	var test, documoo;

	beforeEach( function() {
		$$('body')[0].adopt( test = new Element('div',{id:"TEST"}) );
		test.set('html',"empty");

		documoo = new Documoo();
	});
	afterEach( function(){
		test.dispose();
	});

	it('should retain text without markup', function() {

		var s = [
			"/* Some text without markup. ",
			"   End of text.*/"
		].join('\n');

		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( test.get('text') ).toEqual( " Some text without markup. \n   End of text." );

	});

	it('should format bold, italic and monospace text', function() {

		var s = [
			"/*	First a *bold* word or __two__ ",
			"   More ''italic'' words are here",
			"   And finally some {{monospaced}} text",
			"	End of text.*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST b").length ).toEqual( 2 );
		expect( $$("#TEST i").length ).toEqual(1);
		expect( $$("#TEST tt").length ).toEqual(1);
		expect( $$("#TEST b")[1].get('text') ).toMatch(/two/);
		expect( $$("#TEST i")[0].get('text') ).toMatch(/italic/);
		expect( $$("#TEST tt")[0].get('text') ).toMatch(/monospaced/);

	});

	it('should allow to escape markup with a tilde', function() {

		var s = [
			"/*	This is not a ~*bold~* word",
			"	End of text.*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST b").length ).toEqual( 0 );
		expect( $$("#TEST")[0].get('text') ).toMatch(/ \*bold\* /);

	});

	it('should format headings and sub-headings', function() {

		var s = [
			"/*	",
			"Class: testClass",
			"   Documentation text comes here",
			"Arguments:",
			"   This is a subheading",
			"*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST h1").length ).toEqual( 1 );
		expect( $$("#TEST h2").length ).toEqual( 1 );
		expect( $$("#TEST h1")[0].get('text') ).toEqual("Class : testClass");
		expect( $$("#TEST h1")[0].get('id') ).toMatch(/testClass/);
		expect( $$("#TEST h2")[0].get('text') ).toEqual("Arguments:");
		expect( $$("#TEST h2")[0].get('id') ).toEqual(null);

	});

	it('should format hyperlinks', function() {

		var s = [
			"/*	First line",
			"   contains a http:www.google.com link",
			"   and some [internal1] and <internal2> links",
			"	End of text.*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST a").length ).toEqual( 3 );
		expect( $$("#TEST a")[0].get('text') ).toMatch(/http:www.google.com/);
		expect( $$("#TEST a")[0].get('href') ).toMatch(/http:www.google.com/);
		expect( $$("#TEST a")[1].get('text') ).toMatch(/internal1/);
		expect( $$("#TEST a")[1].get('href') ).toMatch(/#internal1/);
		expect( $$("#TEST a")[2].get('text') ).toMatch(/internal2/);

	});

	it('should format unordered lists', function() {

		var s = [
			"/*   First line.",
			"	* some list",
			"	* some list",
			"	  continues on next line",
			"	* some list",
			"	This text is not part of the list",
			"	Second line",
			"	- some other list",
			"	- some other list",
			"	End of text.*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST ul").length ).toEqual( 2 );
		expect( $$("#TEST ul > li").length ).toEqual(5);
		expect( $$("#TEST ul li").length ).toEqual(5);
		expect( $$("#TEST ul li")[1].get('text') ).toMatch(/continues/);
		expect( $$("#TEST ul li")[2].get('text') ).toNotMatch(/continues/);
		expect( $$("#TEST ol").length ).toEqual(0);

	});

	it('should format ordered lists', function(){

		var s = [
			"/*   First line.",
			"	# some list",
			"	# some list",
			"	  continues on next line",
			"   End of Text",
			"*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST ol").length ).toEqual( 1 );
		expect( $$("#TEST ol li").length ).toEqual(2);
		expect( $$("#TEST ol li")[1].get('text') ).toMatch(/continues/);
		expect( $$("#TEST ul").length ).toEqual(0);

	});

	it('should format definition lists', function(){

		var s = [
			"/*	First line.",
			"	fruit - Apples, oranges and lemons are fruits",
			"	;vegetables: Tomatoes, carrots etc.",
			"	   and indented text continues on next line",
			"   this is not - a definition list",
			"	End of text.*/"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST dl").length ).toEqual( 1 );
		expect( $$("#TEST dl dd").length ).toEqual(2);
		expect( $$("#TEST dl dt").length ).toEqual(2);
		expect( $$("#TEST dl dt")[0].get('text') ).toMatch(/fruit/);
		expect( $$("#TEST dl dd")[0].get('text') ).toNotMatch(/continues/);
		expect( $$("#TEST dl dt")[1].get('text') ).toMatch(/vegetables/);
		expect( $$("#TEST dl dd")[1].get('text') ).toMatch(/continues/);
		expect( $$("#TEST dl dd")[1].get('text') ).toNotMatch(/End of/);

	});

	it('should preserve preformatted ordered lists', function(){
		var pre = "This  is    *preformatted*      stuff";
		var s = [
			"/*	First line.",
			"   > "+pre,
			"   > "+pre,
			"   > "+pre,
			"",
			"   {{{"+pre+"}}}",
			"",
			"(start code)",
			pre,
			"(end)",
			"	End of text. */"
		].join('\n');
		test.set('html', documoo.formatDoc( 'test', s ) );

		expect( $$("#TEST pre").length ).toEqual( 3 );
		expect( $$("#TEST pre")[0].get('text') ).toEqual( pre+'\n'+pre+'\n'+pre );
		expect( $$("#TEST pre")[1].get('text') ).toEqual( pre );
		expect( $$("#TEST pre")[2].get('text') ).toEqual( pre );

	});

});

