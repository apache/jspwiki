/*
 *  Contains a large amount of different UI-related utility Javascript
 *  for JSPWiki.
 *  (C) Dirk Fredericx, Janne Jalkanen 2005
 */

/**
 ** 010 String stuff
 **/

// repeat string size time
String.prototype.repeat = function( size )
{
   var a = new Array( size );
   for( var i=0; i < size; i++ ) { a[i] = this; }
   return( a.join("") );
}

// remove leading and trailing whitespace
String.prototype.trim = function() 
{
  return this.replace(/^\s+|\s+$/g,'')
}
// split CamelCase string in readable string
String.prototype.deCamelize = function()
{
  return this.replace(/([a-z])([A-Z])/g,'$1 $2');
}
// parse color : prefix with # if amtched with 3 or 6 hex codes
var REparseColor =  new RegExp( "^[0-9a-fA-F]+" );
String.prototype.parseColor = function()
{
  var s = this;
  if( ((s.length==6) || (s.length==3)) && REparseColor.test(s) ) s = "#" + s;
  return( s );
}

// replace xml chars by &entities;
String.prototype.escapeXML = function()
{
  var s = this.replace( /&/g, "&amp;" );
  s = s.replace( /</g, "&lt;" );
  s = s.replace( />/g, "&gt;" );
  return s;
}

/**
 ** 020 Array stuff
 **/
function ExtArray() {
  this.first = function() { return this[0] }
  this.last  = function() { return this[this.length-1] }
}
ExtArray.prototype = new Array();
if( !ExtArray.prototype.push ) ExtArray.push = function() {
  for (var i=0; i<arguments.length; i++) this[this.length] = arguments[i];
  return this.length;
}



/**
 ** 030 DOM document functions
 **/
 
// get text of a dhtml node
getNodeText = function( node )
{
  if( node.nodeType == 3)  return( node.nodeValue );

  var s = "";
  for( var n = node.firstChild; n ; n = n.nextSibling )
  {
    s += this.getNodeText( n );
  }
  return( s );
}

// find first ancestor element with tagName
function getAncestorByTagName( node, tagName ) 
{
  if( !node) return null;
  if( node.nodeType == 1 && (node.tagName.toLowerCase() == tagName.toLowerCase()))
    return node;
  else
    return getAncestorByTagName( node.parentNode, tagName );
}

// walk all ancestors and match node with the given classname
function getAncestorsByClassName ( node, clazz, matchFirst)
{
  var result = [];
  var re = new RegExp ('(?:^| )'+clazz+'(?: |$)');  
  while( node )
  { 
    if( re.test( node.className ) )
    {
      if( matchFirst ) return( node );
      result.push( node );
    }
    node = node.parentNode;
  }
  return( (result.length==0) ? null : result ); 
}


// returns an array of elements matching the classname
// returns only the first element matching the classname when matchFirst is true
// returns null when nothing found
function getElementsByClassName( node, clazz, matchFirst )
{
  var result = [];
  var re;
  if( clazz instanceof RegExp) { re = clazz; } 
  else if( typeof clazz == 'string' ) { re = new RegExp ('(?:^| )'+clazz+'(?: |$)'); }
  else return null;

  var n = (node.all) ? node.all : node.getElementsByTagName("*");
  for( var i=0; i<n.length; i++ )
  { 
    if( re.test(n[i].className) )  
    { 
      if( matchFirst ) return(n[i]);
      result.push(n[i]); 
    }
  }
  return( (result.length==0) ? null : result );
}

  
// className = css class name of any element
// matchFirst = true, when matching only the first occurence
document.getElementsByClassName = function( className, matchFirst ) 
{
  return getElementsByClassName( document.documentElement, className, matchFirst );
}

// tagName = name of element, like DIV...
// className = css class name
// matchFirst = true, when matching only the first occurence
document.getElementsByTagAndClassName = function( tagName, className, matchFirst )
{
  return getElementsByClassName( this.getElementsByTagName( tagName ) 
                               , className, matchFirst );
}


/**
 ** Wiki functions
 **/
var Wiki = new Object();
Wiki.DELIM = "\u00A4"; //non-typable char - used as delimitter

Wiki.reImageTypes = new RegExp( '(.bmp|.gif|.png|.jpg|.jpeg|.tiff)$','i' );

