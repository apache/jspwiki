<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="ApplicationName" /> Edit: <wiki:PageName /></title>
  <meta name="ROBOTS" content="NOINDEX">
  <%@ include file="header.jsp" %>
</head>

<wiki:CheckRequestContext context="edit">
  <body class="edit" bgcolor="#D9E8FF" onLoad="document.forms[1].text.focus()">
</wiki:CheckRequestContext>

<wiki:CheckRequestContext context="comment">
  <body class="comment" bgcolor="#EEEEEE" onLoad="document.forms[1].text.focus()">
</wiki:CheckRequestContext>

<table border="0" cellspacing="8">

  <tr>
    <td class="leftmenu" width="15%" valign="top" nowrap="true">
       <%@ include file="LeftMenu.jsp" %>
       <p>
       <wiki:LinkTo page="TextFormattingRules">Help on editing</wiki:LinkTo>
       </p>
       <%@ include file="LeftMenuFooter.jsp" %>
    </td>

    <td class="page" width="85%" valign="top">

      <wiki:CheckRequestContext context="comment">
         <wiki:Include page="CommentContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="edit">
         <wiki:Include page="EditContent.jsp" />
      </wiki:CheckRequestContext>

    </td>
  </tr>

</table>

</body>

</html>
