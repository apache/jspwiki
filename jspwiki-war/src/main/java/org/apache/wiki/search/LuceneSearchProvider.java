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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLEncoder;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WatchDog;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;


/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @since 2.2.21.
 */
public class LuceneSearchProvider implements SearchProvider {

    protected static final Logger log = Logger.getLogger(LuceneSearchProvider.class);

    private WikiEngine m_engine;

    // Lucene properties.

    /** Which analyzer to use.  Default is StandardAnalyzer. */
    public static final String PROP_LUCENE_ANALYZER    = "jspwiki.lucene.analyzer";

    private static final String PROP_LUCENE_INDEXDELAY   = "jspwiki.lucene.indexdelay";
    private static final String PROP_LUCENE_INITIALDELAY = "jspwiki.lucene.initialdelay";

    private String m_analyzerClass = "org.apache.lucene.analysis.standard.ClassicAnalyzer";

    private static final String LUCENE_DIR             = "lucene";

    /** These attachment file suffixes will be indexed. */
    public static final String[] SEARCHABLE_FILE_SUFFIXES = new String[] { ".txt", ".ini", ".xml", ".html", "htm", ".mm", ".htm",
                                                                          ".xhtml", ".java", ".c", ".cpp", ".php", ".asm", ".sh",
                                                                          ".properties", ".kml", ".gpx", ".loc" };

    protected static final String LUCENE_ID            = "id";
    protected static final String LUCENE_PAGE_CONTENTS = "contents";
    protected static final String LUCENE_AUTHOR        = "author";
    protected static final String LUCENE_ATTACHMENTS   = "attachment";
    protected static final String LUCENE_PAGE_NAME     = "name";

    private String           m_luceneDirectory;
    protected Vector<Object[]> m_updates = new Vector<Object[]>(); // Vector because multi-threaded.

    /** Maximum number of fragments from search matches. */
    private static final int MAX_FRAGMENTS = 3;

    /** The maximum number of hits to return from searches. */
    public static final int MAX_SEARCH_HITS = 99999;
    
    private static final String c_punctuationSpaces = StringUtils.repeat(" ", MarkupParser.PUNCTUATION_CHARS_ALLOWED.length() );

    /**
     *  {@inheritDoc}
     */
    @Override
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

                log.info("Starting Lucene reindexing, this can take a couple of minutes...");

                Directory luceneDir = new SimpleFSDirectory(dir.toPath());
                
                try
                {
                    writer = getIndexWriter( luceneDir );
                    long pagesStart = System.currentTimeMillis();
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
                        catch( IOException e )
                        {
                            log.warn( "Unable to index page " + page.getName() + ", continuing to next ", e );
                        }
                    }
                    log.info("Indexed all pages in " + (System.currentTimeMillis() - pagesStart) + "ms");

