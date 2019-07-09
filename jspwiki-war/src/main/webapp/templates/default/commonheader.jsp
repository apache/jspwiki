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
<%--
BOOTSTRAP, IE compatibility / http://getbootstrap.com/getting-started/#support-ie-compatibility-modes
--%>
<meta charset="<wiki:ContentEncoding />">
<meta http-equiv="x-ua-compatible" content="ie=edge" />
<meta name="viewport" content="width=device-width, initial-scale=1">
<%--
<meta http-equiv="Content-Security-Policy" content="upgrade-insecure-requests">
--%>

<wiki:PageExists>
  <meta name="author" content="<wiki:Author format='plain'/>">
  <meta name="description" content="Page version <wiki:PageVersion />, last modified by <wiki:Author format='plain'/>, on <wiki:PageDate format='${prefs["DateFormat"]}'/>" />
  <c:set var="keywords"><wiki:Variable var='keywords' default='' /></c:set>
  <c:if test="${!empty keywords}">
    <meta name="keywords" content="<wiki:Variable var='keywords' default='' />" /><%--seo keywords--%>
  </c:if>
</wiki:PageExists>


<%-- COOKIE read client preferences --%>
<%
   Preferences.setupPreferences(pageContext);
%>

<%-- Localized JS; must come before any css, to avoid blocking immediate execution --%>
<%-- var LocalizedStrings= { "javascript.<xx>":"...", etc. } --%>
<script type="text/javascript">//<![CDATA[
<wiki:IncludeResources type="jslocalizedstrings"/>
String.I18N = LocalizedStrings;
String.I18N.PREFIX = "javascript.";
//]]></script>

<link rel="stylesheet" type="text/css" media="screen, projection, print" id="main-stylesheet"
     href="<wiki:Link format='url' templatefile='haddock.css'/>" />

<c:if test="${prefs.Appearance }">
<link rel="stylesheet" type="text/css" media="screen, projection, print" id="main-stylesheet"
     href="<wiki:Link format='url' templatefile='haddock-dark.css'/>"/>
</c:if>

<wiki:IncludeResources type="stylesheet"/>
<wiki:IncludeResources type="inlinecss" />

<%-- JAVASCRIPT --%>

<script src="<wiki:Link format='url' jsp='scripts/haddock.js'/>"></script>

<wiki:IncludeResources type="script"/>


<meta name="wikiContext" content='<wiki:Variable var="requestcontext" />' />
<wiki:Permission permission="edit"><meta name="wikiEditPermission" content="true"/></wiki:Permission>
<meta name="wikiBaseUrl" content='<wiki:BaseURL />' />
<meta name="wikiPageUrl" content='<wiki:Link format="url"  page="#$%"/>' />
<meta name="wikiEditUrl" content='<wiki:EditLink format="url" page="#$%"/>' />
<meta name="wikiCloneUrl" content='<wiki:EditLink format="url" page="#$%"/>&clone=<wiki:Variable var="pagename" />' />
<meta name="wikiJsonUrl" content='<%= WikiContext.findContext(pageContext).getURL( WikiContext.NONE, "ajax" ) %>' /><%--unusual pagename--%>
<meta name="wikiPageName" content='<wiki:Variable var="pagename" />' /><%--pagename without blanks--%>
<meta name="wikiUserName" content='<wiki:UserName />' />
<meta name="wikiTemplateUrl" content='<wiki:Link format="url" templatefile="" />' />
<meta name="wikiApplicationName" content='<wiki:Variable var="ApplicationName" />' />
<%--CHECKME
    <wiki:link> seems not to lookup the right jsp from the right template directory
    EG when a templatefile is not present, the generated link should point to the default template.
    Solution for now: manually force the relevant links back to the default template
--%>
<meta name="wikiXHRSearch" content='<wiki:Link format="url" templatefile="../default/AJAXSearch.jsp" />' />
<meta name="wikiXHRPreview" content='<wiki:Link format="url" templatefile="../default/AJAXPreview.jsp" />' />
<meta name="wikiXHRCategories" content='<wiki:Link format="url" templatefile="../default/AJAXCategories.jsp" />' />
<meta name="wikiXHRHtml2Markup" content='<wiki:Link format="url" jsp="XHRHtml2Markup.jsp" />' />
<meta name="wikiXHRMarkup2Wysiwyg" content='<wiki:Link format="url" jsp="XHRMarkup2Wysiwyg.jsp" />' />

<script type="text/javascript">//<![CDATA[
<wiki:IncludeResources type="jsfunction"/>
//]]></script>

<link rel="search" href="<wiki:LinkTo format='url' page='Search'/>"
    title='Search <wiki:Variable var="ApplicationName" />' />
<link rel="help"   href="<wiki:LinkTo format='url' page='TextFormattingRules'/>"
    title="Help" />
<c:set var="frontpage"><wiki:Variable var="jspwiki.frontPage" /></c:set>
<link rel="start"  href="<wiki:Link page='${frontpage}' format='url' />" title="Front page" />
<link rel="alternate stylesheet" type="text/css" href="<wiki:Link format='url' templatefile='haddock.css'/>"
    title="Standard" />

<%-- Generated by https://realfavicongenerator.net/ --%>
<link rel="apple-touch-icon" sizes="180x180" href="favicons/apple-touch-icon.png">
<link rel="icon" type="image/png" sizes="32x32" href="favicons/favicon-32x32.png">
<link rel="icon" type="image/png" sizes="16x16" href="favicons/favicon-16x16.png">
<link rel="manifest" href="favicons/site.webmanifest">
<link rel="mask-icon" href="favicons/safari-pinned-tab.svg" color="#da532c">
<link rel="shortcut icon" href="favicons/favicon.ico">
<meta name="msapplication-TileColor" content="#da532c">
<meta name="msapplication-config" content="favicons/browserconfig.xml">
<meta name="theme-color" content="#ffffff">

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
<script type="text/javascript"
         src="<wiki:Link format='url' templatefile='skins/' /><c:out value='${prefs.SkinName}/skin.js' />" ></script>
</c:if>

<wiki:Include page="localheader.jsp"/>