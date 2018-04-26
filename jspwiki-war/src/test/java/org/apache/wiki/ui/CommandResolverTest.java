/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
/*
 * (C) Janne Jalkanen 2005
 *
 */
package org.apache.wiki.ui;

import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.GroupPrincipal;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sourceforge.stripes.mock.MockHttpServletRequest;


public class CommandResolverTest
{
    TestEngine m_engine;
    CommandResolver resolver;

    @Before
    public void setUp() throws Exception
    {
        Properties props = TestEngine.getTestProperties();
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "yes" );
        m_engine = new TestEngine( props );
        resolver = m_engine.getCommandResolver();
        m_engine.saveText( "SinglePage", "This is a test." );
        m_engine.saveText( "PluralPages", "This is a test." );
    }

    @After
    public void tearDown() throws Exception
    {
        m_engine.deletePage( "TestPage" );
        m_engine.deletePage( "SinglePage" );
        m_engine.deletePage( "PluralPage" );
    }

    @Test
    public void testFindStaticWikiAction()
    {
        Command a;

        // If we look for action with "edit" request context, we get EDIT action
        a = CommandResolver.findCommand( WikiContext.EDIT );
        Assert.assertEquals( PageCommand.EDIT, a );
        Assert.assertEquals( WikiContext.EDIT, a.getRequestContext() );

        // Ditto for prefs context
        a = CommandResolver.findCommand( WikiContext.PREFS );
        Assert.assertEquals( WikiCommand.PREFS, a );
        Assert.assertEquals( WikiContext.PREFS, a.getRequestContext() );

        // Ditto for group view context
        a = CommandResolver.findCommand( WikiContext.VIEW_GROUP );
        Assert.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assert.assertEquals( WikiContext.VIEW_GROUP, a.getRequestContext() );

        // Looking for non-existent context; should result in exception
        try
        {
            a = CommandResolver.findCommand( "nonExistentContext" );
            Assert.assertFalse( "Context supported, strangely...", true );
        }
        catch ( IllegalArgumentException e )
        {
            // Good; this is what we expect
        }
    }

    @Test
    public void testFindWikiActionNoParams()
    {
        Command a;
        MockHttpServletRequest request = m_engine.newHttpRequest( "" );

        // Passing an EDIT request with no explicit page params means the EDIT action
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( PageCommand.EDIT, a );
        Assert.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assert.assertEquals( "Edit.jsp", a.getJSP() );
        Assert.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assert.assertNull( a.getTarget() );

        // Ditto for prefs context
        a = resolver.findCommand( request, WikiContext.PREFS );
        Assert.assertEquals( WikiCommand.PREFS, a );
        Assert.assertNull( a.getTarget() );

        // Ditto for group view context
        a = resolver.findCommand( request, WikiContext.VIEW_GROUP );
        Assert.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assert.assertNull( a.getTarget() );

        // Looking for non-existent context; should result in exception
        try
        {
            a = resolver.findCommand( request, "nonExistentContext" );
            Assert.assertFalse( "Context supported, strangely...", true );
        }
        catch ( IllegalArgumentException e )
        {
            // Good; this is what we expect
        }

        // Request for "UserPreference.jsp" should resolve to PREFS action
        request = m_engine.newHttpRequest( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( WikiCommand.PREFS, a );
        Assert.assertNull( a.getTarget() );

        // Request for "NewGroup.jsp" should resolve to CREATE_GROUP action
        // but targeted at the wiki
        request = m_engine.newHttpRequest( "/NewGroup.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertNotSame( WikiCommand.CREATE_GROUP, a );
        Assert.assertEquals( WikiCommand.CREATE_GROUP.getRequestContext(), a.getRequestContext() );
        Assert.assertEquals( m_engine.getApplicationName(), a.getTarget() );

        // But request for JSP not mapped to action should get default
        request = m_engine.newHttpRequest( "/NonExistent.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( PageCommand.EDIT, a );
        Assert.assertNull( a.getTarget() );
    }

    @Test
    public void testFindWikiActionWithParams() throws Exception
    {
        Command a;
        WikiPage page = m_engine.getPage( "SinglePage" );

        // Passing an EDIT request with page param yields a wrapped action
        MockHttpServletRequest request = m_engine.newHttpRequest( "/Edit.jsp?page=SinglePage" );
        request.getParameterMap().put( "page", new String[]{ "SinglePage" } );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertNotSame( PageCommand.EDIT, a );
        Assert.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assert.assertEquals( "Edit.jsp", a.getJSP() );
        Assert.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assert.assertEquals( page, a.getTarget() );

        // Passing an EDIT request with page=Search yields FIND action, *not* edit
        request.setContextPath( "/Edit.jsp?page=Search" );
        request.getParameterMap().put( "page", new String[]{ "Search" } );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( WikiCommand.FIND, a );
        Assert.assertEquals( "FindContent.jsp", a.getContentTemplate() );
        Assert.assertEquals( "Search.jsp", a.getJSP() );
        Assert.assertEquals( "%uSearch.jsp", a.getURLPattern() );
        Assert.assertNull( a.getTarget() );

        // Passing an EDIT request with group="Foo" yields wrapped VIEW_GROUP
        request = m_engine.newHttpRequest( "/Group.jsp?group=Foo" );
        request.getParameterMap().put( "group", new String[]{ "Foo" } );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertNotSame( GroupCommand.VIEW_GROUP, a );
        Assert.assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        Assert.assertEquals( "Group.jsp", a.getJSP() );
        Assert.assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        Assert.assertEquals( new GroupPrincipal( "Foo" ), a.getTarget() );
    }

    @Test
    public void testFindWikiActionWithPath()
    {
        MockHttpServletRequest request;
        Command a;

        // Passing an EDIT request with View JSP yields EDIT of the Front page
        request = m_engine.newHttpRequest( "/Wiki.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertNotNull( a.getTarget() );
        Assert.assertEquals( ((WikiPage)a.getTarget()).getName(), m_engine.getFrontPage() );

        // Passing an EDIT request with Group JSP yields VIEW_GROUP
        request = m_engine.newHttpRequest( "/Group.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assert.assertNull( a.getTarget() );

        // Passing an EDIT request with UserPreferences JSP yields PREFS
        request = m_engine.newHttpRequest( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assert.assertEquals( WikiCommand.PREFS, a );
        Assert.assertNull( a.getTarget() );
    }

    @Test
    public void testFinalPageName() throws Exception
    {
        String page;
        page = resolver.getFinalPageName( "SinglePage" );
        Assert.assertEquals( "SinglePage", page );
        page = resolver.getFinalPageName( "SinglePages" );
        Assert.assertEquals( "SinglePage", page );

        page = resolver.getFinalPageName( "PluralPages" );
        Assert.assertEquals( "PluralPages", page );
        page = resolver.getFinalPageName( "PluralPage" );
        Assert.assertEquals( "PluralPages", page );

        page = resolver.getFinalPageName( "NonExistentPage" );
        Assert.assertNull( page );
    }

    @Test
    public void testSpecialPageReference()
    {
        String url;
        url = resolver.getSpecialPageReference( "RecentChanges" );
        Assert.assertEquals( "/test/RecentChanges.jsp", url );

        url = resolver.getSpecialPageReference( "Search" );
        Assert.assertEquals( "/test/Search.jsp", url );

        // UserPrefs doesn't exist in our test properties
        url = resolver.getSpecialPageReference( "UserPrefs" );
        Assert.assertNull( url );
    }

}
