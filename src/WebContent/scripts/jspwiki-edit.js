/*! 
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

/*
Function: toISODate
	Return the current date in ISO8601 format 'yyyy-mm-dd'.
	(ref. EcmaScript 5)	

Example:
> alert( new Date().toISODate() ); // alerts 2009-05-21
> alert( new Date().toISODate() ); // alerts 2009-05-21T16:06:05.000TZ
*/
$native(Date);
Date.extend({
	toISODate: function(){
		var d = this,
			dd = d.getDate(),
			mm = d.getMonth()+1;

		return d.getFullYear() + '-' + (mm<10 ?'0':'') + mm + '-' + (dd<10 ?'0':'') + dd;
	}
});



/*
Class: WikiEditor
	The WikiEditor class implement all JSPWiki's plain editor support functions.
	It uses the [SnipEditor] class to enhance the textarea object.
	
	The WikiEditor contains all JSPWiki's specific snippets to ease the entry of 
	the JSPWiki markup syntax.	
*/
var WikiEdit = 
{
	initialize: function(){

		//should always run first, but seems not guaranteed on ie so let's do this for sure
		Wiki.initialize(); 
		
		var txta = $('editorarea'),
			self = this,
			snipe,
			config,
			tocCursor,
			prefs = Wiki.prefs,
			tileBtns, tileFn,
 			height = prefs.get('EditorSize');

		window.onbeforeunload = function(){
			if( txta.value != txta.defaultValue ) return "edit.areyousure".localize();
		};

		if( height ) txta.setStyle('height',height);

		// open the new snipEditor
		snipe = self.snipEditor = new SnipEditor( txta, {

			tabsnips: self.tabSnippets, 
			// tabcompletion => set by this.configFn()
			// directsnips => set by this.configFn() 
			suggestsnips: self.suggestSnippets(), 

  			buttons: $$('a.tool'),

			toc: {
				tocParser: (Wiki.Context =='edit') ? self.jspwikiTOC : null,
				all: "edit.allsections".localize(),
				startOfPage: "edit.startOfPage".localize()
			},

			dialogs: {
				//config: [Dialog, {caption:'Configuration Dialog'.localize(), body:$('configDialog')}],
				config: new Dialog({caption:'Configuration Dialog'.localize(), body:$('configDialog'), showNow:false}),
				//pagename: new Dialog...
				fonts: {caption:'My-Fonts'},
				colors: {colorImage:'templates/default/images/circle-256.png'},
				special: {caption:'My-Special Chars'}
			},

			findForm: {
				findDialog: $('findDialog'),
				submit: $('doreplace'),
				findInput: $('tbFIND'),
				replaceInput: $('tbREPLACE'),
				isRegExp: $('tbREGEXP'),
				isMatchCase: $('tbMatchCASE'),
				isReplaceGlobal: $('tbGLOBAL'),
				msgNoMatchFound: function(){
					Wiki.alert('edit.findandreplace.nomatch'.localize());
					//Wiki.roar('edit.findandreplace.nomatch'.localize());
					//FIXME: alert('edit.findandreplace.nomatch'.localize());
				}
			},

  			next: $('nextInput'),

  			onresize:function(height){  
  				//save the height in the preference cookie
  				prefs.set('EditorSize',height); 
  			}

  		});

		//initialize the configuration dialog and link it to the buttons
		configFn = function(){
			snipe.set('directsnips', $('smartpairs').checked ? self.directSnippets : {})
				.set('tabcompletion', $('tabcompletion').checked == true );
		};
		['smartpairs', 'tabcompletion'].each( function(id){
			var element = $(id);
			if( element ){ 
				element.setProperty( 'checked', prefs.get(id) || false )
					.addEvent( 'click', function(e){
						configFn();
						prefs.set(id, this.checked);
					});
			}
		});
		configFn();
		
		//initialize the preview layout buttons: tile-vert or tile-horz
		tileBtns = $$('.tHORZ','.tVERT');
		tileFn = function(tile){

			prefs.set('previewLayout',tile);
			tileBtns.each(function(el){
				el[( el.getText()==tile ) ? 'hide': 'show']();
			});

			tile = (tile=='tile-vert') ? 'size1of1':'size1of2';
			$('editor-content').getChildren().each(function(el){
				el.removeClass('size1of1').removeClass('size1of2').addClass(tile);
			});
		};
		tileBtns.each( function(el){
			el.addEvent( 'click',function(){ tileFn(this.getText()); });
		});
		tileFn( prefs.get('previewLayout')||'tile-vert' );


		//add a localized hover title to the resize drag-bar
		$E('.editor-container .resize-bar').set({'title':'edit.resize'.localize()});

		// Select the right section of the page:  URL?section=0..n
		// cursor = -2 (all) or 0..n (section# - first section is 0)
		tocCursor = location.search.match(/[&?]section=(\d+)/);
		snipe.selectTocItem( tocCursor ? tocCursor[1].toInt() : -2 );

		self.initializePreview( snipe );		
	},

	/*
	Function: directSnippets
		DirectSnippet definitions for JSPWiki, aka ''smartpairs''.
		These snippets are directly expanded on keypress.
	*/
	directSnippets: { 
		'"' : '"',
		'(' : ')',
		'[' : ']',
		'{' : '}',
		"'" : {
			snippet:"'",
			scope:{
				"[{":"}]"  //plugin parameters
			}
		}
	},
	
	/*
	Function: tabSnippets
		TabSnippet definitions for JSPWiki.
		These snippets are expanded when clicking the corresponding toolbar button.
		When the ''tabcompletion'' flag is on, the tabSnippets are expanded after
		pressing the TAB key.

	Example:
	Clicking following link(button) with text "br" inserts a "\\" followed by a nexline
	>  <a href="#" class="tool" id="tbBR" title="key='editor.plain.tbBR.title'">br</a>
	
	The keystrokes {{nl<TAB>}} are converted to "\\" followed by a newline, 
	when the ''tabcompletion'' flag is on.
	>	'nl':'\\\\\n'

	*/
	tabSnippets: {

		"%%": {
			nScope: { "%%(":")" },
			snippet: "%%{css-style} {body-text} /% ",
			"css-style": {
				//"(css-definitions)|information|warning|error|quote|sub-script|superscript|strikethrough|monospace"
				"css definitions":"( css:value; )",
				"information":"information",
				"warning":"warning",
				"error":"error",
				"commentbox":"commentbox",
				"quoted paragraph":"quote",
				"sub-script<span class='sub'>2</span>":"sub",
				"super-script<span class='sup'>2</span>":"sup",
				"<span class='strike'>strikethrough</span>":"strike"

			}
		},
		"br": "\\\\\n",
		"nl": { synonym: "br" },
		"hr": "\n----\n",
		"h1": "\n!!!{Heading 1 title}\n", 
		"h2": "\n!!{Heading 2 title}\n",
		"h3": "\n!{Heading 3 title}\n",

		"font": {
			nScope: { 
				"%%(":")",
				"font-family:":";"
			},
			snippet: "%%(font-family:{fonts};) {body}/% "
		},
		"color": {
			nScope: { "%%(":")" },
			snippet: "%%(color:{colors};background-color:{colors}; ) {body}/% "
		},
		"special": {
			snippet: "{special}",
			nScope: { "%%(":")" }
		},
		
		
		"dl": "\n;{term}:{definition-text} ", 
		"sub": "%%sub {subscript text}/% ",
		"sup": "%%sup {superscript text}/% ",
		"strike": "%%strike {strikethrough text}/% ",
		"xflow": "\n%%xflow\n{wide content}\n/%\n ",
		"pre": "\n\\{\\{\\{\n{some preformatted block}\n}}}\n",
		"code": "\n%%prettify \n\\{\\{\\{\n{/* some code block */}\n}}}\n/%\n",
		"mono": "\\{\\{{monospaced text}}} ",

		"link": {
			snippet:"[{link text}|{pagename or url}|{attributes}] ",
			attributes:"acceskey=X|title=description|target:_blank"
		},
/*		'accesskey', 'charset', 'class', 'hreflang', 'id', 'lang', 'dir', 
		'rel', 'rev', 'style', 'tabindex', 'target', 'title', 'type'
		- link-text
		- wiki-page or url
		- description:title
		
		
		
		- target: _blank --new-- window yes or no
*/

		"bold": "__{bold text}__ ",
		"italic": "''{italic text}'' ",
		
		"acl": {
			snippet: "\n[\{ALLOW {permission} {principal(,principal)} \}]\n",
			permission: "view|edit|modify|comment|rename|upload|delete",
			"principal(,principal)": function(){
				return "Anonymous|Asserted|Authenticated|All";
				//FIXME: retrieve user group list through ajax call
			}
		},
		"allow": { synonym: "acl" },
	
		"img": {
			snippet:"\n[\\{Image src='{img.jpg}' width='{400px}' height='{300px}' align='{text-align}' style='{css-style}' class='{css-class}' }]\n",
			'text-align':'left|center|right'
		},
				
		"plugin": {
			snippet:"\n[\\{{plugin}}]\n",
			plugin: {
				"Set a page variable":"SET name='value'",
				"Get a page variable":"$varname",
				"Test a page variable":"If name='value' page='pagename' exists='true' contains='regexp'\n\nbody\n",
				"Insert Page":"InsertPage page='pagename'",
				"Table Of Contents [toc]":"TableOfContents",
				"Make Page Alias":"SET alias='pagename'",
				"Current Time":"CurrentTimePlugin format='yyyy mmm-dd'",
				"Incoming Links":"ReferredPagesPlugin page='pagename' type='local|external|attachment' depth='1..8' include='regexp' exclude='regexp'",
				"Outgoing Links":"ReferringPagesPlugin page='pagename' separator=',' include='regexp' exclude='regexp'",
				"Search":"Search query='Janne' max='10'",
				"Display weblog posts":"WeblogPlugin page='pagename' startDate='300604' days='30' maxEntries='30' allowComments='false'",
				"New weblog entry":"WeblogEntryPlugin"
			}
		},
		"tab": {
			nScope: { 
				"%%(":")",
				"%%tabbedSection":"/%"
			},
			snippet:"%%tabbedSection \n%%tab-{tabTitle1}\n{tab content 1}\n/%\n%%tab-{tabTitle2}\n{tab content 2}\n/%\n/%\n"
		},
		"alias": {
			nScope: { "[{":"}]" },
			snippet:"\n[\\{SET alias='{pagename}' }]\n"
		},
		"toc": {
			nScope: { "[{":"}]" },
			snippet:"\n[\\{TableOfContents }]\n"
		},
		"table": "\n||heading-1||heading-2\n| cell11   | cell12\n| cell21   | cell22\n",
		"quote": "\n%%quote \n{quoted text}\n/%\n",
	
		"sign": function(){
			var name = Wiki.UserName || 'UserName';
			return "\\\\--" + name + ", "+ new Date().toISODate() + "\n";
		},

		"date": function(k) {
			return new Date().toISODate()+' ';
			//return "[{Date value='" + d.toISODate() + "' }]"
			//return "[{Date " + d.toISODate() + " }]"
		},
		"lorem" : "Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n",
		"Lorem" : { synonym: "lorem" }
	}, 
		
	/*
	Function: suggestSnippets
		Suggest-Snippet definitions for JSPWiki.
		The snippets are processed after key-up events or mouse-clicks.
		The typically pop-up a ''suggestion'' dialog-box, assisting the
		entry of information in the textarea.
		The suggestion dialog-box is only shown in certain ''contextual'' conditions.
	*/
	suggestSnippets:function(){
		return {
		/*
		Suggest snippet: color
			This snippet will popup the colorwheel whenever the cursor is
			after or inside a hexadecimal color definition
		*/
		colors: {
			//scope:{	"%%(":")" },
			scope: function(txta){
				//fixme
				var iscolor = txta.getFromStart().test( /(#[0-9a-f]*)$/i );
				if(iscolor){
					//
					var len = RegExp.$1.length,
						caret = txta.getSelectionRange();
					txta.setSelectionRange( caret.start-len, caret.start );
				}
				return iscolor
			}
			//dialog = 'colors'
		},
		/*
		Suggest snippet: Link
			This snippet will popup a dialog with partial matching pagenames.
			The list of pages is retrieved via an JAX-RPC call to JSPWiki.
		*/
		//suggestLink is the name of a predefined dialog
		//suggest:
		link: {
			scope: function( txta ){
				//match [<newline>
				//match [xxxx <until ]>
				//match [xxxx <until newline>
				//dont match [{
				//dont match [[
				return txta.getFromStart().test( /\[(:?[^\[\{\]\n\r][^\]\n\r]*)?$/ );
			},
			dialog: [SelectionDialog, {
				body:'',
				caption:'Link Suggestion Dialog', 
				onShow:this.updateSuggest.bind(this),
				onSelect:this.updateLink.bind(this)
			}]
		}

		};
	},
	
	/*
	Function: suggestLink
		Suggest list of page of page/attachement names based on the
		partial match of the textarea input.
	*/
	updateLink: function( value ){

		var txta = this.snipEditor.get('textarea'),
			caret = txta.getSelectionRange();
		
		//extend the selection till next ] or end of the line
		if( caret.thin ){ 
			var end = txta.getTillEnd().search( /[\n\r\]]/ );
			if( end!=-1 ) txta.setSelectionRange( caret.start, caret.start + end );
		}
		txta.setSelection( value.slice(this.suggestPrefix) );
	},

	/*
	Function: updateSuggest
		onShow() event Handler on the link suggestion dialog
	*/
	updateSuggest: function( dialog ){

		var txta = this.snipEditor.get('textarea'),
			fromStart = txta.getFromStart(),
			//match '[' + 'any char except \n or ]' at end of the string
			searchword = fromStart.match( /\[([^\n\r\]]*)$/ )[1];
	
		if(searchword.indexOf('|') != -1) searchword = searchword.split('|')[1];
		this.suggestPrefix = searchword.length;
		
		//ifn o searchword, then get list of page attachments
		if(searchword == "" ) searchword = Wiki.PageName + '/';
		//alert(searchword);		
		
		Wiki.jsonrpc('search.getSuggestions', [searchword,30], function(result,exception){
			//offline testing:
			//var result = {list:['result1', 'result longer 2', 'result very longereererere 3', 'results moremore']};
			//var exception;
		
			if( exception ){ 

				alert( exception.message ); 

			} else if( !result.list || ( result.list.length == 0 ) ){ 

				dialog.hide();

			} else {
			
				dialog.setBody( result.list );

			}

		});
		
	},
	
	/*
	Function: jspwikiTOC
		Convert a jspwiki-markup page to an array of page sections.
		Each section starts with a JSPWiki header line. ( !, !! !!! )
		This function is a callback function for the [SnipEditor]. 
		It is called by [snipeditor.buildToc] every time the textarea of the
		snipeditor is being changed.
	
	Returns:
		This function returns a array of objects [{title, start, indent}]
		title - (string) title of the section without markup characters
	 	start - (integer) offset within the text string where this section starts
	 	indent - (integer) indentation or nesting level of the section 0,1...n
	*/
	jspwikiTOC: function( text ){
		
		// mask any header line inside a {{{ ... }}} but keep length of the text unchanged!
		text = text.replace(/\{\{\{([\s\S]*?)\}\}\}/g, function(match){
			return match.replace( /^!/mg, ' ' );
		});
			
		var result = [],
			DELIM = '\u00a4',
			// after the regexp and split text, you'll get an array:
			// [0] : text prior to the first header
			// [1,odd] : header markup !, !! or !!!
			// [2,even] : remainder of the section, starting with header title
			tt = text.replace( /^([!]{1,3})/mg, DELIM+"$1"+DELIM ).split(DELIM),
				
			pos = tt.shift().length,  //get length of the first element, prior to first section
			count = tt.length;

		for( var i=0; i<count; i=i+2 ){

			var hlen = tt[i].length,
				//take first line
				title = tt[i+1].split(/[\r\n]/)[0]
					//remove unescaped(~) inline wiki markup __,'',{{,}}, %%(*), /%
					.replace(/(^|[^~])(__|''|\{\{|\}\}|%%\([^\)]+\)|%%\S+\s|%%\([^\)]+\)|\/%)/g,'$1')
				    //and remove wiki-markup escape chars ~ 			
					.replace(/~([^~])/g, '$1'); 

			//Indent: convert length of header markup (!!!,!!,!) into #indent-level:  3,2,1 => 0,1,2
			result.push({'title':title, 'start':pos, 'indent':3-hlen})
			pos += hlen + tt[i+1].length;
		};

		return result;
	},
	
	/*
	Function: initializePreview
		Initialize textarea preview functionality.
		When #autopreview checkbox is checked, bind the
		[refreshPreview] handler to the {{preview}} event
		of the textarea.
		
		Finally, send periodically the preview event. 
	*/
	initializePreview : function( snipe ){
	
		var autopreview = 'autopreview',
			self = this,
			prefs = Wiki.prefs,
			refreshFn = self.refreshPreview.bind(self);

		$(autopreview)
			.setProperty('checked', prefs.get(autopreview) || false)
			.addEvent('click', function(){
				prefs.set(autopreview, this.checked);
				refreshFn();
			})
			.fireEvent('click');
		
		self.refreshPreview.periodical(3000, self);
		snipe.toElement().addEvent('change',refreshFn);
    },


	/*
	Function: refreshPreview
		Make AJAX call to the backend to convert the contents of the textarea
		(wiki markup) to HTML.
		
		
	*/
	refreshPreview: function(){

    	var	self = this,
    		snipe = self.snipEditor,
    		text = snipe.get('textarea').getValue(),
    		page = Wiki.PageName,
    		preview = $('livepreview'),
    		spin = $('previewspin');


		if( !$('autopreview').checked ){ 

			if( self.previewcache ){
				preview.empty();
				self.previewcache = null;
			}

		} else if( self.previewcache != text ){

			self.previewcache = text;		
    		
			new Ajax( Wiki.TemplateUrl + "/AJAXPreview.jsp?page=" + page, { 
				postBody: 'wikimarkup=' + encodeURIComponent( text ),
				update: preview,
				onRequest: function(){ spin.show(); },
				onComplete: function(){ spin.hide(); Wiki.renderPage(preview, page); }
			}).request();
		
		}
	}
	
}

