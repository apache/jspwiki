
package com.ecyrd.jspwiki.providers;

import junit.framework.*;
import java.io.*;
import java.util.*;
import com.ecyrd.jspwiki.*;

public class RCSFileProviderTest extends TestCase
{
    public static final String NAME1 = "Test1";

    Properties props = new Properties();

    WikiEngine engine;

    public RCSFileProviderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki_rcs.properties") );

        engine = new TestEngine(props);
    }

    public void tearDown()
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        f.delete();

        f = new File( files+File.separator+"RCS", NAME1+FileSystemProvider.FILE_EXT+",v" );

        f.delete();

        f = new File( files, "RCS" );

        f.delete();
    }

    /**
     *  Bug report by Anon: RCS under Windows 2k:
     * <PRE>
     * In getPageInfo of RCSFileProvider:
     *
     * Problem:
     *
     * With a longer rlog result, the break clause in the last "else if" 
     * breaks out of the reading loop before all the lines in the full 
     * rlog have been read in. This causes the process.wait() to hang.
     *
     * Suggested quick fix:
     *
     * Always read all the contents of the rlog, even if it is slower.
     * </PRE>
     *
     */

    public void testMillionChanges()
    {
        String text = "";
        String name = NAME1;
        int    maxver = 100; // Save 100 versions.

        for( int i = 0; i < maxver; i++ )
        {
            text = text + ".";
            engine.saveText( name, text );
        }

        WikiPage pageinfo = engine.getPage( NAME1 );

        assertEquals( "wrong version", maxver, pageinfo.getVersion() );
        // +2 comes from \r\n at the end of each file.
        assertEquals( "wrong text", maxver+2, engine.getText(NAME1).length() );
    }

    /**
     *  Checks if migration from FileSystemProvider to VersioningFileProvider
     *  works by creating a dummy file without corresponding content in OLD/
     */
    public void testMigration()
        throws IOException
    {
        String files = props.getProperty( FileSystemProvider.PROP_PAGEDIR );
        
        File f = new File( files, NAME1+FileSystemProvider.FILE_EXT );

        Writer out = new FileWriter( f );
        FileUtil.copyContents( new StringReader("foobar"), out );
        out.close();

        String res = engine.getText( NAME1 );

        assertEquals( "latest did not work", "foobar", res );

        res = engine.getText( NAME1, 1 ); // Should be the first version.

        assertEquals( "fetch by direct version did not work", "foobar", res );
    }

    public void testGetByVersion()
    {
        String text = "diddo\r\n";

        engine.saveText( NAME1, text );

        WikiPage page = engine.getPage( NAME1, 1 );
       
        assertEquals( "name", NAME1, page.getName() );
        assertEquals( "version", 1, page.getVersion() );
    }

    public static Test suite()
    {
        return new TestSuite( RCSFileProviderTest.class );
    }
}
