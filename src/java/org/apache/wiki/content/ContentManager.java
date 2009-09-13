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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.Permission;
import java.security.Principal;
import java.util.*;

import javax.jcr.*;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.content.lock.PageLock;
import org.apache.wiki.event.*;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.parser.MarkupParser;
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
 *  Events which are fired by this Manager:
 *  <ul>
 *    <li>WikiPageEvent.PAGE_DELETE_REQUEST - Before deletion actually starts. Page still exists in
 *        the repository.</li>
 *    <li>WikiPageEvent.PAGE_DELETED - After deletion is complete, which means that the page no longer
 *        exists in the repository.</li>
 *  <ul>
 *  @since 3.0
 */

public class ContentManager implements WikiEventListener
{
    private static final String WIKI_VERSIONS = "wiki:versions";

    /**
     *  The name of the default WikiSpace.
     */
    public static final String DEFAULT_SPACE = "Main";
    
    private static final String JCR_DEFAULT_SPACE = "pages/"+DEFAULT_SPACE;

    private static final String JCR_PAGES_NODE = "pages";

    private static final long serialVersionUID = 2L;
    
    /** Workflow attribute for storing the ACL. */
    private static final String PRESAVE_PAGE_ACL = "page.acl";
    
    /** Workflow attribute for storing the page author. */
    private static final String PRESAVE_PAGE_AUTHOR = "page.author";
    
    /** Workflow attribute for storing the page attributes. */
    private static final String PRESAVE_PAGE_ATTRIBUTES = "page.attributes";
    
    /** Workflow attribute for storing the page last-modified date. */
    private static final String PRESAVE_PAGE_LASTMODIFIED = "page.lastmodified";
    
    /** Workflow attribute for storing the qualfiied page name. */
    private static final String PRESAVE_PAGE_NAME = "page.name";
    
    /** Workflow attribute for storing the proposed page text. */
    private static final String PRESAVE_PAGE_TEXT = "page.text";
    
    /** The property value for setting the amount of time before the page locks expire. 
     *  Value is {@value}.
     */
    public static final String PROP_LOCKEXPIRY   = "jspwiki.lockExpiryTime";
    
    /** The message key for storing the text for the presave task.  Value is <tt>{@value}</tt>*/
    public static final String PRESAVE_TASK_MESSAGE_KEY = "task.preSaveWikiPage";
    
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
    
