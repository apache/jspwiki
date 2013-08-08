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

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiSession;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.Users;
import org.apache.wiki.providers.WikiPageProvider;

public class IfPluginTest extends TestCase
{
    
    TestEngine testEngine;
    
    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        Properties props = TestEngine.getTestProperties();
        testEngine = new TestEngine( props );
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
        
        testEngine.deletePage( "Test" );
    }
    
    /**
     * Returns a {@link WikiContext} for the given page, with user {@link Users#JANNE} logged in.
     * 
     * @param page given {@link WikiPage}.
     * @return {@link WikiContext} associated to given {@link WikiPage}.
     * @throws WikiException problems while logging in.
     */
    WikiContext getJanneBasedWikiContextFor( WikiPage page ) throws WikiException 
    {
        MockHttpServletRequest request = testEngine.newHttpRequest();
        WikiSession session =  WikiSession.getWikiSession( testEngine, request );
        testEngine.getAuthenticationManager().login( session, 
                                                     request,
                                                     Users.JANNE,
                                                     Users.JANNE_PASS );
        
        return new WikiContext( testEngine, request, page );
    }
    
    /**
     * Checks that user access is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginUserAllowed() throws WikiException 
    {
        String src = "[{IfPlugin user='Janne Jalkanen'\n" +
        		     "\n" +
        		     "Content visible for Janne Jalkanen}]";
        String expected = "<p>Content visible for Janne Jalkanen</p>\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that user access is forbidden.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginUserNotAllowed() throws WikiException 
    {
        String src = "[{IfPlugin user='!Janne Jalkanen'\n" +
                     "\n" +
                     "Content NOT visible for Janne Jalkanen}]";
        String expected = "\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that IP address is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginIPAllowed() throws WikiException 
    {
        String src = "[{IfPlugin ip='127.0.0.1'\n" +
                     "\n" +
                     "Content visible for 127.0.0.1}]";
        String expected = "<p>Content visible for 127.0.0.1</p>\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    /**
     * Checks that IP address is granted.
     * 
     * @throws WikiException test failing.
     */
    public void testIfPluginIPNotAllowed() throws WikiException 
    {
        String src = "[{IfPlugin ip='!127.0.0.1'\n" +
                     "\n" +
                     "Content NOT visible for 127.0.0.1}]";
        String expected = "\n";
        
        testEngine.saveText( "Test", src );
        WikiPage page = testEngine.getPage( "Test", WikiPageProvider.LATEST_VERSION );
        WikiContext context = getJanneBasedWikiContextFor( page );
        
        String res = testEngine.getHTML( context, page );
        assertEquals( expected, res );
    }
    
    public static Test suite()
    {
        return new TestSuite( IfPluginTest.class );
    }
    
}
