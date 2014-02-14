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

    boolean supportsCookieAuthentication = mgr.allowsCookieAuthentication();
%>
<div class="page-content">

<%-- Login functionality --%>
<wiki:UserCheck status="notauthenticated">
<%--<wiki:Include page='LoginTab.jsp'/>--%>

<div class="rightAccordion">

<h4><fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h4>
<form action="<%=postURL%>"
          id="login"
       class=""
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding />" >


  <div class="help-block"><fmt:message key="login.help"></fmt:message></div>

    <div class="form-group">
        <wiki:Messages div="error" topic="login"
                    prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
    </div>
    <div class="form-group">
      <td><label class="control-label form-col-20" for="j_username"><fmt:message key="login.login"/></label></td>
      <td><input autofocus="autofocus" class="form-control form-col-50" type="text" size="24" value="<wiki:Variable var='uid' default='' />"
                 name="j_username" id="j_username" /></td>
    </div>
    <div class="form-group">
      <td><label class="control-label form-col-20"for="j_password"><fmt:message key="login.password"/></label></td>
      <td><input class="form-control form-col-50" type="password" size="24"
                 name="j_password" id="j_password" /></td>
    </div>
    <% if( supportsCookieAuthentication ) { %>
    <div class="form-group">
      <td><label class="control-label form-col-20" for="j_remember"><fmt:message key="login.remember"/></label></td>
      <td><input type="checkbox"
                 name="j_remember" id="j_remember" /></td>
    </div>
    <% } %>
    <div class="form-group">
        <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
        <input class="btn btn-primary form-col-offset-20" type="submit" name="submitlogin" value="<fmt:message key='login.submit.login'/>" />
    </div>

<%--
    <div class="help-block">
      <fmt:message key="login.lostpw"/>
      <a href="#" onclick="$('menu-lostpassword').fireEvent('click');"
                    title="<fmt:message key='login.lostpw.title'/>" >
        <fmt:message key="login.lostpw.getnew"/>
      </a>
    </div>
    <div class="help-block">
      <fmt:message key="login.nopassword"/>
      <a href="#" onclick="$('menu-profile').fireEvent('click');"
                    title="<fmt:message key='login.registernow.title'/>" >
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </div>
--%>

</form>


<%-- Lost pasword functionality --%>
<h4><%=LocaleSupport.getLocalizedMessage(pageContext, "login.lostpw.tab")%></h4>

<form action="<wiki:Link jsp='LostPassword.jsp' format='url'><wiki:Param name='tab' value='lostpassword'/></wiki:Link>"
          id="lostpw"
       class=""
      method="post" accept-charset="<wiki:ContentEncoding />" >

  <%--<h4><fmt:message key="login.lostpw.heading" /></h4>--%>

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
      <td><label class="control-label form-col-20" for="name"><fmt:message key="login.lostpw.name"/></label></td>
      <td><input class="form-control form-col-50" type="text" size="24" name="name" id="name" /></td>
    </div>
      <div class="help-block form-col-offset-20"><fmt:message key="login.lostpw.help"></fmt:message></div>
    <div class="form-group">
        <input type="hidden" name="action" value="resetPassword"/>
        <input class="btn btn-primary form-col-offset-20" type="submit" name="Submit" value="<fmt:message key='login.lostpw.submit'/>" />
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
<h4><%=LocaleSupport.getLocalizedMessage(pageContext, "login.register.tab")%></h4>
  <wiki:Include page='ProfileTab.jsp'/>
</wiki:Permission>

</div>
</div>

