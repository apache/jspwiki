/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.auth.acl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.auth.acl.AclEntryImpl;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;

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
