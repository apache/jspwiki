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
package org.apache.wiki.ajax;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Assertions;

import org.apache.wiki.ajax.WikiAjaxDispatcherServlet;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.plugin.SampleAjaxPlugin;

/**
 * @since 2.10.2-svn10
 */
public class WikiAjaxServletTest {

    @Test
    public void testServlets() throws Exception {
        String[] paths = new String[] {
                "/ajax/MyPlugin",
                "/ajax/MyPlugin/",
                "/ajax/MyPlugin/Friend",
                "/ajax/MyPlugin?",
                "/ajax/MyPlugin?param=123&param=231",
                "/ajax/MyPlugin#hashCode?param=123&param=231",
                "http://google.com.au/test/ajax/MyPlugin#hashCode?param=123&param=231",
                "/test//ajax/MyPlugin#hashCode?param=123&param=231",
                "http://localhost:8080/ajax/MyPlugin#hashCode?param=123&param=231" };

        Assertions.assertEquals(9,paths.length);
        WikiAjaxDispatcherServlet wikiAjaxDispatcherServlet = new WikiAjaxDispatcherServlet();
        for (String path : paths) {
            String servletName = wikiAjaxDispatcherServlet.getServletName(path);
            Assertions.assertEquals("MyPlugin", servletName);
        }

        // The plugin SampleAjaxPlugin
        WikiAjaxDispatcherServlet.registerServlet(new SampleAjaxPlugin());
        WikiAjaxServlet servlet = wikiAjaxDispatcherServlet.findServletByName("SampleAjaxPlugin");
        Assertions.assertNotNull(servlet);
        Assertions.assertTrue(servlet instanceof SampleAjaxPlugin);

        /** Note sure about this
        WikiAjaxDispatcherServlet.registerServlet(new RPCServlet());
        WikiAjaxServlet servlet2 = wikiAjaxDispatcherServlet.findServletByName("RPCServlet");
        Assertions.assertNotNull(servlet2);
        Assertions.assertTrue(servlet2 instanceof RPCServlet);
        */

        WikiAjaxServlet servlet3 = wikiAjaxDispatcherServlet.findServletByName("TestWikiNonAjaxServlet");
        Assertions.assertNull(servlet3);
    }

}
