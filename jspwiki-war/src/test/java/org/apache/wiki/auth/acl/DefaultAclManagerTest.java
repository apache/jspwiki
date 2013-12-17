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

import java.security.Principal;
import java.util.Properties;
import java.util.regex.Matcher;

import org.apache.commons.lang.ArrayUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.permissions.PermissionFactory;

public class DefaultAclManagerTest
    extends TestCase
{
    TestEngine m_engine;

    public DefaultAclManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        m_engine = new TestEngine(props);

        String text = "Foo";
        m_engine.saveText( "TestDefaultPage", text );

        text = "Bar. [{ALLOW edit Charlie, Herman}] ";
        m_engine.saveText( "TestAclPage", text );
    }

    public void tearDown()
    {
        try
        {
            m_engine.deletePage( "TestDefaultPage" );
            m_engine.deletePage( "TestAclPage" );
        }
        catch ( ProviderException e )
        {
        }
    }

    public void testGetPermissions()
    {
        WikiPage page = m_engine.getPage( "TestDefaultPage" );
        Acl acl = m_engine.getAclManager().getPermissions( page );
        assertNotNull( page.getAcl() );
        assertTrue(page.getAcl().isEmpty());

        page = m_engine.getPage( "TestAclPage" );
        acl = m_engine.getAclManager().getPermissions( page );
        assertNotNull( page.getAcl() );
        assertFalse(page.getAcl().isEmpty());

        Principal[] p;

        // Charlie is an editor; reading is therefore implied
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "view") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );

        // Charlie should be in the ACL as an editor
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "edit") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );

        // Charlie should not be able to delete this page
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "delete") );
        assertEquals( 0, p.length );

        // Herman is an unregistered user and editor; reading is implied
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "view") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );

        // Herman should be in the ACL as an editor
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "edit") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );

        // Herman should not be able to delete this page
        p = acl.findPrincipals( PermissionFactory.getPagePermission(page, "delete") );
        assertEquals( 0, p.length );
    }

    public void testAclRegex()
    {
        String acl;
        Matcher m;

        acl = "[{ALLOW view Bob, Alice, Betty}] Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        assertTrue ( m.find() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{ALLOW view Bob, Alice, Betty}]", m.group(0) );
        assertEquals( "view", m.group(1) );
        assertEquals( "Bob, Alice, Betty", m.group(2) );
        assertFalse( m.find() );

        acl = "[{ALLOW view Alice}] Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        assertTrue ( m.find() );
        System.out.println( m.group() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{ALLOW view Alice}]", m.group(0) );
        assertEquals( "view", m.group(1) );
        assertEquals( "Alice", m.group(2) );
        assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view   Alice  }]  Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        assertTrue ( m.find() );
        System.out.println( m.group() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{   ALLOW   view   Alice  }]", m.group(0) );
        assertEquals( "view", m.group(1) );
        assertEquals( "Alice", m.group(2) );
        assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view  Alice  ,  Bob  }]  Test text.";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        assertTrue ( m.find() );
        System.out.println( m.group() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{   ALLOW   view  Alice  ,  Bob  }]", m.group(0) );
        assertEquals( "view", m.group(1) );
        assertEquals( "Alice  ,  Bob", m.group(2) );
        assertFalse( m.find() );

        acl = "Test text   [{   ALLOW   view  Alice  ,  Bob  }]  Test text  [{ALLOW edit Betty}].";
        m = DefaultAclManager.ACL_PATTERN.matcher( acl );
        assertTrue ( m.find() );
        System.out.println( m.group() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{   ALLOW   view  Alice  ,  Bob  }]", m.group(0) );
        assertEquals( "view", m.group(1) );
        assertEquals( "Alice  ,  Bob", m.group(2) );
        assertTrue ( m.find() );
        assertEquals( 2, m.groupCount() );
        assertEquals( "[{ALLOW edit Betty}]", m.group(0) );
        assertEquals( "edit", m.group(1) );
        assertEquals( "Betty", m.group(2) );
        assertFalse( m.find() );
    }

    public void testPrintAcl()
    {
        // Verify that the printed Acl for the test page is OK
        WikiPage page = m_engine.getPage( "TestAclPage" );
        Acl acl = m_engine.getAclManager().getPermissions( page );
        String aclString = DefaultAclManager.printAcl( acl );
        assertEquals( "[{ALLOW edit Charlie,Herman}]\n", aclString );

        // Create an ACL from scratch
        acl = new AclImpl();
        AclEntry entry = new AclEntryImpl();
        entry.setPrincipal( new WikiPrincipal( "Charlie" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "view" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        acl.addEntry( entry );
        entry = new AclEntryImpl();
        entry.setPrincipal( new WikiPrincipal( "Devin" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "edit" ) );
        entry.addPermission( PermissionFactory.getPagePermission( "Main:Foo", "delete" ) );
        acl.addEntry( entry );

        // Verify that the printed ACL is OK
        String expectedValue = "[{ALLOW delete Devin}]\n[{ALLOW edit Charlie,Devin}]\n[{ALLOW view Charlie}]\n";
        assertEquals( expectedValue, DefaultAclManager.printAcl( acl ) );
    }

    public static Test suite()
    {
        return new TestSuite( DefaultAclManagerTest.class );
    }

}
