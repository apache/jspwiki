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
package org.apache.wiki.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.ajax.AjaxUtil;
import org.apache.wiki.ajax.WikiAjaxDispatcherServlet;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.filters.BasePageFilter;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 *  Manages searching the Wiki.
 *
 *  @since 2.2.21.
 */
public class DefaultSearchManager extends BasePageFilter implements SearchManager {

    private static final Logger LOG = LogManager.getLogger( DefaultSearchManager.class );

    private SearchProvider m_searchProvider;

    /**
     *  Creates a new SearchManager.
     *
     *  @param engine The Engine that owns this SearchManager.
     *  @param properties The list of Properties.
     *  @throws FilterException If it cannot be instantiated.
     */
    public DefaultSearchManager( final Engine engine, final Properties properties ) throws FilterException {
        initialize( engine, properties );
        WikiEventManager.addWikiEventListener( m_engine.getManager( PageManager.class ), this );

        // TODO: Replace with custom annotations. See JSPWIKI-566
        WikiAjaxDispatcherServlet.registerServlet( JSON_SEARCH, new JSONSearch() );
    }

    /**
     *  Provides a JSON AJAX API to the JSPWiki Search Engine.
     */
    public class JSONSearch implements WikiAjaxServlet {

        public static final String AJAX_ACTION_SUGGESTIONS = "suggestions";
        public static final String AJAX_ACTION_PAGES = "pages";
        public static final int DEFAULT_MAX_RESULTS = 20;
        public int maxResults = DEFAULT_MAX_RESULTS;

        /** {@inheritDoc} */
        @Override
        public String getServletMapping() {
            return JSON_SEARCH;
        }

        /** {@inheritDoc} */
        @Override
        public void service( final HttpServletRequest req,
                             final HttpServletResponse resp,
                             final String actionName,
                             final List< String > params ) throws IOException {
            String result = "";
            if( StringUtils.isNotBlank( actionName ) ) {
                if( params.size() < 1 ) {
                    return;
                }
                final String itemId = params.get( 0 );
                LOG.debug( "itemId=" + itemId );
                if( params.size() > 1 ) {
                    final String maxResultsParam = params.get( 1 );
                    LOG.debug( "maxResultsParam=" + maxResultsParam );
                    if( StringUtils.isNotBlank( maxResultsParam ) && StringUtils.isNumeric( maxResultsParam ) ) {
                        maxResults = Integer.parseInt( maxResultsParam );
                    }
                }

                if( actionName.equals( AJAX_ACTION_SUGGESTIONS ) ) {
                    LOG.debug( "Calling getSuggestions() START" );
                    final List< String > callResults = getSuggestions( itemId, maxResults );
                    LOG.debug( "Calling getSuggestions() DONE. " + callResults.size() );
                    result = AjaxUtil.toJson( callResults );
                } else if( actionName.equals( AJAX_ACTION_PAGES ) ) {
                    LOG.debug("Calling findPages() START");
                    final Context wikiContext = Wiki.context().create( m_engine, req, ContextEnum.PAGE_VIEW.getRequestContext() );
                    final List< Map< String, Object > > callResults = findPages( itemId, maxResults, wikiContext );
                    LOG.debug( "Calling findPages() DONE. " + callResults.size() );
                    result = AjaxUtil.toJson( callResults );
                }
            }
            LOG.debug( "result=" + result );
            resp.getWriter().write( result );
        }

