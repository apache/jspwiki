
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.permissions.*;
import com.ecyrd.jspwiki.attachment.*;
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
        m_engine.deletePage("FooGroup");
    }

    public void testSimplePermissions()
        throws Exception
    {
        String src = "[{DENY edit Guest}] [{ALLOW edit FooBar}]";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");
        UserProfile wup = new UserProfile();
        wup.setLoginStatus( UserProfile.PASSWORD );
        wup.setName( "FooBar" );

        System.out.println(printPermissions( p ));

        assertTrue( "read 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
            
        wup.setName( "GobbleBlat" );
        assertTrue( "read 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertFalse( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }

    public void testNamedPermissions()
        throws Exception
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

    public void testAttachmentPermissions()
        throws Exception
    {
        String src = "[{ALLOW edit NamedGuest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        Attachment att = new Attachment( "Test", "foobar.jpg" );

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertFalse( "edit 1", m_manager.checkPermission( att, wup, new UploadPermission() ) );

        wup.setLoginStatus( UserProfile.COOKIE );
        
        assertTrue( "edit 2", m_manager.checkPermission( att, wup, new UploadPermission() ) );
    }

    public void testAttachmentPermissions2()
        throws Exception
    {
        String src = "[{ALLOW upload FooBar}] [{ALLOW view Guest}] ";

        m_engine.saveText( "Test", src );

        Attachment att = new Attachment( "Test", "foobar.jpg" );

        UserProfile wup = new UserProfile();
        wup.setLoginStatus( UserProfile.PASSWORD );
        wup.setName( "FooBar" );
                
        assertTrue( "download", m_manager.checkPermission( att, wup, "view" ) );

        
        assertTrue( "upload", m_manager.checkPermission( att, wup, "upload" ) );
    }

    /**
     *  An user should not be allowed to simply set their name in the 
     *  cookie and be allowed access.
     */
    public void testNamedPermissions2()
        throws Exception
    {
        String src = "[{ALLOW edit FooBar}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertFalse( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.COOKIE );
        
        assertFalse( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.CONTAINER );

        assertTrue( "edit 3", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.PASSWORD );

        assertTrue( "edit 4", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }

    /**
     *  An user should not be allowed to simply set their name in the 
     *  cookie and be allowed access (this time with group data).
     */
    /*
     * TODO: Fix this test
   
    public void testNamedPermissions3()
        throws Exception
    {
        String src = "[{ALLOW edit FooGroup}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        m_engine.saveText( "FooGroup", "[{SET members=FooBar}]" );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName( "FooBar" );

        assertFalse( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.COOKIE );
        
        assertFalse( "edit 2", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.CONTAINER );

        assertTrue( "edit 3", m_manager.checkPermission( p, wup, new EditPermission() ) );

        wup.setLoginStatus( UserProfile.PASSWORD );

        assertTrue( "edit 4", m_manager.checkPermission( p, wup, new EditPermission() ) );
    }
*/
    /**
     *  A superuser should be allowed permissions.
     */
    public void testAdminPermissions()
        throws Exception
    {
        String src = "[{DENY view Guest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setLoginStatus( UserProfile.CONTAINER );
        wup.setName( "AdminGroup" );

        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
        assertTrue( "view 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "delete 1", m_manager.checkPermission( p, wup, new DeletePermission() ) );
        assertTrue( "comment 1", m_manager.checkPermission( p, wup, new CommentPermission() ) );

        wup.setName( "NobodyHere" );

        assertFalse( "view 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
    }

    /**
     *  Also, anyone in the supergroup should be allowed all permissions.
     */
    public void testAdminPermissions2()
        throws Exception
    {
        String src = "[{DENY view Guest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        src = "[{SET members=FooBar}]";
        
        m_engine.saveText( "AdminGroup", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setLoginStatus( UserProfile.PASSWORD );
        wup.setName( "FooBar" );

        assertTrue( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
        assertTrue( "view 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertTrue( "delete 1", m_manager.checkPermission( p, wup, new DeletePermission() ) );
        assertTrue( "comment 1", m_manager.checkPermission( p, wup, new CommentPermission() ) );

        wup.setName( "NobodyHere" );

        assertFalse( "view 2", m_manager.checkPermission( p, wup, new ViewPermission() ) );
    }

    /**
     *  A superuser should be allowed permissions, but not if he's not logged in.
     */
    public void testAdminPermissionsNoLogin()
        throws Exception
    {
        String src = "[{DENY view Guest}] [{DENY edit Guest}] ";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        m_engine.saveText( "AdminGroup", "[{SET members=Hobble}]" );

        UserProfile wup = new UserProfile();
        wup.setName( "Hobble" );

        assertFalse( "edit 1", m_manager.checkPermission( p, wup, new EditPermission() ) );
        assertFalse( "view 1", m_manager.checkPermission( p, wup, new ViewPermission() ) );
        assertFalse( "delete 1", m_manager.checkPermission( p, wup, new DeletePermission() ) );
        assertFalse( "comment 1", m_manager.checkPermission( p, wup, new CommentPermission() ) );
    }

    /**
     *  From Paul Downes.
     */
    public void testFunnyPermissions()
        throws Exception
    {
        String src = "[{DENY edit Guest}]\n[{ALLOW edit NamedGuest}]\n";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName("Foogor");

        assertFalse("guest edit", m_manager.checkPermission( p, wup, new EditPermission() ) );
        
        wup.setLoginStatus( UserProfile.COOKIE );

        assertTrue("namedguest edit", m_manager.checkPermission( p, wup, new EditPermission() ));
    }

    /**
     *  From Paul Downes.
     */
    public void testFunnyPermissions2()
        throws Exception
    {
        String src = "[{ALLOW edit Guest}]\n[{DENY edit Guest}]\n";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName("Foogor");

        assertTrue("guest edit", m_manager.checkPermission( p, wup, new EditPermission() ) );
        
        wup.setLoginStatus( UserProfile.COOKIE );

        assertTrue("namedguest edit", m_manager.checkPermission( p, wup, new EditPermission() ));
    }

    /**
     *  From Paul Downes.
     */
    public void testFunnyPermissions3()
        throws Exception
    {
        String src = "[{ALLOW edit Guest}]\n[{DENY view Guest}]\n";

        m_engine.saveText( "Test", src );

        WikiPage p = m_engine.getPage("Test");

        UserProfile wup = new UserProfile();
        wup.setName("Foogor");

        assertFalse("guest edit", m_manager.checkPermission( p, wup, new ViewPermission() ) );

        assertTrue("view", m_manager.checkPermission( p, wup, new EditPermission() ));
    }


    /**
     *  Returns a string representation of the permissions of the page.
     */
    public static String printPermissions( WikiPage p )
        throws Exception
    {
        StringBuffer sb = new StringBuffer();

        AccessControlList acl = p.getAcl();

        sb.append("page = "+p.getName()+"\n");

        if( acl != null )
        {
            for( Enumeration e = acl.entries(); e.hasMoreElements(); )
            {
                AclEntry entry = (AclEntry) e.nextElement();

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
