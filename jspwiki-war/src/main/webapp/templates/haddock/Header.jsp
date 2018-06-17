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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="org.apache.wiki.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<c:set var="frontpage"><wiki:Variable var="jspwiki.frontPage" /></c:set>

<div class="header">

  <c:set var="titlebox"><wiki:InsertPage page="TitleBox" /></c:set>
  <c:if test="${!empty titlebox}"><div class="titlebox">${titlebox}</div></c:if>

  <div class="topline">

    <div class="cage pull-left" tabindex="0">

    <a class="logo pull-left"
        href="<wiki:Link page='${frontpage}' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param>${frontpage}</fmt:param></fmt:message> ">apache<b>jsp&#x03C9;iki</b></a>

        <wiki:PageExists page="HomeMenu">
        <ul class="dropdown-menu" data-hover-parent=".cage">
          <li class="logo-menu"><wiki:InsertPage page="HomeMenu" /></li>
        </ul>
        </wiki:PageExists>
    </div>

    <wiki:Include page="UserBox.jsp" />
    <wiki:Include page="SearchBox.jsp" />

    <div class="pagename" title="<wiki:PageName />">
      <wiki:CheckRequestContext context='viewGroup|createGroup|editGroup'><span class="icon-group"></span></wiki:CheckRequestContext>
      <wiki:PageType type="attachment"><span class="icon-paper-clip"></span></wiki:PageType>

        <c:choose>
          <c:when test="${not empty fn:substringBefore(param.page,'_blogentry_')}">
            <wiki:Link>${fn:replace(fn:replace(param.page,'_blogentry_',' ['),'_','#')}]</wiki:Link>
          </c:when>
          <c:when test="${not empty fn:substringBefore(param.page,'_comments_')}">
            <wiki:Link>${fn:replace(fn:replace(param.page,'_comments_',' ['),'_','#')}]</wiki:Link>
          </c:when>
          <c:otherwise><a href="#top" tabindex="-1"><wiki:PageName /></a></c:otherwise>
        </c:choose>

    </div>

  </div>
  <wiki:Include page="Nav.jsp" />

</div>