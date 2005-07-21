package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;
import com.ecyrd.jspwiki.dav.items.HTMLPageDavItem;
import com.ecyrd.jspwiki.dav.items.PageDavItem;
import com.ecyrd.jspwiki.providers.ProviderException;

public class HTMLPagesDavProvider extends RawPagesDavProvider
{
    public HTMLPagesDavProvider( WikiEngine engine )
    {
        super(engine);
    }

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
                    DavItem di = new HTMLPageDavItem( this, p );
                
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
        
        if( pname.endsWith(".html") && pname.length() > 5 )
        {
            pname = pname.substring(0,pname.length()-5);
        }
        
        WikiPage page = m_engine.getPage( pname );
        
        if( page != null )
        {
            return new HTMLPageDavItem( this, page );
        }
        
        return null;
    }
}
