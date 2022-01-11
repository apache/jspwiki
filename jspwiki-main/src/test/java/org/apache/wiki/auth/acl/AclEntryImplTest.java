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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AclEntryImplTest

{
    AclEntryImpl m_ae;

    @BeforeEach
    public void setUp()
    {
        m_ae = new AclEntryImpl();
    }

    @Test
    public void testAddPermission()
    {
        m_ae.addPermission( PagePermission.VIEW );

        Assertions.assertTrue( m_ae.checkPermission( PagePermission.VIEW ), "no permission" );
        Assertions.assertFalse( m_ae.checkPermission( PagePermission.EDIT ), "permission found" );
    }

    @Test
    public void testAddPermission2()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        Assertions.assertTrue( m_ae.checkPermission( PagePermission.EDIT ), "no editpermission" );
        Assertions.assertTrue( m_ae.checkPermission( PagePermission.VIEW ), "no viewpermission" );
    }

    @Test
    public void testAddPermission3()
    {
        m_ae.addPermission( PagePermission.COMMENT );

        Assertions.assertFalse( m_ae.checkPermission( PagePermission.EDIT ), "has edit permission" );
    }

    @Test
    public void testAddPermission4()
    {
        m_ae.addPermission( PagePermission.EDIT );

        Assertions.assertTrue( m_ae.checkPermission( PagePermission.COMMENT ), "has comment permission" );
    }

    @Test
    public void testAddPermission5()
    {
        m_ae.addPermission( PagePermission.VIEW );

        Assertions.assertTrue( m_ae.checkPermission( PagePermission.VIEW ), "has view all" );
        Assertions.assertTrue( m_ae.checkPermission( PermissionFactory.getPagePermission( "mywiki:SamplePage", "view" ) ), "has view on single page" );
    }

    @Test
    public void testRemovePermission()
    {
        m_ae.addPermission( PagePermission.VIEW );
        m_ae.addPermission( PagePermission.EDIT );

        Assertions.assertTrue( m_ae.checkPermission( PagePermission.EDIT ), "has edit permission" );
        Assertions.assertTrue( m_ae.checkPermission( PagePermission.VIEW ), "has view permission" );

        m_ae.removePermission( PagePermission.EDIT );

        Assertions.assertFalse( m_ae.checkPermission( PagePermission.EDIT ), "no edit permission" );
        Assertions.assertTrue( m_ae.checkPermission( PagePermission.VIEW ), "has view permission" );
    }

    @Test
    public void testDefaults()
    {
        Assertions.assertFalse( m_ae.permissions().hasMoreElements(), "elements" );
        Assertions.assertNull( m_ae.getPrincipal(), "principal" );
    }

}
