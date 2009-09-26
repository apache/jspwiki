/*
Script: spec-jspwiki-edit.js

License:
	http://www.apache.org/licenses/LICENSE-2.0
*/


/*
Script: snipeditorSpecs.js
	Test following classes: WikiEditor, SnipEditor, Textarea, UndoRedo

License:
	xxx

*/


/*
Native: StubEvent
	Mockup Event object to allow simulation of keyboard and mouse events
	(based on mootools v1.2.3)
*/
var StubEvent = new Native({

	name: 'Event',

	initialize: function(options){
		return $extend(this, {
			event: options.event||{},
			type: options.type,

			page: options.page,
			client: options.client,
			rightClick: options.rightClick,

			wheel: options.wheel,

			relatedTarget: options.relatedTarget,
			target: options.target,

			code: options.code,
			key: options.key,

			shift: options.shift,
			control: options.control,
			alt: options.alt,
			meta: options.meta
		});
	}
});

StubEvent.Keys = Event.Keys;

StubEvent.implement({
	setKey: function(keystroke,shift){
		this.type='keypress';
		this.shift=shift||false,
		this.code=keystroke.charCodeAt(0);
		this.key=keystroke;
		return this;
	},
	stop: function(){ return this; },
	stopPropagation: function(){ return this; },
	preventDefault: function(){ return this; }
});



describe('UndoRedo Class',function(){

	var WORK, UndoElement, RedoElement, UNDOREDO;

	beforeEach( function(){
		WORK = {
			state : 'testing',
			getState: function(){ return this.state; },
			putState: function(obj){ this.state = obj; }
		}

		$$('body')[0].adopt( UndoElement = new Element('a'), RedoElement = new Element('a') );

		UNDOREDO = new UndoRedo(WORK, {redo:RedoElement, undo:UndoElement});

	});

	afterEach( function(){
		WORK = null, UNDOREDO = null;
		UndoElement.dispose();
		RedoElement.dispose();
	});

	it('should initialize', function(){

		expect(UNDOREDO.obj).toEqual(WORK);
		expect(UNDOREDO.options.maxundo).toEqual(40);
		expect(UNDOREDO.redo).toEqual([]);
		expect(UNDOREDO.undo).toEqual([]);
		expect(UNDOREDO.undoEL).toNotEqual(null);
		expect(UNDOREDO.redoEL).toEqual(RedoElement);

		UNDOREDO = new UndoRedo( WORK ,{maxundo:3});

		expect(UNDOREDO.obj).toEqual(WORK);
		expect(UNDOREDO.options.maxundo).toEqual(3);

	});

	it('should undo a single element', function(){

		UNDOREDO.onChange();
		WORK.state = "changed";

		expect(UNDOREDO.undo).toEqual(['testing']);
		expect(WORK.state).toEqual('changed');
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

		UNDOREDO.onUndo();

		expect(WORK.state).toEqual('testing');
		expect(UNDOREDO.undo).toEqual([]);
		expect(UNDOREDO.redo).toEqual(['changed']);
		expect(UndoElement.hasClass('disabled')).toBeTruthy();
		expect(RedoElement.hasClass('disabled')).toBeFalsy();

	});

	it('should undo multiple elements', function(){

		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";

		expect(UNDOREDO.undo).toEqual(['testing','changed1']);
		expect(WORK.state).toEqual('changed2');
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

		UNDOREDO.onUndo();

		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeFalsy();


		UNDOREDO.onUndo();

		expect(WORK.state).toEqual('testing');
		expect(UNDOREDO.undo).toEqual([]);
		expect(UNDOREDO.redo).toEqual(['changed2','changed1']);
		expect(UndoElement.hasClass('disabled')).toBeTruthy();
		expect(RedoElement.hasClass('disabled')).toBeFalsy();
	});

	it('should undo maximum "maxundo" elements', function(){

		UNDOREDO = new UndoRedo( WORK ,{maxundo:3});

		UNDOREDO.onChange();
		WORK.state = "changed1";

		expect(UNDOREDO.undo).toEqual(['testing']);

		UNDOREDO.onChange();
		WORK.state = "changed2";

		expect(UNDOREDO.undo).toEqual(['testing','changed1']);

		UNDOREDO.onChange();
		WORK.state = "changed3";

		expect(UNDOREDO.undo).toEqual(['testing','changed1','changed2']);

		UNDOREDO.onChange();
		WORK.state = "changed4";

		expect(UNDOREDO.undo).toEqual(['changed1','changed2','changed3']);
		expect(WORK.state).toEqual('changed4');

		UNDOREDO.onUndo();
		UNDOREDO.onUndo();
		UNDOREDO.onUndo();
		UNDOREDO.onUndo();

		expect(WORK.state).toEqual('changed1');
		expect(UNDOREDO.undo).toEqual([]);
		expect(UNDOREDO.redo).toEqual(['changed4','changed3','changed2']);
	});

	it('should redo an element', function(){

		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";

		expect(UNDOREDO.undo).toEqual(['testing','changed1']);
		expect(WORK.state).toEqual('changed2');
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

		UNDOREDO.onUndo();

		expect(WORK.state).toEqual('changed1');
		expect(UNDOREDO.undo).toEqual(['testing']);
		expect(UNDOREDO.redo).toEqual(['changed2']);
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeFalsy();

		UNDOREDO.onRedo();

		expect(WORK.state).toEqual('changed2');
		expect(UNDOREDO.undo).toEqual(['testing','changed1']);
		expect(UNDOREDO.redo).toEqual([]);
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

	});

	it('should clear the redo stack when a new undo element is pushed', function(){

		UNDOREDO.onChange();
		WORK.state = "changed1";
		UNDOREDO.onChange();
		WORK.state = "changed2";

		expect(UNDOREDO.undo).toEqual(['testing','changed1']);
		expect(WORK.state).toEqual('changed2');
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

		UNDOREDO.onUndo();

		expect(WORK.state).toEqual('changed1');
		expect(UNDOREDO.undo).toEqual(['testing']);
		expect(UNDOREDO.redo).toEqual(['changed2']);
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeFalsy();

		//no make a new change, this should clear the redo stack
		WORK.state = "changed3";
		UNDOREDO.onChange();

		expect(WORK.state).toEqual('changed3');
		expect(UNDOREDO.undo).toEqual(['testing','changed3']);
		expect(UNDOREDO.redo).toEqual([]);
		expect(UndoElement.hasClass('disabled')).toBeFalsy();
		expect(RedoElement.hasClass('disabled')).toBeTruthy();

	});


});

