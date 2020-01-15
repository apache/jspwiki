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
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.MalformedPatternException;
import org.apache.oro.text.regex.Pattern;
import org.apache.oro.text.regex.PatternCompiler;
import org.apache.oro.text.regex.PatternMatcher;
import org.apache.oro.text.regex.Perl5Matcher;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.InitializablePlugin;
import org.apache.wiki.api.plugin.WikiPlugin;
import org.apache.wiki.event.WikiEngineEvent;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.event.WikiPageRenameEvent;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.util.TextUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;


/**
 * This plugin counts the number of times a page has been viewed.<br/>
 * Parameters:
 * <ul>
 * <li>count=yes|no</li>
 * <li>show=none|count|list</li>
 * <li>entries=maximum number of list entries to be returned</li>
 * <li>min=minimum page count to be listed</li>
 * <li>max=maximum page count to be listed</li>
 * <li>sort=name|count</li>
 * </ul>
 * Default values:<br/>
 * <code>show=none  sort=name</code>
 * 
 * @since 2.8
 */
public class PageViewPlugin extends AbstractReferralPlugin implements WikiPlugin, InitializablePlugin {

    private static final Logger log = Logger.getLogger( PageViewPlugin.class );

    /** The page view manager. */
    private static PageViewManager c_singleton = null;

    /** Constant for the 'count' parameter / value. */
    private static final String PARAM_COUNT = "count";

    /** Name of the 'entries' parameter. */
    private static final String PARAM_MAX_ENTRIES = "entries";

    /** Name of the 'max' parameter. */
    private static final String PARAM_MAX_COUNT = "max";

    /** Name of the 'min' parameter. */
    private static final String PARAM_MIN_COUNT = "min";

    /** Name of the 'refer' parameter. */
    private static final String PARAM_REFER = "refer";

    /** Name of the 'sort' parameter. */
    private static final String PARAM_SORT = "sort";

    /** Constant for the 'none' parameter value. */
    private static final String STR_NONE = "none";

    /** Constant for the 'list' parameter value. */
    private static final String STR_LIST = "list";

    /** Constant for the 'yes' parameter value. */
    private static final String STR_YES = "yes";

    /** Constant for empty string. */
    private static final String STR_EMPTY = "";

    /** Constant for Wiki markup separator. */
    private static final String STR_SEPARATOR = "----";

    /** Constant for comma-separated list separator. */
    private static final String STR_COMMA = ",";

    /** Constant for no-op glob expression. */
    private static final String STR_GLOBSTAR = "*";

    /** Constant for file storage. */
    private static final String COUNTER_PAGE = "PageCount.txt";

    /** Constant for storage interval in seconds. */
    private static final int STORAGE_INTERVAL = 60;

    /**
     * Initialize the PageViewPlugin and its singleton.
     * 
     * @param engine The wiki engine.
     */
    public void initialize( WikiEngine engine )
    {

        log.info( "initializing PageViewPlugin" );

        synchronized( this )
        {
            if( c_singleton == null )
            {
                c_singleton = new PageViewManager(  );
            }
            c_singleton.initialize( engine );
        }
    }

    /**
     * Cleanup the singleton reference.
     */
    private void cleanup()
    {
        log.info( "cleaning up PageView Manager" );

        c_singleton = null;
    }

    /**
     *  {@inheritDoc}
     */
    public String execute( WikiContext context, Map<String, String> params ) throws PluginException
    {
        PageViewManager manager = c_singleton;
        String result = STR_EMPTY;

        if( manager != null )
        {
            result = manager.execute( context, params );
        }

        return result;
    }

    /**
     * Page view manager, handling all storage.
     */
    public final class PageViewManager implements WikiEventListener
    {
        /** Are we initialized? */
        private boolean m_initialized = false;

        /** The page counters. */
        private Map<String, Counter> m_counters = null;

        /** The page counters in storage format. */
        private Properties m_storage = null;

        /** Are all changes stored? */
        private boolean m_dirty = false;

        /** The page count storage background thread. */
        private Thread m_pageCountSaveThread = null;

        /** The work directory. */
        private String m_workDir = null;

        /** Comparator for descending sort on page count. */
        private final Comparator<Object> m_compareCountDescending = new Comparator<Object>() {
            public int compare( Object o1, Object o2 )
            {
                final int v1 = getCount( o1 );
                final int v2 = getCount( o2 );
                return (v1 == v2) ? ((String) o1).compareTo( (String) o2 ) : (v1 < v2) ? 1 : -1;
            }
        };

