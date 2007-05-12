<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.default"/>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<html>

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
  <meta name="ROBOTS" content="NOINDEX" />
  <wiki:Include page="commonheader.jsp"/>
</head>

<wiki:CheckRequestContext context="edit"><body class="edit" ></wiki:CheckRequestContext>
<wiki:CheckRequestContext context="comment"><body class="comment" ></wiki:CheckRequestContext>

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