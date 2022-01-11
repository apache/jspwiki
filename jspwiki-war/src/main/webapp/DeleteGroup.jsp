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
<%@ page import="org.apache.wiki.auth.AuthorizationManager" %>
<%@ page import="org.apache.wiki.auth.NoSuchPrincipalException" %>
<%@ page import="org.apache.wiki.auth.WikiSecurityException" %>
<%@ page import="org.apache.wiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

<%!
    Logger log = LogManager.getLogger("JSPWiki");
%>

<%
    Engine wiki = Wiki.engine().find( getServletConfig() );
    // Create wiki context and check for authorization
    Context wikiContext = Wiki.context().create( wiki, request, ContextEnum.GROUP_DELETE.getRequestContext() );
    if(!wiki.getManager( AuthorizationManager.class ).hasAccess( wikiContext, response )) return;

    Session wikiSession = wikiContext.getWikiSession();
    GroupManager groupMgr = wiki.getManager( GroupManager.class );
    String name = request.getParameter( "group" );

    if ( name == null )
    {
        // Group parameter was null
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, "Parameter 'group' cannot be null." );
        response.sendRedirect( "Group.jsp" );
    }

    // Check that the group exists first
    try
    {
        groupMgr.getGroup( name );
    }
    catch ( NoSuchPrincipalException e )
    {
        // Group does not exist
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
        response.sendRedirect( "Group.jsp" );
    }

    // Now, let's delete the group
    try
    {
        groupMgr.removeGroup( name );
        //response.sendRedirect( "." );
        response.sendRedirect( "Group.jsp?group=" + name );
    }
    catch ( WikiSecurityException e )
    {
        // Send error message
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
        response.sendRedirect( "Group.jsp" );
    }

%>

