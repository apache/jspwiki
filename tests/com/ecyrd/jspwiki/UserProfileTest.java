
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

/**
 *  Tests the UserProfile class.
 *  @author Janne Jalkanen
 */
public class UserProfileTest extends TestCase
{
    public UserProfileTest( String s )
    {
        super( s );
        Properties props = new Properties();
        try
        {
            props.load( TestEngine.findTestProperties() );
            PropertyConfigurator.configure(props);
        }
        catch( IOException e ) {}
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testStringRepresentation()
        throws Exception
    {
        UserProfile p = new UserProfile("username=JanneJalkanen");

        assertEquals( "name", "JanneJalkanen",p.getName() );
    }

    /**
     *  Sometimes not all servlet containers offer you correctly
     *  decoded cookies.  Reported by KalleKivimaa.
     */
    public void testBrokenStringRepresentation()
        throws Exception
    {
        UserProfile p = new UserProfile("username%3DJanneJalkanen");

        assertEquals( "name", "JanneJalkanen",p.getName() );
    }

    public void testUTFStringRepresentation()
        throws Exception
    {
        UserProfile p = new UserProfile();

        p.setName("Määmöö");
        String s = p.getStringRepresentation();

        UserProfile p2 = new UserProfile( s );
        assertEquals( "name", "Määmöö", p2.getName() );
    }

    public void testUTFURLStringRepresentation()
        throws Exception
    {
        UserProfile p = new UserProfile("username="+TextUtil.urlEncodeUTF8("Määmöö"));

        assertEquals( "name", "Määmöö",p.getName() );
    }

    public void testRoles()
        throws Exception
    {
        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        wup.addRole( "role1" );
        wup.addRole( "role2" );

        assertTrue( wup.hasRole( "role1" ) );
        assertTrue( wup.hasRole( "role2" ) );
        assertTrue( !wup.hasRole( "role" ) );

        Iterator it = wup.getRoles();
        assertTrue( it != null && it.hasNext() );
    }


    public void testPermissions()
        throws Exception
    {
        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        wup.addPermission( "perm1" );
        wup.addPermission( "perm2" );

        assertTrue( wup.hasPermission( "perm1" ) );
        assertTrue( wup.hasPermission( "perm2" ) );
        assertTrue( !wup.hasPermission( "perm" ) );

        Iterator it = wup.getPermissions();
        assertTrue( it != null && it.hasNext() );
    }
    
    public void testInfo()
        throws Exception
    {
        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        wup.setInfo( "info1", "42" );

        assertTrue( "42".equals( wup.getInfo( "info1" ) ) );
    }

    public static Test suite()
    {
        return new TestSuite( UserProfileTest.class );
    }
}