/*
Global: load/domready
	Initialize the WikiEdit class on page load.
*/
window.addEvent('domready', function(){ WikiEdit.initialize() } );



/*
Class: SnipEditor
	The SnipEditor class enriches a TEXTAREA object with many capabilities, 
	including tab-autocompletion, auto-indentation, smart typing pairs, suggestion 
	popups, live-preview, textarea resizing, toggle buttons etc.
	The configuration of the snip-editor is done through Snippet objects. 
	See [getSnippet] for more info on how to define snippets.

Credit:
	The SnipEditor was inspired by postEditor (by Daniel Mota aka IceBeat, 
	http://icebeat.bitacoras.com ) and ''textMate'' (http://macromates.com/).
	It has been written to fit with needs of the JSPWIKI project.
	The main changes and enhancements include support for suggestion-popups, toolbar 
	driven toggle buttons, simplified the snippet definition, 
	and compatible with IE6/7. (ugh)
	
	Dirk Frederickx, Oct-Dec 2008
	
Arguments:
	el - textarea element
	options - optional, see options below

Options:
	tab - (string) number of spaces used to insert/remove a tab in the textarea; 
		default is 4
	tabcompletion - (boolean, default true) when set to true, 
	    the tabSnippet keywords will be expanded
		when pressing the TAB key.  See also [tabSnippet]
	tabsnips - (snippet-object) set of snippets, which will be expanded when
		clicking a button or pressing the TAB key. See [getSnippet], [tabSnippet]
	directsnips - (snippet-object) set of snippets which are directly expanded
		on key-down. See [getSnippet], [directSnippet]
	suggestsnips - (snippet-object) set of snippets which are triggered at
		key-up or mouse click events. Typically suggestsnips are used to generate
		help dialog popups.
	buttons - (array of Elements), each button elemnet will bind its click-event 
	    with [onButtonClick}. When the click event fires, the {{rel}} attribute
	    or the text of the element will be used as snippet keyword.
	    See also [tabSnippet].
	dialogs - set of dialogs, consisting of either a Dialog object, 
		or a set of {dialog-options} for the predefined
		dialogs suchs as Font, Color and Special.
		See property [initializeDialogs] and [openDialog]
	findForm - (object) list of form-controls. See [onFindAndReplace] handler.
	next - (Element), when pressing Shift-Enter, the textarea will ''blur'' and
		this ''next'' element will ge the focus.
		This compensates the overwritting default TAB handling of the browser.
	onresize - (function, optional), when present, a textarea resize bar 
		with css class {{resize-bar}} is added after the textarea,
		allowing to resize the heigth of the textarea.
		This onresize callback function is called whenever
		the height of the textarea is changed.

Dependencies:
	[Textarea]
	[UndoRedo]
	
Example:
	(start code)
	<script>
		new SnipEditor( "mainTextarea", {
			tabsnips: { bold:"**{bold text}**", italic:"''{italice text}''" }, 
			tabcompletion:true,
			directsnips: { '(':')', '[' : ']' }, 
			buttons: $$('a.tool'),
			next:'nextInputField' 
	  	});
  	</script>
	(end)

*/
var SnipEditor = new Class({

	options: {
		tab: "    ", //default tab = 4 spaces
		tabcompletion: true,
		tabsnips: {},
		directsnips: {},
		suggestsnips: {},
		buttons: null,
		toc: {
			tocParser: null,
			all: "(All)",
			startOfPage: "Start of the page"
		},
		dialogs: {
			//fonts: {caption:'Fonts'},
			//colors: {colorImage: '../dialog/circle-256.png'},
			//special: {caption:'Special Chars'}
		},
		findForm: {
			findDialog: null,
			submit: null, //will get a click-event attached
			findInput: null, //input element
			replaceInput: null, //input element
			isRegExp: null, //checkbox, regular expression allowed
			isReplaceGlobal: null, //checkbox, global find&replace
			isMatchCase: null, //checkbox, match upper/lower case
			msgNoMatchFound: function(){}  //called when no find match is found
		},
		next: null, //element, shift-Enter will get you to the 'next' element
		onresize: null //function, onresize callback handler
	},

	initialize: function(el, options){

		this.setOptions(options);

		var self = this,
			options = self.options,
			main = self.mainarea = $(el),
			txta = main.clone()
				.removeProperty('id')
				.removeProperty('name')
				.injectBefore( main.hide() )
				.addEvent('change', self.onChangeTXTA.bind( self ) ); 
				// Make sure to catch ALL {{change}} events, and copy the 
				// content of txta back to the main textarea. 
				// This includes the ''last'' change event, just before firing 
				// the submit event of the form.

		self.textarea = new Textarea( txta );
		self.undoredo = new UndoRedo( self );
		self.activeSnip = null;
		self.initializeDialogs();
		self.initializeToc( txta );
		self.initializeResizing( txta );

		var keystroke = self.onKeystroke.bind( self ),
			btns = options.buttons,
			find = options.findForm.submit,
			suggest = self.suggestSnippet.bind( self );

		if( btns ) btns.addEvent( 'click', self.onButtonClick.bind( self ) );
		if( find ) find.addEvent( 'click', self.onFindAndReplace.bind( self ) );

		txta.addEvent( 'keydown', keystroke )
			.addEvent( 'keypress', keystroke )
			//fixme: any click outside the suggestion block should clear the active snip
			//better is to attach the click handler of the suggestion block to the document body
			//and decide on the target whether you click inside or not.
			.addEvent( 'click', self.clearActiveSnip.bind( self ))
			.addEvent( 'click', suggest )
			.addEvent( 'keyup', suggest );

	},
	
		
	/*
	Function: toElement
		Retrieve textarea DOM element;
		This allows the dollar function to return 
		the element when passed an instance of the class. (mootools 1.2.x)
		
	Example:
	>	var snipe = new SnipEditor('textarea-element');
	>	$('textarea-element') == snipe.toElement();
	>	$('textarea-element') == $(snipe); //mootools 1.2.x

	*/
	toElement: function(){
		return this.textarea.toElement();
	},	
	
	/*
	Function: get
		Retrieve some of the public properties of the snip-editor: 
		textarea, resize object.

	Arguments:
		item - 'resize' or 'textarea'
	*/
	get: function(item){
		return( /textarea|dialogs|tabcompletion|tabsnips|directsnips|suggestsnips/.test(item) 
			? this[item] : null )
	},
	
	/*
	Function: set
		Set/Reset some of the options of the snip-editor.

	Arguments:
		item - 'tabsnips', 'tabcompletion', 'directsnip' or 'sectionview'
		newvalue - 'resize' or 'textarea'
	Returns
		this SnipEditor object
	*/
	set: function(item, newvalue){
		if( /dialogs|tabcompletion|tabsnips|directsnips|suggestsnips/.test(item) ){
			this.options[item] = newvalue;
		}
		return this;
	},

	/*
	Function: onKeystroke
		This is a cross-browser keystroke handler for keyPress and keyDown 
		events on the textarea.

	Note:
		The KeyPress is used to accept regular character keys.
		The KeyDown event captures all special keys, such as Enter, Del, Backspace, Esc, ...
		To work around some browser incompatibilities, a hack with the {{event.which}} 
		attribute is used to grab the actual special chars.
		Ref. keyboard event paper by Jan Wolter, http://unixpapa.com/js/key.html
		Todo: check on Opera

	Arguments:
		e - (event) keypress or keydown event.
	*/
	onKeystroke: function(e){

		e = new Event(e);

		if( e.type=='keydown' ){

			//Only accept special keys via keydown event
			if( !Event.keys[e.key] ) return;

		} else { // e.type=='keypress'

			//Only accept regular character keys via keypress event
			//Note: cross-browser hack with 'which' attribute for special chars
			if( $chk(e.event.which) && (e.event.which==0) ) return; 

			//Reset faulty 'special char' treatment by mootools
			e.key = String.fromCharCode(e.code).toLowerCase(); 

		}

	    if( e.shift && e.key=='enter' ){
	    
	    	//Exit and jump to the next element
	    	e.stop();
	    	this.clearActiveSnip();
	    	if( this.options.next ) $(this.options.next).focus();
	    	return;

	    }
	    
	    var self = this,
	    	txta = self.textarea,
	    	el = txta.toElement(),
	    	caret = txta.getSelectionRange(),
			top = el.scrollTop, 
			left = el.scrollLeft;
	    
		if( this.directSnippet(e, txta, caret) ) return;

		el.focus();

		switch( e.key ){

			case 'tab': 

				e.stop();
	
				if( self.activeSnip ){

					return self.nextParameter(txta, caret);

				} else {
				
					if( self.tabSnippet(e, txta, caret) ) return;

					self.convertTabToSpaces(e, txta, caret);

				}
				
				break;

			case 'up' :
			case 'down': 
			case 'esc': self.clearActiveSnip(); return;

			case 'enter': self.onEnter(e, txta, caret); break;

			case 'delete': self.onDelete(e, txta, caret); break;

			case 'backspace': self.onBackspace(e, txta, caret); break;

		}

		el.scrollTop  = top;
		el.scrollLeft = left;

	},
	
	/*
	Function: onEnter
		When the Enter key is pressed, the next line will be ''auto-indented''
		or space-aligned with the previous line.

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection
	*/
	onEnter: function(e, txta, caret) {
		
		//if( this.activeSnip ){
			//fixme
			//how to 'continue previous snippet ??
			//eg '\n* {unordered list item}' followed by TAB or ENTER
			//snippet should always start with \n;
			//snippet should have a 'continue on enter' flag ?
		//}

		this.clearActiveSnip();
		
		if( caret.thin ){

			var fromStart = txta.getFromStart(),
				prevline = fromStart.split('\n').pop(),
				indent = prevline.match( /^\s+/gi );

			if( indent ){
				e.stop(); 
				txta.insertAfter( '\n' + indent.join('') );
			}

		}
	},
	
	/*
	Function: onBackspace
		Remove single-character directsnips such as {{ (), [], {} }}

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection
	*/
	onBackspace: function(e, txta, caret) {

		if( caret.thin  && (caret.start > 0) ){

			var key = txta.getValue().charAt(caret.start-1), 
				snip = this.getSnippet( this.options.directsnips );

			if( snip && (snip.snippet == txta.getValue().charAt(caret.start)) ){

				/* remove the closing pair character */
				txta.setSelectionRange( caret.start, caret.start+1 )
					.setSelection('');

			}
  		}
  	},

	/*
	Function: onDelete
		Removes the next TAB (4spaces) if matched

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection
	*/
	onDelete: function(e, txta, caret) {

		var tab = this.options.tab;

		if( caret.thin	&& (txta.getTillEnd().indexOf(tab) == 0) ){

			e.stop();
			txta.setSelectionRange(caret.start, caret.start + tab.length)
				.setSelection('');

		}
	},	


	/*
	Function: toggleFindAndReplace
		Toggles the visibility of the find and replace dialog.
		This event handler is linked with the ''find'' button.

	Arguments:
		e - event
	*/
	toggleFindAndReplace: function(e){

		if(e) new Event(e).stop();

		var form = this.options.findForm,
			dialog = form.findDialog,
			showDialog = ( dialog.getStyle('display')=='none' );
			
		dialog.setStyle( 'display', showDialog ? '' : 'none' );	
		showDialog ? form.findInput.focus() : this.toElement().focus();
		
	},
	
	/*
	Function: onFindAndReplace
		Perform the find and replace operation on either the full textarea
		or the selection of the textarea. It supports 
		regular expressions, case-(in)sensitive replace and global replace.
		This is an event handler, typically linked with the submit button of the
		find and replace dialog. 

	Arguments:
		e - event
	*/
	onFindAndReplace: function(e){

		if(e) new Event(e).stop();

		var txta = this.textarea,
			fo = this.options.findForm,
			findText	= (fo.findInput) ? fo.findInput.value : '',
			replaceText	= (fo.replaceInput) ? fo.replaceInput.value : '',
			isRegExp	= (fo.isRegExp) ? fo.isRegExp.checked : false,
			reGlobal	= (fo.isReplaceGlobal && fo.isReplaceGlobal.checked) ? 'g' : '',
			reMatchCase	= (fo.isMatchCase && fo.isMatchCase.checked) ? '' : 'i';
			
		if( findText == '' ) return;

		var sel = txta.getSelection() || '',
			data = (sel=='') ? txta.getValue() : sel;
			
		if( !isRegExp ) findText = findText.escapeRegExp();
		
		var re = new RegExp(findText, reGlobal + reMatchCase + 'm'); //multiline

		if( data.match(re) ){

			this.undoredo.onChange();
			data = data.replace(re, replaceText);  
			(sel=='') ? txta.toElement().value = data : txta.setSelection(data);			

		} else {

			(fo.msgNoMatchFound) ? fo.msgNoMatchFound() : alert("No match found");

		}
		txta.toElement().focus();
	},

	/*
	Function: setActiveSnip
		The activeSnip contains an array with ''{parameters}'' used to step through 
		each parameter when pressing subsequent tabs. 
		It also contains a ref to the related snippet and snippet key.
		See also nextParameter(..)
		As long the snippet is active, the textarea gets the css class {{activeSnip}}.

	Arguments:
		snip - snippet object to make active
	*/
	setActiveSnip: function( snip ){

		var self = this;

		if( self.options.tabcompletion ){
			self.activeSnip = snip ;
			self.toElement().addClass('activeSnip');
		}
	},

	/*
	Function: clearActiveSnip
		It clears the activeSnip object, and removes the css class from the textarea.
		It also makes sure there no stuff left in the suggestions box
	*/
	clearActiveSnip: function(){

		var self = this;
		
	    self.activeSnip = null;
		self.hideDialog();
		self.toElement().removeClass('activeSnip').focus();
	},

	/*
	Function: getSnippet
		Retrieve and validate the snippet. Returns false when the snippet is not
		found or not in scope.

	About snippets:
	In the simplest case, you can use snippets to insert plain text that you do not 
	want to type again and again. The snippet is expanded when hitting 
	the Tab key: the ''snippet'' is replaced by ''snippet expansion text''.
	
	(start code)
	var tabSnippets = {
		<snippet1> : <snippet expansion text>,
		<snippet2> : <snippet expansion text>
	}
	(end)

	See also [DirectSnippets].

	For example, following snippet will expand the ''toc'' text into the 
	TableOfContents wiki plugin call. Don't forget to escape '{' and '}'  
	with a backslash, because they have a special meaning. (see below)
	Use the '\n' charater to define multi-line snippets. Start the snippet
	with '\n' to make sure the snippet starts on a new line.

	(start code)
	"toc": "\n[\{TableOfContents \}]\n"
	(end)

	After tab-completion, the caret is placed just after the expanded snippet. 

	Snippet parameters:
	If you want, you can put ''{parameters}'' inside the snippet. Pressing the tab
	will jump to the next parameter. If you are ok with the default value, 
	just tab over it. If not, start typing to overwrite it.
	
	(start code)
	"bold": "__{some bold text}__"
	(end)
   
	You can have multiple ''{parameters}'' too. Pressing more tabs will get you there.

	(start code)
	"link": "[{link text}|{pagename}]"
	(end)

	Extended snippet syntax:
	So far we discussed the simple snippet syntax. In order to unlock more advanced 
	snippet features, you'll need to use the extended snippet syntax.
	
	(start code)
	"toc": {
		snippet : "\n[\{TableOfContents \}]\n"
	}
	(end)

	which is actually the same as

	(start code)
	"toc": "\n[\{TableOfContents \}]\n"
	(end)

	Snippet synonyms:
	Instead of defining the snippet text, you can also refer to another snippet.
	This allows you to create synonyms.
	
	(start code)
	"allow": { 
		synonym: "acl" 
	}
	(end)

	Dynamic snippets:
	Next to static snippet texts, you can also dynamically generate 
	the snippet text through a javascript function. For example, you could
	use ajax calls to populate the snippet on the fly. The function should return
	either the string (simple snippet syntax) or a snippet object.
	(eg return {{ { snippet:"..." } }} )
	
	(start code)
	"date": function(e, textarea){
		return new Date().toLocaleString();
	}
	(end)

	or

	(start code)
	"date": function(e, textarea){
		var d = new Date().toLocaleString();
		return { 'snippet': d };
	}
	(end)

	Snippet scope:
	See [inScope] to see how to restrict the scope of a snippet.

	Parameter dialog boxes:
	To help the entry of parameters, you can specify a predefined set of choices
	for a ''{parameter}'', as a string (with | separator), js array or js object.
	A parameter dialog box will be displayed to provide easy selection of 
	one of the choices.  See [SelectionDialog].
	
	Example of parameter suggestion-list:

	(start code)
	"acl": {
		snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
		permission: "view|edit|modify|comment|rename|upload|delete",
		"principal(,principal)": "Anonymous|Asserted|Authenticated|All"
		}
	}
	"acl": {
		snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
		permission: [view,edit,modify]
		}
	}
	"acl": {
		snippet: "[\{ALLOW {permission} {principal(,principal)} \}]",
		permission: {'Only read access':'view','Read and write access':'edit','R/W, rename, delete access':'modify' }
		}
	}
	(end)
	
	You can also define global dialog boxes via the ''dialogs'' option, which
	can be re-used in any snippet.	
	
	(start code)
	new SnipEditor( $('myTextarea'), {
		...	
		tabSnippet: {
			acl: "[\{ALLOW {permission} \}]"
		},
		dialogs: {
			permission: new SelectionDialog({body:[view,edit,modify], caption:'Permission'})
		},
		...
	};
	(end)


	Arguments:
		snips - snippet collection object for lookup of the key
		key - (optional) snippet key. If not present, retreive the key from 
		 	the textarea just to the left of the caret. (i.e. tab-completion)

	Returns:
		Retrun a snippet object or false.
		(start code)
		returned_object = false || {
				key: "snippet-key",
				snippet: " snippet-string ",
				text: " converted snippet-string, no-parameter braces, auto-indented ",
				parms: [parm1, parm2, "last-snippet-string" ]
			}
		(end)
	*/
	getSnippet: function( snips, key ){

		var txta = this.textarea,
			fromStart = txta.getFromStart(),
			snip = false;
			
		if( key ){

			snip = snips[key];

		} else {

			//lookup key and snippet backwards from the text preceeding the caret
			var len = fromStart.length;
				
			for( var sn in snips ){
				if( (len >= sn.length) && (sn == fromStart.slice( - sn.length ) ) ){
					snip = snips[sn];
					key = sn;
					break;
				}
			}
		}

		if( snip ){
			if( snip.synonym ) snip = snips[snip.synonym];
			if( $type(snip) == 'function' ) snip = snip.apply( this, [key] );
			if( $type(snip) == 'string' ) snip = { snippet:snip };
		}
		if(!snip || !this.inScope(snip, fromStart) ) return false;

		snip.key = key;

		var s = snip.snippet,
			tab = this.options.tab,
			parms = [];

		//parse snippet and build the parms[] array with all {parameters}
		s = s.replace( /\\?\{([^{}]+)\}/g, function( match, name ){
			if( match.charAt(0) == '\\' ) return match.slice(1);
			parms.push(name);
			return name;
		}).replace( /\\\{/g, '{' );
		//and finally, replace the escaped '\{' by real '{'

		//also push the last piece of the snippet onto the parms[] array
		var last = parms.getLast();
		if(last) parms.push( s.slice(s.lastIndexOf(last) + last.length) );

		//collapse \n of previous line if the snippet starts with \n
		if( (s.indexOf('\n')==0) 
		&& ( fromStart.slice(0, -key.length ).test( /(^|\n\s*)$/g ) ) ) {
			s = s.substr(1);
		}

		//fixme: also collapse \n of subsequent lines when the snippet ends with a \n

		//auto-indent the snippet's internal \n
		var prevline = fromStart.split('\r?\n').pop(),
			indent = prevline.match(/^\s+/gi); 
		if( indent ) s = s.replace( /\n/g, '\n' + indent.join('') );

		//complete the snip object
		snip.text = s;
		snip.parms = parms;

		return snip; 				
	},
	
	/*
	Function: inScope
		Sometimes it is useful to restrict the scope of a snippet, and only perform 
		the tab-completion in specifc parts of the text. The scope parameter allows
		you to do that by defining start and end delimiting strings. 
		For example, the following "fn" snippet will only expands when it appears
		inside the scope of a script tag. 
	
		(start code)
		"fn": {
			snippet: "function( {args} )\{ \n    {body}\n\}\n",
			scope: {"<script":"</script"} //should be inside this bracket
		}
		(end)

		The opposite is possible too. Use the 'nScope' or not-in-scope parameter
		to make sure the snippet is only inserted when not in scope.
	
		(start code)
		"special": {
			snippet: "{special}",
			nScope: { "%%(":")" } //should not be inside this bracket
		},
		(end)

	Arguments:
		snip - Snippet Object
		text - (string) used to check for open scope items

	Returns:
		True when the snippet is in scope, false otherwise.
	*/
	inScope: function(snip, text){

		if( snip.scope ){

			if( $type( snip.scope )=='function' ){

				return snip.scope( this.textarea ); 

			} else {

				for( var key in snip.scope ){

					var open = text.lastIndexOf(key);
					if( (open > -1) && (text.indexOf( snip.scope[key], open ) == -1) ) return true;

				}
				return false;
			}
		}

		if( snip.nScope ){
		
			for( var key in snip.nScope ){

				var open = text.lastIndexOf(key);
				if( (open > -1) && (text.indexOf( snip.nScope[key], open ) == -1) ) return false;

			}
		
		}
		return true;
	},


	/*
	Function: directSnippet
		Direct snippet are invoked immediately when the key is pressed
		as opposed to a [tabSnippet] which are expanded after pressing the Tab key.

		Direct snippets are typically used for smart typing pairs, 
		such as {{ (), [] or {}. }}
		Direct snippets can also be defined through javascript functions
		or restricted to a certain scope. (ref. [getSnippet], [inScope] )

		First, the snippet is retrieved based on the entered character.
		Then, the opening- and closing- chars are inserted around the selection.

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection

	Returns:
		True/False, when succesfully processed the pressed key.

	Example:
	(start code)
	directSnippets: { 
		'"' : '"',
		'(' : ')',
		'{' : '}',
		"<" : ">",
		"'" : {
			snippet:"'",
			scope:{
				"<javascript":"</javascript",
				"<code":"</code",
				"<html":"</html"
			}
		}
	}
	(end)
	
	*/
	directSnippet: function(e, txta, caret){

		var snip = this.getSnippet( this.options.directsnips, e.key );
		if(!snip) return false;
		
		e.stop();

		txta.setSelection( e.key, txta.getSelection(), snip.snippet )
			.setSelectionRange( caret.start+1, caret.end+1 );

		return true;
	},

	/*
	Function: tabSnippet
		Intercept the TAB key and perform tab-completion function

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection

	Returns:
		True/False, when succesfully processed the pressed key.
	*/
	tabSnippet: function(e, txta, caret){

		if( !this.options.tabcompletion ) return;	
		            
		if( caret.thin ){
			var snip = this.getSnippet( this.options.tabsnips );
		}
		if( !snip ) return false;
		
		this.undoredo.onChange();

		//replace the snippet key by the snippet text
		txta.setSelectionRange( caret.start - snip.key.length, caret.start )
			.setSelection( snip.text );

		caret = txta.getSelectionRange(); //get updated caret

		if( snip.parms.length==0 ){

			//move caret after the inserted snippet
			txta.setSelectionRange( caret.start + snip.text.length );

		} else {

			//store the active snip and process the first {parameter}
			this.setActiveSnip( snip );
			this.nextParameter(txta, caret);

		}
		
		return true;
	},

	/*
	Function: onButtonClick
		This function is a Click event handler.
		It looks up the snippet based on the rel-attribute or the text value 
		of the clicked element, and inserts its value in the textarea.

		When text was selected prior to the click event, the selection will
		be injects in one of the snippet {parameter}. 

		Additionally, when the snippet only contains one {parameter},
		the snippet will toggle: i.e. remove the snippet when already present,
		otherwis insert the snippet.

		TODO:
		Prior to the insertion of the snippet, the caret will be moved to the beginning of the line.
		Prior to the insertion of the snippet, the caret will be moved to the beginning of the next line.

	Arguments:
		e - (event) keypress or keydown event.
	*/
	onButtonClick: function(e){

		e = new Event(e).stop();
		this.toElement().focus();

		var self = this,
			el = e.target,
			key = el.getProperty('rel') || el.getText(); 

		//toggle previously activate dialog, if it is the same command !!fixme
		if( self.activeDialog ) return self.hideDialog();

		//catch predefined commands 
		if( key=='undo' ) return self.undoredo.onUndo();
		if( key=='redo' ) return self.undoredo.onRedo();
		if( key=='find' ) return self.toggleFindAndReplace();


		var snip = self.getSnippet( self.options.tabsnips, key );
		self.relativeTo = el; //this is a button event -- used to positon the dialog 

		if( !snip ){

			//check if this is a button-only dialog command
			if( self.dialogs[key] ) self.openDialog( key );	
			return;
		}
		
		var txta = self.textarea, 
			caret = txta.getSelectionRange();

		self.undoredo.onChange();

		if( self.toggleSnippet(txta, snip, caret) ) return;

		var s = snip.text;

		if( !caret.thin ){

			//when text was selected, inject it the first undefined {parameter}
			var dialogs = self.dialogs; 

			snip.parms.slice(0,-1).some( function(parm,i){

				//skip the snippet parms and the predefined dialog parameters
				if( snip[parm] || dialogs[parm] ) return false;

				s = s.replace( parm, txta.getSelection() );
				return true;

			});
		}
 
		//now insert the snippet text
		txta.setSelection( s );

		if( snip.parms.length==0 ) {

			//when no selection, move caret after the inserted snippet,
			//otherwise leave the selected snippet unchanged
			if(caret.thin) txta.setSelectionRange( caret.start + snip.text.length );

		} else {

			//this snippet has one or more parameters
			//store the active snip and process the first {parameter}
			self.setActiveSnip( snip );
			//alert(Json.toString(snip));
			caret = txta.getSelectionRange(); //update new caret
			
			self.nextParameter(txta, caret);
	
		}

	},

	/*
	Function: toggleSnippet
		This helper function toggles a snippet. 
		The snippet should consists of exactly one parameter.		
		It is called by the onButtonClick event handler.

	Arguments:
		txta - Textarea object
		snip - Snippet object
		caret - Caret object {start, end, thin}
	*/
	toggleSnippet: function(txta, snip, caret){

		var selection = txta.getSelection();

		// First validate the toggle conditions:
		// 1) check if some text was selected, 
		// 2) check whether there is exactly one {parameter} in the snippet
		if( (selection=='') || (snip.parms.length!= 2) ) return false;
				
		var s = snip.text,
			//get the first and last textual parts of the snippet
			//the last marker already contains the final text part of the snippet
			arr = s.split( snip.parms[0] ),
			fst = arr[0],
			lst = arr[1],
			re = new RegExp( '^\\s*' + fst.trim().escapeRegExp() + '\\s*(.*)\\s*' + lst.trim().escapeRegExp() + '\\s*$' );

		// 3) check whether the toggle-strings are not empty
		if( (fst+lst)=='' ) return false;

		// remove the snippet if enclosed inside the selection
		// be clever on the whitespaces to match the selection text
		if( selection.test(re) ){

			//Remove the prefix and suffix of the snippet
			selection = selection.replace( re, '$1' );

		// extend the selection when snippet is valid just before or after the selection
		} else if( txta.getFromStart().test(fst+'$') && txta.getTillEnd().test('^'+lst) ){

			txta.setSelectionRange(caret.start-fst.length, caret.end+lst.length);
			
		} else {

			//insert snippet
			txta.setSelection( fst+lst )
				.setSelectionRange( caret.start + fst.length );
		}

		txta.setSelection( selection );	
		return true;	
	},

	/*
	Method: suggestSnippet
		Suggestion snippets are dialog-boxes appearing as you type.
		When clicking items in the suggest dialogs, content is inserted
		in the textarea.
		
		
	Example:
	> FIXME!!
	(start code)
	suggestionSnippets: [
		{ // 0 : page links  
			scope:{ '[':']'	},
			snippet: function(e, textarea){
				ajax = <retrieve suggestion list>.join('|');
				return "[{" + ajax + "}]";
			}
		},
		{ // 1 : color dialog
			scope:{ 
				'%%(color:#':';',
				'%%(background-color:#':';'
			},
			snippet: function(e,textarea){
				ajax = <retrieve suggestion list>.join('|');
				return "[{" + ajax + "}]";
			}
		}
	]
	(end)
	
	*/
	suggestSnippet: function(){
	
		var self = this,
			txta = self.textarea,
			fromStart = txta.getFromStart(),
			dialogs = self.dialogs,
			snips = self.options.suggestsnips;

		if( !self.activeSnip ){

			for( var sn in snips ){

				var snip = snips[sn];
				if( self.inScope(snip, fromStart) ){
				
					//alert('bingo '+sn);
					//get dialog
					if( !dialogs[sn] ){
						//create and cache a new dialog
						var dialog = snip.dialog;
						//alert(dialog);
						//if( !dialog ) continue; //bad snippet def.
						dialogs[sn] = snip.dialog;
					}
					self.openDialog( sn ); 
					return; //found
				}
			}
			//no suggestions in scope (anymore), so hide any active dialog
			self.hideDialog();
		}
	},

	/*
	Function: nextParameter
		Process the next ''{parameter}'' of the active snippet as you tab along
		or after you clicked a button.

	Arguments:
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection
	*/
	nextParameter: function(txta, caret){
		var parms = this.activeSnip.parms,
			parameter,
			pos;
		//if( !parms ) return;  //is always true

		while( parms.length > 0 ){

			parameter = parms.shift();
			pos = txta.getValue().indexOf(parameter, caret.start);
				
			if( pos > -1 ){
			
				//found the next {parameter} or possibly the end of the snippet
				this.hideDialog();
			
				if( parms.length > 0 ){

					// select the next {parameter}
					txta.setSelectionRange( pos, pos + parameter.length );

					//fixme: remember ever selected snippet parameter
					this.undoredo.onChange();

					// open a suggestion dialog, if any
					this.openDialog( parameter );	

					return; // and retain the activeSnip for subsequent {parameters}
				
				} else {

					// no more {parameters}, move the caret after the end of the snippet
					txta.setSelectionRange( pos + parameter.length );
					
				}
			}		
		}		

		this.clearActiveSnip();

	},

	/*
	Function: initializeResizing
		Add a resize handle afther the editable textarea.
		While dragging the textarea, also updates the size of the
		''toc'' overlay menu. Notice the addtion of 10px (8+2)
		to compensate for padding and borders. (CHECK)
		
		When done, call the onresize callback handler.
		
	Arguments:
		txta - Textarea object
	*/
	initializeResizing: function( txta ){

		var self = this,
			resizeFn = self.options.onresize;

		if( resizeFn ){

			txta.makeResizable({
				handle: new Element('div',{'class': 'resize-bar' }).injectAfter(txta), 
				modifiers: { x:false, y:'height' },
				onDrag: function() { 
					var toc = self.toc.element;
					if( toc ) toc.setStyle('height', 10+this.value.now.y ) ;
				},
				onComplete: function(){ resizeFn( this.value.now.y ) }
			});

		}
	
	},

	/*
	Function: initializeDialogs
		Prepare the set of predefined parameter dialog for fonts, colors, special.
		And mixin the parameters from the dialog options.
		
		The snipEditor dialog options contain entries xxx either 
		- a pre-created Dialog object, 
		- an array with ~[Dialog-object, {Dialog options}~]
			This will be initialised during the first invocation of the dialog.
		- a set of ''{dialog-options}'' which will overwrite some of the options of  
			the predefined dialogs suchs as Font, Color and Special.
	*/
	initializeDialogs: function(){
	
		var select = this.onSelectDialog.bind(this),
			change = this.onChangeDialog.bind(this);
			
		this.dialogs = {
				fonts: [FontDialog, {caption:'Fonts', autoClose:true, onSelect:select}],
				colors: [ColorDialog, {colorImage:'../dialog/circle-256.png', onChange:change}],
				special: [CharsDialog, {caption:'Special Chars', autoClose:true, onSelect:select}]
		};

		//process the snipeditor.options.dialogs		
		var dlgs = this.options.dialogs, 
			dialogs = this.dialogs;
		for( var d in dlgs ){

			if( dialogs[d] ){

				//mixin additional settings for the predefined dialogs
				dialogs[d][1] = $merge(dialogs[d][1], dlgs[d]);

			} else {

				//add new dialogs to the predefined set
				dialogs[d] = dlgs[d];
			}
		}
	},

	/*
	Function: onSelectDialog
		Callback function when data entry via SelectionDialog was successful.
		The 'newtext' parameter will be inserted in the textarea,
		and the next parameter will be processed. (as if a tab was pressed)
		Typically, the SelectionDialogs have their autoClose=true.

	Arguments:
		newtext - string, will be inserted in the textarea
	*/
	onSelectDialog: function( newtext ){

		var txta = this.textarea;
		
		txta.setSelection( newtext )
			.setSelectionRange( txta.getSelectionRange().end );
			
		this.nextParameter(txta, txta.getSelectionRange() );

	},
	/*
	Function: onChangeDialog
		Callback function when data entry via Dialog was successful.
		The 'newtext' parameter will be inserted in the textarea.
		In this case, the dialog typically remains open.
		New changes will simply overwrite the previous ones.

	Arguments:
		newtext - string, will be inserted in the textarea
	*/
	onChangeDialog: function( newtext ){

		this.textarea.setSelection( newtext );

	},
	

	/*
	Function: openDialog
		Turn the snippet parameter definition into a suggestion popup,
		to assist the entry of the parameter.
		Attach onclick events which invoke the callback(replacement-text).

		See also [../dialog/dialog.js#SelectionDialog]

	Arguments:
		parameter - todo
		callback - js-function, will be called to insert text in the textarea.
			The callback is typically attached as on-click event handler to DOM elements
	*/
	openDialog: function( parm ){

		var self = this,
			suggest = self.activeSnip ? self.activeSnip[parm] : false,
			dialog = self.dialogs[parm]; //find predefined dialog

		if( suggest ){

			if( $type(suggest) == 'function' ) suggest = suggest();

			//suggestion defined inside the snippet takes precedences.
			dialog = new SelectionDialog({
				body: suggest,
				caption: parm, 
				onSelect: self.onSelectDialog.bind(self)
			});	

		} else if( dialog ){

			// lazy creation of the Dialog object
			if( $type(dialog) == 'array' ){

				dialog = self.dialogs[parm] = new dialog[0](dialog[1]);

			}

		} else {

			return;

		}

		//self.hideDialog(); //make sure the previous one is closed
		if( self.activeDialog && (dialog != self.activeDialog) ){ self.hideDialog(); }
		if( self.relativeTo) dialog.setPosition( self.relativeTo );
		self.relativeTo = null;
		self.activeDialog = dialog.show();

	},
	
	/*
	Function: hideDialog
		Clear the active dialog and hide it.
	*/
	hideDialog: function(){

		var active = this.activeDialog;
		if( active ){ 
			active.hide();
			this.activeDialog = null;
		}

	},
	
	/*
	Function: initializeToc
		Initialize the Table-Of-Contents of the textarea.
		This clickable TOC allows the user to quickly zoom-in or
		swith between section of the textarea.
		
		The sections are delimitted by header lines.
		
		The textarea is cloned into a main and work area.
		The workarea is used for actual editing.
		The mainarea reflects at all times the whole document.

		The TOC is injected in a #snipetoc element, with absolute position,
		such that is mapped perfectly behind the visible textarea.
		The #snipetoc is resized equally to the textarea.
		
	*/
	initializeToc: function( txta ){
		
		var self = this,
			onChangeTXTA = self.onChangeTXTA.bind(self),
			buildToc = self.buildToc.bind(self);
			
		if( self.options.toc.tocParser ){

			self.toc = { /*items:[], begin:0, end:0,*/ cursor:0 };

			self.toc.element = new Element('div',{
				'id':'snipetoc',
				'class':'hidden',
				'styles':{
					//force initial height equal to the heigth of the textarea
					//add 10px: 4px*2 for textarea padding, 2px for textarea border
					'height': 10+txta.getStyle('height').toInt()
				},
				'events':{
					'mouseenter':function(){ 
						onChangeTXTA();
						$('snipetoc').removeClass('hidden'); 
					},
					'mouseleave':function(){ $('snipetoc').addClass('hidden'); }
				}
			}).adopt( self.toc.elements = new Element('ul') )
				.injectBefore( txta );
				
			buildToc(); /*CHECK*/

		}
	},	
	
	/*
	Function: buildToc
		UPDATE/RFEFRESH the textarea table-of-contents selection list
		This function is called at startup, and everytime the section textarea changes.
		
	Postcondition: 
		The section-edit dropdown contains following entries
		* 0: ( all )
		* 1: start-of-page (if applicable)
		* 2..n: page sections
	 */  
	 buildToc: function(){

		var self = this,
			toc = self.toc,
			options = self.options.toc,

			newItem = function(item, index){
				return new Element('li').adopt(
					new Element('a',{
						'class':'action',
						'styles': {	'padding-left':(item.indent+0.5)+'em' },
						'title':item.title,
						'events':{
							'click':self.selectTocItem.pass([index], self) 
						}
					}).setHTML(item.title.trunc(30))
				); 
			},

			liArr = [ newItem({title: options.all, start:0, indent:0}, -2) ],
			//tocParser: [ {title:text, start:char-offset, indent:indentation-level}, ... ]
			items = options.tocParser( self.mainarea.value );

		toc.items = items;

		if( items.length>0 ){

			if( items[0].start>0 ){
				liArr.push( newItem({title: options.startOfPage, start:0, indent:0}, -1) );
			}

			items.each( function(item, index){ 
				liArr.push( newItem( item, index ) ); 
			},self);

		}
		toc.elements.empty().adopt( liArr );
		self.tocCursor(toc.cursor);

	},

	/* 
	Function: tocCursor

	Arguments:
		cursor - index of the display table of contents item
		-2 - show the complete content (all sections)
		-1 - show the "start of page" content, prior to the first section
		0..n - show the nth section of the textarea
	*/
	tocCursor: function( cursor ){

		var toc = this.toc,
			els = toc.elements.getChildren(),
			offset = ((toc.items.length==0) || (toc.items[0].start>0)) ? 2 : 1;
			
		if( cursor < -2 ) cursor = -2;
		this.toc.cursor = cursor;

		els.removeClass('cursor');
		if( els[cursor+offset] ) els[cursor+offset].addClass('cursor'); 

		$('snipetoc').addClass('hidden');
	},

	/* 
	Function: selectTocItem
		Copy the right section of the main textarea to the work
		textarea.

		This event handler is called when the user clicks one of the 
		entries of the Table Of contents. 

	Arguments:
		cursor - index of the display table of contents item
		-2 - show the complete content (all sections)
		-1 - show the "start of page" content, prior to the first section
		0..n - show the nth section of the textarea
	*/
	selectTocItem: function( cursor ){

		var txta = this.toElement(), 
			main = this.mainarea.value,
			toc = this.toc,
			items = toc.items;
			
		//FIXME : what if no toc undefined : eg wiki-comments.	

		//default : show all
		toc.begin = 0;
		toc.end = main.length;


		if( cursor == -1 ){
			//show Start Of Page, prior to the first toc-entry
			toc.end = items[0].start;

		} else if(cursor >= 0  && cursor < items.length){

			toc.begin = items[cursor].start; 
			if( cursor+1 < items.length ) toc.end = items[cursor+1].start;

		} else {

			cursor = -2;

		}
		
		this.tocCursor( cursor );

		txta.value = main.slice( toc.begin, toc.end );
		txta.focus();

	},

	/*
	Function: onChangeTXTA
		This event handler is invoked when text is changed in the textarea.
		It's main duty is to copy the textarea back into the main textarea,
		on the right location.  When done, the snip toc is refreshed.		
		
		This {{change}} event fires when: 
		# {{change}} event on the textarea
		  This event also fires just before the form is submitted. 
		# user clicks a toolbar-button

		This handler is also invoked when the {{mouseover}} event fires
		to make the toc menu visible.

	*/
	onChangeTXTA: function(){	

		var	txta = this.toElement(),	
			main = this.mainarea,
			toc = this.toc;

		if( toc && (txta.value != toc.cache) ){

			toc.cache = txta.value;

			var	s = main.value,
				//insert \n to ensure the next section always starts on a new line.
				linefeed = (txta.value.slice(-1) != '\n')  ? '\n' : '';
			
			main.value = s.slice(0, toc.begin) + txta.value + linefeed + s.slice(toc.end);
			toc.end = toc.begin + txta.value.length;

			this.buildToc();  //now build a new table of contents
		}
	 },

	/*
	Function: convertTabToSpaces
		Convert tabs to spaces. When no snippets are detected, the default
		treatment of the TAB key is to insert a number of spaces.
		Indentation is applied in case of multi-line selections. 

	Arguments:
		e - event
		txta - Textarea object
		caret - caret object, indicating the start/end of the textarea selection
	*/
	convertTabToSpaces: function(e, txta, caret){

		var tab = this.options.tab,
			selection = txta.getSelection();

		//handle multi-line selection
		if( selection.indexOf('\n') != -1 ){

			var b = txta.isCaretAtStartOfLine();
			if( b ) selection = '\n' + selection;

			if( e.shift ){

				//shift-tab: remove leading tab space-block
				selection = selection.replace(new RegExp('\n'+tab,'g'),'\n');

			} else {

				//tab: auto-indent by inserting a tab space-block
				selection = selection.replace(/\n/g,'\n'+tab);

			}

			txta.setSelection( b ? selection.slice(1) : selection );

		} else {

			var	fromStart = txta.getFromStart();

			if( e.shift ){

				//shift-tab: remove 'backward' tab space-block
				if( fromStart.match( tab + '$' ) ){

					txta.setSelectionRange( caret.start - tab.length, caret.start )
						.setSelection('');

				}

			} else {

				//tab: insert a tab space-block
				txta.setSelection( tab )
					.setSelectionRange( caret.start + tab.length );

			}

		}
	},

	/*
	Function: getState
		Get the current state of the SnipEditor which consist of
		the content and selection of the textarea.
		It implements the ''Undoable'' interface called from the 
		[UndoRedo] class.
	*/
	getState: function(){

		var txta = this.textarea,
			el = txta.toElement(); 

		return { 
			main: this.mainarea.value,
			value: el.getValue(), 
			cursor: txta.getSelectionRange(), 
			scrollTop: el.scrollTop, 
			scrollLeft: el.scrollLeft
		};
	},

	/*
	Function: putState
		Set a state of the SnipEditor. 
		This works in conjunction with the [UndoRedo] class.
	*/
	putState: function(o){

		var self = this,
			txta = self.textarea,
			el = txta.toElement(); 

		self.clearActiveSnip();
		self.mainarea.value = o.main;
		el.value = o.value;
		el.scrollTop = o.scrollTop;
		el.scrollLeft = o.scrollLeft;
		txta.setSelectionRange( o.cursor.start, o.cursor.end )
			.fireEvent('change');
	}

});
SnipEditor.implement(/*new Events,*/ new Options);


/*
Class: Textarea
	The textarea class enriches a TEXTAREA element, and provide cross browser
	support to handling the selected text: get and set the selected text,
	changing the selection, etc.

Arguments:
	el - textarea element

Example:
	(start code)
	<script>
		var txta = new Textarea( "mainTextarea" );
  	</script>
	(end)
*/
var Textarea = new Class({

	initialize: function(el){
		this.ta = $(el);
		return this;
	},

	/*
	Function: toElement
		Return the DOM textarea element.
		This allows the dollar function to return 
		the element when passed an instance of the class. (mootools 1.2.x)
		
	Example:
	>	var txta = new Textarea('textarea-element');
	>	$('textarea-element') == txta.toElement();
	>	$('textarea-element') == $(txta); //mootools 1.2.x
	*/
	toElement: function(){
		return this.ta;
	},

	/*
	Function: getValue
		Returns the value (text content) of the textarea.
	*/
	getValue: function(){
		return this.ta.value;
	},

	/*
	Function: getFromStart
		Returns the first not selected part of the textarea, till the start of the selection.
	*/
	getFromStart: function(){
		return this.ta.value.slice( 0, this.getSelectionRange().start );
	},

	/*
	Function: getTillEnd
		Returns the last not selected part of the textarea, starting from the end of the selection.
	*/
	getTillEnd: function(){
		return this.ta.value.slice( this.getSelectionRange().end );
	},
	
	/*
	Function: getSelection
		Returns the selected text as a string
	
	Note:
		IE fixme: this may return any selection, not only selected text in this textarea 
			//if(window.ie) return document.selection.createRange().text;
	*/
	getSelection: function(){

		var cur = this.getSelectionRange();
		return this.ta.getValue().slice(cur.start, cur.end);

	},
	
	/*
	Function: setSelectionRange 
		Selects the selection range of the textarea from start to end

	Arguments:
		start - start position of the selection
		end - (optional) end position of the seletion (default == start)

	Returns:
		Textarea object
	*/
	setSelectionRange: function(start, end){

		var txta = this.ta;
		if(!end) end = start;

		if($defined(txta.setSelectionRange)){

			txta.setSelectionRange(start, end);

		} else {

            var value = txta.value,
            	diff = value.substr(start, end - start).replace(/\r/g, '').length;

            start = value.substr(0, start).replace(/\r/g, '').length;
           
			var range = txta.createTextRange();
			range.collapse(true);
			range.moveEnd('character', start + diff);
			range.moveStart('character', start);
			range.select();
			//textarea.scrollTop = scrollPosition;
			//textarea.focus();

		}
		return this;
	},

	/*
	Function: getSelectionRange
		Returns an object describing the textarea selection range. 

	Returns:
		{{ { 'start':number, 'end':number, 'thin':boolean } }}
		start - coordinate of the selection
		end - coordinate of the selection
		thin - boolean indicates whether selection is empty (start==end)
	*/

/* ffs
	getIERanges: function(){
		this.ta.focus();
		var txta = this.ta,
			range = document.selection.createRange(),
			re = this.createTextRange(),
			dupe = re.duplicate();
		re.moveToBookmark(range.getBookmark());	
		dupe.setEndPoint('EndToStart', re);
		return { start: dupe.text.length, end: dupe.text.length + range.text.length, length: range.text.length, text: range.text };
	},
*/
	getSelectionRange: function(){

		var txta = this.ta,
			pos = {start: 0, end: 0, thin: true};

		if( $defined(txta.selectionStart) ){

			pos = { start: txta.selectionStart, end: txta.selectionEnd };

		} else {
		
	  		var range = document.selection.createRange();
			if (!range || range.parentElement() != txta) return pos;
	 		var dup = range.duplicate(),
				value = txta.value,
				offset = value.length - value.match(/[\n\r]*$/)[0].length;

			dup.moveToElementText(txta);
			dup.setEndPoint('StartToEnd', range);
			pos.end = offset - dup.text.length;
	  		dup.setEndPoint('StartToStart', range);
			pos.start = offset - dup.text.length;

		}

		pos.thin = (pos.start==pos.end);
		return pos;
	},

	/*
	Function: setSelection 
		Replaces the selection with a new value (concatenation of arguments).
		On return, the selection is set to the replaced text string.

	Arguments:
		string - string to be inserted in the textarea.
			If multiple arguments are passed, all strings will be concatenated.

	Returns:
		Textarea object, with a new selection

	Example:
		> txta.setSelection("new", " value"); //replace selection by 'new value'
	*/
	setSelection: function(){

		var value = $A(arguments).join('').replace(/\r/g, ''),
			txta = this.ta,
			scrollTop = txta.scrollTop; //cache top
		 
		if( $defined(txta.selectionStart) ){

			var start = txta.selectionStart, 
				end = txta.selectionEnd,
				v = txta.value;
			txta.value = v.substr(0, start) + value + v.substr(end);
			txta.selectionStart = start;
			txta.selectionEnd = start + value.length;

		} else { 

			txta.focus();
			var range = document.selection.createRange();
			range.text = value;			
			range.collapse(true);
			range.moveStart("character", -value.length);
			range.select();

		}
		txta.focus();
		txta.scrollTop = scrollTop;		
		txta.fireEvent('change');
		return this;

	},

	/*
	Function: insertAfter
		Inserts the arguments after the selection, and puts caret after inserted value

	Arguments:
		string( one or more) - string to be inserted in the textarea.

	Returns:
		Textarea object
	*/
	insertAfter: function(){
	
		var value = $A(arguments).join('');

		return this.setSelection( value )
			.setSelectionRange( this.getSelectionRange().start + value.length );				

	},
	
	/*
	Function: isCaretAtStartOfLine
		Returns boolean indicating whether caret is at the start of newline
	*/
	isCaretAtStartOfLine: function(){

		var i = this.getSelectionRange().start;
	    return( (i<=0) || ( this.ta.value.charAt( i-1 ).test( /[\n\r]/ ) ) );

	}
	
});
Textarea.implement(new Events);


/*
Class: UndoRedo
	The UndoRedo class implements a simple undo/redo stack to save and restore
	the state of an 'undo-able' object.
	The object needs to provide a {{getState()}} and a {{putState(obj)}} methods.
	Whenever the object changes, it should call the UndoRedo onChange() handler.
	Optionally, event-handlers can be attached for undo() and redo() functions.

Arguments:
	obj - the undo-able object
	options - optional, see options below

Options:
	maxundo - integer , maximal size of the undo and redo stack (default 20)
	redo - (optional) DOM element, will get a click handler to the redo() function
	undo - (optional) DOM element, will get a click handler to the undo() function

Example:
	(start code)
	<script>
		var undoredo = new UndoRedo(this, {
			redoElement:'redoID', 
			undoElement:'undoID' 
		});

		//when a change occurs on the calling object which needs to be persisted
		undoredo.onChange( );
  	</script>
	(end)
*/
var UndoRedo = new Class({

	options: {
		maxundo:40
	},
	initialize: function(obj, options){

		this.setOptions(options);
		this.obj = obj; 
		this.redo = [];
		this.undo = [];

		this.redoEL = $(this.options.redo);
		if(this.redoEL) this.redoEL.addEvent('click', this.onRedo.bind(this) );

		this.undoEL = $(this.options.undo);
		if(this.undoEL) this.undoEL.addEvent('click', this.onUndo.bind(this) );

		this.buttonCSS();
	},

	/*
	Function: onChange
		Call the onChange function to persist the current state of the undo-able object.
		The UndoRedo class will call the {{obj.getState()}} to retrieve the state info.
		
	Arguments:
		state - (optional) state object to be persisted. If not present,
			the state will be retrieved via a call to the {{obj.getState()}} function.
	*/
	onChange: function(state){

		this.undo.push( state || this.obj.getState() );
		this.redo = [];

		if(this.undo.length > this.options.maxundo) this.undo.shift();

		this.buttonCSS();
	},

	/*
	Function: onUndo
		Click event-handler to recall the state of the object
	*/
	onUndo: function(e){

		if(e) new Event(e).stop();

		if(this.undo.length > 0){
			this.redo.push( this.obj.getState() );
			this.obj.putState( this.undo.pop() );
		}

		this.buttonCSS();
	},	

	/*
	Function: onRedo
		Click event-handler to recall the state of the object after a previous undo action.
		The state will be reset by means of the {{obj.putState()}} method
	*/
	onRedo: function(e){

		if(e) new Event(e).stop();

		if(this.redo.length > 0){
			this.undo.push( this.obj.getState() );
			this.obj.putState( this.redo.pop() );
		}

		this.buttonCSS();
	},
	
	/*
	Function: buttonCSS
		Helper function to change the css style of the undo/redo buttons.
	*/
	buttonCSS: function(){
		
		if(this.undoEL) this.undoEL[ (this.undo.length == 0) ? 'addClass' : 'removeClass' ]('disabled');
		if(this.redoEL) this.redoEL[ (this.redo.length == 0) ? 'addClass' : 'removeClass' ]('disabled');

	}

});
UndoRedo.implement(new Options);
