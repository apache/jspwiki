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
 * {@link AuthenticationManager#login(HttpServletRequest)}.
 * If container-managed authentication is used, the
 * {@link WebContainerCallbackHandler}is used instead. This callback handler is
 * designed to be used with {@link UserDatabaseLoginModule}.
 * @author Andrew Jaquith
 * @version $Revision: 1.4 $ $Date: 2005-10-19 04:11:25 $
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