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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<!doctype html>
<html lang="en">
  <head>

  <title><fmt:message key="upload.title"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></title>
  <wiki:Include page="commonheader.jsp"/>
  <meta name="robots" content="noindex,nofollow" />
</head>

<body class="context-<wiki:Variable var='requestcontext' />">

<div class="container${prefs.Layout=='fluid' ? '-fluid' : ''}  ${prefs.Orientation}">

  <wiki:Include page="Header.jsp" />
  <wiki:Include page="Nav.jsp" />

  <c:set var="sidebar"><wiki:Variable var='sidebar' default="${prefs.Sidebar}" /></c:set>
  <c:set var="sidebar" value="${ (sidebar!='off') and (prefs.Orientation!='fav-hidden') ? 'on' : 'off' }" />
  <wiki:CheckRequestContext context='login|prefs|createGroup|viewGroup'>
    <c:set var="sidebar">off</c:set>
  </wiki:CheckRequestContext>

  <div class="content <c:if test='${sidebar != "off"}'>active</c:if>" data-toggle="li#menu,.sidebar>.close">
    <div class="page">
      <wiki:PageExists>
        <wiki:Include page="AttachmentTab.jsp"/>
      </wiki:PageExists>

      <wiki:NoSuchPage>
        <div class="danger">
        <fmt:message key="common.nopage">
          <fmt:param><a class="createpage" href="<wiki:EditLink format='url'/>"><fmt:message key="common.createit"/></a></fmt:param>
        </fmt:message>
        </div>
      </wiki:NoSuchPage>

      <wiki:Include page="PageInfo.jsp"/>
    </div>
    <wiki:Include page="Sidebar.jsp"/>
  </div>
  <wiki:Include page="Footer.jsp" />

</div>
</body>

</html>