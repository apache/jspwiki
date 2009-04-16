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
package stress;

import java.util.Properties;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.providers.CachingProvider;
import org.apache.wiki.util.TextUtil;


public class MassiveRepositoryTest extends TestCase
{
    Properties props = new Properties();

    TestEngine engine;

    protected void setUp() throws Exception
    {
        super.setUp();     

        props.load( TestEngine.findTestProperties("/jspwiki_vers.properties") );
        props.setProperty( CachingProvider.PROP_CACHECAPACITY, "1000" );

        engine = new TestEngine(props);
    }

    protected void tearDown() throws Exception
    {
        engine.emptyRepository();
        super.tearDown();
    }

    private String getName( int i )
    {
        String baseName = "Page";
        return baseName + i;
    }
    
    public void testMassiveRepository1()
    throws Exception
    {
        String baseText = "!This is a page %d\r\n\r\nX\r\n\r\nLinks to [%1], [%2], [%3], [%4], [%5], [%6], [%7], [%8], [%9], [%0]";
        int    numPages = 1000;
        int    numRevisions = 1000;
        int    numRenders = 10000;
        int    tickmarks  = 100;
        
        Random random = new Random();
        Benchmark sw = new Benchmark();
        sw.start();
        
        System.out.println("Creating "+numPages+" pages");
        //
        //  Create repository
        //
      
        int pm = numPages/tickmarks;
        
        for( int i = 0; i < numPages; i++ )
        {
            String name = getName(i);
            String text = TextUtil.replaceString( baseText, "%d", name );
            
            for( int r = 0; r < 10; r++ )
            {
                text = TextUtil.replaceString( text, "%"+r, getName(i+r-5) );
            }            
        
            engine.saveText( name, text );
            if( i % pm == 0 ) { System.out.print("."); System.out.flush(); }
        }
       
        System.out.println("\nTook "+sw.toString()+", which is "+sw.toString(numPages)+" adds/second");
        //
        //  Create new versions
        //
        sw.stop();
        sw.reset();
        sw.start();
        
        System.out.println("Checking in "+numRevisions+" revisions");
        pm = numRevisions/tickmarks;
        
        for( int i = 0; i < numRevisions; i++ )
        {
            String page = getName( random.nextInt( numPages ) );
            
            String content = engine.getPureText( page, WikiProvider.LATEST_VERSION );
            
            content = TextUtil.replaceString( content, "X", "XX" );
            
            engine.saveText( page, content );
            
            if( i % pm == 0 ) { System.out.print("."); System.out.flush(); }
        }
        
        System.out.println("\nTook "+sw.toString()+", which is "+sw.toString(numRevisions)+" adds/second");
        
        assertEquals( "Right number of pages", numPages, engine.getPageCount() );
        
        //
        //  Rendering random pages
        //
        sw.stop();
        sw.reset();
        sw.start();
        
        System.out.println("Rendering "+numRenders+" pages");
        pm = numRenders/tickmarks;
        
        for( int i = 0; i < numRenders; i++ )
        {
            String page = getName( random.nextInt( numPages ) );
            
            String content = engine.getHTML( page, WikiProvider.LATEST_VERSION );
              
            assertNotNull(content);
            
            if( i % pm == 0 ) { System.out.print("."); System.out.flush(); }
        }
        
        sw.stop();
        System.out.println("\nTook "+sw.toString()+", which is "+sw.toString(numRenders)+" renders/second");
        
    }
}
