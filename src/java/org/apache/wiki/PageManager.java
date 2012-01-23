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

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.*;
import org.apache.wiki.content.lock.PageLock;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.modules.ModuleManager;
import org.apache.wiki.modules.WikiModuleInfo;
import org.apache.wiki.providers.ProviderException;
import org.apache.wiki.providers.WikiPageProvider;


/**
 * Use ContentManager instead.
 * 
 * @deprecated
 */
public class PageManager extends ModuleManager
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

    static Logger log = LoggerFactory.getLogger( PageManager.class );

    private WikiPageProvider m_provider;

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
    public List<WikiPage> getAllPages()
        throws ProviderException
    {
        return m_engine.getContentManager().getAllPages(null);
    }

    /**
     *  Fetches the page text from the repository.  This method also does some sanity checks,
     *  like checking for the pageName validity, etc.  Also, if the page repository has been
     *  modified externally, it is smart enough to handle such occurrences.
     *  
     *  @param pageName The name of the page to fetch.
     *  @param version The version to find
     *  @return The page content as a raw string
     *  @throws PageNotFoundException if the wiki page cannot be found
     *  @throws ProviderException If the backend has issues.
     */
    public String getPageText( String pageName, int version )
        throws PageNotFoundException, ProviderException
    {
        WikiPage p = m_engine.getContentManager().getPage( WikiPath.valueOf( pageName ), version );
        
        if( p != null ) return p.getContentAsString();
        
        return null;
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
        WikiPage p;
        try
        {
            p = m_engine.getContentManager().getPage( page.getPath() );
        }
        catch( PageNotFoundException e )
        {
            try
            {
                p = m_engine.getContentManager().addPage( page.getPath(), ContentManager.JSPWIKI_CONTENT_TYPE );
            }
            catch( PageAlreadyExistsException e1 )
            {
                // This should never happen
                throw new ProviderException( e1.getMessage(), e1 );
            }
        }
        p.setContent(content);
        p.save();
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
        return m_engine.getContentManager().lockPage( page, user );
    }

    /**
     *  Marks a page free to be written again.  If there has not been a lock,
     *  will fail quietly.
     *
     *  @param lock A lock acquired in lockPage().  Safe to be null.
     */
    public void unlockPage( PageLock lock )
    {
        m_engine.getContentManager().unlockPage( lock );
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
        return m_engine.getContentManager().getCurrentLock( page );
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
        return m_engine.getContentManager().getActiveLocks();
    }

    /**
     *  Finds a WikiPage object describing a particular page and version.
     *  
     *  @param pageName  The name of the page
     *  @param version   A version number
     *  @return          A WikiPage object, or null, if the page does not exist
     *  @throws PageNotFoundException if the wiki page cannot be found
     *  @throws ProviderException If there is something wrong with the page 
     *                            name or the repository
     */
    public WikiPage getPageInfo( String pageName, int version )
        throws PageNotFoundException, ProviderException
    {
        return m_engine.getContentManager().getPage( WikiPath.valueOf( pageName ), version );        
    }

    /**
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  
     *  @param pageName The name of the page to fetch history for
     *  @return If the page does not exist, returns null, otherwise a List
     *          of WikiPages.
     *  @throws PageNotFoundException if the wiki page cannot be found
     *  @throws ProviderException If the repository fails.
     */
    public List<WikiPage> getVersionHistory( String pageName )
        throws PageNotFoundException, ProviderException
    {
        return m_engine.getContentManager().getVersionHistory( WikiPath.valueOf( pageName ) );
    }

    /**
     *  Returns a human-readable description of the current provider.
     *  
     *  @return A human-readable description.
     */
    public String getProviderDescription()
    {
        return m_engine.getContentManager().getProviderDescription();
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
            return m_engine.getContentManager().getTotalPageCount( null );
        }
        catch( ProviderException e )
        {
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
        return m_engine.pageExists( pageName );
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
        return m_engine.pageExists( pageName, version );
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
        m_engine.getContentManager().deleteVersion( page );
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
        m_engine.getContentManager().deletePage( page );
    }

 
    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection<WikiModuleInfo> modules()
    {
        // TODO Auto-generated method stub
        return null;
    }

}
