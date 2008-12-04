/*
 * (C) Janne Jalkanen 2005
 * 
 */

package com.ecyrd.jspwiki.action;
import java.util.Properties;

import org.apache.jspwiki.api.WikiException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;

import com.ecyrd.jspwiki.*;

public class WikiContextFactoryTest extends TestCase
{
    TestEngine m_engine;
    WikiContextFactory resolver;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( WikiEngine.PROP_MATCHPLURALS, "yes" );
        m_engine = new TestEngine( props );
        resolver = m_engine.getWikiContextFactory();
        m_engine.saveText( "SinglePage", "This is a test." );
        m_engine.saveText( "PluralPages", "This is a test." );
    }
    
    protected void tearDown() throws Exception
    {
        m_engine.deletePage( "TestPage" );
    }
    
    public void testNewActionBean() throws WikiException
    {
        WikiContext context;
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        
        // Supplying an EditActionBean means the EDIT action
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertNull( context.getPage() );
        
        // Change the context to "preview"
        context.setRequestContext( WikiContext.PREVIEW );
        assertEquals( WikiContext.PREVIEW, context.getRequestContext() );
        
        // Change the context to "diff"
        context.setRequestContext( WikiContext.DIFF);
        assertEquals( WikiContext.DIFF, context.getRequestContext() );
        
        // Try changing the context to "comment" (but, this is an error)
        try
        {
            context.setRequestContext( WikiContext.COMMENT);
        }
        catch ( IllegalArgumentException e )
        {
            // Excellent. This what we expect.
        }
        
        // Supplying the PrefsActionBean means the PREFS context
        context = resolver.newContext( request, response, WikiContext.PREFS );
        assertEquals( WikiContext.PREFS, context.getRequestContext() );
        
        // Supplying the GroupActionBean means the VIEW_GROUP context
        context = resolver.newContext( request, response, WikiContext.VIEW_GROUP );
        assertEquals( WikiContext.VIEW_GROUP, context.getRequestContext() );
    }
    
    public void testNewActionBeanByJSP() throws WikiException
    {
        WikiContext context;
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();

        // Request for "UserPreference.jsp" should resolve to PREFS action
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/UserPreferences.jsp");
        request.setSession( session );
        context = resolver.newContext( request, response, WikiContext.PREFS );
        assertEquals( WikiContext.PREFS, context.getRequestContext() );
        
        // We don't care about JSPs not mapped to actions, because the bean we get only depends on the class we pass
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/NonExistent.jsp");
        request.setSession( session );
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertNull( context.getPage() );
    }
    
    public void testActionBeansWithParams() throws Exception
    {
        WikiContext context;
        WikiPage page = m_engine.getPage( "SinglePage" );
        MockRoundtrip trip = m_engine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();
        
        // Passing an EDIT request with page param yields an ActionBean with a non-null page property
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Edit.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"SinglePage"} );
        context = resolver.newContext( request, response, WikiContext.EDIT );
        assertEquals( WikiContext.EDIT, context.getRequestContext() );
        assertEquals( page, context.getPage());
        
        // Passing a VIEW request with page=FindPage yields an ordinary page name, not a special page or JSP
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"FindPage"} );
        context = resolver.newContext( request, response, WikiContext.VIEW );
        assertEquals( WikiContext.VIEW, context.getRequestContext() );
        
        // Passing a VIEW_GROUP request with group="Art" gets a ViewGroupActionBean
        request = new MockHttpServletRequest( m_engine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "group", new String[]{"Art"} );
        context = resolver.newContext( request, response, WikiContext.VIEW_GROUP );
        assertEquals( WikiContext.VIEW_GROUP, context.getRequestContext() );
    }
    
    public void testFinalPageName() throws Exception
    {
        String page;
        page = resolver.getFinalPageName( "SinglePage" );
        assertEquals( "SinglePage", page );
        page = resolver.getFinalPageName( "SinglePages" );
        assertEquals( "SinglePage", page );
        
        page = resolver.getFinalPageName( "PluralPages" );
        assertEquals( "PluralPages", page );
        page = resolver.getFinalPageName( "PluralPage" );
        assertEquals( "PluralPages", page );
        
        page = resolver.getFinalPageName( "NonExistentPage" );
        assertNull( page );
    }
    
    public void testSpecialPageReference()
    {
        String url;
        url = resolver.getSpecialPageReference( "RecentChanges" );
        assertEquals( "RecentChanges.jsp", url );
        
        url = resolver.getSpecialPageReference( "FindPage" );
        assertEquals( "Search.jsp", url );
        
        // UserPrefs doesn't exist in our test properties
        url = resolver.getSpecialPageReference( "UserPrefs" );
        assertNull( url );
    }

    public static Test suite()
    {
        return new TestSuite( WikiContextFactoryTest.class );
    }
}
