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

<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
    WikiContext ctx = WikiContext.findContext( pageContext );
    AuthenticationManager mgr = ctx.getEngine().getAuthenticationManager();
    String loginURL = "";

    if( mgr.isContainerAuthenticated() )
    {
        loginURL = "j_security_check";
    }
    else
    {
        String redir = (String)ctx.getVariable("redirect");
        if( redir == null ) redir = ctx.getEngine().getFrontPage();
        loginURL = ctx.getURL( WikiContext.LOGIN, redir );
    }

%>
<c:set var="allowsCookieAuthentication" value="<%= mgr.allowsCookieAuthentication() %>" />
<div class="page-content">

<%-- Login functionality --%>
<wiki:UserCheck status="notauthenticated">
<%--<wiki:Include page='LoginTab.jsp'/>--%>

<div class="tabs" >

<h3 id="section-login"><fmt:message key="login.tab"/></h3>

<form action="<%= loginURL %>"
          id="login"
       class="login-form"
      method="post" accept-charset="<wiki:ContentEncoding />" >

    <p class="login-header">
      <fmt:message key="login.heading.login">
        <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      </fmt:message>
    </p>
    <%--<div class="help-block"><fmt:message key="login.help"></fmt:message></div>--%>

    <fmt:message key="login.errorprefix" var="msg"/>
    <wiki:Messages div="alert alert-danger" topic="login" prefix="${msg}" />

    <div class="form-group">
      <input class="form-control"
             value="<wiki:Variable var='uid' default='' />"
       placeholder="<fmt:message key='login.login'/>"
              type="text" size="24" autofocus="autofocus" name="j_username" id="j_username" />
    </div>

    <div class="form-group">
      <input class="form-control"
       placeholder="<fmt:message key='login.password'/>"
              type="password" size="24" name="j_password" id="j_password" />
    </div>

    <div class="form-group clearfix"><%-- need clearfix ico no-cookies-auth allowed; ensure the right floated btn gets proper spacing -- ugh! --%>
    <c:if test="${allowsCookieAuthentication}">
      <label class="btn" for="j_remember">
        <input type="checkbox" name="j_remember" id="j_remember" />
        <fmt:message key="login.remember"/>
      </label>
    </c:if>

      <a class="btn btn-link pull-right" href="#section-lostpw"><fmt:message key="login.lostpw" /></a>
    </div>

    <div class="form-group">
      <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
      <input class="btn btn-success btn-block"
              type="submit" name="submitlogin" value="<fmt:message key='login.submit.login'/>" />
    </div>

    <hr />

    <%--CHECKME only allow to register new uses when no container auth !? --%>
    <%--  <wiki:UserProfile property="canChangeLoginName">  --%>
    <p class="login-ref">
      <fmt:message key="login.nopassword"/>
      <a class="" href="#section-register">
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </p>
    <%-- </wiki:UserProfile> --%>

</form>


<%-- Lost pasword functionality --%>
<c:set var="lostpwTab" value="${param.tab == 'lostpassword' ? 'data-activePane': ''}"/>
<h3 ${lostpwTab} id="section-lostpw"><fmt:message key="login.lostpw.tab"/></h3>

<form action="<wiki:Link jsp='LostPassword.jsp' format='url'><wiki:Param name='tab' value='lostpassword'/></wiki:Link>"
          id="lostpw"
       class="login-form"
      method="post" accept-charset="<wiki:ContentEncoding />" >

  <p class="login-header"><fmt:message key="login.lostpw.title" /></p>

  <c:choose>
  <c:when test="${passwordreset == 'done' }">
      <wiki:Messages div="information" topic="resetpw" prefix="" />
      <p>
        <fmt:message key="login.lostpw.reset.login">
          <fmt:param><a href="<wiki:Link jsp='Login.jsp' />"><fmt:message key="login.lostpw.reset.clickhere"/></a></fmt:param>
        </fmt:message>
      </p>
  </c:when>
  <c:otherwise>
    <c:if test="${param.tab eq 'lostpassword'}" >
      <div class="form-group help-block">
        <fmt:message key="login.errorprefix" var="msg"/>
        <wiki:Messages div="alert alert-danger" topic="resetpw" prefix="${msg}" />
        <wiki:Messages div="information" topic="resetpwok" />
      </div>
    </c:if>

    <div class="form-group">
      <p class="help-block"><fmt:message key="login.lostpw.help"></fmt:message></p>
      <%--<label class="control-label" for="name"><fmt:message key="login.lostpw.name"/></label>--%>
      <input class="form-control" type="text" size="24"
       placeholder="<fmt:message key='login.lostpw.name'/>"
              name="name" id="name" />
    </div>
    <div class="form-group">
      <input type="hidden" name="action" value="resetPassword"/>
      <input class="btn btn-success btn-block" type="submit" name="Submit" value="<fmt:message key='login.lostpw.submit'/>" />
    </div>

    <hr />

    <p class="login-ref">
      <fmt:message key="login.invite"/>
      <a href="#section-login"
         title="<fmt:message key='login.title'/>" >
        <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
      </a>
    </p>
    <p class="login-ref">
      <fmt:message key="login.nopassword"/>
      <a class="" href="#section-register">
          <fmt:message key="login.registernow">
            <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
          </fmt:message>
        </a>
    </p>

  </c:otherwise>
  </c:choose>

