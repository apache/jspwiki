

package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;

public class RequirePermissionRuleTest 
    extends TestCase
{

    public RequirePermissionRuleTest( String s )
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
        RequirePermissionRule rule = new RequirePermissionRule( "foobar" );

        UserProfile wup = new UserProfile();
        wup.addPermission( "foobar" );

        int result = rule.evaluate( wup );
        
        assertTrue( result == AccessRule.ALLOW );
    }


    public void testFailure()
    {
        RequirePermissionRule rule = new RequirePermissionRule( "foobar" );

        UserProfile wup = new UserProfile();
        wup.addPermission( "barfoo" );

        int result = rule.evaluate( wup );
        
        assertTrue( result == AccessRule.DENY );
    }


    public static Test suite()
    {
        return new TestSuite( RequirePermissionRuleTest.class );
    }

}
