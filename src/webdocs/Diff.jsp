<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    String getVersionText( int ver )
    {
        return ver > 0 ? ("version "+ver) : "current version";
    }

    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>


<%
    String pagereq = wiki.safeGetParameter( request, "page" );
    String headerTitle = "";

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );    

    // If "r1" is null, then assume current version (= -1)
    // If "r2" is null, then assume the previous version (=current version-1)

    String srev1 = request.getParameter("r1");
    String srev2 = request.getParameter("r2");

    int ver1 = -1, ver2 = -1;

    if( srev1 != null )
    {
        ver1 = Integer.parseInt( srev1 );
    }

    if( srev2 != null )
    {
        ver2 = Integer.parseInt( srev2 );
    }
    else
    {
        int lastver = wiki.getVersion( pagereq );

        if( lastver > 1 )
        {
            ver2 = lastver-1;
        }
    }

    String versionDescription1 = getVersionText( ver1 );
    String versionDescription2 = getVersionText( ver2 );

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
             String diff = wiki.getDiff( pagereq, ver2, ver1 );

             if( diff.length() == 0 ) diff = "<I>No difference.</I>";
             %>
             Difference between revision <%=versionDescription1%> and <%=versionDescription2%>
             <P>
<%=diff%>
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

      <P>
      Back to <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>"><%=pagereq%></A>,
       or to the <A HREF="<%=wiki.getBaseURL()%>PageInfo.jsp?page=<%=pageurl%>">Page History</A>.
       </P>

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
