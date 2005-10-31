<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
  <title><wiki:Variable var="ApplicationName" /> Edit: <wiki:PageName /></title>
  <meta name="ROBOTS" content="NOINDEX" />
  <wiki:Include page="commonheader.jsp"/>
  <!-- <script type="text/javascript" src="scripts/fckeditor/fckeditor.js"></script> -->
</head>

<wiki:CheckRequestContext context="edit">
  <body class="edit" bgcolor="#D9E8FF" onload="document.editForm.text.focus()">
</wiki:CheckRequestContext>

<wiki:CheckRequestContext context="comment">
  <body class="comment" bgcolor="#EEEEEE" onload="document.commentForm.text.focus()">
</wiki:CheckRequestContext>


<div id="wikibody" >

  <wiki:Include page="Header.jsp" />

  <div id="actionsTop"><wiki:Include page="PageActions.jsp"/></div>

  <div id="page"><wiki:Content/></div>

  <div id="favorites"><wiki:Include page="Favorites.jsp"/></div>

  <div id="actionsBottom"><wiki:Include page="PageActions.jsp"/></div>

  <wiki:Include page="Footer.jsp" />

  <div style="clear:both; height:0px;" > </div>

</div>
<a name="Bottom"></a>
</body>

</html>
