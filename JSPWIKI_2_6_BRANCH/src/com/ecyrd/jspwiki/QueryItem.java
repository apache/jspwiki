/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
 *  This simple class just fulfils the role of a container
 *  for searches.  It tells the word and whether it is requested or not.
 *
 *  @author Janne Jalkanen
 */
public class QueryItem
{
    /** The word is required to be in the pages */
    public static final int REQUIRED  = 1;

    /** The word may NOT be in the pages */
    public static final int FORBIDDEN = -1;

    /** The word should be in the pages, but the search engine may
        use its own discretion. */
    public static final int REQUESTED = 0;

    /** The word that is being searched */
    public String word;

    /** The type of the word.  See above for types.  The default
        is REQUESTED. */
    public int    type = REQUESTED;
}
