package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Properties;

import javax.security.auth.Subject;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.providers.ProviderException;

public class DefaultGroupManagerTest extends TestCase
{
    TestEngine   m_engine;

    GroupManager m_manager;

    public DefaultGroupManagerTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );

        m_engine = new TestEngine( props );
        m_manager = new DefaultGroupManager();
        m_manager.initialize( m_engine, props );

        String text1 = "Foobar.\n\n[{SET members=Alice, Bob, Charlie}]\n\nBlood.";
        m_engine.saveText( "GroupTest", text1 );

        String text2 = "[{SET members=Bob}]";
        m_engine.saveText( "GroupTest2", text2 );
        
        String text3 = "[{SET members=Arnold}]";
        m_engine.saveText( "BadGroupTest", text3 );
    }

    public void tearDown()
    {
        try
        {
            m_engine.deletePage( "GroupTest" );
            m_engine.deletePage( "GroupTest2" );
            m_engine.deletePage( "BadGroupTest" );
        }
        catch ( ProviderException e )
        {
        }
   }

    public void testGroupFormation() throws Exception
    {
        Principal p = new WikiPrincipal( "Alice" );
        Subject s = new Subject();
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistant" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Bob" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "Test" ) ) );
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistant" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Charlie" );
        s.getPrincipals().add(p);
        assertTrue( m_manager.isUserInRole( null, s, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistant" ) ) );

        s = new Subject();
        p = new WikiPrincipal( "Biff" );
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "Test" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "Test2" ) ) );
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "NonExistant" ) ) );
    }

    public void testBadGroupFormation() throws Exception
    {
        // BadGroup doesn't start with "Group", so there should be no group created for it
        // (Groups must start with the word "Group")
        Principal p = new WikiPrincipal( "Arnold" );
        Subject s = new Subject();
        s.getPrincipals().add(p);
        assertFalse( m_manager.isUserInRole( null, s, new DefaultGroup( "BadGroup" ) ) );
    }

    public static Test suite()
    {
        return new TestSuite( DefaultGroupManagerTest.class );
    }

}