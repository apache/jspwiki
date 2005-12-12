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

<h3>Login</h3>

<div class="formcontainer">
  <div class="instructions">
    Welcome to <wiki:Variable var="applicationname" />. Please sign in
    with your login name and password.
  </div>
  <div class="instructions">
     <span style="color:red"><wiki:Messages prefix="Could not log in: " /></span>
  </div>
  
  <form id="login" action="<%=postURL%>" 
    method="POST" accept-charset="<wiki:ContentEncoding />" >
      
    <!-- User name -->
    <div class="block">
      <label>Login</label>
      <input type="text" name="j_username" value="<wiki:Variable var="uid" default="" />" />
    </div>

    <!-- Password -->
    <div class="block">
      <label>Password</label>
      <input type="password" name="j_password" />
    </div>

    <input type="hidden" name="page" value="<wiki:Variable var="pagename" />" />
    <input type="submit" name="action" value="login" />
    <div class="instructions">
      Don't have a password? 
      <a href="UserPreferences.jsp?tab=profile">Set up a user profile</a>
      with <wiki:Variable var="applicationname" /> now!
    </div>
  </form>
</div>
