<%@page import="org.apache.wiki.auth.login.CookieAuthenticationLoginModule"%>
<%@page import="org.apache.wiki.WikiEngine" %>
<%@page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%
  WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
  wiki.getAuthenticationManager().logout( request );

  // Clear the user cookie
  CookieAssertionLoginModule.clearUserCookie( response );

  // Delete the login cookie
  CookieAuthenticationLoginModule.clearLoginCookie( wiki, request, response );

  // Redirect to the webroot
  // TODO: Should redirect to a "goodbye" -page?
  response.sendRedirect(".");
%>
