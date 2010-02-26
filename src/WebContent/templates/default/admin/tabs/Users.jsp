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
<%@ page errorPage="${templates['Error.jsp']}" %>
<div>
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
            <th><s:label for="profile.password" /></th>
            <th><s:label for="passwordAgain" /></th>
            <th><s:label for="profile.email" name="email" /></th>
            <td><label><fmt:message key="prefs.creationdate" /></label></td>
            <td><label><fmt:message key="prefs.profile.lastmodified" /></label></td>
          </tr>
        </thead>
        <c:forEach var="user" items="${wikiActionBean.users}">
          <tr>
            <td><s:text name="loginName" id="loginName" size="20" value="${user.loginName}" /></td>
            <td><s:password name="password" id="password" size="20" value="" /></td>
            <td></td>
            <td><s:text name="fullname" id="fullname" size="20" value="${user.Fullname}" /></td>
            <td><s:text name="email" id="email" size="20" value="${user.email}" /></td>
            <td>${user.created}</td>
            <td>${user.lastModified}</td>
          </tr>
        </c:forEach>
      </table>
    </s:form>
  </div>
</div>