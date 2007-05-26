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
  MainareaID    : "editorarea",

  FindTextID    : "tbFIND",
  ReplaceTextID : "tbREPLACE",
  RegExpID      : "tbREGEXP",
  GlobalID      : "tbGLOBAL",
  MatchCaseID   : "tbMatchCASE",
  UndoID        : "tbUNDO",
  RedoID        : "tbREDO",
  PasteID       : "tbPASTE",  
  
  mainarea      : null,
  textarea      : null,  //either mainarea or workarea

  /*
   * initialize EditTools on pageload 
   */
  onPageLoad : function()
  {
    this.mainarea = $('editorarea'); if( !this.mainarea ) return;
    this.textarea = this.mainarea;
    
    /* add textarea resize drag bar */
	var h = new Element('div',{'class':'textarea-resizer', 'title':'edit.resize'.localize()})
		.injectAfter(this.textarea);	
	this.textarea.makeResizable({handle:h, modifiers: {x:false, y:'height'} });			
  } ,
    
  /**
   ** TOOLBAR stuff
   **/
  
  /* TOOLBAR: find&replace */
  doReplace: function( )
  {
    var findText    = $(this.FindTextID).value; if( findText == "") return;
    var replaceText = $(this.ReplaceTextID).value;
    var isRegExp    = $(this.RegExpID).checked;
    var reGlobal    = $(this.GlobalID).checked ? "g" : "";
    var reMatchCase = $(this.MatchCaseID).checked ? "" : "i";

    var sel = TextArea.getSelection(this.textarea);
    var data = ( !sel || (sel=="") ) ? this.textarea.value : sel;
  
    if( !isRegExp ) /* escape all special re characters */
    {
      var re = new RegExp( "([\.\*\\\?\+\[\^\$])", "gi");
      findText = findText.replace( re,"\\$1" );
    }
    
    var re = new RegExp( findText, reGlobal+reMatchCase+"m" ); //multiline
    if( !re.exec(data) )
    {
      alert( "edit.findandreplace.nomatch".localize() );
      return(true);
    } 
      
    data = data.replace( re, replaceText );  
  
    this.storeTextarea();
    if( !sel || (sel=="") ) { this.textarea.value = data; }
    else                    { TextArea.setSelection( this.textarea, data ); }
    if( this.textarea.onchange ) this.textarea.onchange();
    
    return( true );
  } ,
      
  /* TOOLBAR: cut/copy/paste clipboard functionality */
  CLIPBOARD : null,
  clipboard : function( format )
  {
    var s = TextArea.getSelection( this.textarea );
    if( !s || s == "") return;

    this.CLIPBOARD = s ;
    $( this.PasteID ).className = this.ToolbarMarker;
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
  storeTextarea : function()
  {
    this.UNDOstack.push( this.mainarea.value );
    $( this.UndoID ).disabled = '';
    this.REDOstack = [];
    $( this.RedoID ).disabled = 'true';
    if( this.UNDOstack.length > this.UNDOdepth ) this.UNDOstack.shift();
  } ,

  undoTextarea : function( )
  {
    if( this.UNDOstack.length > 0 ) 
    {
      $( this.RedoID ).disabled = '';
      this.REDOstack.push( this.mainarea.value );
      this.mainarea.value = this.UNDOstack.pop();
    }
    if( this.UNDOstack.length == 0 ) $( this.UndoID ).disabled = 'true';
    if( !this.selector ) return;
    this.onSelectorLoad();
    this.onSelectorChanged();
    this.textarea.focus();
  } , 
  redoTextarea : function( )
  {
    if( this.REDOstack.length > 0 ) 
    {
      $( this.UndoID ).disabled = '';
      this.UNDOstack.push( this.mainarea.value );
      this.mainarea.value = this.REDOstack.pop();
    }
    if( this.REDOstack.length == 0 ) $( this.RedoID ).disabled = 'true';
    if( !this.selector ) return;
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
  getSelection: function(id)
  {
    var f = $(id); 
    if( !f ) return ""; 
    
    if( document.selection )  //IE support
    {
      f.focus();
      return( document.selection.createRange().text );
    }
    else if( f.selectionStart 
          || f.selectionStart == '0')  //MOZILLA/NETSCAPE support
    {
      f.focus();   
      var start= f.selectionStart;
      var end  = f.selectionEnd;
      return( f.value.substr( start, end-start ) );
     } 
    else 
    {
      return( "" );
    }    
  } ,
  
  /* replaces the selection with aValue, and returns with aValue selected */
  setSelection: function(id, aValue)
  {
    var f = $(id); 
    if( !f ) return ""; 
    f.focus();
     
    if( document.selection )  //IE support
    {
      document.selection.createRange().text = aValue;
      f.focus();
    }
    else if( f.selectionStart 
          || f.selectionStart == '0')  //MOZILLA/NETSCAPE support
    {
      f.focus();   
      var start= f.selectionStart;
      var end  = f.selectionEnd;
      var top  = f.scrollTop;
      
      f.value  = f.value.substring( 0, start )
               + aValue 
               + f.value.substring( end, f.value.length );
      f.focus();
      f.selectionStart = start;
      f.selectionEnd = start + aValue.length;
      f.scrollTop = top;
    } 
    else 
    {
      f.value += value;
      f.focus();
    }
    if( f.onchange ) f.onchange();
  } ,
  
  /* check whether selection is preceeded by a \n (peek-ahead trick) */
  isSelectionAtStartOfLine : function( id )
  {
    var f = $(id); 
    if( !f ) return ''; 
    f.focus();

    if( document.selection )  //IE support
    {
      var r1 = document.selection.createRange();
      var r2 = document.selection.createRange();
      r2.moveStart( "character", -1);
      if( r1.compareEndPoints("StartToStart", r2) == 0 ) return true;
      if( r2.text.charAt(0).match( /[\n\r]/ ) ) return true;
    }
    else if( f.selectionStart 
          || f.selectionStart == '0')  //MOZILLA/NETSCAPE support
    {
      if( f.selectionStart == 0) return true;
      if( f.value.charAt(f.selectionStart-1) == "\n" ) return true;
    } ; 
    return false;
  }  
}

//*************************

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