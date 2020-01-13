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

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.StringTransmutator;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.pages.PageSorter;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.util.comparators.CollatorComparator;
import org.apache.wiki.util.comparators.HumanComparator;
import org.apache.wiki.util.comparators.JavaNaturalComparator;
import org.apache.wiki.util.comparators.LocaleComparator;

import java.io.IOException;
import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *  This is a base class for all plugins using referral things.
 *
 *  <p>Parameters (also valid for all subclasses of this class) : </p>
 *  <ul>
 *  <li><b>maxwidth</b> - maximum width of generated links</li>
 *  <li><b>separator</b> - separator between generated links (wikitext)</li>
 *  <li><b>after</b> - output after the link</li>
 *  <li><b>before</b> - output before the link</li>
 *  <li><b>exclude</b> -  a regular expression of pages to exclude from the list. </li>
 *  <li><b>include</b> -  a regular expression of pages to include in the list. </li>
 *  <li><b>show</b> - value is either "pages" (default) or "count".  When "count" is specified, shows only the count
 *      of pages which match. (since 2.8)</li>
 *  <li><b>showLastModified</b> - When show=count, shows also the last modified date. (since 2.8)</li>
 *  <li><b>sortOrder</b> - specifies the sort order for the resulting list.  Options are
 *  'human', 'java', 'locale' or a <code>RuleBasedCollator</code> rule string. (since 2.8.3)</li>
 *  </ul>
 *
 */
public abstract class AbstractReferralPlugin implements WikiPlugin
{
    private static Logger log = Logger.getLogger( AbstractReferralPlugin.class );

    /** Magic value for rendering all items. */
    public static final int    ALL_ITEMS              = -1;

    /** Parameter name for setting the maximum width.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_MAXWIDTH         = "maxwidth";

    /** Parameter name for the separator string.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SEPARATOR        = "separator";

    /** Parameter name for the output after the link.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_AFTER            = "after";

    /** Parameter name for the output before the link.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_BEFORE           = "before";

    /** Parameter name for setting the list of excluded patterns.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_EXCLUDE          = "exclude";

    /** Parameter name for setting the list of included patterns.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_INCLUDE          = "include";

    /** Parameter name for the show parameter.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SHOW             = "show";

    /** Parameter name for setting show to "pages".  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SHOW_VALUE_PAGES = "pages";

    /** Parameter name for setting show to "count".  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SHOW_VALUE_COUNT = "count";

    /** Parameter name for showing the last modification count.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_LASTMODIFIED     = "showLastModified";

    /** Parameter name for specifying the sort order.  Value is <tt>{@value}</tt>. */
    protected static final String PARAM_SORTORDER        = "sortOrder";
    protected static final String PARAM_SORTORDER_HUMAN  = "human";
    protected static final String PARAM_SORTORDER_JAVA   = "java";
    protected static final String PARAM_SORTORDER_LOCALE = "locale";

    protected           int      m_maxwidth = Integer.MAX_VALUE;
    protected           String   m_before = ""; // null not blank
    protected           String   m_separator = ""; // null not blank
    protected           String   m_after = "\\\\";

    protected           Pattern[]  m_exclude;
    protected           Pattern[]  m_include;
    protected           PageSorter m_sorter;

    protected           String m_show = "pages";
    protected           boolean m_lastModified=false;
    // the last modified date of the page that has been last modified:
    protected           Date m_dateLastModified = new Date(0);
    protected           SimpleDateFormat m_dateFormat;

    protected           WikiEngine m_engine;

