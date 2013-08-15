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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.apache.wiki.auth.PrincipalComparator" %>
<%@ page import="org.apache.wiki.auth.authorize.Group" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ page errorPage="/Error.jsp" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>

<%
  WikiContext c = WikiContext.findContext( pageContext );

  // Extract the group name and members
  String name = request.getParameter( "group" );
  Group group = (Group)pageContext.getAttribute( "Group",PageContext.REQUEST_SCOPE );
  Principal[] members = null;

  if ( group != null )
  {
    name = group.getName();
    members = group.members();
    Arrays.sort( members, new PrincipalComparator() );
  }
  name = TextUtil.replaceEntities(name);
%>

<wiki:TabbedSection defaultTab="editgroup">

  <wiki:Permission permission="viewGroup">
  <wiki:Tab id="viewgroup" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.viewgroup")%>'
           url='<%=c.getURL(WikiContext.NONE, "Group.jsp", "group="+request.getParameter("group") ) %>'
           accesskey="v" >
  </wiki:Tab>
  </wiki:Permission>

  <wiki:Tab id="editgroup" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "editgroup.tab")%>'>

  <h3><%=name%></h3>

  <form action="<wiki:Link format='url' jsp='EditGroup.jsp'/>"
         class="wikiform"
            id="editGroup"
        method="POST" accept-charset="UTF-8">

    <!-- Members -->
    <%
      StringBuffer s = new StringBuffer();
      for ( int i = 0; i < members.length; i++ )
      {
        s.append( members[i].getName().trim() );
        s.append( '\n' );
      }
    %>
    <div class="formhelp">
    <fmt:message key="editgroup.instructions">
      <fmt:param><%=name%></fmt:param>
     </fmt:message>
    </div>
    <div class="formhelp">
      <wiki:Messages div="error" topic="group" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"editgroup.saveerror") %>' />
    </div>

    <table class="wikitable">
    <tr>
      <th><fmt:message key="group.name"/></th>
      <td><%=name%></td>
    </tr>
    <tr>
      <th><label><fmt:message key="group.members"/></label></th>
      <td>
      <textarea id="members" name="members" rows="10" cols="30"><%=TextUtil.replaceEntities(s.toString())%></textarea>
      <div class="formhelp"><fmt:message key="editgroup.memberlist"/></div>
      </td>
    </tr>
    </table>
    <div class="formhelp">
      <fmt:message key="editgroup.savehelp"><fmt:param><%=name%></fmt:param></fmt:message>
    </div>
      <input type="submit" name="ok" value="<fmt:message key="editgroup.submit.save"/>" />
      <input type="hidden" name="group" value="<%=name%>" />
      <input type="hidden" name="action" value="save" />
  </form>

  <wiki:Permission permission="deleteGroup"> 
  <form action="<wiki:Link format='url' jsp='DeleteGroup.jsp'/>"
         class="wikiform"
            id="deleteGroup"
        onsubmit="return( confirm('<fmt:message key="grp.deletegroup.confirm"/>') 
        && Wiki.submitOnce(this) );"
        method="POST" accept-charset="UTF-8">
      <input type="submit" name="ok" value="<fmt:message key="actions.deletegroup"/>" />
      <input type="hidden" name="group" value="${param.group}" />
  </form>
  </wiki:Permission>

</wiki:Tab>

</wiki:TabbedSection>