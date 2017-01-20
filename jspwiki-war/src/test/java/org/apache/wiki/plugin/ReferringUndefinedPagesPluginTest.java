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

public class ReferringUndefinedPagesPluginTest extends TestCase {
    Properties props = TestEngine.getTestProperties();

	TestEngine testEngine;

	WikiContext context;

	PluginManager manager;

	public void setUp() throws Exception {
        CacheManager.getInstance().removeAllCaches();
		testEngine = new TestEngine(props);

        testEngine.saveText("TestPage01", "Some Text for testing 01 which refers [NonExistingPageA] ");
        testEngine.saveText("TestPage02", "Some Text for testing 02 which refers [NonExistingPageB] ");
		testEngine.saveText("TestPage03", "Some Text for testing 03 which refers [NonExistingPageC] ");

        context = new WikiContext( testEngine, testEngine.newHttpRequest(), new WikiPage(testEngine,"TestPage") );
        manager = new DefaultPluginManager( testEngine, props );	}

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
	public void testSimple() throws Exception {
		String res = manager.execute(context, "{INSERT ReferringUndefinedPagesPlugin}");
		assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
	}

	/**
	 * Test with the include parameter
	 * 
	 * @throws Exception
	 */
	public void testParmInClude() throws Exception {
		String res = manager.execute(context, "{INSERT ReferringUndefinedPagesPlugin} include='TestPage02*'}"); 
		assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
		assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
	}

    /**
     * Test with the exclude parameter
     *
     * @throws Exception
     */
    public void testParmExClude() throws Exception {
        String res = manager.execute(context,"{INSERT ReferringUndefinedPagesPlugin} exclude='TestPage02*'}");

        assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
        assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
    }

    /**
     * Test with the max parameter
     *
     * @throws Exception
     */
    public void testParmMax() throws Exception {
        String res = manager.execute(context,"{INSERT ReferringUndefinedPagesPlugin} max='2'}");

        assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage01\""));
        assertTrue(res.contains("href=\"/test/Wiki.jsp?page=TestPage02\""));
        assertFalse(res.contains("href=\"/test/Wiki.jsp?page=TestPage03\""));
        assertTrue(res.contains("...and 1 more"));
    }

	public static Test suite() {
		return new TestSuite(ReferringUndefinedPagesPluginTest.class);
	}
}
