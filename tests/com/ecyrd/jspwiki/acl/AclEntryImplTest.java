package com.ecyrd.jspwiki.acl;

import junit.framework.*;
import java.util.*;

import com.ecyrd.jspwiki.auth.permissions.*;

public class AclEntryImplTest
    extends TestCase
{
    AclEntryImpl m_ae;

    public AclEntryImplTest( String s )
    {
        super( s );
    }

    public void setUp()
    {
        m_ae = new AclEntryImpl();
    }

    public void tearDown()
    {
    }

    public void testAddPermission()
    {
        m_ae.addPermission( new ViewPermission() );

        assertTrue( "no permission", m_ae.checkPermission( new ViewPermission() ) );
        assertFalse( "permission found", m_ae.checkPermission( new EditPermission() ) );
    }


    public void testAddPermission2()
    {
        m_ae.addPermission( new ViewPermission() );
        m_ae.addPermission( new EditPermission() );

        assertTrue( "no editpermission", m_ae.checkPermission( new EditPermission() ) );
        assertTrue( "no viewpermission", m_ae.checkPermission( new ViewPermission() ) );
    }

    public void testRemPermission()
    {
        m_ae.addPermission( new ViewPermission() );
        m_ae.addPermission( new EditPermission() );

        assertTrue( "no editpermission", m_ae.checkPermission( new EditPermission() ) );
        assertTrue( "no viewpermission", m_ae.checkPermission( new ViewPermission() ) );

        m_ae.removePermission( new EditPermission() );

        assertFalse( "editperm found", m_ae.checkPermission( new EditPermission() ) );
        assertTrue( "viewperm disappeared", m_ae.checkPermission( new ViewPermission() ) );
    }

    public void testDefaults()
    {
        assertFalse( "negative", m_ae.isNegative() );
        assertFalse( "elements", m_ae.permissions().hasMoreElements() );
        assertNull( "principal", m_ae.getPrincipal() );
    }

    public void testNegative()
    {
        m_ae.setNegativePermissions();

        assertTrue( "not negative", m_ae.isNegative() );
    }

    public static Test suite()
    {
        return new TestSuite( AclEntryImplTest.class );
    }
}
