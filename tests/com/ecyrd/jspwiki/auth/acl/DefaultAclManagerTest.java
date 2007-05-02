package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;
import java.util.Properties;
import java.util.regex.Matcher;

import org.apache.commons.lang.ArrayUtils;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.providers.ProviderException;

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
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine(props);
        
        String text = "Foo";
        m_engine.saveText( "TestDefaultPage", text );
        
        text = "Bar. [{ALLOW edit Charlie, Herman}] ";
        m_engine.saveText( "TestAclPage", text );
    }

    public void tearDown()
    {
        try {
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
        p = acl.findPrincipals( new PagePermission(page, "view") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );
        
        // Charlie should be in the ACL as an editor
        p = acl.findPrincipals( new PagePermission(page, "edit") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new WikiPrincipal("Charlie") ) );
        
        // Charlie should not be able to delete this page
        p = acl.findPrincipals( new PagePermission(page, "delete") );
        assertEquals( 0, p.length );
        
        // Herman is an unregistered user and editor; reading is implied
        p = acl.findPrincipals( new PagePermission(page, "view") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );
        
        // Herman should be in the ACL as an editor
        p = acl.findPrincipals( new PagePermission(page, "edit") );
        assertEquals( 2, p.length );
        assertTrue( ArrayUtils.contains( p, new UnresolvedPrincipal("Herman") ) );
        
        // Herman should not be able to delete this page
        p = acl.findPrincipals( new PagePermission(page, "delete") );
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
        entry.addPermission( new PagePermission( "Main:Foo", "view" ) );
        entry.addPermission( new PagePermission( "Main:Foo", "edit" ) );
        acl.addEntry( entry );
        entry = new AclEntryImpl();
        entry.setPrincipal( new WikiPrincipal( "Devin" ) );
        entry.addPermission( new PagePermission( "Main:Foo", "edit" ) );
        entry.addPermission( new PagePermission( "Main:Foo", "delete" ) );
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
