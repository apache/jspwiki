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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import javax.jcr.*;

import org.apache.commons.lang.time.StopWatch;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.ContentManager;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.content.WikiPath;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiEventUtils;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.filters.BasicPageFilter;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.modules.InternalModule;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;


/**
 *  Calculates wikipage references:
 *  <UL>
 *  <LI>What pages a given page refers to
 *  <LI>What pages refer to a given page
 *  </UL>
 *
 */
public class ReferenceManager
    extends BasicPageFilter
    implements InternalModule, WikiEventListener
{

    /** The WikiEngine that owns this object. */
    private WikiEngine     m_engine;

    private boolean        m_matchEnglishPlurals = false;

    private static Logger log = LoggerFactory.getLogger(ReferenceManager.class);

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 4L;

    private static final String REFERENCES_ROOT = "/wiki:references";
    
    /**
     *  Builds a new ReferenceManager.
     *
     *  @param engine The WikiEngine to which this is managing references to.
     */
    public ReferenceManager( WikiEngine engine )
    {
        m_engine = engine;

        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(),
                                                             WikiEngine.PROP_MATCHPLURALS,
                                                             m_matchEnglishPlurals );

    }

    /**
     *  Initializes the entire reference manager with the initial set of pages
     *  from the collection.
     *
     *  @param pages A collection of all pages you want to be included in the reference
     *               count.
     *  @since 2.2
     *  @throws ProviderException If reading of pages fail.
     */
    public void initialize( Collection<WikiPage> pages )
        throws ProviderException
    {
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
            throw new ProviderException("Failed to initialize repository contents",e);
        }
        
        log.debug( "Initializing new ReferenceManager with "+pages.size()+" initial pages." );
        StopWatch sw = new StopWatch();
        sw.start();
        log.info( "Starting cross reference scan of WikiPages" );

        /*
        //
        //  First, try to serialize old data from disk.  If that fails,
        //  we'll go and update the entire reference lists (which'll take
        //  time)
        //
        try
        {
            //
            //  Unserialize things.  The loop below cannot be combined with
            //  the other loop below, simply because engine.getPage() has
            //  side effects such as loading initializing the user databases,
            //  which in turn want all of the pages to be read already...
            //
            //  Yes, this is a kludge.  We know.  Will be fixed.
            //
            long saved = unserializeFromDisk();

            for( Iterator it = pages.iterator(); it.hasNext(); )
            {
                WikiPage page = (WikiPage) it.next();

                unserializeAttrsFromDisk( page );
            }

            //
            //  Now we must check if any of the pages have been changed
            //  while we were in the electronic la-la-land, and update
            //  the references for them.
            //

            Iterator it = pages.iterator();

            while( it.hasNext() )
            {
                WikiPage page = (WikiPage) it.next();

                if( page.isAttachment() )
                {
                    // Skip attachments
                }
                else
                {

                    // Refresh with the latest copy
                    page = m_engine.getPage( page.getName() );

                    if( page.getLastModified() == null )
                    {
                        log.error( "Provider returns null lastModified.  Please submit a bug report." );
                    }
                    else if( page.getLastModified().getTime() > saved )
                    {
                        updatePageReferences( page );
                    }
                }
            }

        }
        catch( Exception e )
        {
            log.info("Unable to unserialize old refmgr information, rebuilding database: "+e.getMessage());
            buildKeyLists( pages );

            // Scan the existing pages from disk and update references in the manager.
            Iterator it = pages.iterator();
            while( it.hasNext() )
            {
                WikiPage page  = (WikiPage)it.next();

                if( page.isAttachment() )
                {
                    // We cannot build a reference list from the contents
                    // of attachments, so we skip them.
                }
                else
                {
                    updatePageReferences( page );

                    serializeAttrsToDisk( page );
                }

            }

            serializeToDisk();
        }
*/
        sw.stop();
        log.info( "Cross reference scan done in "+sw );

        WikiEventUtils.addWikiEventListener(m_engine.getContentManager(),
                                            WikiPageEvent.PAGE_DELETE_REQUEST, this);
    }

    /**
     *  After the page has been saved, updates the reference lists.
     *  
     *  @param context {@inheritDoc}
     *  @param content {@inheritDoc}
     */
    public void postSave( WikiContext context, String content )
    {
        WikiPage page = context.getPage();

        try
        {
            updateReferences( page,
                              context.getEngine().scanWikiLinks( page, content ) );
        }
        catch( ProviderException e )
        {
            log.error("ReferenceManager updates failed, repo is now in inconsistent state!",e);
        }
    }

    /**
     * Updates the m_referedTo and m_referredBy hashmaps when a page has been
     * deleted.
     * <P>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set
     * has to be removed to keep the map clean.
     * Within the m_referredBy map the name is stored as a value. Since a key
     * can have more than one value we have to delete just the key-value-pair
     * referring page:deleted page.
     *
     *  @param pageName Name of the page to remove from the maps.
     * @throws PageNotFoundException 
     * @throws ProviderException 
     */
    public synchronized void pageRemoved( WikiPath pageName ) throws ProviderException, PageNotFoundException
    {
        WikiPage page = m_engine.getContentManager().getPage( pageName );
        
        Collection<WikiPath> refTo = page.getRefersTo();

        for( WikiPath referred : refTo )
        {
            log.debug( "Removing references to page %s from page %s", page.getPath(), referred );
            Set<WikiPath> referredBy = findReferrers(referred);
            
            referredBy.remove( page.getPath() );
            
            try
            {
                setReferredBy( referred, referredBy );
            }
            catch( RepositoryException e )
            {
                throw new ProviderException("Failed to change the contents of pages",e);
            }
        }

    }

    /**
     *  Build the path which is used to store the ReferredBy data  
     */
    private final String getReferredByJCRPath(WikiPath name)
    {
        return "/wiki:references/"+name.getSpace()+"/"+name.getPath();
    }
    
    /**
     *  Finds all the pages which are referenced by the given page.
     *  
     *  @param name The WikiName to find.
     *  @return A set of WikiNames. If the page in question does not exist, returns
     *          an empty set.
     *  @throws ProviderException If something goes wrong.
     *  @since 3.0
     */
    public Set<WikiPath> findReferrers(WikiPath name) throws ProviderException
    {
        String jcrPath = getReferredByJCRPath( name );
        
        try
        {
            jcrPath += "/wiki:referredBy";
            
            Property p = (Property)m_engine.getContentManager().getCurrentSession().getItem(jcrPath);
            
            TreeSet<WikiPath> result = new TreeSet<WikiPath>();
            
            for( Value v : p.getValues() )
            {
                result.add( WikiPath.valueOf( v.getString() ) );
            }
            
            return result;
        }
        catch( PathNotFoundException e )
        {
            // Fine, we can return an empty set
            return new TreeSet<WikiPath>();
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to get the referred-by list",e);
        }
    }
        
    /**
     *  Set the referredBy attribute set.
     *  
     *  @param name
     *  @param references
     *  @throws RepositoryException
     */
    private void setReferredBy(WikiPath name,Set<WikiPath> references) throws RepositoryException
    {
        String jcrPath = getReferredByJCRPath( name );
        Property p = null;

        String[] value = new String[references.size()];
        WikiPath[] refs = references.toArray(new WikiPath[references.size()]);
        
        for( int i = 0; i < references.size(); i++ )
        {
            value[i] = refs[i].toString();
        }
        
        ContentManager mgr = m_engine.getContentManager();
        
        try
        {
            p = (Property)mgr.getCurrentSession().getItem(jcrPath+"/wiki:referredBy");

            p.setValue( value );
        }
        catch( PathNotFoundException e )
        {
            Session s = mgr.getCurrentSession();
            if( !s.itemExists( jcrPath ) )
            {
                mgr.createJCRNode(jcrPath);
            }
            
            Node nd = (Node) s.getItem(jcrPath);
            
            nd.setProperty( "wiki:referredBy", value );
        }
        
        mgr.getCurrentSession().getItem( "/wiki:references" ).save();
    }
    
    /**
     *  Updates list of pages a new or edited WikiPage refers to. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <P>
     *  This is the method to call when a new page has been created and we
     *  want to a) set up its references and b) notify the referred pages
     *  of the references. Use this method during run-time.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    public synchronized void updateReferences( WikiPage page, Collection<WikiPath> references ) 
        throws ProviderException
    {
        try
        {
            internalUpdateReferences( (JCRWikiPage)page, references);
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Failed to update references",e);
        }
    }

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <p>
     *  This method does not synchronize the database to disk.
     *
     *  @param page Name of the page to update.
     *  @param newRefersTo A Collection of Strings, each one pointing to a page this page references.
     * @throws ProviderException 
     * @throws RepositoryException 
     */

    private void internalUpdateReferences(JCRWikiPage page, Collection<WikiPath> newRefersTo) throws ProviderException, RepositoryException
    {
        //
        //  Get the old refererences so that we can go and ping every page on that
        //  list and make sure that their referredBy lists are fine.
        //
        Collection<WikiPath> oldRefersTo = page.getRefersTo();

        //
        //  Set up the new references list
        //

        WikiPath[] wn = newRefersTo.toArray(new WikiPath[newRefersTo.size()]);
        String[] nr = new String[newRefersTo.size()];
        
        for( int i = 0; i < nr.length; i++ )
        {
            nr[i] = wn[i].toString();
        }
        
        Node nd = page.getJCRNode();
      
        nd.setProperty( JCRWikiPage.REFERSTO, nr );
        
        //
        //  Go ping the old pages that the reference list has changed.
        //
        
        for( WikiPath name : oldRefersTo )
        {
            if( !newRefersTo.contains( name ) )
            {
                // A page is no longer referenced, so this page is removed from its
                // referencedBy list.
                Set<WikiPath> refs = findReferrers( name );
                refs.remove( page.getPath() );
                setReferredBy( name, refs );
            }
        }
        
        for( WikiPath name : newRefersTo )
        {
            if( !oldRefersTo.contains(name) )
            {
                // There is a new reference which is not in the old references list,
                // so we will need to add it to the new page's referencedBy list.
                Set<WikiPath> refs = findReferrers( name );
                refs.add( page.getPath() );
                setReferredBy( name, refs );
            }
        }
        
    }



    /**
     *  Finds all unreferenced pages. This requires a linear scan through
     *  m_referredBy to locate keys with null or empty values.
     *  
     *  @return The Collection of Strings
     */
    public synchronized Collection<String> findUnreferenced()
    {
        ArrayList<String> unref = new ArrayList<String>();

        // FIXME: Not implemented yet
        /*
        for( String key : m_referredBy.keySet() )
        {
            Set<?> refs = getReferenceList( m_referredBy, key );
            
            if( refs == null || refs.isEmpty() )
            {
                unref.add( key );
            }
        }
*/
        return unref;
    }


    /**
     * Finds all references to non-existant pages. This requires a linear
     * scan through m_refersTo values; each value must have a corresponding
     * key entry in the reference Maps, otherwise such a page has never
     * been created.
     * <P>
     * Returns a Collection containing Strings of unreferenced page names.
     * Each non-existant page name is shown only once - we don't return information
     * on who referred to it.
     * 
     * @return A Collection of Strings
     */
    public synchronized Collection<String> findUncreated()
    {
        TreeSet<String> uncreated = new TreeSet<String>();

        // Go through m_refersTo values and check that m_refersTo has the corresponding keys.
        // We want to reread the code to make sure our HashMaps are in sync...

        // FIXME: Not yet done
        /*
        Collection<Collection<String>> allReferences = m_refersTo.values();

        for( Collection<String> refs : allReferences )
        {
            if( refs != null )
            {
                for( String aReference : refs )
                {
                    if( m_engine.pageExists( aReference ) == false )
                    {
                        uncreated.add( aReference );
                    }
                }
            }
        }
         */
        return uncreated;
    }

    /**
     *  Returns all pages that this page refers to.  You can use this as a quick
     *  way of getting the links from a page, but note that it does not link any
     *  InterWiki, image, or external links.  It does contain attachments, though.
     *  <p>
     *  The Collection returned is unmutable, so you cannot change it.  It does reflect
     *  the current status and thus is a live object.  So, if you are using any
     *  kind of an iterator on it, be prepared for ConcurrentModificationExceptions.
     *  <p>
     *  The returned value is a Collection, because a page may refer to another page
     *  multiple times.
     *
     * @param pageName Page name to query
     * @return A Collection of Strings containing the names of the pages that this page
     *         refers to. May return null, if the page does not exist or has not
     *         been indexed yet.
     * @throws PageNotFoundException 
     * @throws ProviderException 
     * @since 2.2.33
     * @deprecated Use WikiPage.getRefersTo() instead
     */
    public Collection<String> findRefersTo( String pageName ) throws ProviderException, PageNotFoundException
    {
        ArrayList<String> result = new ArrayList<String>();
        
        Collection<WikiPath> refs = m_engine.getPage( pageName ).getRefersTo();
        
        for( WikiPath wn : refs )
            result.add( wn.toString() );
        
        return result;
    }


    /**
     *  Returns a list of all pages that the ReferenceManager knows about.
     *  This should be roughly equivalent to PageManager.getAllPages(), but without
     *  the potential disk access overhead.  Note that this method is not guaranteed
     *  to return a Set of really all pages (especially during startup), but it is
     *  very fast.
     *
     *  @return A Set of all defined page names that ReferenceManager knows about.
     *  @throws ProviderException 
     *  @since 2.3.24
     *  @deprecated
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
     *  {@inheritDoc}
     */
    public void actionPerformed(WikiEvent event)
    {
        if( (event instanceof WikiPageEvent) && (event.getType() == WikiPageEvent.PAGE_DELETE_REQUEST) )
        {
            String pageName = ((WikiPageEvent) event).getPageName();

            if( pageName != null )
            {
                try
                {
                    pageRemoved( WikiPath.valueOf(pageName) );
                }
                catch( ProviderException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                catch( PageNotFoundException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }
}
