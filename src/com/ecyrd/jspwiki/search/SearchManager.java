/*
JSPWiki - a JSP-based WikiWiki clone.

Copyright (C) 2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.search;

import java.io.IOException;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.modules.InternalModule;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Manages searching the Wiki.
 *
 *  @author Arent-Jan Banck for Informatica
 *  @since 2.2.21.
 */

public class SearchManager
    extends BasicPageFilter
    implements InternalModule
{
    private static final Logger log = Logger.getLogger(SearchManager.class);

    private static      String DEFAULT_SEARCHPROVIDER  = "com.ecyrd.jspwiki.search.LuceneSearchProvider";
    public static final String PROP_USE_LUCENE         = "jspwiki.useLucene";
    public static final String PROP_SEARCHPROVIDER     = "jspwiki.searchProvider";

    private SearchProvider    m_searchProvider = null;
    
    protected WikiEngine m_engine;
    
    public SearchManager( WikiEngine engine, Properties properties )
        throws WikiException
    {
        initialize( engine, properties );
    }

    /**
     *  This particular method starts off indexing and all sorts of various activities,
     *  so you need to run this last, after things are done.
     *   
     * @param engine
     * @param properties
     * @throws WikiException
     */
    public void initialize(WikiEngine engine, Properties properties)
        throws WikiException
    {
        m_engine = engine;
        
        loadSearchProvider(properties);
       
        try 
        {
            m_searchProvider.initialize(engine, properties);
        } 
        catch (NoRequiredPropertyException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } 
        catch (IOException e) 
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            Class providerClass = ClassUtil.findClass( "com.ecyrd.jspwiki.search", providerClassName );
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

    public SearchProvider getSearchEngine()
    {
        return m_searchProvider;
    }

    /**
     *  Sends a search to the current search provider. The query is is whatever native format
     *  the query engine wants to use.
     *  
     * @param query The query.  Null is safe, and is interpreted as an empty query.
     * @return A collection of WikiPages that matched.
     */
    public Collection findPages( String query )
        throws ProviderException, IOException
    {
        if( query == null ) query = "";
        Collection c = m_searchProvider.findPages( query );

        return c;
    }

    /**
     *  Removes the page from the search cache (if any).
     *  @param page  The page to remove
     */
    public void pageRemoved(WikiPage page)
    {
        m_searchProvider.pageRemoved(page);
    }
    
    public void postSave( WikiContext wikiContext, String content )
    {
        reindexPage( wikiContext.getPage() );
    }

    /**
     *   Forces the reindex of the given page.
     *   
     *   @param page
     */
    public void reindexPage(WikiPage page)
    {
        m_searchProvider.reindexPage(page);
    }
}
