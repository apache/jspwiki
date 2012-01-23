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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="content">
    <wiki:TabbedSection defaultTab="${param.tab}">
    
      <wiki:UserCheck status="notauthenticated">
        <%-- Login tab --%>
        <wiki:Tab id="login" titleKey="login.tab"
          beanclass="org.apache.wiki.action.LoginActionBean"/>
        
        <%-- Lost password tab --%>
        <wiki:Tab id="reset" titleKey="login.lostpw.tab"
          beanclass="org.apache.wiki.action.LostPasswordActionBean">
          <wiki:Param name="tab" value="reset" />
        </wiki:Tab>
      </wiki:UserCheck>
      
      <%-- New user tab --%>
      <wiki:Permission permission="editProfile">
        <wiki:Tab id="profile" titleKey="login.register.tab">
          <jsp:include page="${templates['tabs/ProfileTab.jsp']}" />
        </wiki:Tab>
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
