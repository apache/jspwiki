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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.ajax.AjaxUtil;
import org.apache.wiki.ajax.WikiAjaxDispatcherServlet;
import org.apache.wiki.ajax.WikiAjaxServlet;
import org.apache.wiki.api.exceptions.FilterException;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventUtils;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

/**
 *  Manages searching the Wiki.
 *
 *  @since 2.2.21.
 */
public class SearchManager extends BasicPageFilter implements InternalModule, WikiEventListener {

    private static final Logger log = Logger.getLogger(SearchManager.class);

    private static final String DEFAULT_SEARCHPROVIDER  = "org.apache.wiki.search.LuceneSearchProvider";

    /** Old option, now deprecated. */
    private static final String PROP_USE_LUCENE        = "jspwiki.useLucene";

    /**
     *  Property name for setting the search provider. Value is <tt>{@value}</tt>.
     */
    public static final String PROP_SEARCHPROVIDER     = "jspwiki.searchProvider";

    private SearchProvider    m_searchProvider;

    /**
     *  The name of the JSON object that manages search.
     */
    public static final String JSON_SEARCH = "search";

    /**
     *  Creates a new SearchManager.
     *
     *  @param engine The WikiEngine that owns this SearchManager.
     *  @param properties The list of Properties.
     *  @throws FilterException If it cannot be instantiated.
     */
    public SearchManager( WikiEngine engine, Properties properties )
        throws FilterException
    {
        initialize( engine, properties );

        WikiEventUtils.addWikiEventListener(m_engine.getPageManager(),
                                            WikiPageEvent.PAGE_DELETE_REQUEST, this);

        //TODO: Replace with custom annotations. See JSPWIKI-566
        WikiAjaxDispatcherServlet.registerServlet( JSON_SEARCH, new JSONSearch() );
    }

    /**
     *  Provides a JSON RPC API to the JSPWiki Search Engine.
     */
    public class JSONSearch implements WikiAjaxServlet
    {
		public static final String AJAX_ACTION_SUGGESTIONS = "suggestions";
    	public static final String AJAX_ACTION_PAGES = "pages";
    	public static final int DEFAULT_MAX_RESULTS = 20;
    	public int maxResults = DEFAULT_MAX_RESULTS;

		@Override
		public String getServletMapping() {
			return JSON_SEARCH;
		}

    	@Override
    	public void service(HttpServletRequest req, HttpServletResponse resp, String actionName, List<String> params)
    			throws ServletException, IOException {
    		String result = "";
    		if (StringUtils.isNotBlank(actionName)) {
    			if (params.size()<1) {
    				return;
    			}
    			String itemId = params.get(0);
    			log.debug("itemId="+itemId);
    			if (params.size()>1) {
    				String maxResultsParam  = params.get(1);
    				log.debug("maxResultsParam="+maxResultsParam);
    				if (StringUtils.isNotBlank(maxResultsParam) && StringUtils.isNumeric(maxResultsParam)) {
    					maxResults = Integer.parseInt(maxResultsParam);
    				}
    			}

    			if (actionName.equals(AJAX_ACTION_SUGGESTIONS)) {
    				List<String> callResults = new ArrayList<String>();
    				log.debug("Calling getSuggestions() START");
    				callResults = getSuggestions(itemId, maxResults);
    				log.debug("Calling getSuggestions() DONE. "+callResults.size());
    				result = AjaxUtil.toJson(callResults);
    			} else if (actionName.equals(AJAX_ACTION_PAGES)) {
    				List<Map<String,Object>> callResults = new ArrayList<Map<String,Object>>();
    				log.debug("Calling findPages() START");
    				WikiContext wikiContext = m_engine.createContext(req, WikiContext.VIEW);
    				if (wikiContext == null) {
    					throw new ServletException("Could not create a WikiContext from the request "+req);
    				}
    				callResults = findPages(itemId, maxResults, wikiContext);
    				log.debug("Calling findPages() DONE. "+callResults.size());
    				result = AjaxUtil.toJson(callResults);
    			}
    		}
    		log.debug("result="+result);
    		resp.getWriter().write(result);
    	}

