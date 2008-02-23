package com.ecyrd.jspwiki.auth.login;

import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.http.Cookie;

import junit.framework.TestCase;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TestAuthorizer;
import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.action.ViewActionBean;
import com.ecyrd.jspwiki.auth.AuthenticationManager;
import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.XMLUserDatabase;

/**
 * @author Andrew R. Jaquith
 */
public class CookieAssertionLoginModuleTest extends TestCase
{
    Authorizer authorizer;

    UserDatabase db;

    Subject      subject;

    private TestEngine m_engine;

    public final void testLogin()
    {
        MockHttpServletRequest request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        try
        {
            // We can use cookies right?
            assertTrue( AuthenticationManager.allowsCookieAssertions() );

            // Test using Cookie and IP address (AnonymousLoginModule succeeds)
            Cookie cookie = new Cookie( CookieAssertionLoginModule.PREFS_COOKIE_NAME, "Bullwinkle" );
            request.setCookies( new Cookie[]
            { cookie } );
            subject = new Subject();
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "Bullwinkle" ) ) );
            assertTrue( principals.contains( Role.ASSERTED ) );
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
        MockHttpServletRequest request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( new WikiPrincipal( "127.0.0.1" ) ) );  // Stripes mock requests always return this
            assertTrue( principals.contains( Role.ANONYMOUS ) );
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
        props.load( TestEngine.findTestProperties() );
        props.put(XMLUserDatabase.PROP_USERDATABASE, "tests/etc/userdatabase.xml");
        m_engine = new TestEngine(props);
        authorizer = new TestAuthorizer();
        authorizer.initialize( m_engine, props );
        db = new XMLUserDatabase();
        subject = new Subject();
        try
        {
            db.initialize( m_engine, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

}