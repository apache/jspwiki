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
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
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
        ArrayList<DavItem> davItems = new ArrayList<DavItem>();
        
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
