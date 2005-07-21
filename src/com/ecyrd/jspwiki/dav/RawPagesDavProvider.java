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
                    DavItem di = new PageDavItem( this, p );
                
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

    private String getRelativePath( String path )
    {
        if( path.length() > 0 )
        {
            char c = Character.toLowerCase( path.charAt(0) );
        
            return Character.toString(c);
        }
        
        return "";
    }
    
    public String getURL( String path )
    {
        if( path.equals("/") ) path = "";
        
        return m_engine.getURL( WikiContext.NONE, "dav/raw/"+path,
                                null, true );
    }
    
    public DavItem refreshItem( DavItem old, DavPath path )
    {
        if( old instanceof PageDavItem )
        {
            WikiPage cached = ((PageDavItem)old).getPage();
            
            WikiPage current = m_engine.getPage( cached.getName() );
            
            if( cached.getLastModified().equals( current.getLastModified() ) )
            {
                return old;
            }
        }
        
        return getItem( path );
    }
    
    public DavItem getItem( DavPath path )
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
            return new PageDavItem( this, page );
        }
        
        return null;
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

}
