/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.ui;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.GroupPrincipal;

public class CommandResolverTest extends TestCase
{
    TestEngine testEngine;
    CommandResolver resolver;

    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.put( WikiEngine.PROP_MATCHPLURALS, "yes" );
        testEngine = new TestEngine( props );
        resolver = testEngine.getPageResolver();
        testEngine.saveText( "SinglePage", "This is a test." );
        testEngine.saveText( "PluralPages", "This is a test." );
    }
    
    protected void tearDown() throws Exception
    {
        testEngine.deletePage( "TestPage" );
    }
    
    public void testFindStaticWikiAction()
    {
        Command a;
        
        // If we look for action with "edit" request context, we get EDIT action
        a = CommandResolver.findCommand( WikiContext.EDIT );
        assertEquals( PageCommand.EDIT, a );
        assertEquals( WikiContext.EDIT, a.getRequestContext() );
        
        // Ditto for prefs context
        a = CommandResolver.findCommand( WikiContext.PREFS );
        assertEquals( WikiCommand.PREFS, a );
        assertEquals( WikiContext.PREFS, a.getRequestContext() );
        
        // Ditto for group view context
        a = CommandResolver.findCommand( WikiContext.VIEW_GROUP );
        assertEquals( GroupCommand.VIEW_GROUP, a );
        assertEquals( WikiContext.VIEW_GROUP, a.getRequestContext() );
        
        // Looking for non-existent context; should result in exception
        try
        {
            a = CommandResolver.findCommand( "nonExistentContext" );
            assertFalse( "Context supported, strangely...", true );
        }
        catch ( IllegalArgumentException e )
        {
            // Good; this is what we expect
        }
    }
    
    public void testFindWikiActionNoParams()
    {
        Command a;
        TestHttpServletRequest request = new TestHttpServletRequest();
        
        // Passing an EDIT request with no explicit page params means the EDIT action
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( PageCommand.EDIT, a );
        assertEquals( "EditContent.jsp", a.getContentTemplate() );
        assertEquals( "Edit.jsp", a.getJSP() );
        assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        assertNull( a.getTarget() );
        
        // Ditto for prefs context
        a = resolver.findCommand( request, WikiContext.PREFS );
        assertEquals( WikiCommand.PREFS, a );
        assertNull( a.getTarget() );
        
        // Ditto for group view context
        a = resolver.findCommand( request, WikiContext.VIEW_GROUP );
        assertEquals( GroupCommand.VIEW_GROUP, a );
        assertNull( a.getTarget() );
        
        // Looking for non-existent context; should result in exception
        try
        {
            a = resolver.findCommand( request, "nonExistentContext" );
            assertFalse( "Context supported, strangely...", true );
        }
        catch ( IllegalArgumentException e )
        {
            // Good; this is what we expect
        }
        
        // Request for "UserPreference.jsp" should resolve to PREFS action
        request.setServletPath( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( WikiCommand.PREFS, a );
        assertNull( a.getTarget() );
        
        // Request for "NewGroup.jsp" should resolve to CREATE_GROUP action
        request.setServletPath( "/NewGroup.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( WikiCommand.CREATE_GROUP, a );
        assertNull( a.getTarget() );
        
        // But request for JSP not mapped to action should get default
        request.setServletPath( "/NonExistent.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( PageCommand.EDIT, a );
        assertNull( a.getTarget() );
    }
    
    public void testFindWikiActionWithParams() throws Exception
    {
        Command a;
        WikiPage page = testEngine.getPage( "SinglePage" );
        
        // Passing an EDIT request with page param yields a wrapped action
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setParameter( "page", "SinglePage" );
        request.setServletPath( "/Edit.jsp?page=SinglePage" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertNotSame( PageCommand.EDIT, a );
        assertEquals( "EditContent.jsp", a.getContentTemplate() );
        assertEquals( "Edit.jsp", a.getJSP() );
        assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        assertEquals( page, a.getTarget() );
        
        // Passing an EDIT request with page=FindPage yields FIND action, *not* edit
        request.setParameter( "page", "FindPage" );
        request.setServletPath( "/Edit.jsp?page=FindPage" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( WikiCommand.FIND, a );
        assertEquals( "FindContent.jsp", a.getContentTemplate() );
        assertEquals( "Search.jsp", a.getJSP() );
        assertEquals( "%uSearch.jsp", a.getURLPattern() );
        assertNull( a.getTarget() );
        
        // Passing an EDIT request with group="Foo" yields wrapped VIEW_GROUP
        request = new TestHttpServletRequest();
        request.setParameter( "group", "Foo" );
        request.setServletPath( "/Group.jsp?group=Foo" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertNotSame( GroupCommand.VIEW_GROUP, a );
        assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        assertEquals( "Group.jsp", a.getJSP() );
        assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        assertEquals( new GroupPrincipal( "JSPWiki", "Foo" ), a.getTarget() );
    }
    
    public void testFindWikiActionWithPath()
    {
        TestHttpServletRequest request;
        Command a;
        
        // Passing an EDIT request with View JSP yields VIEW
        request = new TestHttpServletRequest();
        request.setServletPath( "/Wiki.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( PageCommand.VIEW, a );
        assertNull( a.getTarget() );
        
        // Passing an EDIT request with Group JSP yields VIEW_GROUP
        request.setServletPath( "/Group.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( GroupCommand.VIEW_GROUP, a );
        assertNull( a.getTarget() );
        
        // Passing an EDIT request with UserPreferences JSP yields PREFS
        request.setServletPath( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        assertEquals( WikiCommand.PREFS, a );
        assertNull( a.getTarget() );
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
        assertEquals( "http://localhost/RecentChanges.jsp", url );
        
        url = resolver.getSpecialPageReference( "FindPage" );
        assertEquals( "http://localhost/Search.jsp", url );
        
        url = resolver.getSpecialPageReference( "FindPage" );
        assertEquals( "http://localhost/Search.jsp", url );
        
        // UserPrefs doesn't exist in our test properties
        url = resolver.getSpecialPageReference( "UserPrefs" );
        assertNull( url );
    }

    public static Test suite()
    {
        return new TestSuite( CommandResolverTest.class );
    }
}
