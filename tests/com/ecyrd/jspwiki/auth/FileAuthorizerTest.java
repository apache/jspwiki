
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;

public class FileAuthorizerTest 
    extends TestCase
{
    Properties props = new Properties();

    private String[] lines = {
        "# A comment line",
        "# default settings:", 
        "default ALLOW READ ALL", 
        "default ALLOW WRITE b", 
        "# A user definition:", 
        "user ebu xyzzy \ta,b, c, d , e ",
        "# A role mapping for role a",
        "role a fee,fie,foe"
    };

    public static final String testFileName = "authtest.tmp";
    private FileAuthorizer m_auth;

    public FileAuthorizerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
        PropertyConfigurator.configure(props);

        // Create a file to parse.
        File testAuth = new File( testFileName );
        if( testAuth.exists() )
            testAuth.delete();

        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( testAuth ) ) );
        for( int i = 0; i < lines.length; i++ )
            out.println( lines[i] );
        out.close();

        props.setProperty( FileAuthorizer.PROP_AUTHFILE, testFileName );
        m_auth = new FileAuthorizer();
        m_auth.initialize( props );
    }

    public void tearDown()
    {
        File testAuth = new File( testFileName );
        if( testAuth.exists() )
            testAuth.delete();
    }


    public void testLoadProfile()
    {
        // Load a user from the info at the start of this class.

        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        m_auth.loadPermissions( wup );

        assertTrue( wup.hasRole( "a" ) );
        assertTrue( wup.hasRole( "b" ) );
        assertTrue( wup.hasRole( "c" ) );
        assertTrue( wup.hasRole( "d" ) );
        assertTrue( wup.hasRole( "e" ) );
        assertTrue( wup.hasPermission( "fee" ) );
        assertTrue( wup.hasPermission( "fie" ) );
        assertTrue( wup.hasPermission( "foe" ) );
        assertFalse( wup.hasPermission( "xyzzy" ) );
    }

    


    public void testDefaultRules()
    {
        AccessRuleSet defaults = m_auth.getDefaultPermissions();
        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.addRole( "b" );
        assertTrue( defaults.hasWriteAccess( wup ) );

        wup = new UserProfile();
        wup.setName( "ebu" );
        wup.addRole( "c" );
        assertFalse( defaults.hasWriteAccess( wup ) );
        assertTrue( defaults.hasReadAccess( wup ) );
    }


    public void testRoleLoading()
    {
        UserProfile wup = new UserProfile();
        wup.setName( "frobozz" );
        m_auth.addRole( wup, "a" );

        assertTrue( wup.hasRole( "a" ) );
        assertTrue( wup.hasPermission( "fee" ) );
    }

    
  

    public static Test suite()
    {
        return new TestSuite( FileAuthorizerTest.class );
    }

}
