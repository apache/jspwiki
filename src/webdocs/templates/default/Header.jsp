<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext(pageContext);
  String frontpage = c.getEngine().getFrontPage(); 
%>

<div id="header">

  <div class="titlebox"><wiki:InsertPage page="TitleBox"/></div>

  <div class="applicationlogo" > 
    <a href="<wiki:LinkTo page='<%=frontpage%>' format='url' />"
       title="<fmt:message key='actions.home.title' ><fmt:param><%=frontpage%></fmt:param></fmt:message> "><fmt:message key='actions.home' /></a>
  </div>

  <div class="companylogo"></div>

  <wiki:Include page="UserBox.jsp" />

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail"/><wiki:Breadcrumbs /></div>

</div>