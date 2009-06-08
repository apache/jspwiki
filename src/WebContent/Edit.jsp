<%-- 
    JSPWiki - a JSP-based WikiWiki clone.

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
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<s:useActionBean beanclass="org.apache.wiki.action.EditActionBean" event="edit" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="${templates['DefaultLayout.jsp']}">

  <%-- Page title should say Edit: + pagename --%>
  <s:layout-component name="headTitle">
    <fmt:message key="edit.title.edit">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName/></fmt:param>
    </fmt:message>
  </s:layout-component>

  <!-- Add Javascript for editors -->
  <s:layout-component name="script">
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/jspwiki-edit.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/posteditor.js' />"></script>
  </s:layout-component>

  <s:layout-component name="content">
    <jsp:include page="${templates['EditContent.jsp']}" />
  </s:layout-component>
  
</s:layout-render>