describe('Textarea Class', function(){

	beforeEach( function() {
		$$('body')[0].adopt( ta = new Element('textarea') );
		ta.value = 'Example text';

		txta = new Textarea( ta );
	});

	afterEach(function(){
		ta.dispose();
	});

	it('should initialize Textarea class', function(){
		expect(txta.getValue()).toEqual('Example text');
	});

	it('should set the caret', function(){
		txta.setSelectionRange(2);
		expect(txta.getSelection()).toEqual('');
		expect(txta.getSelectionRange()).toEqual({start:2,end:2,thin:true})
	});

	it('should set a selection range', function(){
		txta.setSelectionRange(2,4);
		expect(txta.getSelection()).toEqual('am');
		expect(txta.getSelectionRange()).toEqual({start:2,end:4,thin:false})
	});

	it('should set a selection range with a \\n included', function(){
		ta.value = 'Example\ntext';
		txta.setSelectionRange(6,9);
		expect(txta.getSelection()).toEqual('e\nt');
		expect(txta.getSelectionRange()).toEqual({start:6,end:9,thin:false})
	});

	it('should read a fragment from start to caret', function(){
		txta.setSelectionRange(2);
		expect(txta.getFromStart()).toEqual('Ex');
	});

	it('should read a fragment from caret till end', function(){
		txta.setSelectionRange(2,4);
		expect(txta.getTillEnd()).toEqual('ple text');
	});

	it('should replace the selection with another fragment', function(){
		txta.setSelectionRange(2,4);
		txta.setSelection('New Selection ');
		expect(txta.getValue()).toEqual('ExNew Selection ple text');
		expect(txta.getSelection()).toEqual('New Selection ');
		expect(txta.getSelectionRange()).toEqual({start:2,end:16,thin:false})
	});

	it('should replace the selection with another fragment with a \\n', function(){
		txta.setSelectionRange(2,4);
		txta.setSelection('New\nSelection ');
		expect(txta.getValue()).toEqual('ExNew\nSelection ple text');
		expect(txta.getSelection()).toEqual('New\nSelection ');
		expect(txta.getSelectionRange()).toEqual({start:2,end:16,thin:false})
	});

	it('should replace the selection with a \\n with another fragment', function(){
		ta.value = 'Example\ntext';
		txta.setSelectionRange(6,9);
		txta.setSelection('New Selection ');
		expect(txta.getValue()).toEqual('ExamplNew Selection ext');
		expect(txta.getSelection()).toEqual('New Selection ');
		expect(txta.getSelectionRange()).toEqual({start:6,end:20,thin:false})
	});

	it('should insert a fragment at the caret', function(){
		txta.setSelectionRange(2);
		txta.setSelection('New Selection ');
		expect(txta.getValue()).toEqual('ExNew Selection ample text');
		expect(txta.getSelection()).toEqual('New Selection ');
		expect(txta.getSelectionRange()).toEqual({start:2,end:16,thin:false})
	});

	it('should insert a fragment after the selection', function(){
		txta.setSelectionRange(2,4);
		txta.insertAfter('Inserted Stuff ')
		expect(txta.getValue()).toEqual('ExInserted Stuff ple text');
		expect(txta.getSelection()).toEqual('');
		expect(txta.getSelectionRange()).toEqual({start:17,end:17,thin:true})
	});

	it('should insert a set of fragments after the caret', function(){
		txta.setSelectionRange(2);
		txta.insertAfter('Inserted ','Stuff ')
		expect(txta.getValue()).toEqual('ExInserted Stuff ample text');
		expect(txta.getSelectionRange()).toEqual({start:17,end:17,thin:true})
	});

	it('should check if caret is at start of line', function(){
		ta.value = 'Example\ntext';
		txta.setSelectionRange(0);
		expect(txta.isCaretAtStartOfLine()).toBeTruthy();
		txta.setSelectionRange(2);
		expect(txta.isCaretAtStartOfLine()).toBeFalsy();
		txta.setSelectionRange(8);
		expect(txta.isCaretAtStartOfLine()).toBeTruthy();
		txta.setSelectionRange(9);
		expect(txta.isCaretAtStartOfLine()).toBeFalsy();
		txta.setSelectionRange(100);
		expect(txta.isCaretAtStartOfLine()).toBeFalsy();
	});

});

