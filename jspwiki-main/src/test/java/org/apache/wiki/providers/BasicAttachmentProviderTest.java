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
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.util.FileUtil;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.StringReader;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;


public class BasicAttachmentProviderTest {

    public static final String NAME1 = "FirstTestPage";
    public static final String NAME2 = "AfterFirstTestPage";

    Properties props = TestEngine.getTestProperties();
    TestEngine m_engine;
    BasicAttachmentProvider m_provider;

    /** This is the sound of my head hitting the keyboard. */
    private static final String FILE_CONTENTS = "gy th tgyhgthygyth tgyfgftrfgvtgfgtr";

    @BeforeEach
    public void setUp() throws Exception {
        m_engine = new TestEngine( props );

        m_provider = new BasicAttachmentProvider();
        m_provider.initialize( m_engine, props );

        m_engine.saveText( NAME1, "Foobar" );
        m_engine.saveText( NAME2, "Foobar2" );
    }

    private File makeAttachmentFile() throws Exception {
        final File tmpFile = File.createTempFile("test-",".txt");
        return copyContents( tmpFile );
    }

    private File makeExtraFile( final File directory, final String name ) throws Exception {
        final File tmpFile = new File( directory, name );
        return copyContents( tmpFile );
    }

    private File copyContents( final File to ) throws Exception {
        to.deleteOnExit();
        try( final FileWriter out = new FileWriter( to ) ) {
            FileUtil.copyContents( new StringReader( FILE_CONTENTS ), out );
        }

        return to;
    }

    @Test
    public void testExtension() {
        final String s = "test.png";
        Assertions.assertEquals( "png", BasicAttachmentProvider.getFileExtension(s) );
    }

    @Test
    public void testExtension2() {
        final String s = ".foo";
        Assertions.assertEquals( "foo", BasicAttachmentProvider.getFileExtension(s) );
    }

    @Test
    public void testExtension3() {
        final String s = "test.png.3";
        Assertions.assertEquals( "3", BasicAttachmentProvider.getFileExtension(s) );
    }

    @Test
    public void testExtension4() {
        final String s = "testpng";
        Assertions.assertEquals( "bin", BasicAttachmentProvider.getFileExtension(s) );
    }

    @Test
    public void testExtension5() {
        final String s = "test.";
        Assertions.assertEquals( "bin", BasicAttachmentProvider.getFileExtension(s) );
    }

    @Test
    public void testExtension6() {
        final String s = "test.a";
        Assertions.assertEquals( "a", BasicAttachmentProvider.getFileExtension(s) );
    }

    /**
     *  Can we save attachments with names in UTF-8 range?
     */
    @Test
    public void testPutAttachmentUTF8() throws Exception {
        final File in = makeAttachmentFile();
        final Attachment att = new Attachment( m_engine, NAME1, "\u3072\u3048\u308b\u00e5\u00e4\u00f6test.f\u00fc\u00fc" );

        m_provider.putAttachmentData( att, new FileInputStream(in) );
        final List< Attachment > res = m_provider.listAllChanged( new Date(0L) );
        final Attachment a0 = res.get(0);

        Assertions.assertEquals( att.getName(), a0.getName(), "name" );
    }

