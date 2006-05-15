<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%-- Provides a simple searchbox that can be easily included anywhere
     on the page --%>

<form action="<wiki:Link jsp="Search.jsp" format="url"/>"
      onsubmit="SearchBox.submit( this.query.value )"
      onmouseover="document.getElementById('searchboxMenu').style.visibility='visible'"
      onmouseout= "document.getElementById('searchboxMenu').style.visibility='hidden'"
      name="searchForm" id="searchForm" 
      accept-charset="<wiki:ContentEncoding />">
  <div>
  <%-- search images --%>
  <input onblur= "if( this.value == '' ) { this.value = 'Search'}; " 
         onfocus="if( this.value == 'Search' ) { this.value = ''}; "
         type="text" value="Search" name="query" size="20" 
         accesskey="f"></input>
  </div>  
  <div id="searchboxMenu" style='visibility:hidden;'>
    <div>
      <a href="javascript://nop/" 
         onclick="SearchBox.navigation( '<wiki:Link format="url" page="__PAGEHERE__"/>','<wiki:Variable var='pagename' />' );return false;"
         title="View the selected page">view</a> 
      | 
      <a href="javascript://nop/" 
         onclick="SearchBox.navigation( '<wiki:Link format="url" context="edit" page="__PAGEHERE__"/>','<wiki:Variable var='pagename' />' );return false;"
         title="Edit the selected page">edit</a> 
      | 
      <a href="javascript://nop/" 
         onclick="SearchBox.navigation( '<wiki:BaseURL />Search.jsp?query=__PAGEHERE__','<wiki:Variable var='pagename' />' );return false;"
         title="Advanced Search">find</a> 
      [ f ]
    </div>
    <div id="recentSearches" > </div>
  </div>

</form>