    private static final Serializable[] NO_ARGS = new Serializable[0];

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
                // FIXME: Should really use reflection to find this class - this requires
                //        an unnecessary compile-time presence of priha.jar.
                try
                {
                    // Try loading priha.properties from the classpath
                    Properties prihaProps = new Properties();
                    InputStream in = null;
                    boolean propsLoaded = false;
                    try
                    {
                        ServletContext servletContext = engine.getServletContext();
                        in = servletContext.getResourceAsStream( "/WEB-INF/classes/priha.properties" );
                        if ( in != null )
                        {
                            prihaProps.load( in );
                            m_repository = RepositoryManager.getRepository( prihaProps );
                            propsLoaded = true;
                        }
                    }
                    catch( IOException ioe ) { }
                    
                    // Fallback: just use the default repository
                    if ( !propsLoaded )
                    {
                        m_repository = RepositoryManager.getRepository();
                    }
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
                    Class<Repository> jackrabbitRepo = (Class<Repository>) Class.forName( "org.apache.jackrabbit.core.TransientRepository" );
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
    
    /**
     *  Creates a new version of the given page.
     *  
     *  @param page
     *  @throws RepositoryException 
     */
    private void checkin( String path, int currentVersion ) throws RepositoryException
    {
        Session copierSession = null;
        
        try
        {
            copierSession = m_sessionManager.newSession();
            
            // If the item does not exist yet, there is nothing to copy.
            if( !copierSession.itemExists( path ) ) return;
            
            Node nd = (Node)copierSession.getItem( path );
            Node versions;
            
            if( !nd.hasNode( WIKI_VERSIONS ) )
            {
                versions = nd.addNode( WIKI_VERSIONS );
            }
            else
            {
                versions = nd.getNode( WIKI_VERSIONS );
            }
            
            Node newVersion = versions.addNode( Integer.toString( currentVersion ) );
            
            newVersion.addMixin( "mix:referenceable" );
            
            copyProperties( nd, newVersion );
            
            copierSession.save();
        }
        finally
        {
            if( copierSession != null ) copierSession.logout();
        }
        
    }

    private void copyProperties( Node source, Node dest )
                                                         throws RepositoryException,
                                                             ValueFormatException,
                                                             VersionException,
                                                             LockException,
                                                             ConstraintViolationException
    {
        for( PropertyIterator pi = source.getProperties(); pi.hasNext(); )
        {
            Property p = pi.nextProperty();
            
            int opp = p.getDefinition().getOnParentVersion();
            
            //
            //  This should prevent us from copying stuff which is set by
            //  the repository.
            //
            // FIXME: The jcr:uuid check is a Priha 0.1.21-specific hack - I have
            //        no idea why it's giving the wrong OnParentVersionAction.
            if( opp == OnParentVersionAction.COPY && !p.getName().equals( "jcr:uuid" ))
            {
//                System.out.println("  Copying "+p.getName());
                if( p.getDefinition().isMultiple() )
                    dest.setProperty( p.getName(), p.getValues() );
                else
                    dest.setProperty( p.getName(), p.getValue() );
            }
//            else System.out.println("  Skipping "+p.getName());
        }
    }
    
    /**
     *  Saves a WikiPage to the repository and creates a new version.
     *  Sets the following attributes:
     *  <ul>
     *  <li>wiki:created
     *  <li>wiki:version
     *  </ul>
     * 
     *  @param page the page to save
     */
    public void save( WikiPage page ) throws RepositoryException
    {
        WikiPath path = page.getPath();
        Node nd = getJCRNode( getJCRPath( path ) );

        int version = page.getVersion();
        
        nd.setProperty( JCRWikiPage.ATTR_VERSION, version+1 );
        
        if( !nd.hasProperty( "wiki:created" ) )
        {
            nd.setProperty( "wiki:created", Calendar.getInstance() );
        }
        
        if( nd.isNew() )
        {
            // New node, so nothing to check in
            nd.getParent().save();
        }
        else
        {
            checkin( getJCRPath( path ), version );
            nd.save();
        }
        
        fireEvent( ContentEvent.NODE_SAVED, page.getName(), NO_ARGS );
    }
    
    /**
     *  Shuts down the ContentManager in a good fashion.
     */
    @SuppressWarnings("unchecked")
    public void shutdown()
    {
        release();
        
        //
        //  If this is a Jackrabbit Repository, we'll call it's shutdown() method
        //  to make sure it's really shut down.
        //
        // FIXME: I am not too sure whether this really works.
        try
        {
            Class<Repository> jcrRepoClass = (Class<Repository>)Class.forName( "org.apache.jackrabbit.core.JackrabbitRepository" );
            if( m_repository.getClass().isAssignableFrom(jcrRepoClass) )
            {
                log.info( "Shutting down Jackrabbit repository..." );
                Method m = jcrRepoClass.getMethod( "shutdown" );
                
                m.invoke( m_repository );
            }
        }
        catch( ClassNotFoundException e )
        {
            // Fine.
        }
        catch( SecurityException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( NoSuchMethodException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( IllegalArgumentException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( IllegalAccessException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch( InvocationTargetException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        m_repository = null;
    }
    
    /**
     *  Returns the current JCR Session.  If there is no Session, a new
     *  one is created.
     *  
     *  @return A valid JCR Session
     *  @throws LoginException If login credentials are wrong
     *  @throws RepositoryException If something else fails with the repository
     */
    public Session getCurrentSession() throws LoginException, RepositoryException
    {
        return m_sessionManager.getSession();
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
     *  @param space the name of the wiki space containing the pages to get.  May be
     *  <code>null</code>, in which case gets all spaces
     *  @throws ProviderException if the backend has problems.
     */
    public List<WikiPage> getAllPages( String space )
        throws ProviderException
    {
        List<WikiPage> results = new ArrayList<WikiPage>();
        try
        {
            Session session = m_sessionManager.getSession();
        
            QueryManager mgr = session.getWorkspace().getQueryManager();
            
            Query q = mgr.createQuery( "/jcr:root/"+JCR_PAGES_NODE+((space != null) ? ("/"+space) : "")+"/*", Query.XPATH );
            
            QueryResult qr = q.execute();
            
            for( NodeIterator ni = qr.getNodes(); ni.hasNext(); )
            {
                Node n = ni.nextNode();
                
                // Hack to make sure we don't add the space root node. 
                // or any of the special child nodes that have a namespace
                if( !isSpaceRoot(n) && n.getPath().indexOf( ':' ) == -1 )
                {
                    WikiPage page = new JCRWikiPage( getEngine(), n );
                    if ( !results.contains( page ) )
                    {
                        results.add( page );
                    }
                }
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
        
        return results;
    }
    
    /**
     *  Returns all pages in a specified order.  If you need just the page names, 
     *  please see {@link ReferenceManager#findCreated()}, which is probably a lot
     *  faster.  This method may cause repository access.
     *  
     *  @param space the name of the wiki space containing the pages to get.  May be
     *  <code>null</code>, in which case gets all spaces
     *  @param comparator the comparator used for sorting the page collection;
     *  for example, {@link org.apache.wiki.util.PageTimeComparator}.
     *  @return a list of WikiPage objects.
     *  @throws ProviderException if the back-end has problems.
     */
    public List<WikiPage> getAllPages( String space, Comparator<WikiPage> comparator )
        throws ProviderException
    {
        if ( comparator == null )
        {
            throw new IllegalArgumentException ( "Comparator cannot be null." );
        }
        List<WikiPage> pages = getAllPages( space );
        Collections.sort( pages, comparator );
        return pages;
    }

    /**
     *  Returns true, if this Node is the root node of a space.
     *  
     *  @param nd Node to check
     *  @return true, if this is a root node of a space.
     */
    private boolean isSpaceRoot(Node nd) throws RepositoryException
    {
        return nd.getPath().startsWith( "/"+JCR_PAGES_NODE ) && nd.getDepth() == 2;
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
            fireEvent( WikiPageEvent.PAGE_LOCK, page.getName(), NO_ARGS ); // prior to or after actual lock?

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

                log.debug( "Lock set : "+ lock);
            }
            else
            {
                log.debug( "The lock  " + lock + " already exists" );
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

            log.debug( "Released lock " + lock );
        }

        fireEvent( WikiPageEvent.PAGE_UNLOCK, lock.getPage(), NO_ARGS );
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
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  
     *  @param path The name of the page to fetch history for
     *  @return A List of WikiPages
     *  @throws ProviderException If the repository fails.
     *  @throws PageNotFoundException If the page does not exist.
     */

    public List<WikiPage> getVersionHistory( WikiPath path )
        throws ProviderException, PageNotFoundException
    {
        List<WikiPage> result = new ArrayList<WikiPage>();

        try
        {
            Node base = getJCRNode( getJCRPath(path) );
            
            if( base.hasNode( WIKI_VERSIONS ) )
            {
                Node versionHistory = base.getNode( WIKI_VERSIONS );
            
                for( NodeIterator ni = versionHistory.getNodes(); ni.hasNext(); )
                {
                    Node v = ni.nextNode();

                    result.add( new JCRWikiPage(m_engine,path,v) );
                }
            }
            
            result.add( new JCRWikiPage(m_engine,base) );
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Failure in trying to get version history",e);
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
     *  Return the FQN of the class implementing Repository.
     * 
     *  @return A class name.
     */
    public String getProvider()
    {
        return m_repository.getClass().getName();
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
     *  Returns <code>true</code> if a given page exists (any version). 
     *  In order for
     *  this method to return <code>true</code>, the JCR node
     *  representing the page must exist, and it must also have been
     *  previously saved (that is, not "new").
     *  
     *  Unlike {@link WikiEngine#pageExists(String)}, this method does not
     *  resolve the supplied path by calling {@link WikiEngine#getFinalPageName(WikiPath)}. 
     *  
     *  @param wikiPath  the {@link WikiPath} to check for
     *  @return A boolean value describing the existence of a page
     *  @throws ProviderException If the backend fails or the wikiPath is illegal.
     */
    public boolean pageExists( WikiPath wikiPath )
        throws ProviderException
    {
        if( wikiPath == null )
        {
            throw new ProviderException("Illegal page name");
        }
        
        // Find the JCR node
        String jcrPath = getJCRPath( wikiPath ); 
        Node node = null;
        try
        {
            node = getJCRNode( jcrPath );
        }
        catch ( PathNotFoundException e )
        {
            // Node wasn't in JCR; thus, page doesn't exist.
            return false;
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to check for page existence", e );
        }
        
        // Node "exists" only if it's been saved already.
        return  !node.isNew();
    }
    
    /**
     *  Returns <code>true</code> if a given page exists for a specific version.
     *  For the page to "exist" the page must have been previously added
     *  (for example, by {@link #addPage(WikiPath, String)}), although it need not
     *  have been saved. This is unlike the {@link WikiEngine} version of
     *  <code>pageExists</code>, which requires the page to be saved before it
     *  is considered to exist.
     *  
     *  @param wikiPath  the {@link WikiPath} to check for
     *  @param version The version to check
     *  @return <code>true</code> if the page exists, <code>false</code> otherwise
     *  @throws ProviderException If the backend fails or the wikiPath is illegal.
     */
    public boolean pageExists( WikiPath wikiPath, int version )
        throws ProviderException
    {
        if( wikiPath == null )
        {
            throw new ProviderException("Illegal page name");
        }

        // Find the JCR node
        String jcrPath = getJCRPath( wikiPath ); 
        Node node = null;
        try
        {
            node = getJCRNode( jcrPath );
        }
        catch ( PathNotFoundException e )
        {
            // Node wasn't in JCR; thus, page doesn't exist.
            return false;
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to check for page existence", e );
        }
        
        // Node "exists" only if it's been saved already.
        return  !node.isNew();
    }

    /**
     *  Deletes only a specific version of a WikiPage.  No need to call page.save()
     *  after this.
     *  <p>
     *  If this is the only version of the page, then the entire page will be
     *  deleted.
     *  
     *  @param page The page to delete.
     *  @throws ProviderException if the page fails
     *  @return True, if the page existed and was actually deleted, and false if the page did
     *          not exist.
     */
    // TODO: Should fire proper events
    public boolean deleteVersion( WikiPage page )
        throws ProviderException
    {
        //fireEvent( ContentEvent.NODE_DELETE_REQUEST, page.getName(), NO_ARGS );

        JCRWikiPage jcrPage = (JCRWikiPage)page;
        
        try
        {
            if( jcrPage.isLatest() )
            {
                // ..--p8mmmfpppppppppcccc0i9u8ioakkkcnnnnr njv,vv,vb

                try
                {
                    JCRWikiPage pred = jcrPage.getPredecessor();
                
                    restore( pred );
                }
                catch( PageNotFoundException e )
                {
                    deletePage( page );
                }
                catch( PageAlreadyExistsException e )
                {
                    // SHould never happen, so this is quite problematic
                    throw new ProviderException( "Page which was already removed still exists?", e );
                }
                
            }
            else
            {
                jcrPage.getJCRNode().remove();
                
                jcrPage.getJCRNode().getParent().save();
            }
            
            //fireEvent( ContentEvent.NODE_DELETED, page.getName(), NO_ARGS );
            
            return true;
        }
        catch( PathNotFoundException e )
        {
            return false;
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("Unable to delete a page",e);
        }
    }
    
    private void restore( JCRWikiPage page ) throws ProviderException, RepositoryException, PageAlreadyExistsException
    {
        JCRWikiPage original = page.getCurrentVersion();
        
        Node origNode = original.getJCRNode();

        for( PropertyIterator pi = origNode.getProperties(); pi.hasNext(); )
        {
            Property p = pi.nextProperty();

            // TODO: Again, strange Priha-specific hack for 0.1.21
            if( !p.getDefinition().isProtected() && !p.getName().equals("jcr:uuid") )
                p.remove();
        }
        
        origNode.save();
        
        copyProperties( page.getJCRNode(), origNode );
        
        origNode.save();
    }
    
    /**
     *  Deletes an entire page, all versions, all traces.  If the page did not
     *  exist, will just exit quietly and return false.
     *  
     *  @param page The WikiPage to delete
     *  @return True, if the page was found and deleted; false, if the page did not exist in the first place
     *  @throws ProviderException If the backend fails or the page is illegal.
     */
    
    public boolean deletePage( WikiPage page )
        throws ProviderException
    {
        fireEvent( ContentEvent.NODE_DELETE_REQUEST, page.getName(), NO_ARGS );

        try
        {
            Node nd = ((JCRWikiPage)page).getJCRNode();
            
            // Remove the node itself.
            nd.remove();
            
            nd.getParent().save();
            
            fireEvent( ContentEvent.NODE_DELETED, page.getName(), NO_ARGS );
            
            return true;
        }
        catch( PathNotFoundException e )
        {
            return false;
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
                    PageLock p = i.next();

                    if( now.after( p.getExpiryTime() ) )
                    {
                        i.remove();

                        log.info( "Reaped lock: " + p );
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

            // Stash the page author. Prefer the one set for the page
            // already; otherwise use the current author
            WikiPage page = m_context.getPage();
            String author = null;
            if ( page.getAuthor() != null )
            {
                author = page.getAuthor();
            }
            if ( author == null )
            {
                author = m_context.getCurrentUser().getName();
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

            // Stash the page ACL, author, attributes, modified-date, name and new text as workflow attributes

            // FIXME: This works now,  but will not scale, because the attribute list can be exceedingly
            // big (in the order of gigabytes). Alternate method required.
            
            workflow.setAttribute( PRESAVE_PAGE_ACL, page.getAcl() );
            workflow.setAttribute( PRESAVE_PAGE_AUTHOR, author );
            workflow.setAttribute( PRESAVE_PAGE_ATTRIBUTES, (Serializable)page.getAttributes() );
            workflow.setAttribute( PRESAVE_PAGE_LASTMODIFIED, new Date() );
            workflow.setAttribute( PRESAVE_PAGE_NAME, page.getPath() );
            workflow.setAttribute( PRESAVE_PAGE_TEXT, saveText );
            
            return Outcome.STEP_COMPLETE;
        }
    }

    /**
     * Inner class that handles the actual page save and post-save actions. Instances
     * of this class are assumed to have been added to an approval workflow via
     * {@link org.apache.wiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, org.apache.wiki.workflow.Fact[], Task, String)};
     * they will not function correctly otherwise.
     *
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
        @SuppressWarnings("unchecked")
        @Override
        public Outcome execute() throws WikiException
        {
            // Fetch the page that was being saved
            Workflow workflow = getWorkflow();
            WikiEngine engine = workflow.getWorkflowManager().getEngine();
            WikiPath name = (WikiPath)workflow.getAttribute( PRESAVE_PAGE_NAME );
            JCRWikiPage page;
            try
            {
                page = engine.getContentManager().getPage( name );
            }
            catch( PageNotFoundException e )
            {
                // Doesn't exist? No problem. Time to make one.
                try
                {
                    page = engine.getContentManager().addPage( name, ContentManager.JSPWIKI_CONTENT_TYPE );
                }
                catch( PageAlreadyExistsException pae )
                {
                    // This should never happen, but it if does, throw a big honking exception
                    throw new WikiException( "We were just told the page didn't exist. Now it does? Explain that please...", pae );
                }
            }
            
            // Retrieve the page ACL, author, attributes, modified-date, name and new text from the workflow
            Acl acl = (Acl)workflow.getAttribute( PRESAVE_PAGE_ACL );
            String author = (String)workflow.getAttribute( PRESAVE_PAGE_AUTHOR );
            Map<String,Serializable> attributes = (Map<String,Serializable>)workflow.getAttribute( PRESAVE_PAGE_ATTRIBUTES );
            Date date = (Date)workflow.getAttribute( PRESAVE_PAGE_LASTMODIFIED );
            String text = (String)workflow.getAttribute( PRESAVE_PAGE_TEXT );

            // Set the page properties and save it!
            page.setAcl( acl );
            page.setAuthor( author );
            if ( attributes != null )
            {
                for ( Map.Entry<String, Serializable> attribute: attributes.entrySet() )
                {
                    page.setAttribute( attribute.getKey(), attribute.getValue() );
                }
            }
            page.setLastModified( date );
            page.setContent( text );
            page.save();

            // Refresh the context for post save filtering.
            try
            {
                WikiContext context = engine.getWikiContextFactory().newViewContext( page );
                engine.getPage( page.getName() );
                engine.textToHTML( context, text );
                engine.getFilterManager().doPostSaveFiltering( context, text );
            }
            catch( PageNotFoundException e )
            {
                e.printStackTrace();
                throw new WikiException( e.getMessage(), e );
            }
            return Outcome.STEP_COMPLETE;
        }
    }

    // page renaming code....................................................
    
    /**
     *  Renames a page.
     *  
     *  @param context The current context.
     *  @param renameFrom The name from which to rename.
     *  @param renameTo The new name.
     *  @param changeReferrers If true, also changes all the referrers.
     *  @return The final new name (in case it had to be modified)
     *  @throws WikiException If the page cannot be renamed.
     */
    public String renamePage( WikiContext context, 
                              String renameFrom, 
                              String renameTo, 
                              boolean changeReferrers )
        throws WikiException
    {
        //
        //  Sanity checks first
        //
        if( renameFrom == null || renameFrom.length() == 0 )
        {
            throw new WikiException( "From name may not be null or empty" );
        }
        if( renameTo == null || renameTo.length() == 0 )
        {
            throw new WikiException( "To name may not be null or empty" );
        }
       
        //
        //  Clean up the "to" -name so that it does not contain anything illegal
        //
        renameTo = MarkupParser.cleanLink( renameTo.trim() );
        if( renameTo.equals(renameFrom) )
        {
            throw new WikiException( "You cannot rename the page to itself" );
        }
        
        //
        //  Preconditions: "from" page must exist, and "to" page must NOT exist.
        //
        WikiEngine engine = context.getEngine();
        WikiPath fromPage = WikiPath.valueOf( renameFrom );
        WikiPath toPage = WikiPath.valueOf( renameTo );
        if ( !engine.pageExists( fromPage.toString() ) )
        {
            // TODO: Should localize this
            throw new WikiException("Cannot rename: source page '"+fromPage.toString() + "' does not exist." );
        }
        if ( engine.pageExists( toPage.toString() ) )
        {
            // TODO: Should localize this
            throw new WikiException("Cannot rename: destination page '"+toPage.toString() + "' already exists." );
        }
        
        //
        //  Do the actual rename by changing from the frompage to the topage, including
        //  all of the attachments
        //
        try
        {
            getCurrentSession().move( getJCRPath( fromPage ), getJCRPath( toPage ) );
            getCurrentSession().save();
        }
        catch( RepositoryException e )
        {
            throw new WikiException( "Could not rename page. Reason: " + e.getMessage(), e );
        }

        // Make sure the move succeeded...
        WikiPage page;
        try
        {
            page = engine.getPage( toPage );
        }
        catch ( PageNotFoundException e )
        {
            throw new InternalWikiException( "Rename seems to have failed for some strange reason - please check logs!" );
        }
        page.setAttribute( WikiPage.CHANGENOTE, fromPage.toString() + " ==> " + toPage.toString() );
        page.setAuthor( context.getCurrentUser().getName() );

        // Tell everyone we moved the page
        fireEvent( ContentEvent.NODE_RENAMED, toPage.toString(), fromPage.toString(), Boolean.valueOf( changeReferrers ) );
        
        //
        //  Done, return the new name.
        //
        return renameTo;
    }

    // events processing .......................................................

    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners.
     *
     * @see org.apache.wiki.event.WikiPageEvent
     * @param type the event type to be fired
     * @param pagename the wiki page name as a String
     * @param args additional arguments to pass to the event
     */
    protected final void fireEvent( int type, String pagename, Serializable... args )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiPageEvent(m_engine,type,pagename,args));
        }
    }
    
    /**
     *  Evaluates a WikiName in the context of the current page request.
     *  
     *  @param wikiName The WikiName.
     *  @return A full JCR path
     */
    public static String getJCRPath( WikiPath wikiName )
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
     *  @param jcrpath The JCR Path used to get the {@link WikiPath}
     *  @return The {@link WikiPath} for the requested jcr path
     *  @throws ProviderException If the backend fails.
     */
    // FIXME: Should be protected - fix once WikiPage moves to content-package
    public static WikiPath getWikiPath( String jcrpath ) throws ProviderException
    {
        if( jcrpath.startsWith("/"+JCR_PAGES_NODE+"/") )
        {
            String wikiPath = jcrpath.substring( ("/"+JCR_PAGES_NODE+"/").length() );

            int firstSlash = wikiPath.indexOf( '/' );
            
            if( firstSlash != -1 )
            {
                return new WikiPath(wikiPath.substring( 0, firstSlash ), 
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
    public JCRWikiPage addPage( WikiPath path, String contentType ) throws PageAlreadyExistsException, ProviderException
    {
        return addPage( path, getJCRPath(path), contentType );
    }

    /**
     *  Add new content to the repository to a particular JCR path.
     *  
     *  @param path
     *  @param jcrPath
     *  @param contentType
     *  @return
     *  @throws ProviderException
     */
    private JCRWikiPage addPage( WikiPath path, String jcrPath, String contentType ) 
        throws ProviderException
    {
        checkValidContentType( contentType );
        
        try
        {
            Session session = m_sessionManager.getSession();
        
            Node nd = session.getRootNode().addNode( jcrPath );
            
            nd.addMixin( "mix:referenceable" );
            nd.setProperty( JCRWikiPage.CONTENTTYPE, contentType );
            
            JCRWikiPage page = new JCRWikiPage(m_engine, path, nd);
            
            return page;
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Unable to add a page", e );
        }
    
    }
    
    /** Throws an exception if the content type is not a fully valid content type. */
    private void checkValidContentType( String type ) throws ProviderException
    {
        if( type == null ) throw new ProviderException("null content type");
        
        if( type.indexOf('/') == -1 ) throw new ProviderException("Not RFC compliant type");
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
    public JCRWikiPage getPage( WikiPath path ) throws ProviderException, PageNotFoundException
    {
        if ( path == null )
        {
            throw new PageNotFoundException( "null WikiPath given to getPage()" );
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

    /**
     *  Finds a WikiPage with a particular version.
     *  
     *  @param path Path of the page to find.
     *  @param version The version of the page to find
     *  @return A valid WikiPage
     *  @throws ProviderException If the backend fails.
     *  @throws PageNotFoundException If the page version in question cannot be found.
     */
    public JCRWikiPage getPage( WikiPath path, int version ) throws ProviderException, PageNotFoundException
    {
        try
        {
            Session session = m_sessionManager.getSession();
        
            Node original = session.getRootNode().getNode( getJCRPath(path) );
            
            Property p = original.getProperty( "wiki:version" );
            
            if( p.getLong() == version || version == WikiProvider.LATEST_VERSION )
                return new JCRWikiPage( m_engine, original );
            
            Node versionHistory = original.getNode( WIKI_VERSIONS );
            
            Node v = versionHistory.getNode( Integer.toString( version ) );
            
            return new JCRWikiPage( m_engine, path, v );
        }
        catch( PathNotFoundException e )
        {
            throw new PageNotFoundException("No such version "+version+" exists for path "+path);
        }
        catch( RepositoryException e )
        {
            throw new ProviderException( "Repository failed", e );
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
                List<WikiPage> pages = getAllPages( null );
                for ( Iterator<WikiPage> it = pages.iterator(); it.hasNext(); )
                {
                    WikiPage page = it.next();
                    Acl acl = changeAcl( page, oldPrincipals, newPrincipal );
                    if ( acl != null )
                    {
                        // If the Acl needed changing, change it now
                        try
                        {
                            m_engine.getAclManager().setPermissions( page, acl );
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
     *  with a new Principal. If the Acl is changed, the changed Acl is returned. Otherwise,
     *  (the Acl did not change), null is returned.
     * 
     *  @param page the wiki page whose Acl is to be modified
     *  @param oldPrincipals an array of Principals to replace; all AclEntry objects whose
     *   {@link AclEntry#getPrincipal()} method returns one of these Principals will be replaced
     *  @param newPrincipal the Principal that should receive the old Principals' permissions
     *  @return a non-<code>null</code> Acl if the Acl was actually changed; <code>null</code> otherwise
     */
    protected Acl changeAcl( WikiPage page, Principal[] oldPrincipals, Principal newPrincipal )
    {
        Acl acl = page.getAcl();
        boolean aclChanged = false;
        if ( acl != null )
        {
            Enumeration<AclEntry> entries = acl.entries();
            Collection<AclEntry> entriesToAdd    = new ArrayList<AclEntry>();
            Collection<AclEntry> entriesToRemove = new ArrayList<AclEntry>();
            while ( entries.hasMoreElements() )
            {
                AclEntry entry = entries.nextElement();
                if ( ArrayUtils.contains( oldPrincipals, entry.getPrincipal() ) )
                {
                    // Create new entry
                    AclEntry newEntry = new AclEntryImpl();
                    newEntry.setPrincipal( newPrincipal );
                    Enumeration<Permission> permissions = entry.permissions();
                    while ( permissions.hasMoreElements() )
                    {
                        Permission permission = permissions.nextElement();
                        newEntry.addPermission(permission);
                    }
                    aclChanged = true;
                    entriesToRemove.add( entry );
                    entriesToAdd.add( newEntry );
                }
            }
            for ( Iterator<AclEntry> ix = entriesToRemove.iterator(); ix.hasNext(); )
            {
                AclEntry entry = ix.next();
                acl.removeEntry( entry );
            }
            for ( Iterator<AclEntry> ix = entriesToAdd.iterator(); ix.hasNext(); )
            {
                AclEntry entry = ix.next();
                acl.addEntry( entry );
            }
        }
        return aclChanged ? acl : null;
    }

    /*
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
*/
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
                session = newSession(); 
                m_currentSession.set(session);
                return m_trueOwner;
            }
            return m_fakeOwner;
        }

        /**
         *  Creates a new Session object always which you will need to manually logout().
         *  
         *  @return A new Session object.
         *  @throws LoginException
         *  @throws RepositoryException
         */
        public Session newSession() throws LoginException, RepositoryException
        {
            Session session = m_repository.login( new SimpleCredentials( "jspwikiUser", "passwordDoesNotMatter".toCharArray() ),
                                                  m_workspaceName );
            return session;
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
            if( session != null )
            {
                session.logout();
                m_currentSession.set(null);
            }
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

    /**
     *  A shortcut to find a JCR Node from a given JCR Path.  This method should
     *  NOT be called unless you really, really know what you are doing.
     *  
     *  @param jcrPath An absolute JCR path.
     *  @return A JCR Node
     *  @throws RepositoryException If the Node cannot be found or something else fails.
     */
    public Node getJCRNode( String jcrPath ) throws RepositoryException
    {
        return (Node)m_sessionManager.getSession().getItem( jcrPath );
    }

    /**
     *  Creates a JCR Node including all of its parents from a given JCR Path.
     *  If the Node already exists, returns it.
     *  
     *  @param jcrPath An absolute or relative path. If relative, it's interpreted
     *                 as relative to the workspace root node.
     *  @return A valid JCR Node
     *  @throws RepositoryException If the creation fails for some reason
     */
    public Node createJCRNode( String jcrPath ) throws RepositoryException
    {
        String[] components = StringUtils.split( jcrPath, "/" );
        
        Node current = getCurrentSession().getRootNode();

        for( int i = 0; i < components.length; i++ )
        {
            if( !current.hasNode( components[i] ) )
            {
                current.addNode( components[i] );
            }
            current = current.getNode( components[i] );
        }
        
        return current;
    }
}
