/*
Script: jspwiki-editSpecs.js

License:
	xxx

*/


describe('JSPWiki-commonstyles: ...',{
});



describe('JSPWiki-prefs',{
});




/*
Script: snipeditorSpecs.js
	Test following classes: WikiEditor, SnipEditor, Textarea, UndoRedo

License:
	xxx

*/

describe('Edit: UndoRedo Class',{

	before_each: function(){
		WORK = {
			state : 'testing',
			getState: function(){ return this.state; },
			putState: function(obj){ this.state = obj; }
		}

		$$('body')[0].adopt( UndoElement = new Element('a'), RedoElement = new Element('a') );
		
		UNDOREDO = new UndoRedo(WORK, {redo:RedoElement, undo:UndoElement});

	},

	after_each: function(){
		WORK = null, UNDOREDO = null;
		UndoElement.remove();
		RedoElement.remove();
	},

	'should initialize': function(){

		value_of(UNDOREDO.obj).should_be(WORK);				
		value_of(UNDOREDO.options.maxundo).should_be(40);
		value_of(UNDOREDO.redo).should_be_empty();
		value_of(UNDOREDO.undo).should_be_empty();
		value_of(UNDOREDO.undoEL).should_not_be(null);
		value_of(UNDOREDO.redoEL).should_be(RedoElement);

		UNDOREDO = new UndoRedo( WORK ,{maxundo:3});

		value_of(UNDOREDO.obj).should_be(WORK);				
		value_of(UNDOREDO.options.maxundo).should_be(3);
		
	},
	
	'should undo a single element': function(){

		UNDOREDO.onChange();
		WORK.state = "changed";
		
		value_of(UNDOREDO.undo).should_be(['testing']);		
		value_of(WORK.state).should_be(['changed']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();
		
		UNDOREDO.onUndo();

		value_of(WORK.state).should_be(['testing']);
		value_of(UNDOREDO.undo).should_be_empty();
		value_of(UNDOREDO.redo).should_be(['changed']);
		value_of(UndoElement.hasClass('disabled')).should_be_true();
		value_of(RedoElement.hasClass('disabled')).should_be_false();
		
	},
	
	'should undo multiple elements': function(){

		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";
		
		value_of(UNDOREDO.undo).should_be(['testing','changed1']);		
		value_of(WORK.state).should_be(['changed2']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();
		
		UNDOREDO.onUndo();

		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_false();


		UNDOREDO.onUndo();

		value_of(WORK.state).should_be(['testing']);
		value_of(UNDOREDO.undo).should_be_empty();
		value_of(UNDOREDO.redo).should_be(['changed2','changed1']);
		value_of(UndoElement.hasClass('disabled')).should_be_true();
		value_of(RedoElement.hasClass('disabled')).should_be_false();
	},
	
	'should undo maximum "maxundo" elements': function(){

		UNDOREDO = new UndoRedo( WORK ,{maxundo:3});

		UNDOREDO.onChange();
		WORK.state = "changed1";

		value_of(UNDOREDO.undo).should_be(['testing']);		

		UNDOREDO.onChange();
		WORK.state = "changed2";

		value_of(UNDOREDO.undo).should_be(['testing','changed1']);		

		UNDOREDO.onChange();
		WORK.state = "changed3";

		value_of(UNDOREDO.undo).should_be(['testing','changed1','changed2']);		

		UNDOREDO.onChange();
		WORK.state = "changed4";
		
		value_of(UNDOREDO.undo).should_be(['changed1','changed2','changed3']);		
		value_of(WORK.state).should_be(['changed4']);
		
		UNDOREDO.onUndo();
		UNDOREDO.onUndo();
		UNDOREDO.onUndo();
		UNDOREDO.onUndo();

		value_of(WORK.state).should_be(['changed1']);
		value_of(UNDOREDO.undo).should_be_empty();
		value_of(UNDOREDO.redo).should_be(['changed4','changed3','changed2']);
	},
	
	'should redo an element': function(){
	
		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";
		
		value_of(UNDOREDO.undo).should_be(['testing','changed1']);		
		value_of(WORK.state).should_be(['changed2']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();
		
		UNDOREDO.onUndo();

		value_of(WORK.state).should_be(['changed1']);
		value_of(UNDOREDO.undo).should_be(['testing']);		
		value_of(UNDOREDO.redo).should_be(['changed2']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_false();

		UNDOREDO.onRedo();

		value_of(WORK.state).should_be(['changed2']);
		value_of(UNDOREDO.undo).should_be(['testing','changed1']);		
		value_of(UNDOREDO.redo).should_be_empty();
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();

	},
	
	'should clear the redo stack when a new undo element is pushed': function(){
	
		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";
		
		value_of(UNDOREDO.undo).should_be(['testing','changed1']);		
		value_of(WORK.state).should_be(['changed2']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();
		
		UNDOREDO.onUndo();

		value_of(WORK.state).should_be(['changed1']);
		value_of(UNDOREDO.undo).should_be(['testing']);		
		value_of(UNDOREDO.redo).should_be(['changed2']);
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_false();

		//no make a new change, this should clear the redo stack
		WORK.state = "changed3";
		UNDOREDO.onChange();

		value_of(WORK.state).should_be(['changed3']);
		value_of(UNDOREDO.undo).should_be(['testing','changed3']);		
		value_of(UNDOREDO.redo).should_be_empty();
		value_of(UndoElement.hasClass('disabled')).should_be_false();
		value_of(RedoElement.hasClass('disabled')).should_be_true();

	}


});

describe('Edit: Textarea Class', {

	before_each : function() {
		$$('body')[0].adopt( ta = new Element('textarea') );
		ta.value = 'Example text';			
		
		txta = new Textarea( ta );
	},
	after_each: function(){
		ta.remove();
	},
	
	'should initialize Textarea class': function(){			
		value_of(txta.getValue()).should_be('Example text');
	},
	
	'should set the caret':function(){
		txta.setSelectionRange(2);
		value_of(txta.getSelection()).should_be('');
		value_of(txta.getSelectionRange()).should_be({start:2,end:2,thin:true})
	},

	'should set a selection range':function(){
		txta.setSelectionRange(2,4); 
		value_of(txta.getSelection()).should_be('am');
		value_of(txta.getSelectionRange()).should_be({start:2,end:4,thin:false})
	},

	'should set a selection range with a \\n included':function(){
		ta.value = 'Example\ntext';
		txta.setSelectionRange(6,9); 
		value_of(txta.getSelection()).should_be('e\nt');
		value_of(txta.getSelectionRange()).should_be({start:6,end:9,thin:false})
	},

	'should read a fragment from start to caret':function(){
		txta.setSelectionRange(2);
		value_of(txta.getFromStart()).should_be('Ex');
	},

	'should read a fragment from caret till end':function(){
		txta.setSelectionRange(2,4);
		value_of(txta.getTillEnd()).should_be('ple text');
	},
	
	'should replace the selection with another fragment':function(){
		txta.setSelectionRange(2,4);
		txta.setSelection('New Selection ');
		value_of(txta.getValue()).should_be('ExNew Selection ple text');
		value_of(txta.getSelection()).should_be('New Selection ');
		value_of(txta.getSelectionRange()).should_be({start:2,end:16,thin:false})			
	},
	
	'should replace the selection with another fragment with a \\n':function(){
		txta.setSelectionRange(2,4);
		txta.setSelection('New\nSelection ');
		value_of(txta.getValue()).should_be('ExNew\nSelection ple text');
		value_of(txta.getSelection()).should_be('New\nSelection ');
		value_of(txta.getSelectionRange()).should_be({start:2,end:16,thin:false})			
	},
	
	'should replace the selection with a \\n with another fragment':function(){
		ta.value = 'Example\ntext';			
		txta.setSelectionRange(6,9); 
		txta.setSelection('New Selection ');
		value_of(txta.getValue()).should_be('ExamplNew Selection ext');
		value_of(txta.getSelection()).should_be('New Selection ');
		value_of(txta.getSelectionRange()).should_be({start:6,end:20,thin:false})
	},
	
	'should insert a fragment at the caret':function(){
		txta.setSelectionRange(2);
		txta.setSelection('New Selection ');
		value_of(txta.getValue()).should_be('ExNew Selection ample text');
		value_of(txta.getSelection()).should_be('New Selection ');
		value_of(txta.getSelectionRange()).should_be({start:2,end:16,thin:false})			
	},
	
	'should insert a fragment after the selection':function(){
		txta.setSelectionRange(2,4);
		txta.insertAfter('Inserted Stuff ')
		value_of(txta.getValue()).should_be('ExInserted Stuff ple text');
		value_of(txta.getSelection()).should_be('');
		value_of(txta.getSelectionRange()).should_be({start:17,end:17,thin:true})			
	},
	
	'should insert a set of fragments after the caret':function(){
		txta.setSelectionRange(2);
		txta.insertAfter('Inserted ','Stuff ')
		value_of(txta.getValue()).should_be('ExInserted Stuff ample text');
		value_of(txta.getSelectionRange()).should_be({start:17,end:17,thin:true})			
	},
	
	'should check if caret is at start of line':function(){
		ta.value = 'Example\ntext';			
		txta.setSelectionRange(0);
		value_of(txta.isCaretAtStartOfLine()).should_be_true();
		txta.setSelectionRange(2);
		value_of(txta.isCaretAtStartOfLine()).should_be_false();
		txta.setSelectionRange(8);
		value_of(txta.isCaretAtStartOfLine()).should_be_true();
		txta.setSelectionRange(9);
		value_of(txta.isCaretAtStartOfLine()).should_be_false();
		txta.setSelectionRange(100);
		value_of(txta.isCaretAtStartOfLine()).should_be_false();
	}

});

describe('Edit: SnipEditor Class', {

	before_each : function() {

		/* Mockup Event object to allow simulation of keyboard and mouse events*/
		Event.implement({ 
			initialize: function(e){ 
				for(var ee in e ){ this[ee] = e[ee]; }
				this.event=''; //used by snipeditor to check the event.which flag
				return this; 
			},
			stop: function(){ }
		});

		KEY = new Event({ 'type':'keypress', 'shift':false, code:'a'.charCodeAt(0), key:'a' });
		TAB = new Event({ 'type':'keydown', 'shift':false, code:9, key:'tab' });

		//initialise body with basic DOM elements to test the snip-editor
		document.body
			.adopt( main = new Element('textarea').setHTML('Example text') )
			.adopt( next = new Element('input') );
		main.setHTML('Example text');
		next.value = "no-focus";
		
		snipe = new SnipEditor( main, { 
			tabsnips: WikiEdit.tabSnippets, 
			directsnips: WikiEdit.directSnippets, 
			//buttons: $$('a.tool'),
			next:next, 
			//suggest:$('suggestionMenu') 
	  	});

	  	txta = snipe.get('textarea');
	  	ta = txta.toElement();	  	
	  	
		snipe.options.findForm = {
			findInput: {value: ''},	
			replaceInput: {value: ''},
			isRegExp: {checked:false},
			isMatchCase: {checked:false},
			isReplaceGlobal: {checked:true},
			msgNoMatchFound: function(){ }
		};


	},

	after_each: function(){
		main.remove(), ta.remove(), next.remove(), snipe=null, txta=null;
	},
	
	'should initialize the SnipEditor class': function(){
		value_of(main.value).should_be('Example text');
		value_of(ta.value).should_be('Example text');
		value_of(txta.getValue()).should_be('Example text');
	},
	
	//Special keys
	'shift-enter should jump to next form element': function(){
		next.addEvent('focus',function(){ this.value="has-focus"});
		txta.setSelectionRange(2);

		value_of(next.getValue()).should_be('no-focus');

		var SHIFTENTER = new Event({ 'type':'keydown', 'shift':true, code:13, key:'enter' });
		snipe.onKeystroke( SHIFTENTER );

		//value_of(txta.getValue()).should_be('Example text');
		//value_of(txta.getSelectionRange()).should_be({start:2,end:2,thin:true});
		value_of(next.getValue()).should_be('has-focus');
	},

	//auto indentation on TAB
	'tab key should insert tab-spaces at the caret': function(){
		txta.setSelectionRange(2);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Ex    ample text');
		value_of(txta.getSelectionRange()).should_be({start:6,end:6,thin:true})
	},

	'selected text + tab key should replace the selection with tab-spaces': function(){
		txta.setSelectionRange(2,4);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Ex    ple text');
		value_of(txta.getSelectionRange()).should_be({start:6,end:6,thin:true})
	},

	'multi-line selection + tab key should insert tab-spaces at each line': function(){
		txta.toElement().value="Example\ntext";
		txta.setSelectionRange(0,12);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('    Example\n    text');
		value_of(txta.getSelectionRange()).should_be({start:0,end:20,thin:false})		
	},

	'shift-tab key should remove tab-spaces to the left': function(){
		txta.toElement().value="Ex    ample text";
		txta.setSelectionRange(6);
		snipe.onKeystroke( $merge(TAB,{shift:true}) );

		value_of(txta.getValue()).should_be('Example text');
		value_of(txta.getSelectionRange()).should_be({start:2,end:2,thin:true})		
	},
	
	'multi-line selection + shift-tab key should remove the inserted tab-spaces at each line': function(){
		txta.toElement().value="    Example\n    text";
		txta.setSelectionRange(0,20);
		snipe.onKeystroke( $merge(TAB,{shift:true}) );

		value_of(txta.getValue()).should_be('Example\ntext');
		value_of(txta.getSelectionRange()).should_be({start:0,end:12,thin:false})		
	},

	'del key should remove tab-spaces to the right': function(){
		txta.toElement().value="Ex    ample text";
		txta.setSelectionRange(2);
		var DEL = new Event({ 'type':'keydown', 'shift':false, code:46, key:'delete' });
		snipe.onKeystroke( DEL );

		value_of(txta.getValue()).should_be('Example text');
		value_of(txta.getSelectionRange()).should_be({start:2,end:2,thin:true});		
	},


	//direct snippets
	'Direct snippets, should insert a closing-bracket after the caret': function(){
		txta.setSelectionRange(2);
		snipe.onKeystroke( $merge(KEY, {code:'('.charCodeAt(0), key:'('}) );

		value_of(txta.getValue()).should_be('Ex()ample text');
		value_of(txta.getSelectionRange()).should_be({start:3,end:3,thin:true})	;		
	},
	
	'Direct snippets, should insert a bracket around the selection': function(){
		txta.setSelectionRange(2,4);
		snipe.onKeystroke( $merge(KEY, {code:'['.charCodeAt(0), key:'['}) );
 
		value_of(txta.getValue()).should_be('Ex[am]ple text');
		value_of(txta.getSelectionRange()).should_be({start:3,end:5,thin:false});
	},

	'should not expand direct snippets when deactivated': function(){
		snipe.options.directsnips = {};
		txta.setSelectionRange(2);

		snipe.onKeystroke( $merge(KEY, {code:'a'.charCodeAt(0), key:'a'}) );

		//Warning: default key-stroke behaviour is not correct during Spec tests
		// because the way it is simulated.
		//  Normally we would expect 'Ex[ample text'
		value_of(txta.getValue()).should_be('Example text');
		value_of(txta.getSelectionRange()).should_be({start:2,end:2,thin:true});
	},

	//Tab snippet, without parameters 
	'Tab snippet - no parameters: should remove the leading \\n at the start of the first line of the textarea': function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="tocExample text";
		txta.setSelectionRange(3);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('[{TableOfContents }]\nExample text');
		value_of(txta.getSelectionRange()).should_be({start:21,end:21,thin:true});

	},
	'Tab snippet - no parameters: should remove the leading \\n at the start of the nth line the textarea': function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example\ntoc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Example\n[{TableOfContents }]\n text');
		value_of(txta.getSelectionRange()).should_be({start:29,end:29,thin:true})
	},
	'Tab snippet - no parameters: should keep a leading \\n in the middle of a line of the textarea': function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Example \n[{TableOfContents }]\n text');
		value_of(txta.getSelectionRange()).should_be({start:30,end:30,thin:true})
	},

	'Tab snippet: should only allow a tab snippet when in scope': function(){
		snipe.options.tabsnips["toc"] = {
			snippet:"\n[\\{TableOfContents }]\n",
			scope:{
				"test": "endtest",
				"<p": "</p>"
			}
		}

		//in scope
		txta.toElement().value="Example <p>toc</p> text";
		txta.setSelectionRange(14);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be("Example <p>\n[{TableOfContents }]\n</p> text");
		value_of(txta.getSelectionRange()).should_be({start:33,end:33,thin:true})

		//not in scope -- so just expands the tab to 4 spaces
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be("Example toc     text");
		value_of(txta.getSelectionRange()).should_be({start:15,end:15,thin:true})

	},

	'Tab snippet: should not expand tab snippets when deactivated': function(){
		snipe.options.tabsnips = {}
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Example toc     text');
		value_of(txta.getSelectionRange()).should_be({start:15,end:15,thin:true})
	},

	'Tab snippet: should allow to undo the inserted tab snippet': function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		value_of(txta.getValue()).should_be('Example \n[{TableOfContents }]\n text');
		value_of(txta.getSelectionRange()).should_be({start:30,end:30,thin:true});

		snipe.undoredo.onUndo();
		
		value_of(txta.getValue()).should_be('Example toc text');
		value_of(txta.getSelectionRange()).should_be({start:11,end:11,thin:true});

		snipe.undoredo.onRedo();
		
		value_of(txta.getValue()).should_be('Example \n[{TableOfContents }]\n text');
		value_of(txta.getSelectionRange()).should_be({start:30,end:30,thin:true});
	},

	//Tab snippet, with a parameter 
	'Tab snippet with parameter: should select the first parameter': function(){
		
	},

	'Tab snippet with parameter: should allow to tab through all subsequent parameters': function(){
		
	},

	'Tab snippet with parameter: should remove the active snippet when pressing esc/up/down': function(){
		
	},

	'Tab snippet with parameter and parameter dialog: should display the parameter dialog': function(){

	},

	'Tab snippet with font parameter: should display the font dialog': function(){

	},

	'Tab snippet with color parameter: should display the color dialog': function(){

	},

	'Tab snippet with special-charater parameter: should display the chars dialog': function(){

	},

	//Button snippets
	'Button snippet, should insert snippet text without parameters': function(){
	},

	'Button snippet, should replace selected text by snippet without parameters': function(){
	},

	'Button snippet, should insert snippet and tab through parameters without parameter dialog': function(){
	},

	'Button snippet, should insert snippet and select first parameter with parameter dialog': function(){
	},
	
	'Button snippet, should insert snippet and replace first free parameter with seleted text': function(){
		//font snippet
	},
	

	//find and replace 
	'should replace all occurences of a string':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');
	},

	'should undo a replace operation':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');

		snipe.undoredo.onUndo();
		
		value_of(txta.getValue()).should_be('Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.');
		
		snipe.undoredo.onRedo();

		value_of(txta.getValue()).should_be('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');

	},

	'should return no-match found when replacing a string':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='cant find this string';
		snipe.options.findForm.replaceInput.value='oops';
		var error = '';
		snipe.options.findForm.msgNoMatchFound=function(){ error = 'no match';}

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.');
		value_of(error).should_be('no match');
	},

	'should replace all occurences of a string case sensitive':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet; lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='Lorem';
		snipe.options.findForm.replaceInput.value='LOREM'
		snipe.options.findForm.isMatchCase.checked=true;

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('LOREM ipsum dolor sit amet; lorem ipsum dolor sit amet.');
	},
	
	'should replace all occurences of a string with a regular expression':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. LOREM iPSUm dolor sit amet.';
		snipe.options.findForm.findInput.value='i([psu]+)m';
		snipe.options.findForm.replaceInput.value='I$1M'
		snipe.options.findForm.isRegExp.checked=true;

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem IpsuM dolor sit amet. LOREM IPSUM dolor sit amet.');

		//double $ in replace string acts as a single $
		snipe.options.findForm.findInput.value='L([orem]+)m';
		snipe.options.findForm.replaceInput.value='LL$$$1MM'

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('LL$oreMM IpsuM dolor sit amet. LL$OREMM IPSUM dolor sit amet.');
	},
	
	'should replace the first occurences of a string':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'
		snipe.options.findForm.isReplaceGlobal.checked=false;

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem IPSUM dolor sit amet. Lorem ipsum dolor sit amet.');
	},
	
	'should replace all occurences of a string in a textarea selection':function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		txta.setSelectionRange(6,27)
		snipe.options.findForm.findInput.value='i';
		snipe.options.findForm.replaceInput.value='III'

		value_of(txta.getSelection()).should_be('ipsum dolor sit amet.');

		snipe.onFindAndReplace();

		value_of(txta.getValue()).should_be('Lorem IIIpsum dolor sIIIt amet. Lorem ipsum dolor sit amet.');
		value_of(txta.getSelection()).should_be('IIIpsum dolor sIIIt amet.');
	}


});

//todo
describe('Edit: Wiki Editor Class', {

	//test configuration dialog
	
	//setting/reseting of smartpair and tabcompletion preferences
	
	//test cookie get/set 

});



