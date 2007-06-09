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
  <input onblur="if( this.value == '' ) { this.value = this.defaultValue }; return true; "
        onfocus="if( this.value == this.defaultValue ) { this.value = ''}; return true; "
           type="text" value="<fmt:message key='sbox.search.submit'/>"
           name="query" id="query"
           size="20" autocomplete="off"
      accesskey="f"></input>
  <button type="submit"
  		 name="searchSubmit" id="searchSubmit"
  		value="<fmt:message key='find.submit.go'/>"
  		title="<fmt:message key='find.submit.go'/>"></button>
  </div>

  <div id="searchboxMenu" style='visibility:hidden;'>
    <div id="searchTools">
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
    <div id="searchResult" >
	  <fmt:message key='sbox.search.result'/>
      <span id="searchTarget" ><fmt:message key='sbox.search.target'/></span>
      <span id="searchSpin" class="spin" style="position:absolute;display:none;"></span>
	  <div id="searchOutput" ></div>
    </div>
    <div id="recentSearches" style="display:none;">
      <fmt:message key="sbox.recentsearches"/>
      <span><a href="#" id="recentClear"><fmt:message key="sbox.clearrecent"/></a></span>
      <ul id="recentItems"></ul>
    </div>
  </div>

</form>