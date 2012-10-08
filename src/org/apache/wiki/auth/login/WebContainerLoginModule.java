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

import java.io.IOException;
import java.security.Principal;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import org.apache.wiki.auth.WikiPrincipal;

/**
 * <p>
 * Logs in a user by extracting authentication data from an Http servlet
 * session. First, the module tries to extract a Principal object out of the
 * request directly using the servlet requests's <code>getUserPrincipal()</code>
 * method. If one is found, authentication succeeds. If there is no
 * Principal in the request, try calling <code>getRemoteUser()</code>. If
 * the <code>remoteUser</code> exists but the UserDatabase can't find a matching
 * profile, a generic WikiPrincipal is created with this value. If neither
 * <code>userPrincipal</code> nor <code>remoteUser</code> exist in the request, the login fails.
 * </p>
 * <p>
 * This module must be used with a CallbackHandler that supports the following
 * Callback types:
 * </p>
 * <ol>
 * <li>{@link HttpRequestCallback} - supplies the Http request object, from
 * which the getRemoteUser and getUserPrincipal are extracted</li>
 * <li>{@link UserDatabaseCallback} - supplies the user database for looking up
 * the value of getRemoteUser</li>
 * </ol>
 * <p>
 * After authentication, the Subject will contain the Principal that
 * represents the logged-in user.</p>
 *
 * @since 2.3
 */
public class WebContainerLoginModule extends AbstractLoginModule
{

    protected static final Logger log      = Logger.getLogger( WebContainerLoginModule.class );

    /**
     * Logs in the user.
     * @see javax.security.auth.spi.LoginModule#login()
     * 
     * @return {@inheritDoc}
     * @throws {@inheritDoc}
     */
    public boolean login() throws LoginException
    {
        HttpRequestCallback rcb = new HttpRequestCallback();
        Callback[] callbacks = new Callback[] { rcb };
        String userId = null;

        try
        {
            // First, try to extract a Principal object out of the request
            // directly. If we find one, we're done.
            m_handler.handle( callbacks );
            HttpServletRequest request = rcb.getRequest();
            if ( request == null )
            {
                throw new LoginException( "No Http request supplied." );
            }
            HttpSession session = request.getSession(false);
            String sid = (session == null) ? NULL : session.getId();
            Principal principal = request.getUserPrincipal();
            if ( principal == null )
            {
                // If no Principal in request, try the remoteUser
                if ( log.isDebugEnabled() )
                {
                    log.debug( "No userPrincipal found for session ID=" + sid);
                }
                userId = request.getRemoteUser();
                if ( userId == null )
                {
                    if ( log.isDebugEnabled() )
                    {
                        log.debug( "No remoteUser found for session ID=" + sid);
                    }
                    throw new FailedLoginException( "No remote user found" );
                }
                principal = new WikiPrincipal( userId, WikiPrincipal.LOGIN_NAME );
            }
            if ( log.isDebugEnabled() )
            {
                log.debug("Logged in container principal " + principal.getName() + "." );
            }
            m_principals.add( principal );

            return true;
        }
        catch( IOException e )
        {
            log.error( "IOException: " + e.getMessage() );
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            log.error( "UnsupportedCallbackException: " + e.getMessage() );
            return false;
        }
    }
}
