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
package org.apache.wiki.content;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.WikiPathResolver.PathRoot;
import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.event.*;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.LinkCollector;
import org.apache.wiki.util.TextUtil;

/**
 * <p>
 * Manages internal references between wikipages: which pages each page
 * <em>refers to</em> (outbound links), and how page is <em>referred by</em>
 * other pages (inbound). The pages that a given WikiPage refers to can be
 * obtained by calling {@link #getRefersTo(WikiPath)}. The pages that refer to
 * the page can be obtained via {@link #getReferredBy(WikiPath)}. In both
 * cases, the argument supplied to these methods is a WikiPath that denotes a
 * WikiPage.
 * </p>
 * <p>
 * ReferenceManager stores outbound links ("refers to") with the WikiPage that
 * originates the link, as the multi-valued property named
 * {@value #PROPERTY_REFERS_TO}}. Inbound links ("referred by") are stored in a
 * separate part of the content repository, in the special path
 * {@value #REFERENCES_ROOT}, where each node represents the page that is being
 * linked to (<em>i.e,</em> a page link named in wiki markup). The
 * multi-valued property named {@value #PROPERTY_REFERRED_BY} stores the source
 * of the link. The links that are stored are the UUIDs of the node, <em>not
 * the name or WikiPath</em>.
 * </p>
 * <p>
 * To ensure that ReferenceManager operates as efficiently as possible, page
 * references are recalculated only when changes to pages are made: that is,
 * when they are saved or deleted. ReferenceManager listens for the events
 * {@link ContentEvent#NODE_SAVED}, {@link ContentEvent#NODE_RENAMED} and
 * {@link ContentEvent#NODE_DELETE_REQUEST}. When one of these events is
 * detected, ReferenceManager updates the inbound and outbound link references as
 * required. The end result of these choices means that ReferenceManager is
 * relatively fast at reading references, but a bit slower at updating them.
 * Given the asymmetric nature of most wikis -- there are usually far more
 * readers than editors -- this is an appropriate way to do things.
 * </p>
 * <p>
 * In addition to keeping track of links between pages, ReferenceManager also
 * keeps two other lists up to date: the names of pages that are referenced by a
 * WikiPage but haven't been created yet (uncreated); and the UUIDs of pages
 * that have been created but not linked to by any other others (unreferenced).
 * These lists are updated whenever wiki pages are saved or deleted.
 * Uncreated and unreferenced page information is stored in JCR paths
 * {@link #NOT_CREATED} and {@link #NOT_REFERENCED}, respectively.
 * </p>
 * <p>
 * It is always possible that, despite JSPWiki's best efforts, that the link
 * table becomes corrupt. When this happens, the method {@link #rebuild()} will
 * erase and reconstruct all of the link references.
 * </p>
 */
public class ReferenceManager implements InternalModule, WikiEventListener
{

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 4L;

    private static final String PROPERTY_NOT_REFERENCED = "notReferenced";

    private static final String[] NO_VALUES = new String[0];

    protected static final Pattern LINK_PATTERN = Pattern
        .compile( "([\\[\\~]?)\\[([^\\|\\]]*)(\\|)?([^\\|\\]]*)(\\|)?([^\\|\\]]*)\\]" );

    protected static final String REFERENCES_ROOT = "/wiki:references";

    protected static final String PROPERTY_REFERRED_BY = "wiki:referredBy";

    protected static final String PROPERTY_REFERS_TO = "wiki:refersTo";

    protected static final String NOT_REFERENCED = REFERENCES_ROOT + "/wiki:notReferenced";

    protected static final String NOT_CREATED = REFERENCES_ROOT + "/wiki:notCreated";

    /**
     * Replaces camelcase links.
     */
    private static String renameCamelCaseLinks( String sourceText, String from, String to )
    {
        StringBuilder sb = new StringBuilder( sourceText.length() + 32 );

        Pattern linkPattern = Pattern.compile( "\\p{Lu}+\\p{Ll}+\\p{Lu}+[\\p{L}\\p{Digit}]*" );

        Matcher matcher = linkPattern.matcher( sourceText );

        int start = 0;

        while ( matcher.find( start ) )
        {
            String match = matcher.group();

            sb.append( sourceText.substring( start, matcher.start() ) );

            int lastOpenBrace = sourceText.lastIndexOf( '[', matcher.start() );
            int lastCloseBrace = sourceText.lastIndexOf( ']', matcher.start() );

            if( match.equals( from ) && lastCloseBrace >= lastOpenBrace )
            {
                sb.append( to );
            }
            else
            {
                sb.append( match );
            }

            start = matcher.end();
        }

        sb.append( sourceText.substring( start ) );

        return sb.toString();
    }

