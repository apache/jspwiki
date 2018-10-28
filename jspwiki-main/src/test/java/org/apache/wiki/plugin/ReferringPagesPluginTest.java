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
import java.text.SimpleDateFormat;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.engine.PluginManager;
import org.apache.wiki.api.exceptions.PluginException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ReferringPagesPluginTest
{
    Properties props = TestEngine.getTestProperties();
    TestEngine engine;
    WikiContext context;
    PluginManager manager;

    @BeforeEach
    public void setUp()
        throws Exception
    {
        props.setProperty( "jspwiki.breakTitleWithSpaces", "false" );
        engine = new TestEngine(props);

        engine.saveText( "TestPage", "Reference to [Foobar]." );
        engine.saveText( "Foobar", "Reference to [TestPage]." );
        engine.saveText( "Foobar2", "Reference to [TestPage]." );
        engine.saveText( "Foobar3", "Reference to [TestPage]." );
        engine.saveText( "Foobar4", "Reference to [TestPage]." );
        engine.saveText( "Foobar5", "Reference to [TestPage]." );
        engine.saveText( "Foobar6", "Reference to [TestPage]." );
        engine.saveText( "Foobar7", "Reference to [TestPage]." );

        context = new WikiContext( engine, engine.newHttpRequest(), new WikiPage(engine,"TestPage") );
        manager = new DefaultPluginManager( engine, props );
    }

    @AfterEach
    public void tearDown()
    {
        engine.deleteTestPage( "TestPage" );
        engine.deleteTestPage( "Foobar" );
        engine.deleteTestPage( "Foobar2" );
        engine.deleteTestPage( "Foobar3" );
        engine.deleteTestPage( "Foobar4" );
        engine.deleteTestPage( "Foobar5" );
        engine.deleteTestPage( "Foobar6" );
        engine.deleteTestPage( "Foobar7" );
    }

    private String mkLink( String page )
    {
        return mkFullLink( page, page );
    }

    private String mkFullLink( String page, String link )
    {
        return "<a class=\"wikipage\" href=\"/test/Wiki.jsp?page="+link+"\">"+page+"</a>";
    }

    @Test
    public void testSingleReferral()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.ReferringPagesPlugin WHERE max=5}");

        Assertions.assertEquals( mkLink( "TestPage" )+"<br />",
                      res );
    }

    @Test
    public void testMaxReferences()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{INSERT org.apache.wiki.plugin.ReferringPagesPlugin WHERE max=5}");

        int count = 0;
        int index = -1;

        // Count the number of hyperlinks.  We could check their
        // correctness as well, though.

        while( (index = res.indexOf("<a",index+1)) != -1 )
        {
            count++;
        }

        // there is one extra "<a" in the result
        Assertions.assertEquals( 5+1, count );

        String expected = ">...and 2 more</a>";
        count =0;
        while( (index = res.indexOf(expected,index+1)) != -1 )
        {
            count++;
        }
        Assertions.assertEquals(1, count, "End");
    }

    @Test
    public void testReferenceWidth()
        throws Exception
    {
        WikiContext context2 = new WikiContext( engine, new WikiPage(engine, "Foobar") );

        String res = manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.ReferringPagesPlugin WHERE maxwidth=5}");

        Assertions.assertEquals( mkFullLink( "TestP...", "TestPage" )+"<br />",
                      res );
    }

    @Test
    public void testInclude()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin include='*7'}" );

        Assertions.assertTrue( res.indexOf("Foobar7") != -1, "7" );
        Assertions.assertTrue( res.indexOf("Foobar6") == -1, "6" );
        Assertions.assertTrue( res.indexOf("Foobar5") == -1, "5" );
        Assertions.assertTrue( res.indexOf("Foobar4") == -1, "4" );
        Assertions.assertTrue( res.indexOf("Foobar3") == -1, "3" );
        Assertions.assertTrue( res.indexOf("Foobar2") == -1, "2" );
    }

    @Test
    public void testExclude()
        throws Exception
    {
        String res = manager.execute( context, "{ReferringPagesPlugin exclude='*'}");
        Assertions.assertEquals( "...nobody", res );
    }

    @Test
    public void testExclude2()
        throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin exclude='*7'}");

        Assertions.assertTrue( res.indexOf("Foobar7") == -1 );
    }

    @Test
    public void testExclude3()
       throws Exception
    {
        String res = manager.execute( context,
                                      "{ReferringPagesPlugin exclude='*7,*5,*4'}");

        Assertions.assertTrue( res.indexOf("Foobar7") == -1, "7" );
        Assertions.assertTrue( res.indexOf("Foobar6") != -1, "6" );
        Assertions.assertTrue( res.indexOf("Foobar5") == -1, "5" );
        Assertions.assertTrue( res.indexOf("Foobar4") == -1, "4" );
        Assertions.assertTrue( res.indexOf("Foobar3") != -1, "3" );
        Assertions.assertTrue( res.indexOf("Foobar2") != -1, "2" );
    }

    @Test
    public void testCount() throws Exception
    {
        String result = null;
        result = manager.execute(context, "{ReferringPagesPlugin show=count}");
        Assertions.assertEquals("7",result);

        result = manager.execute(context, "{ReferringPagesPlugin,exclude='*7',show=count}");
        Assertions.assertEquals("6",result);

        result = manager.execute(context, "{ReferringPagesPlugin,exclude='*7',show=count,showLastModified=true}");
        String numberResult=result.substring(0,result.indexOf(" "));
        Assertions.assertEquals("6",numberResult);

        String dateString = result.substring(result.indexOf("(")+1,result.indexOf(")"));
        // the date should be parseable:
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy zzz", engine.newHttpRequest().getLocale());
        df.parse(dateString);

        // test if the proper exception is thrown:
        String expectedExceptionString = "showLastModified=true is only valid if show=count is also specified";
        String exceptionString = null;
        try
        {
            result = manager.execute(context, "{ReferringPagesPlugin,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        Assertions.assertEquals(expectedExceptionString, exceptionString);
    }

}
