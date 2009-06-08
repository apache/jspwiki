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
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%
    String postURL = "";
    WikiContext ctx = WikiContextFactory.findContext( pageContext );
    AuthenticationManager mgr = ctx.getEngine().getAuthenticationManager();

    if( mgr.isContainerAuthenticated() )
    {
        postURL = "j_security_check";
    }
    else
    {
        postURL = "/Login.action";
    }

    boolean supportsCookieAuthentication = mgr.allowsCookieAuthentication();
%>
<wiki:TabbedSection defaultTab="${param.tab}">

<%-- Login tab --%>
<wiki:UserCheck status="notauthenticated">
<wiki:Tab id="logincontent" titleKey="login.tab">
<%--<wiki:Include page='LoginTab.jsp'/>--%>

<s:form action="<%=postURL%>" id="login" class="wikiform" acceptcharset="UTF-8">
  <s:param name="tab" value="logincontent" />

  <div class="center">

  <h3><fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message></h3>

  <div class="formhelp"><fmt:message key="login.help"></fmt:message></div>

  <table>
    <tr>
      <td colspan="2" class="formhelp">
        <wiki:Messages div="error" topic="login" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
      </td>
    </tr>
    <tr>
      <td><s:label for="j_username" name="loginName" /></td>
      <td>
        <s:text size="24" name="j_username" id="j_username"><wiki:Variable var="uid" default="" /></s:text>
      </td>
    </tr>
    <tr>
      <td><s:label for="j_password" name="password" /></td>
      <td><s:password size="24" name="j_password" id="j_password" /></td>
    </tr>
    <% if( supportsCookieAuthentication ) { %>
    <tr>
      <td><s:label for="remember" /></td>
      <td><s:checkbox name="remember" id="j_remember" /></td>
    </tr>
    <% } %>
    <tr>
      <td>&nbsp;</td>
      <td>
        <s:submit name="login" />
      </td>
    </tr>
    </table>

    <div class="formhelp">
      <fmt:message key="login.lostpw" />
      <a href="#" onclick="$('menu-lostpassword').fireEvent('click');" title="<fmt:message key='login.lostpw.title' />">
        <fmt:message key="login.lostpw.getnew" />
      </a>
    </div>
    <div class="formhelp">
      <fmt:message key="login.nopassword" />
      <a href="#" onclick="$('menu-profile').fireEvent('click');" title="<fmt:message key='login.registernow.title' />">
        <fmt:message key="login.registernow">
          <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
        </fmt:message>
      </a>
    </div>

  </div>
</s:form>

</wiki:Tab>

<%-- Lost password tab --%>
<wiki:Tab id="lostpassword" titleKey="login.lostpw.tab" url="LostPassword.jsp" />
</wiki:UserCheck>

<%-- Register new user profile tab --%>
<wiki:Permission permission='editProfile'>
  <wiki:Tab id="profile" titleKey="login.register.tab" url="CreateProfile.jsp" />
</wiki:Permission>

<%-- Help tab --%>
<wiki:Tab id="loginhelp" titleKey="login.tab.help">
  <wiki:InsertPage page="LoginHelp" />
  <wiki:NoSuchPage page="LoginHelp">
  <div class="error">
    <fmt:message key="login.loginhelpmissing">
       <fmt:param><wiki:EditLink page="LoginHelp">LoginHelp</wiki:EditLink></fmt:param>
    </fmt:message>
  </div>
  </wiki:NoSuchPage>
</wiki:Tab>

</wiki:TabbedSection>