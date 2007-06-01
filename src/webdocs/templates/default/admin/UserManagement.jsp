<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.rpc.json.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.admin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.default"/>
<script>
function constructdate(date)
{
  var d = new Date();
  d.setTime(date.time);
  return d;
}

function refreshUserInfo()
{
   idlist = document.getElementById('userid');
   userid = idlist.options[idlist.selectedIndex].value;

   WikiAjax.request( function(userprofile){
      loginname = document.getElementById('loginname');
      fullname  = document.getElementById('fullname');
      email     = document.getElementById('email');
      lastmodified = document.getElementById('lastmodified');
      creationdate = document.getElementById('creationdate');

      loginname.value = userprofile.loginName;
      fullname.value  = userprofile.fullname;
      email.value     = userprofile.email;
      lastmodified.innerHTML = constructdate(userprofile.lastModified);
      creationdate.innerHTML = constructdate(userprofile.created);
   }, "users.getUserInfo", userid );
}

function addNew()
{
  document.getElementById('loginname').value = "Undefined";
  document.getElementById('fullname').value = "Undefined";
  document.getElementById('email').value = "Undefined";
  document.getElementById('lastmodified').innerHTML = "";
  document.getElementById('creationdate').innerHTML = "";
 
  idlist=document.getElementById('userid');
  var len = idlist.options.length;
  idlist.options[len] = new Option('Undefined','Undefined');
  idlist.selectedIndex = len;
}
</script>
<div>
   <p>
   This is a list of user accounts that exist in this system.
   </p>
   <div id="userlist">
      <select name="userid" id="userid" size="16" onchange="javascript:refreshUserInfo()">
         <c:forEach var="user" items="${engine.userManager.userDatabase.wikiNames}">
            <option><c:out value="${user.name}" escapeXml="true"/></option>
         </c:forEach>
      </select>
   </div>
   <div id="useredit">
      <form>
     <table>
     <tr>
       <td><label for="loginname"><fmt:message key="prefs.loginname"/></label></td>
       <td>
           <input type="text" name="loginname" id="loginname"
                  size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="password"><fmt:message key="prefs.password"/></label></td>
       <td>
          <input type="password" name="password" id="password" size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="password2"><fmt:message key="prefs.password2"/></label></td>
       <td>
         <input type="password" name="password2" id="password2" size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="fullname"><fmt:message key="prefs.fullname"/></label></td>
       <td>
         <input type="text" name="fullname" id="fullname"
                size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="email"><fmt:message key="prefs.email"/></label></td>
       <td>
         <input type="text" name="email" id="email"
                size="20" value="" />
       </td>
     </tr>

     <tr class="additinfo">
       <td><label>Creation date</label></td>
       <td class="formvalue" id="creationdate">
       </td>
     </tr>
     <tr class="additinfo">
       <td><label>Last modified</label></td>
       <td class="formvalue" id="lastmodified">
       </td>

     <tr>
        <td><input type="submit" value="Save"/></td>
     </tr>

     </table>
   </form>
   </div>
   <div id="useractions">
      <form>
         <input type="submit" value="Remove"/>
         <input type="button" value="Add" onclick="javascript:addNew()"/>
      </form>
   </div>
</div>