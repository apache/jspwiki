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
    <wiki:Messages div="error" prefix="Error: " />
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

    <div class="block">
      <input type="hidden" name="redirect" value="<wiki:Variable var="redirect" />" />
      <input type="submit" name="action" value="login" />
    </div>
    
  </form>
  
    <div class="instructions">
      Don't have a password? 
      <a href="UserPreferences.jsp?tab=profile">Join
      <wiki:Variable var="applicationname" /></a> now!
    </div>
    <div class="instructions">
      Lost your password? 
      <a href="LostPassword.jsp">Get a new one</a>.
    </div>
</div>
