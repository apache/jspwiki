

package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.auth.DummyAuthorizer;
import com.ecyrd.jspwiki.UserProfile;


public class DummyAuthorizerTest 
    extends TestCase
{
    Properties props = new Properties();
    DummyAuthorizer m_auth;

    public DummyAuthorizerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
        PropertyConfigurator.configure(props);
    }

    public void tearDown()
    {
    }

    public void testDummyUserWUP()
        throws Exception
    {
        props.setProperty( DummyAuthorizer.PROP_DUMMYROLES, "wibble,wobble,woo" );
        props.setProperty( DummyAuthorizer.PROP_DUMMYPERMS, "fee,fie,foe" );
        m_auth = new DummyAuthorizer();
        m_auth.initialize( props );

        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        m_auth.loadPermissions( wup );

        assertTrue( wup.hasRole( "wibble" ) );
        assertTrue( wup.hasRole( "wobble" ) );
        assertTrue( wup.hasRole( "woo" ) );
        assertTrue( wup.hasPermission( "fee" ) );
        assertTrue( wup.hasPermission( "fie" ) );
        assertTrue( wup.hasPermission( "foe" ) );
        assertFalse( wup.hasPermission( "xyzzy" ) );
    }


    public static Test suite()
    {
        return new TestSuite( DummyAuthorizerTest.class );
    }

}
