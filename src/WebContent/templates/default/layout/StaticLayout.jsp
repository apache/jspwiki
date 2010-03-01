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
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.Release" %>
<%@ page errorPage="/Error.jsp" %>
<%--
     Minimal layout used for JSPs that don't need any dynamic content.
     Its structure is identical to DefaultLayout.jsp.
     Its only customizable components are 'headTitle,' 'pageTitle,' and 'content'.
--%>
<s:layout-definition>
  <head>
    <title>
      <%--
  
           Title: by default, it just says "JSPWiki"
      --%>
      <s:layout-component name="headTitle">
        JSPWiki
      </s:layout-component>
    </title>
    <%--

         CSS, JavaScript and meta tags
    --%>
    <s:url value="${templates['jspwiki.css']}" var="css" />
    <link rel="stylesheet" media="screen, projection, print" type="text/css" href="${css}" />
    <link rel="alternate stylesheet" type="text/css" href="${css}" title="Standard" />
    <s:url value="${templates['images/favicon.ico']}" var="favicon" />
    <link rel="shortcut icon" type="image/x-icon" href="${favicon}" />
    <script type="text/javascript" src="<s:url value='/scripts/mootools-core.js' />"></script>
    <script type="text/javascript" src="<s:url value='/scripts/mootools-more.js' />"></script>
    <script type="text/javascript" src="<s:url value='/scripts/jspwiki-common.js' />"></script>
    <script type="text/javascript" src="<s:url value='/scripts/jspwiki-commonstyles.js' />"></script>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex,nofollow" />
  </head>

  <body class="static">
    <div id="wikibody" class="LEFT">
    
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
        <div class="titlebox"></div>
        <div class="applicationlogo" >
          <s:link href="/"><fmt:message key="actions.home" /></s:link>
        </div>
        <div class="companylogo"></div>
        <div class="pagename">
          <s:layout-component name="pageTitle">
            JSPWiki
          </s:layout-component>
        </div>
        <div class="searchbox"></div>
        <div class="breadcrumbs"></div>
      </div>

      <%--
            Page content
      --%>
      <div id="content">
        <div id="page">
          <s:layout-component name="content" />
        </div>
      	<div class="clearbox"></div>
      </div>
      
      <%--
            Footer
      --%>
      <div id="footer">
        <div class="applicationlogo" >
          <s:link href="/"><fmt:message key="actions.home" /></s:link>
        </div>
        <div class="companylogo"></div>
        <div class="copyright"></div>
        <div class="wikiversion">
          <%=Release.APPNAME%> v<%=Release.getVersionString()%>
        </div>
      </div>

    </div>
  </body>

</html>
</s:layout-definition>
