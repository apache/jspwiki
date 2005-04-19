/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.providers;

import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.util.*;
import org.apache.log4j.Logger;

import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.*;

import com.opensymphony.module.oscache.base.Cache;
import com.opensymphony.module.oscache.base.NeedsRefreshException;
import com.opensymphony.module.oscache.base.events.CacheEntryEvent;
import com.opensymphony.module.oscache.base.events.CacheEntryEventListener;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Provides a caching page provider.  This class rests on top of a
 *  real provider class and provides a cache to speed things up.  Only
 *  if the cache copy of the page text has expired, we fetch it from
 *  the provider.
 *  <p>
 *  This class also detects if someone has modified the page
 *  externally, not through JSPWiki routines, and throws the proper
 *  RepositoryModifiedException.
 *  <p>
 *  Heavily based on ideas by Chris Brooking.
 *  <p>
 *  Since 2.1.52 uses the OSCache library from OpenSymphony.
 *  <p>
 *  Since 2.1.100 uses the Apache Lucene library to help in searching.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.4
 *  @see RepositoryModifiedException
 */
// FIXME: Keeps a list of all WikiPages in memory - should cache them too.
// FIXME: Synchronization is a bit inconsistent in places.
// FIXME: A part of the stuff is now redundant, since we could easily use the text cache
//        for a lot of things.  RefactorMe.

