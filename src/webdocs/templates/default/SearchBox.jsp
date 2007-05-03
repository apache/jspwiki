<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<%-- Provides a simple searchbox that can be easily included anywhere on the page --%>
<%-- Powered by jswpwiki-common.js//SearchBox --%>

<form action="<wiki:Link jsp='Search.jsp' format='url'/>"
        class="wikiform"
        name="searchForm" id="searchForm" 
        accept-charset="<wiki:ContentEncoding />">

  <div>
  <input onblur="if( this.value == '' ) { this.value = this.defaultValue }; " 
        onfocus="if( this.value == this.defaultValue ) { this.value = ''}; "
       onsubmit="SearchBox.submit(this.query.value)"
           type="text" value="<fmt:message key='sbox.search.submit'/>" 
           name="query" id="query" 
           size="20" autocomplete="off"
      accesskey="f"></input>
  <input type="hidden" name="compact" id="compact" value="true"></input>
  </div>  

  <div id="searchboxMenu" style='visibility:hidden;'>
    <div>
      <a href="#" id='quickView'
      onclick="SearchBox.navigate( '<wiki:Link format="url" page="__PAGEHERE__"/>','<fmt:message key="sbox.view.title"/>' );"
        title="<fmt:message key="sbox.view.title"/>"><fmt:message key="sbox.view"/></a> 
      | 
      <a href="#" id='quickEdit'
      onclick="SearchBox.navigate( '<wiki:Link format="url" context="edit" page="__PAGEHERE__"/>','<fmt:message key="sbox.edit.title"/>' );"
        title="<fmt:message key="sbox.edit.title"/>"><fmt:message key="sbox.edit"/></a> 
      | 
      <a href="#" id='quickClone'
      onclick="return SearchBox.navigate( '<wiki:Link format="url" page="__PAGEHERE__" context="edit" />', '<fmt:message key="sbox.clone.title"/>', true );"
        title="<fmt:message key="sbox.clone.title"/>"><fmt:message key="sbox.clone"/></a> 
      |
      <a href="#" id="advancedSearch"
      onclick="SearchBox.navigate( '<wiki:BaseURL />Search.jsp?query=__PAGEHERE__','<wiki:Variable var="pagename"/>' )"
        title="<fmt:message key="sbox.find.title"/>"><fmt:message key="sbox.find"/></a>
      [ f ]
    </div>
    <div id="searchResult" ></div>
    <div id="recentSearches" style="display:none;"> 
      <fmt:message key="sbox.recentsearches"/>
      <ul id="recentItems"></ul>
      <a href="#" id="recentClear"><fmt:message key="sbox.clearrecent"/></a>
    </div>
  </div>

</form>