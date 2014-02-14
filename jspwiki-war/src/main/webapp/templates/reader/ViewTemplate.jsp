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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="org.apache.wiki.*" %>
<fmt:setBundle basename="templates.default"/>
<!doctype html>
<html lang="en">
  <head>

  <title>
    <fmt:message key="view.title.view">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
  </title>
  <%--<wiki:Include page="commonheader.jsp"/>--%>
<script type="text/javascript">//<![CDATA[
/* Localized javascript strings: LocalizedStrings[] */
<wiki:IncludeResources type="jslocalizedstrings"/>
String.I18N = LocalizedStrings;
String.I18N.PREFIX = "javascript.";
//]]></script>

  <wiki:CheckVersion mode="notlatest">
    <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckVersion>
  <wiki:CheckRequestContext context="diff|info">
    <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckRequestContext>
  <wiki:CheckRequestContext context="!view">
    <meta name="robots" content="noindex,follow" />
  </wiki:CheckRequestContext>

<%-- FIXME: for now, reuse haddock's stylesheet --%>
<link rel="stylesheet" media="screen, projection, print" type="text/css"
     href="<wiki:Link format='url' templatefile='../../templates/haddock/haddock.css'/>"/>

</head>

<body class="context-<wiki:Variable var='requestcontext' />">

<div class="container ${prefs.Orientation}">

  <div class="pagename"><wiki:PageName /></div>
  <div class="page"><wiki:Include page="PageTab.jsp"/></div>
  
</div>

<script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/haddock.js'/>"></script>
</body>
</html>