<%@page import="com.ecyrd.jspwiki.WikiEngine" %>
<%@page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%
  WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
  wiki.getAuthenticationManager().logout( request );
  
  // Clear the user cookie
  CookieAssertionLoginModule.clearUserCookie( response );
  
  // Redirect to the webroot
  response.sendRedirect(".");
%>
