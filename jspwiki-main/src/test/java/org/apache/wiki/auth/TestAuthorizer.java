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
package org.apache.wiki.auth;

import java.security.Principal;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebAuthorizer;

/**
 * A very fast authorizer that does almost nothing. The WebContainerAuthorizer module
 * is very slow, as it parses the web.xml each time, so we use this for most of
 * the different tests.
 * @since 2.3
 */
public class TestAuthorizer implements WebAuthorizer
{
    private Role[] m_roles = new Role[]{ 
            new Role( "Admin" ), 
            Role.AUTHENTICATED,
            new Role( "IT" ),
            new Role( "Finance" ),
            new Role( "Engineering" ) };
    
    public TestAuthorizer()
    {
        super();
    }

    public Principal findRole( String role )
    {
        return null;
    }

    public void initialize( WikiEngine engine, Properties props )
    {
    }

    /**
     * Returns an array of Principal objects containing five elements:
     * Role "Admin", Role.AUTHENTICATED, Role "IT", Role "Finance" and 
     * Role "Engineering."
     */
    public Principal[] getRoles()
    {
        return m_roles;
    }
    
    /**
     * Returns <code>true</code> if the WikiSession's Subject contains 
     * a particular role principal.
     */
    public boolean isUserInRole( WikiSession session, Principal role )
    {
        if ( session == null || role == null )
        {
            return false;
        }
        
        return session.hasPrincipal( role );
    }

    /**
     * Returns <code>true</code> if the HTTP request contains 
     * a particular role principal. Delegates to
     * {@link javax.servlet.http.HttpServletRequest#isUserInRole(String)}.
     * @see org.apache.wiki.auth.authorize.WebAuthorizer#isUserInRole(javax.servlet.http.HttpServletRequest, java.security.Principal)
     */
    public boolean isUserInRole( HttpServletRequest request, Principal role )
    {
        return request.isUserInRole( role.getName() );
    }

}
