/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.acl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.auth.acl.AclEntryImpl;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.PermissionFactory;

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

    public void testAddPermission5()
    {
        m_ae.addPermission( PagePermission.VIEW );

        assertTrue( "has view all", m_ae.checkPermission( PagePermission.VIEW ) );
        assertTrue( "has view on single page", m_ae.checkPermission( PermissionFactory.getPagePermission( "mywiki:SamplePage", "view" ) ) );
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
