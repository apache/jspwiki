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
    String pagereq = wiki.safeGetParameter( request, "page" );

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );
    
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
    }

    // In the future, user access permits affect this
    boolean isEditable = (version < 0);

    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: <%=pagereq%><%=versionInfo%></TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <% if( isEditable ) { %>
          <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Edit <%=pageReference%></A>
       <% } %>
       </P>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

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
             <% } %>
          </td>
          <td align="right">
	     <%
             java.util.Date lastchange = wiki.pageLastChanged(pagereq);

             if( lastchange != null )
             {
                 // FIXME: We want to use pageLastChanged(pagereq, version) below...)
                 %>
                 <I>This page last changed on <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=version%>"><%=wiki.pageLastChanged( pagereq )%></A>.  
                    <A HREF="<%=wiki.getBaseURL()%>PageInfo.jsp?page=<%=pageurl%>">More info...</A></I><BR>
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

<%
    NDC.pop();
    NDC.remove();
%>

