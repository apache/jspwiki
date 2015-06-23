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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;

import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.util.HttpUtil;

/**
 * <p>
 * Logs in a user based solely on IP address; no other authentication is
 * performed. Barring a mis-configuration or I/O error, this LoginModule
 * <em>always</em> succeeds.
 * </p>
 * This module must be used with a CallbackHandler (such as
 * {@link WebContainerCallbackHandler}) that supports the following Callback
 * types:
 * </p>
 * <ol>
 * <li>{@link HttpRequestCallback}- supplies the IP address, which is used as
 * a backup in case no name is supplied.</li>
 * </ol>
 * <p>
 * After authentication, a generic WikiPrincipal based on the IP address will be
 * created and associated with the Subject.
 * @see javax.security.auth.spi.LoginModule#commit()
 *      </p>
 * @since 2.3
 */
public class AnonymousLoginModule extends AbstractLoginModule
{

    /**
     * Bogus prompt sent to the callback handler.
     */
    public static final String PROMPT            = "User name";

    protected static final Logger log            = Logger.getLogger( AnonymousLoginModule.class );

    /**
     * Logs in the user by calling back to the registered CallbackHandler with an
     * HttpRequestCallback. The CallbackHandler must supply the current servlet
     * HTTP request as its response.
     * @return the result of the login; this will always be <code>true</code>.
     * @see javax.security.auth.spi.LoginModule#login()
     * @throws {@inheritDoc}
     */
    public boolean login() throws LoginException
    {
        // Let's go and make a Principal based on the IP address
        HttpRequestCallback hcb = new HttpRequestCallback();
        Callback[] callbacks = new Callback[]
        { hcb };
        try
        {
            m_handler.handle( callbacks );
            HttpServletRequest request = hcb.getRequest();
            WikiPrincipal ipAddr = new WikiPrincipal( HttpUtil.getRemoteAddress(request) );
            if ( log.isDebugEnabled() )
            {
                HttpSession session = request.getSession( false );
                String sid = (session == null) ? NULL : session.getId();
                log.debug("Logged in session ID=" + sid + "; IP=" + ipAddr);
            }
            // If login succeeds, commit these principals/roles
            m_principals.add( ipAddr );
            return true;
        }
        catch( IOException e )
        {
            log.error("IOException: " + e.getMessage());
            return false;
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback, disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

    }

}
