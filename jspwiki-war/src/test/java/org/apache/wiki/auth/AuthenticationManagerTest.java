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
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.WikiSessionTest;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebAuthorizer;
import org.apache.wiki.auth.login.CookieAssertionLoginModule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the AuthorizationManager class.
 *
 */
public class AuthenticationManagerTest
{
    public static class DummyAuthorizer implements WebAuthorizer
    {
        private static Principal[] m_roles = new Principal[] { new Role( "ContainerRole" ), new Role( "AuthorizerRole" ),
                                                              new Role( "DummyRole" ) };

        public Principal findRole( String role )
        {
            for( Principal principal : m_roles )
            {
                if( principal.getName().equals( role ) )
                {
                    return principal;
                }
            }
            return null;
        }

        public Principal[] getRoles()
        {
            return m_roles;
        }

        public void initialize( WikiEngine engine, Properties props ) throws WikiSecurityException
        {
        }

        public boolean isUserInRole( HttpServletRequest request, Principal role )
        {
            return request != null && "ContainerRole".equals( role.getName() );
        }

        public boolean isUserInRole( WikiSession session, Principal role )
        {
            return session != null && "AuthorizerRole".equals( role.getName() );
        }
    }

    private AuthenticationManager m_auth;

    private TestEngine m_engine;

    private GroupManager m_groupMgr;

