package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.user.UserDatabase;

/**
 * Handles logins made from within JSPWiki.
 * @link AuthenticationManager#getWikiSession(HttpServletRequest).
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2006-05-28 23:25:07 $
 * @since 2.3
 */
public class WebContainerCallbackHandler implements CallbackHandler
{
    private final UserDatabase       m_database;

    private final HttpServletRequest m_request;

    private final Authorizer         m_authorizer;
    
    public WebContainerCallbackHandler( HttpServletRequest request, UserDatabase database, Authorizer authorizer )
    {
        m_request = request;
        m_database = database;
        m_authorizer = authorizer;
    }

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException
    {
        for( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];
            if ( callback instanceof HttpRequestCallback )
            {
                ( (HttpRequestCallback) callback ).setRequest( m_request );
            }
            else if ( callback instanceof UserDatabaseCallback )
            {
                ( (UserDatabaseCallback) callback ).setUserDatabase( m_database );
            }
            else if ( callback instanceof AuthorizerCallback )
            {
                ( (AuthorizerCallback) callback ).setAuthorizer( m_authorizer );
            }
            else
            {
                throw new UnsupportedCallbackException( callback );
            }
        }
    }

}