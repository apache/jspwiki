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
    String pagereq = wiki.safeGetParameter( request, "page" );

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );    

    log.debug("Page info request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: Info on <%=pagereq%></TITLE>
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
                                   <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>&version=<%=p.getVersion()%>"><%=p.getVersion()%></A>
                                   </td>
                                   <td><%=p.getLastModified()%></td>
                                   <td><%=p.getAuthor()%></td>
                                   <td>
                                   <% if( p.getVersion() > 1 ) { %>
                                       <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=p.getVersion()%>&r2=<%=p.getVersion()-1%>">diff to version <%=p.getVersion()-1%></A>
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
             <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>">Back to <%=pagereq%></A>

             <%
         }
         else
         {
         %>
             This page does not exist.  Why don't you go and
             <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">create it</A>?
         <%
         }
      %>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
<%
    NDC.pop();
    NDC.remove();
%>


