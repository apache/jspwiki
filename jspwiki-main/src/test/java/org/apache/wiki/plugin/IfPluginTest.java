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
package org.apache.wiki.plugin;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.auth.AuthenticationManager;
import org.apache.wiki.auth.Users;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IfPluginTest {

    TestEngine testEngine = TestEngine.build();

    @AfterEach
    public void tearDown() throws Exception {
        testEngine.getManager( PageManager.class ).deletePage( "Test" );
    }

    /**
     * Returns a {@link WikiContext} for the given page, with user {@link Users#JANNE} logged in.
     *
     * @param page given {@link Page}.
     * @return {@link WikiContext} associated to given {@link Page}.
     * @throws WikiException problems while logging in.
     */
    WikiContext getJanneBasedWikiContextFor( final Page page ) throws WikiException {
        final MockHttpServletRequest request = testEngine.newHttpRequest();
        final Session session =  WikiSession.getWikiSession( testEngine, request );
        testEngine.getManager( AuthenticationManager.class ).login( session, request, Users.JANNE, Users.JANNE_PASS );
        return new WikiContext( testEngine, request, page );
    }

    /**
     * Checks that user access is granted.
     *
     * @throws WikiException test Assertions.failing.
     */
    @Test
    public void testIfPluginUserAllowed() throws WikiException
    {
        final String src = "[{IfPlugin user='Janne Jalkanen'\n\nContent visible for Janne Jalkanen}]";
        final String expected = "<p>Content visible for Janne Jalkanen</p>\n";

        testEngine.saveText( "Test", src );
        final Page page = testEngine.getManager( PageManager.class ).getPage( "Test", PageProvider.LATEST_VERSION );
        final WikiContext context = getJanneBasedWikiContextFor( page );

        final String res = testEngine.getManager( RenderingManager.class ).getHTML( context, page );
        Assertions.assertEquals( expected, res );
    }

    /**
     * Checks that user access is forbidden.
     *
     * @throws WikiException test Assertions.failing.
     */
    @Test
    public void testIfPluginUserNotAllowed() throws WikiException
    {
        final String src = "[{IfPlugin user='!Janne Jalkanen'\n\nContent NOT visible for Janne Jalkanen}]";
        final String expected = "\n";

        testEngine.saveText( "Test", src );
        final Page page = testEngine.getManager( PageManager.class ).getPage( "Test", PageProvider.LATEST_VERSION );
        final WikiContext context = getJanneBasedWikiContextFor( page );

        final String res = testEngine.getManager( RenderingManager.class ).getHTML( context, page );
        Assertions.assertEquals( expected, res );
    }

    /**
     * Checks that IP address is granted.
     *
     * @throws WikiException test Assertions.failing.
     */
    @Test
    public void testIfPluginIPAllowed() throws WikiException {
        final String src = "[{IfPlugin ip='127.0.0.1'\n\nContent visible for 127.0.0.1}]";
        final String expected = "<p>Content visible for 127.0.0.1</p>\n";

        testEngine.saveText( "Test", src );
        final Page page = testEngine.getManager( PageManager.class ).getPage( "Test", PageProvider.LATEST_VERSION );
        final WikiContext context = getJanneBasedWikiContextFor( page );

        final String res = testEngine.getManager( RenderingManager.class ).getHTML( context, page );
        Assertions.assertEquals( expected, res );
    }

    /**
     * Checks that IP address is granted.
     *
     * @throws WikiException test Assertions.failing.
     */
    @Test
    public void testIfPluginIPNotAllowed() throws WikiException {
        final String src = "[{IfPlugin ip='!127.0.0.1'\n\nContent NOT visible for 127.0.0.1}]";
        final String expected = "\n";

        testEngine.saveText( "Test", src );
        final Page page = testEngine.getManager( PageManager.class ).getPage( "Test", PageProvider.LATEST_VERSION );
        final WikiContext context = getJanneBasedWikiContextFor( page );

        final String res = testEngine.getManager( RenderingManager.class ).getHTML( context, page );
        Assertions.assertEquals( expected, res );
    }

}
