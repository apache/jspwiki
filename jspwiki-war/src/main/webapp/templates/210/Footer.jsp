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
<%@ page import="org.apache.wiki.api.Release" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  Context c = Context.findContext(pageContext);
  String frontpage = c.getEngine().getFrontPage(); 
%> 
<div id="footer">

  <div class="applicationlogo" > 
    <a href="<wiki:LinkTo page='<%=frontpage%>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=frontpage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </div>

  <div class="companylogo"></div>

  <div class="copyright"><wiki:InsertPage page="CopyrightNotice"/></div>

  <div class="wikiversion">
    <%=Release.APPNAME%> v<%=Release.getVersionString()%>
  </div>

  <div class="rssfeed">
    <wiki:RSSImageLink title="Aggregate the RSS feed" />
  </div>

</div>