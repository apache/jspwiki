package com.ecyrd.jspwiki.auth.modules;

import junit.framework.*;
import java.util.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.*;

public class WikiDatabaseTest
    extends TestCase
{
    TestEngine m_engine;

    public WikiDatabaseTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        // props.setProperty("jspwiki.userDatabase", "WikiDatabase");

        m_engine = new TestEngine(props);

        String text1 = "Foobar.\n\n[{MEMBERS Alice, Bob, Charlie}]\n\nBlood.";
        String text2 = "[{MEMBERS Bob}]";

        m_engine.saveText( "TestGroup", text1 );
        m_engine.saveText( "TestGroup2", text2 );
    }

    public void tearDown()
    {
        m_engine.deletePage( "TestGroup" );
        m_engine.deletePage( "TestGroup2" );
    }

    private boolean containsGroup( List l, String name )
    {
        for( Iterator i = l.iterator(); i.hasNext(); )
        {
            WikiGroup group = (WikiGroup) i.next();

            if( group.getName().equals( name ) )
                return true;
        }

        return false;
    }

    public void testGroupFormation()
        throws Exception
    {
        UserManager mgr = m_engine.getUserManager();

        UserProfile p = new UserProfile();
        p.setName( "Alice" );
        List l = mgr.getGroupsForPrincipal( p );

        assertEquals("Alice has too many groups", 2, l.size());
        assertTrue("Alice is in the wrong group (0)", containsGroup( l, "TestGroup" ) );
        assertTrue("Alice is in the wrong group (1)", containsGroup( l, "Guest" ) );

        p.setName("Bob");
        l = mgr.getGroupsForPrincipal( p );

        assertEquals("Bob has too many groups", 3, l.size());
        assertTrue("Bob is in the wrong group (0)", containsGroup( l, "TestGroup" ) );
        assertTrue("Bob is in the wrong group (1)", containsGroup( l, "TestGroup2" ) );
        assertTrue("Bob is in the wrong group (2)", containsGroup( l, "Guest" ) );

        p.setName("David");
        l = mgr.getGroupsForPrincipal( p );

        assertEquals("David has too many groups", 1, l.size());
    }

    public static Test suite()
    {
        return new TestSuite( WikiDatabaseTest.class );
    }

}
