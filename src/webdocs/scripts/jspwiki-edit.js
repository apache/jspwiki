/**
 ** jspwiki edit support routines
 ** Based on brushed template
 ** Needs jspwiki-common.js and mootools.js
 **
 ** EditTools object (main object)
 ** - find&replace functionality : with regexp support
 **
 ** - included popup-pagelinks routine from Janne // 
 **
 ** TextArea object
 **     Supports selections inside textarea, in ie and other browsers
 **/

var EditTools = 
{
	onPageLoad: function(){
		this.textarea = $('editorarea'); 
		if(!this.textarea || !this.textarea.visible) return;
		
		/* make textarea more intelligent */
		this.wikisnippets = this.getWikiSnippets();
		this.wikismartpairs = this.getWikiSmartPairs();

		if(!window.ie) {
		this.posteditor = new postEditor.create(this.textarea,'changenote');
		
		/* patch posteditor DF Jul 07 */
		/* righ-arrow nok on FF, nop on Safari */
		this.posteditor.onKeyRight = Class.empty; 				
		/* make posteditor changes undoable */
		this.posteditor.value = function(value) {
			EditTools.storeTextarea();
			this.element.value = value.join("");
		};

		['smartpairs', 'tabcompletion'].each( function(el){
			$(el).setProperty('checked', Wiki.prefs.get(el) || false)
				 .addEvent('click',function(e) {
					Wiki.prefs.set(el,this.checked);
					EditTools.initPostEditor();
				 });
		},this);
				
		this.initPostEditor();
		}
		

		/* activate editassist toolbar */
		var toolbar = $('toolbar');
		var fxToolbar = new Fx.Slide(toolbar,{
			onStart:function(){
				$('editassist').toggleClass('closed');
			}
		});
		var e = $('editassist').addEvent('click', function(e){
			e = new Event(e);
			fxToolbar.toggle();
			e.stop();
		}).getParent().show();


		//FIXME: stop-event not yet working properly on eg UNDO
		$('replace').addEvent('click', function(e) { EditTools.doReplace(); new Event(e).stop(); });
		$('tbREDO').addEvent('click', function(e) { EditTools.redoTextarea(); new Event(e).stop(); });
		$('tbUNDO').addEvent('click', function(e) { new Event(e).stop(); EditTools.undoTextarea();  })
			.getParent().getParent().show();

		toolbar.getElements('a.tool').each(function(el){
			el.addEvent('click', this.insertTextArea.pass(el,this));
		},this);

		/* add textarea resize drag bar */
		var hh=Wiki.prefs.get('EditorSize');
		if(hh) this.textarea.setStyle('height',hh);
		var h = new Element('div',{'class':'textarea-resizer', 'title':'edit.resize'.localize()})
			.injectAfter(this.textarea);	
		this.textarea.makeResizable({
			handle:h, 
			modifiers: {x:false, y:'height'}, 
			onComplete: function(){	Wiki.prefs.set('EditorSize',this.value.now.y); }
		});			
		
	},

	initPostEditor: function(){
		if(! this.posteditor) return;
		this.posteditor.changeSmartTypingPairs( $('smartpairs').checked ? this.wikismartpairs : {} );
		this.posteditor.changeSnippets( $('tabcompletion').checked ? this.wikisnippets : {} );	
	},

	getWikiSnippets: function(){
		return {
	"toc" : {
		snippet:["","[{TableOfContents }]", "\n"],
		tab:['[{TableOfContents }]', '']
	},
	"link" : {
		snippet:["[","link text|pagename", "]"],
		tab:['link text','pagename','']
	},
	"code" : {
		snippet:["%%prettify \n{{{\n","some code block", "\n}}}\n/%\n"],
		tab:['some code block','']
	},
	"pre" : {
		snippet:["{{{\n","some preformatted block", "\n}}}\n"],
		tab:['some preformatted block','']
	},
	"br" : {
		snippet:['\\\\\n','',''],
		tab:['']
	},
	"bold" : {
		snippet:["__","some bold text", "__"],
		tab:['some bold text','']
	},
	"italic" : {
		snippet:["''","some italic text", "''"],
		tab:['some italic text','']
	},
	"h1" : {
		snippet:["!!! ","Heading 1 title", "\n"],
		tab:["Heading 1 title", ""]
	},
	"h2" : {
		snippet:["!! ","Heading 2 title", "\n"],
		tab:["Heading 2 title", ""]
	},
	"h3" : {
		snippet:["! ","Heading 3 title", "\n"],
		tab:["Heading 3 title", ""]
	},
	"dl" : {
		snippet:["\n",";term:definition text", "\n"],
		tab:["term","definition text", ""]
	},
	"mono" : {
		snippet:["{{","some monospaced text", "}}"],
		tab:["some monospaced text", ""]
	},
	"hr" : {
		snippet:['----\n','',''],
		tab:['']
	},
	"sub" : {
		snippet:["%%sub ","subscript text", "/%"],
		tab:['subscript text','']
	},
	"sup" : {
		snippet:["%%sup ","superscript text", "/%"],
		tab:['superscript text','']
	},
	"strike" : {
		snippet:["%%strike ","strikethrough text", "/%"],
		tab:['strikethrough text','']
	},
	"tab" : {
		snippet:["%%tabbedSection \n","%%tab-tabTitle1\ntab content 1\n/%\n%%tab-tabTitle2\ntab content 2", "\n/%\n/%\n"],
		tab:['tabTitle1','tab content 1','tabTitle2','tab content 2','']
	},
	"table" : {
		snippet:["\n","||heading 1||heading 2\n| cell 1   | cell 2", "\n"],
		tab:['heading 1','heading 2','cell 1','cell 2','']
	},
	"img" : {
		snippet:["","[{Image src='img.jpg' width='..' height='..' align='left|center|right' style='..' class='..' }]", "\n"],
		tab:['img.jpg', '']
	},
	"quote" : {
		snippet:["%%quote \n","quoted text", "\n/%\n"],
		tab:['quoted text','']
	},
	"%%" : {
		snippet:["%%","wikistyle\nsome text", "\n/%"],
		tab:['wikistyle','some text','']
	},
	//dynamic snippets
	"sign" : {
		snippet:["\\\\\n--",Wiki.UserName+", "+"25 Sep 07","\n"],
		tab:[Wiki.UserName,'25 Sep 07','']
	},
	/* TODO: how to insert the proper current date/timestamp, inline with the preferred time format */
	"date" : {
		//return new object snippet
		command: function(k) {
			var dayNames = ["Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"],
			monthNames = ["January","February","March","April","May","June","July","August","September","October","November","December"],
			dt = new Date(),
			y  = dt.getYear();
			if (y < 1000) y +=1900;
			var date = dayNames[dt.getDay()] + ", " + monthNames[dt.getMonth()] + " " + dt.getDate() + ", " + y;
			return {
				//key:"date", optional
				snippet:['',date,' '],
				tab:[date,'']
			};
		}
	}
} /* return */

	},
	getWikiSmartPairs: function(){
		return {
		'"' : '"',
		'(' : ')',
		'{' : '}',
		'[' : ']',
		'<' : '>',
		"'" : { scope:{ "{{{":"}}}" }, pair:"'" }
		}
	},
	
	insertTextArea: function(el) {
		var snippy = this.wikisnippets[el.getText()]; if(!snippy) return

		var s = TextArea.getSelection(this.textarea),
			t = snippy.snippet.join('');
		EditTools.storeTextarea();

		if((el.rel=='break') && (!TextArea.isSelectionAtStartOfLine(this.textarea))) { 
			t = "\n" + t;
		}
		if(s) t = t.replace( snippy.tab[0], s)
		TextArea.setSelection(this.textarea, t);
	} ,

	/* TOOLBAR: find&replace */
	doReplace: function( ){
		var findText	= $('tbFIND').value, 
			isRegExp	= $('tbREGEXP').checked,
			reGlobal	= $('tbGLOBAL').checked ? 'g' : '',
			replaceText	= $('tbREPLACE').value,
			reMatchCase	= $('tbMatchCASE').checked ? '' : 'i';

		if( findText == "") return;

		var sel = TextArea.getSelection(this.textarea);
		var data = ( !sel || (sel=="") ) ? this.textarea.value : sel;
	
		if(!isRegExp){ /* escape all special re characters */
			var re = new RegExp( "([\.\*\\\?\+\[\^\$])", "gi");
			findText = findText.replace( re,"\\$1" );
		}
		
		var re = new RegExp(findText, reGlobal+reMatchCase+"m" ); //multiline
		if(!re.exec(data)){
			alert( "edit.findandreplace.nomatch".localize() );
			return true;
		}
		
		data = data.replace(re, replaceText);  
	
		this.storeTextarea();
		if(!sel || (sel=="")){
			this.textarea.value = data;
		} else {
			TextArea.setSelection( this.textarea, data );
		}
		if(this.textarea.onchange) this.textarea.onchange();
	} ,
			
	/* TOOLBAR: cut/copy/paste clipboard functionality */
	CLIPBOARD : null,
	clipboard : function( format ){
		var s = TextArea.getSelection(this.textarea);
		if( !s || s == "") return;

		this.CLIPBOARD = s ;
		$( 'tbPASTE' ).className = this.ToolbarMarker;
		var ss = format.replace( /\$/, s);
		if( s == ss ) return; //copy

		this.storeTextarea(); //cut
		TextArea.setSelection( this.textarea, ss );
	} ,

	paste : function()
	{
		if( !this.CLIPBOARD ) return;
		this.storeTextarea();
		TextArea.setSelection( this.textarea, this.CLIPBOARD );
	} ,

	/* UNDO functionality: use by all toolbar and find&replace functions */
	UNDOstack : [],
	REDOstack : [],
	UNDOdepth : 20,

	storeTextarea : function() {
		this.UNDOstack.push( this.textarea.value );
		$('tbUNDO').disabled = '';
		this.REDOstack = [];
		$('tbREDO').disabled = 'true';
		if(this.UNDOstack.length > this.UNDOdepth) this.UNDOstack.shift();
	},
	undoTextarea : function(){
		if(this.UNDOstack.length > 0){
			$('tbREDO').disabled = '';
			this.REDOstack.push(this.textarea.value);
			this.textarea.value = this.UNDOstack.pop();
		}
		if(this.UNDOstack.length == 0) $('tbUNDO').disabled = 'true';
		if(!this.selector) return;
		this.onSelectorLoad();
		this.onSelectorChanged();

		this.textarea.focus();
	},	
	redoTextarea : function(){
		if(this.REDOstack.length > 0){
			$('tbUNDO').disabled = '';
			this.UNDOstack.push(this.textarea.value);
			this.textarea.value = this.REDOstack.pop();
		}
		if(this.REDOstack.length == 0) $('tbREDO').disabled = 'true';
		if(!this.selector) return;
		this.onSelectorLoad();
		this.onSelectorChanged();
		this.textarea.focus();
	}			  
} 