    /**
     * This method does a correct replacement of a single link, taking into
     * account anchors.
     */
    private static String renameLink( String original, String from, String newlink )
    {
        int hash = original.indexOf( '#' );
        int slash = original.indexOf( '/' );
        String reallink = original;
        String oldStyleRealLink;

        if( hash != -1 )
            reallink = original.substring( 0, hash );
        if( slash != -1 )
            reallink = original.substring( 0, slash );

        reallink = MarkupParser.cleanLink( reallink );
        oldStyleRealLink = MarkupParser.wikifyLink( reallink );

        //
        // Yes, these point to the same page.
        //
        if( reallink.equalsIgnoreCase( from ) || original.equalsIgnoreCase( from ) 
            || oldStyleRealLink.equalsIgnoreCase( from ) )
        {
            //
            // if the original contains blanks, then we should introduce a link,
            // for example: [My Page] => [My Page|My Renamed Page]
            int blank = reallink.indexOf( " " );

            if( blank != -1 )
            {
                return original + "|" + newlink;
            }

            return newlink + ((hash > 0) ? original.substring( hash ) : "");
        }

        return original;
    }

    /**
     * Renames a link in a given source text into a new name, and returns the
     * transformed text.
     * 
     * @param sourceText the source text
     * @param from the link to change, for example, "Main"
     * @param to the name to change the link to, for example "RenamedMain"
     * @return the transformed text
     */
    protected static String renameLinks( String sourceText, String from, String to )
    {
        StringBuilder sb = new StringBuilder( sourceText.length() + 32 );

        //
        // This monstrosity just looks for a JSPWiki link pattern. But it is
        // pretty
        // cool for a regexp, isn't it? If you can understand this in a single
        // reading,
        // you have way too much time in your hands.
        //
        Matcher matcher = LINK_PATTERN.matcher( sourceText );

        int start = 0;

        // System.out.println("====");
        // System.out.println("SRC="+sourceText.trim());
        while ( matcher.find( start ) )
        {
            char charBefore = (char) -1;

            if( matcher.start() > 0 )
                charBefore = sourceText.charAt( matcher.start() - 1 );

            if( matcher.group( 1 ).length() > 0 || charBefore == '~' || charBefore == '[' )
            {
                //
                // Found an escape character, so I am escaping.
                //
                sb.append( sourceText.substring( start, matcher.end() ) );
                start = matcher.end();
                continue;
            }

            String text = matcher.group( 2 );
            String link = matcher.group( 4 );
            String attr = matcher.group( 6 );

            /*
             * System.out.println("MATCH="+matcher.group(0));
             * System.out.println(" text="+text); System.out.println("
             * link="+link); System.out.println(" attr="+attr);
             */
            if( link.length() == 0 )
            {
                text = renameLink( text, from, to );
            }
            else
            {
                link = renameLink( link, from, to );

                //
                // A very simple substitution, but should work for quite a few
                // cases.
                //
                text = TextUtil.replaceString( text, from, to );
            }

            //
            // Construct the new string
            //
            sb.append( sourceText.substring( start, matcher.start() ) );
            sb.append( "[" + text );
            if( link.length() > 0 )
                sb.append( "|" + link );
            if( attr.length() > 0 )
                sb.append( "|" + attr );
            sb.append( "]" );

            start = matcher.end();
        }

        sb.append( sourceText.substring( start ) );

        return sb.toString();
    }

    /** The WikiEngine that owns this object. */
    private WikiEngine m_engine;

    private ContentManager m_cm;
    
    private WikiPathResolver m_pathCache;

    private boolean m_camelCase = false;

    private boolean m_matchEnglishPlurals = false;

    /**
     * Default constructor that creates a new ReferenceManager. Callers must
     * should call {@link #initialize(WikiEngine, Properties)} to activate the
     * ReferenceManager.
     */
    public ReferenceManager()
    {
        // Do nothing, really.
        super();
    }

