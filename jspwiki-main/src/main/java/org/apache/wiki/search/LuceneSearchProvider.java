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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
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
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WatchDog;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.providers.WikiProvider;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.FileUtil;
import org.apache.wiki.util.TextUtil;

import java.io.*;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @since 2.2.21.
 */
public class LuceneSearchProvider implements SearchProvider {

    protected static final Logger log = LogManager.getLogger(LuceneSearchProvider.class);

    private Engine m_engine;
    private Executor searchExecutor;

    // Lucene properties.

    /** Which analyzer to use.  Default is StandardAnalyzer. */
    public static final String PROP_LUCENE_ANALYZER      = "jspwiki.lucene.analyzer";
    private static final String PROP_LUCENE_INDEXDELAY   = "jspwiki.lucene.indexdelay";
    private static final String PROP_LUCENE_INITIALDELAY = "jspwiki.lucene.initialdelay";

    private String m_analyzerClass = "org.apache.lucene.analysis.standard.ClassicAnalyzer";

    private static final String LUCENE_DIR = "lucene";

    /** These attachment file suffixes will be indexed. */
    public static final String[] SEARCHABLE_FILE_SUFFIXES = new String[] { ".txt", ".ini", ".xml", ".html", "htm", ".mm", ".htm",
                                                                           ".xhtml", ".java", ".c", ".cpp", ".php", ".asm", ".sh",
                                                                           ".properties", ".kml", ".gpx", ".loc", ".md", ".xml" };

    protected static final String LUCENE_ID            = "id";
    protected static final String LUCENE_PAGE_CONTENTS = "contents";
    protected static final String LUCENE_AUTHOR        = "author";
    protected static final String LUCENE_ATTACHMENTS   = "attachment";
    protected static final String LUCENE_PAGE_NAME     = "name";
    protected static final String LUCENE_PAGE_KEYWORDS = "keywords";

    private String m_luceneDirectory;
    protected final List< Object[] > m_updates = Collections.synchronizedList( new ArrayList<>() );

    /** Maximum number of fragments from search matches. */
    private static final int MAX_FRAGMENTS = 3;

    /** The maximum number of hits to return from searches. */
    public static final int MAX_SEARCH_HITS = 99_999;
    
    private static final String PUNCTUATION_TO_SPACES = StringUtils.repeat(" ", TextUtil.PUNCTUATION_CHARS_ALLOWED.length() );

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws NoRequiredPropertyException, IOException  {
        m_engine = engine;
        searchExecutor = Executors.newCachedThreadPool();

        m_luceneDirectory = engine.getWorkDir()+File.separator+LUCENE_DIR;

        final int initialDelay = TextUtil.getIntegerProperty( props, PROP_LUCENE_INITIALDELAY, LuceneUpdater.INITIAL_DELAY );
        final int indexDelay   = TextUtil.getIntegerProperty( props, PROP_LUCENE_INDEXDELAY, LuceneUpdater.INDEX_DELAY );

        m_analyzerClass = TextUtil.getStringProperty( props, PROP_LUCENE_ANALYZER, m_analyzerClass );
        // FIXME: Just to be simple for now, we will do full reindex only if no files are in lucene directory.

        final File dir = new File(m_luceneDirectory);
        log.info("Lucene enabled, cache will be in: "+dir.getAbsolutePath());
        try {
            if( !dir.exists() ) {
                dir.mkdirs();
            }

            if( !dir.exists() || !dir.canWrite() || !dir.canRead() ) {
                log.error("Cannot write to Lucene directory, disabling Lucene: "+dir.getAbsolutePath());
                throw new IOException( "Invalid Lucene directory." );
            }

            final String[] filelist = dir.list();
            if( filelist == null ) {
                throw new IOException( "Invalid Lucene directory: cannot produce listing: "+dir.getAbsolutePath());
            }
        } catch( final IOException e ) {
            log.error("Problem while creating Lucene index - not using Lucene.", e);
        }

        // Start the Lucene update thread, which waits first
        // for a little while before starting to go through
        // the Lucene "pages that need updating".
        final LuceneUpdater updater = new LuceneUpdater( m_engine, this, initialDelay, indexDelay );
        updater.start();
    }

    /**
     *  Returns the handling engine.
     *
     *  @return Current Engine
     */
    protected Engine getEngine()
    {
        return m_engine;
    }

