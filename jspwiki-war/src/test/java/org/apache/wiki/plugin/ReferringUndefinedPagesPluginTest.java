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

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sf.ehcache.CacheManager;

public class ReferringUndefinedPagesPluginTest {
    Properties props = TestEngine.getTestProperties();

	TestEngine testEngine;

	WikiContext context;

	PluginManager manager;

    @BeforeEach
    public void setUp() throws Exception {
        CacheManager.getInstance().removeAllCaches();
		testEngine = new TestEngine(props);

        testEngine.saveText("TestPage01", "Some Text for testing 01 which refers [NonExistingPageA] ");
        testEngine.saveText("TestPage02", "Some Text for testing 02 which refers [NonExistingPageB] ");
		testEngine.saveText("TestPage03", "Some Text for testing 03 which refers [NonExistingPageC] ");

        context = new WikiContext( testEngine, testEngine.newHttpRequest(), new WikiPage(testEngine,"TestPage") );
        manager = new DefaultPluginManager( testEngine, props );	}

    @AfterEach
    public void tearDown() {
		testEngine.deleteTestPage("TestPage01");
		testEngine.deleteTestPage("TestPage02");
		testEngine.deleteTestPage("TestPage03");

		TestEngine.emptyWorkDir();
	}

	/**
	 * Plain test without parameters
	 *
	 * @throws Exception
	 */
    @Test
	public void testSimple() throws Exception {
		String res = manager.execute(context, "{INSERT ReferringUndefinedPagesPlugin}");
		Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
	}

	/**
	 * Test with the include parameter
	 *
	 * @throws Exception
	 */
    @Test
	public void testParmInClude() throws Exception {
		String res = manager.execute(context, "{INSERT ReferringUndefinedPagesPlugin} include='TestPage02*'}");
		Assertions.assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
		Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        Assertions.assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
	}

    /**
     * Test with the exclude parameter
     *
     * @throws Exception
     */
    @Test
    public void testParmExClude() throws Exception {
        String res = manager.execute(context,"{INSERT ReferringUndefinedPagesPlugin} exclude='TestPage02*'}");

        Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
        Assertions.assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
    }

    /**
     * Test with the max parameter
     *
     * @throws Exception
     */
    @Test
    public void testParmMax() throws Exception {
        String res = manager.execute(context,"{INSERT ReferringUndefinedPagesPlugin} max='2'}");

        Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
        Assertions.assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        Assertions.assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
        Assertions.assertTrue(res.contains("...and 1 more"));
    }

}
