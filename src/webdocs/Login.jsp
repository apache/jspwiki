<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.attachment.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<!--
    This is a sample login page, in case you prefer a clear
    front page instead of the default sign-in type login box
    at the side of the normal entry page. Set this page in
    the welcome-file-list tag in web.xml to default here 
    when entering the site.
-->


<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    WikiContext wikiContext = wiki.createContext( request, WikiContext.LOGIN );
    String pagereq = wikiContext.getPage().getName();

    NDC.push( wiki.getApplicationName() + ":Login.jsp"  );

    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );

    String action = request.getParameter("action");
    String uid    = request.getParameter("uid");
    String passwd = request.getParameter("passwd");

    UserManager mgr = wiki.getUserManager();    

    if( "login".equals(action) )
    {
        if( mgr.login( uid, passwd, session ) )
        {
            response.sendRedirect( wiki.getViewURL(pagereq) );
            return;
        }
    }
    else if( "logout".equals(action) )
    {
        mgr.logout( session );
        response.sendRedirect( wiki.getViewURL(pagereq) );
        return;
    }

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname"/> login</title>
  <%@ include file="templates/default/cssinclude.js" %>
  <META NAME="ROBOTS" CONTENT="NOINDEX">
</head>

<body class="login" bgcolor="#FFFFFF">
  <br />
  <br />
  <form action="<wiki:Variable var="baseURL"/>Login.jsp" accept-charset="UTF-8" method="POST" />
  <input type="hidden" name="page" value="<wiki:Variable var="pagename" />" />
  <div align="center">
    <table border="0" cellspacing="3" cellpadding="5" width="35%" bgcolor="#efefef" />
      <tr>
        <td colspan="2" bgcolor="#bfbfff">
          <div align="center">
            <h3>Welcome to <wiki:Variable var="applicationname"/></h3>

            <% if( action != null ) { %>
            <p>
            Unknown username or password.  Please try again.
            </p>
            <% } %>
          </div>
        </td>
      </tr>
      <tr>
        <td>Login:</td>
        <td><input type="text" name="uid" /></td>
      </tr>
      <tr>
        <td>Password:</td>
        <td><input type="password" name="passwd" /></td>
      </tr>
      <tr>
        <td colspan="2">
          <div align="center">
            <input type="submit" name="action" value="login" />
          </div>
        </td>
      </tr>
    </table>
  </div>
</body>

</html>
<%
    NDC.pop();
    NDC.remove();
%>
