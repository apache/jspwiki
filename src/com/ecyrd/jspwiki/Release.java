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

/**
 *  Contains release information.
 *
 *  @author Janne Jalkanen
 */
public class Release
{
    /**
     *  This is the default application name.
     */
    public static final String     APPNAME       = "JSPWiki";

    /** This should be empty when doing a release - otherwise
        keep it as "cvs" so that whenever someone checks out the code,
        they know it is a bleeding-edge version. */
    private static final String    POSTFIX       = "-cvs";

    /**
     *  This should be increased every time you do a release.
     */
    public static final String     RELEASE       = "R4";

    public static final int        VERSION       = 1;
    public static final int        REVISION      = 6;
    public static final int        MINORREVISION = 11;

    /**
     *  This is the generic version string you should use
     *  when printing out the version.
     */
    public static final String     VERSTR        = 
        VERSION+"."+REVISION+"."+MINORREVISION+POSTFIX;
}
