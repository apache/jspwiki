<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="ApplicationName" /> Edit: <wiki:PageName /></title>
  <meta name="ROBOTS" content="NOINDEX">
  <%@ include file="cssinclude.js" %>
</head>

<body class="edit" bgcolor="#D9E8FF" onLoad="document.forms[1].text.focus()">

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

      <table width="100%" cellspacing="0" cellpadding="0" border="0">
         <tr>
            <td align="left">
                <h1 class="pagename">Edit <wiki:PageName/></h1></td>
            <td align="right">
                <%@ include file="SearchBox.jsp" %>
            </td>
         </tr>
      </table>

      <p><hr></p>

      <wiki:CheckVersion mode="notlatest">
         <p class="versionnote">You are about to restore version <wiki:PageVersion/>.
         Click on "Save" to restore.  You may also edit the page before restoring it.
         </p>
      </wiki:CheckVersion>

      <wiki:CheckLock mode="locked" id="lock">
         <p class="locknote">User '<%=lock.getLocker()%>' has started to edit this page, but has not yet
         saved.  I won't stop you from editing this page anyway, BUT be aware that
         the other person might be quite annoyed.  It would be courteous to wait for his lock
         to expire or until he stops editing the page.  The lock expires in 
         <%=lock.getTimeLeft()%> minutes.
         </p>
      </wiki:CheckLock>

      <form action="<wiki:EditLink format="url" />" method="POST" 
            accept-charset="<wiki:ContentEncoding />">

      <p>
      <%-- These are required parts of this form.  If you do not include these,
           horrible things will happen.  Do not modify them either. --%>

      <%-- FIXME: This is not required, is it? --%>
      <input type="hidden" name="page"     value="<wiki:PageName/>" />
      <input type="hidden" name="action"   value="save" />
      <input type="hidden" name="edittime" value="<%=pageContext.getAttribute("lastchange", PageContext.REQUEST_SCOPE )%>" />
      <wiki:CheckRequestContext context="comment">
         <input type="hidden" name="comment" value="true" />
      </wiki:CheckRequestContext>

      <%-- End of required area --%>

      <textarea class="editor" wrap="virtual" name="text" rows="25" cols="80" style="width:100%;"><wiki:CheckRequestContext context="edit"><wiki:InsertPage mode="plain" /></wiki:CheckRequestContext></textarea>
      <wiki:CheckRequestContext context="comment">
         <p>
         <label for="authorname">Your name</label>
         <input type="text" name="author" id="authorname" value="<wiki:UserName/>" />
         </p>
      </wiki:CheckRequestContext>

      <p>      
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <input type="submit" name="preview" value="Preview" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <input type="submit" name="cancel" value="Cancel" />
      </p>
      </form>

      <p>
      <wiki:NoSuchPage page="EditPageHelp">
         Ho hum, it seems that the EditPageHelp<wiki:EditLink page="EditPageHelp">?</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
      </wiki:NoSuchPage>
      </p>

      <wiki:InsertPage page="EditPageHelp" />

    </td>
  </tr>

</table>

</body>

</html>
