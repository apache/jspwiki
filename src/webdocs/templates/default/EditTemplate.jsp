<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<!DOCTYPE html 
     PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
     "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">

<html>

<head>
  <title><wiki:Variable var="ApplicationName" /> Edit: <wiki:PageName /></title>
  <meta name="ROBOTS" content="NOINDEX" />
  <wiki:Include page="commonheader.jsp"/>
</head>

<body
<wiki:CheckRequestContext context="edit">
  class="edit"
</wiki:CheckRequestContext>
<wiki:CheckRequestContext context="comment">
  class="comment"
</wiki:CheckRequestContext>
>

<div id="wikibody" >

  <wiki:Include page="Header.jsp" />

  <div id="page"><wiki:Content/></div>

  <div id="favorites">

  <wiki:Include page="Favorites.jsp"/>
  
  </div>

  <wiki:Include page="Footer.jsp" />

  <div style="clear:both; height:0px;" > </div>

</div>
<a name="Bottom"></a>
</body>

</html>
