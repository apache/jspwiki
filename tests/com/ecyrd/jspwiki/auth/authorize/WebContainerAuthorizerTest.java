package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.jdom.Document;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;

public class WebContainerAuthorizerTest extends TestCase
{
    WikiEngine m_engine;
    WebContainerAuthorizer m_authorizer;
    Document   m_webxml;

    public WebContainerAuthorizerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        m_authorizer = new WebContainerAuthorizer();
        m_authorizer.initialize( m_engine, props );
        m_webxml = m_authorizer.getWebXml();
        if ( m_webxml == null )
        {
            throw new Exception("Could not load web.xml");
        }
    }

    public void testConstraints() throws Exception
    {
        assertTrue( m_authorizer.isConstrained( m_webxml, "/Delete.jsp", new Role( "Admin" ) ) );
        assertTrue( m_authorizer.isConstrained( m_webxml, "/Login.jsp", Role.AUTHENTICATED ) );
        assertFalse( m_authorizer.isConstrained( m_webxml, "/UserPreferences.jsp", Role.AUTHENTICATED ) );
    }
    
    public void testGetRoles()
    {
        // We should find 2 roles: AUTHENTICATED and ADMIN
        Principal[] roles = m_authorizer.getRoles();
        assertEquals( 2, roles.length );
        boolean found = false;
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( Role.AUTHENTICATED ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find AUTHENTICATED", found );
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( new Role( "Admin" ) ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find ADMIN", found );
    }
    
    public void testRoles() throws Exception
    {
        Role[] roles = m_authorizer.getRoles( m_webxml );
        boolean found = false;
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( Role.AUTHENTICATED ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find AUTHENTICATED", found );
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( new Role( "Admin" ) ) )
            {
                found = true;
            }
        }
        assertTrue( "Didn't find ADMIN", found );
    }
    
    public void testIsContainerAuthorized()
    {
        assertTrue( m_authorizer.isContainerAuthorized() );
    }

    public static Test suite()
    {
        return new TestSuite( WebContainerAuthorizerTest.class );
    }

}
