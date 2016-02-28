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

<%@ page import="java.security.Principal" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.apache.wiki.auth.PrincipalComparator" %>
<%@ page import="org.apache.wiki.auth.authorize.Group" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
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

  StringBuffer membersAsString = new StringBuffer();
  for ( int i = 0; i < members.length; i++ )
  {
    membersAsString.append( members[i].getName().trim() ).append( '\n' );
  }

%>
<c:set var="name" value="<%= name%>" />
<c:set var="members" value="<%= membersAsString%>" />

<div class="page-content">

  <form action="<wiki:Link format='url' jsp='EditGroup.jsp' />"
            id="editGroup"
        method="POST" accept-charset="UTF-8">

    <input type="hidden" name="group" value="${name}" />

    <div class="form-group">
      <button class="btn btn-success" type="submit" name="action" value="save">
        <fmt:message key="editgroup.submit.save"/>
      </button>
      <a class="btn btn-danger pull-right" href="<wiki:Link format='url' jsp='Group.jsp'><wiki:Param name='group' value='${name}'/></wiki:Link>" >
        <fmt:message key='editgroup.cancel.submit'/>
      </a>
    </div>

    <%--<wiki:Messages div="error help-block" topic="group" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"editgroup.saveerror") %>' />--%>
    <wiki:Messages div="error help-block" topic="group" prefix="<fmt:message key='editgroup.saveerror'/>" />

    <div class="help-block">
      <fmt:message key="editgroup.instructions"><fmt:param>${name}</fmt:param></fmt:message>
    </div>

    <div class="form-group">
      <label for="members"><fmt:message key="group.members"/></label><br />
      <textarea class="form-control form-col-50" rows=8 autofocus="autofocus" name="members" id="members" >${members}</textarea>
    </div>
    <div class="help-block"><fmt:message key="editgroup.memberlist"/></div>

  </form>

</div>