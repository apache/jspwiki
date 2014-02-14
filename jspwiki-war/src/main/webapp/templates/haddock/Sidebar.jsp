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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%@ page import="org.apache.wiki.*" %>

<div class="sidebar">
  <button class="close" type="button">&times;</button>

  <%-- move this to the LeftMenu
  <wiki:UserCheck status="known">
  <wiki:Translate>[{If page='{$username}Favorites' exists='true'

%%accordion
! [My Favorites|{$username}Favorites]
[{InsertPage page='{$username}Favorites' }]
%% }]
  </wiki:Translate>
  </wiki:UserCheck>
--%>
  <wiki:Permission permission="view">

  <%-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" --%>
  <div class="leftmenu">
    <wiki:InsertPage page="LeftMenu" />
    <wiki:NoSuchPage page="LeftMenu">
      <div class="error">
        <wiki:EditLink page="LeftMenu">
          <fmt:message key="fav.nomenu"><fmt:param>LeftMenu</fmt:param></fmt:message>
        </wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>
  
  <div class="leftmenufooter">
    <wiki:InsertPage page="LeftMenuFooter" />
    <wiki:NoSuchPage page="LeftMenuFooter">
      <div class="error">
        <wiki:EditLink page="LeftMenuFooter">
          <fmt:message key="fav.nomenu"><fmt:param>LeftMenuFooter</fmt:param></fmt:message>
        </wiki:EditLink>
      </div>
    </wiki:NoSuchPage>
  </div>

  </wiki:Permission>

  <div class="wikiversion"><%=Release.APPNAME%> v<%=Release.getVersionString()%>
    <wiki:RSSImageLink title='<%=LocaleSupport.getLocalizedMessage(pageContext,"fav.aggregatewiki.title")%>' />
  </div>  
  
</div>