/*
 * (C) Janne Jalkanen 2005
 * 
 */

package com.ecyrd.jspwiki.action;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import net.sourceforge.stripes.mock.MockHttpSession;
import net.sourceforge.stripes.mock.MockRoundtrip;

import com.ecyrd.jspwiki.*;

public class WikiActionBeanFactoryTest extends TestCase
{
    TestEngine testEngine;
    WikiActionBeanFactory resolver;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( WikiEngine.PROP_MATCHPLURALS, "yes" );
        testEngine = new TestEngine( props );
        resolver = testEngine.getWikiActionBeanFactory();
        testEngine.saveText( "SinglePage", "This is a test." );
        testEngine.saveText( "PluralPages", "This is a test." );
    }
    
    protected void tearDown() throws Exception
    {
        testEngine.deletePage( "TestPage" );
    }
    
    public void testNewActionBean() throws WikiException
    {
        WikiActionBean bean;
        MockRoundtrip trip = testEngine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        
        // Supplying an EditActionBean means the EDIT action
        bean = resolver.newActionBean( request, response, EditActionBean.class );
        assertEquals( WikiContext.EDIT, bean.getRequestContext() );
        assertNull( ((WikiContext)bean).getPage() );
        
        // Change the context to "preview"
        bean.setRequestContext( WikiContext.PREVIEW );
        assertEquals( WikiContext.PREVIEW, bean.getRequestContext() );
        
        // Change the context to "diff"
        bean.setRequestContext( WikiContext.DIFF);
        assertEquals( WikiContext.DIFF, bean.getRequestContext() );
        
        // Try changing the context to "comment" (but, this is an error)
        try
        {
            bean.setRequestContext( WikiContext.COMMENT);
        }
        catch ( IllegalArgumentException e )
        {
            // Excellent. This what we expect.
        }
        
        // Supplying the PrefsActionBean means the PREFS context
        bean = resolver.newActionBean( request, response, UserPreferencesActionBean.class );
        assertEquals( WikiContext.PREFS, bean.getRequestContext() );
        
        // Supplying the GroupActionBean means the VIEW_GROUP context
        bean = resolver.newActionBean( request, response, GroupActionBean.class );
        assertEquals( WikiContext.VIEW_GROUP, bean.getRequestContext() );
        assertNull( ((GroupActionBean)bean).getGroup() );
    }
    
    public void testNewActionBeanByJSP() throws WikiException
    {
        WikiActionBean bean;
        MockRoundtrip trip = testEngine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();

        // Request for "UserPreference.jsp" should resolve to PREFS action
        request = new MockHttpServletRequest( testEngine.getServletContext().getServletContextName(), "/UserPreferences.jsp");
        request.setSession( session );
        bean = resolver.newActionBean( request, response, UserPreferencesActionBean.class );
        assertEquals( WikiContext.PREFS, bean.getRequestContext() );
        
        // We don't care about JSPs not mapped to actions, because the bean we get only depends on the class we pass
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( testEngine.getServletContext().getServletContextName(), "/NonExistent.jsp");
        request.setSession( session );
        bean = resolver.newActionBean( request, response, EditActionBean.class );
        assertEquals( WikiContext.EDIT, bean.getRequestContext() );
        assertNull( ((WikiContext)bean).getPage() );
    }
    
    public void testActionBeansWithParams() throws Exception
    {
        WikiActionBean bean;
        WikiPage page = testEngine.getPage( "SinglePage" );
        MockRoundtrip trip = testEngine.guestTrip( ViewActionBean.class );
        MockHttpServletRequest request = trip.getRequest();
        MockHttpServletResponse response = trip.getResponse();
        MockHttpSession session = (MockHttpSession)request.getSession();
        
        // Passing an EDIT request with page param yields an ActionBean with a non-null page property
        request = new MockHttpServletRequest( testEngine.getServletContext().getServletContextName(), "/Edit.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"SinglePage"} );
        bean = resolver.newActionBean( request, response, EditActionBean.class );
        assertEquals( WikiContext.EDIT, bean.getRequestContext() );
        assertEquals( page, ((WikiContext)bean).getPage());
        
        // Passing a VIEW request with page=FindPage yields an ordinary page name, not a special page or JSP
        // FIXME: this won't work because WikiActionBeanResolver doesn't keep a cache of URLBindings 
        request = new MockHttpServletRequest( testEngine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "page", new String[]{"FindPage"} );
        bean = resolver.newActionBean( request, response, ViewActionBean.class );
        assertEquals( WikiContext.VIEW, bean.getRequestContext() );
        
        // Passing a VIEW_GROUP request with group="Art" gets a ViewGroupActionBean
        request = new MockHttpServletRequest( testEngine.getServletContext().getServletContextName(), "/Wiki.jsp");
        request.setSession( session );
        request.getParameterMap().put( "group", new String[]{"Art"} );
        bean = resolver.newActionBean( request, response, GroupActionBean.class );
        assertEquals( WikiContext.VIEW_GROUP, bean.getRequestContext() );
        assertEquals( "/Group.jsp", bean.getClass().getAnnotation(UrlBinding.class).value() );
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
        return new TestSuite( WikiActionBeanFactoryTest.class );
    }
}
