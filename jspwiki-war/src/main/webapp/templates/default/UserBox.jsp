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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page import="org.apache.wiki.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
%>
<c:set var="redirect"><%= c.getEngine().encodeName(c.getName()) %></c:set>
<c:set var="username"><wiki:UserName /></c:set>
<c:set var="loginstatus"><wiki:Variable var='loginstatus'/></c:set>

<div class="cage pull-right userbox user-${loginstatus}" tabindex="0">

  <%-- <div onclick="" class="btn">
      FFS the onclick="" is needed for hover effect on ipad https://www.codehaven.co.uk/fix-css-hover-on-iphone-ipad/ --%>
  <a href="#" aria-label="<fmt:message key='userbox.button'/>" class="btn">
    <span class="icon-user"></span><span class="caret"></span>
  </a>
  <ul class="dropdown-menu pull-right" data-hover-parent=".userbox">
    <li>
      <wiki:UserCheck status="anonymous">
        <wiki:LinkTo page="UserPreferences">
          <span class="icon-user"></span>
          <fmt:message key="fav.greet.anonymous"/>
        </wiki:LinkTo>
      </wiki:UserCheck>
      <wiki:UserCheck status="known"><%-- asserted or authenticated --%>
        <wiki:LinkTo page="${username}">
          <span class="icon-user" ></span>
          <wiki:UserCheck status="asserted">
            <fmt:message key="fav.greet.asserted"><fmt:param>${username}</fmt:param></fmt:message>
          </wiki:UserCheck>
          <wiki:UserCheck status="authenticated">
            <fmt:message key="fav.greet.authenticated"><fmt:param>${username}</fmt:param></fmt:message>
          </wiki:UserCheck>
        </wiki:LinkTo>
      </wiki:UserCheck>
    </li>

    <li class="divider"></li>

    <li class="dropdown-header">
      <%--
           user preferences button
      --%>
      <wiki:CheckRequestContext context='!prefs'>
        <wiki:CheckRequestContext context='!preview'>
          <wiki:Link cssClass="btn btn-default btn-block" jsp="UserPreferences.jsp">
            <wiki:Param name='redirect' value='${redirect}'/>
           <fmt:message key="actions.prefs" />
          </wiki:Link>
          <wiki:Permission permission="createGroups">
          <wiki:Link cssClass="btn btn-default btn-block" jsp="UserPreferences.jsp">
            <wiki:Param name='redirect' value='${redirect}'/>
            <wiki:Param name='tab' value='groups'/>
            <span class="icon-group"></span> <fmt:message key="actions.groups" />
          </wiki:Link>
          </wiki:Permission>
         </wiki:CheckRequestContext>
      </wiki:CheckRequestContext>
      <%--
           login button
      --%>
      <wiki:UserCheck status="notAuthenticated">
        <wiki:CheckRequestContext context='!login'>
        <wiki:Permission permission="login">
          <wiki:Link cssClass="btn btn-primary btn-block login" jsp="Login.jsp">
            <wiki:Param name='redirect' value='${redirect}'/>
            <span class="icon-signin"></span> <fmt:message key="actions.login" />
        </wiki:Link>
        </wiki:Permission>
        <wiki:Permission permission='editProfile'>
        <wiki:Link cssClass="btn btn-link btn-block register" jsp="Login.jsp">
          <wiki:Param name='redirect' value='${redirect}'/>
          <wiki:Param name='tab' value='register'/>
          <fmt:message key="actions.registernow" />
        </wiki:Link>
        </wiki:Permission>
        </wiki:CheckRequestContext>
      </wiki:UserCheck>
      <%--
           logout button
      --%>
      <wiki:UserCheck status="authenticated">
        <a href="<wiki:Link jsp='Logout.jsp' format='url' />"
          class="btn btn-default btn-block logout" data-modal=".modal">
            <span class="icon-signout"></span> <fmt:message key="actions.logout"/>
          <div class="modal"><fmt:message key='actions.confirmlogout'/></div>
        </a>
      </wiki:UserCheck>
    </li>
  </ul>
</div>