        /**
         *  Provides a list of suggestions to use for a page name.
         *  Currently the algorithm just looks into the value parameter,
         *  and returns all page names from that.
         *
         *  @param wikiName the page name
         *  @param maxLength maximum number of suggestions
         *  @return the suggestions
         */
        public List<String> getSuggestions( String wikiName, int maxLength )
        {
            StopWatch sw = new StopWatch();
            sw.start();
            List<String> list = new ArrayList<String>(maxLength);

            if( wikiName.length() > 0 )
            {

                // split pagename and attachment filename
                String filename = "";
                int pos = wikiName.indexOf("/");
                if( pos >= 0 )
                {
                    filename = wikiName.substring( pos ).toLowerCase();
                    wikiName = wikiName.substring( 0, pos );
                }

                String cleanWikiName = MarkupParser.cleanLink(wikiName).toLowerCase() + filename;

                String oldStyleName = MarkupParser.wikifyLink(wikiName).toLowerCase() + filename;

                Set< String > allPages = m_engine.getReferenceManager().findCreated();

                int counter = 0;
                for( Iterator< String > i = allPages.iterator(); i.hasNext() && counter < maxLength; )
                {
                    String p = i.next();
                    String pp = p.toLowerCase();
                    if( pp.startsWith( cleanWikiName) || pp.startsWith( oldStyleName ) )
                    {
                        list.add( p );
                        counter++;
                    }
                }
            }

            sw.stop();
            if( log.isDebugEnabled() ) log.debug("Suggestion request for "+wikiName+" done in "+sw);
            return list;
        }

        /**
         *  Performs a full search of pages.
         *
         *  @param searchString The query string
         *  @param maxLength How many hits to return
         *  @return the pages found
         */
        public List<Map<String,Object>> findPages( String searchString, int maxLength, WikiContext wikiContext )
        {
            StopWatch sw = new StopWatch();
            sw.start();

            List<Map<String,Object>> list = new ArrayList<Map<String,Object>>(maxLength);

            if( searchString.length() > 0 )
            {
                try
                {
                    Collection< SearchResult > c;

                    if( m_searchProvider instanceof LuceneSearchProvider ) {
                        c = ((LuceneSearchProvider)m_searchProvider).findPages( searchString, 0, wikiContext );
                    } else {
                        c = m_searchProvider.findPages( searchString, wikiContext );
                    }

                    int count = 0;
                    for( Iterator< SearchResult > i = c.iterator(); i.hasNext() && count < maxLength; count++ )
                    {
                        SearchResult sr = i.next();
                        HashMap<String,Object> hm = new HashMap<>();
                        hm.put( "page", sr.getPage().getName() );
                        hm.put( "score", sr.getScore() );
                        list.add( hm );
                    }
                }
                catch(Exception e)
                {
                    log.info("AJAX search failed; ",e);
                }
            }

            sw.stop();
            if( log.isDebugEnabled() ) log.debug("AJAX search complete in "+sw);
            return list;
        }
    }


    /**
     *  This particular method starts off indexing and all sorts of various activities,
     *  so you need to run this last, after things are done.
     *
     * @param engine the wiki engine
     * @param properties the properties used to initialize the wiki engine
     * @throws FilterException if the search provider failed to initialize
     */
    public void initialize(WikiEngine engine, Properties properties)
        throws FilterException
    {
        m_engine = engine;

        loadSearchProvider(properties);

        try
        {
            m_searchProvider.initialize(engine, properties);
        }
        catch (NoRequiredPropertyException e)
        {
            log.error( e.getMessage(), e );
        }
        catch (IOException e)
        {
            log.error( e.getMessage(), e );
        }
    }

