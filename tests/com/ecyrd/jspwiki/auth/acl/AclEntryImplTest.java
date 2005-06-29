package com.ecyrd.jspwiki.auth.acl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.auth.acl.AclEntryImpl;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

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
        m_ae.addPermission( new PagePermission( "view" ) );

        assertTrue( "no permission", m_ae.checkPermission( new PagePermission( "view" ) ) );
        assertFalse( "permission found", m_ae.checkPermission( new PagePermission( "edit" ) ) );
    }


    public void testAddPermission2()
    {
        m_ae.addPermission( new PagePermission( "view" ) );
        m_ae.addPermission( new PagePermission( "edit" ) );

        assertTrue( "no editpermission", m_ae.checkPermission( new PagePermission( "edit" ) ) );
        assertTrue( "no viewpermission", m_ae.checkPermission( new PagePermission( "view" ) ) );
    }

    public void testAddPermission3()
    {
        m_ae.addPermission( new PagePermission( "comment" ) );

        assertFalse( "has edit permission", m_ae.checkPermission( new PagePermission( "edit" ) ) );
    }

    public void testAddPermission4()
    {
        m_ae.addPermission( new PagePermission( "edit" ) );

        assertTrue( "has comment permission", m_ae.checkPermission( new PagePermission( "comment" ) ) );
    }
    
    public void testAddPermission5() {
        m_ae.addPermission( new PagePermission( "view" ) );
        
        assertTrue( "has view all", m_ae.checkPermission( new PagePermission( "view" ) ) );
        assertTrue( "has view on single page", m_ae.checkPermission( new PagePermission( "SamplePage", "view" ) ) );
    }

    public void testRemovePermission()
    {
        m_ae.addPermission( new PagePermission( "view" ) );
        m_ae.addPermission( new PagePermission( "edit" ) );

        assertTrue( "has edit permission", m_ae.checkPermission( new PagePermission( "edit" ) ) );
        assertTrue( "has view permission", m_ae.checkPermission( new PagePermission( "view" ) ) );

        m_ae.removePermission( new PagePermission( "edit" ) );

        assertFalse( "no edit permission", m_ae.checkPermission( new PagePermission( "edit" ) ) );
        assertTrue( "has view permission", m_ae.checkPermission( new PagePermission( "view" ) ) );
    }

    public void testDefaults()
    {
        assertFalse( "elements", m_ae.permissions().hasMoreElements() );
        assertNull( "principal", m_ae.getPrincipal() );
    }

    public static Test suite()
    {
        return new TestSuite( AclEntryImplTest.class );
    }
}
