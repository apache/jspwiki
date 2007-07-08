<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  String frontPage = c.getEngine().getVariable(c,"jspwiki.frontPage");
%>

<div id="header">

  <div class="applicationlogo" > 
    <a href="<wiki:LinkTo page='<%=frontPage%>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=frontPage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </div>

  <div class="companylogo"></div>

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail"/><wiki:Breadcrumbs /></div>

</div>