                    long attachmentStart = System.currentTimeMillis();
                    Collection allAttachments = m_engine.getAttachmentManager().getAllAttachments();
                    for( Iterator iterator = allAttachments.iterator(); iterator.hasNext(); )
                    {
                        Attachment att = (Attachment) iterator.next();
                        
                        try
                        {
                            String text = getAttachmentContent( att.getName(), WikiProvider.LATEST_VERSION );
                            luceneIndexPage( att, text, writer );
                        }
                        catch( IOException e )
                        {
                            log.warn( "Unable to index attachment " + att.getName() + ", continuing to next", e );
                        }
                    }
                    log.info("Indexed all attchments in " + (System.currentTimeMillis() - attachmentStart) + "ms");
                }
                finally
                {
                    close( writer );
                }

                Date end = new Date();
                log.info( "Full Lucene index finished in " + (end.getTime() - start.getTime()) + " milliseconds." );
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
     * This should be replaced /moved to Attachment search providers or some other 'pluggable' wat to search attachments
     */
    protected String getAttachmentContent( Attachment att )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        //FIXME: Add attachment plugin structure

        String filename = att.getFileName();

        boolean searchSuffix = false;
        for( String suffix : SEARCHABLE_FILE_SUFFIXES )
        {
            if( filename.endsWith( suffix ) )
            {
                searchSuffix = true;
            }
        }

        String out = null;
        if( searchSuffix )
        {
            InputStream attStream = null;
            StringWriter sout = new StringWriter();
            
            try
            {
                attStream = mgr.getAttachmentStream( att );
                FileUtil.copyContents( new InputStreamReader(attStream), sout );
                out = sout.toString();
            }
            catch (ProviderException e)
            {
                log.error("Attachment cannot be loaded", e);
            }
            catch (IOException e)
            {
                log.error("Attachment cannot be loaded", e);
            }
            finally 
            {
            	IOUtils.closeQuietly( attStream );
            	IOUtils.closeQuietly( sout );
            }
        }

        return out;
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

        Directory luceneDir = null;
        try
        {
            pageRemoved( page );

            // Now add back the new version.
            luceneDir = new SimpleFSDirectory(new File(m_luceneDirectory).toPath());
            writer = getIndexWriter( luceneDir );
            
            luceneIndexPage( page, text, writer );
        }
        catch ( IOException e )
        {
            log.error("Unable to update page '" + page.getName() + "' from Lucene index", e);
            // reindexPage( page );
        }
        catch( Exception e )
        {
            log.error("Unexpected Lucene exception - please check configuration!",e);
            // reindexPage( page );
        }
        finally
        {
            close( writer );
        }

        log.debug("Done updating Lucene index for page '" + page.getName() + "'.");
    }


    private Analyzer getLuceneAnalyzer() throws ProviderException
    {
        try
        {
            Class< ? > clazz = ClassUtil.findClass( "", m_analyzerClass );
            Constructor< ? > constructor = clazz.getConstructor();
            Analyzer analyzer = (Analyzer) constructor.newInstance();
            return analyzer;
        }
        catch( Exception e )
        {
            String msg = "Could not get LuceneAnalyzer class " + m_analyzerClass + ", reason: ";
            log.error( msg, e );
            throw new ProviderException( msg + e );
        }
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
        Field field = new Field( LUCENE_ID, page.getName(), StringField.TYPE_STORED );
        doc.add( field );

        // Body text.  It is stored in the doc for search contexts.
        field = new Field( LUCENE_PAGE_CONTENTS, text, TextField.TYPE_STORED );
        doc.add( field );

        // Allow searching by page name. Both beautified and raw
        String unTokenizedTitle = StringUtils.replaceChars( page.getName(),
                                                            MarkupParser.PUNCTUATION_CHARS_ALLOWED,
                                                            c_punctuationSpaces );

        field = new Field( LUCENE_PAGE_NAME,
                           TextUtil.beautifyString( page.getName() ) + " " + unTokenizedTitle,
                           TextField.TYPE_STORED );
        doc.add( field );

        // Allow searching by authorname

        if( page.getAuthor() != null )
        {
            field = new Field( LUCENE_AUTHOR, page.getAuthor(), TextField.TYPE_STORED );
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
            field = new Field( LUCENE_ATTACHMENTS, attachmentNames, TextField.TYPE_STORED );
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
    @Override
    public void pageRemoved(WikiPage page )
    {
        IndexWriter writer = null;
        try
        {
            Directory luceneDir = new SimpleFSDirectory(new File(m_luceneDirectory).toPath());
            writer = getIndexWriter( luceneDir );
            Query query = new TermQuery( new Term( LUCENE_ID, page.getName() ) );
            writer.deleteDocuments( query );
        }
        catch ( Exception e )
        {
            log.error("Unable to remove page '" + page.getName() + "' from Lucene index", e);
        }
        finally
        {
            close( writer );
        }
    }
    
    IndexWriter getIndexWriter( Directory luceneDir ) throws
            IOException, ProviderException
    {
        IndexWriter writer = null;
        IndexWriterConfig writerConfig = new IndexWriterConfig( getLuceneAnalyzer() );
        writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
        writer = new IndexWriter( luceneDir, writerConfig );
        
        // writer.setInfoStream( System.out );
        return writer;
    }
    
    void close( IndexWriter writer ) 
    {
        try
        {
            if( writer != null ) 
            {
                writer.close( );
            }
        }
        catch( IOException e )
        {
            log.error( e );
        }
    }


    /**
     *  Adds a page-text pair to the lucene update queue.  Safe to call always
     *
     *  @param page WikiPage to add to the update queue.
     */
    @Override
    public void reindexPage(WikiPage page )
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
    @Override
    public Collection findPages(String query, WikiContext wikiContext )
        throws ProviderException
    {
        return findPages( query, FLAG_CONTEXTS, wikiContext );
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
    public Collection findPages( String query, int flags, WikiContext wikiContext )
        throws ProviderException
    {
        IndexSearcher  searcher = null;
        ArrayList<SearchResult> list = null;
        Highlighter highlighter = null;

        try
        {
            String[] queryfields = { LUCENE_PAGE_CONTENTS, LUCENE_PAGE_NAME, LUCENE_AUTHOR, LUCENE_ATTACHMENTS };

            QueryParser qp = new MultiFieldQueryParser( queryfields, getLuceneAnalyzer() );
            qp.setAllowLeadingWildcard(true);

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
                File dir = new File(m_luceneDirectory);
                Directory luceneDir = new SimpleFSDirectory(dir.toPath());
                IndexReader reader = DirectoryReader.open(luceneDir);
                searcher = new IndexSearcher(reader);
            }
            catch( Exception ex )
            {
                log.info("Lucene not yet ready; indexing not started",ex);
                return null;
            }

            ScoreDoc[] hits = searcher.search(luceneQuery, MAX_SEARCH_HITS).scoreDocs;

            AuthorizationManager mgr = m_engine.getAuthorizationManager();

            list = new ArrayList<SearchResult>(hits.length);
            for ( int curr = 0; curr < hits.length; curr++ )
            {
                int docID = hits[curr].doc;
                Document doc = searcher.doc( docID );
                String pageName = doc.get(LUCENE_ID);
                WikiPage page = m_engine.getPage(pageName, WikiPageProvider.LATEST_VERSION);

                if(page != null)
                {
                    if(page instanceof Attachment)
                    {
                        // Currently attachments don't look nice on the search-results page
                        // When the search-results are cleaned up this can be enabled again.
                    }

                    PagePermission pp = new PagePermission( page, PagePermission.VIEW_ACTION );
	                if( mgr.checkPermission( wikiContext.getWikiSession(), pp ) ) {
	
	                    int score = (int)(hits[curr].score * 100);
	
	
	                    // Get highlighted search contexts
	                    String text = doc.get(LUCENE_PAGE_CONTENTS);
	
	                    String[] fragments = new String[0];
	                    if( text != null && highlighter != null ) {
	                        TokenStream tokenStream = getLuceneAnalyzer()
	                        .tokenStream(LUCENE_PAGE_CONTENTS, new StringReader(text));
	                        fragments = highlighter.getBestFragments(tokenStream, text, MAX_FRAGMENTS);
	                    }
	
	                    SearchResult result = new SearchResultImpl( page, score, fragments );     
	                    list.add(result);
	                }
                }
                else
                {
                    log.error("Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache");
                    pageRemoved(new WikiPage( m_engine, pageName ));
                }
            }
        }
        catch( IOException e )
        {
            log.error("Failed during lucene search",e);
        }
        catch( ParseException e )
        {
            log.info("Broken query; cannot parse query ",e);

            throw new ProviderException("You have entered a query Lucene cannot process: "+e.getMessage());
        }
        catch( InvalidTokenOffsetsException e )
        {
            log.error("Tokens are incompatible with provided text ",e);
        }
        finally
        {
            if( searcher != null )
            {
                try
                {
                    searcher.getIndexReader().close();
                }
                catch( IOException e )
                {
                    log.error( e );
                }
            }
        }

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
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

        @Override
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
                throw new InternalWikiException("Interrupted while waiting to start.", e);
            }

            m_watchdog.enterState("Full reindex");
            // Reindex everything
            m_provider.doFullLuceneReindex();
            m_watchdog.exitState();
        }

        @Override
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
        private final WikiPage m_page;
        private final int      m_score;
        private final String[] m_contexts;

        public SearchResultImpl( WikiPage page, int score, String[] contexts )
        {
            m_page  = page;
            m_score = score;
            m_contexts = contexts != null ? contexts.clone() : null;
        }

        @Override
        public WikiPage getPage()
        {
            return m_page;
        }

        /* (non-Javadoc)
         * @see org.apache.wiki.SearchResult#getScore()
         */
        @Override
        public int getScore()
        {
            return m_score;
        }


        @Override
        public String[] getContexts()
        {
            return m_contexts;
        }
    }

}
