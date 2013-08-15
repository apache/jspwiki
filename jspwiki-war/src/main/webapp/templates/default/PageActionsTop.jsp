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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<div id="actionsTop" class="pageactions"> 
  <ul>

    <wiki:CheckRequestContext context='view|info|diff|upload|rename'>
    <wiki:Permission permission="edit">
	<li>
        <wiki:PageType type="page">
          <a href="<wiki:EditLink format='url' />" accesskey="e"  class="action edit"
            title="<fmt:message key='actions.edit.title'/>" ><fmt:message key='actions.edit'/></a>
        </wiki:PageType>
        <wiki:PageType type="attachment">
          <a href="<wiki:BaseURL/>Edit.jsp?page=<wiki:ParentPageName />" accesskey="e" class="action edit"
            title="<fmt:message key='actions.editparent.title'/>" ><fmt:message key='actions.editparent'/></a>
        </wiki:PageType>
    </li>
    </wiki:Permission>
    </wiki:CheckRequestContext>

    <%-- converted to popup menu by jspwiki-common.js--%>
    <li id="morebutton">
      <a href="<wiki:Link format='url' page='MoreMenu' />" class="action more"><fmt:message key="actions.more"/></a>
    </li>

  </ul>
</div>
