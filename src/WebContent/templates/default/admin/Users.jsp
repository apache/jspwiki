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
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/DefaultLayout.jsp']}">
  <s:layout-component name="headTitle">
    JSPWiki Administration
  </s:layout-component>
  
  <s:layout-component name="pageTitle">
    JSPWiki Administration
  </s:layout-component>

  <s:layout-component name="content">
    <h1>JSPWiki Administration</h1>

    <wiki:TabbedSection defaultTab="${param['tab']}">
    
      <wiki:Tab id="security" title="Security"
        beanclass="org.apache.wiki.action.AdminActionBean">
        <wiki:Param name="tab" value="security" />
      </wiki:Tab>

      <wiki:Tab id="users" title="Users">
        <p>
          This is a list of user accounts that exist in this system.
        </p>
        <div id="useredit">
          <s:form beanclass="org.apache.wiki.action.AdministerProfilesActionBean"
            class="wikiform" id="adminuserform" acceptcharset="UTF-8">
            <p><s:messages/></p>
            <table>
              <thead>
                <tr>
                  <th><s:label for="profile.loginName" /></th>
                  <th><s:label for="profile.fullname" /></th>
                  <th><s:label for="profile.email" name="email" /></th>
                  <td><label><fmt:message key="prefs.creationdate" /></label></td>
                  <td><label><fmt:message key="prefs.profile.lastmodified" /></label></td>
                  <th><s:label for="profile.password" /></th>
                </tr>
              </thead>
              <c:forEach var="user" items="${wikiActionBean.users}" varStatus="loop">
                <tr>
                  <td><s:text name="users[${loop.index}].loginName" size="20" /></td>
                  <td><s:text name="users[${loop.index}].fullname" id="fullname" size="20" /></td>
                  <td><s:text name="users[${loop.index}].email" id="email" size="20" /></td>
                  <td>${user.created}</td>
                  <td>${user.lastModified}</td>
                  <td><s:password name="users[${loop.index}].password" size="20" value="" /></td>
                </tr>
              </c:forEach>
            </table>
          </s:form>
        </div>
      </wiki:Tab>
        
      <wiki:Tab id="groups" title="Groups"
        beanclass="org.apache.wiki.action.AdminActionBean">
        <wiki:Param name="tab" value="groups" />
      </wiki:Tab>
  
      <wiki:Tab id="filters" title="Filters"
        beanclass="org.apache.wiki.action.AdminActionBean">
        <wiki:Param name="tab" value="filters" />
      </wiki:Tab>
      
    </wiki:TabbedSection>

  </s:layout-component>
</s:layout-render>
