/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.   
 */
package com.ecyrd.jspwiki;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 *  Generic utilities related to file and stream handling.
 */
// FIXME3.0: This class will move to "util" directory in 3.0
public final class FileUtil
{
    /** Size of the buffer used when copying large chunks of data. */
    private static final int      BUFFER_SIZE = 4096;
    private static final Logger   log         = LoggerFactory.getLogger(FileUtil.class);

    /**
     *  Private constructor prevents instantiation.
     */
    private FileUtil()
    {}

    /**
     *  Makes a new temporary file and writes content into it. The temporary
     *  file is created using <code>File.createTempFile()</code>, and the usual
     *  semantics apply.  The files are not deleted automatically in exit.
     *
     *  @param content Initial content of the temporary file.
     *  @param encoding Encoding to use.
     *  @return The handle to the new temporary file
     *  @throws IOException If the content creation failed.
     *  @see java.io.File#createTempFile(String,String,File)
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

    /**
     *  Creates a new temporary file using the default encoding
     *  of ISO-8859-1 (Latin1).
     *
     *  @param content The content to put into the file.
     *  @throws IOException If writing was unsuccessful.
     *  @return A handle to the newly created file.
     *  @see #newTmpFile( String, String )
     *  @see java.io.File#createTempFile(String,String,File)
     */
    public static File newTmpFile( String content )
        throws IOException
    {
        return newTmpFile( content, "ISO-8859-1" );
    }

    /**
     *  Runs a simple command in given directory.
     *  The environment is inherited from the parent process (e.g. the
     *  one in which this Java VM runs).
     *
     *  @return Standard output from the command.
     *  @param  command The command to run
     *  @param  directory The working directory to run the command in
     *  @throws IOException If the command failed
     *  @throws InterruptedException If the command was halted
     */
    public static String runSimpleCommand( String command, String directory )
        throws IOException,
               InterruptedException
    {
        StringBuffer result = new StringBuffer();

        log.info("Running simple command "+command+" in "+directory);

        Process process = Runtime.getRuntime().exec( command, null, new File(directory) );

        BufferedReader stdout = null;
        BufferedReader stderr = null;

        try
        {
            stdout = new BufferedReader( new InputStreamReader(process.getInputStream()) );
            stderr = new BufferedReader( new InputStreamReader(process.getErrorStream()) );

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

        }
        finally
        {
            // we must close all by exec(..) opened streams: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4784692
            process.getInputStream().close();
            if( stdout != null ) stdout.close();
            if( stderr != null ) stderr.close();
        }

        return result.toString();
    }


    /**
     *  Just copies all characters from <I>in</I> to <I>out</I>.  The copying
     *  is performed using a buffer of bytes.
     *
     *  @since 1.5.8
     *  @param in The reader to copy from
     *  @param out The reader to copy to
     *  @throws IOException If reading or writing failed.
     */
    public static void copyContents( Reader in, Writer out )
        throws IOException
    {
        char[] buf = new char[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0)
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }

    /**
     *  Just copies all bytes from <I>in</I> to <I>out</I>.  The copying is
     *  performed using a buffer of bytes.
     *
     *  @since 1.9.31
     *  @param in The inputstream to copy from
     *  @param out The outputstream to copy to
     *  @throws IOException In case reading or writing fails.
     */
    public static void copyContents( InputStream in, OutputStream out )
        throws IOException
    {
        byte[] buf = new byte[BUFFER_SIZE];
        int bytesRead = 0;

        while ((bytesRead = in.read(buf)) > 0)
        {
            out.write(buf, 0, bytesRead);
        }

        out.flush();
    }

    /**
     *  Reads in file contents.
     *  <P>
     *  This method is smart and falls back to ISO-8859-1 if the input stream does not
     *  seem to be in the specified encoding.
     *
     *  @param input The InputStream to read from.
     *  @param encoding The encoding to assume at first.
     *  @return A String, interpreted in the "encoding", or, if it fails, in Latin1.
     *  @throws IOException If the stream cannot be read or the stream cannot be
     *          decoded (even) in Latin1
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
     *  @param in The reader from which the contents shall be read.
     *  @return String read from the Reader
     *  @throws IOException If reading fails.
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
                out.close();
            }
            catch( Exception e )
            {
                log.error("Not able to close the stream while reading contents.");
            }
        }
    }

    /**
     *  Returns the class and method name (+a line number) in which the
     *  Throwable was thrown.
     *
     *  @param t A Throwable to analyze.
     *  @return A human-readable string stating the class and method.  Do not rely
     *          the format to be anything fixed.
     */
    public static String getThrowingMethod( Throwable t )
    {
        StackTraceElement[] trace = t.getStackTrace();
        StringBuffer sb = new StringBuffer();

        if( trace == null || trace.length == 0 )
        {
            sb.append( "[Stack trace not available]" );
        }
        else
        {
            sb.append( trace[0].isNativeMethod() ? "native method" : "" );
            sb.append( trace[0].getClassName() );
            sb.append(".");
            sb.append(trace[0].getMethodName()+"(), line "+trace[0].getLineNumber());
        }
        return sb.toString();
    }
}
