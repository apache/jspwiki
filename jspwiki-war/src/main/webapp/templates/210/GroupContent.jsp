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
<%@ page import="java.util.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.PrincipalComparator" %>
<%@ page import="org.apache.wiki.auth.authorize.Group" %>
<%@ page import="org.apache.wiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
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
  String modified = "";
  String created = "";
  String modifier = "";
  String creator = "";

  if ( group != null )
  {
    name = group.getName();
    members = group.members();
    Arrays.sort( members, new PrincipalComparator() );
    creator = group.getCreator();
    if ( group.getCreated() != null )
    {
      created = Preferences.renderDate(WikiContext.findContext( pageContext ), group.getCreated(),Preferences.TimeFormat.DATETIME);
    }
    modifier = group.getModifier();
    if ( group.getLastModified() != null )
    {
      modified = Preferences.renderDate(WikiContext.findContext( pageContext ), group.getLastModified(),Preferences.TimeFormat.DATETIME) ;
    }
  }
  name = TextUtil.replaceEntities(name);
%>

<wiki:TabbedSection defaultTab="${param.tab}">
  <wiki:Tab id="viewgroup" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "group.tab")%>'>
  <h3><%=name%></h3>

<%
  if ( group == null )
  {
    if ( c.getWikiSession().getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
    {
%>
    <fmt:message key="group.doesnotexist"/>
    <wiki:Permission permission="createGroups">
      <fmt:message key="group.createsuggestion">
        <fmt:param><wiki:Link jsp="NewGroup.jsp">
                      <wiki:Param name="group" value="<%=name%>" />
                      <wiki:Param name="group" value="<%=name%>" />
                      <fmt:message key="group.createit"/>
                   </wiki:Link>
        </fmt:param>
      </fmt:message>
    </wiki:Permission>
<%
    }
    else
    {
%>
       <wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix")%>'/>
<%
    }
  }
  else
  {
%>
 <table class="wikitable">
    <tr>
      <th scope="col"><fmt:message key="group.name"/></th>
      <td>
        <fmt:message key="group.groupintro">
          <fmt:param><em><%=name%></em></fmt:param>
        </fmt:message>
      </td>
    </tr>
    <!-- Members -->
    <tr>
      <th scope="col"><fmt:message key="group.members"/>
      </th>
      <td><%
            for ( int i = 0; i < members.length; i++ )
            {
              out.println( members[i].getName().trim() );
              if ( i < ( members.length - 1 ) )
              {
                out.println( "<br/>" );
              }
            }
          %></td>
          <%--fmt:message key="group.membership"/--%>
      </tr>
      <tr>
        <td colspan="2">
        <fmt:message key="group.modifier">
           <fmt:param><%=modifier%></fmt:param>
           <fmt:param><%=modified%></fmt:param>
        </fmt:message>
        </td>
      </tr>
      <tr>
        <td colspan="2">
        <fmt:message key="group.creator">
           <fmt:param><%=creator%></fmt:param>
           <fmt:param><%=created%></fmt:param>
        </fmt:message>
        </td>
      </tr>
    </table>
<%
  }
%>

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

<wiki:Permission permission="editGroup">
  <wiki:Tab id="editgroup" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "actions.editgroup")%>'
           url='<%=c.getURL(WikiContext.NONE, "EditGroup.jsp", "group="+request.getParameter("group") ) %>'
           accesskey="e" >
  </wiki:Tab>
</wiki:Permission>

</wiki:TabbedSection>
