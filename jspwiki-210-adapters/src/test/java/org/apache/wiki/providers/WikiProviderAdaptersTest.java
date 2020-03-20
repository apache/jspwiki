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

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.pages.PageManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.apache.wiki.TestEngine.with;


public class WikiProviderAdaptersTest {

    TestEngine engine = TestEngine.build( with( "jspwiki.usePageCache", "false" ),
                                          with( "jspwiki.pageProvider", "WikiPageAdapterProvider" ),
                                          with( "jspwiki.attachmentProvider", "WikiAttachmentAdapterProvider" ),
                                          with( "jspwiki.pageProvider.adapter.impl", "com.example.providers.TwoXWikiPageProvider" ),
                                          with( "jspwiki.attachmentProvider.adapter.impl", "com.example.providers.TwoXWikiAttachmentProvider" ) );

    @Test
    public void testPageProvider() throws Exception {
        final PageProvider pageProvider = engine.getManager( PageManager.class ).getProvider();
        final QueryItem qi = new QueryItem();
        qi.word = "blablablabla";
        qi.type = QueryItem.REQUESTED;

        Assertions.assertEquals( "com.example.providers.TwoXWikiPageProvider", pageProvider.getProviderInfo() );
        Assertions.assertEquals( 3, pageProvider.getAllChangedSince( new Date( 0L ) ).size() );
        Assertions.assertEquals( 3, pageProvider.getAllPages().size() );
        Assertions.assertEquals( 3, pageProvider.getPageCount() );
        Assertions.assertTrue( pageProvider.pageExists( "page1" ) );
        Assertions.assertTrue( pageProvider.pageExists( "page1", 0 ) );

        pageProvider.movePage( "page1", "page0" );
        Assertions.assertTrue( pageProvider.pageExists( "page0" ) );
        Assertions.assertFalse( pageProvider.pageExists( "page1" ) );

        pageProvider.putPageText( new WikiPage( engine, "page4" ), "bloblobloblo" );
        Assertions.assertTrue( pageProvider.pageExists( "page4" ) );
        Assertions.assertEquals( 1, pageProvider.findPages( new QueryItem[] { qi } ).size() );
        pageProvider.putPageText( new WikiPage( engine, "page4" ), "blublublublu" );
        Assertions.assertEquals( 2, pageProvider.getVersionHistory( "page4" ).size() );
        Assertions.assertEquals( "bloblobloblo", pageProvider.getPageText( "page4", 0 ) );
        Assertions.assertEquals( "blublublublu", pageProvider.getPageText( "page4", 1 ) );
        pageProvider.deleteVersion( "page4", 1 );
        Assertions.assertEquals( 1, pageProvider.getVersionHistory( "page4" ).size() );
        pageProvider.deletePage( "page4" );
        Assertions.assertFalse( pageProvider.pageExists( "page4" ) );
    }

    @Test
    public void testAttachmentProvider() throws Exception {
        final AttachmentProvider attachmentProvider = engine.getManager( AttachmentManager.class ).getCurrentProvider();
        final Attachment att11 = new Attachment( engine, "page1", "att11.txt" );
        final Attachment att13 = new Attachment( engine, "page1", "att13.txt" );
        final QueryItem qi = new QueryItem();
        qi.word = "doesn't matter will be ignored";
        qi.type = QueryItem.REQUESTED;

        Assertions.assertEquals( "com.example.providers.TwoXWikiAttachmentProvider", attachmentProvider.getProviderInfo() );
        Assertions.assertEquals( 2, attachmentProvider.listAttachments( new WikiPage( engine, "page1" ) ).size() );
        final byte[] attDataArray = new byte[ attachmentProvider.getAttachmentData( att11 ).available() ];
        attachmentProvider.getAttachmentData( att11 ).read( attDataArray );
        Assertions.assertArrayEquals( "blurb".getBytes( StandardCharsets.UTF_8 ), attDataArray );
        Assertions.assertEquals( 0, attachmentProvider.findAttachments( new QueryItem[]{ qi } ).size() );
        Assertions.assertEquals( 3, attachmentProvider.listAllChanged( new Date( 0L ) ).size() );
        Assertions.assertEquals( att11.getName(), attachmentProvider.getAttachmentInfo( new WikiPage( engine, "page1" ), "att11.txt", 0 ).getName() );
        Assertions.assertEquals( 1, attachmentProvider.getVersionHistory( att11 ).size() );

        attachmentProvider.putAttachmentData( att13, new ByteArrayInputStream( "blorb".getBytes( StandardCharsets.UTF_8 ) ) );
        Assertions.assertEquals( 3, attachmentProvider.listAttachments( new WikiPage( engine, "page1" ) ).size() );

        attachmentProvider.putAttachmentData( att13, new ByteArrayInputStream( "blorb".getBytes( StandardCharsets.UTF_8 ) ) );
        Assertions.assertEquals( 2, attachmentProvider.getVersionHistory( att13 ).size() );
        attachmentProvider.deleteVersion( attachmentProvider.getVersionHistory( att13 ).get( 1 ) );
        Assertions.assertEquals( 1, attachmentProvider.getVersionHistory( att13 ).size() );
        attachmentProvider.deleteAttachment( att13 );
        Assertions.assertEquals( 0, attachmentProvider.getVersionHistory( att13 ).size() );

        Assertions.assertEquals( 2, attachmentProvider.listAttachments( new WikiPage( engine, "page1" ) ).size() );
        attachmentProvider.moveAttachmentsForPage( "page1", "page0" );
        Assertions.assertEquals( 2, attachmentProvider.listAttachments( new WikiPage( engine, "page0" ) ).size() );
        Assertions.assertEquals( 0, attachmentProvider.listAttachments( new WikiPage( engine, "page1" ) ).size() );
    }

}
