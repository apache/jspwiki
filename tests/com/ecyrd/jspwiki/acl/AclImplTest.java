package com.ecyrd.jspwiki.acl;

import junit.framework.*;
import java.util.*;

import java.security.acl.*;

import com.ecyrd.jspwiki.auth.permissions.*;
import com.ecyrd.jspwiki.auth.*;

public class AclImplTest
    extends TestCase
{
    AclImpl m_acl;
    AclImpl m_aclGroup;

    public AclImplTest( String s )
    {
        super( s );
    }

    /**
     *  We setup the following rules:
     *  Alice = may view
     *  Bob   = may view, may edit
     *  Charlie = may view, may NOT edit
     *
     *  groupAcl:
     *  FooGroup = Alice, Bob - may edit
     *  BarGroup = Bob, Charlie - may NOT edit
     */

    public void setUp()
    {
        m_acl = new AclImpl();
        m_aclGroup = new AclImpl();

        //  User 1

        UserProfile u1 = new UserProfile();
        u1.setName( "Alice" );

        UserProfile u2 = new UserProfile();
        u2.setName( "Bob" );
        
        UserProfile u3 = new UserProfile();
        u3.setName( "Charlie" );

        //  ALLOW VIEW

        AclEntry ae = new AclEntryImpl();
        ae.addPermission( new ViewPermission() );
        ae.setPrincipal( u1 );

        //  DENY EDIT

        AclEntry ae2 = new AclEntryImpl();
        ae2.addPermission( new EditPermission() );
        ae2.setNegativePermissions();
        ae2.setPrincipal( u3 );

        AclEntry ae2b = new AclEntryImpl();
        ae2b.addPermission( new ViewPermission() );
        ae2b.setPrincipal( u3 );

        //  ALLOW VIEW, EDIT

        AclEntry ae3 = new AclEntryImpl();
        ae3.addPermission( new ViewPermission() );
        ae3.addPermission( new EditPermission() );
        ae3.setPrincipal( u2 );


        m_acl.addEntry( null, ae );
        m_acl.addEntry( null, ae2 );
        m_acl.addEntry( null, ae2b );
        m_acl.addEntry( null, ae3 );

        //  Groups

        WikiGroup group1 = new WikiGroup();
        group1.addMember( u1 );
        group1.addMember( u2 );
        group1.setName( "FooGroup" );

        WikiGroup group2 = new WikiGroup();
        group2.addMember( u2 );
        group2.addMember( u3 );
        group2.setName( "BarGroup" );
       
        AclEntry ag1 = new AclEntryImpl();
        ag1.addPermission( new EditPermission() );
        ag1.setPrincipal( group1 );

        AclEntry ag2 = new AclEntryImpl();
        ag2.addPermission( new EditPermission() );
        ag2.setNegativePermissions();
        ag2.setPrincipal( group2 );

        m_aclGroup.addEntry( null, ag1 );  // allow edit FooGroup
        m_aclGroup.addEntry( null, ag2 );  // deny  edit BarGroup
        m_aclGroup.addEntry( null, ae );   // allow view Alice
        m_aclGroup.addEntry( null, ae2 );  // deny  edit Charlie
    }

    public void tearDown()
    {
    }

    public void testAlice()
    {
        UserProfile wup = new UserProfile();
        wup.setName("Alice");

        assertTrue( "view", m_acl.checkPermission( wup, new ViewPermission() ) );
        assertFalse( "edit", m_acl.checkPermission( wup, new EditPermission() ) );
        assertFalse( "comment", m_acl.checkPermission( wup, new CommentPermission() ) );

        assertEquals( "edit none", AclImpl.NONE, 
                      m_acl.findPermission( wup, new EditPermission() ) );

        assertEquals( "comment none", AclImpl.NONE, 
                      m_acl.findPermission( wup, new CommentPermission() ) );


        assertEquals( "view allow", AclImpl.ALLOW, 
                      m_acl.findPermission( wup, new ViewPermission() ) );
    }

    public void testBob()
    {
        UserProfile wup = new UserProfile();
        wup.setName("Bob");

        assertTrue( "view", m_acl.checkPermission( wup, new ViewPermission() ) );
        assertTrue( "edit", m_acl.checkPermission( wup, new EditPermission() ) );
        assertTrue( "comment", m_acl.checkPermission( wup, new CommentPermission() ) );

        assertEquals( "view allow", AclImpl.ALLOW, 
                      m_acl.findPermission( wup, new ViewPermission() ) );
        assertEquals( "edit allow", AclImpl.ALLOW, 
                      m_acl.findPermission( wup, new EditPermission() ) );
        assertEquals( "comment allow", AclImpl.ALLOW,
                      m_acl.findPermission( wup, new CommentPermission() ) );
    }

    public void testCharlie()
    {
        UserProfile wup = new UserProfile();
        wup.setName("Charlie");

        assertTrue( "view", m_acl.checkPermission( wup, new ViewPermission() ) );
        assertFalse( "edit", m_acl.checkPermission( wup, new EditPermission() ) );

        assertEquals( "view allow", AclImpl.ALLOW, 
                      m_acl.findPermission( wup, new ViewPermission() ) );

        assertEquals( "edit deny", AclImpl.DENY, 
                      m_acl.findPermission( wup, new EditPermission() ) );
    }

    public void testFooGroup()
    {
        UserProfile wup = new UserProfile();
        wup.setName( "Alice" );

        assertEquals( "view allow Alice", AclImpl.ALLOW,
                      m_aclGroup.findPermission( wup, new ViewPermission() ) );

        assertEquals( "edit allow Alice", AclImpl.ALLOW,
                      m_aclGroup.findPermission( wup, new EditPermission() ) );

        wup.setName( "Bob" );

        assertEquals( "deny edit Bob", AclImpl.DENY,
                      m_aclGroup.findPermission( wup, new EditPermission() ) );
        
        assertEquals( "allow view Bob", AclImpl.NONE,
                      m_aclGroup.findPermission( wup, new ViewPermission() ) );

        wup.setName( "Charlie" );

        assertEquals( "deny edit Charlie", AclImpl.DENY,
                      m_aclGroup.findPermission( wup, new EditPermission() ) );
        
        assertEquals( "Default view Charlie", AclImpl.NONE,
                      m_aclGroup.findPermission( wup, new ViewPermission() ) );        

    }

    public void testAllGroup()
        throws Exception
    {
        UserProfile wup = new UserProfile();
        wup.setName("Alice");

        WikiGroup group1 = new AllGroup();
        group1.setName( "All" );

        AclEntry ae = new AclEntryImpl();
        ae.setPrincipal( group1 );
        ae.addPermission( new ViewPermission() );
        ae.addPermission( new EditPermission() );

        AccessControlList acl = new AclImpl();
        acl.addEntry( null, ae );

        assertEquals( "no view!", AclImpl.ALLOW, 
                      acl.findPermission( wup, new ViewPermission() ) );
        assertEquals( "no edit!", AclImpl.ALLOW, 
                      acl.findPermission( wup, new EditPermission() ) );
    }

    public static Test suite()
    {
        return new TestSuite( AclImplTest.class );
    }
}
