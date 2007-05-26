<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html id="top">

<head>
  <title>
    <fmt:message key="view.title.view">
      <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
      <fmt:param><wiki:PageName /></fmt:param>
    </fmt:message>
  </title>
  <wiki:Include page="commonheader.jsp"/>
  <wiki:CheckVersion mode="notlatest">
    <meta name="robots" content="noindex,nofollow" />
  </wiki:CheckVersion>
</head>

<body class="view">

<div id="wikibody" >

  <wiki:Include page="Header.jsp" />

  <wiki:Include page="PageActionsTop.jsp"/>

  <div id="page"><wiki:Content/></div>

  <wiki:Include page="Favorites.jsp"/>

  <wiki:Include page="PageActionsBottom.jsp"/>

  <wiki:Include page="Footer.jsp" />

</div>

</body>
</html>