<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.ui.Installer" %>
<%@ page import="com.ecyrd.jspwiki.log.Logger" %>
<%@ page import="com.ecyrd.jspwiki.log.LoggerFactory" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.text.MessageFormat" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="CoreResources"/>

<%!
    Logger log = LoggerFactory.getLogger("JSPWiki"); 
%>

<%
WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
// Create wiki context and check for authorization
WikiContext wikiContext = wiki.createContext( request, WikiContext.INSTALL );
if(!wikiContext.hasAccess( response )) return;

Installer installer = new Installer( request, config );
WikiSession wikiSession = wikiContext.getWikiSession();

// Parse the existing properties
installer.parseProperties();
boolean validated = false;
String password = null;
ResourceBundle rb = wikiContext.getBundle("CoreResources");

// If user hit "submit" button, validate and install them
if( request.getParameter("submit") != null )
{
    validated = installer.validateProperties();
    if ( validated )
    {
        installer.saveProperties();
        password = installer.createAdministrator();
        if ( password != null )
        {
        	Object[] args = { Installer.ADMIN_ID, password, Installer.ADMIN_GROUP };
        	wikiSession.addMessage( Installer.INSTALL_INFO, 
        			MessageFormat.format( rb.getString( "install.jsp.install.msg.rnd.pwd" ), args ) );
       }
    }
}

if ( !installer.adminExists() )
{
	wikiSession.addMessage( Installer.INSTALL_WARNING, rb.getString( "install.jsp.install.msg.admin.notexists" ) );
}

    // Make this HTTP response non-cached, and never-expiring
    response.addHeader("Pragma", "no-cache");
    response.setHeader( "Expires", "-1" );
    response.setHeader("Cache-Control", "no-cache" );
    response.setContentType("text/html; charset=UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
  <title><fmt:message key="install.jsp.title" /></title>
  <link rel="stylesheet" media="screen, projection" type="text/css" href='<wiki:Link format="url" templatefile="jspwiki.css"/>'/>
  <wiki:IncludeResources type="stylesheet"/>
</head>
<body class="view">
<div id="wikibody">
<div id="page">
<div id="pagecontent">

<h1><fmt:message key="install.jsp.intro.title" /></h1>

<p><fmt:message key="install.jsp.intro.p1" /></p>

<p><fmt:message key="install.jsp.intro.p2" /></p>

<p><fmt:message key="install.jsp.intro.p3" /></p>

<!-- Any messages or errors? -->
<div class="instructions">
  <wiki:Messages div="information" topic="<%=Installer.INSTALL_INFO%>" prefix='<%= rb.getString( "install.jsp.install.info" )%>'/>
  <wiki:Messages div="warning" topic="<%=Installer.INSTALL_WARNING%>" prefix='<%= rb.getString( "install.jsp.install.warning" )%>'/>
  <wiki:Messages div="error" topic="<%=Installer.INSTALL_ERROR%>" prefix='<%= rb.getString( "install.jsp.install.error" )%>'/>
</div>
 
<div class="formcontainer">

<form action="Install.jsp" method="post">

  <!-- Page and log directories -->
  <h3><fmt:message key="install.jsp.basics.title" /></h3>
  <div class="block">
  
    <label><fmt:message key="install.jsp.basics.appname.label" /></label>
    <input type="text" name="<%=Installer.APP_NAME%>" size="20" value="<%=installer.getProperty( Installer.APP_NAME )%>"/><br />
    <div class="description">
      <fmt:message key="install.jsp.basics.appname.desc" />
    </div>
    
    <label><fmt:message key="install.jsp.basics.baseurl.label" /></label>
    <input type="text" name="<%=Installer.BASE_URL%>" size="40" value="<%=installer.getProperty( Installer.BASE_URL )%>"/><br />
    <div class="description">
      <fmt:message key="install.jsp.basics.baseurl.desc" />
    </div>
    
    <label><fmt:message key="install.jsp.basics.page.storage.label" /></label>
    <input type="text" name="<%=Installer.PAGE_DIR%>" size="40" value="<%=installer.getProperty( Installer.PAGE_DIR )%>"/><br />
    <div class="description">
      <fmt:message key="install.jsp.basics.page.storage.desc" />
    </div>
    
  </div>
  
  <h3><fmt:message key="install.jsp.security.title" /></h3>
  <div class="block">
  
    <label><fmt:message key="install.jsp.security.sec.conf.label" /></label><br/>
    <input type="radio" name="<%=AuthenticationManager.PROP_SECURITY%>" value="<%=AuthenticationManager.SECURITY_JAAS%>" checked="checked"/>
      <fmt:message key="install.jsp.security.sec.conf.opt1" /><br/>
    <input type="radio" name="<%=AuthenticationManager.PROP_SECURITY%>" value="<%=AuthenticationManager.SECURITY_OFF%>"/>
      <fmt:message key="install.jsp.security.sec.conf.opt2" />
   <div class="description">
     <fmt:message key="install.jsp.security.sec.conf.desc" />
   </div>
  
    <% 
      if( validated )
      {
        if ( password != null )
        {
    %>
      <label><fmt:message key="install.jsp.security.admaccount.label" /></label>
      <p><fmt:message key="install.jsp.security.admaccount.enabled" /></p>
      <div class="description">
        <fmt:message key="install.jsp.security.admaccount.enabled.desc" />
      </div>
    <%
        }
      }
      else
      {
    %>
      <label><fmt:message key="install.jsp.security.admaccount.label" /></label>
      <p><fmt:message key="install.jsp.security.admaccount.notenabled" /></p>
      <div class="description">
        <fmt:message key="install.jsp.security.admaccount.notenabled.desc" />
      </div>
    <%
      }
    %>
  </div>
  
  <h3><fmt:message key="install.jsp.adv.settings.title" /></h3>
  <div class="block">
    <label><fmt:message key="install.jsp.adv.settings.logfile.label" /></label>
    <input type="text" name="<%=Installer.LOG_DIR%>" value="<%=installer.getProperty( Installer.LOG_DIR )%>" size="40"/><br />
    <div class="description">
      <fmt:message key="install.jsp.adv.settings.logfile.desc" />
    </div>

    <label><fmt:message key="install.jsp.adv.settings.workdir.label" /></label>
    <input type="text" name="<%=Installer.WORK_DIR%>" size="40" value="<%=installer.getProperty( Installer.WORK_DIR )%>"/><br />
    <div class="description">
      <fmt:message key="install.jsp.adv.settings.workdir.desc" />
    </div>
  </div>
  
  <br /><br />
  <div class="block">
    <div class="instructions">
      <fmt:message key="install.jsp.instr.desc" >
        <fmt:param><%=installer.getPropertiesPath()%></fmt:param>
      </fmt:message>
    </div>
    <input type="submit" name="submit" value="<fmt:message key="install.jsp.instr.submit" />" />
  </div>
      
</form>

</div>

<hr />
    <% 
      if( validated )
      {
    %>
       <h3><fmt:message key="install.jsp.validated.new.props" /></h3>
       <pre><%=installer.getProperties()%></pre>
   <%
     }
   %>

<!-- We're done... -->
</div>
</div>
</div>
</body>
</html>
