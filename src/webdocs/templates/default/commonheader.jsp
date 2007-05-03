<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%-- 
   This file provides a common header which includes the important JSPWiki scripts and other files.
   You need to include this in your template, within <head> and </head>.  It is recommended that
   you don't change this file in your own template, as it is likely to change quite a lot between
   revisions.
   
   Any new functionality, scripts, etc, should be included using the TemplateManager resource
   include scheme (look below at the <wiki:IncludeResources> tags to see what kind of things
   can be included).
--%>
<%-- CSS stylesheet --%>
<link rel="stylesheet" media="screen, projection, print" type="text/css" 
     href="<wiki:Link format="url" templatefile="jspwiki.css"/>"/>
<wiki:IncludeResources type="stylesheet"/>
<wiki:IncludeResources type="inlinecss" />

<%-- JAVASCRIPT --%>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools.js'/>"></script>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-common.js'/>"></script>
<wiki:CheckRequestContext context='edit|comment'>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-edit.js'/>" ></script>
</wiki:CheckRequestContext>
<%-- TODO
<wiki:CheckRequestContext context='viewGroup|editGroup|deleteGroup|createGroup|prefs'>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-prefs.js'/>" ></script>
</wiki:CheckRequestContext>
--%>
<wiki:IncludeResources type="script"/>

<%-- COOKIE read client preferences --%>
<%
  /* cookie-format:
   * skinname DELIM dateformat DELIM timezone DELIM editareaheight DELIM editortype
   */
  String DELIM  = "\u00a0";
  String prefSkinName = null;
  String prefDateFormat = "dd-MMM-yyyy HH:mm"; /* TODO should this be part of default.properties ??*/
  String prefTimeZone = java.util.TimeZone.getDefault().getID(); /* TODO */
  String prefEditAreaHeight = "24"; 

  Cookie[] cookies = request.getCookies();
  if (cookies != null)
  {
    for (int i = 0; i < cookies.length; i++)
    {
       if( "JSPWikiUserPrefs".equals( cookies[i].getName() ) )
       {
          String s = TextUtil.urlDecodeUTF8 (cookies[i].getValue() ) ;

          java.util.StringTokenizer st = new java.util.StringTokenizer (s, DELIM);

          if( st.hasMoreTokens() ) prefSkinName = st.nextToken();
          if( st.hasMoreTokens() ) prefDateFormat = st.nextToken();
          if( st.hasMoreTokens() ) prefTimeZone = st.nextToken();
          if( st.hasMoreTokens() ) prefEditAreaHeight = st.nextToken();

          break;
       }
    }
  }
  session.setAttribute("prefSkinName",       prefSkinName );
  session.setAttribute("prefDateFormat",     prefDateFormat );
  session.setAttribute("prefTimeZone",       prefTimeZone );
  session.setAttribute("prefEditAreaHeight", prefEditAreaHeight );

 %>

<script type="text/javascript">
//<![CDATA[

/* I18N
   Get the i18n scripts from the server.  This creates a new global JS variable called
   "LocalizedStrings", which is an associative array of key-value pairs.  You can then search
   this array based on the abstract key in the script file. */

 var LocalizedStrings = { 
<%
    WikiContext context = WikiContext.findContext( pageContext );
    ResourceBundle rb = context.getBundle("templates.default");
    boolean first = true;
    for( Enumeration en = rb.getKeys(); en.hasMoreElements(); )
    {
        String key = (String)en.nextElement();
        
        if( key.startsWith("javascript") )
        {
            if( first )
            {
              first = false; 
            } 
            else 
            { 
              out.println(","); 
            }
            out.println( "\""+key+"\" : \""+rb.getString(key)+"\"");
        }         
    }
%>};

/* Initialise glboal Wiki js object with server and page dependent variables */
/* FIXME : better is to add this to the window.onload handler */
Wiki.init({
	'BaseURL': '<wiki:BaseURL />',
	'TemplateDir': '<wiki:Link format="url" templatefile=""/>',
	'PageName': '<wiki:Variable var="pagename" />',/* pagename without blanks */
	'UserName':  '<wiki:UserName />',
	'DELIM': '<%=DELIM %>',
	'PrefSkinName': '<c:out value="${prefSkinName}" />',
	'PrefTimeZone': '<c:out value="${prefTimeZone}" />',
	'PrefDateFormat': '<c:out value="${prefDateFormat}" />',
	'PrefEditAreaHeight': <c:out value="${prefEditAreaHeight}" />
	});

<wiki:IncludeResources type="jsfunction"/>

/* Define additional stuff to run at page load. */
/* TODO function runOnLoad()
{ 
  TabbedSection.onPageLoad();
  SearchBox.onPageLoad();
  Wiki.onPageLoad();
  Sortable.onPageLoad();
  ZebraTable.onPageLoad();
  HighlightWord.onPageLoad();
  Collapsable.onPageLoad();
  GraphBar.onPageLoad();
  <%--wiki:IncludeResources type="jsfunction"/--%>   !!!! FIXME
}
window.onload = runOnLoad;
*/

//]]>
</script>

<meta http-equiv="Content-Type" content="text/html; charset=<wiki:ContentEncoding />" />
<link rel="search" href="<wiki:LinkTo format='url' page='FindPage'/>" 
    title="Search <wiki:Variable var="ApplicationName" />" />
<link rel="help"   href="<wiki:LinkTo format='url' page='TextFormattingRules'/>" 
    title="Help" />
<link rel="start"  href="<wiki:LinkTo format='url' page='Main'/>" 
    title="Front page" />
<link rel="stylesheet" type="text/css" media="print" href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>" 
    title="Print friendly" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki.css'/>" 
    title="Standard" />
<link rel="icon" type="image/png" href="<wiki:Link format='url' jsp='images/favicon.png'/>" />

<wiki:FeedDiscovery />


<%-- SKINS : extra stylesheets, extra javascript --%>
<c:if test='${!empty prefSkinName}'>
<link rel="stylesheet" type="text/css" media="screen, projection, print" 
     href="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefSkinName}/skin.css' />" />
<link rel="stylesheet" type="text/css" media="print" 
     href="<wiki:Link format='url' templatefile='skins/' /><:cout value='${prefSkinName}/print_skin.css' />" />
<script type="text/javascript" src="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefSkinName}/skin.js' />" ></script>
</c:if>

<%-- deprecated listSkins -> use cookie instead
<%  
   
    TemplateManager mgr = context.getEngine().getTemplateManager();
    
    Set skins = mgr.listSkins(pageContext,context.getTemplate());

    for( Iterator i = skins.iterator(); i.hasNext(); )
    {
        String skinName = (String)i.next();
%>
        <link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='<%="skins/"+skinName+"/skin.css"%>'/> title="<%=skinName%>" /> 
<%
    }
%>
--%>
