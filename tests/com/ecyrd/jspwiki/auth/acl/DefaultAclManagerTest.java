package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiPage;
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
        
        text = "Bar. [{ALLOW edit Charlie}] ";
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
        
        // Charlie is an editor; reading is therefore implied
        Principal[] principals = acl.findPrincipals( new PagePermission( page, "view") );
        assertEquals( 1, principals.length );
        assertEquals( new UnresolvedPrincipal("Charlie"), principals[0]);
        
        // Charlie should be in the ACL as an editor
        principals = acl.findPrincipals( new PagePermission( page, "edit") );
        assertEquals( 1, principals.length );
        assertEquals( new UnresolvedPrincipal("Charlie"), principals[0]);
        
        // Charlie should not be able to delete this page
        principals = acl.findPrincipals( new PagePermission( page, "delete") );
        assertEquals( 0, principals.length );
    }

    public static Test suite()
    {
        return new TestSuite( DefaultAclManagerTest.class );
    }

}