        /**
         * Initialize the page view manager.
         * 
         * @param engine The wiki engine.
         */
        public synchronized void initialize( WikiEngine engine )
        {
            log.info( "initializing PageView Manager" );

            m_workDir = engine.getWorkDir();

            engine.addWikiEventListener( this );

            if( m_counters == null )
            {
                // Load the counters into a collection
                m_storage = new Properties();
                m_counters = new TreeMap<String, Counter>();

                loadCounters();
            }

            // backup counters every 5 minutes
            if( m_pageCountSaveThread == null )
            {
                m_pageCountSaveThread = new CounterSaveThread( engine, 5 * STORAGE_INTERVAL, this );
                m_pageCountSaveThread.start();
            }

            m_initialized = true;
        }

        /**
         * Handle the shutdown event via the page counter thread.
         * 
         */
        private synchronized void handleShutdown()
        {
            log.info( "handleShutdown: The counter store thread was shut down." );

            cleanup();

            if( m_counters != null )
            {

                m_dirty = true;
                storeCounters();

                m_counters.clear();
                m_counters = null;

                m_storage.clear();
                m_storage = null;
            }

            m_initialized = false;

            m_pageCountSaveThread = null;
        }

        /**
         * Inspect wiki events for shutdown.
         * 
         * @param event The wiki event to inspect.
         */
        public void actionPerformed( WikiEvent event )
        {
            if( event instanceof WikiEngineEvent )
            {
                if( event.getType() == WikiEngineEvent.SHUTDOWN )
                {
                    log.info( "Detected wiki engine shutdown" );
                    handleShutdown();
                }
            } 
            else if( (event instanceof WikiPageRenameEvent) && (event.getType() == WikiPageRenameEvent.PAGE_RENAMED) )
            {
                String oldPageName = ((WikiPageRenameEvent) event).getOldPageName();
                String newPageName = ((WikiPageRenameEvent) event).getNewPageName();
                Counter oldCounter = m_counters.get(oldPageName);
                if ( oldCounter != null )
                {
                    m_storage.remove(oldPageName);
                    m_counters.put(newPageName, oldCounter);
                    m_storage.setProperty(newPageName, oldCounter.toString());
                    m_counters.remove(oldPageName);
                    m_dirty = true;
                }
            }
            else if( (event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.PAGE_DELETED) )
            {
                 String pageName = ((WikiPageEvent) event).getPageName();
                 m_storage.remove(pageName);
                 m_counters.remove(pageName);
            }
        }

