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

import java.io.Serializable;
import java.util.*;
import org.apache.log4j.Logger;

// FIXME: Does not implement equals().
public class PageTimeComparator
    implements Comparator, Serializable
{
    private static final long serialVersionUID = 0L;

    static Logger log = Logger.getLogger( PageTimeComparator.class ); 

    public int compare( Object o1, Object o2 )
    {
        WikiPage w1 = (WikiPage)o1;
        WikiPage w2 = (WikiPage)o2;

        if( w1 == null || w2 == null ) 
        {
            log.error( "W1 or W2 is NULL in PageTimeComparator!");
            return 0; // FIXME: Is this correct?
        }

        if( w1.getLastModified() == null )
        {
            log.error( "NULL MODIFY DATE WITH "+w1.getName() );
            return 0;
        }
        else if( w2.getLastModified() == null )
        {
            log.error( "NULL MODIFY DATE WITH "+w2.getName() );
            return 0;
        }

        // This gets most recent on top
        int timecomparison = w2.getLastModified().compareTo( w1.getLastModified() );

        if( timecomparison == 0 )
        {
            return w1.getName().compareTo( w2.getName() );
        }

        return timecomparison;
    }
}
