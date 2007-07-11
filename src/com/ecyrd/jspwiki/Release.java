/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import org.apache.commons.lang.StringUtils;

/**
 *  Contains release and version information.  You may also invoke this
 *  class directly, in which case it prints out the version string.  This
 *  is a handy way of checking which JSPWiki version you have - just type
 *  from a command line:
 *  <pre>
 *  % java -cp JSPWiki.jar com.ecyrd.jspwiki.Release
 *  2.5.38
 *  </pre>
 *  <p>
 *  As a historical curiosity, this is the oldest JSPWiki file.  According
 *  to the CVS history, it dates from 6.7.2001, and it really hasn't changed
 *  much since.
 *  </p>
 *  @author Janne Jalkanen
 *  @since  1.0
 */
public final class Release
{
    private static final String VERSION_SEPARATORS = ".-";

    /**
     *  This is the default application name.
     */
    public static final String     APPNAME       = "JSPWiki";

    /**
     *  This should be empty when doing a release - otherwise
     *  keep it as "cvs" so that whenever someone checks out the code,
     *  they know it is a bleeding-edge version.  Other possible
     *  values are "-alpha" and "-beta" for alpha and beta versions,
     *  respectively.
     */
    private static final String    POSTFIX       = "-cvs";

    /** The JSPWiki major version. */
    public static final int        VERSION       = 2;

    /** The JSPWiki revision. */
    public static final int        REVISION      = 5;

    /** The minor revision, or a "build number", if you wish. */
    public static final int        MINORREVISION = 96;

    /**
     *  This is the generic version string you should use
     *  when printing out the version.  It is of the form "x.y.z-ttt".
     */
    public static final String     VERSTR        =
        VERSION+"."+REVISION+"."+MINORREVISION+POSTFIX;

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
     *  Returns true, if this version of JSPWiki is newer or equal than what is requested.
     *  @param version A version parameter string (a.b.c-something). B and C are optional.
     *  @return A boolean value describing whether the given version is newer than the current JSPWiki.
     *  @since 2.4.57
     *  @throws IllegalArgumentException If the version string could not be parsed.
     */
    public static boolean isNewerOrEqual( String version )
        throws IllegalArgumentException
    {
        if( version == null ) return true;
        String[] versionComponents = StringUtils.split(version,VERSION_SEPARATORS);
        int reqVersion       = versionComponents.length > 0 ? Integer.parseInt(versionComponents[0]) : Release.VERSION;
        int reqRevision      = versionComponents.length > 1 ? Integer.parseInt(versionComponents[1]) : Release.REVISION;
        int reqMinorRevision = versionComponents.length > 2 ? Integer.parseInt(versionComponents[2]) : Release.MINORREVISION;

        if( VERSION == reqVersion )
        {
            if( REVISION == reqRevision )
            {
                if( MINORREVISION == reqMinorRevision )
                {
                    return true;
                }

                return MINORREVISION > reqMinorRevision;
            }

            return REVISION > reqVersion;
        }

        return VERSION > reqVersion;
    }

    /**
     *  Returns true, if this version of JSPWiki is older or equal than what is requested.
     *  @param version A version parameter string (a.b.c-something)
     *  @return A boolean value describing whether the given version is older than the current JSPWiki version
     *  @since 2.4.57
     *  @throws IllegalArgumentException If the version string could not be parsed.
     */
    public static boolean isOlderOrEqual( String version )
        throws IllegalArgumentException
    {
        if( version == null ) return true;

        String[] versionComponents = StringUtils.split(version,VERSION_SEPARATORS);
        int reqVersion       = versionComponents.length > 0 ? Integer.parseInt(versionComponents[0]) : Release.VERSION;
        int reqRevision      = versionComponents.length > 1 ? Integer.parseInt(versionComponents[1]) : Release.REVISION;
        int reqMinorRevision = versionComponents.length > 2 ? Integer.parseInt(versionComponents[2]) : Release.MINORREVISION;

        if( VERSION == reqVersion )
        {
            if( REVISION == reqRevision )
            {
                if( MINORREVISION == reqMinorRevision )
                {
                    return true;
                }

                return MINORREVISION < reqMinorRevision;
            }

            return REVISION < reqVersion;
        }

        return VERSION < reqVersion;
    }

    /**
     *  Executing this class directly from command line prints out
     *  the current version.  It is very useful for things like
     *  different command line tools.
     *  <P>Example:
     *  <PRE>
     *  % java com.ecyrd.jspwiki.Release
     *  1.9.26-cvs
     *  </PRE>
     *
     *  @param argv The argument string.  This class takes in no arguments.
     */
    public static void main( String[] argv )
    {
        System.out.println(VERSTR);
    }
}
