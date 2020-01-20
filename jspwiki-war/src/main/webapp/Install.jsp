<%--
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>

<?xml version="1.0" encoding="UTF-8"?>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.AuthenticationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.ui.Installer" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="java.text.MessageFormat" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="CoreResources"/>

<%!
    Logger log = Logger.getLogger("JSPWiki");
%>

<%
WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
// Create wiki context and check for authorization
WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.INSTALL );
if(!wiki.getAuthorizationManager().hasAccess( wikiContext, response )) return;

Installer installer = new Installer( request, config );
WikiSession wikiSession = wikiContext.getWikiSession();

// Parse the existing properties
installer.parseProperties();
boolean validated = false;
String password = null;
ResourceBundle rb = Preferences.getBundle( wikiContext, "CoreResources" );

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
<!doctype html>
<html lang="en">
<head>
  <title><fmt:message key="install.jsp.title" /></title>
  <link rel="stylesheet" media="screen, projection" type="text/css" href='<wiki:Link format="url" templatefile="jspwiki.css"/>'/>
  <wiki:IncludeResources type="stylesheet"/>
</head>
<body class="container">
<div id="wikibody">
<div id="page">
<div id="pagecontent">

<h1><fmt:message key="install.jsp.intro.title" /></h1>

<p><fmt:message key="install.jsp.intro.p1" /></p>

<p><fmt:message key="install.jsp.intro.p2" /></p>

<p><fmt:message key="install.jsp.intro.p3" /></p>

<!-- Any messages or errors? -->
<div class="help-block">
  <wiki:Messages div="information" topic="<%=Installer.INSTALL_INFO%>" prefix='<%= rb.getString( "install.jsp.install.info" )%>'/>
  <wiki:Messages div="warning" topic="<%=Installer.INSTALL_WARNING%>" prefix='<%= rb.getString( "install.jsp.install.warning" )%>'/>
  <wiki:Messages div="error" topic="<%=Installer.INSTALL_ERROR%>" prefix='<%= rb.getString( "install.jsp.install.error" )%>'/>
</div>

<div class="formcontainer">

<form action="Install.jsp" method="post">

  <!-- Page directory -->
  <h3><fmt:message key="install.jsp.basics.title" /></h3>

    <label class="control-label" ><fmt:message key="install.jsp.basics.appname.label" />
    <input class="form-control" type="text" name="<%=Installer.APP_NAME%>" size="20" value="<%=installer.getProperty( Installer.APP_NAME )%>"/>
    </label>
    <div class="help-block">
      <fmt:message key="install.jsp.basics.appname.desc"/>
    </div>

    <label class="control-label" ><fmt:message key="install.jsp.basics.page.storage.label" />
    <input class="form-control" type="text" name="<%=Installer.PAGE_DIR%>" size="40" value="<%=installer.getProperty( Installer.PAGE_DIR )%>"/>
    </label>
    <div class="help-block">
      <fmt:message key="install.jsp.basics.page.storage.desc" />
    </div>


  <h3><fmt:message key="install.jsp.security.title" /></h3>

    <%
      if( validated )
      {
        if ( password != null )
        {
    %>
      <label class="control-label" ><fmt:message key="install.jsp.security.admaccount.label" /></label>
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
      <label class="control-label" ><fmt:message key="install.jsp.security.admaccount.label" /></label>
      <p><fmt:message key="install.jsp.security.admaccount.notenabled" /></p>
      <div class="help-block">
        <fmt:message key="install.jsp.security.admaccount.notenabled.desc" />
      </div>
    <%
      }
    %>

  <h3><fmt:message key="install.jsp.adv.settings.title" /></h3>

    <label class="control-label" ><fmt:message key="install.jsp.adv.settings.workdir.label" />
    <input class="form-control" type="text" name="<%=Installer.WORK_DIR%>" size="40" value="<%=installer.getProperty( Installer.WORK_DIR )%>"/>
    </label>
    <div class="help-block">
      <fmt:message key="install.jsp.adv.settings.workdir.desc" />
    </div>


    <p class="help-block">
      <fmt:message key="install.jsp.instr.desc" >
        <fmt:param><%=installer.getPropertiesPath()%></fmt:param>
      </fmt:message>
    </p>
    <input class="btn btn-primary" type="submit" name="submit" value="<fmt:message key="install.jsp.instr.submit" />" />

</form>

</div>

<hr />
    <%
      if( validated )
      {
    %>
       <h3><fmt:message key="install.jsp.validated.new.props" /></h3>
       <pre><%=installer.getPropertiesList()%></pre>
   <%
     }
   %>

<!-- We're done... -->
</div>
</div>
</div>
</body>
</html>
