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

import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.Test;

import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;

public class UndefinedPagesPluginTest extends TestCase
{
    Properties props = TestEngine.getTestProperties();
    TestEngine testEngine;
    WikiContext context;
    PluginManager manager;

    public UndefinedPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        CacheManager.getInstance().removalAll();
        testEngine = new TestEngine(props);

        testEngine.saveText( "TestPage", "Reference to [Foobar]." );
        testEngine.saveText( "Foobar", "Reference to [Foobar 2], [Foobars]" );

        context = new WikiContext( testEngine, new WikiPage(testEngine, "TestPage") );
        manager = new DefaultPluginManager( testEngine, props );
    }

    public void tearDown()
    {
        testEngine.deleteTestPage( "TestPage" );
        testEngine.deleteTestPage( "Foobar" );
        TestEngine.emptyWorkDir();
    }

    private String wikitize( String s )
    {
        return testEngine.textToHTML( context, s );
    }

    /**
     *  Tests that only correct undefined links are found.
     *  We also check against plural forms here, which should not
     *  be listed as non-existent.
     */
    public void testSimpleUndefined()
        throws Exception
    {
        WikiContext context2 = new WikiContext( testEngine, new WikiPage(testEngine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.UndefinedPagesPlugin");

        String exp = "[Foobar 2]\\\\";

        assertEquals( wikitize(exp), res );
    }

    public void testCount() throws Exception
    {
        String result = null;
        result = manager.execute(context, "{UndefinedPagesPlugin show=count}");
        assertEquals("1", result);

        // test if the proper exception is thrown:
        String expectedExceptionString = "parameter showLastModified is not valid for the UndefinedPagesPlugin";
        String exceptionString = null;
        try
        {
            result = manager.execute(context, "{UndefinedPagesPlugin,show=count,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        assertEquals(expectedExceptionString, exceptionString);
    }

    public static Test suite()
    {
        return new TestSuite( UndefinedPagesPluginTest.class );
    }
}
