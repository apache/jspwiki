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

<%@ page import="org.apache.logging.log4j.Logger" %>
<%@ page import="org.apache.logging.log4j.LogManager" %>
<%@ page import="org.apache.wiki.api.core.*" %>
<%@ page import="org.apache.wiki.api.spi.Wiki" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.ui.admin.*" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.commons.lang3.time.StopWatch" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>
<%
    String bean = request.getParameter("bean");
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.WIKI_ADMIN.getRequestContext() );
    if(!wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response ) ) return;

    //
    //  This is an experimental feature, so we will turn it off unless the user really wants to.
    //
    if( !TextUtil.isPositive(wiki.getWikiProperties().getProperty("jspwiki-x.adminui.enable")) )
    {
        %>
        <!doctype html>
        <html lang="en">
        <head>
          <title><wiki:Variable var="applicationname" />: ADMIN UI</title>
          <base href="../"/>
          <link rel="stylesheet" media="screen, projection" type="text/css" href="<wiki:Link format="url" templatefile="jspwiki.css"/>"/>
          <wiki:IncludeResources type="stylesheet"/>
        </head>
        <body class="container">
           <h1>Disabled</h1>
           <p>JSPWiki admin UI has been disabled.  This is an experimental feature, and is
           not guaranteed to work.  You may turn it on by specifying</p>
           <pre>
               jspwiki-x.adminui.enable=true
           </pre>
           <p>in your <code>jspwiki-custom.properties</code> file.</p>
           <p>Have a nice day.  Don't forget to eat lots of fruits and vegetables.</p>
        </body>
        </html>
        <%
        return;
    }

    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getManager( TemplateManager.class ).findJSP( pageContext, wikiContext.getTemplate(), "admin/AdminTemplate.jsp" );

    pageContext.setAttribute( "engine", wiki, PageContext.REQUEST_SCOPE );
    pageContext.setAttribute( "context", wikiContext, PageContext.REQUEST_SCOPE );

    if( request.getMethod().equalsIgnoreCase("post") && bean != null ) {
        AdminBean ab = wiki.getManager( AdminBeanManager.class ).findBean( bean );

        if( ab != null ) {
            ab.doPost( wikiContext );
        } else {
            wikiContext.getWikiSession().addMessage( "No such bean "+bean+" was found!" );
        }
    }

%><wiki:Include page="<%=contentPage%>" />