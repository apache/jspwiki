<%@page import="com.ecyrd.jspwiki.WikiEngine" %>
<%!
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    WikiEngine wiki;
%>

<%
  wiki.getAuthenticationManager().logout( request );
  // Redirect to the webroot
  response.sendRedirect(".");
%>
