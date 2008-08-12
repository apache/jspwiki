<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  String frontpage = c.getEngine().getFrontPage(); 
%> 
<div id="footer">

  <div class="applicationlogo" > 
    <a href="<wiki:LinkTo page='<%=frontpage%>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=frontpage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
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