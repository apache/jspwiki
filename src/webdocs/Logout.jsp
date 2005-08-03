<%@page import="javax.servlet.http.Cookie" %>
<%@page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule" %>
<%
  AuthenticationManager.logout( session );
  CookieAssertionLoginModule.clearUserCookie( response );
  
  // Redirect to the webroot
  response.sendRedirect(".");
%>
