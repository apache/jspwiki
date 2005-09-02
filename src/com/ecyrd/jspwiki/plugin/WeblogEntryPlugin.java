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
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  Builds a simple weblog.
 *
 *  @since 1.9.21
 */
public class WeblogEntryPlugin implements WikiPlugin
{
    private static Logger     log = Logger.getLogger(WeblogEntryPlugin.class);

    public static final int MAX_BLOG_ENTRIES = 10000; // Just a precaution.

    public static final String PARAM_ENTRYTEXT = "entrytext";

    public String getNewEntryPage( WikiEngine engine, String blogName )
        throws ProviderException
    {
        SimpleDateFormat fmt = new SimpleDateFormat(WeblogPlugin.DEFAULT_DATEFORMAT);
        String today = fmt.format( new Date() );
            
        int entryNum = findFreeEntry( engine.getPageManager(),
                                      blogName,
                                      today );

                        
        String blogPage = WeblogPlugin.makeEntryPage( blogName,
                                                      today,
                                                      ""+entryNum );

        return blogPage;
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String weblogName = context.getPage().getName();
        WikiEngine engine = context.getEngine();
        
        StringBuffer sb = new StringBuffer();

        String entryText = (String) params.get(PARAM_ENTRYTEXT);
        if( entryText == null ) entryText = "New entry";
        
        String url = context.getURL( WikiContext.NONE, "NewBlogEntry.jsp", "page="+engine.encodeName(weblogName) );
            
        //sb.append("<a href=\""+engine.getEditURL(blogPage)+"\">New entry</a>");
        //sb.append("<a href=\""+engine.getBaseURL()+"NewBlogEntry.jsp?page="+engine.encodeName(weblogName)+"\">"+entryText+"</a>");
        sb.append("<a href=\""+url+"\">"+entryText+"</a>");

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

        //
        //  Find the first page that has no page lock.
        //
        int idx = max+1;

        while( idx < MAX_BLOG_ENTRIES )
        {
            WikiPage page = new WikiPage( mgr.getEngine(),
                                          WeblogPlugin.makeEntryPage( baseName, 
                                                                      date, 
                                                                      Integer.toString(idx) ) );
            PageLock lock = mgr.getCurrentLock( page );

            if( lock == null )
            {
                break;
            }

            idx++;
        }
        
        return idx;
    }
    
}
