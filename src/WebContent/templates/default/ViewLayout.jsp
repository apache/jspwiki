<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<stripes:layout-definition>

<html id="top" xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title>
      <fmt:message key="view.title.view">
        <fmt:param><wiki:Variable var="ApplicationName" /></fmt:param>
        <fmt:param><wiki:PageName/></fmt:param>
      </fmt:message>
    </title>
    <wiki:Include page="commonheader.jsp" />
    <wiki:CheckVersion mode="notlatest">
      <meta name="robots" content="noindex,nofollow" />
    </wiki:CheckVersion>
    <wiki:CheckRequestContext context="diff|info">
      <meta name="robots" content="noindex,nofollow" />
    </wiki:CheckRequestContext>
    <wiki:CheckRequestContext context="!view">
      <meta name="robots" content="noindex,follow" />
    </wiki:CheckRequestContext>
  </head>

  <body class="view">
    <div id="wikibody" class="${prefs.Orientation}">
     
      <wiki:Include page="Header.jsp" />
    
      <div id="content">
        <div id="page">
          <wiki:Include page="PageActionsTop.jsp" />
          <stripes:layout-component name="content" />
          <wiki:Include page="PageActionsBottom.jsp" />
        </div>
        <wiki:Include page="Favorites.jsp" />
      	<div class="clearbox"></div>
      </div>
    
      <wiki:Include page="Footer.jsp" />
    
    </div>
  </body>

</html>

</stripes:layout-definition>
