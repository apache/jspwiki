/*
    JSPWiki - a JSP-based WikiWiki clone.

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
import java.util.Locale;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.plugin.PluginManager;


public class ReferringPagesPluginTest extends TestCase
{
    Properties m_props = new Properties();
    TestEngine m_engine;
    WikiContext m_context;
    PluginManager m_manager;

    public ReferringPagesPluginTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );

        m_props.setProperty( "jspwiki.breakTitleWithSpaces", "false" );
        m_engine = new TestEngine(m_props);

        m_engine.saveText( "TestPage", "Reference to [Foobar]." );
        m_engine.saveText( "Foobar", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar2", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar3", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar4", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar5", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar6", "Reference to [TestPage]." );
        m_engine.saveText( "Foobar7", "Reference to [TestPage]." );

        m_context = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage( "TestPage" ) );
        m_manager = new PluginManager( m_engine, m_props );
    }

    public void tearDown() throws Exception
    {
        m_engine.emptyRepository();
        m_engine.shutdown();
    }

    private String mkLink( String page )
    {
        return mkFullLink( page, page );
    }

    private String mkFullLink( String page, String link )
    {
        return "<a class=\"wikipage\" href=\"/Wiki.jsp?page="+link+"\">"+page+"</a>";
    }

    public void testSingleReferral()
        throws Exception
    {
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage( "Foobar" ) );

        String res = m_manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.ReferringPagesPlugin WHERE max=5}");

        assertEquals( mkLink( "TestPage" )+"<br />",
                      res );
    }

    public void testMaxReferences()
        throws Exception
    {
        String res = m_manager.execute( m_context,
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
        assertEquals( 5+1, count );
    
        String expected = ">...and 2 more</a>";
        count =0;
        while( (index = res.indexOf(expected,index+1)) != -1 )
        {
            count++;
        }
        assertEquals("End", 1, count );
    }

    public void testReferenceWidth()
        throws Exception
    {
        WikiContext context2 = m_engine.getWikiContextFactory().newViewContext( m_engine.getPage( "Foobar" ) );

        String res = m_manager.execute( context2,
                                      "{INSERT org.apache.wiki.plugin.ReferringPagesPlugin WHERE maxwidth=5}");

        assertEquals( mkFullLink( "TestP...", "TestPage" )+"<br />",
                      res );
    }

    public void testInclude()
        throws Exception
    {
        String res = m_manager.execute( m_context,
                                      "{ReferringPagesPlugin include='*7'}" );

        assertTrue( "7", res.indexOf("Foobar7") != -1 );
        assertTrue( "6", res.indexOf("Foobar6") == -1 );
        assertTrue( "5", res.indexOf("Foobar5") == -1 );
        assertTrue( "4", res.indexOf("Foobar4") == -1 );
        assertTrue( "3", res.indexOf("Foobar3") == -1 );
        assertTrue( "2", res.indexOf("Foobar2") == -1 );
    }

    public void testExclude()
        throws Exception
    {
        String res = m_manager.execute( m_context,
                                      "{ReferringPagesPlugin exclude='*'}");

        assertEquals( "...nobody",
                      res );
    }

    public void testExclude2()
        throws Exception
    {
        String res = m_manager.execute( m_context,
                                      "{ReferringPagesPlugin exclude='*7'}");

        assertTrue( res.indexOf("Foobar7") == -1 );
    }

    public void testExclude3()
       throws Exception
    {
        String res = m_manager.execute( m_context,
                                      "{ReferringPagesPlugin exclude='*7,*5,*4'}");

        assertTrue( "7", res.indexOf("Foobar7") == -1 );
        assertTrue( "6", res.indexOf("Foobar6") != -1 );
        assertTrue( "5", res.indexOf("Foobar5") == -1 );
        assertTrue( "4", res.indexOf("Foobar4") == -1 );
        assertTrue( "3", res.indexOf("Foobar3") != -1 );
        assertTrue( "2", res.indexOf("Foobar2") != -1 );
    }

    public void testCount() throws Exception
    {
        String result = null;
        result = m_manager.execute(m_context, "{ReferringPagesPlugin show=count}");
        assertEquals("7",result);
        
        result = m_manager.execute(m_context, "{ReferringPagesPlugin,exclude='*7',show=count}");
        assertEquals("6",result);
        
        result = m_manager.execute(m_context, "{ReferringPagesPlugin,exclude='*7',show=count,showLastModified=true}");
        String numberResult=result.substring(0,result.indexOf(" "));
        assertEquals("6",numberResult);
        
        String dateString = result.substring(result.indexOf("(")+1,result.indexOf(")"));
        // the date should be parseable:
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss dd-MMM-yyyy zzz", new Locale( "" ) ); // Locale fixed in TestEngine:174
        df.parse(dateString);

        // test if the proper exception is thrown:
        String expectedExceptionString = "showLastModified=true is only valid if show=count is also specified";
        String exceptionString = null;
        try
        {
            result = m_manager.execute(m_context, "{ReferringPagesPlugin,showLastModified=true}");
        }
        catch (PluginException pe)
        {
            exceptionString = pe.getMessage();
        }

        assertEquals(expectedExceptionString, exceptionString);
    }

    public static Test suite()
    {
        return new TestSuite( ReferringPagesPluginTest.class );
    }
}
