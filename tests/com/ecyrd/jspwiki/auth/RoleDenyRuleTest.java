

package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;

public class RoleDenyRuleTest 
    extends TestCase
{

    public RoleDenyRuleTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testMatch()
    {
        RoleDenyRule rule = new RoleDenyRule( "foobar" );

        UserProfile wup = new UserProfile();
        wup.addRole( "foobar" );

        int result = rule.evaluate( wup );
        
        assertTrue( result == AccessRule.DENY );
    }


    public void testFailure()
    {
        RoleDenyRule rule = new RoleDenyRule( "foobar" );

        UserProfile wup = new UserProfile();
        wup.addPermission( "barfoo" );

        int result = rule.evaluate( wup );
        
        assertTrue( result == AccessRule.CONTINUE );
    }


    public static Test suite()
    {
        return new TestSuite( RoleDenyRuleTest.class );
    }

}
