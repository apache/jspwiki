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
package org.apache.wiki.plugin;

import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;


/**
 *  Creates a list of all weblog entries on a monthly basis.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>page</b> - the page name</li>
 *  </ul>
 *  
 *  @since 1.9.21
 */
public class WeblogArchivePlugin implements WikiPlugin
{
    private static Logger     log = LoggerFactory.getLogger(WeblogArchivePlugin.class);

    /** Parameter name for setting the page.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_PAGE = "page";

    private SimpleDateFormat m_monthUrlFormat;

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String,Object> params )
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

        StringBuilder sb = new StringBuilder();

        sb.append( "<div class=\"weblogarchive\">\n" );
        

        //
        //  Collect months that have blog entries
        //

        try
        {
            Collection<Calendar> months = collectMonths( engine, weblogName );
            int year = 0;

            //
            //  Output proper HTML.
            //

            sb.append( "<ul>\n" );

            if( months.size() > 0 )
            {
                year = months.iterator().next().get( Calendar.YEAR );

                sb.append( "<li class=\"archiveyear\">"+year+"</li>\n" );
            }

            for( Calendar cal : months )
            {
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

    private SortedSet<Calendar> collectMonths( WikiEngine engine, String page )
        throws ProviderException
    {
        Comparator<Calendar> comp = new ArchiveComparator();
        TreeSet<Calendar> res = new TreeSet<Calendar>( comp );

        WeblogPlugin pl = new WeblogPlugin();

        List<WikiPage> blogEntries = pl.findBlogEntries( engine.getContentManager(),
                                               page, new Date(0L), new Date() );
        
        for( WikiPage p : blogEntries )
        {
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
        implements Comparator<Calendar>
    {

        public int compare( Calendar a, Calendar b ) 
        {
            if( a == null || b == null )
            {
                throw new ClassCastException( "Invalid calendar supplied for comparison." );
            }
                    
            if( a.get( Calendar.YEAR ) == b.get( Calendar.YEAR ) &&
                a.get( Calendar.MONTH ) == b.get( Calendar.MONTH ) )
            {
                return 0;
            }

            return b.getTime().before( a.getTime() ) ? 1 : -1;
        }
    }
}
