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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.action.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
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
        postURL = "/Login.jsp";
    }
%>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="${param.tab}">
    
      <wiki:UserCheck status="notauthenticated">
        <%-- Login tab --%>
        <wiki:Tab id="login" titleKey="login.tab">
          <div class="formcontainer">
            <s:form action="<%=postURL%>" id="login" class="wikiform" acceptcharset="UTF-8">
              <s:param name="tab" value="login" />
              <h3>
                <fmt:message key="login.heading.login">
                  <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                </fmt:message>
              </h3>

              <%-- Global form errors and messages --%>
              <div class="formhelp"><fmt:message key="login.help"></fmt:message></div>
              <div class="instructions"><s:messages key="login" /></div>
              <div class="errors"><s:errors beanclass="org.apache.wiki.action.LoginActionBean" /></div>

              <%-- Username --%>
              <div>
                <s:label for="j_username" name="loginName" />
                <s:text size="24" name="j_username" id="j_username"><wiki:Variable var="uid" default="" /></s:text>
              </div>

              <%-- Password --%>
              <div>
                <s:label for="j_password" name="password" />
                <s:password size="24" name="j_password" id="j_password" />
              </div>
              <c:if test="${wikiEngine.authenticationManager.cookieAuthenticated}">
                <div>
                  <s:label for="remember" />
                  <s:checkbox name="remember" id="j_remember" />
                </div>
              </c:if>
              
              <%-- Login submit button --%>
              <div class="description"><s:submit name="login" /></div>
                
              <%-- Help --%>
              <div class="formhelp">
                <fmt:message key="login.lostpw" />
                <s:link beanclass="org.apache.wiki.action.LostPasswordActionBean">
                  <fmt:message key="login.lostpw.getnew" />
                </s:link>
              </div>
              <div class="formhelp">
                <fmt:message key="login.nopassword" />
                <s:link beanclass="org.apache.wiki.action.UserProfileActionBean" event="create">
                  <fmt:message key="login.registernow">
                    <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                  </fmt:message>
                </s:link>
              </div>
            </s:form>
          </div>
        </wiki:Tab>
      </wiki:UserCheck>

      <%-- Lost password tab --%>
      <wiki:Tab id="reset" titleKey="login.lostpw.tab">
        <div class="formcontainer">
          <s:form beanclass="org.apache.wiki.action.LostPasswordActionBean" id="lostpw" class="wikiform" acceptcharset="UTF-8">
            <s:param name="tab" value="reset" />
            <h3><fmt:message key="login.lostpw.heading" /></h3>

            <%-- Global form errors and messages --%>
            <div class="formhelp"><fmt:message key="login.lostpw.help"></fmt:message></div>
            <div class="instructions"><s:messages key="reset" /></div>
            <div class="errors"><s:errors beanclass="org.apache.wiki.action.LostPasswordActionBean"/></div>
            
            <%-- E-mail --%>
            <div>
              <s:label for="email" />
              <s:text size="24" name="email" id="email" />
              <s:errors field="email" />
            </div>
            
            <%-- Reset submit button --%>
            <s:submit name="reset" />
          
            <%-- Help --%>
            <div class="formhelp">
              <fmt:message key="login.invite" />
              <s:link beanclass="org.apache.wiki.action.UserProfileActionBean" event="create">
                <fmt:message key="login.heading.login">
                  <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                </fmt:message>
              </s:link>
            </div>
            <div class="formhelp">
              <fmt:message key="login.nopassword" />
              <s:link beanclass="org.apache.wiki.action.LostPasswordActionBean">
                <s:param name="tab" value="reset" />
                <fmt:message key="login.registernow">
                  <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
                </fmt:message>
              </s:link>
            </div>
          </s:form>
        </div>
      </wiki:Tab>
      
      <%-- New user tab --%>
      <wiki:Permission permission="editProfile">
        <wiki:Tab id="profile" titleKey="login.register.tab"
          beanclass="org.apache.wiki.action.UserProfileActionBean" event="create" />
      </wiki:Permission>
      
      <%-- Help tab --%>
      <wiki:Tab id="help" titleKey="login.tab.help">
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
  </s:layout-component>
</s:layout-render>
