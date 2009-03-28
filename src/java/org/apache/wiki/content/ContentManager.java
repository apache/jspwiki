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

import java.security.Permission;
import java.security.Principal;
import java.util.*;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wiki.*;
import org.apache.wiki.api.FilterException;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.auth.acl.AclEntry;
import org.apache.wiki.auth.acl.AclEntryImpl;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.event.*;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.util.TextUtil;
import org.apache.wiki.util.WikiBackgroundThread;
import org.apache.wiki.workflow.Outcome;
import org.apache.wiki.workflow.Task;
import org.apache.wiki.workflow.Workflow;
import org.priha.RepositoryManager;
import org.priha.util.ConfigurationException;


/**
 *  Provides access to the content repository.  Unlike previously, in JSPWiki
 *  3.0 all content is managed by this single repository, and we use the MIME
 *  type of the content to determine what kind of content it actually is.
 *  <p>
 *  The underlying content is stored in a JCR Repository object.  JSPWiki
 *  will first try to locate a Repository object using JNDI, under the
 *  "java:comp/env/jcr/repository" name.  If this fails, it will try to see
 *  if there is a property called "jspwiki.repository" defined in jspwiki.properties.
 *  Current allowed values are "priha" for the <a href="http://www.priha.org/">Priha content repository</a>,
 *  and "jackrabbit" for <a href="http://jackrabbit.apache.org">Apache Jackrabbit</a>.
 *  <p>
 *  If there is no property defined, defaults to "priha".
 *  <p>
 *  The methods in this class always return valid values. In case they cannot
 *  be acquired by some means, it throws an Exception.  This is different from
 *  the way the old PageManager used to work, so you can go ahead and get rid
 *  of all the null-checks from your old code.
 *
 *  FIXME:
 *    * This class is currently designed to be a drop-in replacement for PageManager.
 *      However, this brings in a lot of unfortunate side effects in complexity, and
 *      it does not really take advantage of all of the JCR API.  Therefore, this
 *      class should be treated as extremely volatile.
 *  
 *  @since 3.0
 */

public class ContentManager implements WikiEventListener
{
    /**
     *  The name of the default WikiSpace.
     */
    public static final String DEFAULT_SPACE = "Main";
    
    private static final String JCR_DEFAULT_SPACE = "pages/"+DEFAULT_SPACE;

    private static final String JCR_PAGES_NODE = "pages";

    private static final long serialVersionUID = 2L;
    
    /** The property value for setting the amount of time before the page locks expire. 
     *  Value is {@value}.
     */
    public static final String PROP_LOCKEXPIRY   = "jspwiki.lockExpiryTime";
    
    /** The message key for storing the text for the presave task.  Value is <tt>{@value}</tt>*/
    public static final String PRESAVE_TASK_MESSAGE_KEY = "task.preSaveWikiPage";
    
    /** The workflow attribute which stores the wikiContext. */
    public static final String PRESAVE_WIKI_CONTEXT = "wikiContext";
    
    /** The name of the key from jspwiki.properties which defines who shall approve
     *  the workflow of storing a wikipage.  Value is <tt>{@value}</tt>*/
    public static final String SAVE_APPROVER             = "workflow.saveWikiPage";
    
    /** The message key for storing the Decision text for saving a page.  Value is {@value}. */
    public static final String SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    
    /** The message key for rejecting the decision to save the page.  Value is {@value}. */
    public static final String SAVE_REJECT_MESSAGE_KEY   = "notification.saveWikiPage.reject";
    
    /** The message key of the text to finally approve a page save.  Value is {@value}. */
    public static final String SAVE_TASK_MESSAGE_KEY     = "task.saveWikiPage";
    
    /** Fact name for storing the page name.  Value is {@value}. */
    public static final String FACT_PAGE_NAME = "fact.pageName";
    
    /** Fact name for storing a diff text. Value is {@value}. */
    public static final String FACT_DIFF_TEXT = "fact.diffText";
    
