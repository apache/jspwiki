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
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiEngine;

/**
 * Handles logins made from within JSPWiki.
 * @see org.apache.wiki.WikiSession#getWikiSession(WikiEngine,HttpServletRequest)
 * @since 2.3
 */
public final class WebContainerCallbackHandler implements CallbackHandler
{
    private final HttpServletRequest m_request;

    private final WikiEngine         m_engine;

    /**
     *  Create a new handler.
     *  
     *  @param engine The WikiEngine
     *  @param request The request to look into
     */
    public WebContainerCallbackHandler( WikiEngine engine, HttpServletRequest request )
    {
        m_engine  = engine;
        m_request = request;
    }

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     * 
     * {@inheritDoc}
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
