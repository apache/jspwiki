/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.io.StringReader;

import java.util.Calendar;
import java.text.SimpleDateFormat;

import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
//import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.providers.ProviderException;


/**
 *  Provides a nice calendar.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */

// FIXME: This class is extraordinarily lacking.

public class CalendarTag
    extends WikiTagBase
{
    private String m_year  = null;
    private String m_month = null;
    private SimpleDateFormat m_pageFormat = null;

    /*
    public void setYear( String year )
    {
        m_year = year;
    }

    public void setMonth( String month )
    {
        m_month = month;
    }
    */

    public void setPageformat( String format )
    {
        m_pageFormat = new SimpleDateFormat(format);
    }

    private String getDayLink( Calendar day )
    {
        WikiEngine engine = m_wikiContext.getEngine();

        if( m_pageFormat != null )
        {
            String pagename = m_pageFormat.format( day.getTime() );
            
            if( engine.pageExists( pagename ) )
            {
                return "<a href=\""+engine.getViewURL( pagename )+"\">"+
                       day.get( Calendar.DATE )+"</a>";
            }
        }

        return Integer.toString( day.get(Calendar.DATE) );
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        SimpleDateFormat monthfmt = new SimpleDateFormat( "MMMM yyyy" );

        JspWriter out = pageContext.getOut();
        Calendar cal = Calendar.getInstance();

        cal.set( Calendar.DATE, 1 ); // First, set to first day of month

        out.write( "<table class=\"calendar\">\n" );

        out.write( "<tr><td colspan=7>"+monthfmt.format( cal.getTime() )+"</td>" );

        int month = cal.get( Calendar.MONTH );
        cal.set( Calendar.DAY_OF_WEEK, Calendar.MONDAY ); // Then, find the first day of the week.

        out.write( "<tr><td>Mon</td><td>Tue</td><td>Wed</td><td>Thu</td><td>Fri</td><td>Sat</td><td>Sun</td></tr>\n" );

        boolean noMoreDates = false;
        while( !noMoreDates )
        {
            out.write( "<tr>" );
            
            for( int i = 0; i < 7; i++ )
            {
                int day = cal.get( Calendar.DATE );
                int mth = cal.get( Calendar.MONTH );

                if( mth != month )
                {
                    out.write("<td class=\"othermonth\">");
                }
                else
                {
                    out.write("<td>");
                }

                out.write( getDayLink(cal) );

                out.write("</td>");

                cal.add( Calendar.DATE, 1 );
            }

            if( cal.get( Calendar.MONTH ) != month )
            {
                noMoreDates = true;
            }

            out.write( "</tr>\n" );
        }

        out.write( "</table>\n" );

        return EVAL_BODY_INCLUDE;
    }

}
