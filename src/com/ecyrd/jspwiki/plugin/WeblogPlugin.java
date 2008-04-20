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

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.parser.PluginContent;
import com.ecyrd.jspwiki.preferences.Preferences;
import com.ecyrd.jspwiki.preferences.Preferences.TimeFormat;
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
    implements WikiPlugin, ParserStagePlugin
{
    private static Logger     log = Logger.getLogger(WeblogPlugin.class);
    private static final Pattern headingPattern;

    /** How many days are considered by default.  Default value is {@value} */
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

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        Calendar   startTime;
        Calendar   stopTime;
        int        numDays = DEFAULT_DAYS;
        WikiEngine engine = context.getEngine();
        AuthorizationManager mgr = engine.getAuthorizationManager();
        
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
            entryFormat = Preferences.getDateFormat( context, TimeFormat.DATETIME );
        }
        else
        {
            entryFormat = new SimpleDateFormat( (String)params.get(PARAM_ENTRYFORMAT) );
        }

        if( days != null )
        {
            if( days.equalsIgnoreCase("all") )
            {
                numDays = Integer.MAX_VALUE;
            }
            else
            {
                numDays = TextUtil.parseIntParameter( days, DEFAULT_DAYS );
            }
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

                if( mgr.checkPermission( context.getWikiSession(), 
                                         new PagePermission(p, PagePermission.VIEW_ACTION) ) )
                {
                    addEntryHTML(context, entryFormat, hasComments, sb, p);
                }
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

    /**
     *  Generates HTML for an entry.
     *  
     *  @param context
     *  @param entryFormat
     *  @param hasComments  True, if comments are enabled.
     *  @param buffer       The buffer to which we add.
     *  @param entry
     *  @throws ProviderException
     */
    private void addEntryHTML(WikiContext context, DateFormat entryFormat, boolean hasComments, StringBuffer buffer, WikiPage entry) 
        throws ProviderException
    {
        WikiEngine engine = context.getEngine();
        buffer.append("<div class=\"weblogentry\">\n");

        //
        //  Heading
        //
        buffer.append("<div class=\"weblogentryheading\">\n");

        Date entryDate = entry.getLastModified();
        buffer.append( entryFormat.format(entryDate) );

        buffer.append("</div>\n");

        //
        //  Append the text of the latest version.  Reset the
        //  context to that page.
        //

        WikiContext entryCtx = (WikiContext) context.clone();
        entryCtx.setPage( entry );

        String html = engine.getHTML( entryCtx, engine.getPage(entry.getName()) );

        // Extract the first h1/h2/h3 as title, and replace with null
        buffer.append("<div class=\"weblogentrytitle\">\n");
        Matcher matcher = headingPattern.matcher( html );
        if ( matcher.find() )
        {
            String title = matcher.group(2);
            html = matcher.replaceFirst("");
            buffer.append( title );
        }
        else
        {
            buffer.append( entry.getName() );
        }
        buffer.append("</div>\n");

        buffer.append("<div class=\"weblogentrybody\">\n");
        buffer.append( html );
        buffer.append("</div>\n");

        //
        //  Append footer
        //
        buffer.append("<div class=\"weblogentryfooter\">\n");
            
        String author = entry.getAuthor();

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

        buffer.append("By "+author+"&nbsp;&nbsp;");
        buffer.append( "<a href=\""+entryCtx.getURL(WikiContext.VIEW, entry.getName())+"\">Permalink</a>" );
        String commentPageName = TextUtil.replaceString( entry.getName(),
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
            buffer.append( "&nbsp;&nbsp;" );
            buffer.append( "<a target=\"_blank\" href=\""+
                       entryCtx.getURL(WikiContext.COMMENT,
                                       commentPageName,
                                       "nc="+numComments)+
                       "\">Comments? ("+
                       numComments+
                       ")</a>" );
        }

        buffer.append("</div>\n");

        //
        //  Done, close
        //
        buffer.append("</div>\n");
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
     *  @param mgr A PageManager which is used to get the pages
     *  @param baseName The basename (e.g. "Main" if you want "Main_blogentry_xxxx")
     *  @param start The date which is the first to be considered
     *  @param end   The end date which is the last to be considered
     *  @return a list of pages with their FIRST revisions.
     *  @throws ProviderException If something goes wrong
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
    private static class PageDateComparator implements Comparator
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

    /** 
     *  Mark us as being a real weblog. 
     *  {@inheritDoc}
     */
    public void executeParser(PluginContent element, WikiContext context, Map params)
    {
        context.getPage().setAttribute( ATTR_ISWEBLOG, "true" );
    }
}
