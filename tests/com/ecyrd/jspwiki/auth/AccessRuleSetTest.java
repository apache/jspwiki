
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;


public class AccessRuleSetTest
    extends TestCase
{

    UserProfile m_wup;


    public AccessRuleSetTest( String s )
    {
        super( s );
        Properties props = new Properties();
        try
        {
            props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
            PropertyConfigurator.configure(props);
        }
        catch( IOException e ) {}
    }

    public void setUp()
        throws Exception
    {
        // We create a helper UserProfile with role wibble and permission wobble.

        m_wup = new UserProfile();
        m_wup.setName( "ebu" );
        m_wup.addRole( "wibble" );
        m_wup.addPermission( "wobble" );
        m_wup.setStatus( UserProfile.VALIDATED );
    }

    public void tearDown()
    {
    }

    public void testPluginRecognition1()
        throws Exception
    {
        assertTrue( AccessRuleSet.isAccessRule( "{ALLOW wibble wobble woo" ) );
    }

    public void testPluginRecognition2()
        throws Exception
    {
        assertTrue( AccessRuleSet.isAccessRule( "{DENY wibble wobble woo" ) );
    }

    public void testPluginRecognition3()
        throws Exception
    {
        assertTrue( AccessRuleSet.isAccessRule( "{REQUIRE wibble wobble woo" ) );
    }

    public void testAllowRead()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{ALLOW READ wibble}" );
        assertTrue( rules.hasReadAccess( m_wup ) );

        rules.clear();

        rules.addRule( "ALLOW READ wibble" );
        assertTrue( rules.hasReadAccess( m_wup ) );
    }

    public void testAllowWrite()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{ALLOW WRITE wibble}" );
        assertTrue( rules.hasWriteAccess( m_wup ) );

        rules.clear();

        rules.addRule( "ALLOW WRITE wibble" );
        assertTrue( rules.hasWriteAccess( m_wup ) );
    }

    public void testRequireWrite()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{REQUIRE WRITE wobble}" );
        assertTrue( rules.hasWriteAccess( m_wup ) );

        rules.clear();

        rules.addRule( "REQUIRE WRITE wobble" );
        assertTrue( rules.hasWriteAccess( m_wup ) );
    }

    public void testRequireRead()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{REQUIRE READ wobble}" );
        assertTrue( rules.hasReadAccess( m_wup ) );

        rules.clear();

        rules.addRule( "REQUIRE READ wobble" );
        assertTrue( rules.hasReadAccess( m_wup ) );
    }

    public void testPositiveChain1()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );
        rules.addRule( "{ALLOW READ wibble}" );
        assertTrue( rules.hasReadAccess( m_wup ) );
    }

    public void testPositiveChain2()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );
        rules.addRule( "{ALLOW READ wibble}" );
        rules.addRule( "{DENY READ xyzzy}" );
        assertTrue( rules.hasReadAccess( m_wup ) );
    }

    public void testNegativeChain1()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );
        rules.addRule( "{ALLOW READ xyzzy}" );
        assertFalse( rules.hasReadAccess( m_wup ) );
    }

    public void testNegativeChain2()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        assertFalse( rules.hasReadAccess( m_wup ) );
    }

    public void testNegativeChain3()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{ALLOW READ ALL}" );
        rules.addRule( "{DENY READ wibble}" );
        assertFalse( rules.hasReadAccess( m_wup ) );
    }

    public void testNegativeChain4()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );
        rules.addRule( "{ALLOW READ wibble}" );
        rules.addRule( "{REQUIRE READ xyzzy}" );
        assertFalse( rules.hasReadAccess( m_wup ) );
    }

    public void testCatenation()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );

        AccessRuleSet rules2 = new AccessRuleSet();
        rules2.addRule( "{ALLOW READ wibble}" );
        
        rules.add( rules2 );

        assertTrue( rules.hasReadAccess( m_wup ) );
    }

    
    public void testCopy()
        throws Exception
    {
        AccessRuleSet rules = new AccessRuleSet();
        rules.addRule( "{DENY READ ALL}" );
        rules.addRule( "{ALLOW READ wibble}" );

        AccessRuleSet rules2 = rules.copy();
        assertTrue( rules2.hasReadAccess( m_wup ) );
    }

    public static Test suite()
    {
        return new TestSuite( AccessRuleSetTest.class );
    }

}