</form>

</wiki:UserCheck>

<%-- Register new user profile --%>
<wiki:Permission permission='editProfile'>
<c:set var="registerTab" value="${param.tab == 'register' ? 'data-activePane': ''}"/>
<h3 ${registerTab} id="section-register"><fmt:message key="login.register.tab" /></h3>

<%-- <wiki:Include page='ProfileTab.jsp'/> --%>
<form action="<wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab' value='register'/></wiki:Link>"
          id="editProfile"
       class="login-form"
      method="post" accept-charset="UTF-8">

  <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />

  <p class="login-header"><fmt:message key="login.registernow.title" /></p>

  <fmt:message key="prefs.errorprefix.profile" var="msg"/>
  <wiki:Messages div="alert alert-danger" topic="profile" prefix="${msg}" />

  <%-- Login name --%>
  <%-- TODO:  can be simplified in case of registering a new user --%>
  <div class="form-group">
    <wiki:UserProfile property="canChangeLoginName">
      <input class="form-control" type="text" name="loginname" id="loginname" size="20"
       placeholder="<fmt:message key='prefs.loginname' />"
             value="<wiki:UserProfile property='loginname' />" />
    </wiki:UserProfile>

    <wiki:UserProfile property="!canChangeLoginName">
      <!-- If user can't change their login name, it's because the container manages the login -->
      <wiki:UserProfile property="new">
        <div class="warning"><fmt:message key="prefs.loginname.cannotset.new"/></div>
      </wiki:UserProfile>
      <wiki:UserProfile property="exists">
        <span class="form-control-static"><wiki:UserProfile property="loginname"/></span>
        <div class="warning"><fmt:message key="prefs.loginname.cannotset.exists"/></div>
      </wiki:UserProfile>
    </wiki:UserProfile>
  </div>

  <%-- Password field; not displayed if container auth used --%>
  <wiki:UserProfile property="canChangePassword">
    <div class="form-group">
       <input class="form-control" type="password" name="password" id="password" size="20"
        placeholder="<fmt:message key='prefs.password' />"
              value="" />
     </div>
     <div class="form-group">
       <input class="form-control" type="password" name="password2" id="password2" size="20"
        placeholder="<fmt:message key='prefs.password2' />"
              value="" />
       <%-- FFS: extra validation ? min size, allowed chars? password-strength js check --%>
     </div>
  </wiki:UserProfile>

  <%-- Full name --%>
  <div class="form-group">
    <input class="form-control" type="text" name="fullname" id="fullname" size="20"
     placeholder="<fmt:message key='prefs.fullname'/>"
           value="<wiki:UserProfile property='fullname' />" />
    <p class="help-block"><fmt:message key="prefs.fullname.description"/></p>
  </div>

  <%-- E-mail --%>
  <div class="form-group">
    <input class="form-control" type="text" name="email" id="email" size="20"
     placeholder="<fmt:message key='prefs.email'/>"
           value="<wiki:UserProfile property='email' />" />
    <p class="help-block"><fmt:message key="prefs.email.description"/></p>
  </div>

  <div class="form-group">
    <button class="btn btn-success btn-block" type="submit" name="action" value="saveProfile">
      <fmt:message key='prefs.newprofile' />
    </button>
  </div>

  <hr />

  <p class="login-ref">
    <fmt:message key="login.invite"/>
    <a href="#section-login"
      title="<fmt:message key='login.title'/>" >
      <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
    </a>
  </p>

</form>

</wiki:Permission>

</div>

</div>