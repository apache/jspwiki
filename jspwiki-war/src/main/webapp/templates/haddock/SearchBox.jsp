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
<%-- Provides a simple searchbox that can be easily included anywhere on the page --%>
<%-- Powered by jswpwiki-common.js//SearchBox --%>

<form action="<wiki:Link jsp='Search.jsp' format='url'/>"
  class="searchbox col6 offset6 pull-right form-inline" 
  id="searchForm" 
  accept-charset="<wiki:ContentEncoding />">

  <wiki:Include page="UserBox.jsp" />

  <span>
  <input type="text" size="20" 
    class="form-control" name="query" id="query" 
    autofocus="autofocus" 
    accesskey="f"
    placeholder="<fmt:message key='sbox.search.submit'/>" />
  <button type="submit" 
    class="btn" name="searchSubmit" id="searchSubmit" 
  	value="<fmt:message key='find.submit.go'/>"
  	title="<fmt:message key='find.submit.go'/>"><span class="icon-search"></span></button>

  <ul class="dropdown-menu pull-right" data-hover-parent="span">
   <li class="dropdown-header">Quick Search results (type ahead)</li>
    <%-- see wiki/Searchbox.js
        <li class="findpages"><a class="createpage" href="/wiki/B" >[Create] B</a></li>
        <li class="findpages"><a class="createpage" href="/wiki/B&clone=Main" >[Create & clone this page] B</a></li>
        <li class="findpages"><a href="/wiki/Brushed" >Brushed (99)</a></li>
        <li class="findpages"><a href="/wiki/BrushedTemplate">BrushedTemplate (49)</a></li>
        <li class="findpages"><a href="/wiki/BrushedSkins">BrushedSkins (29)</a></li>
    --%>
    <li class="divider"></li>
    <li class="dropdown-header"><fmt:message key="sbox.recentsearches"/></li>
    <%-- see wiki/Recents.js
        <li class="recents"><a>Recent-1</a></li>
        <li class="recents"><a>Recent-2</a></li>
        <li class="recents clear"><a>[Clear recent searches]</a></li>
    --%>
  </ul>
  </span>
  
</form>
