
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

public class FileUtilTest extends TestCase
{
    public FileUtilTest( String s )
    {
        super( s );
        Properties props = new Properties();
        try
        {
            props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
            PropertyConfigurator.configure(props);
        }
        catch( IOException e ) {}
    }

    public void setUp()
        throws Exception
    {
    }

    public void tearDown()
    {
    }

    public void testReadContentsLatin1()
        throws Exception
    {
        String src = "abcåäö";

        String res = FileUtil.readContents( new ByteArrayInputStream( src.getBytes("ISO-8859-1") ),
                                            "ISO-8859-1" );

        assertEquals( src, res );
    }

    /**
     *  Check that fallbacks to ISO-Latin1 still work.
     */
    public void testReadContentsLatin1_2()
        throws Exception
    {
        String src = "abcåäödef";

        String res = FileUtil.readContents( new ByteArrayInputStream( src.getBytes("ISO-8859-1") ),
                                            "UTF-8" );

        assertEquals( src, res );
    }

    /**
       ISO Latin 1 from a pipe.

       FIXME: Works only on UNIX systems now.
    */
    public void testReadContentsFromPipe()
        throws Exception
    {
        String src = "abc\n123456\n\nfoobar.\n";

        // Make a very long string.

        for( int i = 0; i < 10; i++ )
        {
            src += src;
        }

        src += "åäö";

        File f = FileUtil.newTmpFile( src, "ISO-8859-1" );

        String[] envp = {};

        try
        {
            Process process = Runtime.getRuntime().exec( "cat "+f.getAbsolutePath(), 
                                                         envp, 
                                                         f.getParentFile() );

            String result = FileUtil.readContents( process.getInputStream(), "UTF-8" );

            f.delete();

            assertEquals( src,
                          result );
        }
        catch( IOException e ) {}
    }

    public void testReadContentsReader()
        throws IOException
    {
        String data = "ABCDEF";

        String result = FileUtil.readContents( new StringReader( data ) );

        assertEquals( data,
                      result );
    }

    public static Test suite()
    {
        return new TestSuite( FileUtilTest.class );
    }
}
