<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%-- Provides a simple searchbox that can be easily included anywhere
     on the page --%>

<form action="<wiki:Variable var='baseURL'/>Search.jsp"
      onsubmit="SearchBox.submit( this.query.value )"
      onmouseover="document.getElementById('searchboxMenu').style.visibility='visible'"
      onmouseout= "document.getElementById('searchboxMenu').style.visibility='hidden'"
      name="searchForm" id="searchForm"
      accept-charset="<wiki:ContentEncoding />">

  <input onblur= "if( this.value == '' ) { this.value = 'Search'}; "
         onfocus="if( this.value == 'Search' ) { this.value = ''}; "
         type="text" value="Search" name="query" id="query" size="20" ></input>

  <div id="searchboxMenu" style="position:absolute; visibility:hidden;" >
    <%--
    <div><input type="submit" name="ok" id="ok" value="Find!" ></input></div>
    --%>
    <div><wiki:LinkTo page="FindPage">Advanced Search</wiki:LinkTo></div>

    <div id="recentSearches" ></div>

  </div>

</form>
