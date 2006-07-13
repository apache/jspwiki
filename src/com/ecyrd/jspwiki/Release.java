/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

/**
 *  Contains release and version information.
 *
 *  @author Janne Jalkanen
 */
public class Release
{
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

    public static final int        VERSION       = 2;
    public static final int        REVISION      = 4;
    public static final int        MINORREVISION = 19;

    /**
     *  This is the generic version string you should use
     *  when printing out the version.  It is of the form "x.y.z-ttt".
     */
    public static final String     VERSTR        = 
        VERSION+"."+REVISION+"."+MINORREVISION+POSTFIX;

    /**
     *  This method is useful for templates, because hopefully it will
     *  not be inlined, and thus any change to version number does not
     *  need recompiling the pages.
     *
     *  @since 2.1.26.
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
     *  % java com.ecyrd.jspwiki.Release
     *  1.9.26-cvs
     *  </PRE>
     */
    public static void main( String argv[] )
    {
        System.out.println(VERSTR);
    }
}
