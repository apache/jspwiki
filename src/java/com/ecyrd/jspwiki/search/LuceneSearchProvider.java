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
package com.ecyrd.jspwiki.search;

import java.io.*;
import java.util.*;

import org.apache.commons.lang.StringUtils;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

import org.apache.jspwiki.api.WikiPage;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.content.WikiName;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.*;

/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @author Arent-Jan Banck
 *  @since 2.2.21.
 */
public class LuceneSearchProvider implements SearchProvider
{
    protected static final Logger log = LoggerFactory.getLogger(LuceneSearchProvider.class);

    private WikiEngine m_engine;

    // Lucene properties.

    /** Which analyzer to use.  Default is StandardAnalyzer. */
    public static final String PROP_LUCENE_ANALYZER    = "jspwiki.lucene.analyzer";

    private static final String PROP_LUCENE_INDEXDELAY   = "jspwiki.lucene.indexdelay";
    private static final String PROP_LUCENE_INITIALDELAY = "jspwiki.lucene.initialdelay";

    private String m_analyzerClass = "org.apache.lucene.analysis.standard.StandardAnalyzer";

    private static final String LUCENE_DIR             = "lucene";

    /**
     *  Number of page updates before we optimize the index.
     */
    public static final int LUCENE_OPTIMIZE_COUNT      = 10;
    protected static final String LUCENE_ID            = "id";
    protected static final String LUCENE_PAGE_CONTENTS = "contents";
    protected static final String LUCENE_AUTHOR        = "author";
    protected static final String LUCENE_ATTACHMENTS   = "attachment";
    protected static final String LUCENE_PAGE_NAME     = "name";

    private String           m_luceneDirectory = null;
    private int              m_updateCount = 0;
    protected Vector<Object[]> m_updates = new Vector<Object[]>(); // Vector because multi-threaded.

    /** Maximum number of fragments from search matches. */
    private static final int MAX_FRAGMENTS = 3;

    private static String c_punctuationSpaces = StringUtils.repeat(" ", MarkupParser.PUNCTUATION_CHARS_ALLOWED.length() );

    /**
     *  {@inheritDoc}
     */
    public void initialize(WikiEngine engine, Properties props)
            throws NoRequiredPropertyException, IOException
    {
        m_engine = engine;

        m_luceneDirectory = engine.getWorkDir()+File.separator+LUCENE_DIR;

        int initialDelay = TextUtil.getIntegerProperty( props, PROP_LUCENE_INITIALDELAY, LuceneUpdater.INITIAL_DELAY );
        int indexDelay   = TextUtil.getIntegerProperty( props, PROP_LUCENE_INDEXDELAY, LuceneUpdater.INDEX_DELAY );

        m_analyzerClass = TextUtil.getStringProperty( props, PROP_LUCENE_ANALYZER, m_analyzerClass );
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
        }
        catch ( IOException e )
        {
            log.error("Problem while creating Lucene index - not using Lucene.", e);
        }

