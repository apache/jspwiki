<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.WikiContext" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%--
     This file contains the default layout used by all JSPWiki 3 pages.
     The default layout contains the HTML doctype declaration, header,
     and page layout. It can be customized in the following ways:
     
     1) Top-level JSPs can define default components, as defined by
        Stripes <s:layout-component name="foo"> elements. Named components
        that can be overridden include:
        
          headTitle           : The HTML page title, which will be rendered
                                in the <title> element. Default=wiki: pagename
          stylesheet          : Link tags to external stylesheets. Default=blank
          inlinecss           : Inline stylesheets. Default=blank
          script              : JavaScript <script> elements. Default=blank
          jslocalizedstrings  : Localized scripts for JavaScript
                                functions. Default=blank
          jsfunction          : JavaScript functions. Default=blank
          head.meta.robots    : Search engine options. Default=noindex,nofollow
          content             : The page contents. Default=blank
          
     2) DefaultLayout injects additional JSPs that are meant to be
        customized. These include:
          
          commonheader.jsp    : A "local header" that can contain company logos
                                or other markup. Default=blank

--%>
<s:layout-definition>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>
    <%--

         Title: by default, use the "view page" title
    --%>
    <s:layout-component name="headTitle">
      <fmt:message key="view.title.view">
        <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
        <fmt:param><wiki:PageName/></fmt:param>
      </fmt:message>
    </s:layout-component>
    </title>
    <%--

         CSS stylesheets
    --%>
    <link rel="stylesheet" media="screen, projection, print" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki.css' />" />
    <%-- put this at the top, to avoid double load when not yet cached --%>
    <link rel="stylesheet" type="text/css" media="print" href="<wiki:Link format='url' templatefile='jspwiki_print.css' />" />
    <link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki_print.css' />" title="Print friendly" />
    <link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki.css' />" title="Standard" />
    <s:layout-component name="stylesheet" />
    <s:layout-component name="inlinecss" />
    <%--

         Links to favicon and common pages
    --%>
    <link rel="search" href="<wiki:LinkTo format='url' page='FindPage' />" title='Search ${wikiEngine.applicationName}' />
    <link rel="help" href="<wiki:LinkTo format='url' page='TextFormattingRules' />" title="Help" />
    <link rel="start" href="<wiki:LinkTo format='url' page='${wikiEngine.frontPage}' />" title="Front page" />
    <link rel="shortcut icon" type="image/x-icon" href="<wiki:Link format='url' jsp='images/favicon.ico' />" />
    <%-- ie6 needs next line --%>
    <link rel="icon" type="image/x-icon" href="<wiki:Link format='url' jsp='images/favicon.ico' />" />
    <%--

         Support for the universal edit button
         (www.universaleditbutton.org)
    --%>
    <wiki:CheckRequestContext context='view|info|diff|upload'>
    <wiki:Permission permission="edit">
    <wiki:PageType type="page">
    <link rel="alternate" type="application/x-wiki" href="<wiki:EditLink format='url' />" title="<fmt:message key='actions.edit.title' />" />
    </wiki:PageType>
    </wiki:Permission>
    </wiki:CheckRequestContext>
    <%--

         Skins: extra stylesheets, extra javascript
    --%>
    <c:if test='${(!empty prefs.Skin) && (prefs.Skin!="PlainVanilla") }'>
    <link rel="stylesheet" type="text/css" media="screen, projection, print" href="<wiki:Link format='url' templatefile='skins/${prefs.Skin}/skin.css' />" />
    <script type="text/javascript" src="<wiki:Link format='url' templatefile='skins/${prefs.Skin}/skin.js' />"></script>
    </c:if>
    <%--

         JavaScript
    --%>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/prettify.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-common.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-commonstyles.js' />"></script>
    <s:layout-component name="script" />
    <%--

         JavaScript: localized strings and functions
    --%>
    <script type="text/javascript">//<![CDATA[
    /* Localized javascript strings: LocalizedStrings[] */
    <s:layout-component name="jslocalizedstrings" />
    <s:layout-component name="jsfunction" />
    //]]></script>
    <%--

         Meta tags
    --%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="wikiContext" content='${wikiContext.requestContext}' />
    <meta name="wikiBaseUrl" content='<wiki:BaseURL/>' />
    <meta name="wikiPageUrl" content='<wiki:Link format="url" page="#$%" />' />
    <meta name="wikiEditUrl" content='<wiki:EditLink format="url" />' />
    <meta name="wikiJsonUrl" content='<%=  WikiContextFactory.findContext(pageContext).getURL( WikiContext.NONE, "JSON-RPC" ) %>' /><%--unusual pagename--%>
    <meta name="wikiPageName" content='<wiki:Variable var="pagename" />' /><%--pagename without blanks--%>
    <meta name="wikiUserName" content='<wiki:UserName/>' />
    <meta name="wikiTemplateUrl" content='<wiki:Link format="url" templatefile="" />' />
    <meta name="wikiApplicationName" content='${wikiEngine.applicationName}' />
    <%--

         Search engines: by default, page is not indexed or followed
    --%>
    <s:layout-component name="head.meta.robots">
    <meta name="robots" content="noindex,nofollow" />
    </s:layout-component>
    <%--

         RSS Feed discovery
    --%>
    <wiki:FeedDiscovery/>

    <wiki:Include page="localheader.jsp" />

    
  </head>

  <body class="${wikiContext.requestContext}">

    <div id="wikibody" class="${prefs.Orientation}">
     
      <wiki:Include page="Header.jsp" />
    
      <div id="content">
        <div id="page">
          <wiki:Include page="PageActionsTop.jsp" />
          <s:layout-component name="content" />
          <wiki:Include page="PageActionsBottom.jsp" />
        </div>
        <wiki:Include page="Favorites.jsp" />
      	<div class="clearbox"></div>
      </div>
    
      <wiki:Include page="Footer.jsp" />
    
    </div>
  </body>

</html>

</s:layout-definition>
