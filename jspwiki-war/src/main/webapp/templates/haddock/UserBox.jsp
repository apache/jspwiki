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

<div class="cage pull-right userbox user-${loginstatus}">

  <div class="btn"><span class="icon-user"></span><span class="caret"/></span></div>

  <ul class="dropdown-menu pull-right" data-hover-parent=".userbox">

    <li>
      <wiki:UserCheck status="anonymous">
        <wiki:LinkTo page="UserPreferences">
          <span class="icon-user"> <fmt:message key="fav.greet.${loginstatus}"/>
        </wiki:LinkTo>
      </wiki:UserCheck>
      <wiki:UserCheck status="known"><%-- asserted or authenticated --%>
        <wiki:LinkTo page="${username}">
          <span class="icon-user">
            <fmt:message key="fav.greet.${loginstatus}"><fmt:param>${username}</fmt:param></fmt:message>
          </span>
        </wiki:LinkTo>
      </wiki:UserCheck>
    </li>

    <li class="dropdown-header">
      <div class="btn-group btn-group-justified">
      <%--
           login button
      --%>
      <wiki:UserCheck status="notAuthenticated">
        <wiki:CheckRequestContext context='!login'>
        <wiki:Permission permission="login">
        <a href="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='redirect' value='${redirect}'/></wiki:Link>"
           class="btn btn-default login"
           title="<fmt:message key='actions.login.title'/>"><fmt:message key="actions.login"/></a>
        </wiki:Permission>
        </wiki:CheckRequestContext>
      </wiki:UserCheck>
      <%--
           logout button
      --%>
      <wiki:UserCheck status="authenticated">
        <a href="<wiki:Link jsp='Logout.jsp' format='url' />"
          class="btn btn-default logout"
          title="<fmt:message key='actions.logout.title'/>"
          data-modal="<fmt:message key='actions.confirmlogout'/>"><fmt:message key="actions.logout"/>
        </a>
      </wiki:UserCheck>
      <%--
           user preferences button
      --%>
      <wiki:CheckRequestContext context='!prefs'>
        <wiki:CheckRequestContext context='!preview'>
          <a href="<wiki:Link jsp='UserPreferences.jsp' format='url' ><wiki:Param name='redirect' value='${redirect}'/></wiki:Link>"
            class="btn btn-default prefs"
            title="<fmt:message key='actions.prefs.title'/>"><fmt:message key="actions.prefs" />
          </a>
         </wiki:CheckRequestContext>
      </wiki:CheckRequestContext>
      </div>
    </li>
  </ul>
</div>
