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
    
    String userName = wiki.getUserName( request );
    if( userName == null ) 
    {
        userName="";
    }

    String ok = request.getParameter("ok");
    String clear = request.getParameter("clear");

    if( ok != null || "save".equals(request.getParameter("action")) )
    {
        //
        //  Servlet 2.2 API assumes all incoming data is in ISO-8859-1, so when
        //  returning UTF-8, you get the right bytes but with the wrong encoding.
        //
        //  For more information, see:
        //    http://www.jguru.com/faq/view.jsp?EID=137049
        //
        String name;
        name = new String( request.getParameter("username").getBytes("ISO-8859-1"), "UTF-8");

        UserProfile profile = new UserProfile();
        profile.setName( name );

        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, 
                                   profile.getStringRepresentation() );
        prefs.setMaxAge( 90*24*60*60 ); // 90 days is default.

        response.addCookie( prefs );

        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }
    else if( clear != null )
    {
        Cookie prefs = new Cookie( WikiEngine.PREFS_COOKIE_NAME, "" );
        prefs.setMaxAge( 0 );
        response.addCookie( prefs );

        // FIXME: Should really redirect to some other place, like this page.
        response.sendRedirect( wiki.getBaseURL()+"Wiki.jsp" );
    }       

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

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
            ACCEPT-CHARSET="UTF-8">

         <B>User name:</B> <INPUT type="text" name="username" size="30" value="<%=userName%>">
         <I>This must be a proper WikiName, no punctuation.</I>
         <BR><BR>
         <INPUT type="submit" name="ok" value="Set my preferences!">
         <INPUT type="hidden" name="action" value="save">
      </FORM>

      <HR/>

      <H3>Removing your preferences</h3>

      <P>In some cases, you may need to remove the above preferences from the computer.
      Click the button below to do that.  Note that it will remove all preferences
      you've set up, permanently.  You will need to enter them again.</P>

      <DIV align="center">
      <FORM action="<%=wiki.getBaseURL()%>UserPreferences.jsp"
            method="POST"
            ACCEPT-CHARSET="UTF-8">
      <INPUT type="submit" name="clear" value="Remove preferences from this computer" />
      </FORM>
      </DIV>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

<%
    NDC.pop();
    NDC.remove();
%>

