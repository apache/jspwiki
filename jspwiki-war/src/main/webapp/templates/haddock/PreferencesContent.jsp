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
<%@ page import="org.apache.wiki.ui.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext context = WikiContext.findContext( pageContext );
  TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT,
          context.getURL( WikiContext.NONE, "scripts/haddock-prefs.js" ) );
%>
<div class="page-content">
<div class="rightAccordion">

  <h3><fmt:message key="prefs.tab.prefs" /></h3>
  <wiki:Include page="PreferencesTab.jsp" />

  <%-- <wiki:UserCheck status="authenticated"> --%>
  <wiki:Permission permission="editProfile">
  <%-- <wiki:UserProfile property="exists"> --%>
    <h3><fmt:message key="prefs.tab.profile"/></h3>
    <wiki:Include page="ProfileTab.jsp" />
    <%-- <%=LocaleSupport.getLocalizedMessage(pageContext, "prefs.tab.profile")%> --%>
  <%-- </wiki:UserProfile> --%>
  </wiki:Permission>
  <%-- </wiki:UserCheck> --%>

  <wiki:Permission permission="createGroups"> <%-- FIXME check right permissions --%>
    <h3><fmt:message key="group.tab" /></h3>
    <wiki:Include page="GroupTab.jsp" />
  </wiki:Permission>

</div>
</div>