/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ecyrd.jspwiki;

import java.io.*;
import org.apache.log4j.Category;

/**
 *  Generic utilities.
 */
public class FileUtil
{
    private static final Category   log = Category.getInstance(FileUtil.class);

    /**
     *  Makes a new temporary file and writes content into it.
     *
     *  @param content Initial content of the temporary file.
     *  @return The handle to the new temporary file
     *  @throws IOException If the content creation failed.
     */
    public static File newTmpFile( String content )
        throws IOException
    {
        File f = File.createTempFile( "jspwiki", null );

        StringReader in = new StringReader( content );

        FileWriter out = new FileWriter( f );

        int c;

        while( (c = in.read()) != -1 )
        {
            out.write( c );
        }

        out.close();

        return f;
    }

    /**
     *  Runs a simple command in given directory.
     *
     *  @return Standard output from the command.
     */
    public static String runSimpleCommand( String command, String directory )
        throws IOException,
               InterruptedException
    {
        StringBuffer result = new StringBuffer();        

        log.debug("Running simple command "+command+" in "+directory);
        String[] env = new String[0];

        Process process = Runtime.getRuntime().exec( command, env, new File(directory) );

        BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );

        String line;

        while( (line = stdout.readLine()) != null )
        { 
            result.append( line+"\n");
        }            

        process.waitFor();
        
        return result.toString();
    }
}
