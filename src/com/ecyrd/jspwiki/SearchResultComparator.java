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

import java.util.*;

/**
 *  Simple class that decides which search results are more
 *  important than others.
 */
public class SearchResultComparator
    implements Comparator
{
    /**
     *  Compares two SearchResult objects, returning
     *  the one that scored higher.
     */
    public int compare( Object o1, Object o2 )
    {
        SearchResult s1 = (SearchResult)o1;
        SearchResult s2 = (SearchResult)o2;

        // Bigger scores are first.

        int res = s2.getScore() - s1.getScore();

        if( res == 0 )
            res = s1.getPage().getName().compareTo(s2.getPage().getName());

        return res;
    }
}
