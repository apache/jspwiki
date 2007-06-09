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
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.DirectoryItem;
import com.ecyrd.jspwiki.dav.items.HTMLPageDavItem;
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
                    DavPath dp = new DavPath( path );
                    dp.append( p.getName()+".html" );
                    
                    DavItem di = new HTMLPageDavItem( this, dp, p );
                
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
        
        if( pname.endsWith(".html") && pname.length() > 5 )
        {
            pname = pname.substring(0,pname.length()-5);
        }
        
        WikiPage page = m_engine.getPage( pname );
        
        if( page != null )
        {
            return new HTMLPageDavItem( this, path, page );
        }
        
        return null;
    }
    
    public String getURL( DavPath path )
    {
        return m_engine.getURL( WikiContext.NONE, DavUtil.combineURL("dav/html",path.getPath()),
                                null, true );
    }

}
