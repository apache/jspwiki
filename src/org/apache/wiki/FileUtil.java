/*
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
package org.apache.wiki;

import java.io.*;

/**
 *  Generic utilities related to file and stream handling.
 *  @deprecated will be removed in 2.10 scope. Consider using {@link org.apache.wiki.util.FileUtil} 
 *  instead
 */
@Deprecated
public final class FileUtil
{
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
        return org.apache.wiki.util.FileUtil.newTmpFile( content, encoding );
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
        return org.apache.wiki.util.FileUtil.newTmpFile( content, "ISO-8859-1" );
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
        return org.apache.wiki.util.FileUtil.runSimpleCommand( command, directory );
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
        org.apache.wiki.util.FileUtil.copyContents( in, out );
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
        org.apache.wiki.util.FileUtil.copyContents( in, out );
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
        return org.apache.wiki.util.FileUtil.readContents( input, encoding );
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
        return org.apache.wiki.util.FileUtil.readContents( in );
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
        return org.apache.wiki.util.FileUtil.getThrowingMethod( t );
    }

}
