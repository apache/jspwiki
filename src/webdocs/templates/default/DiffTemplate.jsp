<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><wiki:ApplicationName />: Diff <wiki:PageName/></TITLE>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

      <wiki:PageExists>
          Difference between revision <%=versionDescription1%> and <%=versionDescription2%>:           
          <DIV>
          <wiki:InsertDiff>
              <I>No difference detected.</I>
          </wiki:InsertDiff>
          </DIV>

      </wiki:PageExists>

      <wiki:NoSuchPage>
             This page does not exist.  Why don't you go and
             <wiki:EditLink>create it</wiki:EditLink>?
      </wiki:NoSuchPage>

      <P>
      Back to <wiki:LinkTo><wiki:PageName/></wiki:LinkTo>,
       or to the <wiki:PageInfoLink>Page History</wiki:PageInfoLink>.
       </P>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
