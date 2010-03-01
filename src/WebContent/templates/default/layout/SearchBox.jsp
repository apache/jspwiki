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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page errorPage="/Error.jsp" %>
<%-- Provides a simple searchbox that can be easily included anywhere on the page --%>
<%-- Powered by jswpwiki-common.js//SearchBox --%>

<s:form beanclass="org.apache.wiki.action.SearchActionBean" class="wikiform" id="searchForm" acceptcharset="UTF-8">

  <c:set var="goTitle" scope="page"><fmt:message key="find.submit.go"/></c:set>
  <s:text onblur="if( this.value == '' ) { this.value = this.defaultValue }; return true; "
         onfocus="if( this.value == this.defaultValue ) { this.value = ''}; return true; "
            name="query" id="query"
            size="20" 
       accesskey="f"><fmt:message key='sbox.search.submit'/></s:text>
  <s:submit name="search" id="searchSubmit" title="${goTitle}" value="${goTitle}" />

  <div id="searchboxMenu" style='visibility:hidden;'>
    <div id="searchTools">
      <a href="#" id='quickView' class='btn'
      onclick="SearchBox.navigate( '<s:url beanclass="org.apache.wiki.action.ViewActionBean"/>','<fmt:message key="sbox.view.title"/>' );"
        title="<fmt:message key="sbox.view.title"/>"><span><span><fmt:message key="sbox.view"/></span></span></a>
      <a href="#" id='quickEdit' class='btn'
      onclick="SearchBox.navigate( '<s:url beanclass="org.apache.wiki.action.EditActionBean"><s:param name="page" value="Main"/></s:url>','<fmt:message key="sbox.edit.title"/>' );"
        title="<fmt:message key="sbox.edit.title"/>"><span><span><fmt:message key="sbox.edit"/></span></span></a>
      <a href="#" id='quickClone' class='btn'	
      onclick="return SearchBox.navigate( '<s:url beanclass="org.apache.wiki.action.EditActionBean"><s:param name="page" value="Main"/></s:url>', '<fmt:message key="sbox.clone.title"/>', true );"
        title="<fmt:message key="sbox.clone.title"/>"><span><span><fmt:message key="sbox.clone"/></span></span></a>
      <a href="#" id="advancedSearch" class='btn'
      onclick="SearchBox.navigate( '<s:url beanclass="org.apache.wiki.action.SearchActionBean"><s:param name="query" value="Main"/></s:url>','<wiki:PageName/>' )"
        title="<fmt:message key="sbox.find.title"/> [ f ]"><span><span><fmt:message key="sbox.find"/></span></span></a>
    </div>
    <div id="searchResult">
	  <fmt:message key='sbox.search.result' />
      <span id="searchTarget"><fmt:message key='sbox.search.target' /></span>
      <span id="searchSpin" class="spin" style="position:absolute;display:none;"></span>
	  <div id="searchOutput"></div>
    </div>
    <div id="recentSearches" style="display:none;">
      <fmt:message key="sbox.recentsearches" />
      <span><a href="#" id="recentClear"><fmt:message key="sbox.clearrecent" /></a></span>
    </div>
  </div>

</s:form>