        // Start the Lucene update thread, which waits first
        // for a little while before starting to go through
        // the Lucene "pages that need updating".
        LuceneUpdater updater = new LuceneUpdater( m_engine, this, initialDelay, indexDelay );
        updater.start();
    }

    /**
     *  Returns the handling engine.
     *
     *  @return Current WikiEngine
     */
    protected WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     *  Performs a full Lucene reindex, if necessary.
     *
     *  @throws IOException If there's a problem during indexing
     */
    protected void doFullLuceneReindex()
        throws IOException
    {
        File dir = new File(m_luceneDirectory);

        String[] filelist = dir.list();

        if( filelist == null )
        {
            throw new IOException( "Invalid Lucene directory: cannot produce listing: "+dir.getAbsolutePath());
        }

        try
        {
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
                    Collection allPages = m_engine.getPageManager().getAllPages();

                    for( Iterator iterator = allPages.iterator(); iterator.hasNext(); )
                    {
                        WikiPage page = (WikiPage) iterator.next();
                        
                        try
                        {
                            String text = m_engine.getPageManager().getPageText( page.getName(),
                                                                                 WikiProvider.LATEST_VERSION );
                            luceneIndexPage( page, text, writer );
                        }
                        catch( Exception e )
                        {
                            log.info("Unable to index page, continuing to next: "+page.getName(),e );
                        }
                    }

                    Collection allAttachments = m_engine.getAttachmentManager().getAllAttachments();
                    for( Iterator iterator = allAttachments.iterator(); iterator.hasNext(); )
                    {
                        Attachment att = (Attachment) iterator.next();
                        
                        try
                        {
                            String text = getAttachmentContent( att.getName(),
                                                                WikiProvider.LATEST_VERSION );
                            luceneIndexPage( att, text, writer );
                        }
                        catch( Exception e )
                        {
                            log.info("Unable to index attachment, continuing to next: "+att.getName(),e );                            
                        }
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
        }
        catch ( IOException e )
        {
            log.error("Problem while creating Lucene index - not using Lucene.", e);
        }
        catch ( ProviderException e )
        {
            log.error("Problem reading pages while creating Lucene index (JSPWiki won't start.)", e);
            throw new IllegalArgumentException("unable to create Lucene index");
        }
        catch( ClassNotFoundException e )
        {
            log.error("Illegal Analyzer specified:",e);
        }
        catch( Exception e )
        {
            log.error("Unable to start lucene",e);
        }

    }

    /**
     *  Fetches the attachment content from the repository.
     *  Content is flat text that can be used for indexing/searching or display
     *  
     *  @param attachmentName Name of the attachment.
     *  @param version The version of the attachment.
     *  
     *  @return the content of the Attachment as a String.
     */
    protected String getAttachmentContent( String attachmentName, int version )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();

        try
        {
            Attachment att = mgr.getAttachmentInfo( attachmentName, version );
            //FIXME: Find out why sometimes att is null
            if(att != null)
            {
                return getAttachmentContent( att );
            }
        }
        catch (ProviderException e)
        {
            log.error("Attachment cannot be loaded", e);
        }
        // Something was wrong, no result is returned.
        return null;
    }

    /**
     * @param att Attachment to get content for. Filename extension is used to determine the type of the attachment.
     * @return String representing the content of the file.
     * FIXME This is a very simple implementation of some text-based attachment, mainly used for testing.
     * This should be replaced /moved to Attachment search providers or some other 'plugable' wat to search attachments
     */
    protected String getAttachmentContent( Attachment att )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        //FIXME: Add attachment plugin structure

        String filename = att.getFileName();

        if(filename.endsWith(".txt") ||
           filename.endsWith(".xml") ||
           filename.endsWith(".ini") ||
           filename.endsWith(".html"))
        {
            InputStream attStream;

            try
            {
                attStream = mgr.getAttachmentStream( att );

                StringWriter sout = new StringWriter();
                FileUtil.copyContents( new InputStreamReader(attStream), sout );

                attStream.close();
                sout.close();

                return sout.toString();
            }
            catch (ProviderException e)
            {
                log.error("Attachment cannot be loaded", e);
                return null;
            }
            catch (IOException e)
            {
                log.error("Attachment cannot be loaded", e);
                return null;
            }
        }

        return null;
    }

    /**
     *  Updates the lucene index for a single page.
     *
     *  @param page The WikiPage to check
     *  @param text The page text to index.
     */
    protected synchronized void updateLuceneIndex( WikiPage page, String text )
    {
        IndexWriter writer = null;

        log.debug("Updating Lucene index for page '" + page.getName() + "'...");

        try
        {
            pageRemoved(page);

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

    /**
     *  Indexes page using the given IndexWriter.
     *
     *  @param page WikiPage
     *  @param text Page text to index
     *  @param writer The Lucene IndexWriter to use for indexing
     *  @return the created index Document
     *  @throws IOException If there's an indexing problem
     */
    protected Document luceneIndexPage( WikiPage page, String text, IndexWriter writer )
        throws IOException
    {
        if( log.isDebugEnabled() ) log.debug( "Indexing "+page.getName()+"..." );
        
        // make a new, empty document
        Document doc = new Document();

        if( text == null ) return doc;

        // Raw name is the keyword we'll use to refer to this document for updates.
        Field field = new Field(LUCENE_ID, page.getName(), Field.Store.YES, Field.Index.UN_TOKENIZED);
        doc.add( field );

        // Body text.  It is stored in the doc for search contexts.
        field = new Field(LUCENE_PAGE_CONTENTS, text,
                          Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO);
        doc.add( field );

        // Allow searching by page name. Both beautified and raw
        String unTokenizedTitle = StringUtils.replaceChars( page.getName(),
                                                            MarkupParser.PUNCTUATION_CHARS_ALLOWED,
                                                            c_punctuationSpaces );

        field = new Field(LUCENE_PAGE_NAME,
                          TextUtil.beautifyString( page.getName() ) + " " + unTokenizedTitle,
                          Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO);
        doc.add( field );

        // Allow searching by authorname

        if( page.getAuthor() != null )
        {
            field = new Field(LUCENE_AUTHOR, page.getAuthor(),
                              Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO);
            doc.add( field );
        }

        // Now add the names of the attachments of this page
        try
        {
            Collection attachments = m_engine.getAttachmentManager().listAttachments(page);
            String attachmentNames = "";

            for( Iterator it = attachments.iterator(); it.hasNext(); )
            {
                Attachment att = (Attachment) it.next();
                attachmentNames += att.getName() + ";";
            }
            field = new Field(LUCENE_ATTACHMENTS, attachmentNames,
                              Field.Store.YES, Field.Index.TOKENIZED, Field.TermVector.NO);
            doc.add( field );

        }
        catch(ProviderException e)
        {
            // Unable to read attachments
            log.error("Failed to get attachments for page", e);
        }
        writer.addDocument(doc);

        return doc;
    }

    /**
     *  {@inheritDoc}
     */
    public void pageRemoved( WikiPage page )
    {
        try
        {
            // Must first remove existing version of page.
            IndexReader reader = IndexReader.open(m_luceneDirectory);
            reader.deleteDocuments(new Term(LUCENE_ID, page.getName()));
            reader.close();
        }
        catch ( IOException e )
        {
            log.error("Unable to update page '" + page.getName() + "' from Lucene index", e);
        }
    }


    /**
     *  Adds a page-text pair to the lucene update queue.  Safe to call always
     *
     *  @param page WikiPage to add to the update queue.
     */
    public void reindexPage( WikiPage page )
    {
        if( page != null )
        {
            String text;

            // TODO: Think if this was better done in the thread itself?

            if( page instanceof Attachment )
            {
                text = getAttachmentContent( (Attachment) page );
            }
            else
            {
                text = m_engine.getPureText( page );
            }

            if( text != null )
            {
                // Add work item to m_updates queue.
                Object[] pair = new Object[2];
                pair[0] = page;
                pair[1] = text;
                m_updates.add(pair);
                log.debug("Scheduling page " + page.getName() + " for index update");
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    public Collection findPages( String query )
        throws ProviderException
    {
        return findPages( query, FLAG_CONTEXTS );
    }

    /**
     *  Create contexts also.  Generating contexts can be expensive,
     *  so they're not on by default.
     */
    public static final int FLAG_CONTEXTS = 0x01;

    /**
     *  Searches pages using a particular combination of flags.
     *
     *  @param query The query to perform in Lucene query language
     *  @param flags A set of flags
     *  @return A Collection of SearchResult instances
     *  @throws ProviderException if there is a problem with the backend
     */
    public Collection findPages( String query, int flags )
        throws ProviderException
    {
        Searcher  searcher = null;
        ArrayList<SearchResult> list = null;
        Highlighter highlighter = null;

        try
        {
            String[] queryfields = { LUCENE_PAGE_CONTENTS, LUCENE_PAGE_NAME, LUCENE_AUTHOR, LUCENE_ATTACHMENTS };
            QueryParser qp = new MultiFieldQueryParser( queryfields, getLuceneAnalyzer() );

            //QueryParser qp = new QueryParser( LUCENE_PAGE_CONTENTS, getLuceneAnalyzer() );
            Query luceneQuery = qp.parse( query );

            if( (flags & FLAG_CONTEXTS) != 0 )
            {
                highlighter = new Highlighter(new SimpleHTMLFormatter("<span class=\"searchmatch\">", "</span>"),
                                              new SimpleHTMLEncoder(),
                                              new QueryScorer(luceneQuery));
            }

            try
            {
                searcher = new IndexSearcher(m_luceneDirectory);
            }
            catch( Exception ex )
            {
                log.info("Lucene not yet ready; indexing not started",ex);
                return null;
            }

            Hits hits = searcher.search(luceneQuery);

            list = new ArrayList<SearchResult>(hits.length());
            for ( int curr = 0; curr < hits.length(); curr++ )
            {
                Document doc = hits.doc(curr);
                String pageName = doc.get(LUCENE_ID);
                WikiPage page = m_engine.getPage(pageName, WikiPageProvider.LATEST_VERSION);

                if(page != null)
                {
                    if(page instanceof Attachment)
                    {
                        // Currently attachments don't look nice on the search-results page
                        // When the search-results are cleaned up this can be enabled again.
                    }

                    int score = (int)(hits.score(curr) * 100);


                    // Get highlighted search contexts
                    String text = doc.get(LUCENE_PAGE_CONTENTS);

                    String[] fragments = new String[0];
                    if( text != null && highlighter != null )
                    {
                        TokenStream tokenStream = getLuceneAnalyzer()
                        .tokenStream(LUCENE_PAGE_CONTENTS, new StringReader(text));
                        fragments = highlighter.getBestFragments(tokenStream,
                                                                 text, MAX_FRAGMENTS);

                    }

                    SearchResult result = new SearchResultImpl( page, score, fragments );     
                    list.add(result);
                }
                else
                {
                    log.error("Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache");
                    pageRemoved(m_engine.createPage( WikiName.valueOf( pageName ) ));
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
            log.info("Broken query; cannot parse",e);

            throw new ProviderException("You have entered a query Lucene cannot process: "+e.getMessage());
        }
        finally
        {
            if( searcher != null )
            {
                try
                {
                    searcher.close();
                }
                catch( IOException e )
                {}
            }
        }

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    public String getProviderInfo()
    {
        return "LuceneSearchProvider";
    }

    /**
     * Updater thread that updates Lucene indexes.
     */
    private static final class LuceneUpdater extends WikiBackgroundThread
    {
        protected static final int INDEX_DELAY    = 5;
        protected static final int INITIAL_DELAY = 60;
        private final LuceneSearchProvider m_provider;

        private int m_initialDelay;

        private WatchDog m_watchdog;

        private LuceneUpdater( WikiEngine engine, LuceneSearchProvider provider,
                               int initialDelay, int indexDelay )
        {
            super( engine, indexDelay );
            m_provider = provider;
            setName("JSPWiki Lucene Indexer");
        }

        public void startupTask() throws Exception
        {
            m_watchdog = getEngine().getCurrentWatchDog();

            // Sleep initially...
            try
            {
                Thread.sleep( m_initialDelay * 1000L );
            }
            catch( InterruptedException e )
            {
                throw new InternalWikiException("Interrupted while waiting to start.");
            }

            m_watchdog.enterState("Full reindex");
            // Reindex everything
            m_provider.doFullLuceneReindex();
            m_watchdog.exitState();
        }

        public void backgroundTask() throws Exception
        {
            m_watchdog.enterState("Emptying index queue", 60);

            synchronized ( m_provider.m_updates )
            {
                while( m_provider.m_updates.size() > 0 )
                {
                    Object[] pair = m_provider.m_updates.remove(0);
                    WikiPage page = ( WikiPage ) pair[0];
                    String text = ( String ) pair[1];
                    m_provider.updateLuceneIndex(page, text);
                }
            }

            m_watchdog.exitState();
        }

    }

    // FIXME: This class is dumb; needs to have a better implementation
    private static class SearchResultImpl
        implements SearchResult
    {
        private WikiPage m_page;
        private int      m_score;
        private String[] m_contexts;

        public SearchResultImpl( WikiPage page, int score, String[] contexts )
        {
            m_page  = page;
            m_score = score;
            m_contexts = contexts;
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


        public String[] getContexts()
        {
            return m_contexts;
        }
    }
}
