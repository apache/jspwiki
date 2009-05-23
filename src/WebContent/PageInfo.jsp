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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<s:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="info" executeResolution="true" id="wikiActionBean" />
<s:layout-render name="/templates/default/DefaultLayout.jsp">

  <s:layout-component name="content">
    <wiki:NoSuchPage>
      <fmt:message key="common.nopage">
        <fmt:param><wiki:EditLink><fmt:message key="common.createit" /></wiki:EditLink></fmt:param>
      </fmt:message>
    </wiki:NoSuchPage>
    <wiki:PageExists>
      <wiki:PageType type="page">
        <jsp:include page="/templates/default/PageInfoTab.jsp" />
      </wiki:PageType>
      <wiki:PageType type="attachment">
        <jsp:include page="/templates/default/AttachmentInfoTab.jsp" />
      </wiki:PageType>
    </wiki:PageExists>
  </s:layout-component>
  
</s:layout-render>
