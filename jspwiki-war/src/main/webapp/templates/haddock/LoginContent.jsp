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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
    String postURL = "";
    WikiContext ctx = WikiContext.findContext( pageContext );
    AuthenticationManager mgr = ctx.getEngine().getAuthenticationManager();

    if( mgr.isContainerAuthenticated() )
    {
        postURL = "j_security_check";
    }
    else
    {
        String redir = (String)ctx.getVariable("redirect");
        if( redir == null ) redir = ctx.getEngine().getFrontPage();
        postURL = ctx.getURL( WikiContext.LOGIN, redir );
    }

%>
<c:set var="allowsCookieAuthentication" value="<%= mgr.allowsCookieAuthentication() %>" />
<div class="page-content">

<%-- Login functionality --%>
<wiki:UserCheck status="notauthenticated">
<%--<wiki:Include page='LoginTab.jsp'/>--%>

<div class="accordion center form-col-50">

<h3><fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

<form action="<%=postURL%>"
          id="login"
       class=""
      method="post" accept-charset="<wiki:ContentEncoding />" >

    <%--<div class="help-block"><fmt:message key="login.help"></fmt:message></div>--%>

    <wiki:Messages div="error from-group" topic="login"
                prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />

    <div class="form-group">
      <%--<label class="control-label form-col-20" for="j_username"><fmt:message key="login.login"/></label>--%>
      <input autofocus="autofocus" class="form-control xform-col-50" type="text" size="24" value="<wiki:Variable var='uid' default='' />"
                placeholder="<fmt:message key='login.login'/>"
                 name="j_username" id="j_username" />
    </div>
    <div class="form-group">
      <%--<label class="control-label form-col-20"for="j_password"><fmt:message key="login.password"/></label>--%>
      <input class="form-control xform-col-50" type="password" size="24"
                placeholder="<fmt:message key='login.password'/>"
                 name="j_password" id="j_password" />
    </div>
    <c:if test="${allowsCookieAuthentication}">
    <div class="form-group">
      <label class="control-label xform-col-20" for="j_remember"><fmt:message key="login.remember"/></label>
      <input type="checkbox"
             name="j_remember" id="j_remember" />
    </div>
    </c:if>
    <div class="form-group">
        <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
        <input class="btn btn-primary btn-block xform-col-offset-20" type="submit" name="submitlogin" value="<fmt:message key='login.submit.login'/>" />
    </div>

</form>


<%-- Lost pasword functionality --%>
<h3><%=LocaleSupport.getLocalizedMessage(pageContext, "login.lostpw.tab")%></h3>

<form action="<wiki:Link jsp='LostPassword.jsp' format='url'><wiki:Param name='tab' value='lostpassword'/></wiki:Link>"
          id="lostpw"
       class=""
      method="post" accept-charset="<wiki:ContentEncoding />" >

  <%--<h3><fmt:message key="login.lostpw.heading" /></h3>--%>

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
        <wiki:Messages div="error" topic="resetpw"
                    prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
        <wiki:Messages div="information" topic="resetpwok" />
    </div>
    </c:if>

    <div class="form-group">
      <%--<label class="control-label form-col-20" for="name"><fmt:message key="login.lostpw.name"/></label>--%>
      <input class="form-control xform-col-50" type="text" size="24"
                placeholder="<fmt:message key='login.lostpw.name'/>"
                name="name" id="name" />
    </div>
    <%--<div class="help-block form-col-offset-20"><fmt:message key="login.lostpw.help"></fmt:message></div>--%>
    <div class="form-group">
        <input type="hidden" name="action" value="resetPassword"/>
        <input class="btn btn-primary btn-block xform-col-offset-20" type="submit" name="Submit" value="<fmt:message key='login.lostpw.submit'/>" />
    </div>

<%--
  <div class="form-group help-block">
    <fmt:message key="login.invite"/>
    <a href="#" onclick="$('menu-logincontent').fireEvent('click');"
                  title="<fmt:message key='login.title'/>" >
      <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
    </a>
  </div>
  <div class="form-group help-block">
    <fmt:message key="login.nopassword"/>
    <.--<a href="UserPreferences.jsp?tab=profile">--.>
    <a href="#" onclick="$('menu-profile').fireEvent('click');"
                  title="<fmt:message key='login.registernow.title'/>" >
      <fmt:message key="login.registernow">
        <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      </fmt:message>
    </a>
  </div>
--%>
  </c:otherwise>
  </c:choose>

</form>

</wiki:UserCheck>

<%-- Register new user profile --%>

<wiki:Permission permission='editProfile'>
<h3><%=LocaleSupport.getLocalizedMessage(pageContext, "login.register.tab")%></h3>
  <wiki:Include page='ProfileTab.jsp'/>
</wiki:Permission>

</div>

</div>

