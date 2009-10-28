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
package org.apache.wiki.auth.user;

import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.Principal;
import java.util.Properties;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.JSPWikiTestBase;
import org.apache.wiki.NotExecutableException;
import org.apache.wiki.TestEngine;
import org.apache.wiki.auth.*;

/**
 */
public class LdapUserDatabaseTest extends JSPWikiTestBase
{
    private static final String LDAP_HOST = "127.0.0.1";
    private static final int    LDAP_PORT = 4890;
    
    private LdapUserDatabase m_db;

    private TestEngine m_engine = null;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
        
        //
        //  First check if the LDAP server exists.
        //
        
        try
        {
            Socket socket = new Socket( LDAP_HOST, LDAP_PORT );
            if ( !socket.isConnected() )
            {
                socket.connect( new InetSocketAddress(0) );
            }
            socket.close();
        }
        catch( ConnectException e )
        {
            // OK, so there is no LDAP server existing.
            throw new NotExecutableException();
        }

        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( UserManager.PROP_DATABASE, "org.apache.wiki.auth.user.LdapUserDatabase" );
        props.put( LdapConfig.PROPERTY_CONNECTION_URL, "ldap://"+LDAP_HOST+":"+LDAP_PORT );
        props.put( LdapConfig.PROPERTY_USER_BASE, "ou=people,dc=jspwiki,dc=org" );
        props.put( LdapConfig.PROPERTY_AUTHENTICATION, "simple" );
        props.put( LdapConfig.PROPERTY_LOGIN_ID_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
        m_engine = new TestEngine( props );
        m_db = new LdapUserDatabase();
        m_db.initialize( m_engine, props );
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }

    public void testFindByEmail() throws Exception
    {
        UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
        assertEquals( "uid=janne,ou=people,dc=jspwiki,dc=org", profile.getUid() );
        assertEquals( "janne", profile.getLoginName() );
        assertEquals( "Janne Jalkanen", profile.getFullname() );
        assertEquals( "JanneJalkanen", profile.getWikiName() );
        assertEquals( "janne@ecyrd.com", profile.getEmail() );
        
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            fail( "Found nonexistent user!" );
        }
        catch( NoSuchPrincipalException e )
        {
        }
    }

    public void testFindByFullName() throws Exception
    {
        UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
        assertEquals( "uid=janne,ou=people,dc=jspwiki,dc=org", profile.getUid() );
        assertEquals( "janne", profile.getLoginName() );
        assertEquals( "Janne Jalkanen", profile.getFullname() );
        assertEquals( "JanneJalkanen", profile.getWikiName() );
        assertEquals( "janne@ecyrd.com", profile.getEmail() );

        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            fail( "Found nonexistent user!" );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByUid() throws Exception
    {
        UserProfile profile = m_db.findByUid( "uid=janne,ou=people,dc=jspwiki,dc=org" );
        assertEquals( "uid=janne,ou=people,dc=jspwiki,dc=org", profile.getUid() );
        assertEquals( "janne", profile.getLoginName() );
        assertEquals( "Janne Jalkanen", profile.getFullname() );
        assertEquals( "JanneJalkanen", profile.getWikiName() );
        assertEquals( "janne@ecyrd.com", profile.getEmail() );
        
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            fail( "Found nonexistent user!" );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByWikiName() throws Exception
    {
        UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
        assertEquals( "uid=janne,ou=people,dc=jspwiki,dc=org", profile.getUid() );
        assertEquals( "janne", profile.getLoginName() );
        assertEquals( "Janne Jalkanen", profile.getFullname() );
        assertEquals( "JanneJalkanen", profile.getWikiName() );
        assertEquals( "janne@ecyrd.com", profile.getEmail() );

        try
        {
            m_db.findByEmail( "foo" );
            // We should never get here
            fail( "Found nonexistent user!" );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByLoginName() throws Exception
    {
        UserProfile profile = m_db.findByLoginName( "janne" );
        assertEquals( "uid=janne,ou=people,dc=jspwiki,dc=org", profile.getUid() );
        assertEquals( "janne", profile.getLoginName() );
        assertEquals( "Janne Jalkanen", profile.getFullname() );
        assertEquals( "JanneJalkanen", profile.getWikiName() );
        assertEquals( "janne@ecyrd.com", profile.getEmail() );
        try
        {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            fail( "Found nonexistent user!" );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testGetWikiNames() throws WikiSecurityException
    {
        // There are 8 test users in the database
        Principal[] p = m_db.getWikiNames();
        assertEquals( 8, p.length );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "JanneJalkanen", WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "TestUser", WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "Administrator", WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.ALICE, WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BOB, WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.CHARLIE, WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( "FredFlintstone", WikiPrincipal.WIKI_NAME ) ) );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal( Users.BIFF, WikiPrincipal.WIKI_NAME ) ) );
    }

    public void testValidatePassword()
    {
        assertFalse( m_db.validatePassword( "janne", "test" ) );
        assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        assertTrue( m_db.validatePassword( "user", "password" ) );
    }

}