public class CachingProvider
    implements WikiPageProvider
{
    private static final Logger log = Logger.getLogger(CachingProvider.class);

    private WikiPageProvider m_provider;

    private Cache            m_cache;
    private Cache            m_negCache; // Cache for holding non-existing pages
    
    private Cache            m_textCache;
    private Cache            m_historyCache;

    private long             m_cacheMisses = 0;
    private long             m_cacheHits   = 0;

    private long             m_historyCacheMisses = 0;
    private long             m_historyCacheHits   = 0;

    private int              m_expiryPeriod = 30;
    
    /**
     *  This can be very long, as normally all modifications are noticed in an earlier
     *  stage.
     */
    private int              m_pageContentExpiryPeriod = 24*60*60;
    
    // FIXME: This MUST be cached somehow.

    private boolean          m_gotall = false;

    // Lucene data, if used.
    private boolean          m_useLucene = false;
    private String           m_luceneDirectory = null;
    private int              m_updateCount = 0;
    private Thread           m_luceneUpdateThread = null;
    private Vector           m_updates = new Vector(); // Vector because multi-threaded.

    private CacheItemCollector m_allCollector = new CacheItemCollector();
    
    /**
     *  Defines, in seconds, the amount of time a text will live in the cache
     *  at most before requiring a refresh.
     */
    
    public static final String PROP_CACHECHECKINTERVAL = "jspwiki.cachingProvider.cacheCheckInterval";
    public static final String PROP_CACHECAPACITY      = "jspwiki.cachingProvider.capacity";

    private static final int   DEFAULT_CACHECAPACITY   = 1000; // Good most wikis

    private static final String OSCACHE_ALGORITHM      = "com.opensymphony.module.oscache.base.algorithm.LRUCache";

    // Lucene properties.
    public static final String PROP_USE_LUCENE         = "jspwiki.useLucene";

    private static final String LUCENE_DIR             = "lucene";

    // Number of page updates before we optimize the index.
    public static final int LUCENE_OPTIMIZE_COUNT      = 10;
    private static final String LUCENE_ID              = "id";
    private static final String LUCENE_PAGE_CONTENTS   = "contents";

    public void initialize( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException,
               IOException
    {
        log.debug("Initing CachingProvider");

        //
        //  Cache consistency checks
        //
        m_expiryPeriod = TextUtil.getIntegerProperty( properties,
                                                      PROP_CACHECHECKINTERVAL,
                                                      m_expiryPeriod );

        log.debug("Cache expiry period is "+m_expiryPeriod+" s");

        //
        //  Text cache capacity
        //
        int capacity = TextUtil.getIntegerProperty( properties,
                                                    PROP_CACHECAPACITY,
                                                    DEFAULT_CACHECAPACITY );

        log.debug("Cache capacity "+capacity+" pages.");

        m_cache = new Cache( true, false );
        m_cache.addCacheEntryEventListener( m_allCollector );
        
        m_negCache = new Cache( true, false );
        
        m_textCache = new Cache( true, false,
                                 OSCACHE_ALGORITHM,
                                 capacity );

        m_historyCache = new Cache( true, false,
                                    OSCACHE_ALGORITHM,
                                    capacity );
                                    
        //
        //  Find and initialize real provider.
        //
        String classname = WikiEngine.getRequiredProperty( properties, 
                                                           PageManager.PROP_PAGEPROVIDER );
        
        try
        {            
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                                                       classname );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing real provider class "+m_provider);
            m_provider.initialize( engine, properties );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new IllegalArgumentException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new IllegalArgumentException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
            throw new IllegalArgumentException("illegal provider class");
        }

        //
        // See if we're using Lucene, and if so, ensure that its 
        // index directory is up to date.
        // 
        m_useLucene = TextUtil.getBooleanProperty(properties, PROP_USE_LUCENE, true );

        if( m_useLucene )
        {
            m_luceneDirectory = engine.getWorkDir()+File.separator+LUCENE_DIR;
          
            // FIXME: Just to be simple for now, we will do full reindex 
            // only if no files are in lucene directory.

            File dir = new File(m_luceneDirectory);

            log.info("Lucene enabled, cache will be in: "+dir.getAbsolutePath());

            try
            {
                if( !dir.exists() )
                {
                    dir.mkdirs();
                }

                if( !dir.exists() || !dir.canWrite() || !dir.canRead() )
                {
                    log.error("Cannot write to Lucene directory, disabling Lucene: "+dir.getAbsolutePath());
                    throw new IOException( "Invalid Lucene directory." );
                }
                
                String[] filelist = dir.list();
                
                if( filelist == null )
                {
                    throw new IOException( "Invalid Lucene directory: cannot produce listing: "+dir.getAbsolutePath());
                }
                
                if( filelist.length == 0 )
                {  
                    //
                    //  No files? Reindex!
                    //
                    Date start = new Date();
                    IndexWriter writer = null;

                    log.info("Starting Lucene reindexing, this can take a couple minutes...");

                    try
                    {
                        // FIXME: Should smartly use a different analyzer
                        //        in case the language is something else
                        //        than English.

                        writer = new IndexWriter( m_luceneDirectory, 
                                                  new StandardAnalyzer(), 
                                                  true );
                        Collection allPages = getAllPages();

                        for( Iterator iterator = allPages.iterator(); iterator.hasNext(); )
                        {
                            WikiPage page = (WikiPage) iterator.next();
                            String text = getPageText( page.getName(), 
                                                       WikiProvider.LATEST_VERSION );
                            luceneIndexPage( page, text, writer );
                        }
                        writer.optimize();
                    }
                    finally
                    {
                        try
                        {
                            if( writer != null ) writer.close();
                        }
                        catch( IOException e ) {}
                    }

                    Date end = new Date();
                    log.info("Full Lucene index finished in " + 
                             (end.getTime() - start.getTime()) + " milliseconds.");
                }
                else
                {
                    log.info("Files found in Lucene directory, not reindexing.");
                }
            }
            catch( NoClassDefFoundError e )
            {
                log.info("Lucene libraries do not exist - not using Lucene.");
                m_useLucene = false;
            }
            catch ( IOException e )
            {
                log.error("Problem while creating Lucene index - not using Lucene.", e);
                m_useLucene = false;
            }
            catch ( ProviderException e )
            {
                log.error("Problem reading pages while creating Lucene index (JSPWiki won't start.)", e);
                throw new IllegalArgumentException("unable to create Lucene index");
            }

            startLuceneUpdateThread();
        }
    }

    /*
    public void finalize()
    {
        if( m_luceneUpdateThread != null )
        {
            m_luceneUpdateThread.
        }
    }
    */

    /**
     *  Waits first for a little while before starting to go through
     *  the Lucene "pages that need updating".
     */
    private void startLuceneUpdateThread()
    {        
        m_luceneUpdateThread = new Thread(new Runnable()
        {
            public void run()
            {
                // FIXME: This is a kludge - JSPWiki should somehow report
                //        that init phase is complete.
                try
                {
                    Thread.sleep( 60000L );
                }
                catch( InterruptedException e ) {}

                while( true )
                {
                    while( m_updates.size() > 0 )
                    {
                        Object[] pair = ( Object[] ) m_updates.remove(0);
                        WikiPage page = ( WikiPage ) pair[0];
                        String text = ( String ) pair[1];
                        updateLuceneIndex(page, text);
                    }
                    try
                    {
                        Thread.sleep(500);
                    }
                    catch ( InterruptedException e ) {}
                }
            }
        });
        m_luceneUpdateThread.start();
    }


    private void luceneIndexPage( WikiPage page, String text, IndexWriter writer ) 
        throws IOException
    {
        // make a new, empty document
        Document doc = new Document();

        // Raw name is the keyword we'll use to refer to this document for updates.
        doc.add(Field.Keyword(LUCENE_ID, page.getName()));

        // Body text is indexed, but not stored in doc. We add in the 
        // title text as well to make sure it gets considered.
        doc.add(Field.Text(LUCENE_PAGE_CONTENTS, new StringReader(text + " " +
                TextUtil.beautifyString(page.getName()))));
        writer.addDocument(doc);
    }

    /**
     * Attempts to fetch the given page information from the cache.  If the
     * page is not in there, checks the real provider.
     * 
     * @param name The page name to look for
     * @return The WikiPage, or null, if the page does not exist
     * @throws ProviderException If something failed
     * @throws RepositoryModifiedException  If the page exists, but has been changed in
     *         the repository
     */
    private WikiPage getPageInfoFromCache( String name )
        throws ProviderException,
               RepositoryModifiedException
    {
        try
        {
            WikiPage item = (WikiPage)m_cache.getFromCache( name, m_expiryPeriod );
            
            if( item != null )
                return item;
            
            return null;
        }
        catch( NeedsRefreshException e )
        {
            WikiPage cached = (WikiPage)e.getCacheContent();
            
            // int version = (cached != null) ? cached.getVersion() : WikiPageProvider.LATEST_VERSION;
            
            WikiPage refreshed = m_provider.getPageInfo( name, WikiPageProvider.LATEST_VERSION );
  
            if( refreshed == null && cached != null )
            {
                //  Page has been removed evilly by a goon from outer space

                log.debug("Page "+name+" has been removed externally.");
                
                m_cache.putInCache( name, null );
                m_textCache.putInCache( name, null );
                m_historyCache.putInCache( name, null );
                // We cache a page miss
                m_negCache.putInCache( name, name );
                
                if (m_useLucene)
                {
                    deleteFromLucene(new WikiPage(name));
                }
                throw new RepositoryModifiedException( "Removed: "+name, name );
            }
            else if( cached == null )
            {
                // The page did not exist in the first place
                
                if( refreshed != null )
                {
                    // We must now add it
                    m_cache.putInCache( name, refreshed );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( name, null );
                    
                    throw new RepositoryModifiedException( "Added: "+name, name );
                    // return refreshed;
                }
                else
                {
                    // Cache page miss
                    m_negCache.putInCache( name, name );
                }
            }
            else if( cached.getVersion() != refreshed.getVersion() )
            {
                //  The newest version has been deleted, but older versions still remain
                log.debug("Page "+cached.getName()+" newest version deleted, reloading...");
                
                m_cache.putInCache( name, refreshed );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );

                m_textCache.flushEntry( name );
                m_historyCache.flushEntry( name );
                
                return refreshed;
            }
            else if( Math.abs(refreshed.getLastModified().getTime()-cached.getLastModified().getTime()) > 1000L )
            {
                //  Yes, the page has been modified externally and nobody told us
         
                log.info("Page "+cached.getName()+" changed, reloading...");

                m_cache.putInCache( name, refreshed );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );
                m_textCache.flushEntry( name );
                m_historyCache.flushEntry( name );

                throw new RepositoryModifiedException( "Modified: "+name, name );
            }
            else
            {
                // Refresh the cache by putting the same object back
                m_cache.putInCache( name, cached );
                // Requests for this page are now no longer denied
                m_negCache.putInCache( name, null );
            }
            return cached;
        }
    }

    public boolean pageExists( String pageName )
    {
        //
        //  First, check the negative cache if we've seen it before
        //
        try
        {        
            String isNonExistant = (String) m_negCache.getFromCache( pageName, m_expiryPeriod );
            
            if( isNonExistant != null ) return false; // No such page
        }
        catch( NeedsRefreshException e )
        {
            // OSCache 2.1 locks the Entry which leads to a deadlock. We must unlock the entry
            // if there is no entry yet in there. If you want to use OSCache 2.1, uncomment the
            // following line.
            // m_negCache.cancelUpdate(pageName);

            // Let's just check if the page exists in the normal way
        }

        WikiPage p = null;
        
        try
        {
            p = getPageInfoFromCache( pageName );
        }
        catch( RepositoryModifiedException e ) 
        {
            // The repository was modified, we need to check now if the page was removed or
            // added.
            // TODO: This information would be available in the exception, but we would
            //       need to subclass.
            
            try
            {
                p = getPageInfoFromCache( pageName );
            }
            catch( Exception ex ) { return false; } // This should not happen
        }
        catch( ProviderException e ) 
        {
            log.info("Provider failed while trying to check if page exists: "+pageName);
            return false;
        }
        
        //
        //  A null item means that the page either does not
        //  exist, or has not yet been cached; a non-null
        //  means that the page does exist.
        //
        if( p != null )
        {
            return true;
        }

        //
        //  If we have a list of all pages in memory, then any page
        //  not in the cache must be non-existent.
        //
        //  FIXME: There's a problem here; if someone modifies the
        //         repository by adding a page outside JSPWiki, 
        //         we won't notice it.

        if( m_gotall )
        {
            return false;
        }

        //
        //  We could add the page to the cache here as well,
        //  but in order to understand whether that is a
        //  good thing or not we would need to analyze
        //  the JSPWiki calling patterns extensively.  Presumably
        //  it would be a good thing if pageExists() is called
        //  many times before the first getPageText() is called,
        //  and the whole page is cached.
        //
        return m_provider.pageExists( pageName );
    }

    /**
     *  @throws RepositoryModifiedException If the page has been externally modified.
     */
    public String getPageText( String pageName, int version )
        throws ProviderException,
               RepositoryModifiedException
    {
        String result = null;

        if( version == WikiPageProvider.LATEST_VERSION )
        {
            result = getTextFromCache( pageName );
        }
        else
        {
            WikiPage p = getPageInfoFromCache( pageName );

            //
            //  Or is this the latest version fetched by version number?
            //
            if( p != null && p.getVersion() == version )
            {
                result = getTextFromCache( pageName );
            }
            else
            {
                result = m_provider.getPageText( pageName, version );
            }
        }

        return result;
    }

    /**
     *  Adds a page-text pair to the lucene update queue.  Safe to call
     *  always - if lucene is not used, does nothing.
     */
    private void addToLuceneQueue( WikiPage page, String text )
    {
        if( m_useLucene && page != null )
        {
            // Add work item to m_updates queue.
            Object[] pair = new Object[2];
            pair[0] = page;
            pair[1] = text;
            m_updates.add(pair);
            log.debug("Scheduling page " + page.getName() + " for index update");
        }
    }

    /**
     *  @throws RepositoryModifiedException If the page has been externally modified.
     */
    private String getTextFromCache( String pageName )
        throws ProviderException,
               RepositoryModifiedException
    {
        String text;

        WikiPage page = getPageInfoFromCache( pageName );

        try
        {
            text = (String)m_textCache.getFromCache( pageName,
                                                     m_pageContentExpiryPeriod );
            
            if( text == null )
            {
                if( page != null )
                {
                    text = m_provider.getPageText( pageName, WikiPageProvider.LATEST_VERSION );
                
                    m_textCache.putInCache( pageName, text );

                    addToLuceneQueue( page, text );

                    m_cacheMisses++;
                }
                else
                {
                    return null;
                }
            }
            else
            {
                m_cacheHits++;
            }
        }
        catch( NeedsRefreshException e )
        {            
            if( pageExists(pageName) )
            {
                text = m_provider.getPageText( pageName, WikiPageProvider.LATEST_VERSION );
                    
                m_textCache.putInCache( pageName, text );

                addToLuceneQueue( page, text );

                m_cacheMisses++;
            }
            else
            {
                m_textCache.putInCache( pageName, null );
                return null; // No page exists
            }
        }
        
        return text;
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
        synchronized(this)
        {
            addToLuceneQueue( page, text );

            m_provider.putPageText( page, text );

            page.setLastModified( new Date() );
            
            // Refresh caches properly
            
            m_cache.flushEntry( page.getName() );
            m_textCache.flushEntry( page.getName() );
            m_historyCache.flushEntry( page.getName() );
            m_negCache.flushEntry( page.getName() );
            
            // Refresh caches
            try
            {
                getPageInfoFromCache( page.getName() );
            }
            catch(RepositoryModifiedException e) {} // Expected
        }
    }

    private synchronized void updateLuceneIndex( WikiPage page, String text )
    {
        IndexWriter writer = null;

        log.debug("Updating Lucene index for page '" + page.getName() + "'...");        

        try
        {
            deleteFromLucene(page);

            // Now add back the new version.
            writer = new IndexWriter(m_luceneDirectory, new StandardAnalyzer(), false);
            luceneIndexPage(page, text, writer);
            m_updateCount++;
            if( m_updateCount >= LUCENE_OPTIMIZE_COUNT )
            {
                writer.optimize();
                m_updateCount = 0;
            }
        }
        catch ( IOException e )
        {
            log.error("Unable to update page '" + page.getName() + "' from Lucene index", e);
        }
        finally
        {
            try
            {
                if( writer != null ) writer.close();
            }
            catch( IOException e ) {}
        }

        log.debug("Done updating Lucene index for page '" + page.getName() + "'.");
    }

    private void deleteFromLucene( WikiPage page )
    {
        try
        {
            // Must first remove existing version of page.
            IndexReader reader = IndexReader.open(m_luceneDirectory);
            reader.delete(new Term(LUCENE_ID, page.getName()));
            reader.close();
        }
        catch ( IOException e )
        {
            log.error("Unable to update page '" + page.getName() + "' from Lucene index", e);
        }
    }


    public Collection getAllPages()
        throws ProviderException
    {
        Collection all;

        if( m_gotall == false )
        {
            all = m_provider.getAllPages();

            // Make sure that all pages are in the cache.

            synchronized(this)
            {
                for( Iterator i = all.iterator(); i.hasNext(); )
                {
                    WikiPage p = (WikiPage) i.next();
                    
                    m_cache.putInCache( p.getName(), p );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( p.getName(), null );
                }

                m_gotall = true;
            }
        }
        else
        {
            all = m_allCollector.getAllItems();
        }

        return all;
    }

    public Collection getAllChangedSince( Date date )
    {
        return m_provider.getAllChangedSince( date );
    }

    public int getPageCount()
        throws ProviderException
    {
        return m_provider.getPageCount();
    }

    public Collection findPages( QueryItem[] query )
    {
        //
        //  If the provider is a fast searcher, then
        //  just pass this request through.
        //
        if( m_provider instanceof FastSearch )
        {
            return m_provider.findPages( query );
        }
        
        TreeSet res = new TreeSet( new SearchResultComparator() );
        SearchMatcher matcher = new SearchMatcher( query );

        Collection allPages = null;
        try
        {
            if( m_useLucene )
            {
// To keep the scoring mechanism the same, we'll only use Lucene to determine which pages to score.
                allPages = searchLucene(query);
            }
            else
            {
                allPages = getAllPages();
            }
        }
        catch( ProviderException pe )
        {
            log.error( "Unable to retrieve page list", pe );
            return( null );
        }

        Iterator it = allPages.iterator();
        while( it.hasNext() )
        {
            try
            {
                WikiPage page = (WikiPage) it.next();
                if (page != null) 
                {
                    String pageName = page.getName();
                    String pageContent = getTextFromCache( pageName );
                    SearchResult comparison = matcher.matchPageContent( pageName, pageContent );
                    
                    if( comparison != null )
                    {
                        res.add( comparison );
                    }
                }
            }
            catch( RepositoryModifiedException rme )
            {
                // FIXME: What to do in this case???
            }
            catch( ProviderException pe )
            {
                log.error( "Unable to retrieve page from cache", pe );
            }
            catch( IOException ioe )
            {
                log.error( "Failed to search page", ioe );
            }
        }
    
        return( res );
    }


    /**
     * @param queryTerms
     * @return Collection of WikiPage items for the pages that Lucene claims will match the search.
     */
    private Collection searchLucene( QueryItem[] queryTerms )
    {
        try
        {
            Searcher searcher = new IndexSearcher(m_luceneDirectory);

            BooleanQuery query = new BooleanQuery();
            for ( int curr = 0; curr < queryTerms.length; curr++ )
            {
                QueryItem queryTerm = queryTerms[curr];
                if( queryTerm.word.indexOf(' ') >= 0 )
                {   // this is a phrase search
                    StringTokenizer tok = new StringTokenizer(queryTerm.word);
                    while( tok.hasMoreTokens() )
                    {
// Just find pages with the words, so that included stop words don't mess up search.
                        String word = tok.nextToken();
                        query.add(new TermQuery(new Term(LUCENE_PAGE_CONTENTS, word)),
                                queryTerm.type == QueryItem.REQUIRED,
                                queryTerm.type == QueryItem.FORBIDDEN);
                    }
/* Since we're not using Lucene to score, no reason to use PhraseQuery, which removes stop words.
                    PhraseQuery phraseQ = new PhraseQuery();
                    StringTokenizer tok = new StringTokenizer(queryTerm.word);
                    while (tok.hasMoreTokens()) {
                        String word = tok.nextToken();
                        phraseQ.add(new Term(LUCENE_PAGE_CONTENTS, word));
                    }
                    query.add(phraseQ,
                            queryTerm.type == QueryItem.REQUIRED,
                            queryTerm.type == QueryItem.FORBIDDEN);
*/
                }
                else
                { // single word query
                    query.add(new TermQuery(new Term(LUCENE_PAGE_CONTENTS, queryTerm.word)),
                            queryTerm.type == QueryItem.REQUIRED,
                            queryTerm.type == QueryItem.FORBIDDEN);
                }
            }
            Hits hits = searcher.search(query);

            ArrayList list = new ArrayList(hits.length());
            for ( int curr = 0; curr < hits.length(); curr++ )
            {
                Document doc = hits.doc(curr);
                String pageName = doc.get(LUCENE_ID);
                WikiPage result = getPageInfo(pageName, WikiPageProvider.LATEST_VERSION);
                if (result != null)
                {
                    list.add(result);
                }
                else
                {
                    log.error("Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache");
                    deleteFromLucene(new WikiPage(pageName));
                }
            }

            searcher.close();
            return list;
        }
        catch ( Exception e )
        {
            log.error("Failed during Lucene search", e);
            return Collections.EMPTY_LIST;
        }
    }


    public WikiPage getPageInfo( String pageName, int version )
        throws ProviderException,
               RepositoryModifiedException
    {
        WikiPage cached = getPageInfoFromCache( pageName );
        
        int latestcached = (cached != null) ? cached.getVersion() : Integer.MIN_VALUE;
       
        if( version == WikiPageProvider.LATEST_VERSION ||
            version == latestcached )
        {
            if( cached == null )
            {
                WikiPage data = m_provider.getPageInfo( pageName, version );

                if( data != null )
                {
                    m_cache.putInCache( pageName, data );
                    // Requests for this page are now no longer denied
                    m_negCache.putInCache( pageName, null );
                }
                return data;
            }

            return cached;
        }        
        else
        {
            // We do not cache old versions.
            return m_provider.getPageInfo( pageName, version );
        }
    }

    public List getVersionHistory( String page )
        throws ProviderException
    {
        List history = null;

        try
        {
            history = (List)m_historyCache.getFromCache( page,
                                                         m_expiryPeriod );

            log.debug("History cache hit for page "+page);
            m_historyCacheHits++;
        }
        catch( NeedsRefreshException e )
        {
            history = m_provider.getVersionHistory( page );

            m_historyCache.putInCache( page, history );

            log.debug("History cache miss for page "+page);
            m_historyCacheMisses++;
        }

        return history;
    }

    public synchronized String getProviderInfo()
    {              
        return("Real provider: "+m_provider.getClass().getName()+
               "<br />Cache misses: "+m_cacheMisses+
               "<br />Cache hits: "+m_cacheHits+
               "<br />History cache hits: "+m_historyCacheHits+
               "<br />History cache misses: "+m_historyCacheMisses+
               "<br />Cache consistency checks: "+m_expiryPeriod+"s"+
               "<br />Lucene enabled: "+(m_useLucene?"yes":"no") );
    }

    public void deleteVersion( String pageName, int version )
        throws ProviderException
    {
        //
        //  Luckily, this is such a rare operation it is okay
        //  to synchronize against the whole thing.
        //
        synchronized( this )
        {
            WikiPage cached = getPageInfoFromCache( pageName );

            int latestcached = (cached != null) ? cached.getVersion() : Integer.MIN_VALUE;
        
            //
            //  If we have this version cached, remove from cache.
            //
            if( version == WikiPageProvider.LATEST_VERSION ||
                version == latestcached )
            {
                m_cache.flushEntry( pageName );
                m_textCache.putInCache( pageName, null );
                m_historyCache.putInCache( pageName, null );
            }

            m_provider.deleteVersion( pageName, version );
        }
    }

    public void deletePage( String pageName )
        throws ProviderException
    {
        //
        //  See note in deleteVersion().
        //
        synchronized(this)
        {
            if( m_useLucene ) 
            {
                deleteFromLucene(getPageInfo(pageName, WikiPageProvider.LATEST_VERSION));
            }

            m_cache.putInCache( pageName, null );
            m_textCache.putInCache( pageName, null );
            m_historyCache.putInCache( pageName, null );
            m_negCache.putInCache( pageName, pageName );
            m_provider.deletePage( pageName );
        }
    }

    /**
     *  Returns the actual used provider.
     *  @since 2.0
     */
    public WikiPageProvider getRealProvider()
    {
        return m_provider;
    }

    /**
     *  This is a simple class that keeps a list of all WikiPages that
     *  we have in memory.  Because the OSCache cannot give us a list
     *  of all pages currently in cache, we'll have to check this.
     * 
     *  @author jalkanen
     *
     *  @since
     */
    private class CacheItemCollector
        implements CacheEntryEventListener
    {
        private TreeSet m_allItems = new TreeSet();
        
        public Set getAllItems()
        {
            return m_allItems;
        }
        
        public void cacheEntryAdded( CacheEntryEvent arg0 )
        {
        }

        public void cacheEntryFlushed( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                m_allItems.remove( item );
            }
        }

        public void cacheEntryRemoved( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                m_allItems.remove( item );
            }
        }

        public void cacheEntryUpdated( CacheEntryEvent arg0 )
        {
            WikiPage item = (WikiPage) arg0.getEntry().getContent();

            if( item != null )
            {
                m_allItems.add( item );
            }
        }
    }
}
