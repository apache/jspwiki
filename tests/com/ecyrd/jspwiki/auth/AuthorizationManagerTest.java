
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.permissions.*;
import com.ecyrd.jspwiki.acl.*;
import java.security.acl.*;

/**
 *  Tests the AuthorizationManager class.
 *  @author Janne Jalkanen
 */
public class AuthorizationManagerTest extends TestCase
{
    private AuthorizationManager m_manager;
    private TestEngine m_engine;

    public AuthorizationManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);
        m_manager = m_engine.getAuthorizationManager();
    }

    public void tearDown()
    {
        m_engine.deletePage("Test");
        m_engine.deletePage("AdminGroup");
    }

    public void testSimplePermissions()
        throws Exception
    {
        String src = "[{DENY edit Guest}] [{ALLOW edit FooBar}]";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");
        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        System.out.println(printPermissions( p ));

        assertTrue( "read 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
            
        wup.setName( "GobbleBlat" );
        assertTrue( "read 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertFalse( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }

    public void testNamedPermissions()
    {
        String src = "[{ALLOW edit NamedGuest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertFalse( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.COOKIE );
        
        assertTrue( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }

    /**
     *  An user should not be allowed to simply set their name in the 
     *  cookie and be allowed access.
     */
    public void testNamedPermissions2()
    {
        String src = "[{ALLOW edit FooBar}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertFalse( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.COOKIE );
        
        assertFalse( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }

    /**
     *  A superuser should be allowed permissions.
     */
    public void testAdminPermissions()
    {
        String src = "[{DENY view Guest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "AdminGroup" );

        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
        assertTrue( "view 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "delete 1", m_manager.checkPermission( p, wup, new DeletePermission() ) );
        assertTrue( "comment 1", m_manager.checkPermission( p, wup, new CommentPermission() ) );

        wup.setName( "NobodyHere" );

        assertFalse( "view 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
    }

    public void testAdminPermissions2()
    {
        String src = "[{DENY view Guest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        src = "[{MEMBERS FooBar}]";
        
        m_engine.saveText( "AdminGroup", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
        assertTrue( "view 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "delete 1", m_manager.checkPermission( p, wup, new DeletePermission() ) );
        assertTrue( "comment 1", m_manager.checkPermission( p, wup, new CommentPermission() ) );

        wup.setName( "NobodyHere" );

        assertFalse( "view 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
    }

    /**
     *  Returns a string representation of the permissions of the page.
     */
    public static String printPermissions( WikiPage p )
    {
        StringBuffer sb = new StringBuffer();

        AccessControlList acl = p.getAcl();

        sb.append("page = "+p.getName()+"\n");

        if( acl != null )
        {
            for( Enumeration enum = acl.entries(); enum.hasMoreElements(); )
            {
                AclEntry entry = (AclEntry) enum.nextElement();

                sb.append("  user = "+entry.getPrincipal().getName()+": ");

                if( entry.isNegative() ) sb.append("NEG");

                sb.append("(");
                for( Enumeration perms = entry.permissions(); perms.hasMoreElements(); )
                {
                    Permission perm = (Permission) perms.nextElement();
                    sb.append( perm.toString() );
                }
                sb.append(")\n");
            }
        }
        else
        {
            sb.append("  no permissions\n");
        }

        return sb.toString();
    }

    public static Test suite()
    {
        return new TestSuite( AuthorizationManagerTest.class );
    }
}
