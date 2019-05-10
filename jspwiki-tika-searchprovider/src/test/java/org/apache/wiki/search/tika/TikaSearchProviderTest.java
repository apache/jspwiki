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
import org.apache.wiki.TestEngine;
import org.apache.wiki.attachment.Attachment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
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
        engine.saveText( "Test-tika", "blablablabla" );
        byte[] filePdf = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "aaa-diagram.pdf" ).toURI() ) );
        byte[] filePng = Files.readAllBytes( Paths.get( TikaSearchProviderTest.class.getClassLoader().getResource( "favicon.png" ).toURI() ) );
        engine.addAttachment( "Test-tika", "aaa-diagram.pdf", filePdf );
        engine.addAttachment( "Test-tika", "favicon.png", filePng );

        TikaSearchProvider tsp = ( TikaSearchProvider )engine.getSearchManager().getSearchEngine();

        Attachment attPdf = engine.getAttachmentManager().getAttachmentInfo( "Test-tika/aaa-diagram.pdf" );
        String pdfIndexed = tsp.getAttachmentContent( attPdf );
        Assertions.assertTrue( pdfIndexed.contains( "aaa-diagram.pdf" ) );
        Assertions.assertTrue( pdfIndexed.contains( "WebContainerAuthorizer" ) );

        Attachment attPng = engine.getAttachmentManager().getAttachmentInfo( "Test-tika/favicon.png" );
        String pngIndexed = tsp.getAttachmentContent( attPng );
        Assertions.assertTrue( pngIndexed.contains( "favicon.png" ) );
    }

}