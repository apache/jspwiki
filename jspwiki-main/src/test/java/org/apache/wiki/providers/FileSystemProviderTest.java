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
import org.apache.log4j.PropertyConfigurator;
import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.util.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;



public class FileSystemProviderTest {

    FileSystemProvider m_provider;
    FileSystemProvider m_providerUTF8;
    String             m_pagedir;
    Properties props = TestEngine.getTestProperties();

    TestEngine         m_engine;

    @BeforeEach
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

    @AfterEach
    public void tearDown() {
        TestEngine.deleteAll( new File( props.getProperty( FileSystemProvider.PROP_PAGEDIR ) ) );
    }

    @Test
    public void testScandinavianLetters() throws Exception {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%C5%E4Test.txt" );

        Assertions.assertTrue( resultfile.exists(), "No such file" );

        String contents = FileUtil.readContents( new FileInputStream(resultfile), "ISO-8859-1" );

        Assertions.assertEquals( contents, "test", "Wrong contents" );
    }

    @Test
    public void testScandinavianLettersUTF8() throws Exception {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_providerUTF8.putPageText( page, "test\u00d6" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%C3%85%C3%A4Test.txt" );

        Assertions.assertTrue( resultfile.exists(), "No such file" );

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );

        Assertions.assertEquals( contents, "test\u00d6", "Wrong contents" );
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

        Assertions.assertTrue( resultfile.exists(), "No such file" );

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );

        Assertions.assertEquals( contents, "test", "Wrong contents" );
    }

    @Test
    public void testSlashesInPageNames()
         throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test/Foobar");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "Test%2FFoobar.txt" );

        Assertions.assertTrue( resultfile.exists(), "No such file" );

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "ISO-8859-1" );

        Assertions.assertEquals( contents, "test", "Wrong contents" );
    }

    @Test
    public void testDotsInBeginning()
       throws Exception
    {
        WikiPage page = new WikiPage(m_engine, ".Test");

        m_provider.putPageText( page, "test" );

        File resultfile = new File(  props.getProperty( FileSystemProvider.PROP_PAGEDIR ) , "%2ETest.txt" );

        Assertions.assertTrue( resultfile.exists(), "No such file" );

        String contents = FileUtil.readContents( new FileInputStream(resultfile), "ISO-8859-1" );

        Assertions.assertEquals( contents, "test", "Wrong contents" );
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

            Assertions.assertEquals( "Min\u00e4", page2.getAuthor() );
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

        Assertions.assertTrue( f.exists(), "didn't create it" );
        Assertions.assertTrue( f.isDirectory(), "isn't a dir" );

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

            pr.setProperty( FileSystemProvider.PROP_PAGEDIR, tmpFile.getAbsolutePath() );

            FileSystemProvider test = new FileSystemProvider();

            try
            {
                test.initialize( m_engine, pr );

                Assertions.fail( "Wiki did not warn about wrong property." );
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

        Assertions.assertTrue( f.exists(), "file does not exist" );

        f = new File( files, "Test.properties" );

        Assertions.assertTrue( f.exists(), "property file does not exist" );

        m_provider.deletePage( "Test" );

        f = new File( files, "Test"+FileSystemProvider.FILE_EXT );

        Assertions.assertFalse( f.exists(), "file exists" );

        f = new File( files, "Test.properties" );

        Assertions.assertFalse( f.exists(), "properties exist" );
    }

    @Test
    public void testCustomProperties() throws Exception {
        String pageDir = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        String pageName = "CustomPropertiesTest";
        String fileName = pageName+FileSystemProvider.FILE_EXT;
        File file = new File (pageDir,fileName);

        Assertions.assertFalse( file.exists() );
        WikiPage testPage = new WikiPage(m_engine,pageName);
        testPage.setAuthor("TestAuthor");
        testPage.setAttribute("@test","Save Me");
        testPage.setAttribute("@test2","Save You");
        testPage.setAttribute("test3","Do not save");
        m_provider.putPageText( testPage, "This page has custom properties" );
        Assertions.assertTrue( file.exists(), "No such file" );
        WikiPage pageRetrieved = m_provider.getPageInfo( pageName, -1 );
        String value = pageRetrieved.getAttribute("@test");
        String value2 = pageRetrieved.getAttribute("@test2");
        String value3 = pageRetrieved.getAttribute("test3");
        Assertions.assertNotNull(value);
        Assertions.assertNotNull(value2);
        Assertions.assertNull(value3);
        Assertions.assertEquals("Save Me",value);
        Assertions.assertEquals("Save You",value2);
        file.delete();
        Assertions.assertFalse( file.exists() );
    }

}
