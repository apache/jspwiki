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
        class="wikiform"
           id="searchForm" accept-charset="<wiki:ContentEncoding />">

  <div style="position:relative">
  <input onblur="if( this.value == '' ) { this.value = this.defaultValue }; return true; "
        onfocus="if( this.value == this.defaultValue ) { this.value = ''}; return true; "
           type="text" value="<fmt:message key='sbox.search.submit'/>"
           name="query" id="query"
           size="20" 
      accesskey="f" />
  <button type="submit"
  		 name="searchSubmit" id="searchSubmit"
  		value="<fmt:message key='find.submit.go'/>"
  		title="<fmt:message key='find.submit.go'/>"></button>
  </div>
  <div id="searchboxMenu" style='visibility:hidden;'>
    <div id="searchTools">
      <a href="#" id='quickView' class='action'
      onclick="SearchBox.navigate( '<wiki:Link format="url" page="__PAGEHERE__"/>','<fmt:message key="sbox.view.title"/>' );"
        title="<fmt:message key="sbox.view.title"/>"><fmt:message key="sbox.view"/></a>
      <a href="#" id='quickEdit' class='action'
      onclick="SearchBox.navigate( '<wiki:Link format="url" context="edit" page="__PAGEHERE__"/>','<fmt:message key="sbox.edit.title"/>' );"
        title="<fmt:message key="sbox.edit.title"/>"><fmt:message key="sbox.edit"/></a>
      <a href="#" id='quickClone' class='action'	
      onclick="return SearchBox.navigate( '<wiki:Link format="url" page="__PAGEHERE__" context="edit" />', '<fmt:message key="sbox.clone.title"/>', true );"
        title="<fmt:message key="sbox.clone.title"/>"><fmt:message key="sbox.clone"/></a>
      <a href="#" id="advancedSearch" class='action'
      onclick="SearchBox.navigate( '<wiki:BaseURL />Search.jsp?query=__PAGEHERE__','<wiki:Variable var="pagename"/>' )"
        title="<fmt:message key="sbox.find.title"/> [ f ]"><fmt:message key="sbox.find"/></a>
    </div>
    <div id="searchResult" >
	  <fmt:message key='sbox.search.result'/>
      <span id="searchTarget" ><fmt:message key='sbox.search.target'/></span>
      <span id="searchSpin" class="spin" style="position:absolute;display:none;"></span>
	  <div id="searchOutput" ></div>
    </div>
    <div id="recentSearches" style="display:none;">
      <fmt:message key="sbox.recentsearches"/>
      <span><a href="#" id="recentClear"><fmt:message key="sbox.clearrecent"/></a></span>
    </div>
  </div>

</form>