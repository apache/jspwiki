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
import java.text.ParseException;
import java.util.*;

/**
 *  Builds a simple weblog.
 *  <P>
 *  The pageformat can use the following params:<br>
 *  %p - Page name<br>
 *
 *  <B>Parameters</B>
 *  <UL>
 *    <LI>days - how many days the weblog aggregator should show.
 *    <LI>pageformat - What the entries should look like.
 *    <LI>startDate - Date when to start.  Format is "ddMMyy";
 *  </UL>
 *
 *  The "days" and "startDate" can also be sent in HTTP parameters,
 *  and the names are "weblog.days" and "weblog.startDate", respectively.
 *
 *  @since 1.9.21
 */

// FIXME: Add "entries" param as an alternative to "days".
// FIXME: Entries arrive in wrong order.

public class WeblogPlugin implements WikiPlugin
{
    private static Category     log = Category.getInstance(WeblogPlugin.class);

    public static final int     DEFAULT_DAYS = 7;
    public static final String  DEFAULT_PAGEFORMAT = "%p_blogentry_";

    public static final String  DEFAULT_DATEFORMAT = "ddMMyy";

    public static String makeEntryPage( String pageName,
                                        String date,
                                        String entryNum )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date+"_"+entryNum;
    }

    public static String makeEntryPage( String pageName )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName);
    }

    public static String makeEntryPage( String pageName, String date )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date;
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        String weblogName = context.getPage().getName();
        Calendar   startTime;
        Calendar   stopTime;
        int        numDays;
        WikiEngine engine = context.getEngine();

        //
        //  Parse parameters.
        //
        String days;
        String startDay = null;

        if( (days = (String) params.get("days")) == null )
        {
            days = context.getHttpParameter( "weblog.days" );
        }

        numDays = TextUtil.parseIntParameter( days, DEFAULT_DAYS );


        if( (startDay = (String)params.get("startDate")) == null )
        {
            startDay = context.getHttpParameter( "weblog.startDate" );
        }

        startTime = Calendar.getInstance();
        stopTime  = Calendar.getInstance();

        if( startDay != null )
        {
            SimpleDateFormat fmt = new SimpleDateFormat( DEFAULT_DATEFORMAT );
            try
            {
                Date d = fmt.parse( startDay );
                startTime.setTime( d );
                stopTime.setTime( d );
            }
            catch( ParseException e )
            {
                return "Illegal time format: "+startDay;
            }
        }

        //
        //  We make a wild guess here that nobody can do millisecond
        //  accuracy here.
        //
        startTime.add( Calendar.DAY_OF_MONTH, -numDays );
        startTime.set( Calendar.HOUR, 0 );
        startTime.set( Calendar.MINUTE, 0 );
        startTime.set( Calendar.SECOND, 0 );
        stopTime.set( Calendar.HOUR, 23 );
        stopTime.set( Calendar.MINUTE, 59 );
        stopTime.set( Calendar.SECOND, 59 );

        StringBuffer sb = new StringBuffer();
        
        try
        {
            List blogEntries = findBlogEntries( context.getEngine().getPageManager(),
                                                weblogName,
                                                startTime.getTime(),
                                                stopTime.getTime() );

            Collections.sort( blogEntries, new PageNameComparator() );

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

                sb.append("<div class=\"weblogpermalink\">");
                sb.append( "<a href=\"Wiki.jsp?page="+engine.encodeName(p.getName())+"\">Permalink</a>" );
                sb.append("</div>");
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

    /**
     *  Attempts to locate all pages that correspond to the 
     *  blog entry pattern.
     *
     */
    public List findBlogEntries( PageManager mgr,
                                 String baseName, Date start, Date end )
        throws ProviderException
    {
        Collection everyone = mgr.getAllPages();
        ArrayList  result = new ArrayList();

        baseName = makeEntryPage( baseName );
        SimpleDateFormat fmt = new SimpleDateFormat(DEFAULT_DATEFORMAT);
 
        for( Iterator i = everyone.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();

            String pageName = p.getName();

            if( pageName.startsWith( baseName ) )
            {
                String entry = pageName.substring( baseName.length() );

                int idx = entry.indexOf('_');

                if( idx == -1 )
                    continue;

                String day = entry.substring( 0, idx );

                //
                //  Note that we're not interested in the actual modification
                //  date, we use the page name for the creation date.
                //
                Date pageDay = fmt.parse( day, new ParsePosition(0) );
                
                if( pageDay != null )
                {
                    if( pageDay.after(start) && pageDay.before(end) )
                    {
                        result.add(p);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     *  Reverse comparison.
     */
    private class PageNameComparator implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            if( o1 == null || o2 == null ) 
            { 
                return 0; 
            }
            
            WikiPage page1 = (WikiPage)o1;
            WikiPage page2 = (WikiPage)o2;

            return page2.getName().compareTo( page1.getName() );
        }
    }
}
