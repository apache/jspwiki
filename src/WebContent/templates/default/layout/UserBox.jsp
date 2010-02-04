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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<div class="userbox">

  <wiki:UserCheck status="anonymous">
    <span class="username anonymous">
      <fmt:message key="fav.greet.anonymous" />
    </span>
  </wiki:UserCheck>
  <wiki:UserCheck status="asserted">
    <span class="username asserted">
      <fmt:message key="fav.greet.asserted">
      <fmt:param><wiki:Translate>[<wiki:UserName/>]</wiki:Translate></fmt:param>
    </fmt:message>
    </span>
  </wiki:UserCheck>
  <wiki:UserCheck status="authenticated">
    <span class="username authenticated">
      <fmt:message key="fav.greet.authenticated">
        <fmt:param><wiki:Translate>[<wiki:UserName/>]</wiki:Translate></fmt:param>
      </fmt:message>
    </span>
  </wiki:UserCheck>

  <%-- action buttons --%>
  <wiki:UserCheck status="notAuthenticated">
    <wiki:CheckRequestContext context='!login'>
      <wiki:Permission permission="login">
        <c:set var="loginTitle"><fmt:message key='actions.login.title' /></c:set>
        <s:link
          beanclass="org.apache.wiki.action.LoginActionBean"
          class="btn login"
          title="${loginTitle}">
          <s:param name="redirect" value="${wikiContext.page.name}" />
          <span><span><fmt:message key="actions.login"/></span></span>
        </s:link>
      </wiki:Permission>
    </wiki:CheckRequestContext>
  </wiki:UserCheck>
  
  <wiki:UserCheck status="authenticated">
    <c:set var="logoutTitle"><fmt:message key='actions.logout.title' /></c:set>
    <s:link
      beanclass="org.apache.wiki.action.LoginActionBean" event="logout"
      class="btn logout"
      title="${logoutTitle}">
      <span><span><fmt:message key="actions.logout"/></span></span>
    </s:link>
  </wiki:UserCheck>

  <wiki:CheckRequestContext context='!prefs'>
    <wiki:CheckRequestContext context='!preview'>
      <c:set var="prefsTitle"><fmt:message key='actions.prefs.title' /></c:set>
      <s:link
        beanclass="org.apache.wiki.action.UserPreferencesActionBean"
        class="btn prefs" accesskey="p"
        title="${prefsTitle}">
        <s:param name="redirect" value="${wikiContext.page.name}" />
        <span><span><fmt:message key="actions.prefs"/></span></span>
      </s:link>
    </wiki:CheckRequestContext>
  </wiki:CheckRequestContext>

  <div class="clearbox"></div>

</div>