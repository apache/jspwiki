
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

public class FileSystemProviderTest extends TestCase
{
    FileSystemProvider m_provider;
    String             m_pagedir;
    Properties props  = new Properties();

    public FileSystemProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_pagedir = System.getProperties().getProperty("java.io.tmpdir");

        Properties props2 = new Properties();

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           m_pagedir );

        props2.load( TestEngine.findTestProperties() );
        PropertyConfigurator.configure(props2);
        
        m_provider = new FileSystemProvider();

        m_provider.initialize( props );
    }

    public void tearDown()
    {
    }

    public void testScandinavianLetters()
        throws Exception
    {
        try
        {
            WikiPage page = new WikiPage("≈‰Test");

            m_provider.putPageText( page, "test" );

            File resultfile = new File( m_pagedir, "%C5%E4Test.txt" );

            assertTrue("No such file", resultfile.exists());

            String contents = FileUtil.readContents( new FileInputStream(resultfile),
                                                     "ISO-8859-1" );

            assertEquals("Wrong contents", contents, "test");
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
        }
    }

    public void testAuthor()
        throws Exception
    {
        try
        {
            WikiPage page = new WikiPage("≈‰Test");
            page.setAuthor("Min‰");

            m_provider.putPageText( page, "test" );

            WikiPage page2 = m_provider.getPageInfo( "≈‰Test", 1 );

            assertEquals( "Min‰", page2.getAuthor() );
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

        test.initialize( props );

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
                test.initialize( props );

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
        m_provider.putPageText( new WikiPage("Test"), "v1" );

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
