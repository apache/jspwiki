/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.search.tika;

import net.sf.ehcache.CacheManager;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.search.SearchResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Properties;


public class TikaSearchProviderTest {

    private static final long SLEEP_TIME = 200L;
    private static final int SLEEP_COUNT = 500;
    TestEngine engine;
    Properties props;

    @BeforeEach
    void setUp() throws Exception {
        props = TestEngine.getTestProperties();
        TestEngine.emptyWorkDir( props );
        CacheManager.getInstance().removeAllCaches();

        engine = new TestEngine( props );
    }

    @Test
    void testGetAttachmentContent() throws Exception {
        engine.saveText( "test-tika", "blablablabla" );
        byte[] filePng = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "favicon.png" ).toURI() ) );
        byte[] filePdf = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "aaa-diagram.pdf" ).toURI() ) );
        engine.addAttachment( "test-tika", "aaa-diagram.pdf", filePdf );
        engine.addAttachment( "test-tika", "favicon.png", filePng );

        TikaSearchProvider tsp = ( TikaSearchProvider )engine.getSearchManager().getSearchEngine();
        while( !tsp.pendingUpdates().isEmpty() ) { // allow Lucene full index to finish
            Thread.sleep( 100L );
        }

        engine.getSearchManager().getSearchEngine().reindexPage( engine.getPage( "test-tika" ) );

        while( !tsp.pendingUpdates().isEmpty() ) { // allow Lucene reindex to finish
            Thread.sleep( 100L );
        }

        Collection< SearchResult > res = waitForIndex( "favicon.png" , "testGetAttachmentContent" );
        Assertions.assertNotNull( res );
        Assertions.assertEquals( 2, res.size(), debugSearchResults( res ) );

        res = waitForIndex( "application\\/pdf" , "testGetAttachmentContent" );
        Assertions.assertNotNull( res );
        Assertions.assertEquals( 1, res.size(), debugSearchResults( res ) );
    }

    String debugSearchResults( Collection< SearchResult > res ) {
        StringBuilder sb = new StringBuilder();
        for( SearchResult next : res ) {
            sb.append( System.lineSeparator() + "* page: " + next.getPage() );
            for( String s : next.getContexts() ) {
                sb.append( System.lineSeparator() + "** snippet: " + s );
            }
        }
        return sb.toString();
    }

    /**
     * Should cover for both index and initial delay
     */
    Collection< SearchResult > waitForIndex( String text, String testName ) throws Exception {
        MockHttpServletRequest request = engine.newHttpRequest();
        WikiContext ctx = engine.createContext( request, WikiContext.VIEW );
        Collection< SearchResult > res = engine.getSearchManager().findPages( text, ctx );
        for( long l = 0; l < SLEEP_COUNT; l++ ) {
            if( res == null || res.isEmpty() ) {
                Thread.sleep( SLEEP_TIME );
            } else {
                break;
            }
            res = engine.getSearchManager().findPages( text, ctx );
        }
        return res;
    }

}