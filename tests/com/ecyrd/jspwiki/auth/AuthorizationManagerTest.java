
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
    }

    public void testSimplePermissions()
        throws Exception
    {
        String src = "[{DENY edit All}] [{ALLOW edit FooBar}]";

        m_engine.saveText( "Test", src );

        try
        {
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
        finally
        {
            m_engine.deletePage( "Test" );
        }
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
