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
package org.apache.wiki.web;

import java.io.FileOutputStream;
import java.io.PrintStream;

/**
 * Catching the stdout and stderr when running under a spawned ant java task is difficult, therefore we wrap the TestContainer with this 
 * class and redirect the output to files.
 * 
 */
public class StreamRedirector
{
    public static void main( String[] args ) throws Exception
    {
        String mainClass = args[0];
        String logFile = args[1];

        // redirect the streams
        PrintStream outfile = new PrintStream( new FileOutputStream( logFile ) );
        System.setErr( outfile );
        System.setOut( outfile );

        // call the TestContainer after stripping of the first two args and redirecting the output.
        String[] strippedArgs = new String[args.length - 2];
        System.arraycopy( args, 2, strippedArgs, 0, strippedArgs.length );
        Class.forName( mainClass ).getMethod( "main", String[].class ).invoke( null, (Object) strippedArgs );
    }
}
