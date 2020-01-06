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

package org.apache.wiki.providers;

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.util.FileUtil;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.Properties;

public class CachingProviderTest
{
    protected TestEngine testEngine = TestEngine.build();

    @AfterEach
    public void tearDown() {
        testEngine.deleteTestPage("Testi");
        TestEngine.emptyWorkDir();
        CacheManager.getInstance().removeAllCaches();
    }

    /**
     *  Checks that at startup we call the provider once, and once only.
     */
    @Test
    public void testInitialization()
        throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        props.setProperty( "jspwiki.usePageCache", "true" );
        props.setProperty( "jspwiki.pageProvider", "org.apache.wiki.providers.CounterProvider" );
        props.setProperty( "jspwiki.cachingProvider.capacity", "100" );

        TestEngine engine = new TestEngine( props );

        CounterProvider p = (CounterProvider)((CachingProvider)engine.getPageManager().getProvider()).getRealProvider();

        Assertions.assertEquals( 1, p.m_initCalls, "init" );
        Assertions.assertEquals( 1, p.m_getAllPagesCalls, "getAllPages" );
        Assertions.assertEquals( 0, p.m_pageExistsCalls, "pageExists" );
        Assertions.assertEquals( 4, p.m_getPageTextCalls, "getPageText" );

        engine.getPageManager().getPage( "Foo" );

        Assertions.assertEquals( 0, p.m_pageExistsCalls, "pageExists2" );
    }

    @Test
    public void testSneakyAdd()
        throws Exception
    {
        Properties props = TestEngine.getTestProperties();

        props.setProperty( "jspwiki.cachingProvider.cacheCheckInterval", "2" );

        TestEngine engine = new TestEngine( props );

        String dir = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( dir, "Testi.txt" );
        String content = "[fuufaa]";

        PrintWriter out = new PrintWriter( new FileWriter(f) );
        FileUtil.copyContents( new StringReader(content), out );
        out.close();

        Awaitility.await( "testSneakyAdd" ).until( () -> engine.getPageManager().getPage( "Testi" ) != null );
        WikiPage p = engine.getPageManager().getPage( "Testi" );
        Assertions.assertNotNull( p, "page did not exist?" );

        String text = engine.getPageManager().getText( "Testi");
        Assertions.assertEquals( "[fuufaa]", text, "text" );

        // TODO: ReferenceManager check as well
    }

}
