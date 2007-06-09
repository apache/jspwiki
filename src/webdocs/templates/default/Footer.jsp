<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.Release" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<div id="footer">

  <div class="applicationlogo" > 
    <%--FIXMIE<a href="<wiki:LinkTo page='SystemInfo' format='url'/>" title="JSPWiki System Info"><wiki:Variable var="ApplicationName" /></a>--%>
    <wiki:Link page='SystemInfo' title='<%=LocaleSupport.getLocalizedMessage(pageContext, "header.systeminfo")%>'><wiki:Variable var="ApplicationName" /></wiki:Link>
  </div>

  <div class="companylogo"></div>

  <div class="copyright"><wiki:InsertPage page="CopyrightNotice"/></div>

  <div class="wikiversion">
    <%=Release.APPNAME%> v<%=Release.getVersionString()%>
  </div>

  <div class="rssfeed">
    <wiki:RSSImageLink title="Aggregate the RSS feed" />
  </div>

</div>