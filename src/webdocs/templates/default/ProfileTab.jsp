<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.user.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  /* dateformatting not yet supported by wiki:UserProfile tag - diy */
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  UserManager manager = wikiContext.getEngine().getUserManager();
  UserProfile profile = manager.getUserProfile( wikiContext.getWikiSession() );
%>

<form action="<wiki:Link jsp='UserPreferences.jsp'
      format='url'><wiki:Param name='tab' value='profile'/></wiki:Link>"
        name="editProfile" id="editProfile"
       class="wikiform"
    onsubmit="return Wiki.submitOnce( this );"
      method="post" accept-charset="UTF-8">

      <h3>
      <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile"/></wiki:UserProfile>
      <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile"/></wiki:UserProfile>
      </h3>
      <%-- trivial help
      <div class="formhelp">
      <wiki:UserProfile property="new"><fmt:message key="prefs.newprofile.help"/></wiki:UserProfile>
      <wiki:UserProfile property="exists"><fmt:message key="prefs.oldprofile.help"/></wiki:UserProfile>
      </div>
      --%>

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
           <span class="formvalue"><wiki:UserProfile property="loginname"/></span>
         </wiki:UserProfile>
         <span class="formhelp"><fmt:message key="prefs.loginname.description"/></span>
       </td>
     </tr>

     <!-- Password; not displayed if container auth used -->
     <wiki:UserProfile property="canChangePassword">
       <tr>
         <td><label for="password"><fmt:message key="prefs.password"/></td>
         <%--
               <wiki:UserProfile property="exists">Change Password</wiki:UserProfile>
               <wiki:UserProfile property="new">Set Password</wiki:UserProfile>
         --%>
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
            <%-- trivial help
            <div class="formhelp">
              <fmt:message key="prefs.password.description"/>
              <fmt:message key="prefs.password2.description"/>
            </div>
            --%>
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
       <td><label>Roles</label></td>
       <td><div class="formvalue"><wiki:UserProfile property="roles" /></div></td>
     </tr>
     <tr class="additinfo">
       <td><label>Groups</label></td>
       <td>
         <%-- TODO this should become clickable group links so you can immediately go and look at them if you want --%>
         <div class="formvalue"><wiki:UserProfile property="groups" /></div>
         <div class="formhelp"><fmt:message key="prefs.acl.info" /></div>
       </td>
     </tr>

     <tr class="additinfo">
       <td><label>Creation date</label></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="created"/>--%>
 	     <fmt:formatDate value="<%= profile.getCreated() %>" pattern="${prefDateFormat}" />
       </td>
     </tr>
     <tr class="additinfo">
       <td><label>Last modified</label></td>
       <td class="formvalue">
         <%--<wiki:UserProfile property="modified"/>--%>
 	     <fmt:formatDate value="<%= profile.getLastModified() %>" pattern="${prefDateFormat}" />
       </td>
     </tr>
     </wiki:UserProfile>

     <tr>
       <td />
       <td>
       <wiki:UserProfile property="exists">
        <input type="submit" name="ok" value="Save User Profile" style="display:none;" />
        <input type="button" name="ox" value="<fmt:message key='prefs.save.submit' />" onclick="this.form.ok.click();" />
       </wiki:UserProfile>
       <wiki:UserProfile property="new">
         <input type="submit" name="ok" value="Create User Profile" style="display:none;" />
         <input type="button" name="ox" value="<fmt:message key='prefs.save.submit' />" onclick="this.form.ok.click();" />
       </wiki:UserProfile>
       <input type="hidden" name="action" value="saveProfile" />

       <wiki:UserCheck status="assertionsAllowed">
          <div class="formhelp"><fmt:message key="prefs.cookie.info"/></div>
        </wiki:UserCheck>
       <%-- trivial msg <fmt:message key="prefs.save.description"/> --%>
       </td>
     </tr>

   </table>

</form>