    /**
     *  Performs a full Lucene reindex, if necessary.
     *
     *  @throws IOException If there's a problem during indexing
     */
    protected void doFullLuceneReindex() throws IOException {
        final File dir = new File(m_luceneDirectory);
        final String[] filelist = dir.list();
        if( filelist == null ) {
            throw new IOException( "Invalid Lucene directory: cannot produce listing: "+dir.getAbsolutePath());
        }

        try {
            if( filelist.length == 0 ) {
                //
                //  No files? Reindex!
                //
                final Date start = new Date();

                log.info("Starting Lucene reindexing, this can take a couple of minutes...");

                final Directory luceneDir = new NIOFSDirectory( dir.toPath() );
                try( final IndexWriter writer = getIndexWriter( luceneDir ) ) {
                    final Collection< Page > allPages = m_engine.getManager( PageManager.class ).getAllPages();
                    for( final Page page : allPages ) {
                        try {
                            final String text = m_engine.getManager( PageManager.class ).getPageText( page.getName(), WikiProvider.LATEST_VERSION );
                            luceneIndexPage( page, text, writer );
                        } catch( final IOException e ) {
                            log.warn( "Unable to index page " + page.getName() + ", continuing to next ", e );
                        }
                    }

                    final Collection< Attachment > allAttachments = m_engine.getManager( AttachmentManager.class ).getAllAttachments();
                    for( final Attachment att : allAttachments ) {
                        try {
                            final String text = getAttachmentContent( att.getName(), WikiProvider.LATEST_VERSION );
                            luceneIndexPage( att, text, writer );
                        } catch( final IOException e ) {
                            log.warn( "Unable to index attachment " + att.getName() + ", continuing to next", e );
                        }
                    }

                }

                final Date end = new Date();
                log.info( "Full Lucene index finished in " + (end.getTime() - start.getTime()) + " milliseconds." );
            } else {
                log.info("Files found in Lucene directory, not reindexing.");
            }
        } catch ( final IOException e ) {
            log.error("Problem while creating Lucene index - not using Lucene.", e);
        } catch ( final ProviderException e ) {
            log.error("Problem reading pages while creating Lucene index (JSPWiki won't start.)", e);
            throw new IllegalArgumentException("unable to create Lucene index");
        } catch( final Exception e ) {
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
    protected String getAttachmentContent( final String attachmentName, final int version ) {
        final AttachmentManager mgr = m_engine.getManager( AttachmentManager.class );
        try {
            final Attachment att = mgr.getAttachmentInfo( attachmentName, version );
            //FIXME: Find out why sometimes att is null
            if( att != null ) {
                return getAttachmentContent( att );
            }
        } catch( final ProviderException e ) {
            log.error("Attachment cannot be loaded", e);
        }
        return null;
    }

    /**
     * @param att Attachment to get content for. Filename extension is used to determine the type of the attachment.
     * @return String representing the content of the file.
     * FIXME This is a very simple implementation of some text-based attachment, mainly used for testing.
     * This should be replaced /moved to Attachment search providers or some other 'pluggable' way to search attachments
     */
    protected String getAttachmentContent( final Attachment att ) {
        final AttachmentManager mgr = m_engine.getManager( AttachmentManager.class );
        //FIXME: Add attachment plugin structure

        final String filename = att.getFileName();

        boolean searchSuffix = false;
        for( final String suffix : SEARCHABLE_FILE_SUFFIXES ) {
            if( filename.endsWith( suffix ) ) {
                searchSuffix = true;
                break;
            }
        }

        String out = filename;
        if( searchSuffix ) {
            try( final InputStream attStream = mgr.getAttachmentStream( att ); final StringWriter sout = new StringWriter() ) {
                FileUtil.copyContents( new InputStreamReader( attStream ), sout );
                out = out + " " + sout;
            } catch( final ProviderException | IOException e ) {
                log.error("Attachment cannot be loaded", e);
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
    protected synchronized void updateLuceneIndex( final Page page, final String text ) {
        log.debug("Updating Lucene index for page '" + page.getName() + "'...");
        pageRemoved( page );

        // Now add back the new version.
        try( final Directory luceneDir = new NIOFSDirectory( new File( m_luceneDirectory ).toPath() );
             final IndexWriter writer = getIndexWriter( luceneDir ) ) {
            luceneIndexPage( page, text, writer );
        } catch( final IOException e ) {
            log.error("Unable to update page '" + page.getName() + "' from Lucene index", e);
            // reindexPage( page );
        } catch( final Exception e ) {
            log.error("Unexpected Lucene exception - please check configuration!",e);
            // reindexPage( page );
        }

        log.debug("Done updating Lucene index for page '" + page.getName() + "'.");
    }

    private Analyzer getLuceneAnalyzer() throws ProviderException {
        try {
            final Class< ? > clazz = ClassUtil.findClass( "", m_analyzerClass );
            final Constructor< ? > constructor = clazz.getConstructor();
            return ( Analyzer )constructor.newInstance();
        } catch( final Exception e ) {
            final String msg = "Could not get LuceneAnalyzer class " + m_analyzerClass + ", reason: ";
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
    protected Document luceneIndexPage( final Page page, final String text, final IndexWriter writer ) throws IOException {
        if( log.isDebugEnabled() ) {
            log.debug( "Indexing " + page.getName() + "..." );
        }
        
        // make a new, empty document
        final Document doc = new Document();

        if( text == null ) {
            return doc;
        }
        final String indexedText = text.replace( "__", " " ); // be nice to Language Analyzers - cfr. JSPWIKI-893

        // Raw name is the keyword we'll use to refer to this document for updates.
        Field field = new Field( LUCENE_ID, page.getName(), StringField.TYPE_STORED );
        doc.add( field );

        // Body text.  It is stored in the doc for search contexts.
        field = new Field( LUCENE_PAGE_CONTENTS, indexedText, TextField.TYPE_STORED );
        doc.add( field );

        // Allow searching by page name. Both beautified and raw
        final String unTokenizedTitle = StringUtils.replaceChars( page.getName(), TextUtil.PUNCTUATION_CHARS_ALLOWED, PUNCTUATION_TO_SPACES );
        field = new Field( LUCENE_PAGE_NAME, TextUtil.beautifyString( page.getName() ) + " " + unTokenizedTitle, TextField.TYPE_STORED );
        doc.add( field );

        // Allow searching by authorname
        if( page.getAuthor() != null ) {
            field = new Field( LUCENE_AUTHOR, page.getAuthor(), TextField.TYPE_STORED );
            doc.add( field );
        }

        // Now add the names of the attachments of this page
        try {
            final List< Attachment > attachments = m_engine.getManager( AttachmentManager.class ).listAttachments( page );
            final StringBuilder attachmentNames = new StringBuilder();

            for( final Attachment att : attachments ) {
                attachmentNames.append( att.getName() ).append( ";" );
            }
            field = new Field( LUCENE_ATTACHMENTS, attachmentNames.toString(), TextField.TYPE_STORED );
            doc.add( field );

        } catch( final ProviderException e ) {
            // Unable to read attachments
            log.error( "Failed to get attachments for page", e );
        }

        // also index page keywords, if available
        if( page.getAttribute( "keywords" ) != null ) {
            field = new Field( LUCENE_PAGE_KEYWORDS, page.getAttribute( "keywords" ).toString(), TextField.TYPE_STORED );
            doc.add( field );
        }
        synchronized( writer ) {
            writer.addDocument(doc);
        }

        return doc;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public synchronized void pageRemoved( final Page page ) {
        try( final Directory luceneDir = new NIOFSDirectory( new File( m_luceneDirectory ).toPath() );
             final IndexWriter writer = getIndexWriter( luceneDir ) ) {
            final Query query = new TermQuery( new Term( LUCENE_ID, page.getName() ) );
            writer.deleteDocuments( query );
        } catch ( final Exception e ) {
            log.error("Unable to remove page '" + page.getName() + "' from Lucene index", e);
        }
    }
    
    IndexWriter getIndexWriter(final  Directory luceneDir ) throws IOException, ProviderException {
        final IndexWriterConfig writerConfig = new IndexWriterConfig( getLuceneAnalyzer() );
        writerConfig.setOpenMode( OpenMode.CREATE_OR_APPEND );
        return new IndexWriter( luceneDir, writerConfig );
    }
    
    /**
     *  Adds a page-text pair to the lucene update queue.  Safe to call always
     *
     *  @param page WikiPage to add to the update queue.
     */
    @Override
    public void reindexPage( final Page page ) {
        if( page != null ) {
            final String text;

            // TODO: Think if this was better done in the thread itself?
            if( page instanceof Attachment ) {
                text = getAttachmentContent( ( Attachment )page );
            } else {
                text = m_engine.getManager( PageManager.class ).getPureText( page );
            }

            if( text != null ) {
                // Add work item to m_updates queue.
                final Object[] pair = new Object[2];
                pair[0] = page;
                pair[1] = text;
                m_updates.add( pair );
                log.debug("Scheduling page " + page.getName() + " for index update");
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< SearchResult > findPages( final String query, final Context wikiContext ) throws ProviderException {
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
    public Collection< SearchResult > findPages( final String query, final int flags, final Context wikiContext ) throws ProviderException {
        ArrayList<SearchResult> list = null;
        Highlighter highlighter = null;

        try( final Directory luceneDir = new NIOFSDirectory( new File( m_luceneDirectory ).toPath() );
             final IndexReader reader = DirectoryReader.open( luceneDir ) ) {
            final String[] queryfields = { LUCENE_PAGE_CONTENTS, LUCENE_PAGE_NAME, LUCENE_AUTHOR, LUCENE_ATTACHMENTS, LUCENE_PAGE_KEYWORDS };
            final QueryParser qp = new MultiFieldQueryParser( queryfields, getLuceneAnalyzer() );
            final Query luceneQuery = qp.parse( query );
            final IndexSearcher searcher = new IndexSearcher( reader, searchExecutor );

            if( (flags & FLAG_CONTEXTS) != 0 ) {
                highlighter = new Highlighter(new SimpleHTMLFormatter("<span class=\"searchmatch\">", "</span>"),
                                              new SimpleHTMLEncoder(),
                                              new QueryScorer( luceneQuery ) );
            }

            final ScoreDoc[] hits = searcher.search(luceneQuery, MAX_SEARCH_HITS).scoreDocs;
            final AuthorizationManager mgr = m_engine.getManager( AuthorizationManager.class );

            list = new ArrayList<>(hits.length);
            for( final ScoreDoc hit : hits ) {
                final int docID = hit.doc;
                final Document doc = searcher.doc( docID );
                final String pageName = doc.get( LUCENE_ID );
                final Page page = m_engine.getManager( PageManager.class ).getPage( pageName, PageProvider.LATEST_VERSION );

                if( page != null ) {
                    final PagePermission pp = new PagePermission( page, PagePermission.VIEW_ACTION );
                    if( mgr.checkPermission( wikiContext.getWikiSession(), pp ) ) {
                        final int score = ( int )( hit.score * 100 );

                        // Get highlighted search contexts
                        final String text = doc.get( LUCENE_PAGE_CONTENTS );

                        String[] fragments = new String[ 0 ];
                        if( text != null && highlighter != null ) {
                            final TokenStream tokenStream = getLuceneAnalyzer()
                                    .tokenStream( LUCENE_PAGE_CONTENTS, new StringReader( text ) );
                            fragments = highlighter.getBestFragments( tokenStream, text, MAX_FRAGMENTS );
                        }

                        final SearchResult result = new SearchResultImpl( page, score, fragments );
                        list.add( result );
                    }
                } else {
                    log.error( "Lucene found a result page '" + pageName + "' that could not be loaded, removing from Lucene cache" );
                    pageRemoved( Wiki.contents().page( m_engine, pageName ) );
                }
            }
        } catch( final IOException e ) {
            log.error("Failed during lucene search",e);
        } catch( final ParseException e ) {
            log.info("Broken query; cannot parse query: " + query, e);
            throw new ProviderException( "You have entered a query Lucene cannot process [" + query + "]: " + e.getMessage() );
        } catch( final InvalidTokenOffsetsException e ) {
            log.error("Tokens are incompatible with provided text ",e);
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
    private static final class LuceneUpdater extends WikiBackgroundThread {
        static final int INDEX_DELAY    = 5;
        static final int INITIAL_DELAY = 60;
        private final LuceneSearchProvider m_provider;

        private final int m_initialDelay;

        private WatchDog m_watchdog;

        private LuceneUpdater( final Engine engine, final LuceneSearchProvider provider, final int initialDelay, final int indexDelay ) {
            super( engine, indexDelay );
            m_provider = provider;
            m_initialDelay = initialDelay;
            setName("JSPWiki Lucene Indexer");
        }

        @Override
        public void startupTask() throws Exception {
            m_watchdog = WatchDog.getCurrentWatchDog( getEngine() );

            // Sleep initially...
            try {
                Thread.sleep( m_initialDelay * 1000L );
            } catch( final InterruptedException e ) {
                throw new InternalWikiException("Interrupted while waiting to start.", e);
            }

            m_watchdog.enterState( "Full reindex" );
            // Reindex everything
            m_provider.doFullLuceneReindex();
            m_watchdog.exitState();
        }

        @Override
        public void backgroundTask() {
            m_watchdog.enterState("Emptying index queue", 60);

            synchronized ( m_provider.m_updates ) {
                while( m_provider.m_updates.size() > 0 ) {
                    final Object[] pair = m_provider.m_updates.remove(0);
                    final Page page = ( Page ) pair[0];
                    final String text = ( String ) pair[1];
                    m_provider.updateLuceneIndex(page, text);
                }
            }

            m_watchdog.exitState();
        }

    }

    // FIXME: This class is dumb; needs to have a better implementation
    private static class SearchResultImpl implements SearchResult {

        private final Page m_page;
        private final int      m_score;
        private final String[] m_contexts;

        public SearchResultImpl( final Page page, final int score, final String[] contexts ) {
            m_page = page;
            m_score = score;
            m_contexts = contexts != null ? contexts.clone() : null;
        }

        @Override
        public Page getPage()
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
