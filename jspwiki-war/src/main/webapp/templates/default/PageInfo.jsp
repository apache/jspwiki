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
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>

<wiki:CheckRequestContext context='view|diff|edit|upload|info'>
<div class="page-footer visible-print">
  <wiki:PageExists>
    <wiki:CheckVersion mode="latest">
      <fmt:message key="info.lastmodified">
        <fmt:param><wiki:PageVersion /></fmt:param>
        <fmt:param><wiki:DiffLink version="latest" newVersion="previous"><wiki:PageDate format='${prefs["DateFormat"]}'/></wiki:DiffLink></fmt:param>
        <fmt:param><wiki:Author /></fmt:param>
      </fmt:message>
    </wiki:CheckVersion>
    <wiki:CheckVersion mode="notlatest">
      <fmt:message key="actions.publishedon">
        <fmt:param><wiki:PageDate format='${prefs["DateFormat"]}'/></fmt:param>
        <fmt:param><wiki:Author /></fmt:param>
      </fmt:message>
    </wiki:CheckVersion>
    <wiki:RSSImageLink mode="wiki" title="<fmt:message key='info.feed'/>" />
    <a href="#top" title="<fmt:message key='actions.gototop'/>" class="btn">Top<span class="caret-up"></span></a>
  </wiki:PageExists>

  <wiki:NoSuchPage><fmt:message key="actions.notcreated"/></wiki:NoSuchPage>
</div>
</wiki:CheckRequestContext>