    /** Fact name for storing the current text.  Value is {@value}. */
    public static final String FACT_CURRENT_TEXT = "fact.currentText";
    
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    public static final String FACT_PROPOSED_TEXT = "fact.proposedText";
    
    /** Fact name for storing whether the user is authenticated or not.  Value is {@value}. */
    public static final String FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    /** MIME type for JSPWiki markup content. */
    public static final String JSPWIKI_CONTENT_TYPE = "text/x-jspwiki";

    private static final String NS_JSPWIKI = "http://www.jspwiki.org/ns#";
    
    private static final String DEFAULT_WORKSPACE = "jspwiki";
    
    static Logger log = LoggerFactory.getLogger( ContentManager.class );

    protected HashMap<String,PageLock> m_pageLocks = new HashMap<String,PageLock>();

    private WikiEngine m_engine;

    private int m_expiryTime = 60;

    private LockReaper m_reaper = null;

    private Repository m_repository;
    
    private String m_workspaceName = DEFAULT_WORKSPACE; // FIXME: Make settable
    
    private JCRSessionManager m_sessionManager = new JCRSessionManager();
    
    /**
     *  Creates a new PageManager.
     *  
     *  @param engine WikiEngine instance
     *  @throws WikiException If anything goes wrong, you get this.
     */
    @SuppressWarnings("unchecked")
    public ContentManager( WikiEngine engine )
        throws WikiException
    {
        m_engine = engine;

        m_expiryTime = TextUtil.parseIntParameter( engine.getWikiProperties().getProperty( PROP_LOCKEXPIRY ), 60 );

        InitialContext context;
        try
        {
            //
            //  Attempt to locate the repository object from the JNDI using
            //  "java:comp/env/jcr/repository" name
            //
            context = new InitialContext();
            
            Context environment = (Context) context.lookup("java:comp/env");
            m_repository = (Repository) environment.lookup("jcr/repository");
        }
        catch( NamingException e )
        {
            if( log.isDebugEnabled() )
                log.debug( "Unable to locate the repository from JNDI",e );
            else
                log.info( "Unable to locate the repository from JNDI, attempting to locate from jspwiki.properties" );
            
            String repositoryName = engine.getWikiProperties().getProperty( "jspwiki.repository", "priha" );
            
            log.info( "Trying repository "+repositoryName );
            
            if( "priha".equals(repositoryName) )
            {
                try
                {
                    // FIXME: Should really use reflection to find this class - this requires
                    //        an unnecessary compile-time presence of priha.jar.
                    m_repository = RepositoryManager.getRepository();
                }
                catch( ConfigurationException e1 )
                {
                    throw new WikiException( "Unable to initialize Priha as the main repository",e1);
                }
            }
            else if( "jackrabbit".equals(repositoryName) )
            {
                try
                {
                    Class<Repository> jackrabbitRepo = (Class<Repository>) Class.forName( "org.apache.jackrabbit.TransientRepository" );
                    m_repository = jackrabbitRepo.newInstance();
                }
                catch( ClassNotFoundException e1 )
                {
                    throw new WikiException("Jackrabbit libraries not found in the classpath",e1);
                }
                catch( InstantiationException e1 )
                {
                    throw new WikiException("Jackrabbit could not be initialized",e1);
                }
                catch( IllegalAccessException e1 )
                {
                    throw new WikiException("You do not have permission to access Jackrabbit",e1);
                }
                
            }
            else
            {
                throw new WikiException("Unable to initialize repository for repositorytype "+repositoryName);
            }
        }
        
        try
        {
            initialize();
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Failed to initialize the repository content",e);
        }
        
        log.info("ContentManager initialized!");
    }

