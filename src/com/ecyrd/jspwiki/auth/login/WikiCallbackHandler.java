/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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
