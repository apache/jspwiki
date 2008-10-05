<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setLocale value="${prefs.Language}" />
<fmt:setBundle basename="templates.default"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html id="top" xmlns="http://www.w3.org/1999/xhtml">

<head>
  <title>
    <wiki:CheckRequestContext context="edit">
    <fmt:message key="edit.title.edit">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
    </wiki:CheckRequestContext>
    <wiki:CheckRequestContext context="comment">
    <fmt:message key="comment.title.comment">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
    </wiki:CheckRequestContext>
  </title>
  <meta name="robots" content="noindex,follow" />
  <wiki:Include page="commonheader.jsp"/>
</head>

<body <wiki:CheckRequestContext context='edit'>class="edit"</wiki:CheckRequestContext><wiki:CheckRequestContext context='comment'>class="comment"</wiki:CheckRequestContext> >

<div id="wikibody" class="${prefs.Orientation}">

  <wiki:Include page="Header.jsp" />

  <div id="content">

    <div id="page">
      <wiki:Include page="PageActionsTop.jsp"/>
      <wiki:Content/>
      <wiki:Include page="PageActionsBottom.jsp"/>
	</div>

    <wiki:Include page="Favorites.jsp"/> 

	<div class="clearbox"></div>
  </div>	

  <wiki:Include page="Footer.jsp" />

</div>

</body>
</html>