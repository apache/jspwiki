
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;


public class AuthorizerTest 
    extends TestCase
{
    Properties props = new Properties();

    public AuthorizerTest( String s )
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
        // No jspwiki.authenticator definition should cause a DummyAuthorizer 
        // to be used.
        props.remove( Authorizer.PROP_AUTHORIZER );
        Authorizer auth = new Authorizer( props );

        assertTrue( auth.getAuthorizer() instanceof DummyAuthorizer );
    }

    public void testFileAuth()
        throws Exception
    {
        props.setProperty( Authorizer.PROP_AUTHORIZER, "FileAuthorizer" );
        Authorizer auth = new Authorizer( props );

        assertTrue( auth.getAuthorizer() instanceof FileAuthorizer );
    }

    public static Test suite()
    {
        return new TestSuite( AuthorizerTest.class );
    }

}
