
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.*;

public class FileSystemProviderTest extends TestCase
{
    FileSystemProvider m_provider;

    public FileSystemProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testNonExistantDirectory()
        throws Exception
    {
        String tmpdir = System.getProperties().getProperty("java.tmpdir");
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
