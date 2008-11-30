<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/stripes.tld" prefix="stripes" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  /* dateformatting not yet supported by wiki:UserProfile tag - diy */
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  UserManager manager = wikiContext.getEngine().getUserManager();
  UserProfile profile = manager.getUserProfile( wikiContext.getWikiSession() );
%>
<stripes:form beanclass="com.ecyrd.jspwiki.action.UserPreferencesActionBean"
          id="editProfile"
       class="wikiform"
      method="post" acceptcharset="${wikiEngine.contentEncoding}">
      <stripes:param name="tab" value="profile"/>

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
           <stripes:text name="loginname" id="loginname"
                  size="20"><wiki:UserProfile property="loginname"/></stripes:text>
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
            <label for="password">Old</label>&nbsp;
            <input type="password" name="password0" id="password0" size="20" value="" />
            &nbsp;&nbsp;--%>
            <stripes:password name="password" id="password" size="20" value="" />
          </td>
        </tr>
        <tr>
          <td><label for="passwordAgain"><fmt:message key="prefs.password2"/></label></td>
          <td>
            <stripes:password name="passwordAgain" id="passwordAgain" size="20" value="" />
            <%-- extra validation ? min size, allowed chars? --%>
         </td>
       </tr>
     </wiki:UserProfile>

     <!-- Full name -->
     <tr>
       <td><label for="fullname"><fmt:message key="prefs.fullname"/></label></td>
       <td>
         <stripes:text name="fullname" id="fullname"
                size="20"><wiki:UserProfile property="fullname"/></stripes:text>
         <div class="formhelp"><fmt:message key="prefs.fullname.description"/></div>
       </td>
     </tr>

     <!-- E-mail -->
     <tr>
       <td><label for="email"><fmt:message key="prefs.email"/></label></td>
       <td>
         <stripes:text name="email" id="email"
                size="20"><wiki:UserProfile property="email" /></stripes:text>
         <div class="formhelp"><fmt:message key="prefs.email.description"/></div>
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
        <stripes:submit name="saveProfile"><fmt:message key="prefs.save.submit"/></stripes:submit>
       </wiki:UserProfile>
       <wiki:UserProfile property="new">
        <stripes:submit name="saveProfile"><fmt:message key="prefs.save.submit"/></stripes:submit>
       </wiki:UserProfile>

       <wiki:UserCheck status="assertionsAllowed">
          <div class="formhelp"><fmt:message key="prefs.cookie.info"/></div>
        </wiki:UserCheck>
       </td>
     </tr>
   </table>
</stripes:form>
