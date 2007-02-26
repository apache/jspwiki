<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<div id="header">

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail"/> <wiki:Breadcrumbs /></div>

  <div id="actionsTop"><wiki:Include page="PageActions.jsp"/></div>

  <div style="clear:both; height:0;" > </div>

</div>
