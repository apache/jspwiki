/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
 */

package org.apache.wiki.action;

import java.security.Principal;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.util.UrlBuilder;
import net.sourceforge.stripes.validation.*;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.apache.wiki.auth.login.CookieAuthenticationLoginModule;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * Logs the user into the wiki.
 */
@UrlBinding( "/Login.jsp" )
public class LoginActionBean extends AbstractActionBean
{
    private static final Logger log = LoggerFactory.getLogger( LoginActionBean.class );

    /**
     * Sets cookies and redirects the user to a wiki page after a successful
     * profile creation or login.
     * 
     * @param bean the WikiActionBean currently executing
     * @param pageName the page to redirect to after login or profile creation
     * @param rememberMe whether to set the "remember me?" cookie
     */
    private Resolution saveCookiesAndRedirect( String pageName, boolean rememberMe )
    {
        // Set user cookie
        Principal principal = getContext().getWikiSession().getUserPrincipal();
        CookieAssertionLoginModule.setUserCookie( getContext().getResponse(), principal.getName() );

        // Set "remember me?" cookie
        if( rememberMe )
        {
            CookieAuthenticationLoginModule.setLoginCookie( getContext().getEngine(), getContext().getResponse(), principal
                .getName() );
        }

        UrlBuilder builder = new UrlBuilder( getContext().getLocale(), ViewActionBean.class, false );
        if( pageName != null )
        {
            builder.addParameter( "page", pageName );
        }
        return new RedirectResolution( builder.toString() );
    }

    private String m_username = null;

    private boolean m_remember = false;

    private String m_password;

    private String m_redirect = null;

    public String getJ_password()
    {
        return m_password;
    }

    public boolean getRemember()
    {
        return m_remember;
    }

    public String getJ_username()
    {
        return m_username;
    }

    public String getRedirect()
    {
        return m_redirect;
    }

    @ValidationMethod( on = "login", when = ValidationState.NO_ERRORS )
    public void validateCredentials()
    {
        WikiSession wikiSession = getContext().getWikiSession();
        ValidationErrors errors = getContext().getValidationErrors();
        HttpServletRequest request = getContext().getRequest();

        log.debug( "Attempting to authenticate user " + m_username );

        // Log the user in!
        try
        {
            if( !getContext().getEngine().getAuthenticationManager().login( wikiSession, request, m_username, m_password ) )
            {
            }
        }
        catch ( LoginException e )
        {
        	// Message is already localized
            log.info( "Failed to authenticate user " + m_username + ", reason: " + e.getMessage());
            errors.addGlobalError( new SimpleError( e.getMessage() ) );
        }
        catch( WikiSecurityException e )
        {
        	// Message is already localized
            log.info( "Failed to authenticate user " + m_username + ", reason: " + e.getMessage());
            errors.addGlobalError( new SimpleError( e.getMessage() ) );
        }
    }

    @HandlesEvent( "login" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    public Resolution login()
    {
        // Set cookies as needed and redirect
        log.info( "Successfully authenticated user " + m_username + " (custom auth)" );
        return saveCookiesAndRedirect( m_redirect, m_remember );
    }

    @HandlesEvent( "logout" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "logout" )
    public Resolution logout()
    {
        WikiEngine engine = getContext().getEngine();
        HttpServletRequest request = getContext().getRequest();
        HttpServletResponse response = getContext().getResponse();
        engine.getAuthenticationManager().logout( request );

        // Clear the asserted name cookie
        CookieAssertionLoginModule.clearUserCookie( response );

        // Delete the authentication cookie
        if( engine.getAuthenticationManager().allowsCookieAuthentication() )
        {
            CookieAuthenticationLoginModule.clearLoginCookie( engine, request, response );
        }

        return new RedirectResolution( ViewActionBean.class );
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_password( String password )
    {
        m_password = password;
    }

    @Validate()
    public void setRemember( boolean remember )
    {
        m_remember = remember;
    }

    @Validate( required = true, on = "login", minlength = 1, maxlength = 128 )
    public void setJ_username( String username )
    {
        m_username = username;
    }

    @Validate()
    public void setRedirect( String redirect )
    {
        m_redirect = redirect;
        getContext().setVariable( "redirect", redirect );
    }

    /**
     * Default handler that executes when the login page is viewed. It checks to
     * see if the user is already authenticated, and if so, sends a "forbidden"
     * message.
     * 
     * @return the resolution
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "view" )
    @HandlerPermission( permissionClass = WikiPermission.class, target = "${context.engine.applicationName}", actions = WikiPermission.LOGIN_ACTION )
    @WikiRequestContext( "login" )
    public Resolution view()
    {
        ValidationErrors errors = getContext().getValidationErrors();

        // If user got here and is already authenticated, it means
        // they just aren't allowed access to what they asked for.
        // Weepy tears and hankies all 'round.
        if( getContext().getWikiSession().isAuthenticated() )
        {
            errors.addGlobalError( new LocalizableError( "login.error.noaccess" ) );
            return new RedirectResolution( MessageActionBean.class );
        }

        if( getContext().getEngine().getAuthenticationManager().isContainerAuthenticated() )
        {
            //
            // Have we already been submitted? If yes, then we can assume that
            // we have been logged in before.
            //

            HttpSession session = getContext().getRequest().getSession();
            Object seen = session.getAttribute( "_redirect" );
            if( seen != null )
            {
                getContext().getValidationErrors().addGlobalError( new LocalizableError( "login.error.noaccess" ) );
                return new RedirectResolution( MessageActionBean.class );
            }
            session.setAttribute( "_redirect", "I love Outi" ); // Any marker
            // will do

            // If using container auth, the container will have automatically
            // attempted to log in the user before Login.jsp was loaded.
            // Thus, if we got here, the container must have authenticated
            // the user already. All we do is simply record that fact.
            // Nice and easy.

            Principal user = getContext().getWikiSession().getLoginPrincipal();
            log.info( "Successfully authenticated user " + user.getName() + " (container auth)" );
        }

        // The user hasn't logged in yet, so forward them to the template JSP
        return new ForwardResolution( "/templates/default/Login.jsp" ).addParameter( "tab", "login" );
    }
}
