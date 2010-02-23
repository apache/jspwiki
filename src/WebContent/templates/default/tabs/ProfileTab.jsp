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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<h3>
  <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile" /></wiki:UserProfile>
  <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile" /></wiki:UserProfile>
</h3>

<%-- Global form errors and messages --%>
<div class="formhelp"><fmt:message key="login.help"></fmt:message></div>
<div class="instructions"><s:messages key="login" /></div>
<div class="errors"><s:errors beanclass="org.apache.wiki.action.UserProfileActionBean" globalErrorsOnly="true" /></div>

<div class="formcontainer">
  <s:form beanclass="org.apache.wiki.action.UserProfileActionBean" id="editProfile" class="wikiform" method="post" acceptcharset="UTF-8">
    <s:param name="tab" value="profile" />
  
    <%-- Login name --%>
    <div>
      <s:label for="profile.loginName" />
      <wiki:UserProfile property="canChangeLoginName">
        <s:text name="profile.loginName" id="loginName" size="20"><wiki:UserProfile property="loginname" /></s:text>
        <s:errors field="profile.loginName" />
      </wiki:UserProfile>
      <wiki:UserProfile property="!canChangeLoginName">
        <!-- If user can't change their login name, it's because the container manages the login -->
        <wiki:UserProfile property="new">
          <div class="warning"><fmt:message key="prefs.loginname.cannotset.new" /></div>
        </wiki:UserProfile>
        <wiki:UserProfile property="exists">
          <span class="formvalue"><wiki:UserProfile property="loginname" /></span>
          <div class="warning"><fmt:message key="prefs.loginname.cannotset.exists" /></div>
        </wiki:UserProfile>
      </wiki:UserProfile>
    </div>
  
    <%-- Password; not displayed if container authentication used --%>
    <wiki:UserProfile property="canChangePassword">
      <div>
        <s:label for="profile.password" />
        <s:password name="profile.password" id="password" size="20" value="" />
        <s:errors field="profile.password" />
      </div>
      <div>
        <s:label for="passwordAgain" />
        <s:password name="passwordAgain" id="passwordAgain" size="20" value="" />
        <s:errors field="profile.passwordAgain" />
      </div>
    </wiki:UserProfile>
    
    <%-- Full name --%>
    <div>
      <s:label for="profile.fullname" />
      <wiki:UserProfile property="canChangeFullname">
        <s:text name="profile.fullname" id="fullname" size="20"><wiki:UserProfile property="fullname" /></s:text>
        <s:errors field="profile.fullname" />
      </wiki:UserProfile>
      <wiki:UserProfile property="!canChangeFullname">
        <wiki:UserProfile property="fullname" />
      </wiki:UserProfile>
      <div class="description"><fmt:message key="prefs.fullname.description" /></div>
    </div>
    
    <%-- E-mail --%>
    <div>
      <s:label for="profile.email" name="email" />
      <wiki:UserProfile property="canChangeEmail">
        <s:text name="profile.email" id="email" size="20"><wiki:UserProfile property="email" /></s:text>
        <s:errors field="profile.email" />
      </wiki:UserProfile>
      <wiki:UserProfile property="!canChangeEmail">
        <wiki:UserProfile property="email" />
      </wiki:UserProfile>
      <div class="description"><fmt:message key="prefs.email.description" /></div>
    </div>
    
    <%-- Creation and group info --%>
    <wiki:UserProfile property="exists">
      <div>
        <fmt:message key="prefs.roles" />
        <div class="formvalue"><wiki:UserProfile property="roles" /></div>
      </div>
      <div>
        <fmt:message key="prefs.groups" />
        <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
        <div class="formvalue"><wiki:UserProfile property="groups" /></div>
        <div class="formhelp"><fmt:message key="prefs.acl.info" /></div>
      </div>
      <div>
        <fmt:message key="prefs.creationdate" />
        <fmt:formatDate value="${profile.Created}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
      </div>
      <tr class="additinfo">
        <fmt:message key="prefs.profile.lastmodified" />
        <fmt:formatDate value="${profile.LastModified}" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
      </div>
    </wiki:UserProfile>
    
    <%-- Spam protection: password confirmation or CAPTCHA --%>
    <wiki:UserCheck status="notAuthenticated">
      <div>
        <wiki:SpamProtect challenge="captcha" />
      </div>
    </wiki:UserCheck>
      
    <%-- Save submit button --%>
    <div>
      <s:submit name="save" />
      <wiki:UserCheck status="assertionsAllowed">
        <div class="description"><fmt:message key="prefs.cookie.info" /></div>
      </wiki:UserCheck>
    </div>
    
  </s:form>
</div>