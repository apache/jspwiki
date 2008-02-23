package com.ecyrd.jspwiki.auth.login;

import java.security.Principal;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TestAuthorizer;
import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.action.ViewActionBean;
import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.XMLUserDatabase;

/**
 * @author Andrew R. Jaquith
 */
public class WebContainerLoginModuleTest extends TestCase
{
    Authorizer authorizer;

    UserDatabase db;

    Subject      subject;

    private TestEngine m_engine;

    public final void testLogin()
    {
        Principal principal = new WikiPrincipal( "Andrew Jaquith" );
        Principal wrapper = new PrincipalWrapper( principal );
        MockHttpServletRequest request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        request.setUserPrincipal( principal );
        try
        {
            // Test using Principal (WebContainerLoginModule succeeds)
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue(  principals.contains( wrapper ) );
            assertFalse( principals.contains( Role.ANONYMOUS ) );
            assertFalse( principals.contains( Role.ASSERTED ) );
            assertTrue(  principals.contains( Role.AUTHENTICATED ) );
            assertTrue(  principals.contains( Role.ALL ) );

            // Test using remote user (WebContainerLoginModule succeeds)
            subject = new Subject();
            request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
            request.setUserPrincipal( new WikiPrincipal( "Andrew Jaquith" ) );
            handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue(  principals.contains( wrapper ) );
            assertFalse( principals.contains( Role.ANONYMOUS ) );
            assertFalse( principals.contains( Role.ASSERTED ) );
            assertTrue(  principals.contains( Role.AUTHENTICATED ) );
            assertTrue(  principals.contains( Role.ALL ) );

            // Test using IP address (AnonymousLoginModule succeeds)
            subject = new Subject();
            request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
            handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertFalse( principals.contains( principal ) );
            assertTrue(  principals.contains( Role.ANONYMOUS ) );
            assertFalse( principals.contains( Role.ASSERTED ) );
            assertFalse( principals.contains( Role.AUTHENTICATED ) );
            assertTrue(  principals.contains( Role.ALL ) );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLoginWithRoles() throws Exception
    {
        // Create user with 2 container roles; TestAuthorizer knows about these
        Principal principal = new WikiPrincipal( "Andrew Jaquith" );
        Principal wrapper = new PrincipalWrapper( principal );
        MockHttpServletRequest request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        request.setUserPrincipal( principal );
        Set<String> roles = new HashSet<String>();
        roles.add( "IT" );
        roles.add( "Engineering" );
        request.setRoles( roles );

        // Test using Principal (WebContainerLoginModule succeeds)
        CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
        LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
        context.login();
        Set principals = subject.getPrincipals();
        assertEquals( 5, principals.size() );
        assertTrue( principals.contains( wrapper ) );
        assertFalse( principals.contains( Role.ANONYMOUS ) );
        assertFalse( principals.contains( Role.ASSERTED ) );
        assertTrue(  principals.contains( Role.AUTHENTICATED ) );
        assertTrue(  principals.contains( Role.ALL ) );
        assertTrue(  principals.contains( new Role( "IT" ) ) );
        assertTrue(  principals.contains( new Role( "Engineering" ) ) );
    }

    public final void testLogout()
    {
        Principal principal = new WikiPrincipal( "Andrew Jaquith" );
        Principal wrapper = new PrincipalWrapper( principal );
        MockHttpServletRequest request = m_engine.guestTrip( ViewActionBean.class ).getRequest();
        request.setUserPrincipal( principal );
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( m_engine, request, authorizer );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( wrapper ) );
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