        /**
         * Count a page hit, present a pages' counter or output a list of page counts.
         * 
         * @param context the wiki context
         * @param params the plugin parameters
         * @return String Wiki page snippet
         * @throws PluginException Malformed pattern parameter.
         */
        public String execute( WikiContext context, Map<String, String> params ) throws PluginException
        {
            WikiEngine engine = context.getEngine();
            WikiPage page = context.getPage();
            String result = STR_EMPTY;

            if( page != null )
            {
                // get parameters
                String pagename = page.getName();
                String count = params.get( PARAM_COUNT );
                String show = params.get( PARAM_SHOW );
                int entries = TextUtil.parseIntParameter( params.get( PARAM_MAX_ENTRIES ), Integer.MAX_VALUE );
                final int max = TextUtil.parseIntParameter( params.get( PARAM_MAX_COUNT ), Integer.MAX_VALUE );
                final int min = TextUtil.parseIntParameter( params.get( PARAM_MIN_COUNT ), Integer.MIN_VALUE );
                String sort = params.get( PARAM_SORT );
                String body = params.get( DefaultPluginManager.PARAM_BODY );
                Pattern[] exclude = compileGlobs( PARAM_EXCLUDE, params.get( PARAM_EXCLUDE ) );
                Pattern[] include = compileGlobs( PARAM_INCLUDE, params.get( PARAM_INCLUDE ) );
                Pattern[] refer = compileGlobs( PARAM_REFER, params.get( PARAM_REFER ) );
                PatternMatcher matcher = (null != exclude || null != include || null != refer) ? new Perl5Matcher() : null;
                boolean increment = false;

                // increment counter?
                if( STR_YES.equals( count ) )
                {
                    increment = true;
                }
                else
                {
                    count = null;
                }

                // default increment counter?
                if( (show == null || STR_NONE.equals( show )) && count == null )
                {
                    increment = true;
                }

                // filter on referring pages?
                Collection<String> referrers = null;

                if( refer != null )
                {
                    ReferenceManager refManager = engine.getReferenceManager();

                    Iterator< String > iter = refManager.findCreated().iterator();

                    while ( iter != null && iter.hasNext() )
                    {
                        String name = iter.next();
                        boolean use = false;

                        for( int n = 0; !use && n < refer.length; n++ )
                        {
                            use = matcher.matches( name, refer[n] );
                        }

                        if( use )
                        {
                            Collection< String > refs = engine.getReferenceManager().findReferrers( name );

                            if( refs != null && !refs.isEmpty() )
                            {
                                if( referrers == null )
                                {
                                    referrers = new HashSet<String>();
                                }
                                referrers.addAll( refs );
                            }
                        }
                    }
                }

                synchronized( this )
                {
                    Counter counter = m_counters.get( pagename );

                    // only count in view mode, keep storage values in sync
                    if( increment && WikiContext.VIEW.equalsIgnoreCase( context.getRequestContext() ) )
                    {
                        if( counter == null )
                        {
                            counter = new Counter();
                            m_counters.put( pagename, counter );
                        }
                        counter.increment();
                        m_storage.setProperty( pagename, counter.toString() );
                        m_dirty = true;
                    }

                    if( show == null || STR_NONE.equals( show ) )
                    {
                        // nothing to show

                    }
                    else if( PARAM_COUNT.equals( show ) )
                    {
                        // show page count
                        if( counter == null )
                        {
                            counter = new Counter();
                            m_counters.put( pagename, counter );
                            m_storage.setProperty( pagename, counter.toString() );
                            m_dirty = true;
                        }
                        result = counter.toString();

                    }
                    else if( body != null && 0 < body.length() && STR_LIST.equals( show ) )
                    {
                        // show list of counts
                        String header = STR_EMPTY;
                        String line = body;
                        String footer = STR_EMPTY;
                        int start = body.indexOf( STR_SEPARATOR );

                        // split body into header, line, footer on ----
                        // separator
                        if( 0 < start )
                        {
                            header = body.substring( 0, start );

                            start = skipWhitespace( start + STR_SEPARATOR.length(), body );

                            int end = body.indexOf( STR_SEPARATOR, start );

                            if( start >= end )
                            {
                                line = body.substring( start );

                            }
                            else
                            {
                                line = body.substring( start, end );

                                end = skipWhitespace( end + STR_SEPARATOR.length(), body );

                                footer = body.substring( end );
                            }
                        }

                        // sort on name or count?
                        Map<String, Counter> sorted = m_counters;

                        if( sort != null && PARAM_COUNT.equals( sort ) )
                        {
                            sorted = new TreeMap<String, Counter>( m_compareCountDescending );

                            sorted.putAll( m_counters );
                        }

                        // build a messagebuffer with the list in wiki markup
                        StringBuffer buf = new StringBuffer( header );
                        MessageFormat fmt = new MessageFormat( line );
                        Object[] args = new Object[] { pagename, STR_EMPTY, STR_EMPTY };
                        Iterator< Entry< String, Counter > > iter = sorted.entrySet().iterator();

                        while ( iter != null && 0 < entries && iter.hasNext() )
                        {
                            Entry< String, Counter > entry = iter.next();
                            String name = entry.getKey();

                            // check minimum/maximum count
                            final int value = entry.getValue().getValue();
                            boolean use = min <= value && value <= max;

                            // did we specify a refer-to page?
                            if( use && referrers != null )
                            {
                                use = referrers.contains( name );
                            }

                            // did we specify what pages to include?
                            if( use && include != null )
                            {
                                use = false;

                                for( int n = 0; !use && n < include.length; n++ )
                                {
                                    use = matcher.matches( name, include[n] );
                                }
                            }

                            // did we specify what pages to exclude?
                            if( use && null != exclude )
                            {
                                for( int n = 0; use && n < exclude.length; n++ )
                                {
                                    use &= !matcher.matches( name, exclude[n] );
                                }
                            }

                            if( use )
                            {
                                args[1] = engine.getRenderingManager().beautifyTitle( name );
                                args[2] = entry.getValue();

                                fmt.format( args, buf, null );

                                entries--;
                            }
                        }
                        buf.append( footer );

                        // let the engine render the list
                        result = engine.getRenderingManager().textToHTML( context, buf.toString() );
                    }
                }
            }
            return result;
        }

        /**
         * Compile regexp parameter.
         * 
         * @param name The name of the parameter.
         * @param value The parameter value.
         * @return Pattern[] The compiled patterns, or <code>null</code>.
         * @throws PluginException On malformed patterns.
         */
        private Pattern[] compileGlobs( String name, String value ) throws PluginException
        {
            Pattern[] result = null;

            if( value != null && 0 < value.length() && !STR_GLOBSTAR.equals( value ) )
            {
                try
                {
                    PatternCompiler pc = new GlobCompiler();

                    String[] ptrns = StringUtils.split( value, STR_COMMA );

                    result = new Pattern[ptrns.length];

                    for( int n = 0; n < ptrns.length; n++ )
                    {
                        result[n] = pc.compile( ptrns[n] );
                    }
                }
                catch( MalformedPatternException e )
                {
                    throw new PluginException( "Parameter " + name + " has a malformed pattern: " + e.getMessage() );
                }
            }

            return result;
        }

