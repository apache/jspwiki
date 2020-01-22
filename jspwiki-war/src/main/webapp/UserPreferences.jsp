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
<%@ page import="org.apache.wiki.WikiContext" %>
<%@ page import="org.apache.wiki.WikiSession" %>
<%@ page import="org.apache.wiki.WikiEngine" %>
<%@ page import="org.apache.wiki.auth.UserManager" %>
<%@ page import="org.apache.wiki.auth.WikiSecurityException" %>
<%@ page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="org.apache.wiki.auth.user.DuplicateUserException" %>
<%@ page import="org.apache.wiki.auth.user.UserProfile" %>
<%@ page import="org.apache.wiki.i18n.InternationalizationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.ui.EditorManager" %>
<%@ page import="org.apache.wiki.ui.TemplateManager" %>
<%@ page import="org.apache.wiki.variables.VariableManager" %>
<%@ page import="org.apache.wiki.workflow.DecisionRequiredException" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>

<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.PREFS );
    if(!wiki.getAuthorizationManager().hasAccess( wikiContext, response )) return;
    
    // Extract the user profile and action attributes
    UserManager userMgr = wiki.getUserManager();
    WikiSession wikiSession = wikiContext.getWikiSession();

/* FIXME: Obsolete
    if( request.getParameter(EditorManager.PARA_EDITOR) != null )
    {
    	String editor = request.getParameter(EditorManager.PARA_EDITOR);
    	session.setAttribute(EditorManager.PARA_EDITOR,editor);
    }
*/

    // Are we saving the profile?
    if( "saveProfile".equals(request.getParameter("action")) )
    {
        UserProfile profile = userMgr.parseProfile( wikiContext );
         
        // Validate the profile
        userMgr.validateProfile( wikiContext, profile );

        // If no errors, save the profile now & refresh the principal set!
        if ( wikiSession.getMessages( "profile" ).length == 0 )
        {
            try
            {
                userMgr.setUserProfile( wikiSession, profile );
                CookieAssertionLoginModule.setUserCookie( response, profile.getFullname() );
            }
            catch( DuplicateUserException due )
            {
                // User collision! (full name or wiki name already taken)
                wikiSession.addMessage( "profile", wiki.getInternationalizationManager()
                                                       .get( InternationalizationManager.CORE_BUNDLE,
                                                    		 Preferences.getLocale( wikiContext ), 
                                                             due.getMessage(), due.getArgs() ) );
            }
            catch( DecisionRequiredException e )
            {
                String redirect = wiki.getURL( WikiContext.VIEW, "ApprovalRequiredForUserProfiles", null );
                response.sendRedirect( redirect );
                return;
            }
            catch( WikiSecurityException e )
            {
                // Something went horribly wrong! Maybe it's an I/O error...
                wikiSession.addMessage( "profile", e.getMessage() );
            }
        }
        if ( wikiSession.getMessages( "profile" ).length == 0 )
        {
            String redirectPage = request.getParameter( "redirect" );

            if( !wiki.getPageManager().wikiPageExists( redirectPage ) )
            {
               redirectPage = wiki.getFrontPage();
            }
            
            String viewUrl = ( "UserPreferences".equals( redirectPage ) ) ? "Wiki.jsp" : wikiContext.getViewURL( redirectPage );
            log.info( "Redirecting user to " + viewUrl );
            response.sendRedirect( viewUrl );
            return;
        }
    }
    if( "setAssertedName".equals(request.getParameter("action")) )
    {
        Preferences.reloadPreferences(pageContext);
        
        String assertedName = request.getParameter("assertedName");
        CookieAssertionLoginModule.setUserCookie( response, assertedName );

        String redirectPage = request.getParameter( "redirect" );
        if( !wiki.getPageManager().wikiPageExists( redirectPage ) )
        {
          redirectPage = wiki.getFrontPage();
        }
        String viewUrl = ( "UserPreferences".equals( redirectPage ) ) ? "Wiki.jsp" : wikiContext.getViewURL( redirectPage );

        log.info( "Redirecting user to " + viewUrl );
        response.sendRedirect( viewUrl );
        return;
    }
    if( "clearAssertedName".equals(request.getParameter("action")) )
    {
        CookieAssertionLoginModule.clearUserCookie( response );
        response.sendRedirect( wikiContext.getURL(WikiContext.NONE,"Logout.jsp") );
        return;
    }
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext, wikiContext.getTemplate(), "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" />

