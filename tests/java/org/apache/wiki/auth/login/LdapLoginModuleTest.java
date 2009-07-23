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

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import junit.framework.TestCase;

import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;

/**
 * @author Andrew R. Jaquith
 */
public class LdapLoginModuleTest extends TestCase
{
    static final Map<String,String> m_options;
    
    static {
        m_options = new HashMap<String, String>();
        m_options.put( LdapLoginModule.OPTIONS_CONNECTION_URL, "ldap://127.0.0.1:4890" );
        m_options.put( LdapLoginModule.OPTIONS_USER_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
    }

    public final void testLoginNonExistentUser() throws Exception
    {
        // Log in with a user that isn't in the database
        Subject subject = new Subject();
        CallbackHandler handler = new WikiCallbackHandler( null, null, "NonExistentUser", "password" );
        LoginModule module = new LdapLoginModule();
        module.initialize( subject, handler, new HashMap<String, Object>(), m_options );
        try
        {
            module.login();
        }
        catch ( LoginException e )
        {
            // Could not log in; this is what we expected
        }
        
        // Verify no principals injected into Subject
        Set<Principal> principals = subject.getPrincipals();
        assertEquals( 0, principals.size() );
    }

    public final void testLogin() throws Exception
    {
        // Login with a user that IS in the database
        Subject subject = new Subject();
        CallbackHandler handler = new WikiCallbackHandler( null, null, "janne", "myP@5sw0rd" );
        LoginModule module = new LdapLoginModule();
        module.initialize( subject, handler, new HashMap<String, Object>(), m_options );
        module.login();
        module.commit();
        
        // Successful login will inject the usual LoginPrincipal
        Set<Principal> principals = subject.getPrincipals();
        assertEquals( 3, principals.size() );
        assertTrue( principals.contains( new WikiPrincipal( "janne", WikiPrincipal.LOGIN_NAME ) ) );
        
        // PLUS, in this case only, principals for Wiki Name and Full Name
        assertTrue( principals.contains( new WikiPrincipal( "Janne Jalkanen", WikiPrincipal.FULL_NAME ) ) );
        assertTrue( principals.contains( new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        
        // AuthenticationManager, NOT the LoginModule, adds the Role principals
        assertFalse( principals.contains( Role.AUTHENTICATED ) );
        assertFalse( principals.contains( Role.ALL ) );
    }

    public final void testLogout() throws Exception
    {
        Subject subject = new Subject();

        // Log in with a valid user
        CallbackHandler handler = new WikiCallbackHandler( null, null, "user", "password" );
        LoginModule module = new LdapLoginModule();
        module.initialize( subject, handler, new HashMap<String, Object>(), m_options );
        module.login();
        module.commit();
        Set<Principal> principals = subject.getPrincipals();
        assertEquals( 3, principals.size() );
        assertTrue( principals.contains( new WikiPrincipal( "user", WikiPrincipal.LOGIN_NAME ) ) );
        assertTrue( principals.contains( new WikiPrincipal( "Test User", WikiPrincipal.FULL_NAME ) ) );
        assertTrue( principals.contains( new WikiPrincipal( "TestUser", WikiPrincipal.WIKI_NAME ) ) );
        assertFalse( principals.contains( Role.AUTHENTICATED ) );
        assertFalse( principals.contains( Role.ALL ) );
        
        // Log out and verify no principals still in subject
        module.logout();
        assertEquals( 0, principals.size() );
    }
}
