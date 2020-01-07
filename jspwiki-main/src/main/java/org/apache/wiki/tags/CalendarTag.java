/* 
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
package org.apache.wiki.tags;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.util.HttpUtil;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


/**
 *  Provides a nice calendar.  Responds to the following HTTP parameters:
 *  <ul>
 *  <li>calendar.date - If this parameter exists, then the calendar
 *  date is taken from the month and year.  The date must be in ddMMyy
 *  format.
 *  <li>weblog.startDate - If calendar.date parameter does not exist,
 *  we then check this date.
 *  </ul>
 *
 *  If neither calendar.date nor weblog.startDate parameters exist,
 *  then the calendar will default to the current month.
 *
 *  @since 2.0
 */

// FIXME: This class is extraordinarily lacking.
public class CalendarTag extends WikiTagBase {

    private static final long serialVersionUID = 0L;
    private static final Logger log = Logger.getLogger( CalendarTag.class );
    
    private SimpleDateFormat m_pageFormat = null;
    private SimpleDateFormat m_urlFormat = null;
    private SimpleDateFormat m_monthUrlFormat = null;
    private SimpleDateFormat m_dateFormat = new SimpleDateFormat( "ddMMyy" );

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
        m_pageFormat = m_urlFormat = m_monthUrlFormat = null;
        m_dateFormat = new SimpleDateFormat( "ddMMyy" );
    }

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

    /**
     *  Sets the page format.  If a page corresponding to the format is found when
     *  the calendar is being rendered, a link to that page is created.  E.g. if the
     *  format is set to <tt>'Main_blogentry_'ddMMyy</tt>, it works nicely in
     *  conjuction to the WeblogPlugin.
     *  
     *  @param format The format in the SimpleDateFormat fashion.
     *  
     *  @see SimpleDateFormat
     *  @see org.apache.wiki.plugin.WeblogPlugin
     */
    public void setPageformat( final String format )
    {
        m_pageFormat = new SimpleDateFormat( format );
    }

    /**
     *  Set the URL format.  If the pageformat is not set, all dates are
     *  links to pages according to this format.  The pageformat
     *  takes precedence.
     *  
     *  @param format The URL format in the SimpleDateFormat fashion.
     *  @see SimpleDateFormat
     */
    public void setUrlformat( final String format )
    {
        m_urlFormat = new SimpleDateFormat( format );
    }

    /**
     *  Set the format to be used for links for the months.
     *  
     *  @param format The format to set in the SimpleDateFormat fashion.
     *  
     *  @see SimpleDateFormat
     */
    public void setMonthurlformat( final String format )
    {
        m_monthUrlFormat = new SimpleDateFormat( format );
    }

    private String format( final String txt ) {
        final WikiPage p = m_wikiContext.getPage();
        if( p != null ) {
            return TextUtil.replaceString( txt, "%p", p.getName() );
        }

        return txt;
    }

    /**
     *  Returns a link to the given day.
     */
    private String getDayLink( final Calendar day ) {
        final WikiEngine engine = m_wikiContext.getEngine();
        final String result;

        if( m_pageFormat != null ) {
            final String pagename = m_pageFormat.format( day.getTime() );
            
            if( engine.getPageManager().wikiPageExists( pagename ) ) {
                if( m_urlFormat != null ) {
                    final String url = m_urlFormat.format( day.getTime() );
                    result = "<td class=\"link\"><a href=\""+url+"\">"+day.get( Calendar.DATE )+"</a></td>";
                } else {
                    result = "<td class=\"link\"><a href=\""+m_wikiContext.getViewURL( pagename )+"\">"+
                             day.get( Calendar.DATE )+"</a></td>";
                }
            } else {
                result = "<td class=\"days\">"+day.get(Calendar.DATE)+"</td>";
            }
        } else if( m_urlFormat != null ) {
            final String url = m_urlFormat.format( day.getTime() );
            result = "<td><a href=\""+url+"\">"+day.get( Calendar.DATE )+"</a></td>";
        } else {
            result = "<td class=\"days\">"+day.get(Calendar.DATE)+"</td>";
        }

        return format( result );
    }

    private String getMonthLink( final Calendar day ) {
        final SimpleDateFormat monthfmt = new SimpleDateFormat( "MMMM yyyy" );
        final String result;

        if( m_monthUrlFormat == null ) {
            result = monthfmt.format( day.getTime() );
        } else {
            final Calendar cal = (Calendar)day.clone();
            final int firstDay = cal.getActualMinimum( Calendar.DATE );
            final int lastDay  = cal.getActualMaximum( Calendar.DATE );

            cal.set( Calendar.DATE, lastDay );
            String url = m_monthUrlFormat.format( cal.getTime() );

            url = TextUtil.replaceString( url, "%d", Integer.toString( lastDay - firstDay + 1 ) );

            result = "<a href=\""+url+"\">"+monthfmt.format(cal.getTime())+"</a>";
        }

        return format( result );
    }

    private String getMonthNaviLink( final Calendar day, final String txt, String queryString ) {
        final String result;
        queryString = TextUtil.replaceEntities( queryString );
        final Calendar nextMonth = Calendar.getInstance();
        nextMonth.set( Calendar.DATE, 1 );  
        nextMonth.add( Calendar.DATE, -1);
        nextMonth.add( Calendar.MONTH, 1 ); // Now move to 1st day of next month

        if ( day.before( nextMonth ) ) {
            final WikiPage thePage = m_wikiContext.getPage();
            final String pageName = thePage.getName();

            final String calendarDate = m_dateFormat.format( day.getTime() );
            String url = m_wikiContext.getURL( WikiContext.VIEW, pageName,"calendar.date="+calendarDate );

            if( queryString != null && queryString.length() > 0 ) {
                //
                // Ensure that the 'calendar.date=ddMMyy' has been removed from the queryString
                //

                // FIXME: Might be useful to have an entire library of 
                //        routines for this.  Will fail if it's not calendar.date 
                //        but something else.

                final int pos1 = queryString.indexOf("calendar.date=");
                if( pos1 >= 0 ) {
                    String tmp = queryString.substring( 0, pos1 );
                    // FIXME: Will this fail when we use & instead of &amp?
                    // FIXME: should use some parsing routine
                    final int pos2 = queryString.indexOf("&", pos1 ) + 1;
                    if ( ( pos2 > 0 ) && ( pos2 < queryString.length() ) ) {
                        tmp = tmp + queryString.substring(pos2);
                    }
                    queryString = tmp;
                }

                if( queryString.length() > 0 ) {
                    url = url + "&amp;" + queryString;
                }
            }
            result = "<td><a href=\""+url+"\">"+txt+"</a></td>";
        } else {
            result="<td> </td>";
        }    

        return format( result );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag() throws IOException {
        final WikiEngine engine = m_wikiContext.getEngine();
        final JspWriter out = pageContext.getOut();
        final Calendar cal = Calendar.getInstance();
        final Calendar prevCal = Calendar.getInstance();
        final Calendar nextCal = Calendar.getInstance();

        //
        //  Check if there is a parameter in the request to set the date.
        //
        String calendarDate = pageContext.getRequest().getParameter( "calendar.date" );
        if( calendarDate == null ) {
            calendarDate = pageContext.getRequest().getParameter( "weblog.startDate" );
        }
        
        if( calendarDate != null ) {
            try {
                final Date d = m_dateFormat.parse( calendarDate );
                cal.setTime( d );
                prevCal.setTime( d );
                nextCal.setTime( d );
            } catch( final ParseException e ) {
                log.warn( "date format wrong: " + calendarDate );
            }
        }

        cal.set( Calendar.DATE, 1 );     // First, set to first day of month
        prevCal.set( Calendar.DATE, 1 );
        nextCal.set( Calendar.DATE, 1 );

        prevCal.add(Calendar.MONTH, -1); // Now move to first day of previous month
        nextCal.add(Calendar.MONTH, 1);  // Now move to first day of next month

        out.write( "<table class=\"calendar\">\n" );

        final HttpServletRequest httpServletRequest = m_wikiContext.getHttpRequest();
        final String queryString = HttpUtil.safeGetQueryString( httpServletRequest, engine.getContentEncoding() );
        out.write( "<tr>"+
                   getMonthNaviLink(prevCal,"&lt;&lt;", queryString)+
                   "<td colspan=5 class=\"month\">"+
                   getMonthLink( cal )+
                   "</td>"+
                   getMonthNaviLink(nextCal,"&gt;&gt;", queryString)+ 
                   "</tr>\n"
                 );

        final int month = cal.get( Calendar.MONTH );
        cal.set( Calendar.DAY_OF_WEEK, Calendar.MONDAY ); // Then, find the first day of the week.

        out.write( "<tr><td class=\"weekdays\">Mon</td>"+
                   "<td class=\"weekdays\">Tue</td>"+
                   "<td class=\"weekdays\">Wed</td>"+
                   "<td class=\"weekdays\">Thu</td>"+
                   "<td class=\"weekdays\">Fri</td>"+
                   "<td class=\"weekdays\">Sat</td>"+
                   "<td class=\"weekdays\">Sun</td></tr>\n" );

        boolean noMoreDates = false;
        while( !noMoreDates ) {
            out.write( "<tr>" );
            
            for( int i = 0; i < 7; i++ ) {
                final int mth = cal.get( Calendar.MONTH );

                if( mth != month ) {
                    out.write("<td class=\"othermonth\">"+cal.get(Calendar.DATE)+"</td>");
                } else {
                    out.write( getDayLink(cal) );
                }

                cal.add( Calendar.DATE, 1 );
            }

            if( cal.get( Calendar.MONTH ) != month ) {
                noMoreDates = true;
            }

            out.write( "</tr>\n" );
        }

        out.write( "</table>\n" );

        return EVAL_BODY_INCLUDE;
    }

}
