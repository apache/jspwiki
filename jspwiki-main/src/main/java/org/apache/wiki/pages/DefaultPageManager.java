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
package org.apache.wiki.pages;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.AclEntryImpl;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.event.WikiSecurityEvent;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.providers.RepositoryModifiedException;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.workflow.Decision;
import org.apache.wiki.workflow.DecisionRequiredException;
import org.apache.wiki.workflow.Fact;
import org.apache.wiki.workflow.Step;
import org.apache.wiki.workflow.Workflow;
import org.apache.wiki.workflow.WorkflowBuilder;
import org.apache.wiki.workflow.WorkflowManager;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Manages the WikiPages.  This class functions as an unified interface towards the page providers.  It handles initialization
 * and management of the providers, and provides utility methods for accessing the contents.
 * <p/>
 * Saving a page is a two-stage Task; first the pre-save operations and then the actual save.  See the descriptions of the tasks
 * for further information.
 *
 * @since 2.0
 */
public class DefaultPageManager extends ModuleManager implements PageManager {

    private static final Logger LOG = Logger.getLogger( DefaultPageManager.class );

    private WikiPageProvider m_provider;

    protected ConcurrentHashMap< String, PageLock > m_pageLocks = new ConcurrentHashMap<>();

    private int m_expiryTime;

    private LockReaper m_reaper = null;

    private PageSorter pageSorter = new PageSorter();

