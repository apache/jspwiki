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
<form action="<wiki:CheckRequestContext 
     context='login'><wiki:Link jsp='Login.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext><wiki:CheckRequestContext 
     context='prefs'><wiki:Link jsp='UserPreferences.jsp' format='url'><wiki:Param name='tab'
       value='profile'/></wiki:Link></wiki:CheckRequestContext>" 
          id="editProfile" 
       class="wikiform"
    onsubmit="return Wiki.submitOnce( this );"
      method="post" accept-charset="UTF-8">

      <h3>
      <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile"/></wiki:UserProfile>
      <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile"/></wiki:UserProfile>
      </h3>

      <c:if test="${param.tab eq 'profile'}" >
        <div class="formhelp">
        <wiki:Messages div="error" topic="profile" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.profile")%>'/>
        </div>
      </c:if>

     <table>

     <!-- Login name -->
     <tr>
       <td><label for="loginname"><fmt:message key="prefs.loginname"/></label></td>
       <td>
         <wiki:UserProfile property="canChangeLoginName">
           <input type="text" name="loginname" id="loginname"
                  size="20" value="<wiki:UserProfile property='loginname' />" />
         </wiki:UserProfile>
         <wiki:UserProfile property="!canChangeLoginName">
           <!-- If user can't change their login name, it's because the container manages the login -->
           <wiki:UserProfile property="new">
             <div class="warning"><fmt:message key="prefs.loginname.cannotset.new"/></div>
           </wiki:UserProfile>
           <wiki:UserProfile property="exists">
             <span class="formvalue"><wiki:UserProfile property="loginname"/></span>
             <div class="warning"><fmt:message key="prefs.loginname.cannotset.exists"/></div>
           </wiki:UserProfile>
         </wiki:UserProfile>
       </td>
     </tr>

     <!-- Password; not displayed if container auth used -->
     <wiki:UserProfile property="canChangePassword">
       <tr>
         <td><label for="password"><fmt:message key="prefs.password"/></label></td>
         <td>
            <%--FIXME Enter Old PW to validate change flow, not yet treated by JSPWiki
            <label for="password0">Old</label>&nbsp;
            <input type="password" name="password0" id="password0" size="20" value="" />
            &nbsp;&nbsp;--%>
            <input type="password" name="password" id="password" size="20" value="" />
          </td>
        </tr>
        <tr>
          <td><label for="password2"><fmt:message key="prefs.password2"/></label></td>
          <td>
            <input type="password" name="password2" id="password2" size="20" value="" />
            <%-- extra validation ? min size, allowed chars? --%>
         </td>
       </tr>
     </wiki:UserProfile>

     <!-- Full name -->
     <tr>
       <td><label for="fullname"><fmt:message key="prefs.fullname"/></label></td>
       <td>
         <input type="text" name="fullname" id="fullname"
                size="20" value="<wiki:UserProfile property='fullname'/>" />
         <span class="formhelp"><fmt:message key="prefs.fullname.description"/></span>
       </td>
     </tr>

     <!-- E-mail -->
     <tr>
       <td><label for="email"><fmt:message key="prefs.email"/></label></td>
       <td>
         <input type="text" name="email" id="email"
                size="20" value="<wiki:UserProfile property='email' />" />
         <span class="formhelp"><fmt:message key="prefs.email.description"/></span>
       </td>
     </tr>

     <wiki:UserProfile property="exists">
     <tr class="additinfo">
       <td><label><fmt:message key="prefs.roles"/></label></td>
       <td><div class="formvalue"><wiki:UserProfile property="roles" /></div></td>
     </tr>
     <tr class="additinfo">
       <td><label><fmt:message key="prefs.groups"/></label></td>
       <td>
         <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
         <div class="formvalue"><wiki:UserProfile property="groups" /></div>
         <div class="formhelp"><fmt:message key="prefs.acl.info" /></div>
       </td>
     </tr>

     <tr class="additinfo">
       <td><label><fmt:message key="prefs.creationdate"/></label></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="created"/>--%>
 	     <fmt:formatDate value="<%= profile.getCreated() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </td>
     </tr>
     <tr class="additinfo">
       <td><label><fmt:message key="prefs.profile.lastmodified"/></label></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="modified"/>--%>
 	     <fmt:formatDate value="<%= profile.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </td>
     </tr>
     </wiki:UserProfile>

     <tr>
       <td>&nbsp;</td>
       <td>
       <wiki:UserProfile property="exists">
        <input type="submit" name="ok" value="<fmt:message key='prefs.save.submit' />" />
       </wiki:UserProfile>
       <wiki:UserProfile property="new">
         <input type="submit" name="ok" value="<fmt:message key='prefs.save.submit' />" />
       </wiki:UserProfile>
       <input type="hidden" name="redirect" value="<wiki:Variable var='redirect' default='' />" />
       <input type="hidden" name="action" value="saveProfile" />

       <wiki:UserCheck status="assertionsAllowed">
          <div class="formhelp"><fmt:message key="prefs.cookie.info"/></div>
        </wiki:UserCheck>
       </td>
     </tr>
   </table>
</form>
