
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;


public class AuthenticatorTest 
    extends TestCase
{
    Properties props = new Properties();

    public AuthenticatorTest( String s )
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

    public void testDummyAuth()
        throws Exception
    {
        // No jspwiki.authenticator definition should cause a DummyAuthenticator 
        // to be used.
        props.remove( Authenticator.PROP_AUTHENTICATOR );
        Authenticator auth = new Authenticator( props );

        assertTrue( auth.getAuthenticator() instanceof DummyAuthenticator );

        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        boolean rval = auth.authenticate( wup );
        assertTrue( rval );
        assertTrue( wup.isValidated() );
        assertTrue( "ebu".equals( wup.getName() ) );
    }

    public void testFileAuth()
        throws Exception
    {
        props.setProperty( Authenticator.PROP_AUTHENTICATOR, "FileAuthenticator" );
        Authenticator auth = new Authenticator( props );

        assertTrue( auth.getAuthenticator() instanceof FileAuthenticator );
    }

    public static Test suite()
    {
        return new TestSuite( AuthenticatorTest.class );
    }

}
