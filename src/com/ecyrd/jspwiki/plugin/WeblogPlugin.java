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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  <p>Builds a simple weblog.
 *  The pageformat can use the following params:</p>
 *  <p>%p - Page name</p>
 *  <p>Parameters:</p>
 *  <ul>
 *    <li>page - which page is used to do the blog; default is the current page.</li>
 *    <li>entryFormat - how to display the date on pages, using the J2SE SimpleDateFormat
 *       syntax. Defaults to the current locale's DateFormat.LONG format
 *       for the date, and current locale's DateFormat.SHORT for the time.
 *       Thus, for the US locale this will print dates similar to
 *       this: September 4, 2005 11:54 PM</li>
 *    <li>days - how many days the weblog aggregator should show.  If set to
 *      "all", shows all pages.</li>
 *    <li>pageformat - What the entry pages should look like.</li>
 *    <li>startDate - Date when to start.  Format is "ddMMyy."</li>
 *    <li>maxEntries - How many entries to show at most.</li>
 *  </ul>
 *  <p>The "days" and "startDate" can also be sent in HTTP parameters,
 *  and the names are "weblog.days" and "weblog.startDate", respectively.</p>
 *  <p>The weblog plugin also adds an attribute to each page it is on: 
 *  "weblogplugin.isweblog" is set to "true".  This can be used to quickly
 *  peruse pages which have weblogs.</p>
 *  @since 1.9.21
 */

// FIXME: Add "entries" param as an alternative to "days".
// FIXME: Entries arrive in wrong order.