    /**
     * {@inheritDoc} After the page has been saved, updates the reference lists.
     * Modifications to the underlying JCR nodes are saved by the current JCR
     * {@link Session}.
     */
    public void actionPerformed( WikiEvent event )
    {
        if( !(event instanceof WikiPageEvent) )
        {
            return;
        }

        // We perform an action if the WikiPage was previously saved in the JCR
        WikiPath path = ((WikiPageEvent) event).getPath();
        if( !m_engine.pageExists( path.toString() ) )
        {
            return;
        }

        try
        {
            Session session = m_cm.getCurrentSession();
            JCRWikiPage page = m_cm.getPage( path );
            String uuid = page.getJCRNode().getUUID();

            switch( event.getType() )
            {
                // ========= page saved ==============================

                // If page was saved, update all references
                case (ContentEvent.NODE_SAVED ): {

                    // Remove refersTo/referencedBy links
                    String[] destinations = getFromProperty( page.getJCRNode().getPath(), PROPERTY_REFERS_TO );
                    for ( String destination : destinations )
                    {
                        removeReferral( uuid, destination );
                    }
                    
                    // For current version, set refersTo/referencedBy links
                    List<String> toUuids = extractLinks( page );
                    for ( String destination : toUuids )
                    {
                        addReferral( uuid, destination );
                        session.save();
                    }
                    
                    // If no refs to this page, make it Unreferenced
                    String[] fromUuids = getFromProperty( page.getJCRNode().getPath(), PROPERTY_REFERRED_BY );
                    if ( fromUuids.length == 0 )
                    {
                        addToProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, uuid );
                    }

                    session.save();
                    break;
                }

                // ========= page delete request =====================

                // Before deleting pages, remove all references to it/from it
                case (ContentEvent.NODE_DELETE_REQUEST ): {

                    // Get referral destinations, and remove refersTo/referencedBy links
                    Node node = page.getJCRNode();
                    String[] destinations = getFromProperty( node.getPath(), PROPERTY_REFERS_TO );
                    for ( String destination : destinations )
                    {
                        removeReferral( uuid, destination );
                    }

                    // Always remove old page from Unreferenced list
                    removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, uuid );
                    
                    session.save();
                    break;
                }

                // ========= page renamed ============================

                case (ContentEvent.NODE_RENAMED ): {

                    // Change the wiki markup in every page that refers to this one
                    WikiPath oldPath = (WikiPath) ((WikiPageEvent) event).getArgs()[0];
                    changeWikiReferences( page, oldPath.getPath() );
                    session.save();
                    break;
                }
            }
        }
        catch( PageNotFoundException e )
        {
            e.printStackTrace();
        }
        catch( ProviderException e )
        {
            e.printStackTrace();
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
        }
    }

    /**
     * Replaces all references to a WikiPage from its previous WikiPath value, recursively.
     * @param page the WikiPage whose referrers should be changed
     * @param oldPath the WikiPath that the WikiPage was previously located at
     * @throws ProviderException
     * @throws RepositoryException
     */
    private void changeWikiReferences( JCRWikiPage page, String oldPath ) throws ProviderException, RepositoryException
    {
        String newPath = page.getPath().getPath();
        Node node = page.getJCRNode();
        
        // Get referrers for this Node
        String[] destinations = getFromProperty( node.getPath(), PROPERTY_REFERRED_BY );
        
        // In every referrer, replace all references to the old path with the new one
        for( String destination : destinations )
        {
            try
            {
                WikiPath referrer = m_pathCache.getByUUID( destination );
                WikiPage p = m_cm.getPage( referrer );

                String sourceText = p.getContentAsString();
                String newText = renameLinks( sourceText, oldPath, newPath );

                if( m_camelCase )
                    newText = renameCamelCaseLinks( newText, oldPath, newPath );

                if( !sourceText.equals( newText ) )
                {
                    p.setAttribute( WikiPage.CHANGENOTE, oldPath + " ==> " + newPath );
                    p.setContent( newText );
                    // TODO: do we want to set the author here? (We used to...)
                }
            }
            catch( PageNotFoundException e )
            {
                // Just continue
            }
        }
        
        // Process any sub-pages or attachments
        if ( !page.isAttachment() )
        {
            NodeIterator children = node.getNodes();
            while ( children.hasNext() )
            {
                node = children.nextNode();
                WikiPathResolver cache = WikiPathResolver.getInstance( m_cm );
                WikiPath path = cache.getWikiPath( node.getPath(), PathRoot.PAGES );
                page = new JCRWikiPage( m_engine, path, node );
                String oldChildPath = oldPath + "/" + path.getName();
                changeWikiReferences( page, oldChildPath );
            }
        }
    }
    
    /**
     * Returns a list of all pages that the ReferenceManager knows about. This
     * should be roughly equivalent to PageManager.getAllPages(), but without
     * the potential disk access overhead. Note that this method is not
     * guaranteed to return a Set of really all pages (especially during
     * startup), but it is very fast.
     * 
     * @return A Set of all defined page names that ReferenceManager knows
     *         about.
     * @throws ProviderException
     * @since 2.3.24
     * @deprecated
     */
    public Set<String> findCreated() throws ProviderException
    {
        Collection<WikiPage> c = m_engine.getContentManager().getAllPages( null );
        Set<String> results = new TreeSet<String>();
        for( WikiPage link : c )
        {
            results.add( link.getPath().toString() );
        }
        return results;
    }

    /**
     * Returns all pages that this page refers to. You can use this as a quick
     * way of getting the links from a page, but note that it does not link any
     * InterWiki, image, or external links. It does contain attachments, though.
     * <p>
     * The Collection returned is immutable, so you cannot change it. It does
     * reflect the current status and thus is a live object. So, if you are
     * using any kind of an iterator on it, be prepared for
     * ConcurrentModificationExceptions.
     * <p>
     * The returned value is a Collection, because a page may refer to another
     * page multiple times.
     * 
     * @param pageName Page name to query
     * @return A Collection of Strings containing the names of the pages that
     *         this page refers to. May return null, if the page does not exist
     *         or has not been indexed yet.
     * @throws PageNotFoundException
     * @throws ProviderException
     * @since 2.2.33
     * @deprecated Use {@link #getRefersTo(WikiPath)} instead
     */
    public List<String> findRefersTo( String pageName ) throws ProviderException
    {
        List<WikiPath> links = getRefersTo( WikiPath.valueOf( pageName ) );
        List<String> results = new ArrayList<String>();
        for( WikiPath link : links )
        {
            results.add( link.toString() );
        }
        return results;
    }

    /**
     * <p>
     * Returns a list of WikiPaths representing pages that are referenced in wiki
     * markup, but have not yet been created. Each non-existent page name is
     * shown only once - we don't return information on who referred to it.
     * </p>
     * 
     * @return A list of Strings, where each names a page that hasn't been
     *         created
     */
    public List<WikiPath> findUncreated() throws RepositoryException
    {
        Session session = m_cm.getCurrentSession();
        QueryManager mgr = session.getWorkspace().getQueryManager();
        String uncreated = "/jcr:root"+WikiPathResolver.PathRoot.NOT_CREATED.path()+"/*";
        Query q = mgr.createQuery( uncreated, Query.XPATH );
        QueryResult qr = q.execute();
        List<WikiPath> links = new ArrayList<WikiPath>();
        for( NodeIterator ni = qr.getNodes(); ni.hasNext(); )
        {
            Node nd = ni.nextNode();
            if( nd.getDepth() > 3 )
            {
                String uuid = nd.getUUID();
                links.add( m_pathCache.getByUUID( uuid ) );
            }
        }
        return links;
    }

    /**
     * <p>
     * Returns a list of Strings representing pages that have been created, but
     * not yet referenced in wiki markup by any other pages. Each not-referenced
     * page name is shown only once.
     * </p>
     * 
     * @return A list of Strings, where each names a page that hasn't been
     *         created
     */
    public List<WikiPath> findUnreferenced() throws RepositoryException
    {
        String[] uuids = getFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED );
        List<WikiPath> links = new ArrayList<WikiPath>();
        for( String uuid : uuids )
        {
            links.add( m_pathCache.getByUUID( uuid ) );
        }
        return links;
    }

    /**
     * <p>
     * Returns all pages that refers to a given page. You can use this as a
     * quick way of getting the inbound links to a page from other pages. The
     * page being looked up need not exist. The requested page is <code>not</code>
     * resolved in any way, so if the page is not found as specified exactly by the path,
     * a zero-length list will be returned.
     * </p>
     * 
     * @param destination the page to look up
     * @return the list of pages that link to this page
     * @throws ProviderException If something goes wrong
     * @since 3.0
     */
    public List<WikiPath> getReferredBy( WikiPath destination ) throws ProviderException
    {
        if ( destination == null )
        {
            throw new IllegalArgumentException( "Destination cannot be null!" );
        }
        
        try
        {
            // Get 'referred-by' links for this Node
            Node node = m_cm.getCurrentSession().getNodeByUUID( getSafeNodeByUUID( destination ) );
            String[] uuids = getFromProperty( node.getPath(), PROPERTY_REFERRED_BY );
            List<WikiPath> referrers = new ArrayList<WikiPath>();
            for( String uuid : uuids )
            {
                referrers.add( m_pathCache.getByUUID( uuid ) );
            }

            // Get 'referred-by' links for any child Nodes, recursively
            try
            {
                NodeIterator children = node.getNodes();
                while( children.hasNext() )
                {
                    Node child = children.nextNode();
                    if ( child.getPrimaryNodeType().isMixin() )
                    {
                        String uuid = child.getUUID();
                        WikiPath childPath = m_pathCache.getByUUID( uuid );
                        referrers.addAll( getReferredBy( childPath ) );
                    }
                }
            }
            catch ( PathNotFoundException e )
            {
                // No worries
            }
            
            return referrers;
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new ProviderException( "Could not set 'referredBy' property for " + destination.toString(), e );
        }
    }

    /**
     * <p>
     * Returns all destination pages that a page refers to. You can use this as
     * a quick way of getting the outbound links from a page to the destination
     * pages its markup refers to, but note that it does not link any InterWiki,
     * image, or external links. It does contain attachments, though. Multiple
     * links to the same page are never returned; they will always be
     * de-duplicated. The specified page <code>source</code> is not resolved
     * in any way, so if the page is not found as specified exactly by the path,
     * a zero-length list is returned.
     * </p>
     * 
     * @param source the page to look up
     * @return the list of pages this page links to
     * @throws ProviderException
     */
    public List<WikiPath> getRefersTo( WikiPath source ) throws ProviderException
    {
        if ( source == null )
        {
            throw new IllegalArgumentException( "Source cannot be null!" );
        }

        try
        {
            String jcrPath = WikiPathResolver.getJCRPath( source, PathRoot.PAGES );
            String[] uuids = getFromProperty( jcrPath, PROPERTY_REFERS_TO );
            List<WikiPath> refersTo = new ArrayList<WikiPath>();
            for( String uuid : uuids )
            {
                refersTo.add( m_pathCache.getByUUID( uuid ) );
            }
            return refersTo;
        }
        catch( RepositoryException e )
        {
            e.printStackTrace();
            throw new ProviderException( "Could not get 'refersTo' property for " + source.toString(), e );
        }
    }

    /**
     * Initializes the reference manager. Scans all existing WikiPages for
     * internal links and adds them to the ReferenceManager object.
     * 
     * @param engine The WikiEngine to which this is managing references to.
     * @param props the properties for initializing the WikiEngine
     * @throws WikiException If the reference manager initialization fails.
     */
    public void initialize( WikiEngine engine, Properties props ) throws WikiException
    {
        m_engine = engine;
        m_cm = engine.getContentManager();
        m_pathCache = WikiPathResolver.getInstance( m_cm );

        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(), WikiEngine.PROP_MATCHPLURALS,
                                                             m_matchEnglishPlurals );

        m_camelCase = TextUtil.getBooleanProperty( m_engine.getWikiProperties(), JSPWikiMarkupParser.PROP_CAMELCASELINKS, false );
        
        // Do we need to re-build the references database?
        try
        {
            Node root = m_cm.getCurrentSession().getRootNode();
            if ( !root.hasNode( REFERENCES_ROOT ) || !root.hasNode( NOT_REFERENCED ) )
            {
                rebuild();
            }
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Failed to initialize repository contents", e );
        }

        // Make sure we catch any page add/save/rename events
        WikiEventManager.addWikiEventListener( engine.getContentManager(), this );

        m_cm.release();
    }

    /**
     * Rebuilds the internal references database by parsing every wiki page.
     * Verifies that the JCR nodes for storing references exist, and creates
     * then if they do not. If any nodes are added, they are saved using the
     * current JCR {@link Session} before returning.
     * 
     * @throws RepositoryException
     * @throws LoginException
     */
    public void rebuild() throws RepositoryException
    {
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();

        // Remove all of the references subtrees
        if( s.getRootNode().hasNode( REFERENCES_ROOT ) )
        {
            Node nd = s.getRootNode().getNode( REFERENCES_ROOT );
            nd.remove();
        }

        // Re-add the references subtree
        if( !s.getRootNode().hasNode( REFERENCES_ROOT ) )
        {
            s.getRootNode().addNode( REFERENCES_ROOT );
        }
        if( !s.getRootNode().hasNode( NOT_REFERENCED ) )
        {
            s.getRootNode().addNode( NOT_REFERENCED );
        }
        if( !s.getRootNode().hasNode( NOT_CREATED ) )
        {
            s.getRootNode().addNode( NOT_CREATED );
        }
        s.save();

        // TODO: we should actually parse the pages
    }
    
    /**
     * Returns a resolved WikiPath, taking into account plural variants as
     * determined by {@link WikiEngine#getFinalPageName(WikiPath)}. For
     * example, if page <code>Foobar</code> exists, and the path supplied to
     * this method is <code>Foobars</code>, then <code>Foobar</code> is
     * returned. If no variant for the supplied page exists, or if a page exists
     * whose name exactly matches it, then the supplied name is returned. Thus,
     * if page <code>Foobars</code> actually exists, that path will be
     * returned.
     * 
     * @param path the path of the page to look for
     * @return the resolved path for the page
     * @throws ProviderException if
     *             {@link WikiEngine#getFinalPageName(WikiPath)} throws an
     *             exception
     */
    private WikiPath resolvePage( WikiPath path ) throws ProviderException
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "Path cannot be null!" );
        }
        
        WikiPath finalPath = m_engine.getFinalPageName( path );
        return finalPath == null ? path : finalPath;
    }

    /**
     * Adds a referrral link from one wiki page to another. This adds the UUID of
     * {@code to} to the {@code refersTo} attribute of {@code from}. It
     * adds a reciprocal reverse link to {@code to} by adding to its 
     * {@code referredBy} attribute the UUID of {@code from}.
     * @param from the UUID of the page that refers to another
     * @param to the UIUD that {@code from} refers to
     * @throws RepositoryException 
     * @throws LoginException 
     * @throws RepositoryException if the the JCR cannot obtain a Session,
     * retrieve either Node from the repository, or add required
     * properties to them
     */
    protected void addReferral( String from, String to ) throws RepositoryException
    {
        Session session = m_cm.getCurrentSession();
        Node fromNode = session.getNodeByUUID( from );
        Node toNode = session.getNodeByUUID( to );
        addToProperty( fromNode.getPath(), PROPERTY_REFERS_TO, to );
        addToProperty( toNode.getPath(), PROPERTY_REFERRED_BY, from );
        removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, to );
    }
    
    /**
     * Removes a referral link from one wiki page to another.
     * This removes the UUID of {@code to} from the {@code refersTo}
     * attribute of {@code from}. It removes a reciprocal reverse link
     * from {@code to} by removing to its {@code referredBy} attribute
     * the UUID of {@code from}.
     * @param from the UUID of the page that refers to another
     * @param to the UIUD that {@code from} refers to
     * @throws RepositoryException if the the JCR cannot obtain a Session,
     * retrieve either Node from the repository, or add required
     * properties to them
     */
    protected void removeReferral( String from, String to ) throws RepositoryException
    {
        Session session = m_cm.getCurrentSession();
        Node fromNode = session.getNodeByUUID( from );
        removeFromProperty( fromNode.getPath(), PROPERTY_REFERS_TO, to );
        Node toNode;
        try
        {
            // Remove the incoming referrer from the target node
            toNode = session.getNodeByUUID( to );
            removeFromProperty( toNode.getPath(), PROPERTY_REFERRED_BY, from );
            
            // If no other nodes refer to the target, it becomes Unreferenced
            String[] referrals = getFromProperty( toNode.getPath(), PROPERTY_REFERRED_BY );
            if ( referrals.length == 0 )
            {
                addToProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, to );
            }
        }
        catch ( ItemNotFoundException e )
        {
            // If 'toNode' doesn't exist, that's probably a bug, but one we can work around.
        }
    }

    /**
     * Adds a single String value to a given JCR node and
     * {@link javax.jcr.Property}. If the JCR node does not exist,
     * is is created with all path components converted to lower case.
     * The property is assumed to return an array of{@link javax.jcr.Value} objects. Modifications to the underlying
     * JCR nodes are <em>not</em> saved by the current JCR {@link Session}.
     * If the value already exists in the property, it is not added again.
     * 
     * @param jcrPath the JCR path to the node. Note that all WikiPath-style components
     * <em>must</em> be in lower-case.
     * @param property the property to add to
     * @param newValue the value to add
     */
    protected void addToProperty( String jcrPath, String property, String newValue ) throws RepositoryException
    {
        if ( jcrPath == null || property == null || newValue == null )
        {
            throw new IllegalArgumentException( "jcrNode, property and newValue cannot be null!" );
        }
        
        // Retrieve (or create) the destination node for the page
        Session session = m_cm.getCurrentSession();
        Node node = null;
        try
        {
            node = (Node) session.getItem( jcrPath );
        }
        catch( PathNotFoundException e )
        {
            String[] components = StringUtils.split( jcrPath, "/" );
            node = session.getRootNode();
            for( int i = 0; i < components.length; i++ )
            {
                Node parent = node;
                try
                {
                    node = node.getNode( components[i] );
                }
                catch ( PathNotFoundException e2 )
                {
                    node = node.addNode( components[i] );
                    parent.save();
                }
            }
        }

        // Retrieve the property; add value to the end
        List<String> newValues = new ArrayList<String>();
        try
        {
            Property p = node.getProperty( property );
            boolean notFound = true;
            Value[] values = p.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                String valueString = values[i].getString();
                if( valueString != null && valueString.length() > 0 )
                {
                    newValues.add( valueString );
                    if( newValue.equals( valueString ) )
                    {
                        notFound = false;
                    }
                }
            }
            if( notFound )
            {
                newValues.add( newValue );
            }
            
            node.setProperty( property, newValues.toArray( new String[newValues.size()] ) );
        }
        catch( PathNotFoundException e )
        {
            node.setProperty( property, new String[] { newValue } );
        }
        node.save();
    }

    /**
     * Reads a WikiPage full of data from a String and returns all links
     * internal to this Wiki as a list of UUIDs. Links are "resolved"; that is,
     * page resolution is performed to ensure that plural references resolve to
     * the correct page. The links returned by this method will not contain any
     * duplicates, even if the original page markup linked to the same page more
     * than once.
     * 
     * @param page the WikiPage to scan
     * @return a Collection of Strings
     * @throws ProviderException if the page contents cannot be retrieved, or if
     *             MarkupParser canot parse the document
     */
    protected List<String> extractLinks( JCRWikiPage page ) throws PageNotFoundException, ProviderException, RepositoryException
    {
        if ( page == null )
        {
            throw new IllegalArgumentException( "Path cannot be null!" );
        }
        
        // Set up a streamlined parser to collect links
        LinkCollector pageLinks = new LinkCollector();
        LinkCollector attachmentLinks = new LinkCollector();
        String pagedata = page.getContentAsString();
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        MarkupParser mp = m_engine.getRenderingManager().getParser( context, pagedata );
        mp.addLocalLinkHook( pageLinks );
        mp.addAttachmentLinkHook( attachmentLinks );
        mp.disableAccessRules();

        // Parse the page, and collect the links
        try
        {
            mp.parse();
        }
        catch( IOException e )
        {
            // Rethrow any parsing exceptions
            throw new ProviderException( "Could not parse the document.", e );
        }

        // Return a WikiPath for each link
        ArrayList<String> links = new ArrayList<String>();
        for( String s : pageLinks.getLinks() )
        {
            WikiPath finalPath = resolvePage( WikiPath.valueOf( s ) );
            String uuid = getSafeNodeByUUID( finalPath );
            if( !links.contains( uuid ) )
            {
                links.add( uuid );
            }
        }
        for( String s : attachmentLinks.getLinks() )
        {
            WikiPath finalPath = resolvePage( WikiPath.valueOf( s ) );
            String uuid = getSafeNodeByUUID( finalPath );
            if( !links.contains( uuid ) )
            {
                links.add( uuid );
            }
        }

        return links;
    }
    
    /**
     * Retrieves the UUID of the JCR node that represents a page or
     * or attachment WikiPath. The node does not have to exist; if it doesn't,
     * a new node (and its parents, if necessary) will be created in the
     * "uncreated" tree branch.
     * @param path the WikiPath, which is assumed to be in the correct case
     * @return the UUID of the newly created node
     * @throws PathNotFoundException
     * @throws RepositoryException
     */
    protected String getSafeNodeByUUID( WikiPath path ) throws PathNotFoundException, RepositoryException
    {
        // See if the "uncreated" node has already been created
        try
        {
            return WikiPathResolver.getInstance( m_cm ).getUUID( path );
        }
        catch ( ItemNotFoundException e )
        {
            // Not found! Time to create a new node
        }
        
        // See if the "uncreated" node exists already
        Node nd = null;
        String jcrPath = WikiPathResolver.getJCRPath( path, PathRoot.NOT_CREATED );
        Session session = m_cm.getCurrentSession();
        try
        {
            nd = session.getRootNode().getNode( jcrPath );
        }
        catch( PathNotFoundException e )
        {
        }

        // Create the "uncreated" node and all its parents
        if( nd == null )
        {
            Node currentNode = session.getRootNode().getNode( ReferenceManager.NOT_CREATED );
            String space = path.getSpace();

            // Create the space node if needed
            if( !currentNode.hasNode( space.toLowerCase() ) )
            {
                nd = currentNode.addNode( space.toLowerCase() );
                nd.addMixin( "mix:referenceable" );
                nd.setProperty( JCRWikiPage.CONTENT_TYPE, ContentManager.JSPWIKI_CONTENT_TYPE );
                nd.setProperty( JCRWikiPage.ATTR_TITLE, space );
            }
            currentNode = currentNode.getNode( space.toLowerCase() );

            // Create all of the child path nodes if needed
            String[] pathComponents = path.getPath().split( "/" );
            for( String pathComponent : pathComponents )
            {
                if( !currentNode.hasNode( pathComponent.toLowerCase() ) )
                {
                    nd = currentNode.addNode( pathComponent.toLowerCase() );
                    nd.addMixin( "mix:referenceable" );
                    nd.setProperty( JCRWikiPage.CONTENT_TYPE, ContentManager.JSPWIKI_CONTENT_TYPE );
                    nd.setProperty( JCRWikiPage.ATTR_TITLE, pathComponent );
                }
                currentNode = currentNode.getNode( pathComponent.toLowerCase() );
            }
            session.save();
            nd = session.getRootNode().getNode( jcrPath );
        }
        WikiPathResolver cache = WikiPathResolver.getInstance( m_cm );
        cache.add( path, nd.getUUID() );
        return nd.getUUID();
    }

    /**
     * Retrieves an array of Strings stored at a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. If the node does not exist, a
     * zero-length array is returned.
     * 
     * @param jcrPath the JCR path to the node. Note that the path is case-sensitive.
     * @param property the property to read
     * @throws RepositoryException
     */
    protected String[] getFromProperty( String jcrPath, String property ) throws RepositoryException
    {
        if ( jcrPath == null || property == null )
        {
            throw new IllegalArgumentException( "jcrNode and property cannot be null!" );
        }
        
        // Retrieve the destination node for the page
        ContentManager cm = m_engine.getContentManager();
        Node node = null;
        try
        {
            node = (Node) cm.getCurrentSession().getItem( jcrPath );
        }
        catch( PathNotFoundException e )
        {
            return NO_VALUES;
        }

        // Retrieve the property; re-pack value array into String array
        String[] stringValues = NO_VALUES;
        try
        {
            Property p = node.getProperty( property );
            Value[] values = p.getValues();
            stringValues = new String[values.length];
            for( int i = 0; i < values.length; i++ )
            {
                stringValues[i] = values[i].getString();
            }
        }
        catch( PathNotFoundException e )
        {
            return NO_VALUES;
        }

        return stringValues;
    }

    /**
     * Removes a String value from a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. The node is <em>not</em> created if it does not
     * exist because by definition the property is already removed! Modifications to the
     * underlying JCR Node are saved by the current JCR {@link Session}.
     * 
     * @param jcrPath the JCR path to the node. Note that the path is case-sensitive.
     * @param property the property to add to
     * @param value the value to remove. All occurrences of the matching value
     *            will be removed.
     */
    protected void removeFromProperty( String jcrPath, String property, String value ) throws RepositoryException
    {
        if ( jcrPath == null || property == null || value == null )
        {
            throw new IllegalArgumentException( "jcrNode, property and value cannot be null!" );
        }
        
        // Retrieve (or create) the destination node for the page
        ContentManager cm = m_engine.getContentManager();
        Node node = null;
        try
        {
            node = (Node) cm.getCurrentSession().getItem( jcrPath );
        }
        catch( PathNotFoundException e )
        {
            // If parent node doesn't exist, it's (by definition) already removed
            return;
        }

        // Retrieve the property; remove all instances of value
        List<String> newValues = new ArrayList<String>();
        try
        {
            Property p = node.getProperty( property );
            Value[] values = p.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                String valueString = values[i].getString();
                if( valueString != null && valueString.length() > 0 && !value.equals( valueString ) )
                {
                    newValues.add( valueString );
                }
            }
            p.remove();
            p.save(); // Needed to persist the removal of the original values
        }
        catch( PathNotFoundException e )
        {
            // No worries
        }

        // Set/remove the property
        if( newValues.size() > 0 )
        {
            node.setProperty( property, newValues.toArray( new String[newValues.size()] ) );
        }
        node.save();
    }
}
