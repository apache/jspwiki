/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright 2008 The Apache Software Foundation 
    
    Licensed under the Apache License, Version 2.0 (the "License"); 
    you may not use this file except in compliance with the License. 
    You may obtain a copy of the License at 
    
      http://www.apache.org/licenses/LICENSE-2.0 
      
    Unless required by applicable law or agreed to in writing, software 
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
    See the License for the specific language governing permissions and 
    limitations under the License.    
 */
package com.ecyrd.jspwiki;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.*;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.auth.acl.AclEntry;
import com.ecyrd.jspwiki.auth.acl.AclEntryImpl;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.event.*;
import com.ecyrd.jspwiki.filters.FilterException;
import com.ecyrd.jspwiki.modules.ModuleManager;
import com.ecyrd.jspwiki.providers.CachingProvider;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.RepositoryModifiedException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.ClassUtil;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;
import com.ecyrd.jspwiki.workflow.Outcome;
import com.ecyrd.jspwiki.workflow.Task;
import com.ecyrd.jspwiki.workflow.Workflow;

/**
 *  Manages the WikiPages.  This class functions as an unified interface towards
 *  the page providers.  It handles initialization and management of the providers,
 *  and provides utility methods for accessing the contents.
 *
 *  @since 2.0
 */
// FIXME: This class currently only functions just as an extra layer over providers,
//        complicating things.  We need to move more provider-specific functionality
//        from WikiEngine (which is too big now) into this class.
public class PageManager extends ModuleManager implements WikiEventListener
{
    private static final long serialVersionUID = 1L;

    /** The property value for setting the current page provider.  Value is {@value}. */
    public static final String PROP_PAGEPROVIDER = "jspwiki.pageProvider";
    
    /** The property value for setting the cache on/off.  Value is {@value}. */
    public static final String PROP_USECACHE     = "jspwiki.usePageCache";
    
    /** The property value for setting the amount of time before the page locks expire. 
     *  Value is {@value}.
     */
    public static final String PROP_LOCKEXPIRY   = "jspwiki.lockExpiryTime";
    
    public static final String PRESAVE_TASK_MESSAGE_KEY = "task.preSaveWikiPage";
    public static final String PRESAVE_WIKI_CONTEXT = "wikiContext";
    public static final String SAVE_APPROVER = "workflow.saveWikiPage";
    public static final String SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    public static final String SAVE_REJECT_MESSAGE_KEY = "notification.saveWikiPage.reject";
    public static final String SAVE_TASK_MESSAGE_KEY = "task.saveWikiPage";
    
    /** Fact name for storing the page name.  Value is {@value}. */
    public static final String FACT_PAGE_NAME = "fact.pageName";
    
    /** Fact name for storing a diff text. Value is {@value}. */
    public static final String FACT_DIFF_TEXT = "fact.diffText";
    
    /** Fact name for storing the current text.  Value is {@value}. */
    public static final String FACT_CURRENT_TEXT = "fact.currentText";
    
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    public static final String FACT_PROPOSED_TEXT = "fact.proposedText";
    
    public static final String FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    static Logger log = Logger.getLogger( PageManager.class );

    private WikiPageProvider m_provider;

    protected HashMap m_pageLocks = new HashMap();

    private WikiEngine m_engine;

    private int m_expiryTime = 60;

    private LockReaper m_reaper = null;

    /**
     *  Creates a new PageManager.
     *  
     *  @param engine WikiEngine instance
     *  @param props Properties to use for initialization
     *  @throws WikiException If anything goes wrong, you get this.
     */
    public PageManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        super( engine );

        String classname;

        m_engine = engine;

        boolean useCache = "true".equals(props.getProperty( PROP_USECACHE ));

        m_expiryTime = TextUtil.parseIntParameter( props.getProperty( PROP_LOCKEXPIRY ), 60 );

        //
        //  If user wants to use a cache, then we'll use the CachingProvider.
        //
        if( useCache )
        {
            classname = "com.ecyrd.jspwiki.providers.CachingProvider";
        }
        else
        {
            classname = WikiEngine.getRequiredProperty( props, PROP_PAGEPROVIDER );
        }