public class WeblogPlugin 
    implements WikiPlugin,
               InitializablePlugin
{
    private static Logger     log = Logger.getLogger(WeblogPlugin.class);
    private static final DateFormat DEFAULT_ENTRYFORMAT 
                                = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.SHORT);
    private static final Pattern headingPattern;
    
    public static final int     DEFAULT_DAYS = 7;
    public static final String  DEFAULT_PAGEFORMAT = "%p_blogentry_";

    public static final String  DEFAULT_DATEFORMAT = "ddMMyy";

    public static final String  PARAM_STARTDATE    = "startDate";
    public static final String  PARAM_ENTRYFORMAT  = "entryFormat";
    public static final String  PARAM_DAYS         = "days";
    public static final String  PARAM_ALLOWCOMMENTS = "allowComments";
    public static final String  PARAM_MAXENTRIES   = "maxEntries";
    public static final String  PARAM_PAGE         = "page";

    public static final String  ATTR_ISWEBLOG      = "weblogplugin.isweblog";
    
    static
    {
    	    // This is a pretty ugly, brute-force regex. But it will do for now...
        headingPattern = Pattern.compile("(<h[1-4].*>)(.*)(</h[1-4]>)", Pattern.CASE_INSENSITIVE);
    }

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

    /**
     *  Just sets the "I am a weblog" mark.
     */
    public void initialize( WikiContext context, Map params )
    {
        context.getPage().setAttribute(ATTR_ISWEBLOG, "true");
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        Calendar   startTime;
        Calendar   stopTime;
        int        numDays;
        WikiEngine engine = context.getEngine();

        //
        //  Parse parameters.
        //
        String  days;
        DateFormat entryFormat;
        String  startDay = null;
        boolean hasComments = false;
        int     maxEntries;
        String  weblogName;

        if( (weblogName = (String) params.get(PARAM_PAGE)) == null )
        {
            weblogName = context.getPage().getName();
        }

        if( (days = context.getHttpParameter( "weblog."+PARAM_DAYS )) == null )
        {
            days = (String) params.get( PARAM_DAYS );
        }
        
        if( ( params.get(PARAM_ENTRYFORMAT)) == null )
        {
            entryFormat = DEFAULT_ENTRYFORMAT;
        }
        else
        {
            entryFormat = new SimpleDateFormat( (String)params.get(PARAM_ENTRYFORMAT) );
        }

        if( days != null && days.equalsIgnoreCase("all") )
        {
            numDays = Integer.MAX_VALUE;
        }
        else
        {
            numDays = TextUtil.parseIntParameter( days, DEFAULT_DAYS );
        }


        if( (startDay = (String)params.get(PARAM_STARTDATE)) == null )
        {
            startDay = context.getHttpParameter( "weblog."+PARAM_STARTDATE );
        }

        if( TextUtil.isPositive( (String)params.get(PARAM_ALLOWCOMMENTS) ) )
        {
            hasComments = true;
        }

        maxEntries = TextUtil.parseIntParameter( (String)params.get(PARAM_MAXENTRIES),
                                                 Integer.MAX_VALUE );

        //
        //  Determine the date range which to include.
        //

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
        //  Mark this to be a weblog
        //

        context.getPage().setAttribute(ATTR_ISWEBLOG, "true");

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
            List blogEntries = findBlogEntries( engine.getPageManager(),
                                                weblogName,
                                                startTime.getTime(),
                                                stopTime.getTime() );

            Collections.sort( blogEntries, new PageDateComparator() );

            sb.append("<div class=\"weblog\">\n");
            for( Iterator i = blogEntries.iterator(); i.hasNext() && maxEntries-- > 0 ; )
            {
                WikiPage p = (WikiPage) i.next();

                sb.append("<div class=\"weblogentry\">\n");

                //
                //  Heading
                //
                sb.append("<div class=\"weblogentryheading\">\n");

                Date entryDate = p.getLastModified();
                sb.append( entryFormat.format(entryDate) );

                sb.append("</div>\n");

                //
                //  Append the text of the latest version.  Reset the
                //  context to that page.
                //

                WikiContext entryCtx = (WikiContext) context.clone();
                entryCtx.setPage( p );

                String html = engine.getHTML( entryCtx, engine.getPage(p.getName()) );
                
                // Extract the first h1/h2/h3 as title, and replace with null
                sb.append("<div class=\"weblogentrytitle\">\n");
                Matcher matcher = headingPattern.matcher( html );
                if ( matcher.find() )
                {
                    String title = matcher.group(2);
                    html = matcher.replaceFirst("");
                    sb.append( title );
                }
                else
                {
                    sb.append( p.getName() );
                }
                sb.append("</div>\n");
                
                sb.append("<div class=\"weblogentrybody\">\n");
                sb.append( html );
                sb.append("</div>\n");

                //
                //  Append footer
                //
                sb.append("<div class=\"weblogentryfooter\">\n");

                String author = p.getAuthor();

                if( author != null )
                {
                    if( engine.pageExists(author) )
                    {
                        author = "<a href=\""+entryCtx.getURL( WikiContext.VIEW, author )+"\">"+engine.beautifyTitle(author)+"</a>";
                    }
                }
                else
                {
                    author = "AnonymousCoward";
                }

                sb.append("By "+author+"&nbsp;&nbsp;");
                sb.append( "<a href=\""+entryCtx.getURL(WikiContext.VIEW, p.getName())+"\">Permalink</a>" );
                String commentPageName = TextUtil.replaceString( p.getName(),
                                                                 "blogentry",
                                                                 "comments" );

                if( hasComments )
                {
                    int numComments = guessNumberOfComments( engine, commentPageName );

                    //
                    //  We add the number of comments to the URL so that
                    //  the user's browsers would realize that the page
                    //  has changed.
                    //
                    sb.append( "&nbsp;&nbsp;" );
                    sb.append( "<a target=\"_blank\" href=\""+
                               entryCtx.getURL(WikiContext.COMMENT,
                                               commentPageName,
                                               "nc="+numComments)+
                               "\">Comments? ("+
                               numComments+
                               ")</a>" );
                }
                
                sb.append("</div>\n");

                //
                //  Done, close
                //
                sb.append("</div>\n");
            }
            
            sb.append("</div>\n");
        }
        catch( ProviderException e )
        {
            log.error( "Could not locate blog entries", e );
            throw new PluginException( "Could not locate blog entries: "+e.getMessage() );
        }

        return sb.toString();
    }

    private int guessNumberOfComments( WikiEngine engine, String commentpage )
        throws ProviderException
    {
        String pagedata = engine.getPureText( commentpage, WikiProvider.LATEST_VERSION );

        if( pagedata == null || pagedata.trim().length() == 0 )
        {
            return 0;
        }
        
        return TextUtil.countSections( pagedata );
    }

    /**
     *  Attempts to locate all pages that correspond to the 
     *  blog entry pattern.  Will only consider the days on the dates; not the hours and minutes.
     *
     *  Returns a list of pages with their FIRST revisions.
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
                //
                //  Check the creation date from the page name.
                //  We do this because RCSFileProvider is very slow at getting a
                //  specific page version.
                //
                try
                {
                    //log.debug("Checking: "+pageName);
                    int firstScore = pageName.indexOf('_',baseName.length()-1 );
                    if( firstScore != -1 && firstScore+1 < pageName.length() )
                    {
                        int secondScore = pageName.indexOf('_', firstScore+1);

                        if( secondScore != -1 )
                        {
                            String creationDate = pageName.substring( firstScore+1, secondScore );

                            //log.debug("   Creation date: "+creationDate);

                            Date pageDay = fmt.parse( creationDate );
                
                            //
                            //  Add the first version of the page into the list.  This way
                            //  the page modified date becomes the page creation date.
                            //
                            if( pageDay != null && pageDay.after(start) && pageDay.before(end) )
                            {
                                WikiPage firstVersion = mgr.getPageInfo( pageName, 1 );
                                result.add( firstVersion );
                            }
                        }
                    }
                }
                catch( Exception e )
                {
                    log.debug("Page name :"+pageName+" was suspected as a blog entry but it isn't because of parsing errors",e);
                }
            }
        }
        
        return result;
    }

    /**
     *  Reverse comparison.
     */
    private class PageDateComparator implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            if( o1 == null || o2 == null ) 
            { 
                return 0; 
            }
            
            WikiPage page1 = (WikiPage)o1;
            WikiPage page2 = (WikiPage)o2;

            return page2.getLastModified().compareTo( page1.getLastModified() );
        }
    }
}
