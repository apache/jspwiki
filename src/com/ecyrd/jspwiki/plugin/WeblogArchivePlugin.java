/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
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

    @SuppressWarnings("unchecked")
    private SortedSet collectMonths( WikiEngine engine, String page )
        throws ProviderException
    {
        Comparator comp = new ArchiveComparator();
        TreeSet<Calendar> res = new TreeSet<Calendar>( comp );

        WeblogPlugin pl = new WeblogPlugin();

        List blogEntries = pl.findBlogEntries( engine.getPageManager(),
                                               page, new Date(0L), new Date() );
        
        for( Iterator i = blogEntries.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage) i.next();

            // FIXME: Not correct, should parse page creation time.

            Date d = p.getLastModified();
            Calendar cal = Calendar.getInstance();
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
     * This is a simple comparator for ordering weblog archive entries.
     * Two dates in the same month are considered equal.
     */
    private static class ArchiveComparator
        implements Comparator
    {

        public int compare( Object a, Object b ) 
        {
            if( a == null || b == null || 
                !(a instanceof Calendar) || !(b instanceof Calendar) )
            {
                throw new ClassCastException( "Invalid calendar supplied for comparison." );
            }
                    
            Calendar ca = (Calendar) a;
            Calendar cb = (Calendar) b;
            if( ca.get( Calendar.YEAR ) == cb.get( Calendar.YEAR ) &&
                ca.get( Calendar.MONTH ) == cb.get( Calendar.MONTH ) )
            {
                return 0;
            }

            return cb.getTime().before( ca.getTime() ) ? 1 : -1;
        }
    }
}
