<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<%! 
    public void jspInit()
    {
        WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
        AuthenticationManager mgr = wiki.getAuthenticationManager();
        if ( mgr.isContainerAuthenticated() )
        {   
            posturl = "j_security_check";
        }
        else
        {
            posturl = "Login.jsp";
          }
    }
    String posturl = "";
%>

<h3><fmt:message key="login.heading.login"/></h3>

<div class="formcontainer">
  <div class="instructions">
    <fmt:message key="login.welcome"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
  </div>
  <div class="instructions">
    <wiki:Messages div="error" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
  </div>

  <form id="login" action="<%=posturl%>" 
    method="POST" accept-charset="<wiki:ContentEncoding />" >
      
    <!-- User name -->
    <div class="block">
      <label><fmt:message key="login.login"/></label>
      <input type="text" name="j_username" value="<wiki:Variable var="uid" default="" />" />
    </div>

    <!-- Password -->
    <div class="block">
      <label><fmt:message key="login.password"/></label>
      <input type="password" name="j_password" />
    </div>

    <div class="block">
      <input type="hidden" name="redirect" value="<wiki:Variable var="redirect" default="" />" />
      <input type="submit" name="submitlogin" value="<fmt:message key="login.submit.login"/>" />
    </div>
    
  </form>
  
    <div class="instructions">
      <fmt:message key="login.nopassword"/>
      <a href="UserPreferences.jsp?tab=profile"><fmt:message key="login.registernow">
         <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
         </fmt:message></a>
    </div>
    <div class="instructions">
      <fmt:message key="login.lostpassword"/>
      <a href="LostPassword.jsp"><fmt:message key="login.getnewpassword"/></a>.
    </div>
</div>
