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
import net.sf.ehcache.CacheManager;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;

public class RecentChangesPluginTest extends TestCase {
    Properties props = TestEngine.getTestProperties();

    TestEngine testEngine;

    WikiContext context;

    PluginManager manager;

    public void setUp() throws Exception {
        CacheManager.getInstance().removeAllCaches();
        testEngine = new TestEngine(props);

        testEngine.saveText("TestPage01", "Some Text for testing 01");
        testEngine.saveText("TestPage02", "Some Text for testing 02");
        testEngine.saveText("TestPage03", "Some Text for testing 03");
        testEngine.saveText("TestPage04", "Some Text for testing 04");

        manager = new DefaultPluginManager(testEngine, props);
    }

    public void tearDown() {
        testEngine.deleteTestPage("TestPage01");
        testEngine.deleteTestPage("TestPage02");
        testEngine.deleteTestPage("TestPage03");
        testEngine.deleteTestPage("TestPage04");

        TestEngine.emptyWorkDir();
    }

    /**
     * Plain test without parameters
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        context = new WikiContext(testEngine, new WikiPage(testEngine, "TestPage01"));

        String res = manager.execute(context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin}");

        // we don't want to compare the complete html returned, but check if
        // certain Strings are present and other Strings are not present
        assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test with the include parameter
     * 
     * @throws Exception
     */
    public void testParmInClude() throws Exception {
        context = new WikiContext(testEngine, new WikiPage(testEngine, "TestPage02"));

        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin include='TestPage02*'}" );

        assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        assertFalse(res.contains("<a href=\"/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        assertFalse(res.contains("<a href=\"/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test with the exclude parameter
     * 
     * @throws Exception
     */
    public void testParmExClude() throws Exception {
        context = new WikiContext(testEngine, new WikiPage(testEngine, "TestPage03"));

        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin exclude='TestPage03*'}" );

        assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        assertTrue(res.contains("<a href=\"/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        assertFalse(res.contains("<a href=\"/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test an empty recent changes table
     * 
     * @throws Exception
     */
    public void testNoRecentChanges() throws Exception {
        context = new WikiContext(testEngine, new WikiPage(testEngine, "TestPage04"));

        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin since='-1'}" );

        assertTrue( "<table class=\"recentchanges\" cellpadding=\"4\"></table>".equals( res ) );
        assertFalse( "<table class=\"recentchanges\" cellpadding=\"4\" />".equals( res ) );
    }

    public static Test suite() {
        return new TestSuite( RecentChangesPluginTest.class );
    }
    
}
