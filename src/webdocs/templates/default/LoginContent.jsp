<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
        AuthenticationManager mgr = wiki.getAuthenticationManager();
        if ( mgr.isContainerAuthenticated() )
        {   
            postURL = "j_security_check";
        }
        else
        {
            postURL = "Login.jsp";
        }
    }
    String postURL;
%>


<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<html>

<head>
  <title><wiki:Variable var="applicationname"/> Login</title>
  <wiki:Include page="commonheader.jsp"/>
  <meta name="robots" content="noindex,nofollow">
</head>

<body class="login" bgcolor="#FFFFFF">
  <p>Welcome to <wiki:Variable var="applicationname" />. Please sign in
    with your login name and password.</p>
  <br />
  <br />
  <form action="<%=postURL%>"accept-charset="<wiki:ContentEncoding />" method="post" >
  <input type="hidden" name="page" value="<wiki:Variable var="pagename" />" />
  <div align="center">
    <table border="0" cellspacing="3" cellpadding="5" width="35%" bgcolor="#efefef" />
      <tr>
        <td colspan="2" bgcolor="#bfbfff">
          <div align="center">
            <h3>Welcome to <wiki:Variable var="applicationname"/></h3>
            <p style="color:red"><wiki:Variable var="msg" /></p>
          </div>
        </td>
      </tr>
      <tr>
        <td>Login:</td>
        <td><input type="text" name="j_username" value="<wiki:Variable var="uid" default="" />" /></td>
      </tr>
      <tr>
        <td>Password:</td>
        <td><input type="password" name="j_password" /></td>
      </tr>
      <tr>
        <td colspan="2">
          <div align="center">
            <input type="submit" name="action" value="login" />
          </div>
        </td>
      </tr>
    </table>
    <p>Don't have a password? 
      <a href="UserPreferences.jsp"Join <wiki:Variable var="applicationname" />
      now!</a>
    </p>
  </div>
  </form>
</body>

</html>