    private void loadSearchProvider(Properties properties)
    {
        //
        // See if we're using Lucene, and if so, ensure that its
        // index directory is up to date.
        //
        String useLucene = properties.getProperty(PROP_USE_LUCENE);

        // FIXME: Obsolete, remove, or change logic to first load searchProvder?
        // If the old jspwiki.useLucene property is set we use that instead of the searchProvider class.
        if( useLucene != null )
        {
            log.info( PROP_USE_LUCENE+" is deprecated; please use "+PROP_SEARCHPROVIDER+"=<your search provider> instead." );
            if( TextUtil.isPositive( useLucene ) )
            {
                m_searchProvider = new LuceneSearchProvider();
            }
            else
            {
                m_searchProvider = new BasicSearchProvider();
            }
            log.debug("useLucene was set, loading search provider " + m_searchProvider);
            return;
        }

        String providerClassName = properties.getProperty( PROP_SEARCHPROVIDER,
                                                           DEFAULT_SEARCHPROVIDER );

        try
        {
            Class<?> providerClass = ClassUtil.findClass( "org.apache.wiki.search", providerClassName );
            m_searchProvider = (SearchProvider)providerClass.newInstance();
        }
        catch( ClassNotFoundException e )
        {
            log.warn("Failed loading SearchProvider, will use BasicSearchProvider.", e);
        }
        catch( InstantiationException e )
        {
            log.warn("Failed loading SearchProvider, will use BasicSearchProvider.", e);
        }
        catch( IllegalAccessException e )
        {
            log.warn("Failed loading SearchProvider, will use BasicSearchProvider.", e);
        }

        if( null == m_searchProvider )
        {
            // FIXME: Make a static with the default search provider
            m_searchProvider = new BasicSearchProvider();
        }
        log.debug("Loaded search provider " + m_searchProvider);
    }

    /**
     *  Returns the SearchProvider used.
     *
     *  @return The current SearchProvider.
     */
    public SearchProvider getSearchEngine()
    {
        return m_searchProvider;
    }

    /**
     *  Sends a search to the current search provider. The query is is whatever native format
     *  the query engine wants to use.
     *
     * @param query The query.  Null is safe, and is interpreted as an empty query.
     * @param wikiContext the context within which to run the search
     * @return A collection of WikiPages that matched.
     * @throws ProviderException If the provider fails and a search cannot be completed.
     * @throws IOException If something else goes wrong.
     */
    public Collection< SearchResult > findPages( String query, WikiContext wikiContext )
        throws ProviderException, IOException
    {
        if( query == null ) query = "";
        return m_searchProvider.findPages( query, wikiContext );
    }

    /**
     *  Removes the page from the search cache (if any).
     *  @param page  The page to remove
     */
    public void pageRemoved(WikiPage page)
    {
        m_searchProvider.pageRemoved(page);
    }

    /**
     *  Reindexes the page.
     *
     *  @param wikiContext {@inheritDoc}
     *  @param content {@inheritDoc}
     */
    @Override
    public void postSave( WikiContext wikiContext, String content )
    {
        //
        //  Makes sure that we're indexing the latest version of this
        //  page.
        //
        WikiPage p = m_engine.getPage( wikiContext.getPage().getName() );
        reindexPage( p );
    }

    /**
     *   Forces the reindex of the given page.
     *
     *   @param page The page.
     */
    public void reindexPage(WikiPage page)
    {
        m_searchProvider.reindexPage(page);
    }

    /**
     *  If the page has been deleted, removes it from the index.
     *
     *  @param event {@inheritDoc}
     */
    public void actionPerformed(WikiEvent event)
    {
        if( (event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.PAGE_DELETE_REQUEST) )
        {
            String pageName = ((WikiPageEvent) event).getPageName();

            WikiPage p = m_engine.getPage( pageName );
            if( p != null )
            {
                pageRemoved( p );
            }
        }
    }

}
