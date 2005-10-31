/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.IOException;
import java.util.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.RepositoryModifiedException;
import com.ecyrd.jspwiki.providers.VersioningProvider;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Manages the WikiPages.  This class functions as an unified interface towards
 *  the page providers.  It handles initialization and management of the providers,
 *  and provides utility methods for accessing the contents.
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
// FIXME: This class currently only functions just as an extra layer over providers,
//        complicating things.  We need to move more provider-specific functionality
//        from WikiEngine (which is too big now) into this class.
public class PageManager
{
    public static final String PROP_PAGEPROVIDER = "jspwiki.pageProvider";
    public static final String PROP_USECACHE     = "jspwiki.usePageCache";
    public static final String PROP_LOCKEXPIRY   = "jspwiki.lockExpiryTime";

    static Logger log = Logger.getLogger( PageManager.class );

    private WikiPageProvider m_provider;

    private HashMap m_pageLocks = new HashMap();

    private WikiEngine m_engine;

    /**
     *  The expiry time.  Default is 60 minutes.
     */
    private int     m_expiryTime = 60;

    /**
     *  Creates a new PageManager.
     *  @throws WikiException If anything goes wrong, you get this.
     */
    public PageManager( WikiEngine engine, Properties props )
        throws WikiException
    {
        String classname;

        m_engine = engine;

        boolean useCache = "true".equals(props.getProperty( PROP_USECACHE ));

        m_expiryTime = TextUtil.parseIntParameter( props.getProperty( PROP_LOCKEXPIRY ),
                                                   m_expiryTime );

        //
        //  If user wants to use a cache, then we'll use the CachingProvider.
        //
        if( useCache )
        {
            classname = "com.ecyrd.jspwiki.providers.CachingProvider";
        }
        else
        {
            classname = props.getProperty( PROP_PAGEPROVIDER );
        }

        try
        {
            Class providerclass = ClassUtil.findClass( "com.ecyrd.jspwiki.providers",
                                                       classname );

            m_provider = (WikiPageProvider)providerclass.newInstance();

            log.debug("Initializing page provider class "+m_provider);
            m_provider.initialize( m_engine, props );
        }
        catch( ClassNotFoundException e )
        {
            log.error("Unable to locate provider class "+classname,e);
            throw new WikiException("no provider class");
        }
        catch( InstantiationException e )
        {
            log.error("Unable to create provider class "+classname,e);
            throw new WikiException("faulty provider class");
        }
        catch( IllegalAccessException e )
        {
            log.error("Illegal access to provider class "+classname,e);
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

        //
        //  Start the lock reaper.
        //
        new LockReaper().start();
    }

    /**
     *  Returns the page provider currently in use.
     */
    public WikiPageProvider getProvider()
    {
        return m_provider;
    }

    public Collection getAllPages()
        throws ProviderException
    {
        return m_provider.getAllPages();
    }

    /**
     *  Fetches the page text from the repository.  This method also does some sanity checks,
     *  like checking for the pageName validity, etc.  Also, if the page repository has been
     *  modified externally, it is smart enough to handle such occurrences.
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
                WikiPage dummy = new WikiPage(m_engine,pageName);
                m_engine.getSearchManager().pageRemoved(dummy);
                m_engine.getReferenceManager().pageRemoved(dummy);
            }
        }

        return text;
    }

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
     *  @return null, if page could not be locked.
     */
    public PageLock lockPage( WikiPage page, String user )
    {
        PageLock lock = null;

        synchronized( m_pageLocks )
        {
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
    }

    /**
     *  Returns the current lock owner of a page.  If the page is not
     *  locked, will return null.
     *
     *  @return Current lock.
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

    public WikiPage getPageInfo( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
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

        return page;
    }

    /**
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  <P>
     *  @return If the page does not exist, returns null, otherwise a List
     *          of WikiPages.
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

    public String getProviderDescription()
    {
        return m_provider.getProviderInfo();
    }

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
     * @since 2.3.29
     * @param pageName
     * @param version
     * @return
     * @throws ProviderException
     */
    public boolean pageExists( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        if( m_provider instanceof VersioningProvider )
            return ((VersioningProvider)m_provider).pageExists( pageName, version );
        
        return m_provider.getPageInfo( pageName, version ) != null;
    }

    /**
     *  Deletes only a specific version of a WikiPage.
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
     */
    public void deletePage( WikiPage page )
        throws ProviderException
    {
        m_provider.deletePage( page.getName() );

        m_engine.getSearchManager().pageRemoved( page );

        m_engine.getReferenceManager().pageRemoved( page );        
    }

    /**
     *  This is a simple reaper thread that runs roughly every minute
     *  or so (it's not really that important, as long as it runs),
     *  and removes all locks that have expired.
     */
    private class LockReaper extends Thread
    {
        public void run()
        {
            while( true )
            {
                try
                {
                    Thread.sleep( 60 * 1000L );

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
                catch( Throwable t ) {}
            }
        }
    }
}