    /**
     * @param context the wiki context
     * @param params parameters for initializing the plugin
     * @throws PluginException if any of the plugin parameters are malformed
     */
    // FIXME: The compiled pattern strings should really be cached somehow.
    public void initialize( WikiContext context, Map<String, String> params )
        throws PluginException
    {
        m_dateFormat = Preferences.getDateFormat( context, TimeFormat.DATETIME );
        m_engine = context.getEngine();
        m_maxwidth = TextUtil.parseIntParameter( params.get( PARAM_MAXWIDTH ), Integer.MAX_VALUE );
        if( m_maxwidth < 0 ) m_maxwidth = 0;

        String s = params.get( PARAM_SEPARATOR );

        if( s != null )
        {
            m_separator = TextUtil.replaceEntities( s );
            // pre-2.1.145 there was a separator at the end of the list
            // if they set the parameters, we use the new format of
            // before Item1 after separator before Item2 after separator before Item3 after
            m_after = "";
        }

        s = params.get( PARAM_BEFORE );

        if( s != null )
        {
            m_before = s;
        }

        s = params.get( PARAM_AFTER );

        if( s != null )
        {
            m_after = s;
        }

        s = params.get( PARAM_EXCLUDE );

        if( s != null )
        {
            try
            {
                PatternCompiler pc = new GlobCompiler();

                String[] ptrns = StringUtils.split( s, "," );

                m_exclude = new Pattern[ptrns.length];

                for( int i = 0; i < ptrns.length; i++ )
                {
                    m_exclude[i] = pc.compile( ptrns[i] );
                }
            }
            catch( MalformedPatternException e )
            {
                throw new PluginException("Exclude-parameter has a malformed pattern: "+e.getMessage());
            }
        }

        // TODO: Cut-n-paste, refactor
        s = params.get( PARAM_INCLUDE );

        if( s != null )
        {
            try
            {
                PatternCompiler pc = new GlobCompiler();

                String[] ptrns = StringUtils.split( s, "," );

                m_include = new Pattern[ptrns.length];

                for( int i = 0; i < ptrns.length; i++ )
                {
                    m_include[i] = pc.compile( ptrns[i] );
                }
            }
            catch( MalformedPatternException e )
            {
                throw new PluginException("Include-parameter has a malformed pattern: "+e.getMessage());
            }
        }

        // log.debug( "Requested maximum width is "+m_maxwidth );
        s = params.get(PARAM_SHOW);

        if( s != null )
        {
            if( s.equalsIgnoreCase( "count" ) )
            {
                m_show = "count";
            }
        }

        s = params.get( PARAM_LASTMODIFIED );

        if( s != null )
        {
            if( s.equalsIgnoreCase( "true" ) )
            {
                if( m_show.equals( "count" ) )
                {
                    m_lastModified = true;
                }
                else
                {
                    throw new PluginException( "showLastModified=true is only valid if show=count is also specified" );
                }
            }
        }

        initSorter( context, params );
    }

    protected List< WikiPage > filterWikiPageCollection( Collection< WikiPage > pages ) {
        List< String > pageNames = filterCollection( pages.stream()
                                                          .map( page -> page.getName() )
                                                          .collect( Collectors.toList() ) );
        return pages.stream()
                    .filter( wikiPage -> pageNames.contains( wikiPage.getName() ) )
                    .collect( Collectors.toList() );
    }

    /**
     *  Filters a collection according to the include and exclude parameters.
     *
     *  @param c The collection to filter.
     *  @return A filtered collection.
     */
    protected List< String > filterCollection( Collection< String > c )
    {
        ArrayList< String > result = new ArrayList<>();

        PatternMatcher pm = new Perl5Matcher();

        for( Iterator< String > i = c.iterator(); i.hasNext(); )
        {
            String pageName = i.next();

            //
            //  If include parameter exists, then by default we include only those
            //  pages in it (excluding the ones in the exclude pattern list).
            //
            //  include='*' means the same as no include.
            //
            boolean includeThis = m_include == null;

            if( m_include != null )
            {
                for( int j = 0; j < m_include.length; j++ )
                {
                    if( pm.matches( pageName, m_include[j] ) )
                    {
                        includeThis = true;
                        break;
                    }
                }
            }

            if( m_exclude != null )
            {
                for( int j = 0; j < m_exclude.length; j++ )
                {
                    if( pm.matches( pageName, m_exclude[j] ) )
                    {
                        includeThis = false;
                        break; // The inner loop, continue on the next item
                    }
                }
            }

            if( includeThis )
            {
                result.add( pageName );
                //
                //  if we want to show the last modified date of the most recently change page, we keep a "high watermark" here:
                WikiPage page = null;
                if( m_lastModified )
                {
                    page = m_engine.getPageManager().getPage( pageName );
                    if( page != null )
                    {
                        Date lastModPage = page.getLastModified();
                        if( log.isDebugEnabled() )
                        {
                            log.debug( "lastModified Date of page " + pageName + " : " + m_dateLastModified );
                        }
                        if( lastModPage.after( m_dateLastModified ) )
                        {
                            m_dateLastModified = lastModPage;
                        }
                    }

                }
            }
        }

        return result;
    }

