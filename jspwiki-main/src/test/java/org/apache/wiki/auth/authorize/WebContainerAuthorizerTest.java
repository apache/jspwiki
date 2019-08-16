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
package org.apache.wiki.auth.authorize;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.jdom2.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.Principal;
import java.util.Properties;

public class WebContainerAuthorizerTest
{
    WikiEngine m_engine;
    WebContainerAuthorizer m_authorizer;
    Document   m_webxml;

    @BeforeEach
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        m_engine = new TestEngine( props );
        m_authorizer = new WebContainerAuthorizer();
        m_authorizer.initialize( m_engine, props );
        m_webxml = m_authorizer.getWebXml();
        if ( m_webxml == null )
        {
            throw new Exception("Could not load web.xml");
        }
    }

    @Test
    public void testConstraints() throws Exception
    {
        Assertions.assertTrue( m_authorizer.isConstrained( "/Delete.jsp", new Role( "Admin" ) ) );
        Assertions.assertTrue( m_authorizer.isConstrained( "/Login.jsp", Role.AUTHENTICATED ) );
        Assertions.assertFalse( m_authorizer.isConstrained( "/UserPreferences.jsp", Role.AUTHENTICATED ) );
    }

    @Test
    public void testGetRoles()
    {
        // We should find 2 roles: AUTHENTICATED plus custom role "Admin"
        Principal[] roles = m_authorizer.getRoles();
        Assertions.assertEquals( 2, roles.length );
        Assertions.assertTrue( ArrayUtils.contains( roles, Role.AUTHENTICATED ) );
        Assertions.assertTrue( ArrayUtils.contains( roles, new Role( "Admin" ) ) );
    }

    @Test
    public void testRoles() throws Exception
    {
        Role[] roles = m_authorizer.getRoles( m_webxml );
        boolean found = false;
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( Role.AUTHENTICATED ) )
            {
                found = true;
            }
        }
        Assertions.assertTrue( found, "Didn't find AUTHENTICATED" );
        for ( int i = 0; i < roles.length; i++ )
        {
            if ( roles[i].equals( new Role( "Admin" ) ) )
            {
                found = true;
            }
        }
        Assertions.assertTrue( found, "Didn't find ADMIN" );
    }

    @Test
    public void testIsContainerAuthorized()
    {
        Assertions.assertTrue( m_authorizer.isContainerAuthorized() );
    }

}
