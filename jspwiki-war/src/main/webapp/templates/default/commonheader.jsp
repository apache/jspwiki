<%--
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
--%>

<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.ui.*" %>
<%@ page import="org.apache.wiki.util.*" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
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
<link rel="stylesheet" media="screen, projection, print" type="text/css"  id="main-stylesheet"
     href="<wiki:Link format='url' templatefile='jspwiki.css'/>"/>
<%-- put this at the top, to avoid double load when not yet cached --%>
<link rel="stylesheet" type="text/css" media="print"
     href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>" />
<wiki:IncludeResources type="stylesheet"/>
<wiki:IncludeResources type="inlinecss" />

<%-- display the more-menu inside the leftmenu, when javascript is not avail --%>
<noscript>
<style type="text/css">
#hiddenmorepopup { display:block; }
</style>
</noscript>

<%-- JAVASCRIPT --%>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools.js'/>"></script>
<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-common.js'/>"></script>
<wiki:IncludeResources type="script"/>

<%-- COOKIE read client preferences --%>
<%
   Preferences.setupPreferences(pageContext);
 %>

<meta name="wikiContext" content='<wiki:Variable var="requestcontext" />' />
<meta name="wikiBaseUrl" content='<wiki:BaseURL />' />
<meta name="wikiPageUrl" content='<wiki:Link format="url"  page="#$%"/>' />
<meta name="wikiEditUrl" content='<wiki:EditLink format="url" />' />
<meta name="wikiJsonUrl" content='<%=  WikiContext.findContext(pageContext).getURL( WikiContext.NONE, "ajax" ) %>' /><%--unusual pagename--%>
<meta name="wikiPageName" content='<wiki:Variable var="pagename" />' /><%--pagename without blanks--%>
<meta name="wikiUserName" content='<wiki:UserName />' />
<meta name="wikiTemplateUrl" content='<wiki:Link format="url" templatefile="" />' />
<meta name="wikiApplicationName" content='<wiki:Variable var="ApplicationName" />' />

<script type="text/javascript">//<![CDATA[
/* Localized javascript strings: LocalizedStrings[] */
<wiki:IncludeResources type="jslocalizedstrings"/>
<wiki:IncludeResources type="jsfunction"/>
//]]></script>

<meta http-equiv="Content-Type" content="text/html; charset=<wiki:ContentEncoding />" />
<link rel="search" href="<wiki:LinkTo format='url' page='Search'/>"
    title='Search <wiki:Variable var="ApplicationName" />' />
<link rel="help"   href="<wiki:LinkTo format='url' page='TextFormattingRules'/>"
    title="Help" />
<%
  WikiContext c = WikiContext.findContext( pageContext );
  String frontpage = c.getEngine().getFrontPage();
 %>
 <link rel="start"  href="<wiki:LinkTo format='url' page='<%=frontpage%>' />"
    title="Front page" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki_print.css'/>"
    title="Print friendly" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='jspwiki.css'/>"
    title="Standard" />
<link rel="shortcut icon" type="image/x-icon" href="<wiki:Link format='url' jsp='images/favicon.ico'/>" />
<%-- ie6 needs next line --%>
<link rel="icon" type="image/x-icon" href="<wiki:Link format='url' jsp='images/favicon.ico'/>" />

<%-- Support for the universal edit button (www.universaleditbutton.org) --%>
<wiki:CheckRequestContext context='view|info|diff|upload'>
  <wiki:Permission permission="edit">
    <wiki:PageType type="page">
    <link rel="alternate" type="application/x-wiki"
          href="<wiki:EditLink format='url' />"
          title="<fmt:message key='actions.edit.title'/>" />
    </wiki:PageType>
  </wiki:Permission>
</wiki:CheckRequestContext>

<wiki:FeedDiscovery />

<%-- SKINS : extra stylesheets, extra javascript --%>
<c:if test='${(!empty prefs.SkinName) && (prefs.SkinName!="PlainVanilla") }'>
<link rel="stylesheet" type="text/css" media="screen, projection, print"
     href="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs.SkinName}/skin.css' />" />
<%--
<link rel="stylesheet" type="text/css" media="print"
     href="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs.SkinName}/print_skin.css' />" />
--%>
<script type="text/javascript"
         src="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs.SkinName}/skin.js' />" ></script>
</c:if>

<wiki:Include page="localheader.jsp"/>