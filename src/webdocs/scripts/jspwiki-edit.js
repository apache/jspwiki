/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
 
/**
 ** Javascript routines to support JSPWiki Editing
 ** since v.2.6.0
 ** uses mootools v1.1
 **
 ** Needs jspwiki-common.js and mootools.js
 ** EditTools object (main object)
 ** - find&replace functionality : with regexp support
 ** - included popup-pagelinks routines from Janne 
 **
 ** TextArea object
 **     Supports selections inside textarea, in ie and other browsers
 **/

var EditTools = 
{
	onPageLoad: function(){

		this.textarea = $('editorarea'); 
		if(!this.textarea || !this.textarea.visible) return;

		/* create table of contents dropdown,
		   and duplicate the textarea into a main and work area
		   The workarea is used for editing the selection section
		   The mainarea reflects at all times the whole document
		*/
		this.onPageLoadSectionToc()
		
		window.onbeforeunload = (function(){
			var ta = $('editorarea');
			if(ta.value != ta.defaultValue) return "edit.areyousure".localize();
		}).bind(this);

		//alert($('scroll').getValue());

		/* make textarea more intelligent */
		this.wikisnippets = this.getWikiSnippets();
		this.wikismartpairs = this.getWikiSmartPairs();
		this.onPageLoadPostEditor();

		/* activate editassist toolbar */
		var toolbar = $('toolbar'),
			fxToolbar = new Fx.Slide(toolbar,{
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
		$('tbREDO').addEvent('click', function(e) { EditTools.redoTextarea(); new Event(e).stop(); });
		$('tbUNDO').addEvent('click', function(e) { EditTools.undoTextarea(); new Event(e).stop();  });
		$('replace').addEvent('click', function(e) { EditTools.doReplace(); new Event(e).stop(); })
			.getParent().getParent().show();

		toolbar.getElements('a.tool').each(function(el){
			el.addEvent('click', this.insertTextArea.pass(el,this));
		},this);

		/* add textarea resize drag bar */
		var hh=Wiki.prefs.get('EditorSize');
		if(hh) this.textarea.setStyle('height',hh);
		var h = new Element('div',{
			'class':'textarea-resizer', 
			'title':'edit.resize'.localize()
		}).injectAfter(this.textarea);	
		this.textarea.makeResizable({
			handle:h, 
			modifiers: {x:false, y:'height'}, 
			onComplete: function(){	Wiki.prefs.set('EditorSize',this.value.now.y); }
		});			
		
		/* add textarea suggestion events */
		this.textarea
			.addEvent('click',this.getSuggestions.bind(this))
			.addEvent('keyup',this.getSuggestions.bind(this));
			
	},

	onPageLoadPostEditor: function(){
		if(window.ie) return;
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
		$('smartpairs').getParent().show();
				
		this.initPostEditor();	
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
		TextArea.replaceSelection(this.textarea, t);
		return false; /*don't propagate*/
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
			TextArea.replaceSelection( this.textarea, data );
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
		TextArea.replaceSelection( this.textarea, ss );
	} ,

	paste : function(){
		if( !this.CLIPBOARD ) return;
		this.storeTextarea();
		TextArea.replaceSelection( this.textarea, this.CLIPBOARD );
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
		//this.onSelectorLoad();
		//this.onSelectorChanged();
		
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
		//this.onSelectorLoad();
		//this.onSelectorChanged();
		this.textarea.focus();
	},
	
	getSuggestions: function() {
		//if(window.ie) return; NEED TO BE TESTED ON IE - SHOULD WORK FINE

		var textarea = this.textarea,
			pos = TextArea.getCursor(textarea);
			val = textarea.value,
			searchword = "";
			
		var	suggestID = 'findSuggestionMenu', fav = $('favorites'),
			suggest = $(suggestID) || new Element('div',{
				'id':suggestID, 
			}).injectTop(fav);

		/* find a partial jspwiki-link 'searchword' */
		/* look backwards for the start of a wiki-link bracket */
		for( i = pos-1; i > 0; i-- ){
			if( val.charAt(i) == ']' ) break;
			if( val.charAt(i) == '[' && i < val.length-1 ) { 
				searchword = val.substring(i+1,pos); 
				if(searchword.indexOf('|') != -1) searchword = searchword.split('|')[1];
				break; 
			}
		}
		if(searchword =="") return suggest.hide();

		var searchlen = searchword.length;		

		Wiki.jsonrpc('search.getSuggestions', [searchword,30], function(result,exception){
			if(exception) { 
				alert(exception.message); 
			} else if(result.list.length == 0) { 
				suggest.hide();
			} else {
				var ul = new Element('ul').inject( suggest.empty().show() );
				result.list.each( function(rslt) { 
					new Element('li',{
						'title':rslt,
						'events': {
							'click':function(ev){ 
								new Event(ev).stop(); 
								EditTools.storeTextarea();
								TextArea.replaceSelection(textarea, rslt.substr(searchlen));
							}
						}
					}).setHTML( rslt.trunc(36) ).inject(ul);
				}); /* each */
			} /* endif */
		});
	},
	
	onPageLoadSectionToc : function(){
		/* initialise a new sectionToc menu */
		this.selector = new Element('ul');
		Wiki.makeMenuFx( 'sectiontoc', this.selector);

		// create new working textarea, to allow section edits
		var m = this.mainarea = this.textarea;
		this.textarea = m.clone()
			.removeProperty('id')
			.removeProperty('name')
			.addEvent('change', this.onChangeTextarea.bind(this))
			.injectBefore( m.hide() );

		this.onSelectorLoad();
    
		var cursor = location.search.match(/[&?]section=(\d+)/);
		cursor = (cursor && cursor[1]) ? 1+cursor[1].toInt() : 0;
		if((cursor>1) && this.textarea.sop) cursor++;
		this.onChangeSelector(cursor);
	},	
	
	/* 
	 * UPDATE/RFEFRESH the section selector dropdown
	 * This function is called at startup, and everytime the section textarea changes
	 * Postcondition: the sectiontoc dropdown contains following entries
	 *   0. ( all )
	 *   1. start-of-page (if applicable)
	 *   2. text==<<header 1...n>> , <<selector.offset stores start-offset in main textarea>>
	 */  
	 onSelectorLoad : function(){
		var mainarea = this.mainarea.value,
			excursor = this.selector.cursor || 0; //remember previous cursor
		var DELIM = "\u00a4",
		 
		/* mask all headers inside a {{{ ... }}} but keep length unchanged! */
		mainarea = mainarea.replace(/\{\{\{([\s\S]*?)\}\}\}/g, function(match){
			return match.replace( /^!/mg, DELIM );
		});

		var tt = mainarea.split( /^(!{1,3}.*?)/m);

		this.newSelector();
		this.textarea.sop = (tt.length>1) && (tt[0] != ''); //start of page section has no !!!header 
		if(this.textarea.sop) this.addSelector("edit.startOfPage".localize(), 0, 0);
		
		var pos = tt.shift().length,
			ttlen = tt.map(function(i){ return i.length });

		for(var i=0; i<ttlen.length; i=i+2){
			var hlen = ttlen[i],  //length of header markup !!!,!! or !
				indent = (hlen==2) ? 1 : (hlen==1) ? 2 : 0,
				title = tt[i+1].match(/.*?$/m)[0];

			this.addSelector(title, pos, indent);
			pos += hlen + ttlen[i+1];
		}
		//alert(this.selector.offsets);
		//this.selector.cursor = (oldindex < cursor) ? oldindex, ; 
		//if( oldIndex < cursor ) this.selector.options[oldIndex].selected = true;
	},
	setSelector: function( newcursor ){
		var els = this.selector.getChildren();
		
		if(newcursor <0 || newcursor >= els.length) newcursor = 0;
		els.removeClass('cursor');
		els[newcursor].addClass('cursor');
		this.selector.cursor = newcursor; //not used ????
	},
	newSelector: function(){
		this.selector.empty();
		this.selector.offsets = [];
		this.addSelector("( all )",-1);
	},
	addSelector: function(text,offset,indent){
		this.selector.offsets.push(offset);
		this.selector.adopt( 
			new Element('li').adopt(
				new Element('a',{
					'class':'action',
					'styles': {
						'padding-left':(indent+0.5)+'em'
					},
					'title':text,
					'events':{
						'click':this.onChangeSelector.pass([this.selector.offsets.length-1],this) 
					}
				}).setHTML(text.trunc(48))
			) 
		);	
	},

	/* the USER clicks a new item from the section selector dropdown
	 * copy a part of the main textarea to the section textarea
	 */
	onChangeSelector: function(cursor){
		var se = this.selector.offsets, ta = this.textarea, ma = this.mainarea.value;

		this.setSelector(cursor);
		ta.begin = (cursor==0) ? 0 : se[cursor];
		ta.end = ((cursor==0) || (cursor+1 >= se.length)) ? ma.length : se[cursor+1]; 
		ta.value = ma.substring(ta.begin,ta.end);		
		//alert(cursor+' '+ma.length+' '+ta.begin+' '+ta.end+' '+se[cursor+1])
	},

	/*
	 * Changes in the section textarea: 
	 * happens when 
	 *  (i)  textarea is changed and deselected (click outside the textarea) 
	 *  (ii) user clicks a toolbar-button
	 *  
	 * 1) copy section textarea at the right offset of the main textarea
	 * 2) refresh the sectiontoc menu
	 */
	onChangeTextarea : function(){
		var	ta = this.textarea,	ma = this.mainarea;

		//alert('change textarea');

		var	s = ta.value;
		if( s.lastIndexOf("\n") + 1 != s.length ) ta.value += '\n';
		 
		s = ma.value;
		ma.value = s.substring(0, ta.begin) + ta.value + s.substring(ta.end);
		ta.end = ta.begin + ta.value.length;
		this.onSelectorLoad();  //refresh selectortoc menu
	 }
} 


/** 
TextArea support routines 
- getSelection(id) : return selected text (string)
- getCursor(id) : returns char offset of cursor (integer)
- replaceSelection(id,newtext): replaces selection with newtext, on return the new text is selected
- isSelectionAtStartOfLine(id): returns boolean indicating whether cursor is at the start of newline
**/
var TextArea =
{
	getSelection: function(id){
		var f = $(id); if(!f) return ''; 
		
		if(window.ie) return document.selection.createRange().text;
		return f.getValue().substring(f.selectionStart, f.selectionEnd);
	},

	// return char offset of cursor
	getCursor: function(id) {
		var f = $(id); if(!f) return ''; 

		if(window.ie){
			var r1 = document.selection.createRange(),
				r1text = r1.text,
				XX = '#%~'; //small string that will not normally be encountered

			//insert the weirdstring where the cursor is at
			r1.text = r1text + XX; 
			r1.moveStart('character', (0 - r1text.length - XX.length));

			//save off the new string with the weirdstring in it
			var modified = f.value;

			//set the actual text value back to how it was
			r1.text = r1text;

			//look through the new string we saved off and find the location of
			//the weirdstring that was inserted and return that value
			/* just use index of ?? */
			//return( modified.indexOf(XX) - r1text.length);
			for (i=0; i <= modified.length; i++) {
				var tmp = modified.substring(i, i + XX.length);
				if (tmp == XX) {
				  	return (i - r1text.length);
				}
			}
		}
		// Mozilla and the rest
		else if( f.selectionStart || f.selectionStart == '0') {
			return f.selectionStart;
		} else {
			return f.value.length;
		}
	},

	replaceSelection: function(id, newText){
		var f = $(id); if(!f) return;
		var scrollTop = f.scrollTop; //cache top
		 
		if(window.ie){
			f.focus();
			var r = document.selection.createRange();
			r.text = newText;
			r.moveStart('character',-newText.length); /***/
			r.select();
			f.range.select();
		} else { 
			var start = f.selectionStart, end = f.selectionEnd;
			f.value = f.value.substring(0, start) + newText + f.value.substring(end);
			f.selectionStart = start;
			f.selectionEnd = start + newText.length;
		}
		f.focus();
		f.scrollTop = scrollTop;
		
		if(f.onchange) f.onchange();
	},
	
	/* check whether selection is preceeded by a \n (peek-ahead) */
	isSelectionAtStartOfLine: function(id){
		var f = $(id); if(!f) return false;

		if(window.ie){
			f.focus();
			var r1 = document.selection.createRange(),
				r2 = document.selection.createRange();
			r2.moveStart( "character", -1);
			if(r2.text=="") r2.moveEnd( "character", 1);
			if(r1.compareEndPoints("StartToStart", r2) == 0) return true;
			if(r2.text.charAt(0).match( /[\n\r]/ )) return true;
		}
		else {
			if(f.selectionStart == 0) return true;
			if(f.value.charAt(f.selectionStart-1) == '\n') return true;
		} 
		return false;
	}	
};

window.addEvent('load', EditTools.onPageLoad.bind(EditTools) ); //edit only