Wiki.getBaseURL  = function() { return this.BaseURL; } //not yet used
Wiki.getBasePath = function() { return this.BasePath; }
Wiki.getPageName = function() { return this.PageName; }

Wiki.showImage = function( attachment, attDELIM, maxWidth, maxHeight )
{
  // contains Name, Link-url, Info-url 
  var attachArr = attachment.value.split( attDELIM );
  var attachImg  = document.getElementById("attachImg");

  if( !attachImg ) return true;

  if( attachArr.length == 1 ) //no image selected
  {
    return;
  }

  //not clean: should actually be read from the attachImg size - but dont know yet how xbrowser
  this.maxWidth = maxWidth;
  this.maxHeight = maxHeight;

  if( !this.reImageTypes.test( attachArr[0] ) ) 
  { 
    attachImg.innerHTML  = "No image selected"; 
    return;
  }  

  this.pic = new Image();
  this.pic.src = attachArr[1];
  if( this.pic.complete ) return Wiki.showLoadedImage() ; 

  this.countdown = 30;
  setTimeout( "Wiki.showLoadedImage()" , 200 ); 
  attachImg.innerHTML= "Loading image";
}

Wiki.showLoadedImage = function ()
{
  var attachImg  = document.getElementById("attachImg");

  if( this.pic.complete ) 
  { 
    var w = parseInt(this.pic.width);
    var h = parseInt(this.pic.height);

    if( w > this.maxWidth  ) { h *= this.maxWidth/w;  w = this.maxWidth; }
    if( h > this.maxHeight ) { w *= this.maxHeight/h; h = this.maxHeight; }
    attachImg.innerHTML = "<img src='" + this.pic.src + "' width='" + parseInt(w) + "' height='" + parseInt(h)
                        + "' style='margin-top:"+ parseInt((this.maxHeight-h)/2) +"px;' ></img>";
    this.countdown = 0; 
    this.pic = null;
    return;  
  }

  if( this.countdown <= 0 ) 
  {  
    attachImg.innerHTML = "Loading image expired<br />Try loading the image manually";
    return;
  } 

  this.countdown--;
  setTimeout( "Wiki.showLoadedImage()" , 200) ; 
  attachImg.innerHTML = "Loading image " + this.countdown
}


// initialise Wiki global object
Wiki.onPageLoad = function()
{
  // mirrors commonheader.jsp !
  var c = document.getCookie( "JSPWikiUserPrefs" );
  if( c == null ) c=""; 
  var cArr = c.split(Wiki.DELIM);
  this.prefSkinName       = (cArr[0] ? cArr[0] : "PlainVanilla/SkinVanilla.css" );
  this.prefDateFormat     = (cArr[1] ? cArr[1] : "" );
  this.prefTimeZone       = (cArr[2] ? cArr[2] : "" );
  this.prefEditAreaHeight = (cArr[3] ? parseInt(cArr[3]) : 24 );
  this.prefShowQuickLinks = (cArr[4] ? (cArr[4]=="yes") : true);
  this.prefShowCalendar   = (cArr[5] ? (cArr[5]=="yes") : false);

  var u = document.getCookie( "JSPWikiUserProfile" );
  var reUsername = new RegExp ( 'username=(\\w+)' );
  this.username = ( reUsername.test(u) ) ? RegExp.$1 : null;
}

/*
 *  Chooses a suitable stylesheet based on browser.
 *
 * issue a document.write statement with the link to the browser specific stylesheet
 * should always be execute from direct javascript during page-load
 */
Wiki.loadBrowserSpecificCSS  = function ( baseurl, templatePath, pagename )
{
    var IE4 = (document.all && !document.getElementById) ? true : false;
    var NS4 = (document.layers) ? true : false;
    var IE5 = (document.all && document.getElementById) ? true : false;
    var NS6 = (document.getElementById && !document.all) ? true : false;
    var IE  = IE4 || IE5;
    var NS  = NS4 || NS6;
    var Mac = (navigator.platform.indexOf("Mac") == -1) ? false : true;

    var sheet = "";

    if( NS4 )
    {
        sheet = "jspwiki_ns.css";
    }
    else if( Mac )
    {
        sheet = "jspwiki_mac.css";
    }
    else if( IE )
    {
        sheet = "jspwiki_ie.css";
    }

    if( sheet != "" )
    {
        sheet = baseurl+"templates/" +templatePath + "/" + sheet;
        document.write("<link rel=\"stylesheet\" href=\""+sheet+"\" />");
    }
    
    this.BaseURL = baseurl;
    this.BasePath = this.BaseURL.slice( this.BaseURL.indexOf( location.host )
                                      + location.host.length, -1 );
    this.PageName = pagename;
}

