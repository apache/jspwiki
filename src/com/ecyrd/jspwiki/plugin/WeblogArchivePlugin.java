/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
 *  Creates a list of all weblog entries on a monthly basis.
 *
 *  @since 1.9.21
 */
public class WeblogArchivePlugin implements WikiPlugin
{
    private static Logger     log = Logger.getLogger(WeblogArchivePlugin.class);

    public static final String PARAM_PAGE = "page";

    private SimpleDateFormat m_monthUrlFormat;

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();

        //
        //  Parameters
        //
        String weblogName = (String) params.get( PARAM_PAGE );

        if( weblogName == null ) weblogName = context.getPage().getName();
        

        m_monthUrlFormat = new SimpleDateFormat("'"+
                                                context.getURL( WikiContext.VIEW, weblogName,
                                                                "weblog.startDate='ddMMyy'&amp;weblog.days=%d")+"'");

        StringBuffer sb = new StringBuffer();

        sb.append( "<div class=\"weblogarchive\">\n" );
        

        //
        //  Collect months that have blog entries
        //

        try
        {
            Collection months = collectMonths( engine, weblogName );
            int year = 0;

            //
            //  Output proper HTML.
            //

            sb.append( "<ul>\n" );

            if( months.size() > 0 )
            {
                year = ((Calendar)months.iterator().next()).get( Calendar.YEAR );

                sb.append( "<li class=\"archiveyear\">"+year+"</li>\n" );
            }

            for( Iterator i = months.iterator(); i.hasNext(); )
            {
                Calendar cal = (Calendar) i.next();

                if( cal.get( Calendar.YEAR ) != year )
                {
                    year = cal.get( Calendar.YEAR );

                    sb.append( "<li class=\"archiveyear\">"+year+"</li>\n" );
                }

                sb.append( "  <li>" );

                sb.append( getMonthLink( cal ) );

                sb.append( "</li>\n" );
            }

            sb.append( "</ul>\n" );
            sb.append( "</div>\n" );
        }
        catch( ProviderException ex )
        {
            log.info( "Cannot get archive", ex );
            sb.append("Cannot get archive: "+ex.getMessage());
        }

        return sb.toString();
    }

    private SortedSet collectMonths( WikiEngine engine, String page )
        throws ProviderException
    {
        TreeSet res = new TreeSet();

        WeblogPlugin pl = new WeblogPlugin();

        List blogEntries = pl.findBlogEntries( engine.getPageManager(),
                                               page, new Date(0L), new Date() );
        
        Calendar urCalendar = Calendar.getInstance();
        for( Iterator i = blogEntries.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage) i.next();

            // FIXME: Not correct, should parse page creation time.

            Date d = p.getLastModified();

            Calendar cal = new ArchiveCalendar( urCalendar );
            cal.setTime( d );

            res.add( cal );
        }

        return res;
    }

    private String getMonthLink( Calendar day )
    {
        SimpleDateFormat monthfmt = new SimpleDateFormat( "MMMM" );
        String result;

        if( m_monthUrlFormat == null )
        {
            result = monthfmt.format( day.getTime() );
        }
        else
        {
            Calendar cal = (Calendar)day.clone();
            int firstDay = cal.getActualMinimum( Calendar.DATE );
            int lastDay  = cal.getActualMaximum( Calendar.DATE );

            cal.set( Calendar.DATE, lastDay );
            String url = m_monthUrlFormat.format( cal.getTime() );

            url = TextUtil.replaceString( url, "%d", Integer.toString( lastDay-firstDay+1 ) );

            result = "<a href=\""+url+"\">"+monthfmt.format(cal.getTime())+"</a>";
        }

        return result;

    }

    /**
     *  This is a simple calendar that extends the GregorianCalendar to 
     *  provide a Comparable interface so that it can be put in a Set and sorted.  In
     *  addition, it also evaluates two objects that are in the same month
     *  to be equal.
     */
    private class ArchiveCalendar
        extends GregorianCalendar
        implements Comparable
    {
        public ArchiveCalendar( Calendar cal )
        {
            setTime( cal.getTime() );
        }

        public int compareTo(Object o)
        {
            if( o instanceof Calendar )
            {
                Calendar c = (Calendar) o;

                if( equals( c ) ) return 0;

                return c.getTime().before( getTime() ) ? 1 : -1;
            }

            return 0;
        }

        /**
         *  Returns true, if these objects represent the same month and year.
         */
        public boolean equals( Object o )
        {
            if( o != null && o instanceof Calendar )
            {
                Calendar c = (Calendar) o;

                if( c.get( Calendar.YEAR ) == get( Calendar.YEAR ) &&
                    c.get( Calendar.MONTH ) == get( Calendar.MONTH ) )
                {
                    return true;
                }
            }

            return false;
        }
    }
}
