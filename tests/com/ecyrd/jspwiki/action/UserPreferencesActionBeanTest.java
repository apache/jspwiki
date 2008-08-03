package com.ecyrd.jspwiki.action;

import java.util.Properties;

import javax.servlet.http.Cookie;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.login.CookieAssertionLoginModule;

public class UserPreferencesActionBeanTest extends TestCase
{
    TestEngine m_engine;
    
    public void setUp()
    {
        // Start the WikiEngine, and stash reference
        Properties props = new Properties();
        try 
        {
            props.load( TestEngine.findTestProperties() );
            m_engine = new TestEngine( props );
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not set up TestEngine: " + e.getMessage());
        }
    }
    
    public void testCreateAssertedName() throws Exception
    {
        MockServletContext ctx = (MockServletContext)m_engine.getServletContext();
        MockRoundtrip trip;
        UserPreferencesActionBean bean;
        
        // Create session; set 'assertion' param; verify it got saved
        trip = new MockRoundtrip(ctx, "/UserPreferences.jsp");
        trip.setParameter("assertedName", "MyAssertedIdentity");
        trip.setParameter("createAssertedName", "true");
        trip.execute();
        bean = trip.getActionBean(UserPreferencesActionBean.class);
        assertEquals("/", trip.getDestination());
        
        // Verify that the asserted name cookie is present in the Response
        MockHttpServletResponse response = (MockHttpServletResponse)bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        Cookie cookie = cookies[0];
        assertEquals(CookieAssertionLoginModule.PREFS_COOKIE_NAME,cookie.getName());
        assertEquals("MyAssertedIdentity",cookie.getValue());
    }
    
    public void testCreateAssertedNameAfterLogin() throws Exception
    {
        MockServletContext ctx = (MockServletContext)m_engine.getServletContext();
        MockRoundtrip trip;
        UserPreferencesActionBean bean;
        
        // Create session; login in as Janne
        trip = new MockRoundtrip(ctx, "/UserPreferences.jsp");
        MockHttpServletRequest request = trip.getRequest();
        WikiSession wikiSession = WikiSession.getWikiSession(m_engine, request);
        boolean login = m_engine.getAuthenticationManager().login(wikiSession, Users.JANNE,Users.JANNE_PASS);
        assertTrue("Could not log in.", login);
        
        // Set 'assertion' param; verify redirect to front page
        trip.setParameter("assertedName", "MyAssertedIdentity");
        trip.setParameter("createAssertedName", "true");
        trip.execute();
        bean = trip.getActionBean(UserPreferencesActionBean.class);
        assertEquals("/", trip.getDestination());
        
        // Verify that the asserted name cookie is NOT present in the Response
        // (authenticated users cannot set the assertion cookie)
        MockHttpServletResponse response = (MockHttpServletResponse)bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals(0, cookies.length);
    }
    
    public void testClearAssertedName() throws Exception
    {
        MockServletContext ctx = (MockServletContext)m_engine.getServletContext();
        MockRoundtrip trip;
        UserPreferencesActionBean bean;
        
        // Create session; set 'assertion' param; verify it got saved
        trip = new MockRoundtrip(ctx, "/UserPreferences.jsp");
        MockHttpServletRequest request = trip.getRequest();
        Cookie cookie = new Cookie(CookieAssertionLoginModule.PREFS_COOKIE_NAME, "MyAssertedIdentity");
        request.setCookies(new Cookie[]{cookie});
        trip.setParameter("clearAssertedName", "true");
        trip.execute();
        bean = trip.getActionBean(UserPreferencesActionBean.class);
        assertEquals("/Logout.jsp", trip.getDestination());
        
        // Verify that the asserted name cookie is gone from the Response
        MockHttpServletResponse response = (MockHttpServletResponse)bean.getContext().getResponse();
        Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        cookie = cookies[0];
        assertEquals(CookieAssertionLoginModule.PREFS_COOKIE_NAME,cookie.getName());
        assertEquals("",cookie.getValue());
    }
    
    public static Test suite()
    {
        return new TestSuite( UserPreferencesActionBeanTest.class );
    }
    
}
