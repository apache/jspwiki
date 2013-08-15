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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html id="top" xmlns="http://www.w3.org/1999/xhtml" xmlns:jspwiki="http://jspwiki.apache.org">

<head>
  <title><fmt:message key="upload.title"><fmt:param><wiki:Variable var="applicationname"/></fmt:param></fmt:message></title>
  <wiki:Include page="commonheader.jsp"/>
  <meta name="robots" content="noindex,nofollow" />
</head>

<body>

<div id="wikibody" class="${prefs.Orientation}">

  <wiki:Include page="Header.jsp" />

  <div id="content">

    <div id="page">
      <wiki:Include page="PageActionsTop.jsp"/>

      <wiki:TabbedSection defaultTab="attachments" >
        <wiki:Tab id="pagecontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "view.tab")%>'
	  		     url="<%=c.getURL(WikiContext.VIEW, c.getPage().getName())%>"
	       accesskey="v" >
        </wiki:Tab>
        
        <wiki:PageExists>
        <wiki:Tab id="attachments" title="<%= attTitle %>" >
          <wiki:Include page="AttachmentTab.jsp"/>
        </wiki:Tab>
        <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
                 url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
        </wiki:Tab>

        </wiki:PageExists>
      </wiki:TabbedSection>

      <wiki:Include page="PageActionsBottom.jsp"/>

    </div>

    <wiki:Include page="Favorites.jsp"/>

	<div class="clearbox"></div>
  </div>

  <wiki:Include page="Footer.jsp" />

</div>
</body>

</html>