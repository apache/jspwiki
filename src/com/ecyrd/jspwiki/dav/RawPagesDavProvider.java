/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DavItemFactory;
import com.ecyrd.jspwiki.dav.items.PageDavItem;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Implements something for the pages.
 *  
 *  @author jalkanen
 *
 *  @since
 */
public class RawPagesDavProvider implements DavProvider
{
    private WikiEngine m_engine;
    
    private static final Logger log = Logger.getLogger( RawPagesDavProvider.class );
    
    public RawPagesDavProvider( WikiEngine engine )
    {
        m_engine = engine;
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

    public DavItem getItem( DavPath path )
    {
        // TODO Auto-generated method stub
        return null;
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

}
