<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: <%=pagereq%><%=versionInfo%></TITLE>
  <%@ include file="../cssinclude.js" %>
  <wiki:RSSLink />
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="../LeftMenu.jsp" %>
       <P>
       <% if( isEditable ) { %>
          <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Edit <%=pageReference%></A>
       <% } %>
       </P>
       <%@ include file="../LeftMenuFooter.jsp" %>
       <P>
           <DIV ALIGN="center">
           <wiki:RSSImageLink title="Aggregate the RSS feed" /><BR />
           <wiki:RSSUserlandLink title="Aggregate the RSS feed in Radio Userland!" />
           </DIV>
       </P>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <%@ include file="../PageHeader.jsp" %>

      <% if( version > 0 ) { %>
         <FONT COLOR="red">
            <P CLASS="versionnote">This is version <%=version%>.  It is not the current version,
            and thus it cannot be edited.  <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>">(Back to current version)</A></P> 
         </FONT>
      <% } %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             // if version == -1, the current page is returned.
             out.println(wiki.getHTML(pagereq, version));
         }
         else
         {
             if(version == -1)
             {
             %>
                This page does not exist.  Why don't you go and
                <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">create it</A>?
             <%
             }
             else
             {
             %>
                This version of the page does not seem to exist.
             <%
             }
         }
      %>

      <P><HR>
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <% if( isEditable ) { %>
                 <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Edit <%=pageReference%></A>.
                 &nbsp;&nbsp;
             <% } %>
             <% if( wikipage != null ) { %>
                 <A HREF="<%=wiki.getBaseURL()%>PageInfo.jsp?page=<%=pageurl%>">More info...</A></I><BR>
             <% } %>
          </td>
        </tr>
        <tr>
          <td align="left">
             <FONT size="-1">
	     <%
             if( wikipage != null )
             {
                 java.util.Date lastchange = wikipage.getLastModified();

                 String author = wikipage.getAuthor();
                 if( author == null ) author = "unknown";

                 if( version == -1 )
                 {
                     %>                
                     <I>This page last changed on <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=version%>"><%=lastchange%></A> by <%=author%>.</I>
                     <%
                 } else {
                     %>
                     <I>This particular version was published on <%=lastchange%> by <%=author%></I>.
                     <%
                 }
             } else {
                 %>
                 <I>Page not created yet.</I>
                 <%
             }
             %>
             </FONT>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

