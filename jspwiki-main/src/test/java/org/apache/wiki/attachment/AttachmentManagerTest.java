/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.attachment;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

public class AttachmentManagerTest {

    public static final String NAME1 = "TestPage";
    public static final String NAMEU = "TestPage\u00e6";

    TestEngine m_engine;
    AttachmentManager m_manager;

    static String c_fileContents = "ABCDEFGHIJKLMNOPQRSTUVWxyz";

    @BeforeEach
    public void setUp() throws Exception {
        m_engine  = TestEngine.build();
        m_manager = m_engine.getManager( AttachmentManager.class );

        m_engine.saveText( NAME1, "Foobar" );
        m_engine.saveText( NAMEU, "Foobar" );
    }

    private File makeAttachmentFile() throws Exception {
        final File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();
        try( final FileWriter out = new FileWriter( tmpFile ) ) {
            FileUtil.copyContents( new StringReader( c_fileContents ), out );
        }

        return tmpFile;
    }

    @AfterEach
    public void tearDown() {
        m_engine.deleteTestPage( NAME1 );
        m_engine.deleteTestPage( NAMEU );
        m_engine.shutdown();

        TestEngine.deleteAttachments(NAME1);
        TestEngine.deleteAttachments(NAMEU);

        TestEngine.emptyWorkDir();
    }

    @Test
    public void testEnabled()
    {
        Assertions.assertTrue( m_manager.attachmentsEnabled(), "not enabled" );
    }

    @Test
    public void testSimpleStore()
        throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        final Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine, Wiki.contents().page(m_engine, NAME1)), "test1.txt" );

        Assertions.assertNotNull( att2, "attachment disappeared" );
        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
        Assertions.assertEquals( c_fileContents.length(), att2.getSize(), "size" );

        final InputStream in = m_manager.getAttachmentStream( att2 );

        Assertions.assertNotNull( in, "stream" );

        final StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assertions.assertEquals( c_fileContents, sout.toString(), "contents" );
    }

    @Test
    public void testSimpleStoreSpace()
        throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test file.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        final Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine, Wiki.contents().page(m_engine, NAME1)),"test file.txt" );

        Assertions.assertNotNull( att2, "attachment disappeared" );
        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
        Assertions.assertEquals( c_fileContents.length(), att2.getSize(), "size" );

        final InputStream in = m_manager.getAttachmentStream( att2 );

        Assertions.assertNotNull( in, "stream" );

        final StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assertions.assertEquals( c_fileContents, sout.toString(), "contents" );
    }

    @Test
    public void testSimpleStoreByVersion()
        throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        final Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine, Wiki.contents().page(m_engine, NAME1)), "test1.txt", 1 );

        Assertions.assertNotNull( att2, "attachment disappeared" );
        Assertions.assertEquals( 1, att2.getVersion(), "version" );
        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
        Assertions.assertEquals( c_fileContents.length(), att2.getSize(), "size" );

        final InputStream in = m_manager.getAttachmentStream( att2 );

        Assertions.assertNotNull( in, "stream" );

        final StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assertions.assertEquals( c_fileContents, sout.toString(), "contents" );
    }

    @Test
    public void testMultipleStore()
        throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        att.setAuthor( "FooBar" );
        m_manager.storeAttachment( att, makeAttachmentFile() );

        final Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine, Wiki.contents().page(m_engine, NAME1)), "test1.txt" );

        Assertions.assertNotNull( att2, "attachment disappeared" );
        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
        Assertions.assertEquals( 2, att2.getVersion(), "version" );

        final InputStream in = m_manager.getAttachmentStream( att2 );

        Assertions.assertNotNull( in, "stream" );

        final StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assertions.assertEquals( c_fileContents, sout.toString(), "contents" );


        //
        // Check that first author did not disappear
        //

        final Attachment att3 = m_manager.getAttachmentInfo( new WikiContext(m_engine, Wiki.contents().page(m_engine, NAME1)), "test1.txt", 1 );
        Assertions.assertEquals( 1, att3.getVersion(), "version of v1" );
        Assertions.assertEquals( "FirstPost", att3.getAuthor(), "name of v1" );
    }

    @Test
    public void testListAttachments()
        throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        final List< Attachment > c = m_manager.listAttachments( Wiki.contents().page(m_engine, NAME1) );

        Assertions.assertEquals( 1, c.size(), "Length" );

        final Attachment att2 = (Attachment) c.toArray()[0];

        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
    }

    @Test
    public void testSimpleStoreWithoutExt() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        final Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       Wiki.contents().page(m_engine, NAME1)),
                                                       "test1" );

        Assertions.assertNotNull( att2, "attachment disappeared" );
        Assertions.assertEquals( att.getName(), att2.getName(), "name" );
        Assertions.assertEquals( att.getAuthor(), att2.getAuthor(), "author" );
        Assertions.assertEquals( c_fileContents.length(), att2.getSize(), "size" );
        Assertions.assertEquals( 1, att2.getVersion(), "version" );

        final InputStream in = m_manager.getAttachmentStream( att2 );

        Assertions.assertNotNull( in, "stream" );

        final StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assertions.assertEquals( c_fileContents, sout.toString(), "contents" );
    }


    @Test
    public void testExists() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( NAME1+"/test1" ), "attachment disappeared" );
    }

    @Test
    public void testExists2() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test1.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( att.getName() ), "attachment disappeared" );
    }

    @Test
    public void testExistsSpace() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test file.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( NAME1+"/test file.bin" ), "attachment disappeared" );
    }

    @Test
    public void testExistsUTF1() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAME1, "test\u00e4.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( att.getName() ), "attachment disappeared" );
    }

    @Test
    public void testExistsUTF2() throws Exception
    {
        final Attachment att = Wiki.contents().attachment( m_engine, NAMEU, "test\u00e4.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assertions.assertTrue( m_engine.getManager( PageManager.class ).wikiPageExists( att.getName() ), "attachment disappeared" );
    }

    @Test
    public void testNonexistentPage() throws Exception
    {
        try
        {
            m_engine.saveText( "TestPage", "xx" );

            final Attachment att = Wiki.contents().attachment( m_engine, "TestPages", "foo.bin" );

            att.setAuthor("MonicaBellucci");
            m_manager.storeAttachment( att, makeAttachmentFile() );

            Assertions.fail("Attachment was stored even when the page does not exist");
        }
        catch( final ProviderException ex )
        {
            // This is the intended exception
        }
        finally
        {
            m_engine.getManager( PageManager.class ).deletePage("TestPage");
        }
    }

    @Test
    public void testValidateFileName() throws Exception
    {
        Assertions.assertEquals( "foo.jpg", AttachmentManager.validateFileName( "foo.jpg" ), "foo.jpg" );

        Assertions.assertEquals( "test.jpg", AttachmentManager.validateFileName( "C:\\Windows\\test.jpg" ), "C:\\Windows\\test.jpg" );

        final WikiException thrown1 =
        Assertions.assertThrows( WikiException.class, () -> AttachmentManager.validateFileName( "C:\\Windows\\test.jsp" ) );
        Assertions.assertTrue(thrown1.getMessage().contains("attach.unwanted.file"), thrown1.getMessage());

        final WikiException thrown2 =
        Assertions.assertThrows( WikiException.class, () -> AttachmentManager.validateFileName( "C:\\Windows\\test.jsp\\" ) );
        Assertions.assertTrue(thrown2.getMessage().contains("attach.unwanted.file"), thrown2.getMessage());

        Assertions.assertEquals( "test__test.jpg", AttachmentManager.validateFileName( "C:\\Windows\\test#?test.jpg" ), "test#?test.jpg" );

    }

}
