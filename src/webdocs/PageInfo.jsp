<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>


<%
    String pagereq = request.getParameter("page");

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    String pageurl = wiki.encodeName( pagereq );    

    log.debug("Page info request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );
%>

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: Info on <%=pagereq%></TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             %>
             <table cellspacing="4">
                <tr>
                   <td><B>Page name</B></td>
                   <td><%=pagereq%></td>
                </tr>

                <tr>
                   <td><B>Page last modified</B></td>
                   <td><%=wiki.pageLastChanged( pagereq ) %></td>
                </tr>

                <tr>
                   <td><B>Current page version</B></td>
                   <td>
                   <%
                      int version = wiki.getVersion( pagereq );
                      if( version == -1 )
                          out.println("No versioning support.");
                      else
                          out.println( version );
                    %>
                    </td>
                </tr>

                <tr>
                   <td valign="top"><b>Page revision history</b></td>
                   <td>
                       <table border="1" cellpadding="4">
                           <tr>
                               <th>Version</th>
                               <th>Date</th>
                               <th>Author</th>
                               <th>Diff</th>
                           </tr>

                           <%
                           Collection versions = wiki.getVersionHistory( pagereq );

                           for( Iterator i = versions.iterator(); i.hasNext(); )
                           {
                               WikiPage p = (WikiPage) i.next();

                               %>
                               <tr>
                                   <td>
                                   <A HREF="Wiki.jsp?page=<%=pageurl%>&version=<%=p.getVersion()%>"><%=p.getVersion()%></A>
                                   </td>
                                   <td><%=p.getLastModified()%></td>
                                   <td><%=p.getAuthor()%></td>
                                   <td>
                                   <% if( p.getVersion() > 1 ) { %>
                                       <A HREF="Diff.jsp?page=<%=pageurl%>&r1=<%=p.getVersion()%>&r2=<%=p.getVersion()-1%>">diff to version <%=p.getVersion()-1%></A>
                                   <% } %>
                                   </td>
                               </tr>
                               <%
                           }
                           %>
                       </table>
                   </td>
                </tr>
             </table>
             
             <BR>
             <A HREF="Wiki.jsp?page=<%=pageurl%>">Back to <%=pagereq%></A>

             <%
         }
         else
         {
         %>
             This page does not exist.  Why don't you go and
             <A HREF="Edit.jsp?page=<%=pageurl%>">create it</A>?
         <%
         }
      %>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