/** TextArea support routines
 ** allowing to get and set the selected texted in a textarea
 ** These routines have browser specific code to support IE
 ** Also runs on Safari.
 **
 ** Inspired by JS QuickTags from http://www.alexking.org/
 ** but extended for JSPWiki -- DirkFrederickx Jun 06.
 **/
var TextArea =
{
	getSelection: function(id){
		var f = $(id); 
		if(!f) return ''; 
		
		if(document.selection){  //IE
			f.focus();
			return document.selection.createRange().text;
		}
		else if(f.selectionStart || f.selectionStart == '0'){ //MOZILLA/NETSCAPE
			f.focus();   
			var start = f.selectionStart,
				end = f.selectionEnd;
			return f.value.substr( start, end-start );
		}
		return '';
	},
	
	/* replaces the selection with aValue, and returns with aValue selected */
	setSelection: function(id, aValue){
		var f = $(id); 
		if(!f) return ''; 
		f.focus();
		 
		if(document.selection){  //IE
			document.selection.createRange().text = aValue;
			f.focus();
		}
		else if(f.selectionStart || f.selectionStart == '0'){ //MOZILLA/NETSCAPE
			f.focus();   
			var start = f.selectionStart,
				end = f.selectionEnd,
				top = f.scrollTop;
			
			f.value = f.value.substring(0, start)
			         + aValue 
			         + f.value.substring(end, f.value.length);
			f.focus();
			f.selectionStart = start;
			f.selectionEnd = start + aValue.length;
			f.scrollTop = top;
		} else {
			f.value += value;
			f.focus();
		}
		if(f.onchange) f.onchange();
	},
	
	/* check whether selection is preceeded by a \n (peek-ahead trick) */
	isSelectionAtStartOfLine: function(id){
		var f = $(id); 
		if(!f) return ''; 
		f.focus();

		if(document.selection){  //IE
			var r1 = document.selection.createRange(),
				r2 = document.selection.createRange();
			r2.moveStart( "character", -1);
			if(r1.compareEndPoints("StartToStart", r2) == 0) return true;
			if(r2.text.charAt(0).match( /[\n\r]/ )) return true;
		}
		else if(f.selectionStart || f.selectionStart == '0'){ //MOZILLA/NETSCAPE
			if(f.selectionStart == 0) return true;
			if(f.value.charAt(f.selectionStart-1) == '\n') return true;
		} 
		return false;
	}	
};

