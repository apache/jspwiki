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

  int version = -1;
  String rev = request.getParameter("version");
  if( rev != null )
      version = Integer.parseInt( rev );


%>

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: <%=pagereq%> version <%=version%></TITLE>
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
	    // We'll get the current version if the parameter wasn't specified.
             out.println(wiki.getHTML(pagereq, version));
         }
         else
         {
         %>
             This page version does not exist. That's odd.
         <%
         }
      %>

      <P><HR>
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <I>This is  <%=pagereq%> version <%=version%>. It can not be edited.</I>
             <A HREF="Wiki.jsp?page=<%=pagereq%>">Back to current version</A>
          </td>
          <td align="right">
             <I><A HREF="PageInfo.jsp?page=<%=pagereq%>">More info...</A></I><BR>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


