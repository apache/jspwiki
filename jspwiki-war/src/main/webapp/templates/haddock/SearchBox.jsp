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
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%-- Provides a simple searchbox --%>
<%-- Powered by wiki/Findpages.js and wiki/Recents.js  --%>
<form action="<wiki:Link jsp='Search.jsp' format='url'/>"
  class="form-inline searchbox pull-right"
  id="searchForm"
  accept-charset="<wiki:ContentEncoding />">

  <div class="btn"><span class="icon-search"></span><span class="caret"></span></div>

  <ul class="dropdown-menu" data-hover-parent=".searchbox">
    <li class="dropdown-header">
  <input type="text" size="20"
        class="form-control" name="query" id="query"
    placeholder="<fmt:message key='sbox.search.submit'/>" />
    </li>
    <li class="dropdown-header">
    <button type="submit"
           class="btn btn-primary btn-block" name="searchSubmit" id="searchSubmit"
  	       value="<fmt:message key='find.submit.go'/>"> <fmt:message key='sbox.search.fullsearch'/>
    </button>
    </li>
    <%-- see wiki/Findpages.js
        <li class="findpages"> ... </li>
    --%>
    <li class="divider"></li>
    <li class="dropdown-header"><fmt:message key="sbox.recentsearches"/></li>
    <%-- see wiki/Recents.js
        <li class="recents"><a>Recent-1</a></li>
        <li class="recents"><a>Recent-2</a></li>
        <li class="recents clear"><a>[Clear recent searches]</a></li>
    --%>
  </ul>
</form>
