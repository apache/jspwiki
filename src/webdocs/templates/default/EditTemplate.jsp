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

<table border="0" cellspacing="8">

  <tr>
    <td class="leftmenu" width="15%" valign="top" nowrap="nowrap">
       <%@ include file="LeftMenu.jsp" %>
       <p>
       <wiki:LinkTo page="TextFormattingRules">Help on editing</wiki:LinkTo>
       </p>
       <%@ include file="LeftMenuFooter.jsp" %>
    </td>

    <td class="page" width="85%" valign="top">

       <wiki:Content/>

    </td>
  </tr>

</table>

</body>

</html>
