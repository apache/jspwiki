<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%! 
    public void jspInit()
    {
        wiki = new WikiEngine( getServletContext() );
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
    
    log.info("Request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );

    String specialpage = wiki.getSpecialPageReference( pagereq );

    if( specialpage != null )
    {
        response.sendRedirect( specialpage );
        return;        
    }


%>

<HTML>

<HEAD>
  <TITLE><%=Release.APPNAME%>: <%=pagereq%></TITLE>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD WIDTH="10%" VALIGN="top">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Edit.jsp?page=<%=pagereq%>">Edit this page</A>
       </P>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             out.println(wiki.getHTML(pagereq));
         }
         else
         {
         %>
             This page does not exist.  Why don't you go and
             <A HREF="Edit.jsp?page=<%=pagereq%>">create it</A>?
         <%
         }
      %>

      <P><HR>
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <A HREF="Edit.jsp?page=<%=pagereq%>">Edit this page</A>.
          </td>
          <td align="right">
             <I>This page last changed on <%=wiki.pageLastChanged( pagereq )%>.</I><BR>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


