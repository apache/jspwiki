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

import javax.security.auth.callback.Callback;

import org.apache.wiki.auth.Authorizer;

/**
 * Callback for requesting and supplying an Authorizer required by a
 * LoginModule. This Callback is used by LoginModules needing access to the
 * external authorizer or group manager.
 * @since 2.3
 */
public class AuthorizerCallback implements Callback
{

    private Authorizer m_authorizer;

    /**
     * Sets the authorizer object. CallbackHandler objects call this method.
     * @param authorizer the authorizer
     */
    public void setAuthorizer( Authorizer authorizer )
    {
        m_authorizer = authorizer;
    }

    /**
     * Returns the authorizer. LoginModules call this method after a
     * CallbackHandler sets the authorizer.
     * @return the authorizer
     */
    public Authorizer getAuthorizer()
    {
        return m_authorizer;
    }

}
