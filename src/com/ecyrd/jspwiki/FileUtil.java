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
 *  Generic utilities related to file and stream handling.
 */
public class FileUtil
{
    private static final Category   log = Category.getInstance(FileUtil.class);

    /**
     *  Makes a new temporary file and writes content into it.
     *
     *  @param content Initial content of the temporary file.
     *  @param encoding Encoding to use.
     *  @return The handle to the new temporary file
     *  @throws IOException If the content creation failed.
     */
    public static File newTmpFile( String content, String encoding )
        throws IOException
    {
        Writer out = null;
        Reader in  = null;
        File   f   = null;

        try
        {
            f = File.createTempFile( "jspwiki", null );
            
            in = new StringReader( content );

            out = new OutputStreamWriter( new FileOutputStream( f ),
                                          encoding );

            copyContents( in, out );
        }
        finally
        {
            if( in  != null ) in.close();
            if( out != null ) out.close();
        }

        return f;
    }

    /** Default encoding is ISO-8859-1 */
    public static File newTmpFile( String content )
        throws IOException
    {
        return newTmpFile( content, "ISO-8859-1" );
    }

    /**
     *  Runs a simple command in given directory.
     *  The environment is inherited from the parent process.
     *
     *  @return Standard output from the command.
     */
    public static String runSimpleCommand( String command, String directory )
        throws IOException,
               InterruptedException
    {
        StringBuffer result = new StringBuffer();        

        log.info("Running simple command "+command+" in "+directory);

        Process process = Runtime.getRuntime().exec( command, null, new File(directory) );

        BufferedReader stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );
        BufferedReader stderr = new BufferedReader( new InputStreamReader(process.getErrorStream()) );

        String line;

        while( (line = stdout.readLine()) != null )
        { 
            result.append( line+"\n");
        }            

        StringBuffer error = new StringBuffer();
        while( (line = stderr.readLine()) != null )
        { 
            error.append( line+"\n");
        }            

        if( error.length() > 0 )
        {
            log.error("Command failed, error stream is: "+error);
        }

        process.waitFor();
        
        return result.toString();
    }


    /**
     *  Just copies all characters from <I>in</I> to <I>out</I>.
     *
     *  @since 1.5.8
     */
    // FIXME: Could probably be more optimized
    public static void copyContents( Reader in, Writer out )
        throws IOException
    {
        int c;
        
        while( (c = in.read()) != -1  )
        {
            out.write( c );
        }
    }

    /**
     *  Just copies all characters from <I>in</I> to <I>out</I>.
     *
     *  @since 1.9.31
     */
    // FIXME: Could probably be more optimized
    public static void copyContents( InputStream in, OutputStream out )
        throws IOException
    {
        int c;
        
        while( (c = in.read()) != -1  )
        {
            out.write( c );
        }
    }

    /**
     *  Is smart and falls back to ISO-8859-1 if you get exceptions.
     */
    // FIXME: There is a bad bug here.  We cannot guarantee that realinput.available()
    // returns anything sane.  We don't want to read everything into a byte array
    // either, since that would mean having to go through at it again.  Byte array
    // does not support mark()/reset().
    // We get odd exceptions if we don't specify a large enough buffer size.
    // We assume that if we get a small number from available() the data is buffered
    // and use a minimum buffer size to compensate.
    // This may fail in a number of ways, a better way is seriously needed.

    private static final int MINBUFSIZ = 32768; // bytes

    public static String readContents( InputStream input, String encoding )
        throws IOException
    {
        Reader in  = null;
        Writer out = null;

        BufferedInputStream realinput = new BufferedInputStream( input );

        realinput.mark( Math.max( realinput.available() * 2, MINBUFSIZ ) );

        try
        {
            in = new BufferedReader( new InputStreamReader( realinput, 
                                                            encoding ) );
            out = new StringWriter();

            copyContents( in, out );

            return out.toString();
        }
        catch( IOException e )
        {
            //
            //  The reading should fail with an IOException, if the UTF-8 format 
            //  is damaged, i.e. there is ISO-Latin1 instead of UTF-8.
            //

            // FIXME: This does NOT work with JDK1.4!

            if( !encoding.equals("ISO-8859-1") )
            {
                // FIXME: The real exceptions we get in case there is a problem with
                // encoding are sun.io.MalformedInputExceptions, but they are not
                // java standard, so we'd better not catch them.

                log.info( "Unable to read stream - odd exception.  Assuming this data is ISO-8859-1 and retrying.\n  "+e.getMessage() );
                log.debug( "Full exception is", e );
                
                // We try again, this time with a more conventional encoding
                // for backwards compatibility.            

                realinput.reset();
                return readContents( realinput, "ISO-8859-1" );
            }
            else
            {
                throw( e );
            }
        }
        finally
        {
            try
            {
                if( in != null ) in.close();
                if( out != null ) out.close();
            }
            catch( Exception e ) {} // FIXME: Log errors.
        }
    }

    /**
     *  Returns the full contents of the Reader as a String.
     *
     *  @since 1.5.8
     */
    public static String readContents( Reader in )
        throws IOException
    {
        Writer out = null;

        try
        {
            out = new StringWriter();

            copyContents( in, out );

            return out.toString();
        }
        finally
        {
            try
            {
                if( out != null ) out.close();
            }
            catch( Exception e ) {} // FIXME: Log errors.
        }
    }

}
