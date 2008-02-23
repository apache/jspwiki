<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.*" %>
<%@ page import="com.ecyrd.jspwiki.util.*" %>
<%@ page import="com.ecyrd.jspwiki.preferences.*" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>

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
     href="<wiki:Link format='url' templatefile='jspwiki.css'/>"/>
<%-- put this at the top, to avoid double load when not yet cached --%>
<link rel="stylesheet" type="text/css" media="print" href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>" />
<wiki:IncludeResources type="stylesheet"/>
<wiki:IncludeResources type="inlinecss" />

<%-- JAVASCRIPT --%>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools.js'/>"></script>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/prettify.js'/>"></script>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-common.js'/>"></script>
<wiki:IncludeResources type="script"/>

<%-- COOKIE read client preferences --%>
<%
   Preferences.setupPreferences(pageContext);
 %>

<script type="text/javascript">
//<![CDATA[

/* Localized javascript strings: LocalizedStrings[] */
<wiki:IncludeResources type="jslocalizedstrings"/>

/* Initialise glboal Wiki js object with server and page dependent variables */
/* FIXME : better is to add this to the window.onload handler */
Wiki.init({
	'BaseUrl': '<wiki:BaseURL />',
	'PageUrl': '<wiki:Link format="url" absolute="true" page="#$%"/>', /* unusual pagename */
	'TemplateDir': '<wiki:Link format="url" templatefile=""/>',
	'PageName': '<wiki:Variable var="pagename" />',/* pagename without blanks */
	'UserName': '<wiki:UserName />', 
	'JsonUrl' : 'JSON-RPC'
	});
<wiki:IncludeResources type="jsfunction"/>

//]]>
</script>

<meta http-equiv="Content-Type" content="text/html; charset=<wiki:ContentEncoding />" />
<link rel="search" href="<wiki:LinkTo format='url' page='FindPage'/>"
    title='Search <wiki:Variable var="ApplicationName" />' />
<link rel="help"   href="<wiki:LinkTo format='url' page='TextFormattingRules'/>"
    title="Help" />
<link rel="start"  href="<wiki:LinkTo format='url' page='${wikiEngine.frontPage}' />"
    title="Front page" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>"
    title="Print friendly" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki.css'/>"
    title="Standard" />
<link rel="icon" type="image/png" href="<wiki:Link format='url' jsp='images/favicon.png'/>" />

<wiki:FeedDiscovery />

<%-- SKINS : extra stylesheets, extra javascript --%>
<c:if test='${(!empty prefs["SkinName"]) && (prefs["SkinName"]!="PlainVanilla") }'>
<link rel="stylesheet" type="text/css" media="screen, projection, print"
     href="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs["SkinName"]}/skin.css' />" />
<%--
<link rel="stylesheet" type="text/css" media="print"
     href="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs["SkinName"]}/print_skin.css' />" />
--%>
<script type="text/javascript"
         src="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs["SkinName"]}/skin.js' />" ></script>
</c:if>

<wiki:Include page="/WEB-INF/jsp/templates/default/localheader.jsp"/>