/**
 ** 040  cookie stuff 
 **/
document.setCookie = function( name, value, expires, path, domain, secure )
{
  var c = name + "=" + encodeURIComponent( value );

  if( !expires )
  {
    expires = new Date();
	expires.setFullYear( expires.getFullYear() + 1 );
  }

  /* Store the cookies agains the basepath of wiki
     so that different URLformats are supported properly !
   */
  if( !path ) path = Wiki.getBasePath();

  if( expires ) { c += "; expires=" + expires.toGMTString(); } // Date()
  if( path    ) { c += "; path=" + path; }
  if( domain  ) { c += "; domain=" + domain; }
  if( secure  ) { c += "; secure"; } //true = only via https
  //alert("cookie: "+c);
  document.cookie = c;
}

document.getCookie = function( name )
{
  var reMatchCookie = new RegExp ( "(?:; )?" + name + "=([^;]*);?" );
  return( reMatchCookie.test( document.cookie ) ? decodeURIComponent(RegExp.$1) : null );
}

// Select skin
function skinSelect(skin)
{
  //var skin = document.forms["skinForm"].skinSelector;
  if (! skin) return; 

  for (var i=0; i<skin.length; i++)
  {
    if ( skin[i].selected )
    {
      document.cookie = "JspWikiSkin=" + skin[i].value + "#skin#" ;
    }
  }
  location.reload(); /* reload page */
}

/**
 ** 110 Tabbed Section
 **/
var TabbedSection = new Object(); 
TabbedSection.reMatchTabs = new RegExp( "(?:^| )tab-(\\S+)" );
TabbedSection.onPageLoad = function()
{
  var t = document.getElementsByClassName( "tabbedSection" );
  if( !t ) return;

  for( var i = 0; i<t.length; i++)
  {
    if( !t[i].hasChildNodes ) continue; //take next section
    
    t[i].className = t[i].className +" tabs";
    var tabmenu = [];
    var active = true; //first tab assumed to be the active one
    for( var n = t[i].firstChild; n ; n = n.nextSibling )
    {
      if( !this.reMatchTabs.test( n.className ) ) continue; // not a tab: take next element

      if( (n.id==null) || (n.id=="") ) n.id = n.className;
      n.style.display = ( active ? "" : "none" );

      /* <span><a class="active" href="#" id="menu-tabID" 
                 onclick="TabbedSection.onclick('tabID')" >xyz</a></span>
      */
      tabmenu.push( "<span><a class='" + ( active ? "activetab" : "" ) + "' " ); 
      tabmenu.push( "id='menu-" + n.id + "'" ); 
      tabmenu.push( "onclick='TabbedSection.onclick(\"" + n.id + "\")' >" );
      tabmenu.push( RegExp.$1.deCamelize() + "</a></span>" );
      active=false;
    }
    if( tabmenu.length == 0 ) continue; //take next section
    var e = document.createElement( "div" );
    e.className = "tabmenu" ;
    e.innerHTML = tabmenu.join( "" );
    t[i].parentNode.insertBefore( e, t[i] );

  } // take next section  

}

TabbedSection.onclick = function ( tabId )
{
  var target = document.getElementById( tabId );
  //safari and ie choke on some <a /> elements inside e.g. DiffContents.jsp
  //so it would be more safe to walk the parent-path until you find the 
  //element with classname == tabs . ugh - DF oct 2004
  var section = target.parentNode;

  if( !section ) return;
  
  for( var n = section.firstChild; n ; n = n.nextSibling )
  {
    if( !n.id ) continue;
    var m = document.getElementById( "menu-" + n.id );
    var edittab = document.getElementById( "editcontent" );
    
    if( m && m.className == "activetab" )
    {
      if( n.id == target.id ) break; //stop - is already activetab

      // Default to changing tabs if user is not asked.
      changeTabs = true;

      // If current tab is editcontent, save content in JavaScript
      // so it will still be there when you come back.
      if( edittab && edittab.style.display == "" )
      {
        // If editor has a special function for changing tabs, run it.
        if ( window.onTabChange && typeof window.onTabChange === 'function')
        {
          // Save text so that you can click tabs and come back.
          onTabChange();
        }
      }
	  
      n.style.display = "none";
      m.className = "";
      target.style.display = "";
      document.getElementById( "menu-" + target.id ).className = "activetab";
      break;
    }
  }  
}