    /**
     *  Initializes the repository, making sure that everything is in place.
     * @throws RepositoryException 
     * @throws LoginException 
     */
    private void initialize() throws LoginException, RepositoryException 
    {
        Session session = m_sessionManager.getSession();
            
        //
        //  Create the proper namespaces
        //
            
        session.getWorkspace().getNamespaceRegistry().registerNamespace( "wiki", NS_JSPWIKI );
            
        Node root = session.getRootNode();
        
        //
        // Create main page directory
        //
        if( !root.hasNode( JCR_PAGES_NODE ) )
        {
            root.addNode( JCR_PAGES_NODE );
        }
        
        //
        //  Make sure at least the default "Main" wikispace exists.
        //
            
        if( !root.hasNode( JCR_DEFAULT_SPACE ) )
        {
            root.addNode( JCR_DEFAULT_SPACE );
        }
            
        session.save();

    }
    
    /**
     *  Discards all unsaved modifications made to this repository and releases
     *  the Session.  Any calls after this will allocate a new Session. But
     *  that's okay, since an unreleased Session will keep accumulating memory.
     */
    public void release()
    {
        m_sessionManager.releaseSession();
    }
        
    /*
    public Object acquire() throws ProviderException
    {
        try
        {
            return m_sessionManager.createSession();
        }
        catch( Exception e )
        {
            throw new ProviderException( "Unable to create a JCR session", e );
        }
    }
    
    public void release( Object id )
    {
        m_sessionManager.destroySession( id );
    }
    */
    
    /**
     *  Returns all pages in some random order.  If you need just the page names, 
     *  please see {@link ReferenceManager#findCreated()}, which is probably a lot
     *  faster.  This method may cause repository access.
     *  
     *  @param space Name of the Wiki space.  May be null, in which case gets all spaces
     *  @return A Collection of WikiPage objects.
     *  @throws ProviderException If the backend has problems.
     */
   
