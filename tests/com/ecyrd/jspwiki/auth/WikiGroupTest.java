package com.ecyrd.jspwiki.auth;

import junit.framework.*;

public class WikiGroupTest
    extends TestCase
{
    WikiGroup m_group;

    public WikiGroupTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_group = new WikiGroup();
    }

    public void tearDown()
    {
    }

    public void testAdd1()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        m_group.addMember( u1 );

        assertTrue( m_group.isMember(u1) );
    }

    public void testAdd2()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        assertTrue("adding alice", m_group.addMember( u1 ) );
        assertTrue("adding bob",   m_group.addMember( u2 ) );

        assertTrue( "Alice", m_group.isMember(u1) );
        assertTrue( "Bob", m_group.isMember(u2) );
    }

    /**
     *  Check that different objects match as well.
     */
    public void testAdd3()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        UserProfile u3 = new UserProfile();
        u3.setName("Bob");

        assertTrue("adding alice", m_group.addMember( u1 ) );
        assertTrue("adding bob",   m_group.addMember( u2 ) );

        assertTrue( "Alice", m_group.isMember(u1) );
        assertTrue( "Bob", m_group.isMember(u3) );
    }

    public void testRemove()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        UserProfile u3 = new UserProfile();
        u3.setName("Bob");

        m_group.addMember( u1 );
        m_group.addMember( u2 );

        m_group.removeMember( u3 );

        assertTrue( "Alice",  m_group.isMember(u1) );
        assertFalse( "Bob",   m_group.isMember(u2) );
        assertFalse( "Bob 2", m_group.isMember(u3) );
    }

    public void testEquals1()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        m_group.addMember( u1 );
        m_group.addMember( u2 );

        WikiGroup group2 = new WikiGroup();
        UserProfile u3 = new UserProfile();
        u3.setName("Alice");

        UserProfile u4 = new UserProfile();
        u4.setName("Bob");

        group2.addMember( u3 );
        group2.addMember( u4 );

        assertTrue( m_group.equals(group2) );
    }

    public void testEquals2()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        m_group.addMember( u1 );
        m_group.addMember( u2 );

        WikiGroup group2 = new WikiGroup();
        UserProfile u3 = new UserProfile();
        u3.setName("Alice");

        UserProfile u4 = new UserProfile();
        u4.setName("Charlie");

        group2.addMember( u3 );
        group2.addMember( u4 );

        assertFalse( m_group.equals(group2) );
    }

    public void testEquals3()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        m_group.addMember( u1 );
        m_group.addMember( u2 );
        m_group.setName("Blib");

        WikiGroup group2 = new WikiGroup();
        UserProfile u3 = new UserProfile();
        u3.setName("Alice");

        UserProfile u4 = new UserProfile();
        u4.setName("Bob");

        group2.addMember( u3 );
        group2.addMember( u4 );
        group2.setName("Blib");

        assertTrue( m_group.equals(group2) );
    }

    public void testEquals4()
    {
        UserProfile u1 = new UserProfile();
        u1.setName("Alice");

        UserProfile u2 = new UserProfile();
        u2.setName("Bob");

        m_group.addMember( u1 );
        m_group.addMember( u2 );
        m_group.setName("BlibBlab");

        WikiGroup group2 = new WikiGroup();
        UserProfile u3 = new UserProfile();
        u3.setName("Alice");

        UserProfile u4 = new UserProfile();
        u4.setName("Bob");

        group2.addMember( u3 );
        group2.addMember( u4 );
        group2.setName("Blib");

        assertFalse( m_group.equals(group2) );
    }

    public static Test suite()
    {
        return new TestSuite( WikiGroupTest.class );
    }

}
