<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><wiki:ApplicationName /> Edit: <wiki:PageName /></TITLE>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY class="edit" BGCOLOR="#D9E8FF" onLoad="document.forms[1].text.focus()">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <wiki:LinkTo page="TextFormattingRules">Help on editing</wiki:LinkTo>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <%@ include file="PageHeader.jsp" %>

      <FORM action="<wiki:EditLink format="url" />" method="POST" 
            ACCEPT-CHARSET="ISO-8859-1,UTF-8">

      <%-- These are required parts of this form.  If you do not include these,
           horrible things will happen. --%>

      <INPUT type="hidden" name="page"     value="<%=pagereq%>">
      <INPUT type="hidden" name="action"   value="save">
      <INPUT type="hidden" name="edittime" value="<%=lastchange%>">

      <%-- End of required area --%>

      <TEXTAREA CLASS="editor" wrap="virtual" name="text" rows="25" cols="80" style="width:100%;"><wiki:InsertPage mode="plain" /></TEXTAREA>

      <P>
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <input type="submit" name="preview" value="Preview" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <wiki:LinkTo>Cancel</wiki:LinkTo>
      </FORM>

      </P>
      <P>
      <wiki:NoSuchPage page="EditPageHelp">
         Ho hum, it seems that the <wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink>
         page is missing.  Someone must've done something to the installation...
      </wiki:NoSuchPage>
      </P>

      <wiki:InsertPage page="EditPageHelp" />

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
