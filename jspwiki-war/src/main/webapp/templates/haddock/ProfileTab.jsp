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
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.user.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  /* dateformatting not yet supported by wiki:UserProfile tag - diy */
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  UserManager manager = wikiContext.getEngine().getUserManager();
  UserProfile profile = manager.getUserProfile( wikiContext.getWikiSession() );
%>
<form method="post" accept-charset="UTF-8"
      action="<wiki:CheckRequestContext
     context='login'><wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext><wiki:CheckRequestContext
     context='!login'><wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext>"
       class=""
          id="editProfile">

  <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />

  <div class="form-group">
    <span class="form-col-20 control-label"></span>
    <span class="dropdown" style="display:inline-block" >
      <button class="btn btn-success" type="submit" name="action" value="saveProfile">
        <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile"/></wiki:UserProfile>
        <wiki:UserProfile property="new"><fmt:message key='prefs.newprofile' /></wiki:UserProfile>   <%-- prefs.save.submit??--%>
      </button>
      <wiki:UserCheck status="assertionsAllowed">
        <ul class="dropdown-menu" data-hover-parent=".dropdown">
          <li class="dropdown-header"><fmt:message key="prefs.cookie.info"/></li>
        </ul>
      </wiki:UserCheck>
    </span>

  </div>

  <c:if test="${param.tab eq 'profile'}" >
  <div class="">
    <span class="form-col-20 control-label"></span>
    <fmt:message key="prefs.errorprefix.profile" var="msg"/>
    <wiki:Messages div="alert alert-danger form-col-50" topic="profile" prefix="${msg}" />
    <%-- seems not to work .?
    <wiki:Messages div="error form-col-50" prefix="<fmt:message key='prefs.errorprefix.profile' />" topic="profile"  />
    this is ok..
    <wiki:Messages div="error form-col-50" topic="profile" >
      <jsp:attribute name="prefix" ><fmt:message key="prefs.errorprefix.profile"/></jsp:attribute>
    </wiki:Messages>
    --%>
  </div>
  </c:if>

     <!-- Login name -->
     <div class="form-group">
       <label class="control-label form-col-20" for="loginname"><fmt:message key="prefs.loginname"/></label>

       <wiki:UserProfile property="canChangeLoginName">
           <input class="form-control form-col-50" type="text" name="loginname" id="loginname"
                  size="20" value="<wiki:UserProfile property='loginname' />" />
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

     <!-- Password field; not displayed if container auth used -->
     <wiki:UserProfile property="canChangePassword">
     <div class="form-group">
       <label class="control-label form-col-20" for="password"><fmt:message key="prefs.password"/></label>
       <%--FIXME Enter Old PW to validate change flow, not yet treated by JSPWiki
            <label class="control-label form-col-20" for="password0">Old</label>&nbsp;
            <input type="password" name="password0" id="password0" size="20" value="" />
       --%>
       <input class="form-control form-col-50" type="password" name="password" id="password" size="20" value="" />
     </div>
     <div class="form-group">
       <label class="control-label form-col-20" for="password2"><fmt:message key="prefs.password2"/></label>
       <input class="form-control form-col-50" type="password" name="password2" id="password2" size="20" value="" />
       <%-- extra validation ? min size, allowed chars? password-strength js routing --%>
     </div>
     </wiki:UserProfile>

     <!-- Full name -->
     <div class="form-group">
       <label class="control-label form-col-20" for="fullname"><fmt:message key="prefs.fullname"/></label>
       <span class="dropdown form-col-50">
         <input class="form-control" type="text" name="fullname" id="fullname" size="20"
                value="<wiki:UserProfile property='fullname' />" />
         <ul class="dropdown-menu" data-hover-parent=".dropdown">
           <li class="dropdown-header"><fmt:message key="prefs.fullname.description"/></li>
         </ul>
       </span>
     </div>

     <!-- E-mail -->
     <div class="form-group">
       <label class="control-label form-col-20" for="email"><fmt:message key="prefs.email"/></label>
       <span class="dropdown form-col-50">
         <input class="form-control" type="text" name="email" id="email" size="20"
                value="<wiki:UserProfile property='email' />" />
         <ul class="dropdown-menu" data-hover-parent=".dropdown">
           <li class="dropdown-header"><fmt:message key="prefs.email.description"/></li>
         </ul>
       </span>
     </div>

     <wiki:UserProfile property="exists">
     <div class="xinformation form-group">
     <div class="xform-group">
       <label class="control-label form-col-20"><fmt:message key="prefs.roles"/></label>
       <div class="form-control-static form-col-50"><wiki:UserProfile property="roles" /></div>
     </div>
     <div class="xform-group">
       <label class="control-label form-col-20"><fmt:message key="prefs.groups"/></label>

       <span class="dropdown form-col-50">
         <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
         <div class="form-control-static"><wiki:UserProfile property="groups" /></div>
         <ul class="dropdown-menu" data-hover-parent=".dropdown">
           <li class="dropdown-header"><fmt:message key="prefs.acl.info" /></li>
         </ul>
       </span>
     </div>
     <div class="xform-group">
       <label class="control-label form-col-20"><fmt:message key="prefs.creationdate"/></label>
       <div class="form-control-static form-col-50">
 	     <fmt:formatDate value="<%= profile.getCreated() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </div>
     </div>
     <div class="xform-group">
       <label class="control-label form-col-20"><fmt:message key="prefs.profile.lastmodified"/></label>
       <div class="form-control-static form-col-50">
         <%--<wiki:UserProfile property="modified"/>--%>
 	     <fmt:formatDate value="<%= profile.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </div>
     </div>
     </div>
     </wiki:UserProfile>

</form>