    @Test
    public void testGetAttachmentDataRaisesProviderExceptionIfInexistentFileOnDisk() {
        final Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );
        Assertions.assertThrows( ProviderException.class, () -> m_provider.getAttachmentData( att ) );
    }

    ConditionFactory awaitility() {
        return Awaitility.given().ignoreException( ProviderException.class );
    }

    /**
     * Actual save on disk is OS-dependent, meaning the files of two consecutive attachment saves may end up having the
     * same last modified date. As we'll check the order of {@link BasicAttachmentProvider#listAllChanged(Date)}, we must
     * wait until the file saved is actually on disk.
     *
     * If after all, the next saves of two consecutive attachments end up with the same last modified date,
     * {@link BasicAttachmentProvider#listAllChanged(Date)} will sort attachments using wiki page names and versions,
     * so we're preparing attachment names too in order to not mangle with the assertions.
     *
     * @param att attachment to check
     * @return {@code Callable< Boolean.TRUE >} or {@code ProviderException}.
     */
    Callable< Boolean > attachmentIsSaved( final Attachment att ) {
        return () -> {
            m_provider.getAttachmentData( att ).close(); // throws exception if file does not exist on disk
            return true;
        };
    }

    @Test
    public void testListAll() throws Exception {
        final File in = makeAttachmentFile();
        final Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );
        m_provider.putAttachmentData( att, new FileInputStream(in) );

        awaitility().await( "testListAll" ).until( attachmentIsSaved( att ) );

        final Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );
        m_provider.putAttachmentData( att2, new FileInputStream(in) );

        final List< Attachment > res = m_provider.listAllChanged( new Date(0L) );

        Assertions.assertEquals( 2, res.size(), "list size" );

        final Attachment a2 = res.get(0);  // Most recently changed
        final Attachment a1 = res.get(1);  // Least recently changed

        Assertions.assertEquals( att.getName(), a1.getName(), "a1 name :: " + res );
        Assertions.assertEquals( att2.getName(), a2.getName(), "a2 name :: " + res );
    }


    /**
     *  Check that the system does not Assertions.fail if there are extra files in the directory.
     */
    @Test
    public void testListAllExtrafile() throws Exception {
        final File in = makeAttachmentFile();
        final File sDir = new File(m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ) );
        makeExtraFile( sDir, "foobar.blob" );

        final Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );
        m_provider.putAttachmentData( att, new FileInputStream(in) );

        awaitility().await( "testListAllExtrafile" ).until( attachmentIsSaved( att ) );

        final Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );
        m_provider.putAttachmentData( att2, new FileInputStream(in) );

        final List< Attachment > res = m_provider.listAllChanged( new Date(0L) );

        Assertions.assertEquals( 2, res.size(), "list size" );

        final Attachment a2 = res.get(0);  // Most recently changed
        final Attachment a1 = res.get(1);  // Least recently changed

        Assertions.assertEquals( att.getName(), a1.getName(), "a1 name :: " + res );
        Assertions.assertEquals( att2.getName(), a2.getName(), "a2 name :: " + res );
    }

    /**
     *  Check that the system does not Assertions.fail if there are extra files in the attachment directory.
     */
    @Test
    public void testListAllExtrafileInAttachmentDir() throws Exception {
        final File in = makeAttachmentFile();
        final File sDir = new File( m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ) );
        final File attDir = new File( sDir, NAME1+"-att" );

        final Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );
        m_provider.putAttachmentData( att, new FileInputStream(in) );
        makeExtraFile( attDir, "ping.pong" );

        awaitility().await( "testListAllExtrafileInAttachmentDir" ).until( attachmentIsSaved( att ) );

        final Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

        m_provider.putAttachmentData( att2, new FileInputStream(in) );

        final List< Attachment > res = m_provider.listAllChanged( new Date(0L) );

        Assertions.assertEquals( 2, res.size(), "list size" );

        final Attachment a2 = res.get(0);  // Most recently changed
        final Attachment a1 = res.get(1);  // Least recently changed

        Assertions.assertEquals( att.getName(), a1.getName(), "a1 name :: " + res );
        Assertions.assertEquals( att2.getName(), a2.getName(), "a2 name :: " + res );
    }

    /**
     *  Check that the system does not Assertions.fail if there are extra dirs in the attachment directory.
     */
    @Test
    public void testListAllExtradirInAttachmentDir() throws Exception {
        final File in = makeAttachmentFile();
        final File sDir = new File(m_engine.getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR ));
        final File attDir = new File( sDir, NAME1+"-att" );
        final Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );
        m_provider.putAttachmentData( att, new FileInputStream(in) );

        // This is our extraneous directory.
        final File extrafile = new File( attDir, "ping.pong" );
        extrafile.mkdir();

        awaitility().await( "testListAllExtradirInAttachmentDir" ).until( attachmentIsSaved( att ) );

        final Attachment att2 = new Attachment( m_engine, NAME2, "test2.txt" );

        m_provider.putAttachmentData( att2, new FileInputStream(in) );

        final List< Attachment > res = m_provider.listAllChanged( new Date(0L) );

        Assertions.assertEquals( 2, res.size(), "list size" );

        final Attachment a2 = res.get(0);  // Most recently changed
        final Attachment a1 = res.get(1);  // Least recently changed

        Assertions.assertEquals( att.getName(), a1.getName(), "a1 name :: " + res );
        Assertions.assertEquals( att2.getName(), a2.getName(), "a2 name :: " + res );
    }

    @Test
    public void testListAllNoExtension() throws Exception {
        final File in = makeAttachmentFile();
        final Attachment att = new Attachment( m_engine, NAME1, "test1." );
        m_provider.putAttachmentData( att, new FileInputStream(in) );

        awaitility().await( "testListAllNoExtension" ).until( attachmentIsSaved( att ) );

        final Attachment att2 = new Attachment( m_engine, NAME2, "test2." );
        m_provider.putAttachmentData( att2, new FileInputStream(in) );

        final List< Attachment > res = m_provider.listAllChanged( new Date( 0L ) );

        Assertions.assertEquals( 2, res.size(), "list size" );

        final Attachment a2 = res.get(0);  // Most recently changed
        final Attachment a1 = res.get(1);  // Least recently changed

        Assertions.assertEquals( att.getName(), a1.getName(), "a1 name :: " + res );
        Assertions.assertEquals( att2.getName(), a2.getName(), "a2 name :: " + res );
    }

}
