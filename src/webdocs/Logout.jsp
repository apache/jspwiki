<%@page import="javax.servlet.http.Cookie" %>
<%@page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%
  AuthenticationManager.logout( session );
  
  // Redirect to the webroot
  response.sendRedirect(".");
%>
