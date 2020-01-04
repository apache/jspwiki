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

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.GroupPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;


public class CommandResolverTest {
    TestEngine m_engine;
    CommandResolver resolver;

    @BeforeEach
    public void setUp() throws Exception {
        final Properties props = TestEngine.getTestProperties();
        props.setProperty( WikiEngine.PROP_MATCHPLURALS, "yes" );
        m_engine = new TestEngine( props );
        resolver = m_engine.getCommandResolver();
        m_engine.saveText( "SinglePage", "This is a test." );
        m_engine.saveText( "PluralPages", "This is a test." );
    }

    @AfterEach
    public void tearDown() throws Exception {
        m_engine.getPageManager().deletePage( "SinglePage" );
        m_engine.getPageManager().deletePage( "PluralPage" );
    }

    @Test
    public void testFindStaticWikiAction() {
        // If we look for action with "edit" request context, we get EDIT action
        Command a = CommandResolver.findCommand( WikiContext.EDIT );
        Assertions.assertEquals( PageCommand.EDIT, a );
        Assertions.assertEquals( WikiContext.EDIT, a.getRequestContext() );

        // Ditto for prefs context
        a = CommandResolver.findCommand( WikiContext.PREFS );
        Assertions.assertEquals( WikiCommand.PREFS, a );
        Assertions.assertEquals( WikiContext.PREFS, a.getRequestContext() );

        // Ditto for group view context
        a = CommandResolver.findCommand( WikiContext.VIEW_GROUP );
        Assertions.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assertions.assertEquals( WikiContext.VIEW_GROUP, a.getRequestContext() );

        // Looking for non-existent context; should result in exception
        Assertions.assertThrows( IllegalArgumentException.class, () -> CommandResolver.findCommand( "nonExistentContext" ) );
    }

    @Test
    public void testFindWikiActionNoParams() {
        MockHttpServletRequest request = m_engine.newHttpRequest( "" );

        // Passing an EDIT request with no explicit page params means the EDIT action
        Command a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( PageCommand.EDIT, a );
        Assertions.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assertions.assertEquals( "Edit.jsp", a.getJSP() );
        Assertions.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assertions.assertNull( a.getTarget() );

        // Ditto for prefs context
        a = resolver.findCommand( request, WikiContext.PREFS );
        Assertions.assertEquals( WikiCommand.PREFS, a );
        Assertions.assertNull( a.getTarget() );

        // Ditto for group view context
        a = resolver.findCommand( request, WikiContext.VIEW_GROUP );
        Assertions.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assertions.assertNull( a.getTarget() );

        Assertions.assertThrows( IllegalArgumentException.class, () -> resolver.findCommand( m_engine.newHttpRequest( "" ), "nonExistentContext" ) );

        // Request for "UserPreference.jsp" should resolve to PREFS action
        request = m_engine.newHttpRequest( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( WikiCommand.PREFS, a );
        Assertions.assertNull( a.getTarget() );

        // Request for "NewGroup.jsp" should resolve to CREATE_GROUP action
        // but targeted at the wiki
        request = m_engine.newHttpRequest( "/NewGroup.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertNotSame( WikiCommand.CREATE_GROUP, a );
        Assertions.assertEquals( WikiCommand.CREATE_GROUP.getRequestContext(), a.getRequestContext() );
        Assertions.assertEquals( m_engine.getApplicationName(), a.getTarget() );

        // But request for JSP not mapped to action should get default
        request = m_engine.newHttpRequest( "/NonExistent.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( PageCommand.EDIT, a );
        Assertions.assertNull( a.getTarget() );
    }

    @Test
    public void testFindWikiActionWithParams() {
        final WikiPage page = m_engine.getPageManager().getPage( "SinglePage" );

        // Passing an EDIT request with page param yields a wrapped action
        MockHttpServletRequest request = m_engine.newHttpRequest( "/Edit.jsp?page=SinglePage" );
        request.getParameterMap().put( "page", new String[]{ "SinglePage" } );
        Command a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertNotSame( PageCommand.EDIT, a );
        Assertions.assertEquals( "EditContent.jsp", a.getContentTemplate() );
        Assertions.assertEquals( "Edit.jsp", a.getJSP() );
        Assertions.assertEquals( "%uEdit.jsp?page=%n", a.getURLPattern() );
        Assertions.assertEquals( page, a.getTarget() );

        // Passing an EDIT request with page=Search yields FIND action, *not* edit
        request.setContextPath( "/Edit.jsp?page=Search" );
        request.getParameterMap().put( "page", new String[]{ "Search" } );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( WikiCommand.FIND, a );
        Assertions.assertEquals( "FindContent.jsp", a.getContentTemplate() );
        Assertions.assertEquals( "Search.jsp", a.getJSP() );
        Assertions.assertEquals( "%uSearch.jsp", a.getURLPattern() );
        Assertions.assertNull( a.getTarget() );

        // Passing an EDIT request with group="Foo" yields wrapped VIEW_GROUP
        request = m_engine.newHttpRequest( "/Group.jsp?group=Foo" );
        request.getParameterMap().put( "group", new String[]{ "Foo" } );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertNotSame( GroupCommand.VIEW_GROUP, a );
        Assertions.assertEquals( "GroupContent.jsp", a.getContentTemplate() );
        Assertions.assertEquals( "Group.jsp", a.getJSP() );
        Assertions.assertEquals( "%uGroup.jsp?group=%n", a.getURLPattern() );
        Assertions.assertEquals( new GroupPrincipal( "Foo" ), a.getTarget() );
    }

    @Test
    public void testFindWikiActionWithPath() {
        // Passing an EDIT request with View JSP yields EDIT of the Front page
        MockHttpServletRequest request = m_engine.newHttpRequest( "/Wiki.jsp" );
        Command a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertNotNull( a.getTarget() );
        Assertions.assertEquals( ((WikiPage)a.getTarget()).getName(), m_engine.getFrontPage() );

        // Passing an EDIT request with Group JSP yields VIEW_GROUP
        request = m_engine.newHttpRequest( "/Group.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( GroupCommand.VIEW_GROUP, a );
        Assertions.assertNull( a.getTarget() );

        // Passing an EDIT request with UserPreferences JSP yields PREFS
        request = m_engine.newHttpRequest( "/UserPreferences.jsp" );
        a = resolver.findCommand( request, WikiContext.EDIT );
        Assertions.assertEquals( WikiCommand.PREFS, a );
        Assertions.assertNull( a.getTarget() );
    }

    @Test
    public void testFinalPageName() throws Exception {
        String page = resolver.getFinalPageName( "SinglePage" );
        Assertions.assertEquals( "SinglePage", page );
        page = resolver.getFinalPageName( "SinglePages" );
        Assertions.assertEquals( "SinglePage", page );

        page = resolver.getFinalPageName( "PluralPages" );
        Assertions.assertEquals( "PluralPages", page );
        page = resolver.getFinalPageName( "PluralPage" );
        Assertions.assertEquals( "PluralPages", page );

        page = resolver.getFinalPageName( "NonExistentPage" );
        Assertions.assertNull( page );
    }

    @Test
    public void testSpecialPageReference() {
        String url = resolver.getSpecialPageReference( "RecentChanges" );
        Assertions.assertEquals( "/test/RecentChanges.jsp", url );

        url = resolver.getSpecialPageReference( "Search" );
        Assertions.assertEquals( "/test/Search.jsp", url );

        // UserPrefs doesn't exist in our test properties
        url = resolver.getSpecialPageReference( "UserPrefs" );
        Assertions.assertNull( url );
    }

}