/**
 ** 120 SearchBox
 **  Remember 10 most recent search topics
 **  Uses a cookie to store to 10 most recent search topics
 **
 **  Extensions for quick link to View Page, Edit Page, Find as is.
 **  (based on idea of Ron Howard - Nov 05)
 **/
var SearchBox = new Object();
 
SearchBox.submit = function ( queryValue )
{
  for(var i=0; i < this.recentSearches.length; i++)
  {
    if( this.recentSearches[i] == queryValue ) return;
  }

  if( !this.recentSearches ) this.recentSearches = new ExtArray();
  if( this.recentSearches.length > 9 ) this.recentSearches.pop();
  this.recentSearches.unshift( queryValue );

  document.setCookie( "JSPWikiSearchBox", this.recentSearches.join( Wiki.DELIM) );
}

SearchBox.onPageLoad = function()
{
  this.searchForm = document.getElementById("searchForm");
  if( !this.searchForm ) return;

  this.recentSearchesDIV = document.getElementById("recentSearches");
  if( !this.recentSearchesDIV ) return;

  this.recentSearches = new ExtArray();
  var c = document.getCookie( "JSPWikiSearchBox" );
  if( c ) this.recentSearches = c.split( Wiki.DELIM );
  
  var s = "";
  if( this.recentSearches.length == 0 ) return;

  var div1 = "<div onclick='SearchBox.doSearch(this)'>";
  var div2 = "</div>";
  
  var s = "Recent Searches:"; 
  var t = [];
  for( i=0; i < this.recentSearches.length; i++ )
  {
    //todo
  }
  s += div1 + this.recentSearches.join( div2+div1 ) + div2;
  s += "<br /><div onclick='SearchBox.clearRecentSearches()'>Clear Recent Searches</div>";
  this.recentSearchesDIV.innerHTML = s;
}

SearchBox.doSearch = function ( searchDiv )
{
  this.searchForm.query.value = searchDiv.innerHTML; //nodeValue seems not to work
  this.searchForm.submit();
}

SearchBox.clearRecentSearches = function()
{
  document.setCookie( "JSPWikiSearchBox", "" );
  this.recentSearches = new ExtArray();
  this.recentSearchesDIV.innerHTML = "";
}

SearchBox.navigation = function( url, pagename )
{
  var s = SearchBox.searchForm.query.value;
  if( s == 'Search' ) s = '';
  if( s == '' ) s = pagename ; //current page name
  if( s != '' ) location.href = url.replace('__PAGEHERE__', s);
  return(false); //dont exec the click on the <a href=#>
}

/**
 ** 280 ZebraTable
 ** Color odd/even rows of table differently
 ** 1) odd rows get css class odd (ref. jspwiki.css )
 **   %%zebra-table ... %%
 **
 ** 2) odd rows get css style='background=<color>'
 ** %%zebra-<odd-color> ... %%  
 **
 ** 3) odd rows get odd-color, even rows get even-color
 ** %%zebra-<odd-color>-<even-color> ... %%
 **
 ** colors are specified in HEX (without #) format or html color names (red, lime, ...)
 **
 **/

var ZebraTable = new Object();
ZebraTable.REclassName = new RegExp( "(?:^| )zebra-(\\S+)" );

ZebraTable.onPageLoad = function()
{
  var z = document.getElementsByClassName ( this.REclassName ); if( !z ) return;

  for( var i=0; i<z.length; i++)
  {
    var rows = z[i].getElementsByTagName( "TR" );  if( !rows ) continue;
    this.REclassName.test( z[i].className );
    var parms = RegExp.$1.split('-');

    if( parms[0] == 'table' )
    {
      for( var r=0; r < rows.length; r+=2 ) rows[r].className += " odd";
      continue;
    }
   
    if( parms[0] )
    { 
      for( var r=2; r < rows.length; r+=2 ) 
        rows[r].setAttribute( "style", "background:"+ parms[0].parseColor() +";" );
    }
    if( parms[1] )
    {
      for( var r=1; r < rows.length; r+=2 ) 
        rows[r].setAttribute( "style", "background:"+ parms[1].parseColor() +";" );    
    }
  }
}

