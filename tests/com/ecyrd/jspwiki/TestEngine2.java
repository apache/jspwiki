
package com.ecyrd.jspwiki;
import java.util.Properties;
import java.io.File;
import javax.servlet.ServletException;

/** Real test engine */
public class TestEngine2 extends WikiEngine
{
    public TestEngine2( Properties props )
        throws NoRequiredPropertyException,
               ServletException
    {
        super( props );
    }

    public void deletePage( String name )
    {
        String files = getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );

        File f = new File( files, name+FileSystemProvider.FILE_EXT );

        f.delete();
    }
}
