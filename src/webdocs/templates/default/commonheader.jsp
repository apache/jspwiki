<%-- 
   This file provides a common header which includes the important JSPWiki scripts and other files.
   You need to include this in your template, within <head> and </head>.  It is recommended that
   you don't change this file in your own template, as it is likely to change quite a lot between
   revisions.
   
   Any new functionality, scripts, etc, should be included using the TemplateManager resource
   include scheme (look below at the <wiki:IncludeResources> tags to see what kind of things
   can be included).
--%>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="java.util.*" %>
  <link rel="stylesheet" media="screen, projection, print" type="text/css" href="<wiki:Link format="url" templatefile="jspwiki.css"/>"/>
  <wiki:IncludeResources type="stylesheet"/>
  <script src="<wiki:Link format="url" jsp="scripts/jspwiki-common.js"/>" type="text/javascript"></script>
  <wiki:IncludeResources type="script"/>
  <wiki:IncludeResources type="inlinecss" />
  <meta http-equiv="Content-Type" content="text/html; charset=<wiki:ContentEncoding />" />
  <link rel="search" href="<wiki:LinkTo format="url" page="FindPage"/>"            title="Search <wiki:Variable var="ApplicationName" />" />
  <link rel="help"   href="<wiki:LinkTo format="url" page="TextFormattingRules"/>" title="Help" />
  <link rel="start"  href="<wiki:LinkTo format="url" page="Main"/>"                title="Front page" />
  <link rel="stylesheet" type="text/css" media="print" href="<wiki:Link format="url" templatefile="jspwiki_print.css"/>" />
  <link rel="alternate stylesheet" type="text/css" href="<wiki:Link format="url" templatefile="jspwiki_print.css"/>" title="Print friendly" />
  <link rel="alternate stylesheet" type="text/css" href="<wiki:Link format="url" templatefile="jspwiki.css"/>" title="Standard" />
  <link rel="icon" type="image/png" href="<wiki:Link format="url" jsp="images/favicon.png"/>" />
  <wiki:FeedDiscovery />
<%
  // cookie-format
  // skinname DELIM dateformat DELIM timezone DELIM editareaheight
  String DELIM  = "\u00a0";
  String prefSkinName = null;
  String prefDateFormat = "HH:mm dd-MMM-yyyy";
  String prefTimeZone = java.util.TimeZone.getDefault().getID();
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

<%
    WikiContext context = WikiContext.findContext( pageContext );
   
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

<% if(prefSkinName != null) { %>
 <link rel="stylesheet" type="text/css"
       href="<wiki:Link format='url' templatefile='<%="skins/"+prefSkinName%>'/>" />
<% } %>

<script type="text/javascript">Wiki.loadBrowserSpecificCSS("<wiki:BaseURL/>","<wiki:TemplateDir/>","<wiki:Variable var="pagename" />");</script>

<%-- Get the i18n scripts from the server.  This creates a new global JS variable called
     "LocalizedStrings", which is an associative array of key-value pairs.  You can then search
     this array based on the abstract key in the script file. --%>

<script type="text/javascript">
  var LocalizedStrings = { <%
     ResourceBundle rb = context.getBundle("templates.default");
     for( Enumeration en = rb.getKeys(); en.hasMoreElements(); )
     {
         String key = (String)en.nextElement();
         
         if( key.startsWith("javascript") )
         {
             out.println( "\""+key+"\" : \""+rb.getString(key)+"\",");
         }
     }
  %> }
</script>

<%-- Here we define the "run when the page loads" -script. --%>
<script type="text/javascript">
function runOnLoad()
{ 
  TabbedSection.onPageLoad();
  SearchBox.onPageLoad();
  Wiki.onPageLoad();
  Sortable.onPageLoad();
  ZebraTable.onPageLoad();
  HighlightWord.onPageLoad();
  Collapsable.onPageLoad();
  GraphBar.onPageLoad();
  <wiki:IncludeResources type="jsfunction"/>
}

window.onload = runOnLoad;
</script>
