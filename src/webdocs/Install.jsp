<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="javax.servlet.http.*" %>
<%@ page import="javax.servlet.*" %>
<%@ page import="java.io.*" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.AuthenticationManager" %>
<%@ page import="com.ecyrd.jspwiki.ui.Installer" %>
<%@ page import="org.apache.log4j.*" %>

<%!
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Logger log = Logger.getLogger("JSPWiki"); 
    WikiEngine wiki;
%>

<%
// Create wiki context and check for authorization
WikiContext wikiContext = wiki.createContext( request, WikiContext.INSTALL );
if(!wikiContext.hasAccess( response )) return;

Installer installer = new Installer( request, config );
WikiSession wikiSession = wikiContext.getWikiSession();

// Parse the existing properties
installer.parseProperties();
boolean validated = false;
String password = null;

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
            wikiSession.addMessage( Installer.INSTALL_INFO, "Because no administrator account "
              + "exists yet, JSPWiki created one for you, with a random password. You can change "
              + "this password later, of course. The account's id is <strong>" + Installer.ADMIN_ID
              + "</strong> and the password is <strong>" + password + "</strong>. "
              + "<em>Please write this information down and keep it in a safe place</em>. "
              + "JSPWiki also created a wiki group called <strong>" + Installer.ADMIN_GROUP
              + "</strong> that contains this user." );
       }
    }
}

if ( !installer.adminExists() )
{
    wikiSession.addMessage( Installer.INSTALL_WARNING, "Is this the first time you've run the "
        + " Installer? If it is, you should know that after JSPWiki validates and saves your " 
        + "configuration for the first time, you will need administrative privileges to access "
        + "this page again. We do this to prevent random people on the Internet from doing bad "
        + "things to your wiki." );
}

    // Make this HTTP response non-cached, and never-expiring
    response.addHeader("Pragma", "no-cache");
    response.setHeader( "Expires", "-1" );
    response.setHeader("Cache-Control", "no-cache" );
    response.setContentType("text/html; charset=UTF-8");
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
<head>
  <title>JSPWiki Installer</title>
  <link rel="stylesheet" media="screen, projection" type="text/css" href="<wiki:Link format="url" templatefile="jspwiki.css"/>"/>
  <wiki:IncludeResources type="stylesheet"/>
</head>
<body class="view">
<div id="wikibody">
<div id="page">
<div id="pagecontent">

<h1>JSPWiki Installer</h1>

<p>Welcome!  This little JSP page is here to help you do the first difficult stage of JSPWiki
installation.  If you're seeing this page, you have already installed JSPWiki correctly
inside your container.</p>

<p>There are now some things that you should configure.  When you press submit, the
<code>jspwiki.properties</code> file from the distribution will be modified, or if it 
can't be found, a new one will be created.</p>

<p>This setup system is really meant for people who just want to be up and running really quickly.
If you want to integrate JSPWiki with an existing system, I would recommend that you go and edit
the <code>jspwiki.properties</code> file directly.  You can find a sample config file from 
<code>yourwiki/WEB-INF/</code>.</p>

<!-- Any messages or errors? -->
<div class="instructions">
  <wiki:Messages div="information" topic="<%=Installer.INSTALL_INFO%>" prefix="Good news:"/>
  <wiki:Messages div="warning" topic="<%=Installer.INSTALL_WARNING%>" prefix="Warning: "/>
  <wiki:Messages div="error" topic="<%=Installer.INSTALL_ERROR%>" prefix="Could not save configuration: "/>
</div>
 
<div class="formcontainer">

<form action="Install.jsp" method="post">

  <!-- Page and log directories -->
  <h3>Basics</h3>
  <div class="block">
  
    <label>Application Name</label>
    <input type="text" name="<%=Installer.APP_NAME%>" size="20" value="<%=installer.getProperty( Installer.APP_NAME )%>"/><br />
    <div class="description">
      What should your wiki be called?  Try and make this a relatively short name.
    </div>
    
    <label>Base URL</label>
    <input type="text" name="<%=Installer.BASE_URL%>" size="40" value="<%=installer.getProperty( Installer.BASE_URL )%>"/><br />
    <div class="description">
      Please tell JSPWiki where your wiki is located.
    </div>
    
    <label>Page storage</label>
    <input type="text" name="<%=Installer.PAGE_DIR%>" size="40" value="<%=installer.getProperty( Installer.PAGE_DIR )%>"/><br />
    <div class="description">
      By default, JSPWiki will use the VersioningFileProvider that stores files in a particular
      directory on your hard drive. If you specify a directory that does not exist, JSPWiki will
      create one for you. All attachments will also be put in the same directory.
    </div>
    
  </div>
  
  <h3>Security</h3>
  <div class="block">
  
    <label>Security configuration</label>
    <input type="radio" name="<%=AuthenticationManager.PROP_SECURITY%>" value="<%=AuthenticationManager.SECURITY_JAAS%>" checked="checked">
      JAAS plus container security (default)
    </input><br/>
    <input type="radio" name="<%=AuthenticationManager.PROP_SECURITY%>" value="<%=AuthenticationManager.SECURITY_CONTAINER%>">
      Container security only
    </input>
   <div class="description">
     By default, JSPWiki manages access to resources using a JAAS-based security system. 
     It will also respect any container security constraints you might have,
     if you've enabled them in your <code>web.xml</code> file. If you disable JAAS security,
     JSPWiki might not work as you expect. But sometimes you might want to do this if you're
     trying to troubleshoot.
   </div>
  
    <% 
      if( validated )
      {
        if ( password != null )
        {
    %>
      <label>Administrator account</label>
      <p>Enabled</p>
      <div class="description">
        This wiki has an administrator account named <strong>admin</strong> that is part of
        the wiki group <strong>Admin</strong>. By default, JSPWiki's security policy grants 
        all members of the Admin group the all-powerful <code>AllPermission</code>.
      </div>
    <%
        }
      }
      else
      {
    %>
      <label>Administrator account</label>
      <p>Not enabled</p>
      <div class="description">
        This wiki doesn't seem to have an administrator account. When you click <em>Configure!</em>,
        JSPWiki will create one for you.
      </div>
    <%
      }
    %>
  </div>
  
  <h3>Advanced Settings</h3>
  <div class="block">
    <label>Log files</label>
    <input type="text" name="<%=Installer.LOG_DIR%>" value="<%=installer.getProperty( Installer.LOG_DIR )%>" size="40"/><br />
    <div class="description">
      JSPWiki uses Jakarta Log4j for logging.  Please tell JSPWiki where the log files should go.
    </div>

    <label>Work directory</label>
    <input type="text" name="<%=Installer.WORK_DIR%>" size="40" value="<%=installer.getProperty( Installer.WORK_DIR )%>"/><br />
    <div class="description">
      This is the place where all caches and other runtime stuff is stored.
    </div>
  </div>
    
  <div class="block">
    <div class="instructions">
      After you click <em>Configure!</em>, the installer will write your settings to 
      <code><%=installer.getPropertiesPath()%></code>. It will also create an 
      Administrator account with a random password and a corresponding Admin group.
    </div>
    <input type="submit" name="submit" value="Configure!" />
  </div>
      
</form>

</div>

<hr />
    <% 
      if( validated )
      {
    %>
       <h3>Here is your new jspwiki.properties</h3>
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
