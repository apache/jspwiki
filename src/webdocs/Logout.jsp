<%@page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%
  AuthenticationManager.logout( request );
  // Redirect to the webroot
  response.sendRedirect(".");
%>
