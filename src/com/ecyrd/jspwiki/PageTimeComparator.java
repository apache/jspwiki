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

import java.util.*;

public class PageTimeComparator
    implements Comparator
{
    public int compare( Object o1, Object o2 )
    {
        WikiPage w1 = (WikiPage)o1;
        WikiPage w2 = (WikiPage)o2;
            
        // This gets most recent on top
        int timecomparison = w2.getLastModified().compareTo( w1.getLastModified() );

        if( timecomparison == 0 )
        {
            return w1.getName().compareTo( w2.getName() );
        }

        return timecomparison;
    }
}
