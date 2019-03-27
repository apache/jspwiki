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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.PageManager;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.util.FileUtil;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;



public class FileSystemProviderTest {

    FileSystemProvider m_provider;
    FileSystemProvider m_providerUTF8;
    String             m_pagedir;
    Properties props = TestEngine.getTestProperties();

    TestEngine         m_engine;

    @Before
    public void setUp() throws Exception {
        m_pagedir = "./target/jspwiki.test.pages";
        props.setProperty( PageManager.PROP_PAGEPROVIDER, "FileSystemProvider" );
        props.setProperty( FileSystemProvider.PROP_PAGEDIR, m_pagedir );

        Properties props2 = new Properties();
        PropertyConfigurator.configure( props2 );

        m_engine = new TestEngine(props);
        m_provider = new FileSystemProvider();
        m_provider.initialize( m_engine, props );

        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );
        m_providerUTF8 = new FileSystemProvider();
        m_providerUTF8.initialize( m_engine, props );
    }

    @After
    public void tearDown() {
        TestEngine.deleteAll( new File( props.getProperty( FileSystemProvider.PROP_PAGEDIR ) ) );
    }

    @Test
    public void testScandinavianLetters() throws Exception {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%C5%E4Test.txt" );

        Assert.assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile), "ISO-8859-1" );

        Assert.assertEquals("Wrong contents", contents, "test");
    }

    @Test
    public void testScandinavianLettersUTF8() throws Exception {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_providerUTF8.putPageText( page, "test\u00d6" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%C3%85%C3%A4Test.txt" );

        Assert.assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );

        Assert.assertEquals("Wrong contents", contents, "test\u00d6");
    }

    /**
     * This should never happen, but let's check that we're protected anyway.
     * @throws Exception
     */
    @Test
    public void testSlashesInPageNamesUTF8()
         throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test/Foobar");

        m_providerUTF8.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "Test%2FFoobar.txt" );

        Assert.assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );

        Assert.assertEquals("Wrong contents", contents, "test");
    }

    @Test
    public void testSlashesInPageNames()
         throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test/Foobar");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "Test%2FFoobar.txt" );

        Assert.assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "ISO-8859-1" );

        Assert.assertEquals("Wrong contents", contents, "test");
    }

    @Test
    public void testDotsInBeginning()
       throws Exception
    {
        WikiPage page = new WikiPage(m_engine, ".Test");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%2ETest.txt" );

        Assert.assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile), "ISO-8859-1" );

        Assert.assertEquals("Wrong contents", contents, "test");
    }

    @Test
    public void testAuthor()
        throws Exception
    {
        try
        {
            WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");
            page.setAuthor("Min\u00e4");

            m_provider.putPageText( page, "test" );

            WikiPage page2 = m_provider.getPageInfo( "\u00c5\u00e4Test", 1 );

            Assert.assertEquals( "Min\u00e4", page2.getAuthor() );
        }
        finally
        {
            File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ), "%C5%E4Test.txt" );
            try {
                resultfile.delete();
            } catch(Exception e) {}

            resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ), "%C5%E4Test.properties" );
            try {
                resultfile.delete();
            } catch(Exception e) {}
        }
    }

    @Test
    public void testNonExistantDirectory() throws Exception {
        String tmpdir =  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) ;
        String dirname = "non-existant-directory";

        String newdir = tmpdir + File.separator + dirname;

        Properties pr = new Properties();

        pr.setProperty( FileSystemProvider.PROP_PAGEDIR,
                           newdir );

        FileSystemProvider test = new FileSystemProvider();

        test.initialize( m_engine, pr );

        File f = new File( newdir );

        Assert.assertTrue( "didn't create it", f.exists() );
        Assert.assertTrue( "isn't a dir", f.isDirectory() );

        f.delete();
    }

    @Test
    public void testDirectoryIsFile()
        throws Exception
    {
        File tmpFile = null;

        try
        {
            tmpFile = FileUtil.newTmpFile("foobar"); // Content does not matter.

            Properties pr = new Properties();

            pr.setProperty( FileSystemProvider.PROP_PAGEDIR,
                               tmpFile.getAbsolutePath() );

            FileSystemProvider test = new FileSystemProvider();

            try
            {
                test.initialize( m_engine, pr );

                Assert.fail( "Wiki did not warn about wrong property." );
            }
            catch( IOException e )
            {
                // This is okay.
            }
        }
        finally
        {
            if( tmpFile != null )
            {
                tmpFile.delete();
            }
        }
    }

    @Test
    public void testDelete()
        throws Exception
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        WikiPage p = new WikiPage(m_engine,"Test");
        p.setAuthor("AnonymousCoward");

        m_provider.putPageText( p, "v1" );

        File f = new File( files, "Test"+FileSystemProvider.FILE_EXT );

        Assert.assertTrue( "file does not exist", f.exists() );

        f = new File( files, "Test.properties" );

        Assert.assertTrue( "property file does not exist", f.exists() );

		m_provider.deletePage(p);

        f = new File( files, "Test"+FileSystemProvider.FILE_EXT );

        Assert.assertFalse( "file exists", f.exists() );

        f = new File( files, "Test.properties" );

        Assert.assertFalse( "properties exist", f.exists() );
    }

    @Test
    public void testCustomProperties() throws Exception {
        String pageDir = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        String pageName = "CustomPropertiesTest";
        String fileName = pageName+FileSystemProvider.FILE_EXT;
        File file = new File (pageDir,fileName);

        Assert.assertFalse( file.exists() );
        WikiPage testPage = new WikiPage(m_engine,pageName);
        testPage.setAuthor("TestAuthor");
        testPage.setAttribute("@test","Save Me");
        testPage.setAttribute("@test2","Save You");
        testPage.setAttribute("test3","Do not save");
        m_provider.putPageText( testPage, "This page has custom properties" );
        Assert.assertTrue("No such file", file.exists() );
        WikiPage pageRetrieved = m_provider.getPageInfo( pageName, -1 );
        String value = (String)pageRetrieved.getAttribute("@test");
        String value2 = (String)pageRetrieved.getAttribute("@test2");
        String value3 = (String)pageRetrieved.getAttribute("test3");
        Assert.assertNotNull(value);
        Assert.assertNotNull(value2);
        Assert.assertNull(value3);
        Assert.assertEquals("Save Me",value);
        Assert.assertEquals("Save You",value2);
        file.delete();
        Assert.assertFalse( file.exists() );
    }

}
