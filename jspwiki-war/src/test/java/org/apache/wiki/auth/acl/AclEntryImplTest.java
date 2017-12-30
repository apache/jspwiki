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
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AclEntryImplTest

{
    AclEntryImpl m_ae;

    @Before
    public void setUp()
    {
        m_ae = new AclEntryImpl();
    }

    @Test
    public void testAddPermission()
    {
        m_ae.addPermission( PagePermission.VIEW );

        Assert.assertTrue( "no permission", m_ae.checkPermission( PagePermission.VIEW ) );
        Assert.assertFalse( "permission found", m_ae.checkPermission( PagePermission.EDIT ) );
    }

    @Test
    public void testAddPermission2()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        Assert.assertTrue( "no editpermission", m_ae.checkPermission( PagePermission.EDIT ) );
        Assert.assertTrue( "no viewpermission", m_ae.checkPermission( PagePermission.VIEW ) );
    }

    @Test
    public void testAddPermission3()
    {
        m_ae.addPermission( PagePermission.COMMENT );

        Assert.assertFalse( "has edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
    }

    @Test
    public void testAddPermission4()
    {
        m_ae.addPermission( PagePermission.EDIT );

        Assert.assertTrue( "has comment permission", m_ae.checkPermission( PagePermission.COMMENT ) );
    }

    @Test
    public void testAddPermission5()
    {
        m_ae.addPermission( PagePermission.VIEW );

        Assert.assertTrue( "has view all", m_ae.checkPermission( PagePermission.VIEW ) );
        Assert.assertTrue( "has view on single page", m_ae.checkPermission( PermissionFactory.getPagePermission( "mywiki:SamplePage", "view" ) ) );
    }

    @Test
    public void testRemovePermission()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        Assert.assertTrue( "has edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
        Assert.assertTrue( "has view permission", m_ae.checkPermission( PagePermission.VIEW ) );

        m_ae.removePermission( PagePermission.EDIT );

        Assert.assertFalse( "no edit permission", m_ae.checkPermission( PagePermission.EDIT ) );
        Assert.assertTrue( "has view permission", m_ae.checkPermission( PagePermission.VIEW ) );
    }

    @Test
    public void testDefaults()
    {
        Assert.assertFalse( "elements", m_ae.permissions().hasMoreElements() );
        Assert.assertNull( "principal", m_ae.getPrincipal() );
    }

}