    /**
     *  Filters and sorts a collection according to the include and exclude parameters.
     *
     *  @param c The collection to filter.
     *  @return A filtered and sorted collection.
     */
    protected List< String > filterAndSortCollection( Collection< String > c ) {
        List< String > result = filterCollection( c );
        result.sort( m_sorter );
        return result;
    }

    /**
     *  Makes WikiText from a Collection.
     *
     *  @param links Collection to make into WikiText.
     *  @param separator Separator string to use.
     *  @param numItems How many items to show.
     *  @return The WikiText
     */
    protected String wikitizeCollection( Collection< String > links, String separator, int numItems )
    {
        if( links == null || links.isEmpty() )
            return "";

        StringBuilder output = new StringBuilder();

        Iterator< String > it = links.iterator();
        int count = 0;

        //
        //  The output will be B Item[1] A S B Item[2] A S B Item[3] A
        //
        while( it.hasNext() && ( (count < numItems) || ( numItems == ALL_ITEMS ) ) )
        {
            String value = it.next();

            if( count > 0 )
            {
                output.append( m_after );
                output.append( m_separator );
            }

            output.append( m_before );

            // Make a Wiki markup link. See TranslatorReader.
            output.append( "[" + m_engine.getRenderingManager().beautifyTitle(value) + "|" + value + "]" );
            count++;
        }

        //
        //  Output final item - if there have been none, no "after" is printed
        //
        if( count > 0 ) output.append( m_after );

        return output.toString();
    }

    /**
     *  Makes HTML with common parameters.
     *
     *  @param context The WikiContext
     *  @param wikitext The wikitext to render
     *  @return HTML
     *  @since 1.6.4
     */
    protected String makeHTML( WikiContext context, String wikitext )
    {
        String result = "";

        RenderingManager mgr = m_engine.getRenderingManager();

        try
        {
            MarkupParser parser = mgr.getParser(context, wikitext);

            parser.addLinkTransmutator( new CutMutator(m_maxwidth) );
            parser.enableImageInlining( false );

            WikiDocument doc = parser.parse();

            result = mgr.getHTML( context, doc );
        }
        catch( IOException e )
        {
            log.error("Failed to convert page data to HTML", e);
        }

        return result;
    }

    /**
     *  A simple class that just cuts a String to a maximum
     *  length, adding three dots after the cutpoint.
     */
    private static class CutMutator implements StringTransmutator
    {
        private int m_length;

        public CutMutator( int length )
        {
            m_length = length;
        }

        public String mutate( WikiContext context, String text )
        {
            if( text.length() > m_length )
            {
                return text.substring( 0, m_length ) + "...";
            }

            return text;
        }
    }

    /**
     * Helper method to initialize the comparator for this page.
     */
    private void initSorter( WikiContext context, Map< String, String > params ) {
        String order = params.get( PARAM_SORTORDER );
        if( order == null || order.length() == 0 ) {
            // Use the configured comparator
            m_sorter = context.getEngine().getPageManager().getPageSorter();
        } else if( order.equalsIgnoreCase( PARAM_SORTORDER_JAVA ) ) {
            // use Java "natural" ordering
            m_sorter = new PageSorter( JavaNaturalComparator.DEFAULT_JAVA_COMPARATOR );
        } else if( order.equalsIgnoreCase( PARAM_SORTORDER_LOCALE ) ) {
            // use this locale's ordering
            m_sorter = new PageSorter( LocaleComparator.DEFAULT_LOCALE_COMPARATOR );
        } else if( order.equalsIgnoreCase( PARAM_SORTORDER_HUMAN ) ) {
            // use human ordering
            m_sorter = new PageSorter( HumanComparator.DEFAULT_HUMAN_COMPARATOR );
        } else {
            try
            {
                Collator collator = new RuleBasedCollator( order );
                collator.setStrength( Collator.PRIMARY );
                m_sorter = new PageSorter( new CollatorComparator( collator ) );
            }
            catch( ParseException pe )
            {
                log.info( "Failed to parse requested collator - using default ordering", pe );
                m_sorter = context.getEngine().getPageManager().getPageSorter();
            }
        }
    }

}
