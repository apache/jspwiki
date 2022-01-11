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

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.spi.Wiki;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class RecentChangesPluginTest {
    Properties props = TestEngine.getTestProperties();
    TestEngine testEngine = TestEngine.build( props );
    PluginManager manager = new DefaultPluginManager(testEngine, props);

    Context context;

    @BeforeEach
    public void setUp() throws Exception {
        testEngine.saveText("TestPage01", "Some Text for testing 01");
        testEngine.saveText("TestPage02", "Some Text for testing 02");
        testEngine.saveText("TestPage03", "Some Text for testing 03");
        testEngine.saveText("TestPage04", "Some Text for testing 04");
    }

    @AfterEach
    public void tearDown() {
        testEngine.deleteTestPage("TestPage01");
        testEngine.deleteTestPage("TestPage02");
        testEngine.deleteTestPage("TestPage03");
        testEngine.deleteTestPage("TestPage04");

        TestEngine.emptyWorkDir();
        CacheManager.getInstance().removeAllCaches();
    }

    /**
     * Plain test without parameters
     *
     * @throws Exception
     */
    @Test
    public void testSimple() throws Exception {
        context = Wiki.context().create(testEngine, Wiki.contents().page(testEngine, "TestPage01"));

        final String res = manager.execute(context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin}");

        // we don't want to compare the complete html returned, but check if
        // certain Strings are present and other Strings are not present
        Assertions.assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test with the include parameter
     *
     * @throws Exception
     */
    @Test
    public void testParmInClude() throws Exception {
        context = Wiki.context().create(testEngine, Wiki.contents().page(testEngine, "TestPage02"));

        final String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin include='TestPage02*'}" );

        Assertions.assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        Assertions.assertFalse(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        Assertions.assertFalse(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test with the exclude parameter
     *
     * @throws Exception
     */
    @Test
    public void testParmExClude() throws Exception {
        context = Wiki.context().create(testEngine, Wiki.contents().page(testEngine, "TestPage03"));

        final String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.RecentChangesPlugin exclude='TestPage03*'}" );

        Assertions.assertTrue(res.contains("<table class=\"recentchanges\" cellpadding=\"4\">"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage01\">Test Page 01</a>"));
        Assertions.assertTrue(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage02\">Test Page 02</a>"));
        Assertions.assertFalse(res.contains("<a href=\"/test/Wiki.jsp?page=TestPage03\">Test Page 03</a>"));
    }

    /**
     * Test an empty recent changes table
     *
     * @throws Exception
     */
    @Test
    public void testNoRecentChanges() throws Exception {
        context = Wiki.context().create(testEngine, Wiki.contents().page(testEngine, "TestPage04"));

        final String res = manager.execute( context, "{INSERT org.apache.wiki.plugin.RecentChangesPlugin since='-1'}" );

        Assertions.assertEquals( "<table class=\"recentchanges\" cellpadding=\"4\"></table>", res );
        Assertions.assertNotEquals( "<table class=\"recentchanges\" cellpadding=\"4\" />", res );
    }

}
