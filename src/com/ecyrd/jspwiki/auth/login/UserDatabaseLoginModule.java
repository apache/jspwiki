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
package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;
import java.util.Locale;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 * <p>
 * Logs in a user based on a username, password, and static password file
 * location. This module must be used with a CallbackHandler (such as
 * {@link WikiCallbackHandler}) that supports the following Callback types:
 * </p>
 * <ol>
 * <li>{@link javax.security.auth.callback.NameCallback}- supplies the
 * username</li>
 * <li>{@link javax.security.auth.callback.PasswordCallback}- supplies the
 * password</li>
 * <li>{@link com.ecyrd.jspwiki.auth.login.LocaleCallback}- supplies the
 * HTTP request, from which the user's {@link java.util.Locale} is obtained
 * (used for constructing localized error messages)</li>
 * <li>{@link com.ecyrd.jspwiki.auth.login.WikiEngineCallback}- supplies the
 * {@link com.ecyrd.jspwiki.WikiEngine}, from which the
 * {@link com.ecyrd.jspwiki.i18n.InternationalizationManager} is obtained
 * (used for constructing localized error messages)</li>
 * </ol>
 * <p>
 * After authentication, a Principals based on the login name will be created
 * and associated with the Subject.
 * </p>
 * @author Andrew Jaquith
 * @since 2.3
 */
public class UserDatabaseLoginModule extends AbstractLoginModule
{

    private static final Logger log = LoggerFactory.getLogger( UserDatabaseLoginModule.class );

    /**
     *      {@inheritDoc}
     *      <p>Note: this method will throw a
     *      {@link javax.security.auth.login.FailedLoginException} if the
     *      username or password does not match what is contained in the
     *      database. The text of this message will be looked up in the
     *      {@link com.ecyrd.jspwiki.i18n.InternationalizationManager#CORE_BUNDLE}
     *      using the key <code>login.error.password</code>. Any other
     *      Exceptions thrown by this method will <em>not</em> be localized,
     *      because they represent exceptional error conditions that should not
     *      occur unless the wiki is configured incorrectly.</p>
     */
    public boolean login() throws LoginException
    {
        NameCallback ncb = new NameCallback( "User name" );
        PasswordCallback pcb = new PasswordCallback( "Password", false );
        LocaleCallback lcb = new LocaleCallback();
        WikiEngineCallback wcb = new WikiEngineCallback();
        Callback[] callbacks = new Callback[] { ncb, pcb, lcb, wcb };
        try
        {
            m_handler.handle( callbacks );
            String username = ncb.getName();
            String password = new String( pcb.getPassword() );
            Locale locale = lcb.getLocale();
            WikiEngine engine = wcb.getEngine();
            UserDatabase db = engine.getUserManager().getUserDatabase();
            InternationalizationManager i18n = engine.getInternationalizationManager();

            // Look up the user and compare the password hash
            if ( db == null )
            {
                throw new LoginException( "No user database: check the callback handler code!" );
            }
            UserProfile profile;
            try
            {
                profile = db.findByLoginName( username );
            }
            catch( NoSuchPrincipalException e )
            {
                throw new FailedLoginException( i18n.get( InternationalizationManager.CORE_BUNDLE, locale, "login.error.password" ) );
            }
            String storedPassword = profile.getPassword();
            if ( storedPassword != null && db.validatePassword( username, password ) )
            {
                if ( log.isDebugEnabled() )
                {
                    log.debug( "Logged in user database user " + username );
                }

                // If login succeeds, commit these principals/roles
                m_principals.add( new WikiPrincipal( username,  WikiPrincipal.LOGIN_NAME ) );

                return true;
            }
            throw new FailedLoginException( i18n.get( InternationalizationManager.CORE_BUNDLE, locale, "login.error.password" ) );
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
