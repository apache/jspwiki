<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<%--
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
--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.Release" %>
<%@ page import="org.apache.wiki.WikiContext" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page errorPage="/Error.jsp" %>
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
          headMetaRobots      : Search engine options. Default=noindex,nofollow
          pageTitle           : The title for the JSP, which will be rendered
                                at the top of the page body. Default=wiki: pagename
          content             : The page contents. Default=blank

     2) DefaultLayout injects additional JSPs that are meant to be
        customized. These include:

          LocalHeader.jsp     : A "local header" that can contain company logos
                                or other markup. Default=blank

--%>
<s:layout-definition>
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
    <s:url value="${templates['jspwiki.css']}" var="css" />
    <link rel="stylesheet" media="screen, projection, print" type="text/css" href="${css}" />
    <link rel="alternate stylesheet" type="text/css" href="${css}" title="Standard" />
    <s:layout-component name="stylesheet" />
    <s:layout-component name="inlinecss" />
    <%--

         Links to favicon and common pages
    --%>
    <link rel="search" href="<wiki:LinkTo format='url' page='FindPage' />" title='Search ${wikiEngine.applicationName}' />
    <link rel="help" href="<wiki:LinkTo format='url' page='TextFormattingRules' />" title="Help" />
    <link rel="start" href="<wiki:LinkTo format='url' page='${wikiEngine.frontPage}' />" title="Front page" />
    <s:url value="${templates['images/favicon.ico']}" var="favicon" />
    <link rel="shortcut icon" type="image/x-icon" href="${favicon}" />
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
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools-core.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools-more.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/prettify.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-common.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-slimbox.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-commonstyles.js' />"></script>
    <s:layout-component name="script" />
    <%--

         JavaScript: localized strings and functions
    --%>
    <script type="text/javascript">//<![CDATA[
    <s:layout-component name="jslocalizedstrings" />
    <s:layout-component name="jsfunction" />
    //]]></script>
    <%--

         Meta tags
    --%>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="wikiContext" content='${wikiContext.requestContext}' />
    <meta name="wikiBaseUrl" content='<wiki:BaseURL/>' />
    <meta name="wikiPageUrl" content='<wiki:Link format="url"  page="#$%"/>' />
    <meta name="wikiEditUrl" content='<wiki:EditLink format="url" />' />
    <meta name="wikiJsonUrl" content='<%=  WikiContextFactory.findContext(pageContext).getURL( WikiContext.NONE, "JSON-RPC" ) %>' /><%--unusual pagename--%>
    <meta name="wikiPageName" content='<wiki:Variable var="pagename" />' /><%--pagename without blanks--%>
    <meta name="wikiUserName" content='<wiki:UserName/>' />
    <meta name="wikiTemplateUrl" content='<wiki:Link format="url" templatefile="" />' />
    <meta name="wikiApplicationName" content='${wikiEngine.applicationName}' />
    <%--

         Search engines: by default, page is not indexed or followed
    --%>
    <s:layout-component name="headMetaRobots">
      <meta name="robots" content="noindex,nofollow" />
    </s:layout-component>
    <%--

         RSS Feed discovery
    --%>
    <wiki:FeedDiscovery/>
  </head>

  <%--
       Body content
  --%>
  <body class="${wikiContext.requestContext}">

    <div id="wikibody" class="${prefs.Orientation}">

      <%--
            Local header
      --%>
      <div id="localHeader">
        <jsp:include page="${templates['layout/LocalHeader.jsp']}" />
      </div>

      <%--
            Header
      --%>
      <div id="header">
        <div class="titlebox"><wiki:InsertPage page="TitleBox" /></div>
        <div class="applicationlogo" >
          <c:set var="frontPageTitle"><fmt:message key='actions.home.title' ><fmt:param><c:out value='${wikiEngine.frontPage}' /></fmt:param></fmt:message></c:set>
          <s:link beanclass="org.apache.wiki.action.ViewActionBean" title="${frontPageTitle}"><fmt:message key="actions.home" /></s:link>
        </div>
        <div class="companylogo"></div>
        <div class="pagename">
          <s:layout-component name="pageTitle">
            <wiki:PageName/>
          </s:layout-component>
        </div>
        <jsp:include page="${templates['layout/UserBox.jsp']}" />
        <div class="searchbox">
          <jsp:include page="${templates['layout/SearchBox.jsp']}" />
        </div>
        <div class="breadcrumbs"><fmt:message key="header.yourtrail" /><wiki:Breadcrumbs/></div>
      </div>

      <%--
            Page content
      --%>
      <div id="content">
        <div id="page">
          <jsp:include page="${templates['layout/PageActionsTop.jsp']}" />
          <s:layout-component name="content" />
          <jsp:include page="${templates['layout/PageActionsBottom.jsp']}" />
        </div>
        <jsp:include page="${templates['layout/Favorites.jsp']}" />
      	<div class="clearbox"></div>
      </div>

      <%--
            Footer
      --%>
      <div id="footer">
        <div class="applicationlogo" >
          <c:set var="frontPageTitle"><fmt:message key='actions.home.title' ><fmt:param><c:out value='${wikiEngine.frontPage}' /></fmt:param></fmt:message></c:set>
          <s:link beanclass="org.apache.wiki.action.ViewActionBean" title="${frontPageTitle}"><fmt:message key="actions.home" /></s:link>
        </div>
        <div class="companylogo"></div>
        <div class="copyright"><wiki:InsertPage page="CopyrightNotice" /></div>
        <div class="wikiversion">
          <%=Release.APPNAME%> v<%=Release.getVersionString()%>
        </div>
        <div class="rssfeed">
          <wiki:RSSImageLink title="Aggregate the RSS feed" />
        </div>
      </div>

    </div>
  </body>

</html>

</s:layout-definition>