    private WikiSession m_session;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthenticationManager();
        m_groupMgr = m_engine.getGroupManager();
        m_session = WikiSessionTest.adminSession( m_engine );
    }

    /**
     * Tests a dummy WebAuthorizer that is guaranteed to return true for one
     * role for each of the two <code>isInRole</code> methods.
     *
     * @throws Exception
     */
    @Test
    public void testCustomAuthorizer() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        props.put( AuthorizationManager.PROP_AUTHORIZER, "org.apache.wiki.auth.AuthenticationManagerTest$DummyAuthorizer" );
        m_engine = new TestEngine( props );

        // Start a session without any container roles: DummyAuthorizer should ALWAYS allow AuthorizerRole
        WikiSession session = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.hasPrincipal( Role.ALL ) );
        Assertions.assertTrue( session.hasPrincipal( Role.AUTHENTICATED ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( Users.JANNE, WikiPrincipal.LOGIN_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "Janne Jalkanen", WikiPrincipal.FULL_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new Role( "AuthorizerRole") ) );
        Assertions.assertFalse( session.hasPrincipal( new Role( "ContainerRole") ) );
        Assertions.assertFalse( session.hasPrincipal( new Role( "DummyRole") ) );

        // Try again with a container-authenticated session: DummyAuthorizer should ALSO allow ContainerRole
        session = WikiSessionTest.containerAuthenticatedSession( m_engine, Users.JANNE, new Principal[0] );
        Assertions.assertTrue( session.hasPrincipal( Role.ALL ) );
        Assertions.assertTrue( session.hasPrincipal( Role.AUTHENTICATED ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( Users.JANNE, WikiPrincipal.LOGIN_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "Janne Jalkanen", WikiPrincipal.FULL_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new Role( "AuthorizerRole") ) );
        Assertions.assertTrue( session.hasPrincipal( new Role( "ContainerRole") ) );
        Assertions.assertFalse( session.hasPrincipal( new Role( "DummyRole") ) );
    }

    @Test
    public void testCustomJAASLoginModule() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        // Supply a custom LoginModule class
        props.put( "jspwiki.loginModule.class", "org.apache.wiki.auth.login.CookieAssertionLoginModule" );

        // Init the engine and verify that we initialized with a custom auth
        // login module
        WikiEngine engine = new TestEngine( props );
        AuthenticationManager authMgr = engine.getAuthenticationManager();
        Assertions.assertEquals( CookieAssertionLoginModule.class, authMgr.m_loginModuleClass );
    }

    @Test
    public void testCustomJAASLoginModuleOptions() throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        // Supply a custom LoginModule options
        props.put( "jspwiki.loginModule.options.key1", "value1" );
        props.put( "jspwiki.loginModule.options.key2", "value2" );
        props.put( "jspwiki.loginModule.options.key3", "value3" );

        // Init the engine and verify that we initialized with the correct
        // options
        WikiEngine engine = new TestEngine( props );
        AuthenticationManager authMgr = engine.getAuthenticationManager();
        Map<String, String> options = authMgr.m_loginModuleOptions;
        Assertions.assertEquals( 3, options.size() );
        Assertions.assertTrue( options.containsKey( "key1" ) );
        Assertions.assertTrue( options.containsKey( "key2" ) );
        Assertions.assertTrue( options.containsKey( "key3" ) );
        Assertions.assertEquals( "value1", options.get( "key1" ) );
        Assertions.assertEquals( "value2", options.get( "key2" ) );
        Assertions.assertEquals( "value3", options.get( "key3" ) );
    }

    @Test
    public void testIsUserPrincipal()
    {
        Assertions.assertTrue( AuthenticationManager.isUserPrincipal( new WikiPrincipal( "Foo" ) ) );
        Assertions.assertFalse( AuthenticationManager.isUserPrincipal( new GroupPrincipal( "Group1" ) ) );
        Assertions.assertFalse( AuthenticationManager.isUserPrincipal( new Role( "Role1" ) ) );
        Assertions.assertFalse( AuthenticationManager.isUserPrincipal( Role.ANONYMOUS ) );
    }

    @Test
    public void testLoginCustom() throws Exception
    {
        WikiSession session = WikiSessionTest.authenticatedSession( m_engine, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertTrue( session.hasPrincipal( Role.ALL ) );
        Assertions.assertTrue( session.hasPrincipal( Role.AUTHENTICATED ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( Users.JANNE, WikiPrincipal.LOGIN_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "Janne Jalkanen", WikiPrincipal.FULL_NAME ) ) );
    }

    @Test
    public void testLoginCustomWithGroup() throws Exception
    {
        // Flush any pre-existing groups (left over from previous Assertions.failures,
        // perhaps)
        try
        {
            m_groupMgr.removeGroup( "Test1" );
            m_groupMgr.removeGroup( "Test2" );
        }
        catch( NoSuchPrincipalException e )
        {

        }

        // Log in 'janne' and verify there are 5 principals in the subject
        // (ALL, AUTHENTICATED, login, fullname, wikiname Principals)
        WikiSession session = WikiSession.guestSession( m_engine );
        m_auth.login( session, null, Users.JANNE, Users.JANNE_PASS );
        Assertions.assertEquals( 3, session.getPrincipals().length );
        Assertions.assertEquals( 2, session.getRoles().length );
        Assertions.assertTrue( session.hasPrincipal( new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );

        // Listen for any manager group-add events
        GroupManager manager = m_engine.getGroupManager();
        SecurityEventTrap trap = new SecurityEventTrap();
        manager.addWikiEventListener( trap );

        // Create two groups; one with Janne in it, and one without
        Group groupTest1 = m_groupMgr.parseGroup( "Test1", "JanneJalkanen \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, groupTest1 );
        groupTest1 = m_groupMgr.getGroup( "Test1" );
        Principal principalTest1 = groupTest1.getPrincipal();

        Group groupTest2 = m_groupMgr.parseGroup( "Test2", "Alice \n Bob \n Charlie", true );
        m_groupMgr.setGroup( m_session, groupTest2 );
        groupTest2 = m_groupMgr.getGroup( "Test2" );
        Principal principalTest2 = groupTest2.getPrincipal();

        // We should see two security events (one for each group create)
        // We should also see a GroupPrincipal for group Test1, but not Test2
        Assertions.assertEquals( 2, trap.events().length );
        Assertions.assertTrue( session.hasPrincipal( principalTest1 ) );
        Assertions.assertFalse( session.hasPrincipal( principalTest2 ) );

        // If we remove Test1, the GroupPrincipal should disappear
        m_groupMgr.removeGroup( "Test1" );
        Assertions.assertFalse( session.hasPrincipal( principalTest1 ) );
        Assertions.assertFalse( session.hasPrincipal( principalTest2 ) );

        // Now, add 'JanneJalkanen' to Test2 group manually; we should see the
        // GroupPrincipal
        groupTest2.add( new WikiPrincipal( "JanneJalkanen" ) );
        m_groupMgr.setGroup( session, groupTest2 );
        Assertions.assertFalse( session.hasPrincipal( principalTest1 ) );
        Assertions.assertTrue( session.hasPrincipal( principalTest2 ) );

        // Remove 'JanneJalkenen' manually; the GroupPrincipal should disappear
        groupTest2.remove( new WikiPrincipal( "JanneJalkanen" ) );
        m_groupMgr.setGroup( session, groupTest2 );
        Assertions.assertFalse( session.hasPrincipal( principalTest1 ) );
        Assertions.assertFalse( session.hasPrincipal( principalTest2 ) );

        // Clean up
        m_groupMgr.removeGroup( "Test2" );
    }

}
