<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><wiki:ApplicationName />: <wiki:PageName /></TITLE>
  <%@ include file="../cssinclude.js" %>
  <wiki:RSSLink />
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <wiki:Permission permission="edit">
          <wiki:EditLink>Edit this page</wiki:EditLink>
       </wiki:Permission>
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

      <%@ include file="PageHeader.jsp" %>

      <% if( version > 0 ) { %>
         <FONT COLOR="red">
            <P CLASS="versionnote">This is version <%=version%>.  It is not the current version,
            and thus it cannot be edited.  <wiki:LinkTo>(Back to current version)</wiki:LinkTo></P> 
         </FONT>
      <% } %>

      <wiki:InsertPage />

      <wiki:NoSuchPage>
           <!-- FIXME: Should also note when a wrong version has been fetched. -->
           This page does not exist.  Why don't you go and
           <wiki:EditLink>create it</wiki:EditLink>?
      </wiki:NoSuchPage>

      <P><HR>
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <wiki:Permission permission="edit">
                 <wiki:EditLink>Edit this page</wiki:EditLink>&nbsp;&nbsp;
             </wiki:Permission>
             <wiki:PageInfoLink>More info...</wiki:PageInfoLink><BR>
          </td>
        </tr>
        <tr>
          <td align="left">
             <FONT size="-1">
	     <%
             if( wikipage != null )
             {
                 java.util.Date lastchange = wikipage.getLastModified();

                 if( version == -1 )
                 {
                     %>                
                     <I>This page last changed on <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=version%>"><%=lastchange%></A> by <wiki:Author />.</I>
                     <%
                 } else {
                     %>
                     <I>This particular version was published on <%=lastchange%> by <wiki:Author /></I>.
                     <%
                 }
             }
             %>
             <wiki:NoSuchPage>
                 <I>Page not created yet.</I>
             </wiki:NoSuchPage>

             </FONT>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

