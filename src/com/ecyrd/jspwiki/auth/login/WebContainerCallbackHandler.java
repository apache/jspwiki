package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Handles logins made from within JSPWiki.
 * @link AuthenticationManager#getWikiSession(HttpServletRequest).
 * @author Andrew Jaquith
 * @since 2.3
 */
public final class WebContainerCallbackHandler implements CallbackHandler
{
    private final HttpServletRequest m_request;

    private final Authorizer         m_authorizer;

    private final WikiEngine         m_engine;

    public WebContainerCallbackHandler( WikiEngine engine, HttpServletRequest request, Authorizer authorizer )
    {
        m_engine  = engine;
        m_request = request;
        m_authorizer = authorizer;
    }

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public final void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException
    {
        for( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];
            if ( callback instanceof HttpRequestCallback )
            {
                ( (HttpRequestCallback) callback ).setRequest( m_request );
            }
            else if ( callback instanceof AuthorizerCallback )
            {
                ( (AuthorizerCallback) callback ).setAuthorizer( m_authorizer );
            }
            else if( callback instanceof WikiEngineCallback )
            {
                ( (WikiEngineCallback) callback ).setEngine( m_engine );
            }
            else
            {
                throw new UnsupportedCallbackException( callback );
            }
        }
    }

}