        /**
         *  Provides a list of suggestions to use for a page name. Currently, the algorithm just looks into the value parameter,
         *  and returns all page names from that.
         *
         *  @param wikiName the page name
         *  @param maxLength maximum number of suggestions
         *  @return the suggestions
         */
        public List< String > getSuggestions( String wikiName, final int maxLength ) {
            final StopWatch sw = new StopWatch();
            sw.start();
            final List< String > list = new ArrayList<>( maxLength );
            if( !wikiName.isEmpty() ) {
                // split pagename and attachment filename
                String filename = "";
                final int pos = wikiName.indexOf("/");
                if( pos >= 0 ) {
                    filename = wikiName.substring( pos ).toLowerCase();
                    wikiName = wikiName.substring( 0, pos );
                }

                final String cleanWikiName = MarkupParser.cleanLink(wikiName).toLowerCase() + filename;
                final String oldStyleName = MarkupParser.wikifyLink(wikiName).toLowerCase() + filename;
                final Set< String > allPages = m_engine.getManager( ReferenceManager.class ).findCreated();

                int counter = 0;
                for( final Iterator< String > i = allPages.iterator(); i.hasNext() && counter < maxLength; ) {
                    final String p = i.next();
                    final String pp = p.toLowerCase();
                    if( pp.startsWith( cleanWikiName) || pp.startsWith( oldStyleName ) ) {
                        list.add( p );
                        counter++;
                    }
                }
            }

            sw.stop();
            LOG.debug( "Suggestion request for {} done in {}", wikiName, sw );
            return list;
        }

        /**
         *  Performs a full search of pages.
         *
         *  @param searchString The query string
         *  @param maxLength How many hits to return
         *  @return the pages found
         */
        public List< Map< String, Object > > findPages( final String searchString, final int maxLength, final Context wikiContext ) {
            final StopWatch sw = new StopWatch();
            sw.start();

            final List< Map< String, Object > > list = new ArrayList<>( maxLength );
            if( !searchString.isEmpty() ) {
                try {
                    final Collection< SearchResult > c;
                    if( m_searchProvider instanceof LuceneSearchProvider ) {
                        c = ( ( LuceneSearchProvider )m_searchProvider ).findPages( searchString, 0, wikiContext );
                    } else {
                        c = m_searchProvider.findPages( searchString, wikiContext );
                    }

                    int count = 0;
                    for( final Iterator< SearchResult > i = c.iterator(); i.hasNext() && count < maxLength; count++ ) {
                        final SearchResult sr = i.next();
                        final HashMap< String, Object > hm = new HashMap<>();
                        hm.put( "page", sr.getPage().getName() );
                        hm.put( "score", sr.getScore() );
                        list.add( hm );
                    }
                } catch( final Exception e ) {
                    LOG.info( "AJAX search failed; ", e );
                }
            }

            sw.stop();
            LOG.debug( "AJAX search complete in {}", sw );
            return list;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws FilterException {
        m_engine = engine;
        loadSearchProvider(properties);

        try {
            m_searchProvider.initialize( engine, properties );
        } catch( final NoRequiredPropertyException | IOException e ) {
            LOG.error( e.getMessage(), e );
        }
    }

    private void loadSearchProvider( final Properties properties ) {
        // See if we're using Lucene, and if so, ensure that its index directory is up-to-date.
        final String providerClassName = TextUtil.getStringProperty( properties, PROP_SEARCHPROVIDER, DEFAULT_SEARCHPROVIDER );

        try {
            m_searchProvider = ClassUtil.buildInstance( "org.apache.wiki.search", providerClassName );
        } catch( final ReflectiveOperationException e ) {
            LOG.warn( "Failed loading SearchProvider, will use BasicSearchProvider.", e );
        }

        if( null == m_searchProvider ) {
            m_searchProvider = new BasicSearchProvider();
        }
        LOG.debug( "Loaded search provider {}", m_searchProvider );
    }

    /** {@inheritDoc} */
    @Override
    public SearchProvider getSearchEngine()
    {
        return m_searchProvider;
    }

    /** {@inheritDoc} */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( event instanceof WikiPageEvent ) {
            final String pageName = ( ( WikiPageEvent ) event ).getPageName();
            if( event.getType() == WikiPageEvent.PAGE_DELETE_REQUEST ) {
                final Page p = m_engine.getManager( PageManager.class ).getPage( pageName );
                if( p != null ) {
                    pageRemoved( p );
                }
            }
            if( event.getType() == WikiPageEvent.PAGE_REINDEX ) {
                final Page p = m_engine.getManager( PageManager.class ).getPage( pageName );
                if( p != null ) {
                    reindexPage( p );
                }
            }
        }
    }

}
