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
public class WeblogPlugin implements WikiPlugin
{
    private static Category     log = Category.getInstance(WeblogPlugin.class);

    public static final int     DEFAULT_DAYS = 7;
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String weblogName = context.getPage().getName();
        Calendar   startTime;
        Calendar   stopTime;
        WikiEngine engine = context.getEngine();
        
        int numDays = TextUtil.parseIntParameter( (String)params.get("days"),
                                                  DEFAULT_DAYS );

        stopTime = Calendar.getInstance();
        startTime = Calendar.getInstance();
        startTime.add( Calendar.DAY_OF_MONTH, -numDays );

        StringBuffer sb = new StringBuffer();
        
        try
        {
            Collection blogEntries = findBlogEntries( context.getEngine().getPageManager(),
                                                      weblogName,
                                                      startTime.getTime(),
                                                      stopTime.getTime() );

            SimpleDateFormat entryDateFmt = new SimpleDateFormat("dd-MMM-yyyy HH:mm");

            sb.append("<DIV CLASS=\"weblog\">\n");
            for( Iterator i = blogEntries.iterator(); i.hasNext(); )
            {
                WikiPage p = (WikiPage) i.next();

                sb.append("<DIV CLASS=\"weblogheading\">");

                Date entryDate = engine.getPage( p.getName(), 1 ).getLastModified();
                sb.append( entryDateFmt.format(entryDate) );

                // FIXME: Add permalink here.
                
                sb.append("</DIV>\n");

                sb.append("<DIV CLASS=\"weblogentry\">");

                sb.append( engine.getHTML( context, p ) );
                
                sb.append("</DIV>\n");
            }
            
            sb.append("</DIV>\n");
        }
        catch( ProviderException e )
        {
            log.error( "Could not locate blog entries", e );
            throw new PluginException( "Could not locate blog entries: "+e.getMessage() );
        }

        return sb.toString();
    }

    private Collection findBlogEntries( PageManager mgr,
                                        String baseName, Date start, Date end )
        throws ProviderException
    {
        Collection everyone = mgr.getAllPages();
        ArrayList  result = new ArrayList();
        
        for( Iterator i = everyone.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();

            StringTokenizer tok = new StringTokenizer(p.getName(),"-");

            if( tok.countTokens() < 3 )
                continue;
            
            try
            {
                String name = tok.nextToken();

                if( !name.equals(baseName) ) continue;
                
                String day  = tok.nextToken();

                SimpleDateFormat fmt = new SimpleDateFormat("ddMMyy");
                Date pageDay = fmt.parse( day, new ParsePosition(0) );

                if( pageDay.after(start) && pageDay.before(end) )
                {
                    result.add(p);
                }
            }
            catch( NoSuchElementException e )
            {
                // Nope, something odd.
            }
            

        }
        
        return result;
    }
    
}
