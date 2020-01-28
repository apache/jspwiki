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

import org.apache.commons.lang3.StringUtils;


/**
 *  Contains release and version information.  You may also invoke this class directly, in which case it prints
 *  out the version string.  This is a handy way of checking which JSPWiki version you have - just type from a command line:
 *  <pre>
 *  % java -cp JSPWiki.jar org.apache.wiki.Release
 *  2.5.38
 *  </pre>
 *  <p>
 *  As a historical curiosity, this is the oldest JSPWiki file.  According to the CVS history, it dates from 6.7.2001, and it really hasn't
 *  changed much since.
 *  </p>
 *  @since  1.0
 */
public final class Release {

    private static final String VERSION_SEPARATORS = ".-";

    /**
     *  This is the default application name.
     */
    public static final String     APPNAME       = "JSPWiki";

    /**
     *  This should be empty when doing a release - otherwise keep it as "cvs" so that whenever someone checks out the code, they know
     *  it is a bleeding-edge version.  Other possible values are "alpha" and "beta" for alpha and beta versions, respectively.
     *  <p>
     *  If the POSTFIX is empty, it is not added to the version string.
     */
    private static final String    POSTFIX       = "M7";

    /** The JSPWiki major version. */
    public static final int        VERSION       = 2;

    /** The JSPWiki revision. */
    public static final int        REVISION      = 11;

    /** The minor revision.  */
    public static final int        MINORREVISION = 0;

    /** The build number/identifier.  This is a String as opposed to an integer, just so that people can add other identifiers to it.
     * The build number is incremented every time a committer checks in code, and reset when the a release is made.
     *  <p>
     *  If you are a person who likes to build his own releases, we recommend that you add your initials to this
     *  identifier (e.g. "13-jj", or 49-aj").
     *  <p>
     *  If the build identifier is empty, it is not added.
     */
    public static final String     BUILD         = "git-06";

    /**
     *  This is the generic version string you should use when printing out the version.  It is of
     *  the form "VERSION.REVISION.MINORREVISION[-POSTFIX][-BUILD]".
     */
    public static final String     VERSTR        = VERSION + "." +
                                                   REVISION + "." +
                                                   MINORREVISION +
                                                   ( POSTFIX.length() != 0 ? "-" + POSTFIX : "" ) +
                                                   ( BUILD.length() != 0 ? "-" + BUILD : "" );

    /** Private constructor prevents instantiation. */
    private Release() {
    }

    /**
     *  This method is useful for templates, because hopefully it will not be inlined, and thus any change to version number does not
     *  need recompiling the pages.
     *
     *  @since 2.1.26.
     *  @return The version string (e.g. 2.5.23).
     */
    public static String getVersionString() {
        return VERSTR;
    }

    /**
     *  Returns true, if this version of JSPWiki is newer or equal than what is requested.
     *
     *  @param version A version parameter string (a.b.c-something). B and C are optional.
     *  @return A boolean value describing whether the given version is newer than the current JSPWiki.
     *  @since 2.4.57
     *  @throws IllegalArgumentException If the version string could not be parsed.
     */
    public static boolean isNewerOrEqual( final String version ) throws IllegalArgumentException {
        if( version == null ) {
        	return true;
        }
        final String[] versionComponents = StringUtils.split( version, VERSION_SEPARATORS );
        final int reqVersion       = versionComponents.length > 0 ? Integer.parseInt( versionComponents[0] ) : Release.VERSION;
        final int reqRevision      = versionComponents.length > 1 ? Integer.parseInt( versionComponents[1] ) : Release.REVISION;
        final int reqMinorRevision = versionComponents.length > 2 ? Integer.parseInt( versionComponents[2] ) : Release.MINORREVISION;

        if( VERSION == reqVersion ) {
            if( REVISION == reqRevision ) {
                if( MINORREVISION == reqMinorRevision ) {
                    return true;
                }
                return MINORREVISION > reqMinorRevision;
            }
            return REVISION > reqRevision;
        }
        return VERSION > reqVersion;
    }

    /**
     *  Returns true, if this version of JSPWiki is older or equal than what is requested.
     *
     *  @param version A version parameter string (a.b.c-something)
     *  @return A boolean value describing whether the given version is older than the current JSPWiki version
     *  @since 2.4.57
     *  @throws IllegalArgumentException If the version string could not be parsed.
     */
    public static boolean isOlderOrEqual( final String version ) throws IllegalArgumentException {
        if( version == null ) {
        	return true;
        }

        final String[] versionComponents = StringUtils.split( version, VERSION_SEPARATORS );
        final int reqVersion       = versionComponents.length > 0 ? Integer.parseInt( versionComponents[0] ) : Release.VERSION;
        final int reqRevision      = versionComponents.length > 1 ? Integer.parseInt( versionComponents[1] ) : Release.REVISION;
        final int reqMinorRevision = versionComponents.length > 2 ? Integer.parseInt( versionComponents[2] ) : Release.MINORREVISION;

        if( VERSION == reqVersion ) {
            if( REVISION == reqRevision ) {
                if( MINORREVISION == reqMinorRevision ) {
                    return true;
                }
                return MINORREVISION < reqMinorRevision;
            }
            return REVISION < reqRevision;
        }
        return VERSION < reqVersion;
    }

    /**
     *  Executing this class directly from command line prints out the current version.  It is very useful for
     *  things like different command line tools.
     *  <P>Example:
     *  <PRE>
     *  % java org.apache.wiki.Release
     *  1.9.26-cvs
     *  </PRE>
     *
     *  @param argv The argument string.  This class takes in no arguments.
     */
    public static void main( final String[] argv ) {
        System.out.println( VERSTR );
    }

}
