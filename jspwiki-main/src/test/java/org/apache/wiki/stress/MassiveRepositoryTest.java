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
package org.apache.wiki.stress;
import net.sf.ehcache.CacheManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.util.TextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;
import java.util.Random;

public class MassiveRepositoryTest {
	
    Properties props = TestEngine.getTestProperties("/jspwiki-vers-custom.properties");

    TestEngine engine;

    @BeforeEach
    public void setUp() throws Exception {


        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        // Remove file
        File f = new File( files );

        TestEngine.deleteAll(f);

        CacheManager.getInstance().removeAllCaches();

        engine = new TestEngine(props);
    }

    @AfterEach
    public void tearDown() throws Exception {

        
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        // Remove file
        File f = new File( files );

        TestEngine.deleteAll(f);
    }

    private String getName( int i ) {
        String baseName = "Page";
        return baseName + i;
    }
    
    @Test
    public void testMassiveRepositoryGettingAllPagesFromCache() throws Exception {
        int    numPages = 900;
        int    numRevisions = 900;
        int    numRenders = 9000;
        int    tickmarks  = 90;
        
        stressTest( numPages, numRevisions, numRenders, tickmarks );
    }
    
    @Test
    public void testMassiveRepositoryBypassingCacheByHavingTooMuchPages() throws Exception {
    	int    numPages = 1001;
        int    numRevisions = 1001;
        int    numRenders = 10001;
        int    tickmarks  = 100;
        
        stressTest( numPages, numRevisions, numRenders, tickmarks );
    }

	void stressTest( int numPages, int numRevisions, int numRenders, int tickmarks ) throws WikiException {
		String baseText = "!This is a page %d\r\n\r\nX\r\n\r\nLinks to [%1], [%2], [%3], [%4], [%5], [%6], [%7], [%8], [%9], [%0]";
        
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
            
            String content = engine.getPageManager().getPureText( page, WikiProvider.LATEST_VERSION );
            
            content = TextUtil.replaceString( content, "X", "XX" );
            
            engine.saveText( page, content );
            
            if( i % pm == 0 ) { System.out.print("."); System.out.flush(); }
        }
        
        System.out.println("\nTook "+sw.toString()+", which is "+sw.toString(numRevisions)+" adds/second");
        
        Assertions.assertEquals( numPages, engine.getPageManager().getTotalPageCount(), "Right number of pages" );
        
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
            
            String content = engine.getRenderingManager().getHTML( page, WikiProvider.LATEST_VERSION );
              
            Assertions.assertNotNull(content);
            
            if( i % pm == 0 ) { System.out.print("."); System.out.flush(); }
        }
        
        sw.stop();
        System.out.println("\nTook "+sw.toString()+", which is "+sw.toString(numRenders)+" renders/second");
	}

}
