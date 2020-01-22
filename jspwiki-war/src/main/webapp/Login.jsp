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

<%@ page import="java.security.Principal" %>
<%@ page import="java.util.*" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.auth.*" %>
<%@ page import="org.apache.wiki.auth.login.CookieAssertionLoginModule" %>
<%@ page import="org.apache.wiki.auth.login.CookieAuthenticationLoginModule" %>
<%@ page import="org.apache.wiki.auth.user.DuplicateUserException" %>
<%@ page import="org.apache.wiki.auth.user.UserProfile" %>
<%@ page import="org.apache.wiki.i18n.InternationalizationManager" %>
<%@ page import="org.apache.wiki.preferences.Preferences" %>
<%@ page import="org.apache.wiki.workflow.DecisionRequiredException" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="http://jspwiki.apache.org/tags" prefix="wiki" %>
<%!
    Logger log = Logger.getLogger("JSPWiki");
%>
<%
    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    AuthenticationManager mgr = wiki.getAuthenticationManager();
    WikiContext wikiContext = new WikiContext( wiki, request, WikiContext.LOGIN );
    pageContext.setAttribute( WikiContext.ATTR_CONTEXT, wikiContext, PageContext.REQUEST_SCOPE );
    WikiSession wikiSession = wikiContext.getWikiSession();
    ResourceBundle rb = Preferences.getBundle( wikiContext, "CoreResources" );

    // Set the redirect-page variable if one was passed as a parameter
    if( request.getParameter( "redirect" ) != null ) {
        wikiContext.setVariable( "redirect", request.getParameter( "redirect" ) );
    } else {
        wikiContext.setVariable( "redirect", wiki.getFrontPage() );
    }

    // Are we saving the profile?
    if( "saveProfile".equals(request.getParameter("action")) ) {
        UserManager userMgr = wiki.getUserManager();
        UserProfile profile = userMgr.parseProfile( wikiContext );
         
        // Validate the profile
        userMgr.validateProfile( wikiContext, profile );

        // If no errors, save the profile now & refresh the principal set!
        if ( wikiSession.getMessages( "profile" ).length == 0 ) {
            try {
                userMgr.setUserProfile( wikiSession, profile );
                CookieAssertionLoginModule.setUserCookie( response, profile.getFullname() );
            } catch( DuplicateUserException due ) {
                // User collision! (full name or wiki name already taken)
                wikiSession.addMessage( "profile", wiki.getInternationalizationManager()
                		                               .get( InternationalizationManager.CORE_BUNDLE,
                		                            		 Preferences.getLocale( wikiContext ), 
                		                            		 due.getMessage(), due.getArgs() ) );
            } catch( DecisionRequiredException e ) {
                String redirect = wiki.getURL( WikiContext.VIEW, "ApprovalRequiredForUserProfiles", null );
                response.sendRedirect( redirect );
                return;
            } catch( WikiSecurityException e ) {
                // Something went horribly wrong! Maybe it's an I/O error...
                wikiSession.addMessage( "profile", e.getMessage() );
            }
        }
        if ( wikiSession.getMessages( "profile" ).length == 0 ) {
            String redirectPage = request.getParameter( "redirect" );
            response.sendRedirect( wikiContext.getViewURL(redirectPage) );
            return;
        }
    }

    // If NOT using container auth, perform all of the access control logic here...
    // (Note: if using the container for auth, it will handle all of this for us.)
    if( !mgr.isContainerAuthenticated() ) {
        // If user got here and is already authenticated, it means they just aren't allowed access to what they asked for.
        // Weepy tears and hankies all 'round.
        if( wikiSession.isAuthenticated() ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN, rb.getString("login.error.noaccess") );
            return;
        }

        // If using custom auth, we need to do the login now

        String action = request.getParameter("action");
        if( request.getParameter("submitlogin") != null ) {
            String uid    = request.getParameter( "j_username" );
            String passwd = request.getParameter( "j_password" );
            log.debug( "Attempting to authenticate user " + uid );

            // Log the user in!
            if ( mgr.login( wikiSession, request, uid, passwd ) ) {
                log.info( "Successfully authenticated user " + uid + " (custom auth)" );
            } else {
                log.info( "Failed to authenticate user " + uid );
                wikiSession.addMessage( "login", rb.getString("login.error.password") );
            }
        }
    } else {
        //
        //  Have we already been submitted?  If yes, then we can assume that we have been logged in before.
        //
        Object seen = session.getAttribute("_redirect");
        if( seen != null ) {
            response.sendError( HttpServletResponse.SC_FORBIDDEN, rb.getString("login.error.noaccess") );
            session.removeAttribute("_redirect");
            return;
        }
        session.setAttribute("_redirect","I love Outi"); // Just any marker will do

        // If using container auth, the container will have automatically
        // attempted to log in the user before Login.jsp was loaded.
        // Thus, if we got here, the container must have authenticated
        // the user already. All we do is simply record that fact.
        // Nice and easy.

        Principal user = wikiSession.getLoginPrincipal();
        log.info( "Successfully authenticated user " + user.getName() + " (container auth)" );
    }

    // If user logged in, set the user cookie with the wiki principal's name.
    // redirect to wherever we're supposed to go. If login.jsp
    // was called without parameters, this will be the front page. Otherwise,
    // there's probably a 'redirect' parameter telling us where to go.

    if( wikiSession.isAuthenticated() ) {
        String rember = request.getParameter( "j_remember" );

        // Set user cookie
        Principal principal = wikiSession.getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( response, principal.getName() );

        if( rember != null ) {
            CookieAuthenticationLoginModule.setLoginCookie( wiki, response, principal.getName() );
        }

        // If wiki page was "Login", redirect to main, otherwise use the page supplied
        String redirectPage = request.getParameter( "redirect" );
        if( !wiki.getPageManager().wikiPageExists( redirectPage ) ) {
           redirectPage = wiki.getFrontPage();
        }
        String viewUrl = ( "Login".equals( redirectPage ) ) ? "Wiki.jsp" : wikiContext.getViewURL( redirectPage );

        // Redirect!
        log.info( "Redirecting user to " + viewUrl );
        response.sendRedirect( viewUrl );
        return;
    }

    // If we've gotten here, the user hasn't authenticated yet.
    // So, find the login form and include it. This should be in the same directory
    // as this page. We don't need to use the wiki:Include tag.

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );

%><jsp:include page="LoginForm.jsp" />