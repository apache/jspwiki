package com.ecyrd.jspwiki.auth;

import java.security.Principal;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.authorize.Role;

/**
 * Tests the AuthorizationManager class.
 * @author Janne Jalkanen
 */
public class AuthenticationManagerTest extends TestCase
{
    private AuthenticationManager m_auth;

    private TestEngine            m_engine;
 
    public AuthenticationManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine( props );
        m_auth = m_engine.getAuthenticationManager();
    }
    
    public void testForAdmin()
    {
        Principal[] ids;
        Subject subject;
        WikiSession session;
        
        // Test with generic (non-admin) user name
        ids = new Principal[] { new WikiPrincipal( "Fred" ), new WikiPrincipal( "Wilma" ) };
        session = buildSession( ids );
        m_auth.checkForAdmin( session );
        subject = session.getSubject();
        assertFalse( subject.getPrincipals().contains( Role.ADMIN ));
        
        // Test with admin name
        ids = new Principal[] { new WikiPrincipal( "admin" ), new WikiPrincipal( "Wilma" ) };
        session = buildSession( ids );
        m_auth.checkForAdmin( session );
        subject = session.getSubject();
        assertTrue( subject.getPrincipals().contains( Role.ADMIN ));
    }
    
    private WikiSession buildSession( Principal[] ids )
    {
        WikiSession session = WikiSession.guestSession();
        Set principals = session.getSubject().getPrincipals();
        principals.clear();
        for( int i = 0; i < ids.length; i++ )
        {
            principals.add( ids[i] );
        }
        return session;
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("Authentication Manager test");
        suite.addTestSuite( AuthenticationManagerTest.class );
        return suite;
    }
}