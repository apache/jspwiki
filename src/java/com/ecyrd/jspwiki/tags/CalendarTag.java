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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.ParseException;

import javax.servlet.jsp.JspWriter;
import javax.servlet.http.HttpServletRequest;

import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.util.TextUtil;


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
 *
 *  @since 2.0
 */

// FIXME: This class is extraordinarily lacking.

public class CalendarTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
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
     *  @see com.ecyrd.jspwiki.plugin.WeblogPlugin
     */
    public void setPageformat( String format )
    {
        m_pageFormat = new SimpleDateFormat(format);
    }

    /**
     *  Set the URL format.  If the pageformat is not set, all dates are
     *  links to pages according to this format.  The pageformat
     *  takes precedence.
     *  
     *  @param format The URL format in the SimpleDateFormat fashion.
     *  @see SimpleDateFormat
     */
    public void setUrlformat( String format )
    {
        m_urlFormat = new SimpleDateFormat(format);
    }

    /**
     *  Set the format to be used for links for the months.
     *  
     *  @param format The format to set in the SimpleDateFormat fashion.
     *  
     *  @see SimpleDateFormat
     */
    public void setMonthurlformat( String format )
    {
        m_monthUrlFormat = new SimpleDateFormat( format );
    }

    private String format( String txt )
    {
        WikiPage p = m_wikiContext.getPage();

        if( p != null )
        {
            return TextUtil.replaceString( txt, "%p", p.getName() );
        }

        return txt;
    }

    /**
     *  Returns a link to the given day.
     */
    private String getDayLink( Calendar day )
    {
        WikiEngine engine = m_wikiContext.getEngine();
        String result = "";

        if( m_pageFormat != null )
        {
            String pagename = m_pageFormat.format( day.getTime() );
            
            if( engine.pageExists( pagename ) )
            {
                if( m_urlFormat != null )
                {
                    String url = m_urlFormat.format( day.getTime() );

                    result = "<td class=\"link\"><a href=\""+url+"\">"+day.get( Calendar.DATE )+"</a></td>";
                }
                else
                {
                    result = "<td class=\"link\"><a href=\""+m_wikiContext.getViewURL( pagename )+"\">"+
                             day.get( Calendar.DATE )+"</a></td>";
                }
            }
            else
            {
                result = "<td class=\"days\">"+day.get(Calendar.DATE)+"</td>";
            }
        }
        else if( m_urlFormat != null )
        {
            String url = m_urlFormat.format( day.getTime() );

            result = "<td><a href=\""+url+"\">"+day.get( Calendar.DATE )+"</a></td>";
        }
        else
        {
            result = "<td class=\"days\">"+day.get(Calendar.DATE)+"</td>";
        }

        return format(result);
    }

    private String getMonthLink( Calendar day )
    {
        SimpleDateFormat monthfmt = new SimpleDateFormat( "MMMM yyyy" );
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

        return format(result);
        
    }
    private String getMonthNaviLink( Calendar day, String txt, String queryString )
    {
        String result = "";
        queryString = TextUtil.replaceEntities( queryString );
        Calendar nextMonth = Calendar.getInstance();
        nextMonth.set( Calendar.DATE, 1 );  
        nextMonth.add( Calendar.DATE, -1);
        nextMonth.add( Calendar.MONTH, 1 ); // Now move to 1st day of next month

        if ( day.before(nextMonth) )
        {
            WikiPage thePage = m_wikiContext.getPage();
            String pageName = thePage.getName();

            String calendarDate = m_dateFormat.format(day.getTime());
            String url = m_wikiContext.getURL( WikiContext.VIEW, pageName, 
                                               "calendar.date="+calendarDate );

            if ( (queryString != null) && (queryString.length() > 0) )
            {
                //
                // Ensure that the 'calendar.date=ddMMyy' has been removed 
                // from the queryString
                //

                // FIXME: Might be useful to have an entire library of 
                //        routines for this.  Will fail if it's not calendar.date 
                //        but something else.

                int pos1 = queryString.indexOf("calendar.date=");
                if (pos1 >= 0)
                {
                    String tmp = queryString.substring(0,pos1);
                    // FIXME: Will this fail when we use & instead of &amp?
                    // FIXME: should use some parsing routine
                    int pos2 = queryString.indexOf("&",pos1) + 1;
                    if ( (pos2 > 0) && (pos2 < queryString.length()) )
                    {
                        tmp = tmp + queryString.substring(pos2);
                    }
                    queryString = tmp;
                }

                if( queryString != null && queryString.length() > 0 )
                {
                    url = url + "&amp;"+queryString;
                }
            }
            result = "<td><a href=\""+url+"\">"+txt+"</a></td>";
        }
        else
        {
            result="<td> </td>";
        }    

        return format(result);
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        WikiEngine       engine   = m_wikiContext.getEngine();
        JspWriter        out      = pageContext.getOut();
        Calendar         cal      = Calendar.getInstance();
        Calendar         prevCal  = Calendar.getInstance();
        Calendar         nextCal  = Calendar.getInstance();

        //
        //  Check if there is a parameter in the request to set the date.
        //
        String calendarDate = pageContext.getRequest().getParameter( "calendar.date" );
        if( calendarDate == null )
        {
            calendarDate = pageContext.getRequest().getParameter( "weblog.startDate" );
        }
        
        if( calendarDate != null )
        {
            try
            {
                Date d = m_dateFormat.parse( calendarDate );
                cal.setTime( d );
                prevCal.setTime( d );
                nextCal.setTime( d );
            }
            catch( ParseException e )
            {
                log.warn( "date format wrong: "+calendarDate );
            }
        }

        cal.set( Calendar.DATE, 1 );     // First, set to first day of month
        prevCal.set( Calendar.DATE, 1 );
        nextCal.set( Calendar.DATE, 1 );

        prevCal.add(Calendar.MONTH, -1); // Now move to first day of previous month
        nextCal.add(Calendar.MONTH, 1);  // Now move to first day of next month

        out.write( "<table class=\"calendar\">\n" );

        HttpServletRequest httpServletRequest = m_wikiContext.getHttpRequest();
        String queryString = engine.safeGetQueryString( httpServletRequest );
        out.write( "<tr>"+
                   getMonthNaviLink(prevCal,"&lt;&lt;", queryString)+
                   "<td colspan=5 class=\"month\">"+
                   getMonthLink( cal )+
                   "</td>"+
                   getMonthNaviLink(nextCal,"&gt;&gt;", queryString)+ 
                   "</tr>\n"
                 );

        int month = cal.get( Calendar.MONTH );
        cal.set( Calendar.DAY_OF_WEEK, Calendar.MONDAY ); // Then, find the first day of the week.

        out.write( "<tr><td class=\"weekdays\">Mon</td>"+
                   "<td class=\"weekdays\">Tue</td>"+
                   "<td class=\"weekdays\">Wed</td>"+
                   "<td class=\"weekdays\">Thu</td>"+
                   "<td class=\"weekdays\">Fri</td>"+
                   "<td class=\"weekdays\">Sat</td>"+
                   "<td class=\"weekdays\">Sun</td></tr>\n" );

        boolean noMoreDates = false;
        while( !noMoreDates )
        {
            out.write( "<tr>" );
            
            for( int i = 0; i < 7; i++ )
            {
                int mth = cal.get( Calendar.MONTH );

                if( mth != month )
                {
                    out.write("<td class=\"othermonth\">"+cal.get(Calendar.DATE)+"</td>");
                }
                else
                {
                    out.write( getDayLink(cal) );
                }

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
