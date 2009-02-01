<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<%@ page import="org.apache.wiki.*" %>
<div id="footer">

  <div class="applicationlogo" >
    <c:set var="frontPageTitle"><fmt:message key='actions.home.title' ><fmt:param><c:out value='${wikiEngine.frontPage}' /></fmt:param></fmt:message></c:set>
    <stripes:link beanclass="org.apache.wiki.action.ViewActionBean" title="${frontPageTitle}"><fmt:message key="actions.home" /></stripes:link>
  </div>

  <div class="companylogo"></div>

  <div class="copyright"><wiki:InsertPage page="CopyrightNotice" /></div>

  <div class="wikiversion">
    <%=Release.APPNAME%> v<%=Release.getVersionString()%>
  </div>

  <div class="rssfeed">
    <wiki:RSSImageLink title="Aggregate the RSS feed" />
  </div>

</div>