    /**
     * Creates a new PageManager.
     *
     * @param engine WikiEngine instance
     * @param props  Properties to use for initialization
     * @throws NoSuchElementException {@value #PROP_PAGEPROVIDER} property not found on WikiEngine properties
     * @throws WikiException If anything goes wrong, you get this.
     */
    public DefaultPageManager(final WikiEngine engine, final Properties props) throws NoSuchElementException, WikiException {
        super(engine);
        final String classname;
        m_engine = engine;
        final boolean useCache = "true".equals(props.getProperty(PROP_USECACHE));

        m_expiryTime = TextUtil.parseIntParameter(props.getProperty(PROP_LOCKEXPIRY), 60);

        //
        //  If user wants to use a cache, then we'll use the CachingProvider.
        //
        if (useCache) {
            classname = "org.apache.wiki.providers.CachingProvider";
        } else {
            classname = TextUtil.getRequiredProperty(props, PROP_PAGEPROVIDER);
        }

        pageSorter.initialize( props );

        try {
            LOG.debug("Page provider class: '" + classname + "'");
            final Class<?> providerclass = ClassUtil.findClass("org.apache.wiki.providers", classname);
            m_provider = (WikiPageProvider) providerclass.newInstance();

            LOG.debug("Initializing page provider class " + m_provider);
            m_provider.initialize(m_engine, props);
        } catch (final ClassNotFoundException e) {
            LOG.error("Unable to locate provider class '" + classname + "' (" + e.getMessage() + ")", e);
            throw new WikiException("No provider class. (" + e.getMessage() + ")", e);
        } catch (final InstantiationException e) {
            LOG.error("Unable to create provider class '" + classname + "' (" + e.getMessage() + ")", e);
            throw new WikiException("Faulty provider class. (" + e.getMessage() + ")", e);
        } catch (final IllegalAccessException e) {
            LOG.error("Illegal access to provider class '" + classname + "' (" + e.getMessage() + ")", e);
            throw new WikiException("Illegal provider class. (" + e.getMessage() + ")", e);
        } catch (final NoRequiredPropertyException e) {
            LOG.error("Provider did not found a property it was looking for: " + e.getMessage(), e);
            throw e;  // Same exception works.
        } catch (final IOException e) {
            LOG.error("An I/O exception occurred while trying to create a new page provider: " + classname, e);
            throw new WikiException("Unable to start page provider: " + e.getMessage(), e);
        }

    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getProvider()
     */
    @Override
    public WikiPageProvider getProvider() {
        return m_provider;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getAllPages()
     */
    @Override
    public Collection< WikiPage > getAllPages() throws ProviderException {
        return m_provider.getAllPages();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getPageText(java.lang.String, int)
     */
    @Override
    public String getPageText( final String pageName, final int version ) throws ProviderException {
        if (pageName == null || pageName.length() == 0) {
            throw new ProviderException( "Illegal page name" );
        }
        String text;

        try {
            text = m_provider.getPageText( pageName, version );
        } catch ( final RepositoryModifiedException e ) {
            //  This only occurs with the latest version.
            LOG.info( "Repository has been modified externally while fetching page " + pageName );

            //  Empty the references and yay, it shall be recalculated
            final WikiPage p = m_provider.getPageInfo( pageName, version );

            m_engine.getReferenceManager().updateReferences( p );
            m_engine.getSearchManager().reindexPage( p );
            text = m_provider.getPageText( pageName, version );
        }

        return text;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getPureText(String, int)
     */
    public String getPureText( final String page, final int version ) {
        String result = null;
        try {
            result = getPageText( page, version );
        } catch( final ProviderException e ) {
            LOG.error( "ProviderException getPureText for page " + page + " [version " + version + "]", e );
        } finally {
            if( result == null ) {
                result = "";
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getText(String, int)
     */
    public String getText( final String page, final int version ) {
        final String result = getPureText( page, version );
        return TextUtil.replaceEntities( result );
    }

    public void saveText( final WikiContext context, final String text ) throws WikiException {
        // Check if page data actually changed; bail if not
        final WikiPage page = context.getPage();
        final String oldText = getPureText( page );
        final String proposedText = TextUtil.normalizePostData( text );
        if ( oldText != null && oldText.equals( proposedText ) ) {
            return;
        }

        // Check if creation of empty pages is allowed; bail if not
        final boolean allowEmpty = TextUtil.getBooleanProperty( m_engine.getWikiProperties(),
                                                                WikiEngine.PROP_ALLOW_CREATION_OF_EMPTY_PAGES,
                                                         false );
        if ( !allowEmpty && !wikiPageExists( page ) && text.trim().equals( "" ) ) {
            return;
        }

        // Create approval workflow for page save; add the diffed, proposed and old text versions as
        // Facts for the approver (if approval is required). If submitter is authenticated, any reject
        // messages will appear in his/her workflow inbox.
        final WorkflowBuilder builder = WorkflowBuilder.getBuilder( m_engine );
        final Principal submitter = context.getCurrentUser();
        final Step prepTask = m_engine.getTasksManager().buildPreSaveWikiPageTask( context, proposedText );
        final Step completionTask = m_engine.getTasksManager().buildSaveWikiPageTask();
        final String diffText = m_engine.getDifferenceManager().makeDiff( context, oldText, proposedText );
        final boolean isAuthenticated = context.getWikiSession().isAuthenticated();
        final Fact[] facts = new Fact[ 5 ];
        facts[ 0 ] = new Fact( WorkflowManager.WF_WP_SAVE_FACT_PAGE_NAME, page.getName() );
        facts[ 1 ] = new Fact( WorkflowManager.WF_WP_SAVE_FACT_DIFF_TEXT, diffText );
        facts[ 2 ] = new Fact( WorkflowManager.WF_WP_SAVE_FACT_PROPOSED_TEXT, proposedText );
        facts[ 3 ] = new Fact( WorkflowManager.WF_WP_SAVE_FACT_CURRENT_TEXT, oldText);
        facts[ 4 ] = new Fact( WorkflowManager.WF_WP_SAVE_FACT_IS_AUTHENTICATED, isAuthenticated );
        final String rejectKey = isAuthenticated ? WorkflowManager.WF_WP_SAVE_REJECT_MESSAGE_KEY : null;
        final Workflow workflow = builder.buildApprovalWorkflow( submitter,
                                                                 WorkflowManager.WF_WP_SAVE_APPROVER,
                                                                 prepTask,
                                                                 WorkflowManager.WF_WP_SAVE_DECISION_MESSAGE_KEY,
                                                                 facts,
                                                                 completionTask,
                                                                 rejectKey );
        m_engine.getWorkflowManager().start( workflow );

        // Let callers know if the page-save requires approval
        if ( workflow.getCurrentStep() instanceof Decision ) {
            throw new DecisionRequiredException( "The page contents must be approved before they become active." );
        }
    }

    /**
     * Returns the WikiEngine to which this PageManager belongs to.
     *
     * @return The WikiEngine object.
     */
    protected WikiEngine getEngine() {
        return m_engine;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#putPageText(org.apache.wiki.WikiPage, java.lang.String)
     */
    @Override
    public void putPageText( final WikiPage page, final String content ) throws ProviderException {
        if (page == null || page.getName() == null || page.getName().length() == 0) {
            throw new ProviderException("Illegal page name");
        }

        m_provider.putPageText(page, content);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#lockPage(org.apache.wiki.WikiPage, java.lang.String)
     */
    @Override
    public PageLock lockPage( final WikiPage page, final String user) {
        if (m_reaper == null) {
            //
            //  Start the lock reaper lazily.  We don't want to start it in
            //  the constructor, because starting threads in constructors
            //  is a bad idea when it comes to inheritance.  Besides,
            //  laziness is a virtue.
            //
            m_reaper = new LockReaper(m_engine);
            m_reaper.start();
        }

        fireEvent(WikiPageEvent.PAGE_LOCK, page.getName()); // prior to or after actual lock?
        PageLock lock = m_pageLocks.get(page.getName());

        if (lock == null) {
            //
            //  Lock is available, so make a lock.
            //
            final Date d = new Date();
            lock = new PageLock(page, user, d, new Date(d.getTime() + m_expiryTime * 60 * 1000L));
            m_pageLocks.put(page.getName(), lock);
            LOG.debug("Locked page " + page.getName() + " for " + user);
        } else {
            LOG.debug("Page " + page.getName() + " already locked by " + lock.getLocker());
            lock = null; // Nothing to return
        }

        return lock;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#unlockPage(org.apache.wiki.pages.PageLock)
     */
    @Override
    public void unlockPage( final PageLock lock ) {
        if (lock == null) {
            return;
        }

        m_pageLocks.remove( lock.getPage() );
        LOG.debug( "Unlocked page " + lock.getPage() );

        fireEvent( WikiPageEvent.PAGE_UNLOCK, lock.getPage() );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getCurrentLock(org.apache.wiki.WikiPage)
     */
    @Override
    public PageLock getCurrentLock( final WikiPage page ) {
        return m_pageLocks.get( page.getName() );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getActiveLocks()
     */
    @Override
    public List< PageLock > getActiveLocks() {
        return  new ArrayList<>( m_pageLocks.values() );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getPage(java.lang.String)
     */
    public WikiPage getPage( final String pagereq ) {
        return getPage( pagereq, WikiProvider.LATEST_VERSION );
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getPage(java.lang.String, int)
     */
    public WikiPage getPage( final String pagereq, final int version ) {
        try {
            WikiPage p = getPageInfo( pagereq, version );
            if( p == null ) {
                p = m_engine.getAttachmentManager().getAttachmentInfo( null, pagereq );
            }

            return p;
        } catch( final ProviderException e ) {
            LOG.error( "Unable to fetch page info for " + pagereq + " [version " + version + "]", e );
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getPageInfo(java.lang.String, int)
     */
    @Override
    public WikiPage getPageInfo( final String pageName, final int version) throws ProviderException {
        if (pageName == null || pageName.length() == 0) {
            throw new ProviderException("Illegal page name '" + pageName + "'");
        }

        WikiPage page;

        try {
            page = m_provider.getPageInfo(pageName, version);
        } catch ( final RepositoryModifiedException e) {
            //  This only occurs with the latest version.
            LOG.info("Repository has been modified externally while fetching info for " + pageName);
            page = m_provider.getPageInfo(pageName, version);
            if (page != null) {
                m_engine.getReferenceManager().updateReferences(page);
            } else {
                m_engine.getReferenceManager().pageRemoved(new WikiPage(m_engine, pageName));
            }
        }

        return page;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getVersionHistory(java.lang.String)
     */
    @Override
    public < T extends WikiPage > List< T > getVersionHistory( final String pageName ) {
        List< T > c = null;

        try {
            if( pageExists( pageName ) ) {
                c = (List< T >)m_provider.getVersionHistory( pageName );
            }

            if( c == null ) {
                c = (List< T >)m_engine.getAttachmentManager().getVersionHistory( pageName );
            }
        } catch( final ProviderException e ) {
            LOG.error( "ProviderException requesting version history for " + pageName, e );
        }

        return c;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getCurrentProvider()
     */
    public String getCurrentProvider() {
        return getProvider().getClass().getName();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getProviderDescription()
     */
    @Override
    public String getProviderDescription() {
        return m_provider.getProviderInfo();
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getTotalPageCount()
     */
    @Override
    public int getTotalPageCount() {
        try {
            return m_provider.getAllPages().size();
        } catch( final ProviderException e ) {
            LOG.error( "Unable to count pages: ", e );
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#getRecentChanges()
     */
    @Override
    public Set< WikiPage > getRecentChanges() {
        try {
            final TreeSet< WikiPage > sortedPages = new TreeSet<>( new PageTimeComparator() );
            sortedPages.addAll( getAllPages() );
            sortedPages.addAll( m_engine.getAttachmentManager().getAllAttachments() );

            return sortedPages;
        } catch( final ProviderException e ) {
            LOG.error( "Unable to fetch all pages: ", e );
            return Collections.emptySet();
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#pageExists(java.lang.String)
     */
    @Override
    public boolean pageExists( final String pageName ) throws ProviderException {
        if (pageName == null || pageName.length() == 0) {
            throw new ProviderException("Illegal page name");
        }

        return m_provider.pageExists(pageName);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#pageExists(java.lang.String, int)
     */
    @Override
    public boolean pageExists( final String pageName, final int version ) throws ProviderException {
        if (pageName == null || pageName.length() == 0) {
            throw new ProviderException("Illegal page name");
        }

        if (version == WikiProvider.LATEST_VERSION) {
            return pageExists(pageName);
        }

        return m_provider.pageExists(pageName, version);
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#wikiPageExists(java.lang.String)
     */
    public boolean wikiPageExists( final String page ) {
        if( m_engine.getCommandResolver().getSpecialPageReference( page ) != null ) {
            return true;
        }

        Attachment att = null;
        try {
            if( m_engine.getFinalPageName( page ) != null ) {
                return true;
            }

            att = m_engine.getAttachmentManager().getAttachmentInfo( null, page );
        } catch( final ProviderException e ) {
            LOG.debug( "pageExists() failed to find attachments", e );
        }

        return att != null;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#wikiPageExists(java.lang.String, int)
     */
    public boolean wikiPageExists( final String page, final int version ) throws ProviderException {
        if( m_engine.getCommandResolver().getSpecialPageReference( page ) != null ) {
            return true;
        }

        boolean isThere = false;
        final String finalName = m_engine.getFinalPageName( page );
        if( finalName != null ) {
            isThere = pageExists( finalName, version );
        }

        if( !isThere ) {
            //  Go check if such an attachment exists.
            try {
                isThere = m_engine.getAttachmentManager().getAttachmentInfo( null, page, version ) != null;
            } catch( final ProviderException e ) {
                LOG.debug( "wikiPageExists() failed to find attachments", e );
            }
        }

        return isThere;
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#deleteVersion(org.apache.wiki.WikiPage)
     */
    @Override
    public void deleteVersion( final WikiPage page ) throws ProviderException {
        if( page instanceof Attachment ) {
            m_engine.getAttachmentManager().deleteVersion( ( Attachment )page );
        } else {
            m_provider.deleteVersion( page.getName(), page.getVersion() );
            // FIXME: If this was the latest, reindex Lucene, update RefMgr
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#deletePage(java.lang.String)
     */
    public void deletePage( final String pageName ) throws ProviderException {
        final WikiPage p = getPage( pageName );
        if( p != null ) {
            if( p instanceof Attachment ) {
                m_engine.getAttachmentManager().deleteAttachment( ( Attachment )p );
            } else {
                final Collection< String > refTo = m_engine.getReferenceManager().findRefersTo( pageName );
                // May return null, if the page does not exist or has not been indexed yet.

                if( m_engine.getAttachmentManager().hasAttachments( p ) ) {
                    final List< Attachment > attachments = m_engine.getAttachmentManager().listAttachments( p );
                    for( final Attachment attachment : attachments ) {
                        if( refTo != null ) {
                            refTo.remove( attachment.getName() );
                        }

                        m_engine.getAttachmentManager().deleteAttachment( attachment );
                    }
                }
                deletePage( p );
                fireEvent( WikiPageEvent.PAGE_DELETED, pageName );
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.apache.wiki.pages.PageManager#deletePage(org.apache.wiki.WikiPage)
     */
    @Override
    public void deletePage( final WikiPage page ) throws ProviderException {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );
        m_provider.deletePage( page.getName() );
        fireEvent( WikiPageEvent.PAGE_DELETED, page.getName() );
    }

    /**
     * This is a simple reaper thread that runs roughly every minute
     * or so (it's not really that important, as long as it runs),
     * and removes all locks that have expired.
     */
    private class LockReaper extends WikiBackgroundThread {
        /**
         * Create a LockReaper for a given engine.
         *
         * @param engine WikiEngine to own this thread.
         */
        public LockReaper( final WikiEngine engine) {
            super(engine, 60);
            setName("JSPWiki Lock Reaper");
        }

        @Override
        public void backgroundTask() {
            final Collection< PageLock > entries = m_pageLocks.values();
            for( final Iterator<PageLock> i = entries.iterator(); i.hasNext(); ) {
                final PageLock p = i.next();

                if ( p.isExpired() ) {
                    i.remove();

                    LOG.debug( "Reaped lock: " + p.getPage() +
                               " by " + p.getLocker() +
                               ", acquired " + p.getAcquisitionTime() +
                               ", and expired " + p.getExpiryTime() );
                }
            }
        }
    }

    // events processing .......................................................

    /**
     * Fires a WikiPageEvent of the provided type and page name
     * to all registered listeners.
     *
     * @param type     the event type to be fired
     * @param pagename the wiki page name as a String
     * @see org.apache.wiki.event.WikiPageEvent
     */
    protected final void fireEvent( final int type, final String pagename ) {
        if( WikiEventManager.isListening( this ) ) {
            WikiEventManager.fireEvent( this, new WikiPageEvent( m_engine, type, pagename ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection< WikiModuleInfo > modules() {
        return new ArrayList<>();
    }

    /**
     * Returns null!
     *  {@inheritDoc}
     */
    @Override
    public WikiModuleInfo getModuleInfo( final String moduleName ) {
    	return null;
    }

    /**
     * Listens for {@link org.apache.wiki.event.WikiSecurityEvent#PROFILE_NAME_CHANGED}
     * events. If a user profile's name changes, each page ACL is inspected. If an entry contains
     * a name that has changed, it is replaced with the new one. No events are emitted
     * as a consequence of this method, because the page contents are still the same; it is
     * only the representations of the names within the ACL that are changing.
     *
     * @param event The event
     */
    @Override
    public void actionPerformed( final WikiEvent event ) {
        if( !( event instanceof WikiSecurityEvent ) ) {
            return;
        }

        final WikiSecurityEvent se = ( WikiSecurityEvent ) event;
        if( se.getType() == WikiSecurityEvent.PROFILE_NAME_CHANGED ) {
            final UserProfile[] profiles = (UserProfile[]) se.getTarget();
            final Principal[] oldPrincipals = new Principal[] { new WikiPrincipal( profiles[ 0 ].getLoginName() ),
                                                                new WikiPrincipal( profiles[ 0 ].getFullname()),
                                                                new WikiPrincipal( profiles[ 0 ].getWikiName() ) };
            final Principal newPrincipal = new WikiPrincipal( profiles[ 1 ].getFullname() );

            // Examine each page ACL
            try {
                int pagesChanged = 0;
                final Collection< WikiPage > pages = getAllPages();
                for( final WikiPage page : pages ) {
                    final boolean aclChanged = changeAcl( page, oldPrincipals, newPrincipal );
                    if( aclChanged ) {
                        // If the Acl needed changing, change it now
                        try {
                            m_engine.getAclManager().setPermissions( page, page.getAcl() );
                        } catch( final WikiSecurityException e ) {
                            LOG.error("Could not change page ACL for page " + page.getName() + ": " + e.getMessage(), e);
                        }
                        pagesChanged++;
                    }
                }
                LOG.info( "Profile name change for '" + newPrincipal.toString() + "' caused " + pagesChanged + " page ACLs to change also." );
            } catch( final ProviderException e ) {
                // Oooo! This is really bad...
                LOG.error( "Could not change user name in Page ACLs because of Provider error:" + e.getMessage(), e );
            }
        }
    }

    /**
     * For a single wiki page, replaces all Acl entries matching a supplied array of Principals with a new Principal.
     *
     * @param page the wiki page whose Acl is to be modified
     * @param oldPrincipals an array of Principals to replace; all AclEntry objects whose {@link AclEntry#getPrincipal()} method returns
     *                      one of these Principals will be replaced
     * @param newPrincipal the Principal that should receive the old Principals' permissions
     * @return <code>true</code> if the Acl was actually changed; <code>false</code> otherwise
     */
    protected boolean changeAcl( final WikiPage page, final Principal[] oldPrincipals, final Principal newPrincipal ) {
        final Acl acl = page.getAcl();
        boolean pageChanged = false;
        if( acl != null ) {
            final Enumeration< AclEntry > entries = acl.entries();
            final Collection< AclEntry > entriesToAdd = new ArrayList<>();
            final Collection< AclEntry > entriesToRemove = new ArrayList<>();
            while( entries.hasMoreElements() ) {
                final AclEntry entry = entries.nextElement();
                if( ArrayUtils.contains(oldPrincipals, entry.getPrincipal() ) ) {
                    // Create new entry
                    final AclEntry newEntry = new AclEntryImpl();
                    newEntry.setPrincipal( newPrincipal );
                    final Enumeration<Permission> permissions = entry.permissions();
                    while( permissions.hasMoreElements() ) {
                        final Permission permission = permissions.nextElement();
                        newEntry.addPermission(permission);
                    }
                    pageChanged = true;
                    entriesToRemove.add(entry);
                    entriesToAdd.add(newEntry);
                }
            }
            for( final AclEntry entry : entriesToRemove ) {
                acl.removeEntry( entry );
            }
            for( final AclEntry entry : entriesToAdd ) {
                acl.addEntry( entry );
            }
        }
        return pageChanged;
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.pages.PageManager#getPageSorter()
     */
    @Override
    public PageSorter getPageSorter() {
        return pageSorter;
    }

}
