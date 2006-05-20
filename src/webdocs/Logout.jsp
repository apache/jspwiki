<%@page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%!
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    WikiEngine wiki;
%>

<%
  wiki.getAuthenticationManager().logout( request );
  
  // Clear the user cookie
  CookieAssertionLoginModule.clearUserCookie( response );
  
  // Redirect to the webroot
  response.sendRedirect(".");
%>
