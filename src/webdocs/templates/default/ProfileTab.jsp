<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<stripes:form beanclass="com.ecyrd.jspwiki.action.UserProfileActionBean" id="editProfile" class="wikiform" method="post" acceptcharset="UTF-8">
      <stripes:param name="tab" value="profile" />

      <h3>
      <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile" /></wiki:UserProfile>
      <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile" /></wiki:UserProfile>
      </h3>

      <c:if test="${param.tab eq 'profile'}">
        <div class="formhelp">
        <wiki:Messages div="error" topic="profile" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.profile")%>' />
        </div>
      </c:if>

     <table>

     <!-- Login name -->
     <tr>
       <td><stripes:label for="profile.loginName" name="prefs.loginname" /></td>
       <td>
         <wiki:UserProfile property="canChangeLoginName">
           <stripes:text name="profile.loginName" id="loginName" size="20"><wiki:UserProfile property="loginname" /></stripes:text>
           <stripes:errors field="profile.loginName" />
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
       </td>
     </tr>

     <!-- Password; not displayed if container auth used -->
     <wiki:UserProfile property="canChangePassword">
       <tr>
         <td><stripes:label for="profile.password" name="prefs.password" /></td>
         <td>
           <stripes:password name="profile.password" id="password" size="20" value="" />
           <stripes:errors field="profile.password" />
          </td>
        </tr>
        <tr>
          <td><stripes:label for="passwordAgain" name="prefs.password2" /></td>
          <td>
           <stripes:password name="passwordAgain" id="passwordAgain" size="20" value="" />
           <stripes:errors field="profile.passwordAgain" />
         </td>
       </tr>
     </wiki:UserProfile>

     <!-- Full name -->
     <tr>
       <td><stripes:label for="profile.fullname" name="prefs.fullname" /></td>
       <td>
         <stripes:text name="profile.fullname" id="fullname" size="20"><wiki:UserProfile property="fullname" /></stripes:text>
          <stripes:errors field="profile.fullname" />
         <div class="formhelp"><fmt:message key="prefs.fullname.description" /></div>
       </td>
     </tr>

     <!-- E-mail -->
     <tr>
       <td><stripes:label for="profile.email" name="prefs.email" /></td>
       <td>
         <stripes:text name="profile.email" id="email" size="20"><wiki:UserProfile property="email" /></stripes:text>
         <stripes:errors field="profile.email" />
         <div class="formhelp"><fmt:message key="prefs.email.description" /></div>
       </td>
     </tr>

     <wiki:UserProfile property="exists">
     <tr class="additinfo">
       <td><stripes:label name="prefs.roles" /></td>
       <td><div class="formvalue"><wiki:UserProfile property="roles" /></div></td>
     </tr>
     <tr class="additinfo">
       <td><stripes:label name="prefs.groups" /></td>
       <td>
         <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
         <div class="formvalue"><wiki:UserProfile property="groups" /></div>
         <div class="formhelp"><fmt:message key="prefs.acl.info" /></div>
       </td>
     </tr>

     <tr class="additinfo">
       <td><stripes:label name="prefs.creationdate" /></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="created"/>--%>
 	     <fmt:formatDate value="${profile.Created}" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </td>
     </tr>
     <tr class="additinfo">
       <td><stripes:label name="prefs.profile.lastmodified" /></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="modified"/>--%>
 	     <fmt:formatDate value="${profile.LastModified}" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
       </td>
     </tr>
     </wiki:UserProfile>

     <tr>
       <td>&nbsp;</td>
       <td>
       <wiki:UserProfile property="exists">
        <stripes:submit name="save"><fmt:message key="prefs.save.submit" /></stripes:submit>
       </wiki:UserProfile>
       <wiki:UserProfile property="new">
        <stripes:submit name="save"><fmt:message key="prefs.save.submit" /></stripes:submit>
       </wiki:UserProfile>

       <wiki:UserCheck status="assertionsAllowed">
          <div class="formhelp"><fmt:message key="prefs.cookie.info" /></div>
        </wiki:UserCheck>
       </td>
     </tr>
   </table>
</stripes:form>
