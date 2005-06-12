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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.QueryItem;
import com.ecyrd.jspwiki.SearchMatcher;
import com.ecyrd.jspwiki.SearchResult;
import com.ecyrd.jspwiki.SearchResultComparator;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.RepositoryModifiedException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @author Arent-Jan Banck for Informatica
 *  @since 2.2.21.
 */
public class LuceneSearchProvider implements SearchProvider 
{
    private static final Logger log = Logger.getLogger(LuceneSearchProvider.class);

    private WikiEngine m_engine;

    // Lucene properties.

    /** Which analyzer to use.  Default is StandardAnalyzer. */
    public static final String PROP_LUCENE_ANALYZER    = "jspwiki.lucene.analyzer";

    private String m_analyzerClass = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    private static final String LUCENE_DIR             = "lucene";

    // Number of page updates before we optimize the index.
    public static final int LUCENE_OPTIMIZE_COUNT      = 10;
    private static final String LUCENE_ID              = "id";
    private static final String LUCENE_PAGE_CONTENTS   = "contents";

    // Lucene data, if used.
    private boolean          m_useLucene = false;
    private String           m_luceneDirectory = null;
    private int              m_updateCount = 0;
    private Thread           m_luceneUpdateThread = null;
    private Vector           m_updates = new Vector(); // Vector because multi-threaded.


    public void initialize(WikiEngine engine, Properties props)
            throws NoRequiredPropertyException, IOException 
    {

        m_engine = engine;

        // Moved from CachingProvider

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

                //
                //  Do lock recovery, in case JSPWiki was shut down forcibly
                //
                Directory luceneDir = FSDirectory.getDirectory(dir,false);

                if( IndexReader.isLocked(luceneDir) )
                {
                    log.info("JSPWiki was shut down while Lucene was indexing - unlocking now.");
                    IndexReader.unlock( luceneDir );
                }

                try
                {
                    writer = new IndexWriter( m_luceneDirectory,
                                              getLuceneAnalyzer(),
                                              true );
                    Collection allPages = engine.getPageManager().getAllPages();

                    for( Iterator iterator = allPages.iterator(); iterator.hasNext(); )
                    {
                        WikiPage page = (WikiPage) iterator.next();
                        String text = engine.getPageManager().getPageText( page.getName(),
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
        catch( ClassNotFoundException e )
        {
            log.error("Illegal Analyzer specified:",e);
            m_useLucene = false;
        }
        catch( Exception e )
        {
            log.error("Unable to start lucene",e);
            m_useLucene = false;
        }

        startLuceneUpdateThread();
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

    private synchronized void updateLuceneIndex( WikiPage page, String text )
    {
        IndexWriter writer = null;

        log.debug("Updating Lucene index for page '" + page.getName() + "'...");

        try
        {
            deletePage(page);

            // Now add back the new version.
            writer = new IndexWriter(m_luceneDirectory, getLuceneAnalyzer(), false);
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
        catch( Exception e )
        {
            log.error("Unexpected Lucene exception - please check configuration!",e);
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


    private Analyzer getLuceneAnalyzer()
        throws ClassNotFoundException,
               InstantiationException,
               IllegalAccessException
    {
        Class clazz = ClassUtil.findClass( "", m_analyzerClass );
        Analyzer analyzer = (Analyzer)clazz.newInstance();
        return analyzer;
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

    public void deletePage( WikiPage page )
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

    /**
     * @param queryTerms
     * @return Collection of WikiPage items for the pages that Lucene claims will match the search.
     */
    /*
    public Collection search( QueryItem[] queryTerms ) 
        throws ProviderException
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
// Since we're not using Lucene to score, no reason to use PhraseQuery, which removes stop words.
//                    PhraseQuery phraseQ = new PhraseQuery();
//                    StringTokenizer tok = new StringTokenizer(queryTerm.word);
//                    while (tok.hasMoreTokens()) {
//                        String word = tok.nextToken();
//                        phraseQ.add(new Term(LUCENE_PAGE_CONTENTS, word));
//                    }
//                    query.add(phraseQ,
//                            queryTerm.type == QueryItem.REQUIRED,
//                            queryTerm.type == QueryItem.FORBIDDEN);

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
                WikiPage result = m_engine.getPageManager().getPageInfo(pageName, WikiPageProvider.LATEST_VERSION);
                if (result != null)
                {
                    list.add(result);
                }
                else
                {
                    log.error("Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache");
                    deletePage(new WikiPage(pageName));
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
*/
    /**
     *  Adds a page-text pair to the lucene update queue.  Safe to call
     *  always - if lucene is not used, does nothing.
     */
    public void addToQueue( WikiPage page, String text )
    {
        if( page != null )
        {
            // Add work item to m_updates queue.
            Object[] pair = new Object[2];
            pair[0] = page;
            pair[1] = text;
            m_updates.add(pair);
            log.debug("Scheduling page " + page.getName() + " for index update");
        }
    }

    public Collection findPages( String query )
    {
        Searcher  searcher = null;
        ArrayList list     = null;
        
        try
        {
            QueryParser qp = new QueryParser( LUCENE_PAGE_CONTENTS, getLuceneAnalyzer() );
            Query luceneQuery = qp.parse( query );
            searcher = new IndexSearcher(m_luceneDirectory);

            Hits hits = searcher.search(luceneQuery);

            list = new ArrayList(hits.length());
            for ( int curr = 0; curr < hits.length(); curr++ )
            {
                Document doc = hits.doc(curr);
                String pageName = doc.get(LUCENE_ID);
                WikiPage page = m_engine.getPageManager().getPageInfo(pageName, WikiPageProvider.LATEST_VERSION);
                
                if(page != null)
                {
                    int score = (int)(hits.score(curr) * 100);
                    SearchResult result = new SearchResultImpl( page, score );
                    list.add(result);
                }
                else
                {
                    log.error("Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache");
                    deletePage(new WikiPage(pageName));
                }
            }
        }
        catch( IOException e )
        {
            log.error("Failed during lucene search",e);
        }
        catch( InstantiationException e )
        {
            log.error("Unable to get a Lucene analyzer",e);
        }
        catch( IllegalAccessException e )
        {
            log.error("Unable to get a Lucene analyzer",e);
        }
        catch( ClassNotFoundException e )
        {
            log.error("Specified Lucene analyzer does not exist",e);
        }
        catch( ParseException e )
        {
            log.error("Broken query; cannot parse",e);
        }
        catch( ProviderException e )
        {
            log.error("Unable to get page data while searching",e);
        }
        finally
        {
            if( searcher != null ) try { searcher.close(); } catch( IOException e ) {}
        }
        
        return list;
    }


    public String getProviderInfo()
    {
        return "LuceneSearchProvider";
    }
    
    // FIXME: This class is dumb; needs to have a better implementation
    private class SearchResultImpl
        implements SearchResult
    {
        private WikiPage m_page;
        private int      m_score;
        
        public SearchResultImpl( WikiPage page, int score )
        {
            m_page  = page;
            m_score = score;
        }

        public WikiPage getPage()
        {
            return m_page;
        }

        /* (non-Javadoc)
         * @see com.ecyrd.jspwiki.SearchResult#getScore()
         */
        public int getScore()
        {
            return m_score;
        }
        
    }
        
}