//*************************
// TODO
// copied from default -- to be incorporated in EditTools
var globalCursorPos; // global variabe to keep track of where the cursor was

//sets the global variable to keep track of the cursor position
function setCursorPos(id) 
{
	globalCursorPos = getCursorPos( document.getElementById(id) );
}

function getCursorPos(textElement) 
{
	//save off the current value to restore it later,
	var sOldText = textElement.value;

	// For IE
	if( document.selection )
	{
		var objRange = document.selection.createRange();
		var sOldRange = objRange.text;

		//set this string to a small string that will not normally be encountered
		var sWeirdString = '#%~';

		//insert the weirdstring where the cursor is at
		objRange.text = sOldRange + sWeirdString; objRange.moveStart('character', (0 - sOldRange.length - sWeirdString.length));

		//save off the new string with the weirdstring in it
		var sNewText = textElement.value;

		//set the actual text value back to how it was
		objRange.text = sOldRange;

		//look through the new string we saved off and find the location of
		//the weirdstring that was inserted and return that value
		for (i=0; i <= sNewText.length; i++) {
			var sTemp = sNewText.substring(i, i + sWeirdString.length);
			if (sTemp == sWeirdString) {
			  var cursorPos = (i - sOldRange.length);
			  return cursorPos;
			}
		}
	}
	// Mozilla and the rest
	else if( textElement.selectionStart || textElement.selectionStart == '0') 
	{
		return textElement.selectionStart;
	}
	else
	{
		return sOldText.length;
	}
}

