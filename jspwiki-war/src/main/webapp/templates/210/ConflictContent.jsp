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
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<wiki:TabbedSection >

<wiki:Tab id="conflict" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "conflict.oops.title")%>'>
  <div class="error"><fmt:message key="conflict.oops" /></div>
  <fmt:message key="conflict.goedit" >
    <fmt:param><wiki:EditLink><wiki:PageName /></wiki:EditLink></fmt:param>
  </fmt:message>
</wiki:Tab>

<wiki:Tab id="conflictOther" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "conflict.modified")%>' >
  <pre><%=pageContext.getAttribute("conflicttext",PageContext.REQUEST_SCOPE)%></pre>
</wiki:Tab>

<wiki:Tab id="conflictOwn" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "conflict.yourtext")%>' >
  <pre><%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%></pre>
</wiki:Tab>

</wiki:TabbedSection>