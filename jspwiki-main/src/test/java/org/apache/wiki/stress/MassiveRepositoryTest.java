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

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.providers.FileSystemProvider;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.TextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;
import java.util.Random;

public class MassiveRepositoryTest {

    Properties props = TestEngine.getTestProperties( "/jspwiki-vers-custom.properties" );
    TestEngine engine = TestEngine.build( props );

    @AfterEach
    public void tearDown() throws Exception {
        final String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        final File f = new File( files );
        TestEngine.deleteAll( f );
        engine.shutdown();
    }

    private String getName( final int i ) {
        return String.format( "Page%03d", i );
    }

    @Test
    public void testMassiveRepositoryGettingAllPagesFromCache() throws Exception {
        final int numPages = 900;
        final int numRevisions = 900;
        final int numRenders = 9000;
        final int tickmarks = 90;

        stressTest( numPages, numRevisions, numRenders, tickmarks );
    }

    @Test
    public void testMassiveRepositoryBypassingCacheByHavingTooMuchPages() throws Exception {
        final int numPages = 1001;
        final int numRevisions = 1001;
        final int numRenders = 10001;
        final int tickmarks = 100;

        stressTest( numPages, numRevisions, numRenders, tickmarks );
    }

    void stressTest( final int numPages, final int numRevisions, final int numRenders, final int tickmarks ) throws WikiException {
        final String baseText = "!This is a page %d\r\n\r\nX\r\n\r\nLinks to [%1], [%2], [%3], [%4], [%5], [%6], [%7], [%8], [%9], [%0]";
        final Random random = new Random();
        final Benchmark sw = new Benchmark();
        sw.start();

        System.out.println( "Creating " + numPages + " pages" );

        //  Create repository
        int pm = numPages / tickmarks;
        for ( int i = 0; i < numPages; i++ ) {
            final String name = getName( i );
            String text = TextUtil.replaceString( baseText, "%d", name );
            for ( int r = 0; r < 10; r++ ) {
                text = TextUtil.replaceString( text, "%" + r, getName( i + r - 5 ) );
            }
            engine.saveText( name, text );
            if ( i % pm == 0 ) {
                System.out.print( "." );
                System.out.flush();
            }
        }

        System.out.println( "\nTook " + sw + ", which is " + sw.toString( numPages ) + " adds/second" );

        //  Create new versions
        sw.stop();
        sw.reset();
        sw.start();

        System.out.println( "Checking in " + numRevisions + " revisions" );
        pm = numRevisions / tickmarks;
        for( int i = 0; i < numRevisions; i++ ) {
            final String page = getName( random.nextInt( numPages ) );
            String content = engine.getManager( PageManager.class ).getPureText( page, WikiProvider.LATEST_VERSION );
            content = TextUtil.replaceString( content, "X", "XX" );
            engine.saveText( page, content );
            if ( i % pm == 0 ) {
                System.out.print( "." );
                System.out.flush();
            }
        }

        System.out.println( "\nTook " + sw + ", which is " + sw.toString( numRevisions ) + " adds/second" );

        Assertions.assertEquals( numPages, engine.getManager( PageManager.class ).getTotalPageCount(), "Right number of pages " );

        //  Rendering random pages
        sw.stop();
        sw.reset();
        sw.start();

        System.out.println( "Rendering " + numRenders + " pages" );
        pm = numRenders / tickmarks;

        for ( int i = 0; i < numRenders; i++ ) {
            final String page = getName( random.nextInt( numPages ) );
            final String content = engine.getManager( RenderingManager.class ).getHTML( page, WikiProvider.LATEST_VERSION );
            Assertions.assertNotNull( content );
            if ( i % pm == 0 ) {
                System.out.print( "." );
                System.out.flush();
            }
        }

        sw.stop();
        System.out.println( "\nTook " + sw + ", which is " + sw.toString( numRenders ) + " renders/second" );
    }

}
