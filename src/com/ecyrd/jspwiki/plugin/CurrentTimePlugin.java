/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 *  Just displays the current date and time.
 *
 *  @author Janne Jalkanen
 */
public class CurrentTimePlugin
    implements WikiPlugin
{
    private static Category log = Category.getInstance( CurrentTimePlugin.class );

    public static final String DEFAULT_FORMAT = "HH:mm:ss dd-MMM-yyyy zzzz";

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String formatString = (String)params.get("format");

        if( formatString == null )
        {
            formatString = DEFAULT_FORMAT;
        }

        log.debug("Date format string is: "+formatString);

        try
        {
            SimpleDateFormat fmt = new SimpleDateFormat( formatString );

            Date d = new Date();  // Now.

            return fmt.format( d );
        }
        catch( IllegalArgumentException e )
        {
            throw new PluginException("You specified bad format: "+e.getMessage());
        }
    }

}
