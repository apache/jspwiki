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

<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.WikiSecurityException" %>
<%@ page import="org.apache.wiki.auth.authorize.Group" %>
<%@ page import="org.apache.wiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.EDIT_GROUP );
    if(!wiki.getAuthorizationManager().hasAccess( wikiContext, response )) return;
    
    // Extract the current user, group name, members and action attributes
    WikiSession wikiSession = wikiContext.getWikiSession();
    GroupManager groupMgr = wiki.getGroupManager();
    Group group = null;
    try 
    {
        group = groupMgr.parseGroup( wikiContext, false );
        pageContext.setAttribute ( "Group", group, PageContext.REQUEST_SCOPE );
    }
    catch ( WikiSecurityException e )
    {
        wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
        response.sendRedirect( "Group.jsp" );
    }
    
    // Are we saving the group?
    if( "save".equals(request.getParameter("action")) )
    {
        // Validate the group
        groupMgr.validateGroup( wikiContext, group );

        // If no errors, save the group now
        if ( wikiSession.getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
        {
            try
            {
                groupMgr.setGroup( wikiSession, group );
            }
            catch( WikiSecurityException e )
            {
                // Something went horribly wrong! Maybe it's an I/O error...
                wikiSession.addMessage( GroupManager.MESSAGES_KEY, e.getMessage() );
            }
        }
        if ( wikiSession.getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
        {
            response.sendRedirect( "Group.jsp?group=" + group.getName() );
            return;
        }
    }
        
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "EditTemplate.jsp" );

%><wiki:Include page="<%=contentPage%>" />