//this function inserts the input string into the textarea
//where the cursor was at
function insertString(stringToInsert) {
	var firstPart = myForm.myTextArea.value.substring(0, globalCursorPos);
	var secondPart = myForm.myTextArea.value.substring(globalCursorPos,
			                                               myForm.myTextArea.value.length);
	myForm.myTextArea.value = firstPart + stringToInsert + secondPart;
} 

/*******************************/
//JSON-RPC
//POST is
//{"id": 2, "method": "search.getSuggestions", "params": ["p", 10]}
//response is
//{"result":{"list":["Pic\/ruby.jpg","Pic\/telenet-smile.gif","Pic\/spin-greyblocks.gif","Pic\/shadow_transparent2.png","Pic\/monkey-mam-child.jpg","Pic\/brushed-button.jpg","Pic\/resizecursorv.png","Pic\/UserKeychainIcon.tiff","PrototypeJavascriptLibrary","Pizza Margerita"],"javaClass":"java.util.ArrayList"},"id":2}

function getSuggestions(id)
{
	if(window.ie) return;
	var textNode = document.getElementById(id);
	var val = textNode.value;
	var searchword;
	
	var pos = getCursorPos(textNode);
	for( i = pos-1; i > 0; i-- )
	{
		if( val.charAt(i) == ']' ) break;
		if( val.charAt(i) == '[' && i < val.length-1 ) { searchword = val.substring(i+1,pos); break; }
	}

	if( searchword )
	{
		jsonrpc.search.getSuggestions( callback, searchword, 10 );
	}
	else
	{
		var menuNode = document.getElementById("findSuggestionMenu");
		menuNode.style.visibility = "hidden";
	}
}
function callback(result,exception)
{   
	 if(exception) { alert(exception.message); return; }
	 
	 var menuNode = document.getElementById("findSuggestionMenu");
	 
	 var html = "<ul>";
	 for( i = 0; i < result.list.length; i++ )
	 {
			html += "<li>"+result.list[i]+"</li>";
	 }
	 html += "</ul>";
	 menuNode.innerHTML = html;
	 menuNode.style.visibility = "visible";
}


window.addEvent('load', EditTools.onPageLoad.bind(EditTools) ); //edit only