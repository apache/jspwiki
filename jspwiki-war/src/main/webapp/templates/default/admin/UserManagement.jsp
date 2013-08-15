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

<%@ page import="java.util.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.rpc.json.*" %>
<%@ page import="org.apache.wiki.ui.admin.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setLocale value="${prefs.Language}" />
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
   var userid = $('userid').getValue();

   if( userid == '--New--' ) return;

   Wiki.jsonrpc("users.getUserInfo", [userid], function(userprofile){
      $('loginname').value = userprofile.loginName;
      $('loginid').value = userprofile.loginName;
      $('fullname').value = userprofile.fullname;
      $('email').value = userprofile.email;
      $('lastmodified').setHTML(constructdate(userprofile.lastModified));
      $('creationdate').setHTML(constructdate(userprofile.created));
   });
}

function addNew()
{
  $('loginid').value = "--New--";
  $('loginname').value = "--New--";
  $('fullname').value = "Undefined";
  $('email').value = "";
  $('lastmodified').innerHTML = "";
  $('creationdate').innerHTML = "";
 
  var idlist=$('userid');
  var len = idlist.options.length;
  idlist.options[len] = new Option('--New--','--New--');
  idlist.selectedIndex = len;
}
</script>
<div>
   <p>
   This is a list of user accounts that exist in this system.
   </p>
   <p><wiki:Messages/></p>
   <div id="userlist">
      <select name="userid" id="userid" size="16" onchange="javascript:refreshUserInfo()">
         <c:forEach var="user" items="${engine.userManager.userDatabase.wikiNames}">
            <option value="${user.name}"><c:out value="${user.name}" escapeXml="true"/></option>
         </c:forEach>
      </select>
   </div>
   <div id="useredit">
   <form action="<wiki:Link jsp='admin/Admin.jsp' format='url'><wiki:Param name='tab-admin' value='users'/></wiki:Link>" 
       class="wikiform"
          id="adminuserform" 
    onsubmit="return Wiki.submitOnce(this);"
      method="post" accept-charset="<wiki:ContentEncoding/>"
     enctype="application/x-www-form-urlencoded" >
     <input type="hidden" name='bean' value='org.apache.wiki.ui.admin.beans.UserBean'/>
     <input type="hidden" id="loginid" name="loginid" value="" />
     <table>
     <tr>
       <td><label for="loginname">Login name</label></td>
       <td>
           <input type="text" name="loginname" id="loginname"
                  size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="password">Password </label></td>
       <td>
          <input type="password" name="password" id="password" size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="password2">Confirm password</label></td>
       <td>
         <input type="password" name="password2" id="password2" size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="fullname">Full name</label></td>
       <td>
         <input type="text" name="fullname" id="fullname"
                size="20" value="" />
       </td>
     </tr>
     <tr>
       <td><label for="email">Email</label></td>
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
     </tr>

     <tr>
        <td><input type="submit" name="action" value="Save"/></td>
     </tr>

     </table>
   <div id="useractions">
     <input type="submit" name="action" value="Remove" onclick="return( confirm('Are you sure you wish to remove this user?') && Wiki.submitOnce(this) );"/>      <input type="button" value="Add" onclick="javascript:addNew()"/>
   </div>
   </form>
   </div>
</div>