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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.PluginException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.RegExpUtil;
import org.apache.wiki.util.StringTransmutator;
import org.apache.wiki.util.TextUtil;


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
 *  </ul>
 *  
 */
public abstract class AbstractFilteredPlugin
    implements WikiPlugin
{
    private static Logger log = LoggerFactory.getLogger( AbstractFilteredPlugin.class );

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

    /** The parameter name for setting the showAttachment.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_SHOW_ATTACHMENTS = "showAttachments";

    protected           int      m_maxwidth = Integer.MAX_VALUE;
    protected           String   m_before = ""; // null not blank
    protected           String   m_separator = ""; // null not blank
    protected           String   m_after = "\\\\";

    protected           Pattern[] m_exclude;
    protected           Pattern[]  m_include;
    protected           boolean m_showAttachments = true;
    
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
    public void initialize( WikiContext context, Map<String,Object> params )
        throws PluginException
    {
        m_dateFormat = Preferences.getDateFormat( context, TimeFormat.DATETIME );
        m_engine = context.getEngine();
        m_maxwidth = TextUtil.parseIntParameter( (String)params.get( PARAM_MAXWIDTH ), Integer.MAX_VALUE );
        if( m_maxwidth < 0 ) m_maxwidth = 0;

        String showAttachmentsString = (String) params.get( PARAM_SHOW_ATTACHMENTS );
        if( "false".equals( showAttachmentsString ) )
        {
            m_showAttachments = false;
        }

        String s = (String) params.get( PARAM_SEPARATOR );

        if( s != null )
        {
            m_separator = s;
            // pre-2.1.145 there was a separator at the end of the list
            // if they set the parameters, we use the new format of
            // before Item1 after separator before Item2 after separator before Item3 after
            m_after = "";
        }

        s = (String) params.get( PARAM_BEFORE );

        if( s != null )
        {
            m_before = s;
        }

        s = (String) params.get( PARAM_AFTER );

        if( s != null )
        {
            m_after = s;
        }

        s = (String) params.get( PARAM_EXCLUDE );

        if( s != null )
        {
            try
            {
                String[] ptrns = StringUtils.split( s, "," );

                m_exclude = new Pattern[ptrns.length];

                for( int i = 0; i < ptrns.length; i++ )
                {
                    m_exclude[i] = Pattern
                        .compile( RegExpUtil.globToPerl5( ptrns[i].toCharArray(),
                                                                        RegExpUtil.DEFAULT_MASK ) );
                }
            }
            catch( PatternSyntaxException e )
            {
                throw new PluginException( context.getBundle( WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE )
                    .getString( "plugin.abstract.excludeparm.malformed" )
                                           + e.getMessage() );
            }
        }

        // TODO: Cut-n-paste, refactor
        s = (String) params.get( PARAM_INCLUDE );

        if( s != null )
        {
            try
            {
                String[] ptrns = StringUtils.split( s, "," );

                m_include = new Pattern[ptrns.length];

                for( int i = 0; i < ptrns.length; i++ )
                {
                    m_include[i] = Pattern
                        .compile( RegExpUtil.globToPerl5( ptrns[i].toCharArray(),
                                                                        RegExpUtil.DEFAULT_MASK ) );
                }
            }
            catch( PatternSyntaxException e )
            {
                throw new PluginException( context.getBundle( WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE )
                    .getString( "plugin.abstract.includeparm.malformed" )
                                           + e.getMessage() );
            }
        }

        // log.debug( "Requested maximum width is "+m_maxwidth );
        s = (String) params.get(PARAM_SHOW);

        if( s != null )
        {
            if( s.equalsIgnoreCase( "count" ) )
            {
                m_show = "count";
            }
        }

        s = (String) params.get( PARAM_LASTMODIFIED );

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
                    throw new PluginException( context.getBundle( WikiPlugin.CORE_PLUGINS_RESOURCEBUNDLE )
                        .getString( "plugin.abstract.showLastModified" ) );
                }
            }
        }
    }
    
    /**
     *  Filters a collection according to the include and exclude -parameters.
     *  
     *  @param items The collection to filter.
     *  @return A filtered collection.
     */
    protected <T extends Object>Collection<T> filterCollection( Collection<T> items )
    {
        ArrayList<T> filteredItems = new ArrayList<T>();

        for( T item : items )
        {
            String pageName = null;
            if( item instanceof WikiPage )
            {
                pageName = ((WikiPage) item).getName();
            }
            else if ( item instanceof WikiPath )
            {
                pageName = ((WikiPath) item).toString();
            }
            else if ( item instanceof String )
            {
                pageName = (String) item;
            }

            //
            //  If include parameter exists, then by default we include only those
            //  pages in it (excluding the ones in the exclude pattern list).
            //
            //  include='*' means the same as no include.
            //
            boolean includeThis = m_include == null;

            if( m_include != null && pageName != null )
            {
                for( int j = 0; j < m_include.length; j++ )
                {
                    Matcher matcher = m_include[j].matcher( pageName );
                    if( matcher.matches() )
                    {
                        includeThis = true;
                        break;
                    }
                }
            }

            if( m_exclude != null && pageName != null )
            {
                for( int j = 0; j < m_exclude.length; j++ )
                {
                    Matcher matcher = m_exclude[j].matcher( pageName );
                    if( matcher.matches() )
                    {
                        includeThis = false;
                        break; // The inner loop, continue on the next item
                    }
                }
            }

            if( includeThis )
            {
                // show the page if it's not an attachment, or it's and
                // attachment and show_attachment=true
                boolean isAttachment = pageName.contains( "/" );
                if( !isAttachment || (isAttachment && m_showAttachments) )
                {
                    if( item instanceof WikiPage || item instanceof WikiPath || item instanceof String )
                    {
                        filteredItems.add( item );
                    }
                }
                
                //
                //  if we want to show the last modified date of the most recently changed page, we keep a "high watermark" here:
                WikiPage page = null;
                if( m_lastModified )
                {
                    try
                    {
                        page = m_engine.getPage( pageName );

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
                    catch( PageNotFoundException e ) {}
                    catch( ProviderException e )
                    {
                        log.debug( "Error while getting page data", e );
                    }
                }
            }
        }

        return filteredItems;
    }

    /**
     *  Makes WikiText from a Collection.
     *
     *  @param links Collection to make into WikiText.
     *  @param separator Separator string to use.
     *  @param numItems How many items to show.
     *  @return The WikiText
     */
    protected String wikitizeCollection( Collection<WikiPath> links, String separator, int numItems )
    {
        if( links == null || links.isEmpty() )
            return "";

        StringBuilder output = new StringBuilder();

        Iterator<WikiPath> it     = links.iterator();
        int      count  = 0;

        //
        //  The output will be B Item[1] A S B Item[2] A S B Item[3] A
        //
        while( it.hasNext() && ( (count < numItems) || ( numItems == ALL_ITEMS ) ) )
        {
            WikiPath value = it.next();

            if( count > 0 )
            {
                output.append( m_after );
                output.append( m_separator );
            }

            output.append( m_before );

            // Make a Wiki markup link. See TranslatorReader.
            output.append( "[" + m_engine.beautifyTitle(value) + "|" + value + "]" );
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
}