/**
 ** 290 Highlight Word
 **
 ** Inspired by http://www.kryogenix.org/code/browser/searchhi/ 
 ** Modified 20021006 to fix query string parsing and add case insensitivity 
 ** Modified 20030227 by sgala@hisitech.com to skip words 
 **                   with "-" and cut %2B (+) preceding pages 
 ** Refactored for JSPWiki -- now based on regexp, by D.Frederickx. Nov 2005
 **
 **/
var HighlightWord = new Object();
HighlightWord.ClassName = "searchword";
HighlightWord.ClassNameMatch = "<span class='"+HighlightWord.ClassName+"' >$1</span>" ;
HighlightWord.ReQuery = new RegExp( "(?:\\?|&)(?:q|query)=([^&]*)", "g" );

HighlightWord.onPageLoad = function () 
{
  if( !this.ReQuery.test( document.referrer ) ) return;

  var words = decodeURIComponent(RegExp.$1);
  words = words.replace( /\+/g, " " );
  words = words.replace( /\s+-\S+/g, "" );
  words = words.replace( /([\(\[\{\\\^\$\|\)\?\*\.\+])/g, "\\$1" ); //escape metachars
  words = words.trim().split(/\s+/).join("|");
  this.reMatch = new RegExp( "(" + words + ")" , "gi");
  //alert(this.reMatch);
  
  this.walkDomTree( document.getElementById("pagecontent") );
}

// recursive tree walk matching all text nodes
HighlightWord.walkDomTree = function( node )
{
  if(!node) return;
  var nn = null; 
  for( var n = node.firstChild; n ; n = nn )
  {
    nn = n. nextSibling; /* prefetch nextSibling cause the tree will be modified */
    this.walkDomTree( n );
  }
  
  // continue on text-nodes, not yet highlighted, with a word match
  if( node.nodeType != 3 ) return; 
  if( node.parentNode.className == this.ClassName ) return;
  var s = node.nodeValue;
  s = s.escapeXML(); /* bugfix - nodeValue apparently unescapes the xml entities ?! */
  if( !this.reMatch.test( s ) ) return;
  
  //alert("found "+RegExp.$1);  
  var tmp = document.createElement("span");
  tmp.innerHTML = s.replace( this.reMatch, this.ClassNameMatch );

  var f = document.createDocumentFragment();
  while( tmp.firstChild ) f.appendChild( tmp.firstChild );

  node.parentNode.replaceChild( f, node );  
}

/**
 ** 230 Sortable -- for all tables
 **/
var Sortable = new Object();
Sortable.ClassName = "sortable";
Sortable.ClassSort           = "sort";
Sortable.ClassSortAscending  = "sortAscending";
Sortable.ClassSortDescending = "sortDescending";
Sortable.TitleSort           = "Click to sort";
Sortable.TitleSortAscending  = "Ascending order - Click to sort in descending order";
Sortable.TitleSortDescending = "Descending order - Click to sort in ascending order";


Sortable.onPageLoad = function()
{
  var p = document.getElementById( "pagecontent" ); if( !p ) return; 
  var sortables = getElementsByClassName( p, Sortable.ClassName );  if( !sortables ) return;
  for( i=0; i<sortables.length; i++ )
  {
    var table = sortables[i].getElementsByTagName( "table" )[0];
    if( !table ) continue;
    if( table.rows.length < 2 ) continue;
  
    for( var j=0; j < table.rows[0].cells.length; j++ )
    {
      var c = table.rows[0].cells[j];
      if( c.nodeName != "TH" ) break;
      c.onclick    = function() { Sortable.sort(this); } ;
      c.title      = this.TitleSort;
      c.className += " " + this.ClassSort;
    }
  }
}
Sortable.REclassName = new RegExp ('(?:^| )(sort|sortAscending|sortDescending)(?: |$)'); 
Sortable.sort = function( thNode )
{
  var table = getAncestorByTagName(thNode, "table" ); if( !table ) return;
  if( table.tBodies[0] ) table = table.tBodies[0]; //bugfix
  if( table.rows.length < 2 ) return;
  var colidx = 0; //target column to sort
  var thNodeClassName = this.ClassSort; //default column header classname
  
  //validate header row
  for( var i=0; i < table.rows[0].cells.length; i++ )
  {
    var c = table.rows[0].cells[i];
    if( c.nodeName != "TH" ) return;
    
    if( thNode == c ) 
    { 
      colidx = i; 
      if( Sortable.REclassName.test(c.className) ) thNodeClassName = RegExp.$1; 
    }
    else
    {
      c.className = c.className.replace(Sortable.REclassName, "" ) + " " + this.ClassSort ;
      c.title = this.TitleSort;
    }
  }
  
  //find body rows and guess data type of colidx
  var rows = new Array();
  var num  = true;
  var date = true;
  var ip4  = true;
  var ip4_regex = /(\d{1,3}\.){3}\d{1,3}/;
  for( var i=1; i< table.rows.length; i++)
  {
    rows[i-1] = table.rows[i] ;
    //var val = rows[i-1].cells[colidx].firstChild.nodeValue;
    var val = getNodeText( rows[i-1].cells[colidx] );
    if( num  ) num  = !isNaN( parseFloat( val ) ) ;    
    if( date ) date = !isNaN( Date.parse( val ) );
    if( ip4  ) { if( !val.match( ip4_regex )) { ip4 = false; } }
  }

  var datatype = "string";
  if( num ) datatype = "num";
  if( date ) datatype = "date";
  if( ip4 ) datatype = "ip4";

  //do the actual sorting
  if( thNodeClassName == this.ClassSort ) //first time sort of column table.sortCol == colidx ) 
  {
    rows.sort( Sortable.createCompare( colidx, datatype ) );
    thNodeClassName = this.ClassSortAscending;
    thNode.title    = this.TitleSortAscending; 
  }
  else
  { 
    rows.reverse(); 
    if( thNodeClassName == this.ClassSortAscending )
    {
      thNodeClassName = this.ClassSortDescending;
      thNode.title    = this.TitleSortDescending;
    }
    else
    {
      thNodeClassName = this.ClassSortAscending;
      thNode.title    = this.TitleSortDescending;
    }
  }
  thNode.className = thNode.className.replace(Sortable.REclassName, "") + " " + thNodeClassName ;
  
  //put the sorted table back into the document
  var frag = document.createDocumentFragment();
  for( var i=0; i < rows.length; i++ )
  {
    frag.appendChild( rows[i] );
  }
  table.appendChild( frag );
}

Sortable.convert = function( val, datatype )
{
  switch( datatype )
  {
    case "num"  : return parseFloat( val );
    case "date" : return new Date( Date.parse( val ) );
    case "ip4"   : 
		  var octet = val.split(/\./);
                  return parseInt(octet[0]) * 1000000000 + parseInt(octet[1]) * 1000000 + parseInt(octet[2]) * 1000 + parseInt(octet[3]);
    default     : return val.toString();
  }
}

Sortable.createCompare = function( colidx, datatype )
{
  return function(row1, row2)
  {
    //var val1 = Sortable.convert( row1.cells[colidx].firstChild.nodeValue, datatype );
    //var val2 = Sortable.convert( row2.cells[colidx].firstChild.nodeValue, datatype );
    var val1 = Sortable.convert( getNodeText(row1.cells[colidx]), datatype );
    var val2 = Sortable.convert( getNodeText(row2.cells[colidx]), datatype );

    if     ( val1 < val2 ) { return -1; }
    else if( val1 > val2 ) { return 1;  }
    else { return 0; }
  } 
}
/**
 ** 200 Collapsable list items 
 **
 ** See also David Lindquist <first name><at><last name><dot><net>
 ** See: http://www.gazingus.org/html/DOM-Scripted_Lists_Revisited.html
 **
 **/
var Collapsable = new Object();

Collapsable.tmpcookie    = null;
Collapsable.cookies      = [] ;
Collapsable.cookieNames  = [] ;

Collapsable.ClassName    = "collapse";
Collapsable.ClassNameBox = "collapsebox";
Collapsable.ClassNameBody= "collapsebody";
Collapsable.OpenTip      = "Click to collapse";
Collapsable.CloseTip     = "Click to expand";
Collapsable.CollapseID   = "clps"; //prefix for unique IDs of inserted DOM nodes
Collapsable.MarkerOpen   = "O";    //cookie state chars 
Collapsable.MarkerClose  = "C"; 
Collapsable.CookiePrefix = "JSPWikiCollapse";
Collapsable.bullet           = document.createElement("div"); // template bullet node
Collapsable.bullet.className = "collapseBullet";
Collapsable.bullet.innerHTML = "&bull;";


Collapsable.onPageLoad = function()
{
  this.initialise( "favorites",   this.CookiePrefix + "Favorites" );
  this.initialise( "pagecontent", this.CookiePrefix + Wiki.getPageName() );
 }


Collapsable.initialise = function( domID, cookieName )
{
  var page  = document.getElementById( domID );  if( !page ) return;
  this.tmpcookie = document.getCookie( cookieName );
  this.cookies.push( "" ) ; //initialise new empty collapse cookie
  this.cookieNames.push( cookieName );

  var nodes;
  nodes = getElementsByClassName( page, this.ClassName ); 
  if( nodes ) 
  { 
    for( var i=0; i < nodes.length; i++)  this.collapseNode( nodes[i] );
  }
  nodes = getElementsByClassName( page, this.ClassNameBox );
  if( nodes ) 
  { 
    for( var i=0; i < nodes.length; i++)  this.collapseBox( nodes[i] );
  }  
}

Collapsable.REboxtitle = new RegExp ( "h2|h3|h4" );
Collapsable.collapseBox = function( node )
{
  var title = node.firstChild; 
  while( (title != null) && (!this.REboxtitle.test( title.nodeName.toLowerCase() )) )
  {
    title = title.nextSibling;
  }
  if( !title ) return;  
  if( !title.nextSibling ) return;
  
  var body = document.createElement( "div" );
  body.className = this.ClassNameBody;
  while( title.nextSibling ) body.appendChild( title.nextSibling );
  node.appendChild( body );    

  var bullet  = this.bullet.cloneNode(true);
  this.initBullet( bullet, body, this.MarkerOpen );
  title.appendChild( bullet );
}


// Modifies the list such that sublists canbe hidden and shown by clicking the listitem bullet
// The listitem bullet is a node inserted into the DOM tree as the first child of the 
// listitem containing the sublist.
Collapsable.collapseNode = function( node )
{
  var items = node.getElementsByTagName("li");
  for( i=0; i < items.length; i++ )
  {
    var nodeLI = items[i];
    var nodeXL = ( nodeLI.getElementsByTagName("ul")[0] || 
                   nodeLI.getElementsByTagName("ol")[0] ); 

    //dont insert bullet when LI is "empty" -- iow it has no text or no non ulol tags inside
    //eg. * a listitem
    //    *** a nested list item - intermediate level is empty
    var emptyLI = true;
    for( var n = nodeLI.firstChild; n ; n = n.nextSibling )
    {
      if((n.nodeType == 3 ) && ( n.nodeValue.trim() == "" ) ) continue; //keep searching
      if((n.nodeName == "UL") || (n.nodeName == "OL")) break; //seems like an empty li 
      emptyLI = false; 
      break;
    }
    if( emptyLI ) continue; //do not insert a bullet

    var bullet  = this.bullet.cloneNode(true);
    
    if( nodeXL )
    {
      var defaultState = (nodeXL.nodeName == "UL") ? this.MarkerOpen : this.MarkerClose ;
      this.initBullet( bullet, nodeXL, defaultState );
    }
    nodeLI.insertBefore( bullet, nodeLI.firstChild ); 
  }
}


// initialise bullet according to parser settings
Collapsable.initBullet = function( bullet, body, defaultState )
{
  var collapseState = this.parseCookie( defaultState ); 
  bullet.onclick = this.toggleBullet;
  bullet.id = this.CollapseID + "." + (this.cookies.length-1) + 
                                "." + (this.cookies[this.cookies.length-1].length-1);
  this.setOpenOrClose( bullet, ( collapseState == this.MarkerOpen ), body );
}

// modify dom-node according to the setToOpen flag
Collapsable.setOpenOrClose = function( bullet, setToOpen, body )
{
  bullet.innerHTML   = (setToOpen) ? "&raquo;"      : "&laquo;" ;
  bullet.className   = (setToOpen) ? "collapseOpen" : "collapseClose" ;
  bullet.title       = (setToOpen) ? this.OpenTip   : this.CloseTip ;
  body.style.display = (setToOpen) ? "block"        : "none" ;
}
 
 
// parse cookie 
// this.tmpcookie  contains cookie being validated agains the document
// this.cookies.last contains actual cookie being constructed
//    this cookie is stored in the cookies[] 
//    and only persisted when the user opens/closes something
// returns collapseState MarkerOpen, MarkerClose
Collapsable.parseCookie = function( token )
{
  var currentcookie = this.cookies[this.cookies.length-1];
  var cookieToken = token; //default value

  if( (this.tmpcookie) && (this.tmpcookie.length > currentcookie.length) )
  {
    cookieToken = this.tmpcookie.charAt( currentcookie.length );
    if(  ( (token == this.MarkerOpen) && (cookieToken == this.MarkerClose) ) 
      || ( (token == this.MarkerClose) && (cookieToken == this.MarkerOpen) ) ) //##fixed
        token = cookieToken ; 
    if( token != cookieToken )  //mismatch between tmpcookie and expected token
        this.tmpcookie = null;
  }   
  this.cookies[this.cookies.length - 1] += token; //append and save currentcookie

  return( token );    
}


// toggle bullet and update corresponding cookie
// format of ID of bullet = "collapse.<cookies-index>.<cookie-charAt>"
Collapsable.toggleBullet = function( )
{
  var ctx = Collapsable; //avoid confusion with this == clicked bullet

  var idARR  = this.id.split(".");  if( idARR.length != 3 ) return;
  var cookie = ctx.cookies[idARR[1]]; // index in cookies array

  var body;
  if( ctx.REboxtitle.test( this.parentNode.nodeName.toLowerCase() ) )
  {
    body = this.parentNode.nextSibling;
  } 
  else
  {
    body = ( this.parentNode.getElementsByTagName("ul")[0] || 
             this.parentNode.getElementsByTagName("ol")[0] ); 
  }
  if( !body ) return;

  ctx.setOpenOrClose( this, (body.style.display == "none"), body );
  
  var i = parseInt(idARR[2]); // position inside cookie
  var c = ( cookie.charAt(i) == ctx.MarkerOpen ) ? ctx.MarkerClose : ctx.MarkerOpen; 
  cookie = cookie.substring(0,i) + c + cookie.substring(i+1) ;

  document.setCookie( ctx.cookieNames[idARR[1]], cookie );
  ctx.cookies[idARR[1]] = cookie;

  return false;  
}

/**
 ** 130 GraphBar Object : also used on the findpage
 ** %%graphBars ... %%
 ** convert numbers inside %%gBar ... %% tags to graphic horizontal bars
 ** no img needed.
 ** supported parameters: bar-color and bar-maxsize
 ** e.g. %%graphBars-e0e0e0 ... %%  use color #e0e0e0, default size 120
 ** e.g. %%graphBars-red-40 ... %%  use color red, maxsize 40 chars
 **/
var GraphBar = new Object();
GraphBar.REclassName = new RegExp( "(?:^| )graphBars(-\\S+)?" );
GraphBar.onPageLoad = function()
{
  var g = document.getElementsByClassName ( this.REclassName ); if( !g ) return;

  for( var i=0; i < g.length; i++ )
  {
    this.REclassName.test( g[i].className );
    var parms = RegExp.$1.split('-');
    var color =   ( parms[1] ? "style='background:"+parms[1].parseColor()+";color:"+parms[1].parseColor()+";' " : "" );
    var maxsize = ( parms[2] ? parseInt(parms[2],10) : 120 );

    var gBars = getElementsByClassName( g[i], "gBar" ); if( !gBars ) continue;

    var gBarD = [], maxValue = Number.MIN_VALUE; minValue = Number.MAX_VALUE;    

    for( var j=0; j < gBars.length; j++ )
    {
      var k = parseInt( getNodeText(gBars[j]),10 );
      maxValue = Math.max( maxValue, k ); 
      minValue = Math.min( minValue, k );
      gBarD[j] = k;
    }
      
    for( var j=0; j < gBars.length; j++ )
    {
      var s = ".".repeat( parseInt( maxsize * ( gBarD[j]-minValue) / maxValue ) + 1 ) ;
      gBars[j].innerHTML = " <span class='graphBar' "+color+">"+s+"</span> "+gBarD[j];
    }     
  }
}

 
