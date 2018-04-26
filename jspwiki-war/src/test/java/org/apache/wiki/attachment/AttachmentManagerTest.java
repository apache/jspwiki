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
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Properties;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.util.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.sf.ehcache.CacheManager;

public class AttachmentManagerTest
{
    public static final String NAME1 = "TestPage";
    public static final String NAMEU = "TestPage\u00e6";

    Properties props = TestEngine.getTestProperties();

    TestEngine m_engine;
    AttachmentManager m_manager;

    static String c_fileContents = "ABCDEFGHIJKLMNOPQRSTUVWxyz";

    @Before
    public void setUp()
        throws Exception
    {
        CacheManager m_cacheManager = CacheManager.getInstance();
        m_cacheManager.clearAll();
        m_cacheManager.removeAllCaches();

        m_engine  = new TestEngine(props);
        m_manager = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "Foobar" );
        m_engine.saveText( NAMEU, "Foobar" );
    }

    private File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );

        FileUtil.copyContents( new StringReader( c_fileContents ), out );

        out.close();

        return tmpFile;
    }

    @After
    public void tearDown()
    {
        m_engine.deleteTestPage( NAME1 );
        m_engine.deleteTestPage( NAMEU );

        TestEngine.deleteAttachments(NAME1);
        TestEngine.deleteAttachments(NAMEU);

        TestEngine.emptyWorkDir();
    }

    @Test
    public void testEnabled()
    {
        Assert.assertTrue( "not enabled", m_manager.attachmentsEnabled() );
    }

    @Test
    public void testSimpleStore()
        throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test1.txt" );

        Assert.assertNotNull( "attachment disappeared", att2 );
        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", att.getAuthor(), att2.getAuthor() );
        Assert.assertEquals( "size", c_fileContents.length(), att2.getSize() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        Assert.assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assert.assertEquals( "contents", c_fileContents, sout.toString() );
    }

    @Test
    public void testSimpleStoreSpace()
        throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test file.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test file.txt" );

        Assert.assertNotNull( "attachment disappeared", att2 );
        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", att.getAuthor(), att2.getAuthor() );
        Assert.assertEquals( "size", c_fileContents.length(), att2.getSize() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        Assert.assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assert.assertEquals( "contents", c_fileContents, sout.toString() );
    }

    @Test
    public void testSimpleStoreByVersion()
        throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test1.txt", 1 );

        Assert.assertNotNull( "attachment disappeared", att2 );
        Assert.assertEquals( "version", 1, att2.getVersion() );
        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", att.getAuthor(), att2.getAuthor() );
        Assert.assertEquals( "size", c_fileContents.length(), att2.getSize() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        Assert.assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assert.assertEquals( "contents", c_fileContents, sout.toString() );
    }

    @Test
    public void testMultipleStore()
        throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        att.setAuthor( "FooBar" );
        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test1.txt" );

        Assert.assertNotNull( "attachment disappeared", att2 );
        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", att.getAuthor(), att2.getAuthor() );
        Assert.assertEquals( "version", 2, att2.getVersion() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        Assert.assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assert.assertEquals( "contents", c_fileContents, sout.toString() );


        //
        // Check that first author did not disappear
        //

        Attachment att3 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test1.txt",
                                                       1 );
        Assert.assertEquals( "version of v1", 1, att3.getVersion() );
        Assert.assertEquals( "name of v1", "FirstPost", att3.getAuthor() );
    }

    @Test
    public void testListAttachments()
        throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1.txt" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Collection<?> c = m_manager.listAttachments( new WikiPage(m_engine, NAME1) );

        Assert.assertEquals( "Length", 1, c.size() );

        Attachment att2 = (Attachment) c.toArray()[0];

        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", att.getAuthor(), att2.getAuthor() );
    }

    @Test
    public void testSimpleStoreWithoutExt() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiContext(m_engine,
                                                                       new WikiPage(m_engine, NAME1)),
                                                       "test1" );

        Assert.assertNotNull( "attachment disappeared", att2 );
        Assert.assertEquals( "name", att.getName(), att2.getName() );
        Assert.assertEquals( "author", "FirstPost", att2.getAuthor() );
        Assert.assertEquals( "size", c_fileContents.length(), att2.getSize() );
        Assert.assertEquals( "version", 1, att2.getVersion() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        Assert.assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        in.close();
        sout.close();

        Assert.assertEquals( "contents", c_fileContents, sout.toString() );
    }


    @Test
    public void testExists() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assert.assertTrue( "attachment disappeared",
                    m_engine.pageExists( NAME1+"/test1" ) );
    }

    @Test
    public void testExists2() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test1.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assert.assertTrue( "attachment disappeared",
                    m_engine.pageExists( att.getName() ) );
    }

    @Test
    public void testExistsSpace() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test file.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assert.assertTrue( "attachment disappeared",
                    m_engine.pageExists( NAME1+"/test file.bin" ) );
    }

    @Test
    public void testExistsUTF1() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAME1, "test\u00e4.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assert.assertTrue( "attachment disappeared",
                    m_engine.pageExists( att.getName() ) );
    }

    @Test
    public void testExistsUTF2() throws Exception
    {
        Attachment att = new Attachment( m_engine, NAMEU, "test\u00e4.bin" );

        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Assert.assertTrue( "attachment disappeared",
                    m_engine.pageExists( att.getName() ) );
    }

    @Test
    public void testNonexistentPage() throws Exception
    {
        try
        {
            m_engine.saveText( "TestPage", "xx" );

            Attachment att = new Attachment( m_engine, "TestPages", "foo.bin" );

            att.setAuthor("MonicaBellucci");
            m_manager.storeAttachment( att, makeAttachmentFile() );

            Assert.fail("Attachment was stored even when the page does not exist");
        }
        catch( ProviderException ex )
        {
            // This is the intended exception
        }
        finally
        {
            m_engine.deletePage("TestPage");
        }
    }

    @Test
    public void testValidateFileName() throws Exception
    {
        Assert.assertEquals( "foo.jpg", "foo.jpg", AttachmentManager.validateFileName( "foo.jpg" ) );

        Assert.assertEquals( "C:\\Windows\\test.jpg", "test.jpg", AttachmentManager.validateFileName( "C:\\Windows\\test.jpg" ));
    }

}