        /**
         * Adjust offset skipping whitespace.
         * 
         * @param offset The offset in value to adjust.
         * @param value String in which offset points.
         * @return int Adjusted offset into value.
         */
        private int skipWhitespace( int offset, String value )
        {
            while ( Character.isWhitespace( value.charAt( offset ) ) )
            {
                offset++;
            }
            return offset;
        }

        /**
         * Retrieve a page count.
         * 
         * @return int The page count for the given key.
         * @param key the key for the Counter
         */
        protected int getCount( Object key )
        {
            return m_counters.get( key ).getValue();
        }

        /**
         * Load the page view counters from file.
         */
        private void loadCounters() {
            if( m_counters != null && m_storage != null ) {
                log.info( "Loading counters." );
                synchronized( this ) {
                    try( InputStream fis = new FileInputStream( new File( m_workDir, COUNTER_PAGE ) ) ) {
                        m_storage.load( fis );
                    } catch( IOException ioe ) {
                        log.error( "Can't load page counter store: " + ioe.getMessage() + " , will create a new one!" );
                    }

                    // Copy the collection into a sorted map
                    Iterator< Entry< Object, Object > > iter = m_storage.entrySet().iterator();

                    while ( iter != null && iter.hasNext() ) {
                        Entry< ?, ? > entry = iter.next();
                        m_counters.put( (String) entry.getKey(), new Counter( (String) entry.getValue() ) );
                    }
                    
                    log.info( "Loaded " + m_counters.size() + " counter values." );
                }
            }
        }

        /**
         * Save the page view counters to file.
         */
        protected void storeCounters() {
            if( m_counters != null && m_storage != null && m_dirty ) {
                log.info( "Storing " + m_counters.size() + " counter values." );
                synchronized( this ) {
                    // Write out the collection of counters
                    try( final OutputStream fos = new FileOutputStream( new File( m_workDir, COUNTER_PAGE ) ) ) {
                        m_storage.store( fos, "\n# The number of times each page has been viewed.\n# Do not modify.\n" );
                        fos.flush();

                        m_dirty = false;
                    } catch( IOException ioe ) {
                        log.error( "Couldn't store counters values: " + ioe.getMessage() );
                    }
                }
            }
        }

        /**
         * Is the given thread still current?
         * 
         * @return boolean <code>true</code> if the thread is still the current
         *         background thread.
         * @param thrd
         */
        private synchronized boolean isRunning( Thread thrd )
        {
            return m_initialized && thrd == m_pageCountSaveThread;
        }

    }


    /**
     * Counter for page hits collection.
     */
    private static final class Counter
    {

        /** The count value. */
        private int m_count = 0;

        /**
         * Create a new counter.
         */
        public Counter()
        {
        }

        /**
         * Create and initialize a new counter.
         * 
         * @param value Count value.
         */
        public Counter( String value )
        {
            setValue( value );
        }

        /**
         * Increment counter.
         */
        public void increment()
        {
            m_count++;
        }

        /**
         * Get the count value.
         * 
         * @return int
         */
        public int getValue()
        {
            return m_count;
        }

        /**
         * Set the count value.
         * 
         * @param value String representation of the count.
         */
        public void setValue( String value )
        {
            m_count = NumberUtils.toInt( value );
        }

        /**
         * @return String String representation of the count.
         */
        public String toString()
        {
            return String.valueOf( m_count );
        }
    }

    /**
     * Background thread storing the page counters.
     */
    static final class CounterSaveThread extends WikiBackgroundThread
    {

        /** The page view manager. */
        private final PageViewManager m_manager;

        /**
         * Create a wiki background thread to store the page counters.
         * 
         * @param engine The wiki engine.
         * @param interval Delay in seconds between saves.
         * @param pageViewManager
         */
        public CounterSaveThread( WikiEngine engine, int interval, PageViewManager pageViewManager )
        {

            super( engine, interval );

            if( pageViewManager == null )
            {
                throw new IllegalArgumentException( "Manager cannot be null" );
            }

            m_manager = pageViewManager;
        }

        /**
         * Save the page counters to file.
         */
        public void backgroundTask()
        {

            if( m_manager.isRunning( this ) )
            {
                m_manager.storeCounters();
            }
        }
    }
}
