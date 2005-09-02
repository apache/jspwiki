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
        m_ae.addPermission( PagePermission.VIEW );

        assertTrue( "no permission", m_ae.checkPermission( PagePermission.VIEW ) );
        assertFalse( "permission found", m_ae.checkPermission( PagePermission.EDIT ) );
    }


    public void testAddPermission2()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        assertTrue( "no editpermission", m_ae.checkPermission( PagePermission.EDIT ) );
        assertTrue( "no viewpermission", m_ae.checkPermission( PagePermission.VIEW ) );
    }

    public void testAddPermission3()
    {
        m_ae.addPermission( PagePermission.COMMENT );

        assertFalse( "has edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
    }

    public void testAddPermission4()
    {
        m_ae.addPermission( PagePermission.EDIT );

        assertTrue( "has comment permission", m_ae.checkPermission( PagePermission.COMMENT ) );
    }
    
    public void testAddPermission5() {
        m_ae.addPermission( PagePermission.VIEW );
        
        assertTrue( "has view all", m_ae.checkPermission( PagePermission.VIEW ) );
        assertTrue( "has view on single page", m_ae.checkPermission( new PagePermission( "mywiki:SamplePage", "view" ) ) );
    }

    public void testRemovePermission()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        assertTrue( "has edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
        assertTrue( "has view permission", m_ae.checkPermission( PagePermission.VIEW ) );

        m_ae.removePermission( PagePermission.EDIT );

        assertFalse( "no edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
        assertTrue( "has view permission", m_ae.checkPermission( PagePermission.VIEW ) );
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
