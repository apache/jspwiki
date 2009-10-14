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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
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
 * of the link.
 * </p>
 * <p>
 * To ensure that ReferenceManager operates as efficiently as possible, page
 * references are recalculated only when changes to pages are made: that is,
 * when they are saved, renamed or deleted. ReferenceManager listens for the
 * events {@link ContentEvent#NODE_SAVED}, {@link ContentEvent#NODE_RENAMED}
 * and {@link ContentEvent#NODE_DELETE_REQUEST}. When one of these events is
 * detected, ReferenceManager updates the inbound and oubound link references as
 * required. The end result of these choices means that ReferenceManager is
 * relatively fast at reading references, but a bit slower at updating them.
 * Given the asymmetric nature of most wikis -- there are usually far more
 * readers than editors -- this is an appropriate way to do things.
 * </p>
 * <p>
 * In addition to keeping track of links between pages, ReferenceManager also
 * keeps two other lists up to date: the names of pages that are referenced by a
 * WikiPage but haven't been created yet (uncreated); and the names of pages
 * that have been created but not linked to by any other others (unreferenced).
 * These lists are updated whenever wiki pages are saved, renamed or deleted.
 * The JCR paths for uncreated and unreferenced pages are in
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

    private static final String PROPERTY_NOT_CREATED = "notCreated";

    private static final String PROPERTY_NOT_REFERENCED = "notReferenced";

    private static final String[] NO_VALUES = new String[0];

    private static final Pattern LINK_PATTERN = Pattern
        .compile( "([\\[\\~]?)\\[([^\\|\\]]*)(\\|)?([^\\|\\]]*)(\\|)?([^\\|\\]]*)\\]" );

    protected static final String REFERENCES_ROOT = "/wiki:references";

    protected static final String PROPERTY_REFERRED_BY = "wiki:referredBy";

    protected static final String PROPERTY_REFERS_TO = "wiki:refersTo";

    /**
     * JCR path path prefix for inbound "referredby" links, used by
     * {@link #addReferredBy(WikiPath, WikiPath)}. Absolute path whose prefix is
     * {@link #REFERENCES_ROOT}.
     */
    protected static final String REFERRED_BY = REFERENCES_ROOT + "/wiki:referrers";

    protected static final String NOT_REFERENCED = REFERENCES_ROOT + "/wiki:notReferenced";

    protected static final String NOT_CREATED = REFERENCES_ROOT + "/wiki:notCreated";

    protected static final String[] REFERENCES_METADATA = { REFERRED_BY, NOT_REFERENCED, NOT_CREATED };

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
     * account anchors and attachments.
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

        // WikiPage realPage = context.getEngine().getPage( reallink );
        // WikiPage p2 = context.getEngine().getPage( from );

        // System.out.println(" "+reallink+" :: "+ from);
        // System.out.println(" "+p+" :: "+p2);

        //
        // Yes, these point to the same page.
        //
        if( reallink.equals( from ) || original.equals( from ) || oldStyleRealLink.equals( from ) )
        {
            //
            // if the original contains blanks, then we should introduce a link,
            // for example: [My Page] => [My Page|My Renamed Page]
            int blank = reallink.indexOf( " " );

            if( blank != -1 )
            {
                return original + "|" + newlink;
            }

            return newlink + ((hash > 0) ? original.substring( hash ) : "") + ((slash > 0) ? original.substring( slash ) : "");
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

        String pageName = ((WikiPageEvent) event).getPageName();
        WikiPath path = pageName == null ? null : WikiPath.valueOf( pageName );
        if( !isWikiPage( path ) )
        {
            return;
        }

        try
        {
            switch( event.getType() )
            {
                // ========= page saved ==============================

                // If page was saved, update all references
                case (ContentEvent.NODE_SAVED ): {
                    path = resolvePage( path );

                    // Get new linked pages, and set refersTo/referencedBy links
                    List<WikiPath> referenced = extractLinks( path );
                    setLinks( path, referenced );

                    m_cm.getCurrentSession().save();
                    break;
                }

                    // ========= page deleted ==============================

                    // If page was deleted, remove all references to it/from it
                case (ContentEvent.NODE_DELETE_REQUEST ): {
                    path = resolvePage( path );

                    // Remove the links from deleted page to its referenced
                    // pages
                    removeLinks( path );

                    m_cm.getCurrentSession().save();
                    break;
                }

                    // ========= page renamed ==============================

                case (ContentEvent.NODE_RENAMED ): {
                    // Update references from this page
                    WikiPath toPage = path;
                    WikiPath fromPage = WikiPath.valueOf( (String) ((WikiPageEvent) event).getArgs()[0] );
                    Boolean changeReferrers = (Boolean) ((WikiPageEvent) event).getArgs()[1];
                    removeLinks( fromPage );
                    setLinks( toPage, extractLinks( toPage ) );

                    // Change references to the old page; use the new name
                    if( changeReferrers )
                    {
                        renameLinksTo( fromPage, toPage );
                    }

                    m_cm.getCurrentSession().save();
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
     * Returns a list of Strings representing pages that are referenced in wiki
     * markup, but have not yet been created. Each non-existent page name is
     * shown only once - we don't return information on who referred to it.
     * </p>
     * 
     * @return A list of Strings, where each names a page that hasn't been
     *         created
     */
    public List<WikiPath> findUncreated() throws RepositoryException
    {
        String[] linkStrings = getFromProperty( NOT_CREATED, PROPERTY_NOT_CREATED );
        List<WikiPath> links = new ArrayList<WikiPath>();
        for( String link : linkStrings )
        {
            links.add( WikiPath.valueOf( link  ) );
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
        String[] linkStrings = getFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED );
        List<WikiPath> links = new ArrayList<WikiPath>();
        for( String link : linkStrings )
        {
            links.add( WikiPath.valueOf( link ) );
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
            String jcrPath = getReferencedByJCRNode( destination );
            String[] links = getFromProperty( jcrPath, PROPERTY_REFERRED_BY );
            List<WikiPath> referrers = new ArrayList<WikiPath>();
            for( String link : links )
            {
                referrers.add( WikiPath.valueOf( link ) );
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
            String jcrPath = ContentManager.getJCRPath( source );
            String[] links = getFromProperty( jcrPath, PROPERTY_REFERS_TO );
            List<WikiPath> refersTo = new ArrayList<WikiPath>();
            for( String link : links )
            {
                refersTo.add( WikiPath.valueOf( link ) );
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

        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(), WikiEngine.PROP_MATCHPLURALS,
                                                             m_matchEnglishPlurals );

        m_camelCase = TextUtil.getBooleanProperty( m_engine.getWikiProperties(), JSPWikiMarkupParser.PROP_CAMELCASELINKS, false );
        try
        {
            initReferenceMetadata();
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
     * 
     * @throws RepositoryException
     * @throws LoginException
     */
    public void rebuild() throws RepositoryException
    {
        // Remove all of the 'referencedBy' inbound links
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();

        if( s.getRootNode().hasNode( REFERENCES_ROOT ) )
        {
            for( String ref : REFERENCES_METADATA )
            {
                if( s.getRootNode().hasNode( ref ) )
                {
                    Node nd = s.getRootNode().getNode( ref );
                    nd.remove();
                }
            }
            s.getRootNode().getNode( REFERENCES_ROOT ).remove();
        }
        s.save();

        initReferenceMetadata();

        // TODO: we should actually parse the pages
    }

    /**
     * Builds and returns the path used to store the ReferredBy data
     */
    private String getReferencedByJCRNode( WikiPath path )
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "Path cannot be null!" );
        }
        return REFERRED_BY + "/" + path.getSpace() + "/" + path.getPath();
    }

    /**
     * Verifies that the JCR nodes for storing references exist, and creates
     * then if they do not. If any nodes are added, they are saved using the
     * current JCR {@link Session} before returning.
     */
    private void initReferenceMetadata() throws RepositoryException
    {
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();
        if( !s.getRootNode().hasNode( REFERENCES_ROOT ) )
        {
            s.getRootNode().addNode( REFERENCES_ROOT );
        }
        for( String ref : REFERENCES_METADATA )
        {
            if( !s.getRootNode().hasNode( ref ) )
            {
                s.getRootNode().addNode( ref );
            }
        }
        s.save();
    }

    /**
     * This method finds all the pages which refer to <code>oldPath</code> and
     * change their references to <code>newPath</code>.
     * 
     * @param oldPath The old page
     * @param newPath The new page
     */
    private void renameLinksTo( WikiPath oldPath, WikiPath newPath ) throws ProviderException, RepositoryException
    {
        if ( oldPath == null || newPath == null )
        {
            throw new IllegalArgumentException( "oldPath and newPath cannot be null!" );
        }
        
        List<WikiPath> referrers = getReferredBy( oldPath );
        if( referrers.isEmpty() )
            return; // No referrers

        for( WikiPath path : referrers )
        {
            // In case the page was just changed from under us, let's do this
            // small kludge.
            if( path.equals( oldPath.getPath() ) )
            {
                path = newPath;
            }

            try
            {
                ContentManager cm = m_engine.getContentManager();
                WikiPage p = cm.getPage( path );

                String sourceText = m_engine.getPureText( p );

                String newText = renameLinks( sourceText, oldPath.toString(), newPath.toString() );

                if( m_camelCase )
                    newText = renameCamelCaseLinks( newText, oldPath.toString(), newPath.toString() );

                if( !sourceText.equals( newText ) )
                {
                    p.setAttribute( WikiPage.CHANGENOTE, oldPath.toString() + " ==> " + newPath.toString() );
                    p.setContent( newText );
                    // TODO: do we want to set the author here? (We used to...)
                    cm.save( p );
                    setLinks( path, extractLinks( newPath ) );
                    cm.getCurrentSession().save();
                }
            }
            catch( PageNotFoundException e )
            {
                // Just continue
            }
        }
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
     * Adds a "referredBy" inbound link to a page from a source page that links
     * to it. That is, for the destination page, a "referredBy" entry is made
     * that contains the name of the source page. Neither the source or
     * destination pages need exist. Modifications to the underlying JCR node
     * that contains the link are saved by the current JCR {@link Session}.
     * 
     * @param page the page that is the destination for the link
     * @param from the page that originates the link
     * @throws RepositoryException if the underlying JCR node and property
     *             cannot be retrieved
     */
    protected void addReferredBy( WikiPath page, WikiPath from ) throws RepositoryException
    {
        if ( page == null || from == null )
        {
            throw new IllegalArgumentException( "Page and from cannot be null!" );
        }
        
        // Make sure the 'referredBy' root exists
        initReferenceMetadata();

        // Set the inverse 'referredBy' link for the destination (referred by
        // the source)
        String jcrPath = getReferencedByJCRNode( page );
        addToProperty( jcrPath, PROPERTY_REFERRED_BY, from.toString(), true );
    }

    /**
     * Adds a single String value to a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. The node is created if it does not
     * exist. Modifications to the underlying JCR nodes are saved by the
     * current JCR {@link Session}.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to add to
     * @param newValue the value to add
     * @param addAgain whether the value should be added again if it already
     *            exists in the list
     */
    protected void addToProperty( String jcrNode, String property, String newValue, boolean addAgain ) throws RepositoryException
    {
        if ( jcrNode == null || property == null || newValue == null )
        {
            throw new IllegalArgumentException( "jcrNode, property and newValue cannot be null!" );
        }
        checkValueString( newValue );
        
        // Retrieve (or create) the destination node for the page
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();
        Node node = null;
        try
        {
            node = (Node) cm.getCurrentSession().getItem( jcrNode );
        }
        catch( PathNotFoundException e )
        {
            if( !s.itemExists( jcrNode ) )
            {
                node = cm.createJCRNode( jcrNode );
            }
        }

        // Retrieve the property; add value to the end
        List<String> newValues = new ArrayList<String>();
        try
        {
            boolean notFound = true;
            Property p = node.getProperty( property );
            Value[] values = p.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                String valueString = values[i].getString();
                checkValueString( valueString );
                if( valueString != null && valueString.length() > 0 )
                {
                    newValues.add( valueString );
                    if( newValue.equals( valueString ) )
                    {
                        notFound = false;
                    }
                }
            }
            if( notFound || addAgain )
            {
                newValues.add( newValue );
            }
            
            // There seems to be a bug in Priha that causes property files to bloat,
            // so we remove the property first, then re-add it
            p.remove();
            s.save();
            node.setProperty( property, newValues.toArray( new String[newValues.size()] ) );
        }
        catch( PathNotFoundException e )
        {
            node.setProperty( property, new String[] { newValue } );
        }
        s.save();
    }

    /**
     * Reads a WikiPage full of data from a String and returns all links
     * internal to this Wiki in a Collection. Links are "resolved"; that is,
     * page resolution is performed to ensure that plural references resolve to
     * the correct page. The links returned by this method will not contain any
     * duplicates, even if the original page markup linked to the same page more
     * than once.
     * 
     * @param path the of the WikiPage to scan
     * @return a Collection of Strings
     * @throws ProviderException if the page contents cannot be retrieved, or if
     *             MarkupParser canot parse the document
     */
    protected List<WikiPath> extractLinks( WikiPath path ) throws PageNotFoundException, ProviderException
    {
        if ( path == null )
        {
            throw new IllegalArgumentException( "Path cannot be null!" );
        }
        
        // Set up a streamlined parser to collect links
        WikiPage page = m_engine.getPage( path );
        LinkCollector localCollector = new LinkCollector();
        String pagedata = page.getContentAsString();
        WikiContext context = m_engine.getWikiContextFactory().newViewContext( page );
        MarkupParser mp = m_engine.getRenderingManager().getParser( context, pagedata );
        mp.addLocalLinkHook( localCollector );
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
        ArrayList<WikiPath> links = new ArrayList<WikiPath>();
        for( String s : localCollector.getLinks() )
        {
            WikiPath finalPath = resolvePage( WikiPath.valueOf( s ) );
            if( !links.contains( finalPath ) )
            {
                links.add( finalPath );
            }
        }

        return links;
    }

    /**
     * Retrieves an array of Strings stored at a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. If the node does not exist, a
     * zero-length array is returned.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to read
     * @throws RepositoryException
     */
    protected String[] getFromProperty( String jcrNode, String property ) throws RepositoryException
    {
        if ( jcrNode == null || property == null )
        {
            throw new IllegalArgumentException( "jcrNode and property cannot be null!" );
        }
        
        // Retrieve the destination node for the page
        ContentManager cm = m_engine.getContentManager();
        Node node = null;
        try
        {
            node = (Node) cm.getCurrentSession().getItem( jcrNode );
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
                checkValueString( values[i].getString() );
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
     * underlying JCR node are saved by the current JCR {@link Session}.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to add to
     * @param value the value to remove. All occurrences of the matching value
     *            will be removed.
     */
    protected void removeFromProperty( String jcrNode, String property, String value ) throws RepositoryException
    {
        if ( jcrNode == null || property == null || value == null )
        {
            throw new IllegalArgumentException( "jcrNode, property and value cannot be null!" );
        }
        checkValueString( value );
        
        // Retrieve (or create) the destination node for the page
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();
        Node node = null;
        try
        {
            node = (Node) cm.getCurrentSession().getItem( jcrNode );
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
                checkValueString( valueString );
                if( valueString != null && valueString.length() > 0 && !value.equals( valueString ) )
                {
                    newValues.add( valueString );
                }
            }
            if( newValues.size() == 0 )
            {
                // There seems to be a bug in Priha that causes property files to bloat,
                // so we remove the property first, then re-add it
                p.remove();
                s.save();
            }
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
        s.save();
    }
    
    /**
     * Determines whether the WIkiPage at a specified path is in fact a page. If the WikiPage
     * at the specified path does not exist, it is not a page. If the WikiPage is an attachment,
     * it is not a page. The WikiPage at a the specified path is only a page if it exists, and
     * is not an attachment.
     */
    private boolean isWikiPage( WikiPath path )
    {
        if ( path == null ) return false;
        try
        {
            WikiPage page = m_cm.getPage( path );
            return !page.isAttachment();
        }
        catch ( Exception e )
        {
            return false;
        }
    }
    
    /**
     * Strictly for troubleshooting: we look for a non-Roman value in the string and throw an exception.
     * @param v
     */
    private void checkValueString( String v )
    {
        int ch = 0;
        boolean highChar = false;
        for ( int i = 0; i < v.length(); i++ )
        {
            ch = v.charAt( i );
            if ( ch < 32 || ch > 127 )
            {
                highChar = true;
                break;
//                throw new IllegalStateException( "Bad character in string " + v +", char='" + (char)ch + "' int=" + ch );
            }
        }
        if ( highChar )
        {
            System.out.println( "non Roman value detected in String " + v );
//            Thread.dumpStack();
        }
    }

    /**
     * <p>
     * Removes all links between a source page and one or more destination
     * pages, and vice-versa. The source page must exist, although the
     * destinations may not. Modifications to the underlying JCR nodes are
     * saved by the current JCR {@link Session}.
     * </p>
     * <p>
     * In addition to setting the inbound and outbound links, this method also
     * updates the unreferenced/uncreated lists.
     * </p>
     * 
     * @param page Name of the page to remove from the maps.
     * @throws PageNotFoundException if the source page does not exist
     * @throws ProviderException
     * @throws RepositoryException if the links cannot be reset
     */
    protected void removeLinks( WikiPath page ) throws ProviderException, RepositoryException
    {
        if ( page == null )
        {
            throw new IllegalArgumentException( "Page cannot be null!" );
        }
        
        // Get old linked pages; add to 'unreferenced list' if needed
        List<WikiPath> referenced = getRefersTo( page );
        for( WikiPath ref : referenced )
        {
            ref = resolvePage( ref );
            List<WikiPath> referredBy = getReferredBy( ref );

            // Is 'page' the last inbound link for the destination?
            boolean unreferenced = referredBy.size() == 0 || (referredBy.size() == 1 && referredBy.contains( page ));
            if( unreferenced )
            {
                addToProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, ref.toString(), false );
            }
            else
            {
                removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, ref.toString() );
            }
        }

        // Remove all inbound links TO the page
        // Let's pretend B and C ---> A

        // First, remove all inbound links from B & C to A
        String jcrPath = getReferencedByJCRNode( page );
        List<WikiPath> inboundLinks = getReferredBy( page );
        for( WikiPath source : inboundLinks )
        {
            removeFromProperty( jcrPath, PROPERTY_REFERRED_BY, source.toString() );
        }

        // Remove all outbound links FROM the page
        // Let's pretend A ---> B and C

        // Remove all inbound links from B &C to A
        List<WikiPath> outboundLinks = getRefersTo( page );
        for( WikiPath destination : outboundLinks )
        {
            jcrPath = ContentManager.getJCRPath( page );
            removeFromProperty( jcrPath, PROPERTY_REFERS_TO, destination.toString() );

            jcrPath = ContentManager.getJCRPath( destination );
            removeFromProperty( jcrPath, PROPERTY_REFERS_TO, page.toString() );

            jcrPath = getReferencedByJCRNode( destination );
            removeFromProperty( jcrPath, PROPERTY_REFERRED_BY, page.toString() );
        }

        // Remove the deleted page from the 'uncreated' and
        // 'unreferenced' lists
        removeFromProperty( NOT_CREATED, PROPERTY_NOT_CREATED, page.toString() );
        removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, page.toString() );
    }

    /**
     * <p>
     * Sets links between a WikiPage (source) and a list of pages it links to
     * (destinations). The source page must exist, but the destination paths
     * need not. In the source WikiPage, existing outbound <code>refersTo</code>
     * links for the page are replaced. For all destination pages the page
     * previously linked to, these pages' inbound <code>referredBy</code>
     * links are also replaced.
     * </p>
     * <p>
     * In addition to setting the inbound and outbound links, this method also
     * updates the unreferenced/uncreated lists.
     * </p>
     * <p>
     * Use this method when a new page has been saved, to a) set up its
     * references and b) notify the referred pages of the references.
     * Modifications to the underlying JCR nodes are not saved by the current
     * JCR {@link Session}. Callers should call {@link Session#save()} to
     * ensure any changes are persisted.
     * </p>
     * 
     * @param source path of the page whose links should be updated
     * @param destinations the paths the page should link to
     * @throws ProviderException
     * @throws RepositoryException
     */
    protected void setLinks( WikiPath source, List<WikiPath> destinations ) throws ProviderException, RepositoryException
    {
        if ( source == null || destinations == null )
        {
            throw new IllegalArgumentException( "Source and destinations cannot be null!" );
        }

        Session s = m_cm.getCurrentSession();

        // Get old linked pages, and add to 'unreferenced list' if needed
        List<WikiPath> referenced = getRefersTo( source );
        for( WikiPath ref : referenced )
        {
            ref = resolvePage( ref );
            List<WikiPath> referredBy = getReferredBy( ref );
            boolean unreferenced = referredBy.size() == 0 || (referredBy.size() == 1 && referredBy.contains( source ));
            if( unreferenced )
            {
                addToProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, ref.toString(), false );
            }
        }

        // First, find all the current outbound links
        List<WikiPath> oldDestinations = getRefersTo( source );
        for( WikiPath oldDestination : oldDestinations )
        {
            String jcrPath = getReferencedByJCRNode( oldDestination );
            removeFromProperty( jcrPath, PROPERTY_REFERRED_BY, source.toString() );
        }

        // Set the new outbound links
        setRefersTo( source, destinations );

        // Set the new referredBy links
        for( WikiPath destination : destinations )
        {
            addReferredBy( destination, source );
        }

        // Is the page itself referenced by any other pages?
        if( getReferredBy( source ).size() == 0 )
        {
            addToProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, source.toString(), false );
        }
        else
        {
            removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, source.toString() );
        }

        // Subtract each destination link from the 'unreferenced' list; possibly
        // subtract from 'uncreated'
        for( WikiPath ref : destinations )
        {
            ref = resolvePage( ref );
            removeFromProperty( NOT_REFERENCED, PROPERTY_NOT_REFERENCED, ref.toString() );
            if( m_cm.pageExists( ref ) )
            {
                removeFromProperty( NOT_CREATED, PROPERTY_NOT_CREATED, ref.toString() );
            }
            else
            {
                addToProperty( NOT_CREATED, PROPERTY_NOT_CREATED, ref.toString(), false );
            }
        }

        // Remove the saved page from the 'uncreated' list
        removeFromProperty( NOT_CREATED, PROPERTY_NOT_CREATED, source.toString() );

        s.save();
    }

    /**
     * Sets the "refersTo" outbound links between a source page and multiple
     * destination pages. The source page must exist, although the destination
     * pages need not. Modifications to the underlying JCR nodes are <em>not</em> saved
     * by the current JCR {@link Session}. Callers should call
     * {@link Session#save()} to ensure any changes are persisted.
     * 
     * @param source the page that originates the link
     * @param destinations the pages that the source page links to. These are
     *            expected to have been previously resolved
     * @throws RepositoryException if the underlying JCR node cannot be
     *             retrieved
     */
    protected void setRefersTo( WikiPath source, List<WikiPath> destinations ) throws ProviderException, RepositoryException
    {
        if ( source == null || destinations == null )
        {
            throw new IllegalArgumentException( "Source and destinations cannot be null!" );
        }
        
        if( !m_cm.pageExists( source ) )
        {
            return;
        }

        // Transform the destination paths into a String array
        String[] destinationStrings = new String[destinations.size()];
        for( int i = 0; i < destinations.size(); i++ )
        {
            destinationStrings[i] = destinations.get( i ).toString();
        }

        // Retrieve the JCR node and add the 'refersTo' links
        ContentManager cm = m_engine.getContentManager();
        Node nd = cm.getJCRNode( ContentManager.getJCRPath( source ) );
        nd.setProperty( PROPERTY_REFERS_TO, destinationStrings );
    }
}
