/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;
import com.ecyrd.jspwiki.dav.items.PageDavItem;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 *  Implements something for the pages.
 *  
 *  @author jalkanen
 *
 *  @since
 */
public class RawPagesDavProvider extends WikiDavProvider
{
    protected static final Logger log = Logger.getLogger( RawPagesDavProvider.class );

    private Cache m_davItemCache = new Cache(true,false,false);
    
    private int m_refreshPeriod = 30*1000; // In millisseconds
    
    
    public RawPagesDavProvider( WikiEngine engine )
    {
        super(engine);
    }

    protected Collection listAlphabeticals( DavPath path )
    {
        ArrayList charList = new ArrayList();
        
        try
        {
            Collection allPages = m_engine.getPageManager().getAllPages();
            
            for( Iterator i = allPages.iterator(); i.hasNext(); )
            {
                String pageName = ((WikiPage)i.next()).getName();
                
                Character firstChar = new Character(Character.toLowerCase(pageName.charAt(0)));
                
                if( !charList.contains( firstChar ) )
                {
                    charList.add( firstChar );
                }
            }
        }
        catch( ProviderException e )
        {
            log.error("Could not fetch a list of all pages:",e);
        }
        
        Collections.sort( charList );
        
        ArrayList result = new ArrayList();
        
        for( Iterator i = charList.iterator(); i.hasNext(); )
        {
            Character c = (Character)i.next();
            
            result.add( new DirectoryItem(this, new DavPath(c.toString())) );
        }
        
        return result;
    }

    // FIXME: This is wasteful; this should really keep a structure of its
    // own in memory
    
    private Collection listDirContents( DavPath path )
    {
        String st = path.getName();
        
        log.info("Listing contents for dir "+st);
        ArrayList davItems = new ArrayList();
        
        try
        {
            Collection allPages = m_engine.getPageManager().getAllPages();
        
            for( Iterator i = allPages.iterator(); i.hasNext(); )
            {
                WikiPage p = (WikiPage)i.next();
                
                if( p.getName().toLowerCase().startsWith(st) )
                {
                    DavPath np = new DavPath( path );
                    np.append( p.getName()+".txt" );
                    
                    DavItem di = new PageDavItem( this, np, p );
                
                    davItems.add( di );
                }
            }
        }
        catch( ProviderException e )
        {
            log.error("Unable to fetch a list of all pages",e);
            // FIXME
        }
        return davItems;
    }

    public Collection listItems( DavPath path )
    {
        log.info("Listing dav path "+path+", size="+path.size());
        
        switch( path.size() )
        {
            case 1:
                return listAlphabeticals(path);
                
            case 2:
                return listDirContents( path );
                
            default:
                return null;
        }
    }

    protected String getRelativePath( String path )
    {
        if( path.length() > 0 )
        {
            char c = Character.toLowerCase( path.charAt(0) );
        
            return Character.toString(c);
        }
        
        return "";
    }
    
    public String getURL( DavPath path )
    {
        return m_engine.getURL( WikiContext.NONE, DavUtil.combineURL("dav/raw/",path.getPath()),
                                null, true );
    }
    
    public DavItem getItem( DavPath dp )
    {
        DavItem di = null;
    
        try
        {
            di = (DavItem)m_davItemCache.getFromCache( dp.toString(), 
                                                       m_refreshPeriod );
            
            if( di == null )
            {
                di = getItemNoCache( dp );
            }
        }
        catch( NeedsRefreshException e )
        {
            DavItem old = (DavItem)e.getCacheContent();
            
            if( old != null && old instanceof PageDavItem )
            {
                WikiPage cached = ((PageDavItem)old).getPage();
                
                WikiPage current = m_engine.getPage( cached.getName() );
                
                if( cached != null && 
                    cached.getLastModified().equals( current.getLastModified() ) )
                {
                    di = old;
                }
            }
            else
            {
                di = getItemNoCache( dp );
            }
        }

        m_davItemCache.putInCache( dp.toString(), di );

        return di;
    }

    protected DavItem getItemNoCache( DavPath path )
    {
        String pname = path.filePart();
        
        //
        //  Lists top-level elements
        //
        if( path.isRoot() )
        {
            log.info("Adding DAV items from path "+path);
            DirectoryItem di = new DirectoryItem( this, path );
            
            di.addDavItems( listAlphabeticals(path) );
            
            return di;
        }

        //
        //  Lists each item in each subdir
        //
        if( path.isDirectory() )
        {
            log.info("Listing pages in path "+path);
            
            DirectoryItem di = new DirectoryItem( this, path );
            
            di.addDavItems( listDirContents(path) );
            
            return di;
        }
        
        if( pname.endsWith(".txt") && pname.length() > 4 )
        {
            pname = pname.substring(0,pname.length()-4);
        }
        
        WikiPage page = m_engine.getPage( pname );
        
        if( page != null )
        {
            return new PageDavItem( this, path, page );
        }
        
        return null;
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

}
