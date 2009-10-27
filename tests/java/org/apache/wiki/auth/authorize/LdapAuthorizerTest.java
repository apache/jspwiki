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
package org.apache.wiki.auth.authorize;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import org.apache.wiki.JSPWikiTestBase;
import org.apache.wiki.NotExecutableException;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.*;
import org.apache.wiki.auth.login.UserDatabaseLoginModule;
import org.apache.wiki.auth.user.LdapUserDatabase;
import org.freshcookies.security.Keychain;

/**
 */
public class LdapAuthorizerTest extends JSPWikiTestBase
{
    private TestEngine m_engine;

    private static final String LDAP_HOST = "127.0.0.1";
    private static final int    LDAP_PORT = 4890;
    
    protected void setUp() throws Exception
    {
        //
        //  First check if the LDAP server exists.
        //
        
        try
        {
            Socket socket = new Socket( LDAP_HOST, LDAP_PORT );
            socket.connect( new InetSocketAddress(0) );
            socket.close();
        }
        catch( ConnectException e )
        {
            // OK, so there is no LDAP server existing.
            throw new NotExecutableException();
        }
        
        // Create the TestEngine properties
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        // Set the LoginModule options
        props.put( UserManager.PROP_READ_ONLY_PROFILES, "true" );
        props.put( AuthenticationManager.PROP_LOGIN_MODULE, UserDatabaseLoginModule.class.getCanonicalName() );
        props.put( LdapConfig.PROPERTY_CONNECTION_URL, "ldap://"+LDAP_HOST+":"+LDAP_PORT );
        props.put( LdapConfig.PROPERTY_LOGIN_ID_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
        props.put( LdapConfig.PROPERTY_USER_BASE, "dc=jspwiki,dc=org" );
        props.put( LdapConfig.PROPERTY_USER_FILTER, "(&(objectClass=inetOrgPerson)(uid={0}))" );
        props.put( LdapConfig.PROPERTY_AUTHENTICATION, "simple" );
        props.put( LdapConfig.PROPERTY_SSL, "false" );

        // Set the UserDatabase properties
        props.put( UserManager.PROP_DATABASE, LdapUserDatabase.class.getCanonicalName() );
        
        // Set the Authorizer properties
        props.put( AuthorizationManager.PROP_AUTHORIZER, LdapAuthorizer.class.getCanonicalName() );
        props.put( LdapConfig.PROPERTY_ROLE_BASE, "ou=roles,dc=jspwiki,dc=org" );
        props.put( LdapConfig.PROPERTY_IS_IN_ROLE_FILTER, "(&(&(objectClass=groupOfUniqueNames)(cn={0}))(uniqueMember={1}))" );
        props.put( LdapConfig.PROPERTY_BIND_USER, "Fred" );

        m_engine = new TestEngine( props );
        assertEquals( LdapUserDatabase.class, m_engine.getUserManager().getUserDatabase().getClass() );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }

    public void testGetRoles() throws Exception
    {
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();

        // LDAP should return just 2 roles, Admin and Role1
        Principal[] roles = authorizer.getRoles();
        assertEquals( 2, roles.length );
        Role admin = new Role( "Admin" );
        Role role1 = new Role( "Role1" );
        assertTrue( roles[0].equals( admin ) || roles[1].equals( admin ) );
        assertTrue( roles[0].equals( role1 ) || roles[1].equals( role1 ) );
    }

    public void testFindRole() throws Exception
    {
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        
        // We should be able to find roles Admin and Role1
        assertEquals( new Role("Admin"), authorizer.findRole( "Admin" ) );
        assertEquals( new Role("Role1"), authorizer.findRole( "Role1" ) );
        
        // We should not be able to find role Authenticated
        assertNull( null, authorizer.findRole( "Authenticated" ) );
    }
    
    public void testFindRoles() throws Exception
    {
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        Role[] roles;
        
        // Janne does not belong to any roles
        WikiSession session = m_engine.janneSession();
        roles = authorizer.findRoles( session );
        assertEquals( 0, roles.length );
        
        // The Admin belongs to just the Admin role
        session = m_engine.adminSession();
        roles = authorizer.findRoles( session );
        assertEquals( 1, roles.length );
        assertEquals( new Role("Admin"), roles[0] );
    }

    public void testIsUserInRole() throws Exception
    {
        assertTrue( m_engine.getUserManager().isReadOnly() );
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        Role admin = new Role( "Admin" );
        Role role1 = new Role( "Role1" );
        
        // Janne does not belong to any roles
        WikiSession session = m_engine.janneSession();
        assertFalse( authorizer.isUserInRole( session, admin ) );
        assertFalse( authorizer.isUserInRole( session, role1 ) );
        
        // The Admin belongs to just the Admin role
        session = m_engine.adminSession();
        assertTrue( authorizer.isUserInRole( session, admin ) );
        assertFalse( authorizer.isUserInRole( session, role1 ) );
    }

    public final void testLoginNonExistentUser() throws Exception
    {
        // Log in with a user that isn't in the database
        WikiSession session = m_engine.guestSession();
        AuthenticationManager mgr = m_engine.getAuthenticationManager();
        try
        {
            mgr.login( session, null, "NonExistentUser", "password" );
            // Should never get here
            fail( "Allowed login to non-existent user!" );
        }
        catch ( LoginException e )
        {
            // Good! This is what we expect
        }
    }

    public final void testLogin() throws Exception
    {
        // Login with a user that IS in the database
        WikiSession session = m_engine.guestSession();
        AuthenticationManager mgr = m_engine.getAuthenticationManager();
        mgr.login( session, null, "janne", "myP@5sw0rd" );
        
        // Successful login will inject the usual LoginPrincipal
        Principal[] principals = session.getPrincipals();
        assertEquals( 3, principals.length );
        assertTrue( contains( principals, new WikiPrincipal( "janne", WikiPrincipal.LOGIN_NAME ) ) );
        
        // PLUS, in this case only, principals for Wiki Name and Full Name
        assertTrue( contains( principals, new WikiPrincipal( "Janne Jalkanen", WikiPrincipal.FULL_NAME ) ) );
        assertTrue( contains( principals, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        
        // AuthenticationManager adds the Role principals
        principals = session.getRoles();
        assertEquals( 2, principals.length );
        assertTrue( contains( principals, Role.AUTHENTICATED ) );
        assertTrue( contains( principals, Role.ALL ) );
    }

    public final void testLoginFullname() throws Exception
    {
        // Login with a user that has both a surname and given name
        WikiSession session = m_engine.guestSession();
        AuthenticationManager mgr = m_engine.getAuthenticationManager();
        mgr.login( session, null, "Fred", "password" );
        
        // Successful login will inject the usual LoginPrincipal
        Principal[] principals = session.getPrincipals();
        assertEquals( 3, principals.length );
        assertTrue( contains( principals, new WikiPrincipal( "Fred", WikiPrincipal.LOGIN_NAME ) ) );
        
        // PLUS, in this case only, principals for Wiki Name and Full Name
        // NOTE that because Fred has a first name + last name, this is preferred
        // to the common name of "Flintstone, Fred"
        assertTrue( contains( principals, new WikiPrincipal( "Fred Flintstone", WikiPrincipal.FULL_NAME ) ) );
        assertTrue( contains( principals, new WikiPrincipal( "FredFlintstone", WikiPrincipal.WIKI_NAME ) ) );
    }
    
    private boolean contains( Principal[] principals, Principal searchPrincipal )
    {
        for ( Principal principal : principals )
        {
            if ( principal.equals( searchPrincipal ) )
            {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Script for testing Active Directory integration.
     * @param args
     * @throws Exception
     */
    @SuppressWarnings("deprecation")
    public static final void main( String... args ) throws Exception
    {
        // Create the TestEngine properties
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        // Set the basic connection options
        Map<String,String> options = new HashMap<String,String>();
        options.put( LdapConfig.PROPERTY_CONFIG, LdapConfig.Default.ACTIVE_DIRECTORY.toString() );
        options.put( LdapConfig.PROPERTY_CONNECTION_URL, "ldap://camb-dc01.forrester.loc:389" );
        options.put( LdapConfig.PROPERTY_USER_BASE, "OU=Office Locations,OU=forrester,DC=forrester,DC=loc" );
        options.put( LdapConfig.PROPERTY_AUTHENTICATION, "DIGEST-MD5" );
        options.put( LdapConfig.PROPERTY_SSL, "false" );
        
        // Set the Authorizer properties
        props.put( AuthorizationManager.PROP_AUTHORIZER, LdapAuthorizer.class.getCanonicalName() );
        props.put( LdapConfig.PROPERTY_ROLE_BASE, "OU=Distribution Lists,OU=.Global,OU=forrester,DC=forrester,DC=loc" );
        props.put( LdapConfig.PROPERTY_BIND_USER, "ajaquith" );
        props.put( AuthenticationManager.PROP_KEYCHAIN_PATH, "/Users/arj/workspace/ldap/forrester" );
        props.put( AuthenticationManager.PROP_KEYCHAIN_PASSWORD, "keychain-password" );
        
        // Set the UserDatabase properties
        props.put( UserManager.PROP_READ_ONLY_PROFILES, "true" );
        
        TestEngine engine = new TestEngine( props );
        
        //
        // 1. Obtain credentials
        //
        Keychain keychain = new Keychain();
        InputStream stream = new FileInputStream( new File( "/Users/arj/workspace/ldap/forrester") );
        keychain.load( stream, "keychain-password".toCharArray() );
        Keychain.Password password = (Keychain.Password)keychain.getEntry( LdapConfig.KEYCHAIN_LDAP_BIND_PASSWORD );
        
        //
        // 2. Test the LdapAuthorizer
        //
        assertTrue( engine.getUserManager().isReadOnly() );
        Authorizer authorizer = engine.getAuthorizationManager().getAuthorizer();

        Principal[] roles = authorizer.getRoles();
        assertNotSame( 0, roles.length );

        // User does not belong to any roles
        WikiSession session = engine.guestSession();
        engine.getAuthenticationManager().login( session, "ajaquith", password.getPassword() );
        Role admin = new Role( "Admin" );
        Role research = new Role( "Research - IT - Security & Risk Management" );
        assertFalse( authorizer.isUserInRole( session, admin ) );
        assertTrue( authorizer.isUserInRole( session, research ) );
    }
}
