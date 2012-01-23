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

package org.apache.wiki.api;

/**
 *  Contains release and version information.  You may also invoke this
 *  class directly, in which case it prints out the version string.  This
 *  is a handy way of checking which JSPWiki version you have - just type
 *  from a command line:
 *  <pre>
 *  % java -cp JSPWiki-api.jar org.apache.wiki.api.Release
 *  1.0
 *  </pre>
 */
public final class Release
{
    /** The JSPWiki API major version. */
    public static final int        VERSION       = 0;

    /** The JSPWiki API revision. */
    public static final int        REVISION      = 1;
    
    /**
     *  This is the generic version string you should use
     *  when printing out the version.  It is of the form "VERSION.REVISION"
     */
    public static final String     VERSTR        = VERSION+"."+REVISION;

    /**
     *  Private constructor prevents instantiation.
     */
    private Release()
    {}

    /**
     *  This method is useful for templates, because hopefully it will
     *  not be inlined, and thus any change to version number does not
     *  need recompiling the pages.
     *
     *  @since 2.1.26.
     *  @return The version string (e.g. 2.5.23).
     */
    public static String getVersionString()
    {
        return VERSTR;
    }

    /**
     *  Executing this class directly from command line prints out
     *  the current version.  It is very useful for things like
     *  different command line tools.
     *  <P>Example:
     *  <PRE>
     *  % java org.apache.wiki.api.Release
     *  1.0
     *  </PRE>
     *
     *  @param argv The argument string.  This class takes in no arguments.
     */
    public static void main( String[] argv )
    {
        System.out.println(VERSTR);
    }
}
