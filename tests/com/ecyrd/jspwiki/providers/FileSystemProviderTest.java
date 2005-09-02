
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

public class FileSystemProviderTest extends TestCase
{
    FileSystemProvider m_provider;
    FileSystemProvider m_providerUTF8;
    String             m_pagedir;
    Properties props  = new Properties();

    TestEngine         m_engine;

    public FileSystemProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_pagedir = System.getProperties().getProperty("java.io.tmpdir");

        Properties props2 = new Properties();

        props.setProperty( PageManager.PROP_PAGEPROVIDER, "FileSystemProvider" );
        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           m_pagedir );

        props2.load( TestEngine.findTestProperties() );
        PropertyConfigurator.configure(props2);
        
        m_engine = new TestEngine(props);

        m_provider = new FileSystemProvider();

        m_provider.initialize( m_engine, props );
        
        props.setProperty( WikiEngine.PROP_ENCODING, "UTF-8" );
        m_providerUTF8 = new FileSystemProvider();
        m_providerUTF8.initialize( m_engine, props );
    }

    public void tearDown()
    {
        TestEngine.deleteAll( new File(m_pagedir) );
    }

    public void testScandinavianLetters()
        throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_provider.putPageText( page, "test" );
        
        File resultfile = new File( m_pagedir, "%C5%E4Test.txt" );
        
        assertTrue("No such file", resultfile.exists());
        
        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "ISO-8859-1" );
        
        assertEquals("Wrong contents", contents, "test");
    }

    public void testScandinavianLettersUTF8()
        throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");

        m_providerUTF8.putPageText( page, "test\u00d6" );

        File resultfile = new File( m_pagedir, "%C3%85%C3%A4Test.txt" );

        assertTrue("No such file", resultfile.exists());

        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );

        assertEquals("Wrong contents", contents, "test\u00d6");
    }

    /**
     * This should never happen, but let's check that we're protected anyway.
     * @throws Exception
     */
    public void testSlashesInPageNamesUTF8()
         throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test/Foobar");

        m_providerUTF8.putPageText( page, "test" );
        
        File resultfile = new File( m_pagedir, "Test%2FFoobar.txt" );
        
        assertTrue("No such file", resultfile.exists());
        
        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "UTF-8" );
        
        assertEquals("Wrong contents", contents, "test");
    }

    public void testSlashesInPageNames()
         throws Exception
    {
        WikiPage page = new WikiPage(m_engine, "Test/Foobar");

        m_provider.putPageText( page, "test" );
   
        File resultfile = new File( m_pagedir, "Test%2FFoobar.txt" );
   
        assertTrue("No such file", resultfile.exists());
   
        String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                 "ISO-8859-1" );
   
        assertEquals("Wrong contents", contents, "test");
    }

    public void testAuthor()
        throws Exception
    {
        try
        {
            WikiPage page = new WikiPage(m_engine, "\u00c5\u00e4Test");
            page.setAuthor("Min\u00e4");

            m_provider.putPageText( page, "test" );

            WikiPage page2 = m_provider.getPageInfo( "\u00c5\u00e4Test", 1 );

            assertEquals( "Min\u00e4", page2.getAuthor() );
        }
        finally
        {
            File resultfile = new File( m_pagedir,
                                        "%C5%E4Test.txt" );
            try
            {
                resultfile.delete();
            }
            catch(Exception e) {}

            resultfile = new File( m_pagedir,
                                   "%C5%E4Test.properties" );
            try
            {
                resultfile.delete();
            }
            catch(Exception e) {}
        }
    }

    public void testNonExistantDirectory()
        throws Exception
    {
        String tmpdir = m_pagedir;
        String dirname = "non-existant-directory";

        String newdir = tmpdir + File.separator + dirname;

        Properties props = new Properties();

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           newdir );

        FileSystemProvider test = new FileSystemProvider();

        test.initialize( m_engine, props );

        File f = new File( newdir );

        assertTrue( "didn't create it", f.exists() );
        assertTrue( "isn't a dir", f.isDirectory() );

        f.delete();
    }

    public void testDirectoryIsFile()
        throws Exception
    {
        File tmpFile = null;

        try
        {
            tmpFile = FileUtil.newTmpFile("foobar"); // Content does not matter.

            Properties props = new Properties();

            props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                               tmpFile.getAbsolutePath() );

            FileSystemProvider test = new FileSystemProvider();

            try
            {
                test.initialize( m_engine, props );

                fail( "Wiki did not warn about wrong property." );
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

    public void testDelete()
        throws Exception
    {
        m_provider.putPageText( new WikiPage(m_engine, "Test"), "v1" );

        m_provider.deletePage( "Test" );

        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, "Test"+FileSystemProvider.FILE_EXT );

        assertFalse( "file exists", f.exists() );
    }

    public static Test suite()
    {
        return new TestSuite( FileSystemProviderTest.class );
    }
}
