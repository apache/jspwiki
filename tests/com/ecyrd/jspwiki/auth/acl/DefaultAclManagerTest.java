package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;
import java.util.Properties;

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
        assertNull( page.getAcl() );
        
        page = m_engine.getPage( "TestAclPage" );
        acl = m_engine.getAclManager().getPermissions( page );
        assertNotNull( page.getAcl() );
        
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

    public static Test suite()
    {
        return new TestSuite( DefaultAclManagerTest.class );
    }

}
