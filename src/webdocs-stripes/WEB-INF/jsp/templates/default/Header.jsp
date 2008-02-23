<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<div id="header">

  <div class="titlebox"><wiki:InsertPage page="TitleBox"/></div>

  <div class="applicationlogo" > 
    <a href="<wiki:LinkTo page='${wikiEngine.frontPage}' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param>${wikiEngine.frontPage}</fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </div>

  <div class="companylogo"></div>

  <wiki:Include page="/WEB-INF/jsp/templates/default/UserBox.jsp" />

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="/WEB-INF/jsp/templates/default/SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail"/><wiki:Breadcrumbs /></div>

</div>