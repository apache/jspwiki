
package com.ecyrd.jspwiki;
import java.util.Properties;
import javax.servlet.*;
import java.io.*;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine
{
    public TestEngine( Properties props )
        throws NoRequiredPropertyException,
               ServletException
    {
        super( props );
    }

    public boolean pageExists( String page )
    {
        return true;
    }

    /**
     *  Deletes all files under this directory, and does them recursively.
     */
    public static void deleteAll( File file )
    {
        if( file.isDirectory() )
        {
            File[] files = file.listFiles();

            for( int i = 0; i < files.length; i++ )
            {
                if( files[i].isDirectory() )
                {
                    deleteAll(files[i]);                
                }

                files[i].delete();
            }
        }

        file.delete();
    }


}
