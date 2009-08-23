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
package org.apache.wiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * <p>
 * Logs in a user based on a username, password, and static password file
 * location. This module must be used with a CallbackHandler (such as
 * {@link WikiCallbackHandler}) that supports the following Callback types:
 * </p>
 * <ol>
 * <li>{@link javax.security.auth.callback.NameCallback}- supplies the username</li>
 * <li>{@link javax.security.auth.callback.PasswordCallback}- supplies the
 * password</li>
 * <li>{@link org.apache.wiki.auth.login.UserDatabaseCallback}- supplies the
 * {@link org.apache.wiki.auth.user.UserDatabase}, which authenticates the user</li>
 * </ol>
 * <p>
 * After authentication, a Principals based on the login name will be created
 * and associated with the Subject.
 * </p>
 * 
 * @since 2.3
 */
public class UserDatabaseLoginModule extends AbstractLoginModule
{

    private static final InternationalizationManager I18N = new InternationalizationManager( null );

    private static final Logger log = LoggerFactory.getLogger( UserDatabaseLoginModule.class );

    /**
     * {@inheritDoc}
     * <p>
     * Note: this method will throw a
     * {@link javax.security.auth.login.FailedLoginException} if the username or
     * password does not match what is contained in the database. The text of
     * this message will be looked up in the
     * {@link org.apache.wiki.i18n.InternationalizationManager#CORE_BUNDLE}
     * using the key <code>login.error.password</code>. Any other Exceptions
     * thrown by this method will <em>not</em> be localized, because they
     * represent exceptional error conditions that should not occur unless the
     * wiki is configured incorrectly.
     * </p>
     */
    public boolean login() throws LoginException
    {
        NameCallback ncb = new NameCallback( "User name" );
        PasswordCallback pcb = new PasswordCallback( "Password", false );
        UserDatabaseCallback ucb = new UserDatabaseCallback();
        Callback[] callbacks = new Callback[] { ncb, pcb, ucb };
        try
        {
            m_handler.handle( callbacks );
            String username = ncb.getName();
            String password = new String( pcb.getPassword() );
            UserDatabase db = ucb.getUserDatabase();

            // Look up the user and check the password
            if( db == null )
            {
                throw new LoginException( "No user database: check the callback handler code!" );
            }
            if( db.validatePassword( username, password ) )
            {
                if( log.isDebugEnabled() )
                {
                    log.debug( "Logged in user database user " + username );
                }

                // If login succeeds, commit these principals/roles
                m_principals.add( new WikiPrincipal( username, WikiPrincipal.LOGIN_NAME ) );

                return true;
            }
            throw new FailedLoginException( I18N.get( InternationalizationManager.CORE_BUNDLE, m_locale, "login.error.password" ) );
        }
        catch( IOException e )
        {
            String message = "IO exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
    }

}