describe('Edit: SnipEditor Class', function(){

	var KEY, TAB, main, next, snipe, txta, ta;

	beforeEach( function(){

		KEY = new StubEvent({'type':'keypress', 'shift':false}).setKey('a');
		TAB = new StubEvent({ 'type':'keydown', 'shift':false, code:9, key:'tab' });

		//initialise body with basic DOM elements to test the snip-editor
		document.body
			.adopt( main = new Element('textarea').set('html','Example text') )
			.adopt( next = new Element('input') );
		main.set('html','Example text');
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


	});

	afterEach(function(){
		main.dispose(), ta.dispose(), next.dispose(), snipe=null, txta=null;
	});

	it('should initialize the SnipEditor class', function(){
		expect(main.value).toEqual('Example text');
		expect(ta.value).toEqual('Example text');
		expect(txta.getValue()).toEqual('Example text');
	});

	//Special keys
	it('shift-enter should jump to next form element', function(){
		next.addEvent('focus',function(){ this.value="has-focus"});
		txta.setSelectionRange(2);

		expect(next.get('value')).toEqual('no-focus');

		var SHIFTENTER = new StubEvent({ 'type':'keydown', 'shift':true, code:13, key:'enter' });

		snipe.onKeystroke( SHIFTENTER );

		//expect(txta.getValue()).toEqual('Example text');
		//expect(txta.getSelectionRange()).toEqual({start:2,end:2,thin:true});
		expect(next.get('value')).toEqual('has-focus');
	});

	//auto indentation on TAB
	it('tab key should insert tab-spaces at the caret', function(){
		txta.setSelectionRange(2);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Ex    ample text');
		expect(txta.getSelectionRange()).toEqual({start:6,end:6,thin:true})
	});

	it('selected text + tab key should replace the selection with tab-spaces', function(){
		txta.setSelectionRange(2,4);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Ex    ple text');
		expect(txta.getSelectionRange()).toEqual({start:6,end:6,thin:true})
	});

	it('multi-line selection + tab key should insert tab-spaces at each line', function(){
		txta.toElement().value="Example\ntext";
		txta.setSelectionRange(0,12);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('    Example\n    text');
		expect(txta.getSelectionRange()).toEqual({start:0,end:20,thin:false})
	});

	it('shift-tab key should remove tab-spaces to the left', function(){
		txta.toElement().value="Ex    ample text";
		txta.setSelectionRange(6);

		TAB.shift=true;
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example text');
		expect(txta.getSelectionRange()).toEqual({start:2,end:2,thin:true})
	});

	it('multi-line selection + shift-tab key should remove the inserted tab-spaces at each line', function(){
		txta.toElement().value="    Example\n    text";
		txta.setSelectionRange(0,20);
		TAB.shift=true;
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example\ntext');
		expect(txta.getSelectionRange()).toEqual({start:0,end:12,thin:false})
	});

	it('del key should remove tab-spaces to the right', function(){
		txta.toElement().value="Ex    ample text";
		txta.setSelectionRange(2);
		var DEL = new StubEvent({ 'type':'keydown', 'shift':false, code:46, key:'delete' });
		snipe.onKeystroke( DEL );

		expect(txta.getValue()).toEqual('Example text');
		expect(txta.getSelectionRange()).toEqual({start:2,end:2,thin:true});
	});


	//direct snippets
	it('Direct snippets, should insert a closing-bracket after the caret', function(){
		txta.setSelectionRange(2);
		snipe.onKeystroke( KEY.setKey('(') );

		expect(txta.getValue()).toEqual('Ex()ample text');
		expect(txta.getSelectionRange()).toEqual({start:3,end:3,thin:true})	;
	});

	it('Direct snippets, should insert a bracket around the selection', function(){
		txta.setSelectionRange(2,4);
		snipe.onKeystroke( KEY.setKey('[') );

		expect(txta.getValue()).toEqual('Ex[am]ple text');
		expect(txta.getSelectionRange()).toEqual({start:3,end:5,thin:false});
	});

	it('should not expand direct snippets when deactivated', function(){
		snipe.options.directsnips = {};
		txta.setSelectionRange(2);

		snipe.onKeystroke( KEY.setKey('a'));

		//Warning: default key-stroke behaviour is not correct during Spec tests
		// because the way it is simulated.
		//  Normally we would expect 'Ex[ample text'
		expect(txta.getValue()).toEqual('Example text');
		expect(txta.getSelectionRange()).toEqual({start:2,end:2,thin:true});
	});

	//Tab snippet, without parameters
	it('Tab snippet - no parameters: should remove the leading \\n at the start of the first line of the textarea', function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="tocExample text";
		txta.setSelectionRange(3);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('[{TableOfContents }]\nExample text');
		expect(txta.getSelectionRange()).toEqual({start:21,end:21,thin:true});

	});
	it('Tab snippet - no parameters: should remove the leading \\n at the start of the nth line the textarea', function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example\ntoc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example\n[{TableOfContents }]\n text');
		expect(txta.getSelectionRange()).toEqual({start:29,end:29,thin:true})
	});
	it('Tab snippet - no parameters: should keep a leading \\n in the middle of a line of the textarea', function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example \n[{TableOfContents }]\n text');
		expect(txta.getSelectionRange()).toEqual({start:30,end:30,thin:true})
	});

	it('Tab snippet: should only allow a tab snippet when in scope', function(){
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

		expect(txta.getValue()).toEqual("Example <p>\n[{TableOfContents }]\n</p> text");
		expect(txta.getSelectionRange()).toEqual({start:33,end:33,thin:true})

		//not in scope -- so just expands the tab to 4 spaces
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual("Example toc     text");
		expect(txta.getSelectionRange()).toEqual({start:15,end:15,thin:true})

	});

	it('Tab snippet: should not expand tab snippets when deactivated', function(){
		snipe.options.tabsnips = {}
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example toc     text');
		expect(txta.getSelectionRange()).toEqual({start:15,end:15,thin:true})
	});

	it('Tab snippet: should allow to undo the inserted tab snippet', function(){
		snipe.options.tabsnips["toc"] = "\n[\\{TableOfContents }]\n";
		txta.toElement().value="Example toc text";
		txta.setSelectionRange(11);
		snipe.onKeystroke( TAB );

		expect(txta.getValue()).toEqual('Example \n[{TableOfContents }]\n text');
		expect(txta.getSelectionRange()).toEqual({start:30,end:30,thin:true});

		snipe.undoredo.onUndo();

		expect(txta.getValue()).toEqual('Example toc text');
		expect(txta.getSelectionRange()).toEqual({start:11,end:11,thin:true});

		snipe.undoredo.onRedo();

		expect(txta.getValue()).toEqual('Example \n[{TableOfContents }]\n text');
		expect(txta.getSelectionRange()).toEqual({start:30,end:30,thin:true});
	});

	//Tab snippet, with a parameter
	it('Tab snippet with parameter: should select the first parameter', function(){

	});

	it('Tab snippet with parameter: should allow to tab through all subsequent parameters', function(){

	});

	it('Tab snippet with parameter: should remove the active snippet when pressing esc/up/down', function(){

	});

	it('Tab snippet with parameter and parameter dialog: should display the parameter dialog', function(){

	});

	it('Tab snippet with font parameter: should display the font dialog', function(){

	});

	it('Tab snippet with color parameter: should display the color dialog', function(){

	});

	it('Tab snippet with special-charater parameter: should display the chars dialog', function(){

	});

	//Button snippets
	it('Button snippet, should insert snippet text without parameters', function(){
	});

	it('Button snippet, should replace selected text by snippet without parameters', function(){
	});

	it('Button snippet, should insert snippet and tab through parameters without parameter dialog', function(){
	});

	it('Button snippet, should insert snippet and select first parameter with parameter dialog', function(){
	});

	it('Button snippet, should insert snippet and replace first free parameter with seleted text', function(){
		//font snippet
	});


	//find and replace
	it('should replace all occurences of a string', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');
	});

	it('should undo a replace operation', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');

		snipe.undoredo.onUndo();

		expect(txta.getValue()).toEqual('Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.');

		snipe.undoredo.onRedo();

		expect(txta.getValue()).toEqual('Lorem IPSUM dolor sit amet. Lorem IPSUM dolor sit amet.');

	});

	it('should return no-match found when replacing a string', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='cant find this string';
		snipe.options.findForm.replaceInput.value='oops';
		var error = '';
		snipe.options.findForm.msgNoMatchFound=function(){ error = 'no match';}

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.');
		expect(error).toEqual('no match');
	});

	it('should replace all occurences of a string case sensitive', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet; lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='Lorem';
		snipe.options.findForm.replaceInput.value='LOREM'
		snipe.options.findForm.isMatchCase.checked=true;

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('LOREM ipsum dolor sit amet; lorem ipsum dolor sit amet.');
	});

	it('should replace all occurences of a string with a regular expression', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. LOREM iPSUm dolor sit amet.';
		snipe.options.findForm.findInput.value='i([psu]+)m';
		snipe.options.findForm.replaceInput.value='I$1M'
		snipe.options.findForm.isRegExp.checked=true;

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem IpsuM dolor sit amet. LOREM IPSUM dolor sit amet.');

		//double $ in replace string acts as a single $
		snipe.options.findForm.findInput.value='L([orem]+)m';
		snipe.options.findForm.replaceInput.value='LL$$$1MM'

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('LL$oreMM IpsuM dolor sit amet. LL$OREMM IPSUM dolor sit amet.');
	});

	it('should replace the first occurences of a string', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		snipe.options.findForm.findInput.value='ipsum';
		snipe.options.findForm.replaceInput.value='IPSUM'
		snipe.options.findForm.isReplaceGlobal.checked=false;

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem IPSUM dolor sit amet. Lorem ipsum dolor sit amet.');
	});

	it('should replace all occurences of a string in a textarea selection', function(){

		txta.toElement().value='Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet.';
		txta.setSelectionRange(6,27)
		snipe.options.findForm.findInput.value='i';
		snipe.options.findForm.replaceInput.value='III'

		expect(txta.getSelection()).toEqual('ipsum dolor sit amet.');

		snipe.onFindAndReplace();

		expect(txta.getValue()).toEqual('Lorem IIIpsum dolor sIIIt amet. Lorem ipsum dolor sit amet.');
		expect(txta.getSelection()).toEqual('IIIpsum dolor sIIIt amet.');
	});


});

//todo
describe('Edit: Wiki Editor Class', function(){

	//test configuration dialog

	//setting/reseting of smartpair and tabcompletion preferences

	//test cookie get/set

});



