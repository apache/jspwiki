<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname" />: <wiki:PageName /></title>
  <%@ include file="cssinclude.js" %>
  <script src="templates/<wiki:TemplateDir />/search_highlight.js" type="text/javascript"></script>
  <wiki:RSSLink />
</head>

<body bgcolor="#FFFFFF">

<table border="0" cellspacing="8" width="95%">

  <tr>
    <td class="leftmenu" width="10%" valign="top" nowrap="true">
       <%@ include file="LeftMenu.jsp" %>
       <p>
       <wiki:CheckRequestContext context="view">
          <wiki:Permission permission="edit">
             <wiki:EditLink>Edit this page</wiki:EditLink>
          </wiki:Permission>
       </wiki:CheckRequestContext>
       </p>
       <%@ include file="LeftMenuFooter.jsp" %>
       <p>
           <div align="center">
           <wiki:RSSImageLink title="Aggregate the RSS feed" /><br />
           <wiki:RSSUserlandLink title="Aggregate the RSS feed in Radio Userland!" />
           </div>
       </p>
    </td>

    <td class="page" width="85%" valign="top">

      <table width="100%" cellspacing="0" cellpadding="0" border="0">
         <tr>
            <td align="left">
                <h1 class="pagename"><a name="Top"><wiki:PageName/></a></h1>
            </td>
            <td align="right"><%@ include file="SearchBox.jsp" %></td>
         </tr>
         <tr>
            <td colspan="2" class="breadcrumbs">Your trail: <wiki:Breadcrumbs /></td>
         </tr>
      </table>

      <hr />

      <wiki:CheckRequestContext context="view">
         <wiki:Include page="PageContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="diff">
         <wiki:Include page="DiffContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="info">
         <wiki:Include page="InfoContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="preview">
         <wiki:Include page="PreviewContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="conflict">
         <wiki:Include page="ConflictContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="find">
         <wiki:Include page="FindContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="prefs">
         <wiki:Include page="PreferencesContent.jsp" />
      </wiki:CheckRequestContext>

      <wiki:CheckRequestContext context="error">
         <wiki:Include page="DisplayMessage.jsp" />
      </wiki:CheckRequestContext>

    </td>
  </tr>

</table>

</body>

</html>

