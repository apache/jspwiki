<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

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
    
    log.info("Request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );

    String specialpage = wiki.getSpecialPageReference( pagereq );

    if( specialpage != null )
    {
        response.sendRedirect( specialpage );
        return;        
    }

    //
    //  Determine requested version.  If version == -1,
    //  then fetch current version.
    //
    int version          = -1;
    String rev           = request.getParameter("version");
    String pageReference = "this page";
    String versionInfo   = "";

    if( rev != null )
    {
        version = Integer.parseInt( rev );
        pageReference = "current version";
        versionInfo = " (version " + rev + ")";
    }

%>

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: <%=pagereq%><%=versionInfo%></TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Edit.jsp?page=<%=pagereq%>">Edit <%=pageReference%></A>
       </P>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

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
                <A HREF="Edit.jsp?page=<%=pagereq%>">create it</A>?
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
             <A HREF="Edit.jsp?page=<%=pagereq%>">Edit <%=pageReference%></A>.
          </td>
          <td align="right">
	     <%
             java.util.Date lastchange = wiki.pageLastChanged(pagereq);

             if( lastchange != null )
             {
                 // FIXME: We want to use pageLastChanged(pagereq, version) below...)
                 %>
                 <I>This page last changed on <%=wiki.pageLastChanged( pagereq )%>.  
                    <A HREF="PageInfo.jsp?page=<%=pagereq%>">More info...</A></I><BR>
                 <%
             } else {
                 %>
                 <I>Page not created yet.</I>
                 <%
             }
             %>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


