/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DavItemFactory;
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
    private static final Logger log = Logger.getLogger( RawPagesDavProvider.class );
    
    public RawPagesDavProvider( WikiEngine engine )
    {
        super(engine);
    }

    public Collection listItems( DavPath path )
    {
        ArrayList davItems = new ArrayList();
        
        try
        {
            Collection allPages = m_engine.getPageManager().getAllPages();
        
            for( Iterator i = allPages.iterator(); i.hasNext(); )
            {
                DavItem di = new PageDavItem( this, (WikiPage) i.next() );
                
                davItems.add( di );
            }
        }
        catch( ProviderException e )
        {
            log.error("Unable to fetch a list of all pages",e);
            // FIXME
        }
        return davItems;
    }

    public String getURL( String path )
    {
        if( path.equals("/") ) path = "";
        return m_engine.getURL( WikiContext.NONE, "dav/raw/"+path, null, true );
    }
    
    public DavItem getItem( DavPath path )
    {
        String pname = path.filePart();
        
        if( path.isRoot() )
        {
            DirectoryItem di = new DirectoryItem( this, "" );
            
            try
            {
                Collection allPages = m_engine.getPageManager().getAllPages();
            
                for( Iterator i = allPages.iterator(); i.hasNext(); )
                {
                    DavItem pdi = new PageDavItem( this, (WikiPage) i.next() );
                    
                    di.addDavItem( pdi );
                }
            }
            catch( ProviderException ex )
            {
                log.error("Failed to fetch page list",ex);
                return null;
            }
            
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