        try
        {
            log.debug("Page provider class: '"+classname+"'");

            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                                                       classname );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing page provider class "+m_provider);
            m_provider.initialize( m_engine, props );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class '"+classname+"'",e);
            throw new WikiException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class '"+classname+"'",e);
            throw new WikiException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class '"+classname+"'",e);
            throw new WikiException("illegal provider class");
        }
        catch( NoRequiredPropertyException e )
        {
            log.error("Provider did not found a property it was looking for: "+e.getMessage(),
                      e);
            throw e;  // Same exception works.
        }
        catch( IOException e )
        {
            log.error("An I/O exception occurred while trying to create a new page provider: "+classname, e );
            throw new WikiException("Unable to start page provider: "+e.getMessage());
        }

    }


    /**
     *  Returns the page provider currently in use.
     *  
     *  @return A WikiPageProvider instance.
     */
    public WikiPageProvider getProvider()
    {
        return m_provider;
    }

    /**
     *  Returns all pages in some random order.  If you need just the page names, 
     *  please see {@link ReferenceManager#findCreated()}, which is probably a lot
     *  faster.  This method may cause repository access.
     *  
     *  @return A Collection of WikiPage objects.
     *  @throws ProviderException If the backend has problems.
     */
    public Collection getAllPages()
        throws ProviderException
    {
        return m_provider.getAllPages();
    }

    /**
     *  Fetches the page text from the repository.  This method also does some sanity checks,
     *  like checking for the pageName validity, etc.  Also, if the page repository has been
     *  modified externally, it is smart enough to handle such occurrences.
     *  
     *  @param pageName The name of the page to fetch.
     *  @param version The version to find
     *  @return The page content as a raw string
     *  @throws ProviderException If the backend has issues.
     */
    public String getPageText( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        String text = null;

        try
        {
            text = m_provider.getPageText( pageName, version );
        }
        catch( RepositoryModifiedException e )
        {
            //
            //  This only occurs with the latest version.
            //
            log.info("Repository has been modified externally while fetching page "+pageName );

            //
            //  Empty the references and yay, it shall be recalculated
            //
            //WikiPage p = new WikiPage( pageName );
            WikiPage p = m_provider.getPageInfo( pageName, version );

            m_engine.updateReferences( p );

            if( p != null )
            {
                m_engine.getSearchManager().reindexPage( p );
                text = m_provider.getPageText( pageName, version );
            }
            else
            {
                //
                //  Make sure that it no longer exists in internal data structures either.
                //
                WikiPage dummy = new WikiPage(m_engine,pageName);
                m_engine.getSearchManager().pageRemoved(dummy);
                m_engine.getReferenceManager().pageRemoved(dummy);
            }
        }

        return text;
    }

    /**
     *  Returns the WikiEngine to which this PageManager belongs to.
     *  
     *  @return The WikiEngine object.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     *  Puts the page text into the repository.  Note that this method does NOT update
     *  JSPWiki internal data structures, and therefore you should always use WikiEngine.saveText()
     *
     * @param page Page to save
     * @param content Wikimarkup to save
     * @throws ProviderException If something goes wrong in the saving phase
     */
    public void putPageText( WikiPage page, String content )
        throws ProviderException
    {
        if( page == null || page.getName() == null || page.getName().length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        m_provider.putPageText( page, content );
    }

    /**
     *  Locks page for editing.  Note, however, that the PageManager
     *  will in no way prevent you from actually editing this page;
     *  the lock is just for information.
     *
     *  @param page WikiPage to lock
     *  @param user Username to use for locking
     *  @return null, if page could not be locked.
     */
    public PageLock lockPage( WikiPage page, String user )
    {
        PageLock lock = null;

        if( m_reaper == null )
        {
            //
            //  Start the lock reaper lazily.  We don't want to start it in
            //  the constructor, because starting threads in constructors
            //  is a bad idea when it comes to inheritance.  Besides,
            //  laziness is a virtue.
            //
            m_reaper = new LockReaper( m_engine );
            m_reaper.start();
        }

        synchronized( m_pageLocks )
        {
            fireEvent( WikiPageEvent.PAGE_LOCK, page.getName() ); // prior to or after actual lock?

            lock = (PageLock) m_pageLocks.get( page.getName() );

            if( lock == null )
            {
                //
                //  Lock is available, so make a lock.
                //
                Date d = new Date();
                lock = new PageLock( page, user, d,
                                     new Date( d.getTime() + m_expiryTime*60*1000L ) );

                m_pageLocks.put( page.getName(), lock );

                log.debug( "Locked page "+page.getName()+" for "+user);
            }
            else
            {
                log.debug( "Page "+page.getName()+" already locked by "+lock.getLocker() );
                lock = null; // Nothing to return
            }
        }

        return lock;
    }

    /**
     *  Marks a page free to be written again.  If there has not been a lock,
     *  will fail quietly.
     *
     *  @param lock A lock acquired in lockPage().  Safe to be null.
     */
    public void unlockPage( PageLock lock )
    {
        if( lock == null ) return;

        synchronized( m_pageLocks )
        {
            m_pageLocks.remove( lock.getPage() );

            log.debug( "Unlocked page "+lock.getPage() );
        }

        fireEvent( WikiPageEvent.PAGE_UNLOCK, lock.getPage() );
    }

    /**
     *  Returns the current lock owner of a page.  If the page is not
     *  locked, will return null.
     *
     *  @param page The page to check the lock for
     *  @return Current lock, or null, if there is no lock
     */
    public PageLock getCurrentLock( WikiPage page )
    {
        PageLock lock = null;

        synchronized( m_pageLocks )
        {
            lock = (PageLock)m_pageLocks.get( page.getName() );
        }

        return lock;
    }

    /**
     *  Returns a list of currently applicable locks.  Note that by the time you get the list,
     *  the locks may have already expired, so use this only for informational purposes.
     *
     *  @return List of PageLock objects, detailing the locks.  If no locks exist, returns
     *          an empty list.
     *  @since 2.0.22.
     */
    public List getActiveLocks()
    {
        ArrayList result = new ArrayList();

        synchronized( m_pageLocks )
        {
            for( Iterator i = m_pageLocks.values().iterator(); i.hasNext(); )
            {
                result.add( i.next() );
            }
        }

        return result;
    }

    /**
     *  Finds a WikiPage object describing a particular page and version.
     *  
     *  @param pageName  The name of the page
     *  @param version   A version number
     *  @return          A WikiPage object, or null, if the page does not exist
     *  @throws ProviderException If there is something wrong with the page 
     *                            name or the repository
     */
    public WikiPage getPageInfo( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name '"+pageName+"'");
        }

        WikiPage page = null;

        try
        {
            page = m_provider.getPageInfo( pageName, version );
        }
        catch( RepositoryModifiedException e )
        {
            //
            //  This only occurs with the latest version.
            //
            log.info("Repository has been modified externally while fetching info for "+pageName );

            WikiPage p = new WikiPage( m_engine, pageName );

            m_engine.updateReferences( p );

            page = m_provider.getPageInfo( pageName, version );
        }

        //
        //  Should update the metadata.
        //
        /*
        if( page != null && !page.hasMetadata() )
        {
            WikiContext ctx = new WikiContext(m_engine,page);
            m_engine.textToHTML( ctx, getPageText(pageName,version) );
        }
        */
        return page;
    }

    /**
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  
     *  @param pageName The name of the page to fetch history for
     *  @return If the page does not exist, returns null, otherwise a List
     *          of WikiPages.
     *  @throws ProviderException If the repository fails.
     */
    public List getVersionHistory( String pageName )
        throws ProviderException
    {
        if( pageExists( pageName ) )
        {
            return m_provider.getVersionHistory( pageName );
        }

        return null;
    }

    /**
     *  Returns a human-readable description of the current provider.
     *  
     *  @return A human-readable description.
     */
    public String getProviderDescription()
    {
        return m_provider.getProviderInfo();
    }

    /**
     *  Returns the total count of all pages in the repository. This
     *  method is equivalent of calling getAllPages().size(), but
     *  it swallows the ProviderException and returns -1 instead of
     *  any problems.
     *  
     *  @return The number of pages, or -1, if there is an error.
     */
    public int getTotalPageCount()
    {
        try
        {
            return m_provider.getAllPages().size();
        }
        catch( ProviderException e )
        {
            log.error( "Unable to count pages: ",e );
            return -1;
        }
    }

    /**
     *  Returns true, if the page exists (any version).
     *  
     *  @param pageName  Name of the page.
     *  @return A boolean value describing the existence of a page
     *  @throws ProviderException If the backend fails or the name is illegal.
     */
    public boolean pageExists( String pageName )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        return m_provider.pageExists( pageName );
    }

    /**
     *  Checks for existence of a specific page and version.
     *  
     *  @since 2.3.29
     *  @param pageName Name of the page
     *  @param version The version to check
     *  @return <code>true</code> if the page exists, <code>false</code> otherwise
     *  @throws ProviderException If backend fails or name is illegal
     */
    public boolean pageExists( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        if( version == WikiProvider.LATEST_VERSION )
            return pageExists( pageName );

        if( m_provider instanceof CachingProvider )
        {
            return ((CachingProvider)m_provider).pageExists( pageName , version );
        }

        return m_provider.getPageInfo( pageName, version ) != null;
    }

    /**
     *  Deletes only a specific version of a WikiPage.
     *  
     *  @param page The page to delete.
     *  @throws ProviderException if the page fails
     */
    public void deleteVersion( WikiPage page )
        throws ProviderException
    {
        m_provider.deleteVersion( page.getName(), page.getVersion() );

        // FIXME: If this was the latest, reindex Lucene
        // FIXME: Update RefMgr
    }

    /**
     *  Deletes an entire page, all versions, all traces.
     *  
     *  @param page The WikiPage to delete
     *  @throws ProviderException If the repository operation fails
     */
    public void deletePage( WikiPage page )
        throws ProviderException
    {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );

        m_provider.deletePage( page.getName() );

        fireEvent( WikiPageEvent.PAGE_DELETED, page.getName() );
    }

    /**
     *  This is a simple reaper thread that runs roughly every minute
     *  or so (it's not really that important, as long as it runs),
     *  and removes all locks that have expired.
     */
    private class LockReaper extends WikiBackgroundThread
    {
        public LockReaper( WikiEngine engine )
        {
            super( engine, 60 );
            setName("JSPWiki Lock Reaper");
        }

        public void backgroundTask() throws Exception
        {
            synchronized( m_pageLocks )
            {
                Collection entries = m_pageLocks.values();

                Date now = new Date();

                for( Iterator i = entries.iterator(); i.hasNext(); )
                {
                    PageLock p = (PageLock) i.next();

                    if( now.after( p.getExpiryTime() ) )
                    {
                        i.remove();

                        log.debug( "Reaped lock: "+p.getPage()+
                                   " by "+p.getLocker()+
                                   ", acquired "+p.getAcquisitionTime()+
                                   ", and expired "+p.getExpiryTime() );
                    }
                }
            }
        }
    }

    // workflow task inner classes....................................................

    /**
     * Inner class that handles the page pre-save actions. If the proposed page
     * text is the same as the current version, the {@link #execute()} method
     * returns {@link com.ecyrd.jspwiki.workflow.Outcome#STEP_ABORT}. Any
     * WikiExceptions thrown by page filters will be re-thrown, and the workflow
     * will abort.
     *
     * @author Andrew Jaquith
     */
    public static class PreSaveWikiPageTask extends Task
    {
        private final WikiContext m_context;
        private final String m_proposedText;

        public PreSaveWikiPageTask( WikiContext context, String proposedText )
        {
            super( PRESAVE_TASK_MESSAGE_KEY );
            m_context = context;
            m_proposedText = proposedText;
        }

        public Outcome execute() throws WikiException
        {
            // Retrieve attributes
            WikiEngine engine = m_context.getEngine();
            Workflow workflow = getWorkflow();

            // Get the wiki page
            WikiPage page = m_context.getPage();

            // Figure out who the author was. Prefer the author
            // set programmatically; otherwise get from the
            // current logged in user
            if ( page.getAuthor() == null )
            {
                Principal wup = m_context.getCurrentUser();

                if ( wup != null )
                    page.setAuthor( wup.getName() );
            }

            // Run the pre-save filters. If any exceptions, add error to list, abort, and redirect
            String saveText;
            try
            {
                saveText = engine.getFilterManager().doPreSaveFiltering( m_context, m_proposedText );
            }
            catch ( FilterException e )
            {
                throw e;
            }

            // Stash the wiki context, old and new text as workflow attributes
            workflow.setAttribute( PRESAVE_WIKI_CONTEXT, m_context );
            workflow.setAttribute( FACT_PROPOSED_TEXT, saveText );
            return Outcome.STEP_COMPLETE;
        }
    }

    /**
     * Inner class that handles the actual page save and post-save actions. Instances
     * of this class are assumed to have been added to an approval workflow via
     * {@link com.ecyrd.jspwiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, com.ecyrd.jspwiki.workflow.Fact[], Task, String)};
     * they will not function correctly otherwise.
     *
     * @author Andrew Jaquith
     */
    public static class SaveWikiPageTask extends Task
    {
        public SaveWikiPageTask()
        {
            super( SAVE_TASK_MESSAGE_KEY );
        }

        public Outcome execute() throws WikiException
        {
            // Retrieve attributes
            WikiContext context = (WikiContext) getWorkflow().getAttribute( PRESAVE_WIKI_CONTEXT );
            String proposedText = (String) getWorkflow().getAttribute( FACT_PROPOSED_TEXT );

            WikiEngine engine = context.getEngine();
            WikiPage page = context.getPage();

            // Let the rest of the engine handle actual saving.
            engine.getPageManager().putPageText( page, proposedText );

            // Refresh the context for post save filtering.
            engine.getPage( page.getName() );
            engine.textToHTML( context, proposedText );
            engine.getFilterManager().doPostSaveFiltering( context, proposedText );

            return Outcome.STEP_COMPLETE;
        }
    }

    // events processing .......................................................

    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners.
     *
     * @see com.ecyrd.jspwiki.event.WikiPageEvent
     * @param type       the event type to be fired
     * @param pagename   the wiki page name as a String
     */
    protected final void fireEvent( int type, String pagename )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiPageEvent(m_engine,type,pagename));
        }
    }

    /**
     *  {@inheritDoc}
     */
    public Collection modules()
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     *  Listens for {@link com.ecyrd.jspwiki.event.WikiSecurityEvent#PROFILE_NAME_CHANGED}
     *  events. If a user profile's name changes, each page ACL is inspected. If an entry contains
     *  a name that has changed, it is replaced with the new one. No events are emitted
     *  as a consequence of this method, because the page contents are still the same; it is
     *  only the representations of the names within the ACL that are changing.
     * 
     *  @param event The event
     */
    public void actionPerformed(WikiEvent event)
    {
        if (! ( event instanceof WikiSecurityEvent ) )
        {
            return;
        }

        WikiSecurityEvent se = (WikiSecurityEvent)event;
        if ( se.getType() == WikiSecurityEvent.PROFILE_NAME_CHANGED )
        {
            UserProfile[] profiles = (UserProfile[])se.getTarget();
            Principal[] oldPrincipals = new Principal[]
                { new WikiPrincipal( profiles[0].getLoginName() ),
                  new WikiPrincipal( profiles[0].getFullname() ),
                  new WikiPrincipal( profiles[0].getWikiName() ) };
            Principal newPrincipal = new WikiPrincipal( profiles[1].getFullname() );

            // Examine each page ACL
            try
            {
                int pagesChanged = 0;
                Collection pages = getAllPages();
                for ( Iterator it = pages.iterator(); it.hasNext(); )
                {
                    WikiPage page = (WikiPage)it.next();
                    boolean aclChanged = changeAcl( page, oldPrincipals, newPrincipal );
                    if ( aclChanged )
                    {
                        // If the Acl needed changing, change it now
                        try
                        {
                            m_engine.getAclManager().setPermissions( page, page.getAcl() );
                        }
                        catch ( WikiSecurityException e )
                        {
                            log.error( "Could not change page ACL for page " + page.getName() + ": " + e.getMessage() );
                        }
                        pagesChanged++;
                    }
                }
                log.info( "Profile name change for '" + newPrincipal.toString() +
                          "' caused " + pagesChanged + " page ACLs to change also." );
            }
            catch ( ProviderException e )
            {
                // Oooo! This is really bad...
                log.error( "Could not change user name in Page ACLs because of Provider error:" + e.getMessage() );
            }
        }
    }

    /**
     *  For a single wiki page, replaces all Acl entries matching a supplied array of Principals 
     *  with a new Principal.
     * 
     *  @param page the wiki page whose Acl is to be modified
     *  @param oldPrincipals an array of Principals to replace; all AclEntry objects whose
     *   {@link AclEntry#getPrincipal()} method returns one of these Principals will be replaced
     *  @param newPrincipal the Principal that should receive the old Principals' permissions
     *  @return <code>true</code> if the Acl was actually changed; <code>false</code> otherwise
     */
    protected boolean changeAcl( WikiPage page, Principal[] oldPrincipals, Principal newPrincipal )
    {
        Acl acl = page.getAcl();
        boolean pageChanged = false;
        if ( acl != null )
        {
            Enumeration entries = acl.entries();
            Collection entriesToAdd = new ArrayList();
            Collection entriesToRemove = new ArrayList();
            while ( entries.hasMoreElements() )
            {
                AclEntry entry = (AclEntry)entries.nextElement();
                if ( ArrayUtils.contains( oldPrincipals, entry.getPrincipal() ) )
                {
                    // Create new entry
                    AclEntry newEntry = new AclEntryImpl();
                    newEntry.setPrincipal( newPrincipal );
                    Enumeration permissions = entry.permissions();
                    while ( permissions.hasMoreElements() )
                    {
                        Permission permission = (Permission)permissions.nextElement();
                        newEntry.addPermission(permission);
                    }
                    pageChanged = true;
                    entriesToRemove.add( entry );
                    entriesToAdd.add( newEntry );
                }
            }
            for ( Iterator ix = entriesToRemove.iterator(); ix.hasNext(); )
            {
                AclEntry entry = (AclEntry)ix.next();
                acl.removeEntry( entry );
            }
            for ( Iterator ix = entriesToAdd.iterator(); ix.hasNext(); )
            {
                AclEntry entry = (AclEntry)ix.next();
                acl.addEntry( entry );
            }
        }
        return pageChanged;
    }

}
