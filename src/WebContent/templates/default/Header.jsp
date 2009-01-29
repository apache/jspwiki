<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<div id="header">

  <div class="titlebox"><wiki:InsertPage page="TitleBox" /></div>

  <div class="applicationlogo" >
    <c:set var="frontPageTitle"><fmt:message key='actions.home.title' ><fmt:param><c:out value='${wikiEngine.frontPage}' /></fmt:param></fmt:message></c:set>
    <stripes:link beanclass="com.ecyrd.jspwiki.action.ViewActionBean" title="${frontPageTitle}"><fmt:message key="actions.home" /></stripes:link>
  </div>

  <div class="companylogo"></div>

  <wiki:Include page="UserBox.jsp" />

  <div class="pagename"><wiki:PageName/></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail" /><wiki:Breadcrumbs/></div>

</div>