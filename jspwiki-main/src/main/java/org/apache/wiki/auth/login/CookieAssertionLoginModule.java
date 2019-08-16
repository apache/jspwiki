/*
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
package org.apache.wiki.auth.login;

import org.apache.log4j.Logger;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

/**
 * <p>
 * Logs in a user based on assertion of a name supplied in a cookie. If the
 * cookie is not found, authentication fails.
 * </p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 * <ol>
 * <li>{@link HttpRequestCallback}- supplies the cookie, which should contain
 * a user name.</li>
 * </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the username will be
 * created and associated with the Subject.
 * </p>
 * @see javax.security.auth.spi.LoginModule#commit()
 * @see CookieAuthenticationLoginModule
 * @since 2.3
 */
public class CookieAssertionLoginModule extends AbstractLoginModule {

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiAssertedName";

    protected static final Logger log = Logger.getLogger( CookieAssertionLoginModule.class );

    /**
     * Logs in the user by calling back to the registered CallbackHandler with
     * an HttpRequestCallback. The CallbackHandler must supply the current
     * servlet HTTP request as its response.
     * @return the result of the login; if a cookie is
     * found, this method returns <code>true</code>. If not found, this
     * method throws a <code>FailedLoginException</code>.
     * @see javax.security.auth.spi.LoginModule#login()
     * @throws {@inheritDoc}
     */
    public boolean login() throws LoginException
    {
        // Otherwise, let's go and look for the cookie!
        HttpRequestCallback hcb = new HttpRequestCallback();
        Callback[] callbacks = new Callback[]
        { hcb };
        try
        {
            m_handler.handle( callbacks );
            HttpServletRequest request = hcb.getRequest();
            HttpSession session = ( request == null ) ? null : request.getSession( false );
            String sid = ( session == null ) ? NULL : session.getId();
            String name = (request != null) ? getUserCookie( request ) : null;
            if ( name == null )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "No cookie " + PREFS_COOKIE_NAME + " present in session ID=:  " + sid );
                }
                throw new FailedLoginException( "The user cookie was not found." );
            }

            if ( log.isDebugEnabled() )
            {
                log.debug( "Logged in session ID=" + sid + "; asserted=" + name );
            }
            // If login succeeds, commit these principals/roles
            m_principals.add( new WikiPrincipal( name, WikiPrincipal.FULL_NAME ) );
            return true;
        }
        catch( IOException e )
        {
            log.error( "IOException: " + e.getMessage() );
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback, disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

    }

    /**
     *  Returns the username cookie value.
     *
     *  @param request The Servlet request, as usual.
     *  @return the username, as retrieved from the cookie
     */
    public static String getUserCookie( HttpServletRequest request )
    {
        String cookie = HttpUtil.retrieveCookieValue( request, PREFS_COOKIE_NAME );

        return TextUtil.urlDecodeUTF8(cookie);
    }

    /**
     *  Sets the username cookie.  The cookie value is URLEncoded in UTF-8.
     *
     *  @param response The Servlet response
     *  @param name     The name to write into the cookie.
     */
    public static void setUserCookie( HttpServletResponse response, String name )
    {
        name = TextUtil.urlEncodeUTF8(name);
        Cookie userId = new Cookie( PREFS_COOKIE_NAME, name );
        userId.setMaxAge( 1001 * 24 * 60 * 60 ); // 1001 days is default.
        response.addCookie( userId );
    }

    /**
     *  Removes the user cookie from the response.  This makes the user appear
     *  again as an anonymous coward.
     *
     *  @param response The servlet response.
     */
    public static void clearUserCookie( HttpServletResponse response )
    {
        Cookie userId = new Cookie( PREFS_COOKIE_NAME, "" );
        userId.setMaxAge( 0 );
        response.addCookie( userId );
    }
}
