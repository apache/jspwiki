/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ecyrd.jspwiki;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import org.apache.log4j.Logger;

/**
 *  Generic utilities related to file and stream handling.
 */
public class FileUtil
{
    private static final Logger   log      = Logger.getLogger(FileUtil.class);

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
        
        // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
        process.getInputStream().close();
        process.getOutputStream().close();
        process.getErrorStream().close(); 
        
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

        out.flush();
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

        out.flush();
    }

    /**
     *  Reads in file contents.
     *  <P>
     *  This method is smart and falls back to ISO-8859-1 if the input stream does not
     *  seem to be in the specified encoding.
     */
 
    
    public static String readContents( InputStream input, String encoding )
    throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyContents( input, out );

        ByteBuffer     bbuf        = ByteBuffer.wrap( out.toByteArray() );

        Charset        cset        = Charset.forName( encoding );
        CharsetDecoder csetdecoder = cset.newDecoder();

        csetdecoder.onMalformedInput( CodingErrorAction.REPORT );
        csetdecoder.onUnmappableCharacter( CodingErrorAction.REPORT );

        try
        {
            CharBuffer cbuf = csetdecoder.decode( bbuf );

            return cbuf.toString();
        }
        catch( CharacterCodingException e )
        {
            Charset        latin1    = Charset.forName("ISO-8859-1");
            CharsetDecoder l1decoder = latin1.newDecoder();
            
            l1decoder.onMalformedInput( CodingErrorAction.REPORT );
            l1decoder.onUnmappableCharacter( CodingErrorAction.REPORT );

            try
            {
                bbuf = ByteBuffer.wrap( out.toByteArray() );

                CharBuffer cbuf = l1decoder.decode( bbuf );

                return cbuf.toString();
            }
            catch( CharacterCodingException ex )
            {
                throw (CharacterCodingException) ex.fillInStackTrace();
            }
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

    public static String getThrowingMethod( Throwable t )
    {
        StackTraceElement[] trace = t.getStackTrace();
        StringBuffer sb = new StringBuffer();
        
        if( trace == null || trace.length == 0 ) {
            sb.append( "[Stack trace not available]" );
        } else {
            sb.append( trace[0].isNativeMethod() ? "native method" : "" );
            sb.append( trace[0].getClassName() );
            sb.append(".");
            sb.append(trace[0].getMethodName()+"(), line "+trace[0].getLineNumber());
        }
        return sb.toString();
    }
}
