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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%--
   First insert the main page or the corresponding blog-entry page
   Then a horizontal resizer
   And finally the editor for writing the comment
--%>
<div class="page-content">
  <div class="row comment-page">
    <c:set var="mainblogpage" value="${fn:substringBefore(param.page,'_comments_')}" />
    <c:if test="${not empty mainblogpage}">
      <c:set var="blogentrypage" value="${fn:replace(param.page,'_comments_','_blogentry_')}" />
      <wiki:InsertPage page="${blogentrypage}" />
    </c:if>
    <wiki:InsertPage />
  </div>
  <div data-resize=".comment-page" title="<fmt:message key='editor.plain.comment.resize'/>" ></div>
  <wiki:Editor />
</div>