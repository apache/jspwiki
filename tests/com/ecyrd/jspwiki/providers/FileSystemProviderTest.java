
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.*;

public class FileSystemProviderTest extends TestCase
{
    FileSystemProvider m_provider;
    String             m_pagedir;

    public FileSystemProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        m_pagedir = System.getProperties().getProperty("java.io.tmpdir");

        Properties props = new Properties();

        props.setProperty( FileSystemProvider.PROP_PAGEDIR, 
                           m_pagedir );

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

        try
        {
            test.initialize( props );

            fail( "Wiki did not warn about wrong property." );
        }
        catch( IOException e )
        {
            if( e instanceof FileNotFoundException )
            {
                // This is okay.
            }
            else
            {
                fail("Wrong exception: "+e);
            }
        }
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


    public static Test suite()
    {
        return new TestSuite( FileSystemProviderTest.class );
    }
}