    public Collection<WikiPage> getAllPages( String space )
        throws ProviderException
    {
        ArrayList<WikiPage> result = new ArrayList<WikiPage>();
        try
        {
            Session session = m_sessionManager.getSession();
        
            QueryManager mgr = session.getWorkspace().getQueryManager();
            
            Query q = mgr.createQuery( "/jcr:root/"+JCR_PAGES_NODE+"/"+((space != null) ? space : "")+"/*", Query.XPATH );
            
            QueryResult qr = q.execute();
            
            for( NodeIterator ni = qr.getNodes(); ni.hasNext(); )
            {
                Node n = ni.nextNode();
                
                // Hack to make sure we don't add the space root node. 
                if( n.getDepth() != 2 )
                    result.add( new JCRWikiPage( getEngine(), n ) );
            }
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("getAllPages()",e);
        }
        catch( WikiException e )
        {
            throw new ProviderException("getAllPages()",e);
        }
        
        return result;
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
     *  Locks page for editing.  Note, however, that the PageManager
     *  will in no way prevent you from actually editing this page;
     *  the lock is just for information.
     *
     *  @param page WikiPage to lock
     *  @param user Username to use for locking
     *  @return null, if page could not be locked.
     */
    // FIXME: This should probably also cause a lock in the repository.
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

            lock = m_pageLocks.get( page.getName() );

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
            lock = m_pageLocks.get( page.getName() );
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
    public List<PageLock> getActiveLocks()
    {
        ArrayList<PageLock> result = new ArrayList<PageLock>();

        synchronized( m_pageLocks )
        {
            for( PageLock lock : m_pageLocks.values() )
            {
                result.add( lock );
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
    /*
    // FIXME: Remove.  Just exists to make sure that all the things that need
    //        to be called are called.
    public WikiPage getPage( String pageName, int version )
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

            page = m_provider.getPageInfo( pageName, version );

            if( page != null )
            {
                m_engine.updateReferences( page );
            }
            else
            {
                m_engine.getReferenceManager().pageRemoved( new WikiPage(m_engine,pageName) );
            }
        }

        //
        //  Should update the metadata.
        //
        
        if( page != null && !page.hasMetadata() )
        {
            WikiContext context = new WikiContext(m_engine,page);
            m_engine.textToHTML( context, getPageText(pageName,version) );
        }
        
        return page;
    }
*/
    /**
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  
     *  @param path The name of the page to fetch history for
     *  @return If the page does not exist, returns null, otherwise a List
     *          of WikiPages.
     *  @throws ProviderException If the repository fails.
     */

    public List<WikiPage> getVersionHistory( WikiName path )
        throws ProviderException, PageNotFoundException
    {
        List<WikiPage> result = new ArrayList<WikiPage>();
        JCRWikiPage base = getPage(path);

        try
        {
            Node baseNode = base.getJCRNode();
            
            VersionHistory vh = baseNode.getVersionHistory();
            
            for( VersionIterator vi = vh.getAllVersions(); vi.hasNext(); )
            {
                Version v = vi.nextVersion();
                
                result.add( new JCRWikiPage(m_engine,v) );
            }
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException(path);
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to get version history",e);
        }
        catch( WikiException e )
        {
            throw new ProviderException("Unable to get version history",e);
        }
        
        return result;
    }
    
    /**
     *  Returns a human-readable description of the current provider.
     *  
     *  @return A human-readable description.
     */
    public String getProviderDescription()
    {
        return m_repository.getDescriptor( Repository.REP_NAME_DESC );
    }

    /**
     *  Returns the total count of all pages in the repository. This
     *  method is equivalent of calling getAllPages().size(), but
     *  it swallows the ProviderException and returns -1 instead of
     *  any problems.
     *  
     *  @param space Name of the Wiki space.  May be null, in which
     *  case all spaces will be counted
     *  @return The number of pages.
     *  @throws ProviderException If there was an error.
     */
    // FIXME: Unfortunately this method is very slow, since it involves gobbling
    //        up the entire repo.
    public int getTotalPageCount(String space) throws ProviderException
    {
        return getAllPages(space).size();
    }
    /**
     *  Returns true, if the page exists (any version).
     *  
     *  @param wikiPath  the {@link WikiName} to check for
     *  @return A boolean value describing the existence of a page
     *  @throws ProviderException If the backend fails or the wikiPath is illegal.
     */
    public boolean pageExists( WikiName wikiPath )
        throws ProviderException
    {
        if( wikiPath == null )
        {
            throw new ProviderException("Illegal page name");
        }

        try
        {
            Session session = m_sessionManager.getSession();
            
            String jcrPath = getJCRPath( wikiPath ); 
            
            return session.getRootNode().hasNode( jcrPath );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to check for page existence", e );
        }
    }
    
    /**
     *  Checks for existence of a specific page and version.
     *  
     *  @since 2.3.29
     *  @param wikiPath  the {@link WikiName} to check for
     *  @param version The version to check
     *  @return <code>true</code> if the page exists, <code>false</code> otherwise
     *  @throws ProviderException If the backend fails or the wikiPath is illegal.
     */
    public boolean pageExists( WikiName wikiPath, int version )
        throws ProviderException
    {
        if( wikiPath == null )
        {
            throw new ProviderException("Illegal page name");
        }

        try
        {
            Session session = m_sessionManager.getSession();
            
            return session.itemExists( getJCRPath( wikiPath ) );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to check for page existence",e);
        }
    }

    /**
     *  Deletes only a specific version of a WikiPage.
     *  
     *  @param page The page to delete.
     *  @throws ProviderException if the page fails
     *  @throws PageNotFoundException If the page in question does not exist.
     */
    public void deleteVersion( WikiPage page )
        throws ProviderException, PageNotFoundException
    {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );

        JCRWikiPage jcrPage = (JCRWikiPage)page;
        try
        {
            jcrPage.getJCRNode().remove();
            jcrPage.save();
            
            fireEvent( WikiPageEvent.PAGE_DELETED, jcrPage.getName() );
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException(page.getQualifiedName());
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to delete a page",e);
        }
    }
    
    /**
     *  Deletes an entire page, all versions, all traces.
     *  
     *  @param page The WikiPage to delete
     *  @throws ProviderException If the backend fails or the page is illegal.
     *  @throws PageNotFoundException If the page has already disappeared.
     */
    
    public void deletePage( WikiPage page )
        throws ProviderException, PageNotFoundException
    {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );

        VersionHistory vh;
        try
        {
            Node nd = ((JCRWikiPage)page).getJCRNode();
            
            // Remove version history
            if( nd.isNodeType( "mix:versionable" ) )
            {
                vh = nd.getVersionHistory();
            
                for( VersionIterator iter = vh.getAllVersions(); iter.hasNext(); )
                {
                    Version v = iter.nextVersion();
                
                    v.remove();
                    v.save();
                }
                vh.save();
            }
            
            // Remove the node itself.
            nd.remove();
            
            nd.getParent().save();
            
            fireEvent( WikiPageEvent.PAGE_DELETED, page.getName() );
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException(page.getQualifiedName());
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Deletion of pages failed: " + e.getMessage(), e );
        }
    }

    /**
     *  This is a simple reaper thread that runs roughly every minute
     *  or so (it's not really that important, as long as it runs),
     *  and removes all locks that have expired.
     */

    private class LockReaper extends WikiBackgroundThread
    {
        /**
         *  Create a LockReaper for a given engine.
         *  
         *  @param engine WikiEngine to own this thread.
         */
        public LockReaper( WikiEngine engine )
        {
            super( engine, 60 );
            setName("JSPWiki Lock Reaper");
        }

        public void backgroundTask() throws Exception
        {
            synchronized( m_pageLocks )
            {
                Collection<PageLock> entries = m_pageLocks.values();

                Date now = new Date();

                for( Iterator<PageLock> i = entries.iterator(); i.hasNext(); )
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
     * returns {@link org.apache.wiki.workflow.Outcome#STEP_ABORT}. Any
     * WikiExceptions thrown by page filters will be re-thrown, and the workflow
     * will abort.
     *
     * @author Andrew Jaquith
     */
    public static class PreSaveWikiPageTask extends Task
    {
        private static final long serialVersionUID = 6304715570092804615L;
        private final WikiContext m_context;
        private final String m_proposedText;

        /**
         *  Creates the task.
         *  
         *  @param context The WikiContext
         *  @param proposedText The text that was just saved.
         */
        public PreSaveWikiPageTask( WikiContext context, String proposedText )
        {
            super( PRESAVE_TASK_MESSAGE_KEY );
            m_context = context;
            m_proposedText = proposedText;
        }

        /**
         *  {@inheritDoc}
         */
        @Override
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
     * {@link org.apache.wiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, org.apache.wiki.workflow.Fact[], Task, String)};
     * they will not function correctly otherwise.
     *
     * @author Andrew Jaquith
     */
    public static class SaveWikiPageTask extends Task
    {
        private static final long serialVersionUID = 3190559953484411420L;

        /**
         *  Creates the Task.
         */
        public SaveWikiPageTask()
        {
            super( SAVE_TASK_MESSAGE_KEY );
        }

        /** {@inheritDoc} */
        @Override
        public Outcome execute() throws WikiException
        {
            // Retrieve attributes
            WikiContext context = (WikiContext) getWorkflow().getAttribute( PRESAVE_WIKI_CONTEXT );
            String proposedText = (String) getWorkflow().getAttribute( FACT_PROPOSED_TEXT );

            WikiEngine engine = context.getEngine();
            JCRWikiPage page = (JCRWikiPage)context.getPage();

            // Set the last-modified timestamp
            page.setLastModified( new Date() );

            // Let the rest of the engine handle actual saving.
            page.setContent( proposedText );
            
            page.save();

            // Refresh the context for post save filtering.
            try
            {
                engine.getPage( page.getName() );
                engine.textToHTML( context, proposedText );
                engine.getFilterManager().doPostSaveFiltering( context, proposedText );
            }
            catch( PageNotFoundException e )
            {
                e.printStackTrace();
                throw new WikiException( e.getMessage() );
            }
            return Outcome.STEP_COMPLETE;
        }
    }

    // events processing .......................................................

    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiPageEvent
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
     *  Evaluates a WikiName in the context of the current page request.
     *  
     *  @param wikiName The WikiName.
     *  @return A full JCR path
     */
    public static String getJCRPath( WikiName wikiName )
    {
        String spaceName;
        String spacePath;
        
        spaceName = wikiName.getSpace();
        spacePath = wikiName.getPath();
               
        return "/"+JCR_PAGES_NODE+"/"+spaceName+"/"+spacePath;
    }

    /**
     *  Evaluates a WikiName in the context of the current page request.
     *  
     *  @param jcrpath The JCR Path used to get the {@link WikiName}
     *  @return The {@link WikiName} for the requested jcr path
     *  @throws ProviderException If the backend fails.
     */
    // FIXME: Should be protected - fix once WikiPage moves to content-package
    public static WikiName getWikiPath( String jcrpath ) throws ProviderException
    {
        if( jcrpath.startsWith("/"+JCR_PAGES_NODE+"/") )
        {
            String wikiPath = jcrpath.substring( ("/"+JCR_PAGES_NODE+"/").length() );

            int firstSlash = wikiPath.indexOf( '/' );
            
            if( firstSlash != -1 )
            {
                return new WikiName(wikiPath.substring( 0, firstSlash ), 
                                    wikiPath.substring( firstSlash+1 ) );
            }
        }
        
        throw new ProviderException("This is not a valid JSPWiki JCR path: "+jcrpath);
    }
    
    /**
     *  Adds new content to the repository.  To update, get a page, modify
     *  it, then store it back using save().
     *  
     *  @param path the WikiName
     *  @param contentType the type of content
     *  @return the {@link JCRWikiPage} 
     *  @throws PageAlreadyExistsException if the page already exists in the repository
     *  @throws ProviderException if the backend fails
     */
    public JCRWikiPage addPage( WikiName path, String contentType ) throws PageAlreadyExistsException, ProviderException
    {
        try
        {
            Session session = m_sessionManager.getSession();
        
            Node nd = session.getRootNode().addNode( getJCRPath(path) );
            
            JCRWikiPage page = new JCRWikiPage(m_engine, nd);
            
            return page;
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to add a page", e );
        }
    }

    /**
     *  Get content from the repository.
     *  
     *  @param path the path
     *  @return the {@link JCRWikiPage} 
     *  @throws ProviderException If the backend fails.
     *  @throws PageNotFoundException If the page does not exist, or if <code>path</code>
     *  is <code>null</code>
     */
    public JCRWikiPage getPage( WikiName path ) throws ProviderException, PageNotFoundException
    {
        if ( path == null )
        {
            throw new PageNotFoundException( "(null)" );
        }
        try
        {
            Session session = m_sessionManager.getSession();
        
            Node nd = session.getRootNode().getNode( getJCRPath(path) );
            JCRWikiPage page = new JCRWikiPage(m_engine, nd);
            
            return page;
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException( path );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to get a page", e );
        }
        catch( WikiException e )
        {
            throw new ProviderException("Unable to get a  page",e);
        }
    }

    public JCRWikiPage getPage( WikiName path, int version ) throws ProviderException, PageNotFoundException
    {
        try
        {
            JCRWikiPage page = null;
            Session session = m_sessionManager.getSession();
        
            Node nd = session.getRootNode().getNode( getJCRPath(path) );

            try
            {
                VersionHistory vh = nd.getVersionHistory();
            
                Version v = vh.getVersion( Integer.toString( version ) );
            
                page = new JCRWikiPage(m_engine, v);
            }
            catch( UnsupportedRepositoryOperationException e )
            {
                // No version history yet
                
                if( version == WikiProvider.LATEST_VERSION || version == 1)
                    page = new JCRWikiPage( m_engine, nd );
            }
            
            return page;
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException( path );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to get a page", e );
        }
    }
    
    /**
     *  Listens for {@link org.apache.wiki.event.WikiSecurityEvent#PROFILE_NAME_CHANGED}
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
                // FIXME: This is a hack to make this thing compile, not work.
                Collection<WikiPage> pages = getAllPages( null );
                for ( Iterator<WikiPage> it = pages.iterator(); it.hasNext(); )
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
            catch ( WikiException e )
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
            Enumeration<AclEntry> entries = acl.entries();
            Collection<AclEntry> entriesToAdd    = new ArrayList<AclEntry>();
            Collection<AclEntry> entriesToRemove = new ArrayList<AclEntry>();
            while ( entries.hasMoreElements() )
            {
                AclEntry entry = (AclEntry)entries.nextElement();
                if ( ArrayUtils.contains( oldPrincipals, entry.getPrincipal() ) )
                {
                    // Create new entry
                    AclEntry newEntry = new AclEntryImpl();
                    newEntry.setPrincipal( newPrincipal );
                    Enumeration<Permission> permissions = entry.permissions();
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
            for ( Iterator<AclEntry> ix = entriesToRemove.iterator(); ix.hasNext(); )
            {
                AclEntry entry = (AclEntry)ix.next();
                acl.removeEntry( entry );
            }
            for ( Iterator<AclEntry> ix = entriesToAdd.iterator(); ix.hasNext(); )
            {
                AclEntry entry = (AclEntry)ix.next();
                acl.addEntry( entry );
            }
        }
        return pageChanged;
    }

    public WikiPage getDummyPage() throws WikiException
    {
        try
        {
            Session session = m_sessionManager.getSession();
            Node nd = session.getRootNode().addNode( "/pages/Main/Dummy" );
            return new JCRWikiPage( m_engine, nd );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to get dummy page",e);
        }
    }

    /**
     *  Implements the ThreadLocal pattern for managing JCR Sessions.  It is the
     *  responsibility for every user to get a Session, then close it.
     *  <p>
     *  Based on Hibernate ThreadLocal best practices.
     */
    private class JCRSessionManager
    {
        /** the per thread session **/
        private final ThreadLocal<Session> m_currentSession = new ThreadLocal<Session>();
        /** The constants for describing the ownerships **/
        private final Owner m_trueOwner = new Owner(true);
        private final Owner m_fakeOwner = new Owner(false);
        
        /**
         *  This creates a session which can be fetched then with getSession().
         *  
         *  @return An ownership handle which must be passed to destroySession():
         *  @throws LoginException
         *  @throws RepositoryException
         */
        public Object createSession() throws LoginException, RepositoryException
        {
            Session session = m_currentSession.get();  
            if(session == null)
            {
                session = m_repository.login(m_workspaceName); 
                m_currentSession.set(session);
                return m_trueOwner;
            }
            return m_fakeOwner;
        }

        /**
         *  Closes the current session, if this caller is the owner.  Must be called
         *  in your finally- block.
         *  
         *  @param ownership The ownership parameter from createSession()
         */
        public void destroySession(Object ownership) 
        {
            if( ownership != null && ((Owner)ownership).m_identity)
            {
                releaseSession();
            }
        }
 
        public void releaseSession()
        {
            Session session = m_currentSession.get();
            session.logout();
            m_currentSession.set(null);            
        }
        
        /**
         *  Between createSession() and destroySession() you may get the Session object
         *  with this call.
         *  
         *  @return A valid Session object, if called between createSession and destroySession().
         *  @throws IllegalStateException If the object has not been acquired with createSession()
         * @throws RepositoryException 
         * @throws LoginException 
         */
        public Session getSession() throws LoginException, RepositoryException
        {
            Session s = m_currentSession.get();
            
            if( s == null ) 
            {
                createSession();
                s = m_currentSession.get();
            }
            
            return s;
        } 

    }
    
    /**
     * Internal class , for handling the identity. Hidden for the 
     * developers
     */
    private static class Owner 
    {
        public Owner(boolean identity)
        {
            m_identity = identity;
        }
        boolean m_identity = false;        
    }

    public Node getJCRNode( String path ) throws RepositoryException
    {
        return (Node)m_sessionManager.getSession().getItem( path );
    }
}
