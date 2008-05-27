package com.ecyrd.jspwiki.auth.user;

import java.io.IOException;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.PropertyConfigurator;

import com.ecyrd.jspwiki.TestEngine;

/**
 *  Tests the DefaultUserProfile class.
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

    public void testEquals()
    {
        UserProfile p = new DefaultUserProfile();
        UserProfile p2 = new DefaultUserProfile();

        p.setFullname("Alice");
        p2.setFullname("Bob");

        assertFalse( p.equals( p2 ) );
    }

    public void testEquals2()
    {
        UserProfile p = new DefaultUserProfile();
        UserProfile p2 = new DefaultUserProfile();

        p.setFullname("Alice");
        p2.setFullname("Alice");

        assertTrue( p.equals( p2 ) );
    }

    public static Test suite()
    {
        return new TestSuite( UserProfileTest.class );
    }
}
