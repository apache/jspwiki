

package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;


public class DummyAuthenticatorTest 
    extends TestCase
{
    Properties props = new Properties();

    public DummyAuthenticatorTest( String s )
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
        DummyAuthenticator dumdum = new DummyAuthenticator();
        dumdum.initialize( props );

        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        boolean rval = dumdum.authenticate( wup );
        assertTrue( rval );
        assertTrue( wup.isValidated() );
    }


    public static Test suite()
    {
        return new TestSuite( DummyAuthenticatorTest.class );
    }

}
