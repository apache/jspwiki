/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.ProviderException;
import org.apache.log4j.Category;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.text.ParsePosition;
import java.util.*;

/**
 *  Builds a simple weblog.
 *
 *  @since 1.9.21
 */
public class WeblogEntryPlugin implements WikiPlugin
{
    private static Category     log = Category.getInstance(WeblogEntryPlugin.class);
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String weblogName = context.getPage().getName();
        WikiEngine engine = context.getEngine();
        
        StringBuffer sb = new StringBuffer();
        
        try
        {
            SimpleDateFormat fmt = new SimpleDateFormat(WeblogPlugin.DEFAULT_DATEFORMAT);
            String today = fmt.format( new Date() );
            
            int entryNum = findFreeEntry( context.getEngine().getPageManager(),
                                          weblogName,
                                          today );

                        
            String blogPage = WeblogPlugin.makeEntryPage( weblogName,
                                                          today,
                                                          ""+entryNum );

            // FIXME: Generate somehow else.
            sb.append("<A HREF=\""+engine.getEditURL(blogPage)+"\">New entry</A>");
        }
        catch( ProviderException e )
        {
            log.error( "Could not locate blog entries", e );
            throw new PluginException( "Could not locate blog entries: "+e.getMessage() );
        }

        return sb.toString();
    }

    private int findFreeEntry( PageManager mgr,
                               String baseName,
                               String date )
        throws ProviderException
    {
        Collection everyone = mgr.getAllPages();
        int max = 0;

        String startString = WeblogPlugin.makeEntryPage( baseName, date, "" );
        
        for( Iterator i = everyone.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();

            if( p.getName().startsWith(startString) )
            {
                try
                {
                    String probableId = p.getName().substring( startString.length() );

                    int id = Integer.parseInt(probableId);

                    if( id > max )
                    {
                        max = id;
                    }
                }
                catch( NumberFormatException e )
                {
                    log.debug("Was not a log entry: "+p.getName() );
                }
            }
        }
        
        return max+1;
    }
    
}
