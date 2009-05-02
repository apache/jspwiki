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
package org.apache.wiki;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.*;

import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.event.*;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.parser.JSPWikiMarkupParser;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.ProviderException;
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
 * originates the link, as the multi-valued property named {@value #REFERS_TO}}.
 * Inbound links ("referred by") are stored in a separate part of the content
 * repository, in the special path {@value #REFERENCES_ROOT}, where each node
 * represents the page that is being linked to (<em>i.e,</em> a page link
 * named in wiki markup). The multi-valued property named {@value #REFERRED_BY}
 * stores the source of the link.
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
 * </p>
 * <p>
 * It is always possible that, despite JSPWiki's best efforts, that the link
 * table becomes corrupt. When this happens, the method {@link #rebuild()} will
 * erase and reconstruct all of the link references.
 * </p>
 */
public class ReferenceManager implements InternalModule, WikiEventListener
{

    /** The WikiEngine that owns this object. */
    private WikiEngine m_engine;

    private ContentManager m_cm;

    private boolean m_camelCase = false;

    private boolean m_matchEnglishPlurals = false;

    public static final String REFERS_TO = "wiki:refersTo";

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 4L;

    protected static final String REFERENCES_ROOT = "/wiki:references";

    protected static final String NOT_REFERENCED = "notReferenced";

    protected static final String NOT_CREATED = "notCreated";

    private static final List<WikiPath> NO_LINKS = Collections.emptyList();

    private static final String[] NO_VALUES = new String[0];
    

    private static final Pattern LINK_PATTERN = Pattern
        .compile( "([\\[\\~]?)\\[([^\\|\\]]*)(\\|)?([^\\|\\]]*)(\\|)?([^\\|\\]]*)\\]" );

    /**
     * JCR path path prefix for inbound "referredby" links, used by
     * {@link #addReferredBy(WikiPath, List)}.
     */
    private static final String REFERRED_BY = "wiki:referredBy";

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
            Node nd = s.getRootNode().getNode( REFERENCES_ROOT );
            nd.remove();
        }
        s.save();

        initReferredByNodes();
        s.save();
        cm.release();

        // TODO: we should actually parse the pages
    }

    /**
     * Verifies that the JCR nodes for storing references exist, and creates
     * then if they do not. The nodes are NOT saved; that is the responsibility
     * of callers.
     */
    private void initReferredByNodes() throws RepositoryException
    {
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();
        if( !s.getRootNode().hasNode( REFERENCES_ROOT ) )
        {
            s.getRootNode().addNode( REFERENCES_ROOT );
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
            Session session = m_engine.getContentManager().getCurrentSession();

            if( !session.getRootNode().hasNode( REFERENCES_ROOT ) )
            {
                session.getRootNode().addNode( REFERENCES_ROOT );

                session.save();
            }
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Failed to initialize repository contents", e );
        }

        // Make sure we catch any page add/save/rename events
        WikiEventManager.addWikiEventListener( engine.getContentManager(), this );
    }

    /**
     * <p>
     * Removes all links between a source page and one or more destination
     * pages, and vice-versa. The source page must exist, although the
     * destinations may not. The modified nodes are saved.
     * <p>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set
     * has to be removed to keep the map clean. Within the m_referredBy map the
     * name is stored as a value. Since a key can have more than one value we
     * have to delete just the key-value-pair referring page:deleted page.
     * </p>
     * 
     * @param page Name of the page to remove from the maps.
     * @throws PageNotFoundException if the source page does not exist
     * @throws ProviderException
     * @throws RepositoryException if the links cannot be reset
     */
    protected synchronized void removeLinks( WikiPath page ) throws ProviderException, RepositoryException
    {
        Session s = m_cm.getCurrentSession();

        // Remove all inbound links TO the page
        // Let's pretend B and C ---> A

        // First, remove all inbound links from B & C to A
        String jcrPath = getReferencedByJCRNode( page );
        List<WikiPath> inboundLinks = getReferredBy( page );
        for( WikiPath source : inboundLinks )
        {
            removeAllFromValues( jcrPath, REFERRED_BY, source.toString() );
            s.save();
        }

        // Remove all outbound links FROM the page
        // Let's pretend A ---> B and C

        // Remove all inbound links from B &C to A
        List<WikiPath> outboundLinks = getRefersTo( page );
        for( WikiPath destination : outboundLinks )
        {
            jcrPath = ContentManager.getJCRPath( page );
            removeAllFromValues( jcrPath, REFERS_TO, destination.toString() );
            s.save();

            jcrPath = ContentManager.getJCRPath( destination );
            removeAllFromValues( jcrPath, REFERS_TO, page.toString() );
            s.save();

            jcrPath = getReferencedByJCRNode( destination );
            removeAllFromValues( jcrPath, REFERRED_BY, page.toString() );
            s.save();
        }

    }

    /**
     * Build the path which is used to store the ReferredBy data
     */
    private String getReferencedByJCRNode( WikiPath name )
    {
        return "/wiki:references/" + name.getSpace() + "/" + name.getPath();
    }

    /**
     * <p>
     * Returns all pages that refers to a destination page. You can use this as
     * a quick way of getting the inbound links to a page from other pages. The
     * page being looked up need not exist. The requested page is not resolved
     * in any way, so if the page is not found as specified exactly by the path,
     * a zero-length list will be returned.
     * </p>
     * 
     * @param destination the page to look up
     * @return the list of pages that link to this page
     * @throws ProviderException If something goes wrong
     * @throws RepositoryException If the referredBy root cannot be checked (or
     *             created)
     * @since 3.0
     */
    public List<WikiPath> getReferredBy( WikiPath destination ) throws ProviderException
    {
        String jcrPath = getReferencedByJCRNode( destination );

        try
        {
            jcrPath += "/" + REFERRED_BY;

            Property p = (Property) m_engine.getContentManager().getCurrentSession().getItem( jcrPath );

            ArrayList<WikiPath> result = new ArrayList<WikiPath>();

            for( Value v : p.getValues() )
            {
                result.add( WikiPath.valueOf( v.getString() ) );
            }

            return result;
        }
        catch( PathNotFoundException e )
        {
            // Fine, we can return an empty set
            return Collections.emptyList();
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to get the referred-by list", e );
        }
    }

    /**
     * Adds a "referredBy" inbound link to a page from a source page that links
     * to it. That is, for the destination page, a "referredBy" entry is made
     * that contains the name of the source page. Neither the source or
     * destination pages need exist. This method saves the underlying Node after
     * processing is complete.
     * 
     * @param page the page that is the destination for the link
     * @param from the page that originates the link
     * @throws RepositoryException if the underlying JCR node cannot be
     *             retrieved
     */
    protected void addReferredBy( WikiPath page, WikiPath from ) throws RepositoryException
    {
        ContentManager cm = m_engine.getContentManager();
        Session s = cm.getCurrentSession();

        // Make sure the 'referredBy' root exists
        initReferredByNodes();

        // Set the inverse 'referredBy' link for the destination (referred by
        // the source)
        String jcrPath = getReferencedByJCRNode( page );
        addToValues( jcrPath, REFERRED_BY, from.toString() );

        // Save the node
        s.save();
    }

    /**
     * Retrieves an array of Strings stored at a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. If the node does not exist, a zero-length
     * array is returned.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to read
     * @throws RepositoryException
     */
    protected String[] getValues( String jcrNode, String property ) throws RepositoryException
    {
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
            Property p = (Property) node.getProperty( property );
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
     * Adds a single String value to a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. The node is created if it does not
     * exist. Modifications to the node are not saved.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to add to
     * @param value the value to add
     * @param addAgain whether the value should be added again if it already exists in the list
     */
    protected void addToValues( String jcrNode, String property, String newValue, boolean addAgain ) throws RepositoryException
    {
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
        List<String>newValues = new ArrayList<String>();
        try
        {
            boolean notFound = true;
            Property p = (Property) node.getProperty( property );
            Value[] values = p.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                newValues.add( values[i].getString() );
                if ( values[i].equals( newValue ) )
                {
                    notFound = false;
                }
            }
            if ( notFound || addAgain )
            {
                newValues.add( newValue );
            }
            node.setProperty( property, newValues.toArray(new String[newValues.size()]) );
        }
        catch( PathNotFoundException e )
        {
            node.setProperty( property, new String[]{ newValue } );
        }
    }
    
    /**
     * Adds a single String value to a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. The node is created if it does not
     * exist. Modifications to the node are not saved.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to add to
     * @param value the value to add. It it already exists in the list, it will
     *            be added again.
     */
    protected void addToValues( String jcrNode, String property, String newValue ) throws RepositoryException
    {
        addToValues( jcrNode, property, newValue, true );
    }

    /**
     * Removes a String value from a given JCR node and
     * {@link javax.jcr.Property}. The property is assumed to return an array
     * of {@link javax.jcr.Value} objects. The node is created if it does not
     * exist. Modifications to the node are not saved.
     * 
     * @param jcrNode the JCR path to the node
     * @param property the property to add to
     * @param value the value to remove. All occurrences of the matching value
     *            will be removed.
     */
    protected void removeAllFromValues( String jcrNode, String property, String value ) throws RepositoryException
    {
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

        // Retrieve the property; remove all instances of value
        List<String> newValues = new ArrayList<String>();
        try
        {
            Property p = (Property) node.getProperty( property );
            Value[] values = p.getValues();
            for( int i = 0; i < values.length; i++ )
            {
                if( !values[i].getString().equals( value ) )
                {
                    newValues.add( values[i].getString() );
                }
            }
            if( newValues.size() == 0 )
            {
                // This seems like a hack, but zero-length arrays don't seem to
                // work
                // unless we remove the property entirely first.
                p.remove();
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
     * Use this method when a new page has been saved, to a) set up its
     * references and b) notify the referred pages of the references. This
     * method does not synchronize the database to disk.
     * </p>
     * 
     * @param source path of the page whose links should be updated
     * @param destinations the paths the page should link to
     * @throws ProviderException
     * @throws RepositoryException
     */
    protected synchronized void setLinks( WikiPath source, List<WikiPath> destinations )
                                                                                        throws ProviderException,
                                                                                            RepositoryException
    {

        Session s = m_cm.getCurrentSession();

        // Remove all referredBy links from this page

        // First, find all the current outbound links
        List<WikiPath> oldDestinations = getRefersTo( source );
        for( WikiPath oldDestination : oldDestinations )
        {
            String jcrPath = getReferencedByJCRNode( oldDestination );
            removeAllFromValues( jcrPath, REFERRED_BY, source.toString() );
        }

        // Set the new outbound links
        setRefersTo( source, destinations );

        // Set the new referredBy links
        for( WikiPath destination : destinations )
        {
            addReferredBy( destination, source );
        }

        s.save();
    }

    /**
     * Sets the "refersTo" outbound links between a source page and multiple
     * destination pages. The source page must exist, although the destination
     * pages need not.
     * 
     * @param source the page that originates the link
     * @param destinations the pages that the source page links to
     * @throws RepositoryException if the underlying JCR node cannot be
     *             retrieved
     */
    protected void setRefersTo( WikiPath source, List<WikiPath> destinations ) throws ProviderException, RepositoryException
    {
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
        nd.setProperty( REFERS_TO, destinationStrings );
        nd.save();
    }

    /**
     * <p>
     * Returns a list of Strings representing pages that have been created,
     * but not yet referenced in wiki markup by any other pages.
     * Each not-referenced page name is shown only once.
     * </p>
     * 
     * @return A list of Strings, where each names a page that hasn't been
     *         created
     */
    public List<String> findUnreferenced() throws RepositoryException
    {
        String[] linkStrings= getValues( REFERENCES_ROOT, NOT_REFERENCED );
        List<String> links = new ArrayList<String>();
        for ( String link : linkStrings )
        {
            links.add( link );
        }
        return links;
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
    public List<String> findUncreated() throws RepositoryException
    {
        String[] linkStrings= getValues( REFERENCES_ROOT, NOT_CREATED );
        List<String> links = new ArrayList<String>();
        for ( String link : linkStrings )
        {
            links.add( link );
        }
        return links;
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
     * @deprecated Use {@link #getRefersTo(String)} instead
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
     * Returns all destination pages that a page refers to. You can use this as
     * a quick way of getting the outbound links from a page to the destination
     * pages its markup refers to, but note that it does not link any InterWiki,
     * image, or external links. It does contain attachments, though. The
     * requested page is not resolved in any way, so if the page is not found as
     * specified exactly by the path, a zero-length list will be returned.
     * </p>
     * 
     * @param source the page to look up
     * @return the list of pages this page links to
     * @throws ProviderException
     */
    public List<WikiPath> getRefersTo( WikiPath source ) throws ProviderException
    {
        ContentManager cm = m_engine.getContentManager();
        List<WikiPath> links = NO_LINKS;

        try
        {
            links = new ArrayList<WikiPath>();

            Node node = cm.getJCRNode( ContentManager.getJCRPath( source ) );
            Property p = node.getProperty( REFERS_TO );

            Value[] values = p.getValues();

            for( Value v : values )
            {
                links.add( WikiPath.valueOf( v.getString() ) );
            }
        }
        catch( PathNotFoundException e )
        {
            // No worries! Just return the empty list...
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to get the referrals", e );
        }
        return links;
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
        Set<String> result = new TreeSet<String>();
        Collection<WikiPage> c = m_engine.getContentManager().getAllPages( null );

        for( WikiPage p : c )
            result.add( p.getPath().toString() );

        return result;
    }

    /**
     * {@inheritDoc} After the page has been saved, updates the reference lists.
     */
    public void actionPerformed( WikiEvent event )
    {
        if( !(event instanceof WikiPageEvent) )
        {
            return;
        }

        String pageName = ((WikiPageEvent) event).getPageName();
        if( pageName == null )
        {
            return;
        }

        try
        {
            switch( event.getType() )
            {
                // ========= page deleted ==============================
                
                // If page was deleted, remove all references to it/from it
                case (ContentEvent.NODE_DELETE_REQUEST ): {
                    WikiPath path = resolvePage( WikiPath.valueOf( pageName ) );
                    List<WikiPath> referenced = getRefersTo( path );
                    
                    // Remove the links from the deleted page to its referenced pages
                    removeLinks( path );
                    
                    // Check each previously-referenced page; see if they still have inbound refs
                    for ( WikiPath ref : referenced )
                    {
                        if ( getReferredBy( ref ).size() == 0 )
                        {
                            addToValues( REFERENCES_ROOT, NOT_REFERENCED, pageName, false );
                        }
                        else
                        {
                            removeAllFromValues( REFERENCES_ROOT, NOT_REFERENCED, pageName );
                        }
                    }
                    
                    // Remove the deleted page from the 'uncreated' and 'unreferenced' lists
                    removeAllFromValues( REFERENCES_ROOT, NOT_CREATED, pageName );
                    removeAllFromValues( REFERENCES_ROOT, NOT_REFERENCED, pageName );
                    
                    m_cm.getCurrentSession().save();
                    
                    break;
                }

                // ========= page saved ==============================
                
                // If page was saved, update all references
                case (ContentEvent.NODE_SAVED ): {
                    WikiPath path = resolvePage( WikiPath.valueOf( pageName ) );
                    
                    // Get old linked pages, and add to 'unreferenced list' if needed
                    List<WikiPath> referenced = extractLinks( path );
                    for ( WikiPath ref : referenced )
                    {
                        ref = resolvePage( ref );
                        List<WikiPath> referredBy = getReferredBy( ref );
                        boolean unreferenced = referredBy.size() == 0 || ( referredBy.size() == 1 && referredBy.contains( path ) );
                        if ( unreferenced )
                        {
                            addToValues( REFERENCES_ROOT, NOT_REFERENCED, ref.toString() );
                        }
                    }
                    
                    // Get the new linked pages, and set refersTo/referencedBy links
                    referenced = extractLinks( path );
                    setLinks( path, referenced );
                    
                    // Remove the saved page from the 'uncreated' list
                    removeAllFromValues( REFERENCES_ROOT, NOT_CREATED, pageName );
                    
                    // Subtract each link from the 'unreferenced' list; possibly subtract from 'uncreated'
                    for ( WikiPath ref : referenced )
                    {
                        ref = resolvePage( ref );
                        removeAllFromValues( REFERENCES_ROOT, NOT_REFERENCED, ref.toString() );
                        if ( m_cm.pageExists( ref ) )
                        {
                            removeAllFromValues( REFERENCES_ROOT, NOT_CREATED, ref.toString() );
                        }
                        else
                        {
                            addToValues( REFERENCES_ROOT, NOT_CREATED, ref.toString(), false );
                        }
                    }
                    
                    m_cm.getCurrentSession().save();
                    
                    break;
                }

                // ========= page renamed ==============================
                
                case (ContentEvent.NODE_RENAMED ): {
                    // Update references from this page
                    WikiPath toPage = WikiPath.valueOf( pageName );
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

    private WikiPath resolvePage( WikiPath path ) throws ProviderException
    {
        WikiPath finalPath = m_engine.getFinalPageName( path );
        return finalPath == null ? path : finalPath;
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
                }
            }
            catch( PageNotFoundException e )
            {
                // Just continue
            }
        }
    }

    /**
     * Reads a WikiPage full of data from a String and returns all links
     * internal to this Wiki in a Collection.
     * 
     * @param page The WikiPage to scan
     * @return a Collection of Strings
     * @throws ProviderException if the page contents cannot be retrieved, or if
     *             MarkupParser canot parse the document
     */
    protected List<WikiPath> extractLinks( WikiPath path ) throws PageNotFoundException, ProviderException
    {
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
            links.add( finalPath );
        }

        return links;
    }

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
}
