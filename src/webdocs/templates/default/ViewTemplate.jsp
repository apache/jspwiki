<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><wiki:ApplicationName />: <wiki:PageName /></TITLE>
  <%@ include file="cssinclude.js" %>
  <wiki:RSSLink />
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <wiki:CheckRequestContext context="view">
          <wiki:Permission permission="edit">
             <wiki:EditLink>Edit this page</wiki:EditLink>
          </wiki:Permission>
       </wiki:CheckRequestContext>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
       <P>
           <DIV ALIGN="center">
           <wiki:RSSImageLink title="Aggregate the RSS feed" /><BR />
           <wiki:RSSUserlandLink title="Aggregate the RSS feed in Radio Userland!" />
           </DIV>
       </P>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <TABLE WIDTH="100%" CELLSPACING="0" CELLPADDING="0" BORDER="0">
         <TR>
            <TD align="left"><H1 CLASS="pagename"><wiki:PageName/></H1></TD>
            <TD align="right"><%@ include file="SearchBox.jsp" %></TD>
         </TR>
      </TABLE>

      <HR><P>

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

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

