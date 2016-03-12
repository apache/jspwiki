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
package org.apache.wiki.plugin;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.wiki.PageManager;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.ParserStagePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.parser.PluginContent;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;
import org.apache.wiki.util.TextUtil;

/**
 *  <p>Builds a simple weblog.
 *  The pageformat can use the following params:</p>
 *  <p>%p - Page name</p>
 *  <p>Parameters:</p>
 *  <ul>
 *    <li><b>page</b> - which page is used to do the blog; default is the current page.</li>
 *    <li><b>entryFormat</b> - how to display the date on pages, using the J2SE SimpleDateFormat
 *       syntax. Defaults to the current locale's DateFormat.LONG format
 *       for the date, and current locale's DateFormat.SHORT for the time.
 *       Thus, for the US locale this will print dates similar to
 *       this: September 4, 2005 11:54 PM</li>
 *    <li><b>days</b> - how many days the weblog aggregator should show.  If set to
 *      "all", shows all pages.</li>
 *    <li><b>pageformat</b> - What the entry pages should look like.</li>
 *    <li><b>startDate</b> - Date when to start.  Format is "ddMMyy."</li>
 *    <li><b>maxEntries</b> - How many entries to show at most.</li>
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
    private static final Pattern HEADINGPATTERN;

    /** How many days are considered by default.  Default value is {@value} */
    private static final int     DEFAULT_DAYS = 7;
    private static final String  DEFAULT_PAGEFORMAT = "%p_blogentry_";

    /** The default date format used in the blog entry page names. */
    public static final String   DEFAULT_DATEFORMAT = "ddMMyy";

    /** Parameter name for the startDate.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_STARTDATE    = "startDate";
    /** Parameter name for the entryFormat.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_ENTRYFORMAT  = "entryFormat";
    /** Parameter name for the days.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_DAYS         = "days";
    /** Parameter name for the allowComments.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_ALLOWCOMMENTS = "allowComments";
    /** Parameter name for the maxEntries.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_MAXENTRIES   = "maxEntries";
    /** Parameter name for the page.  Value is <tt>{@value}</tt>. */
    public static final String  PARAM_PAGE         = "page";

    /** The attribute which is stashed to the WikiPage attributes to check if a page
     *  is a weblog or not. You may check for its presence.
     */
    public static final String  ATTR_ISWEBLOG      = "weblogplugin.isweblog";

    static
    {
        // This is a pretty ugly, brute-force regex. But it will do for now...
        HEADINGPATTERN = Pattern.compile("(<h[1-4][^>]*>)(.*)(</h[1-4]>)", Pattern.CASE_INSENSITIVE);
    }

    /**
     *  Create an entry name based on the blogname, a date, and an entry number.
     *
     *  @param pageName Name of the blog
     *  @param date The date (in ddMMyy format)
     *  @param entryNum The entry number.
     *  @return A formatted page name.
     */
    public static String makeEntryPage( String pageName,
                                        String date,
                                        String entryNum )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date+"_"+entryNum;
    }

    /**
     *  Return just the basename for entires without date and entry numebr.
     *
     *  @param pageName The name of the blog.
     *  @return A formatted name.
     */
    public static String makeEntryPage( String pageName )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName);
    }

    /**
     *  Returns the entry page without the entry number.
     *
     *  @param pageName Blog name.
     *  @param date The date.
     *  @return A base name for the blog entries.
     */
    public static String makeEntryPage( String pageName, String date )
    {
        return TextUtil.replaceString(DEFAULT_PAGEFORMAT,"%p",pageName)+date;
    }

    /**
     *  {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public String execute( WikiContext context, Map<String, String> params )
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

        if( (weblogName = params.get(PARAM_PAGE)) == null )
        {
            weblogName = context.getPage().getName();
        }

        if( (days = context.getHttpParameter( "weblog."+PARAM_DAYS )) == null )
        {
            days = params.get( PARAM_DAYS );
        }

        if( ( params.get(PARAM_ENTRYFORMAT)) == null )
        {
            entryFormat = Preferences.getDateFormat( context, TimeFormat.DATETIME );
        }
        else
        {
            entryFormat = new SimpleDateFormat( params.get(PARAM_ENTRYFORMAT) );
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


        if( (startDay = params.get(PARAM_STARTDATE)) == null )
        {
            startDay = context.getHttpParameter( "weblog."+PARAM_STARTDATE );
        }

        if( TextUtil.isPositive( params.get(PARAM_ALLOWCOMMENTS) ) )
        {
            hasComments = true;
        }

        maxEntries = TextUtil.parseIntParameter( params.get(PARAM_MAXENTRIES),
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

        StringBuilder sb = new StringBuilder();

        try
        {
            List<WikiPage> blogEntries = findBlogEntries( engine.getPageManager(),
                                                          weblogName,
                                                          startTime.getTime(),
                                                          stopTime.getTime() );

            Collections.sort( blogEntries, new PageDateComparator() );

            sb.append("<div class=\"weblog\">\n");

            for( Iterator< WikiPage > i = blogEntries.iterator(); i.hasNext() && maxEntries-- > 0 ; )
            {
                WikiPage p = i.next();

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
    private void addEntryHTML(WikiContext context, DateFormat entryFormat, boolean hasComments, StringBuilder buffer, WikiPage entry)
        throws ProviderException
    {
        WikiEngine engine = context.getEngine();
        ResourceBundle rb = Preferences.getBundle(context, WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE);

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
        Matcher matcher = HEADINGPATTERN.matcher( html );
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
        buffer.append( "<a href=\""+entryCtx.getURL(WikiContext.VIEW, entry.getName())+"\">"+rb.getString("weblogentryplugin.permalink")+"</a>" );
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

            String addcomment = rb.getString("weblogentryplugin.addcomment");

            buffer.append( "<a href=\""+
                       entryCtx.getURL(WikiContext.COMMENT,
                                       commentPageName,
                                       "nc="+numComments)+
                       "\">"+
                       MessageFormat.format(addcomment, numComments)
                       +"</a>" );
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
        ArrayList<WikiPage> result = new ArrayList<WikiPage>();

        baseName = makeEntryPage( baseName );
        SimpleDateFormat fmt = new SimpleDateFormat(DEFAULT_DATEFORMAT);

        for( Iterator i = everyone.iterator(); i.hasNext(); )
        {
            WikiPage p = (WikiPage)i.next();

            String pageName = p.getName();

            if( pageName.startsWith( baseName ) )
            {
                try
                {
                    WikiPage firstVersion = mgr.getPageInfo( pageName, 1 );
                    result.add( firstVersion );
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
    private static class PageDateComparator implements Comparator<WikiPage>
    {
        public int compare( WikiPage page1, WikiPage page2 )
        {
            if( page1 == null || page2 == null )
            {
                return 0;
            }

            return page2.getLastModified().compareTo( page1.getLastModified() );
        }
    }

    /**
     *  Mark us as being a real weblog.
     *  {@inheritDoc}
     */
    public void executeParser(PluginContent element, WikiContext context, Map<String, String> params)
    {
        context.getPage().setAttribute( ATTR_ISWEBLOG, "true" );
    }
}
