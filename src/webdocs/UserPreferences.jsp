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
    String pagereq = "UserPreferences";
    String headerTitle = "";

    NDC.push( wiki.getApplicationName()+":"+pagereq );
    
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

    String userName = wiki.getUserName( request );
    if( userName == null ) 
    {
        userName="";
    }

    String ok = request.getParameter("ok");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        UserProfile profile = new UserProfile();
        profile.setName( request.getParameter("username") );

        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, 
                                   profile.getStringRepresentation() );
        prefs.setMaxAge( 90*24*60*60 ); // 90 days is default.

        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: User Preferences</TITLE>
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

      <P>
      This is a page which allows you to set up all sorts of interesting things.
      You need to have cookies enabled for this to work, though.
      </P>

      <FORM action="<%=wiki.getBaseURL()%>UserPreferences.jsp" 
            method="POST"
            ACCEPT-CHARSET="ISO-8859-1,UTF-8">

         <B>User name:</B> <INPUT type="text" name="username" size="30" value="<%=userName%>">
         <I>This must be a proper WikiName, no punctuation.</I>
         <BR><BR>
         <INPUT type="submit" name="ok" value="Set my preferences!">
         <INPUT type="hidden" name="action" value="save">
      </FORM>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

<%
    NDC.pop();
    NDC.remove();
%>

