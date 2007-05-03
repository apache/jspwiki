<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  String homepage = "Main";
  WikiContext wikiContext = WikiContext.findContext(pageContext);
  try 
  { 
    homepage = wikiContext.getEngine().getFrontPage(); 
  } 
  catch( Exception  e )  { /* dont care */ } ;
%>

<div id="header">

  <div class="applicationlogo" > 
    <%--FIXME<a href="<wiki:LinkTo page='SystemInfo' format='url'/>" title="JSPWiki System Info"><wiki:Variable var="ApplicationName" /></a>--%>
    <a href="<wiki:LinkTo page='<%= homepage %>' format='url'/>" 
      title="<fmt:message key='header.homepage.title' />"><wiki:Variable var="ApplicationName" /></a>
    <%-- fmt: doeesnt work ???
    <wiki:LinkTo page="<%=homepage %>" title="<fmt:message key='header.homepage.title' />" ><wiki:Variable var="ApplicationName" /></wiki:LinkTo>
    --%>
  </div>

  <div class="companylogo"></div>

  <div class="pagename"><wiki:PageName /></div>

  <div class="searchbox"><wiki:Include page="SearchBox.jsp" /></div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail"/><wiki:Breadcrumbs /></div>

</div>