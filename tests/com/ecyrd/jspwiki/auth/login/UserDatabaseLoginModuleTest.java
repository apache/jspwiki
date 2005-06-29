package com.ecyrd.jspwiki.auth.login;

import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.XMLUserDatabase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 */
public class UserDatabaseLoginModuleTest extends TestCase
{
    UserDatabase db;

    Subject      subject;

    public final void testLogin()
    {
        try
        {
            // Log in with a user that isn't in the database
            CallbackHandler handler = new WikiCallbackHandler( db, "user", "password" );
            LoginContext context = new LoginContext( "JSPWiki-custom", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "user" ) ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );
            
            // Login with a user that IS in the databasse
            subject = new Subject();
            handler = new WikiCallbackHandler( db, "janne", "myP@5sw0rd" );
            context = new LoginContext( "JSPWiki-custom", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 5, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "janne" ) ) );
            assertTrue( principals.contains( new WikiPrincipal( "JanneJalkanen" ) ) );
            assertTrue( principals.contains( new WikiPrincipal( "Janne Jalkanen" ) ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );            
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLogout()
    {
        try
        {
            CallbackHandler handler = new WikiCallbackHandler( db, "user", "password" );
            LoginContext context = new LoginContext( "JSPWiki-custom", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "user" ) ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );
            context.logout();
            assertEquals( 0, principals.size() );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put( XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml" );
        db = new XMLUserDatabase();
        subject = new Subject();
        try
        {
            db.initialize( null, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

}