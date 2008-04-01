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

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

import com.ecyrd.jspwiki.auth.user.UserDatabase;

/**
 * Handles logins made from inside the wiki application, rather than via the web
 * container. This handler is instantiated in
 * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#login(WikiSession, String, String)}.
 * If container-managed authentication is used, the
 * {@link WebContainerCallbackHandler}is used instead. This callback handler is
 * designed to be used with {@link UserDatabaseLoginModule}.
 * @author Andrew Jaquith
 * @since 2.3
 */
public class WikiCallbackHandler implements CallbackHandler
{
    private final UserDatabase m_database;

    private final String       m_password;

    private final String       m_username;

    public WikiCallbackHandler( UserDatabase database, String username, String password )
    {
        m_database = database;
        m_username = username;
        m_password = password;
    }

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException
    {
        for( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];
            if ( callback instanceof UserDatabaseCallback )
            {
                ( (UserDatabaseCallback) callback ).setUserDatabase( m_database );
            }
            else if ( callback instanceof NameCallback )
            {
                ( (NameCallback) callback ).setName( m_username );
            }
            else if ( callback instanceof PasswordCallback )
            {
                ( (PasswordCallback) callback ).setPassword( m_password.toCharArray() );
            }
            else
            {
                throw new UnsupportedCallbackException( callback );
            }
        }
    }
}
