<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="/WEB-INF/tlds/taglib.tld" prefix="jspwiki" %>

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

    String srev1 = request.getParameter("r1");
    String srev2 = request.getParameter("r2");

    if( srev1 == null || srev2 == null )
    {
        throw new ServletException("Empty parameters given to Diff.jsp");
    }    

    int ver1 = Integer.parseInt( srev1 );
    int ver2 = Integer.parseInt( srev2 );

    log.debug("Request for page diff for '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser()+".  R1="+ver1+", R2="+ver2 );

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">


<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: Diff <%=pagereq%></TITLE>
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

      <%
         if( wiki.pageExists( pagereq ) )
         {
             %>
             Difference between revision <%=ver1%> and <%=ver2%>
             <P>
             <PRE>
<%=wiki.getDiff( pagereq, ver2, ver1 )%>
             </PRE>
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
