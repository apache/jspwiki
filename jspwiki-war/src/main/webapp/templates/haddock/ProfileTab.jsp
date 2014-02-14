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
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.user.*" %>
<%@ page errorPage="/Error.jsp" %>
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
      <h3>
      <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile"/></wiki:UserProfile>
      <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile"/></wiki:UserProfile>
      </h3>

<form action="<wiki:CheckRequestContext 
     context='login'><wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext><wiki:CheckRequestContext 
     context='prefs'><wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext>" 
          id="editProfile" 
       class=""
      method="post" accept-charset="UTF-8">


      <c:if test="${param.tab eq 'profile'}" >
        <div class="help-block">
        <wiki:Messages div="error" topic="profile" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.profile")%>'/>
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

     <!-- Password; not displayed if container auth used -->
     <wiki:UserProfile property="canChangePassword">
     <div class="form-group">
         <label class="control-label form-col-20" for="password"><fmt:message key="prefs.password"/></label>
         
            <%--FIXME Enter Old PW to validate change flow, not yet treated by JSPWiki
            <label class="control-label form-col-20" for="password0">Old</label>&nbsp;
            <input type="password" name="password0" id="password0" size="20" value="" />
            &nbsp;&nbsp;--%>
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
       
         <input class="form-control form-col-50" type="text" name="fullname" id="fullname"
                size="20" value="<wiki:UserProfile property='fullname'/>" />
         <p class="help-block form-col-offset-20"><fmt:message key="prefs.fullname.description"/></p>
       
     </div>

     <!-- E-mail -->
     <div class="form-group">
       <label class="control-label form-col-20" for="email"><fmt:message key="prefs.email"/></label>
         <input class="form-control form-col-50" type="text" name="email" id="email"
                size="20" value="<wiki:UserProfile property='email' />" />
         <p class="help-block form-col-offset-20"><fmt:message key="prefs.email.description"/></p>
     </div>

     <wiki:UserProfile property="exists">
     <div class="information">
     <div class="xform-group"> <%--class="additinfo"--%>
       <label class="control-label form-col-20"><fmt:message key="prefs.roles"/></label>
       <div class="form-control-static  form-col-50"><wiki:UserProfile property="roles" /></div>
     </div>
     <div class="xform-group"> <%--class="additinfo"--%>
       <label class="control-label form-col-20"><fmt:message key="prefs.groups"/></label>
       
         <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
         <div class="form-control-static  form-col-50"><wiki:UserProfile property="groups" /></div>
         <p class="help-block form-col-offset-20"><fmt:message key="prefs.acl.info" /></p>
       
     </div>

     <div class="xform-group"> <%--class="additinfo"--%>
       <label class="control-label form-col-20"><fmt:message key="prefs.creationdate"/></label>
       <div class="form-control-static form-col-50">
         <%--<wiki:UserProfile property="created"/>--%>
 	     <fmt:formatDate value="<%= profile.getCreated() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </div>
     </div>
     <div class="xform-group"> <%--class="additinfo"--%>
       <label class="control-label form-col-20"><fmt:message key="prefs.profile.lastmodified"/></label>
       <div class="form-control-static form-col-50">
         <%--<wiki:UserProfile property="modified"/>--%>
 	     <fmt:formatDate value="<%= profile.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </div> 
     </div>
     </div>
     </wiki:UserProfile>

     <div class="form-group"> 
       <wiki:UserProfile property="exists">
        <input class="btn btn-primary form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.oldprofile'/>" />
        <%--input class="btn btn-primary form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.save.submit' />" /--%>
       </wiki:UserProfile>
       <wiki:UserProfile property="new">
         <input class="btn btn-primary form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.newprofile' />" />
         <%--input class="btn btn-primary form-col-offset-20" type="submit" name="ok" value="<fmt:message key='prefs.save.submit' />" /--%>
       </wiki:UserProfile>
       <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
       <input type="hidden" name="action" value="saveProfile" />

       <wiki:UserCheck status="assertionsAllowed">
          <div class="help-block form-col-offset-20"><fmt:message key="prefs.cookie.info"/></div>
        </wiki:UserCheck>
     </div>

</form>
