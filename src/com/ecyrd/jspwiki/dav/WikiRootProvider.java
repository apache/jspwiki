/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav;

import java.util.ArrayList;
import java.util.Collection;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.dav.items.DavItem;
import com.ecyrd.jspwiki.dav.items.TopLevelDavItem;

public class WikiRootProvider extends WikiDavProvider
{
    public WikiRootProvider( WikiEngine engine )
    {
        super( engine );
    }

    public Collection listItems( DavPath path )
    {
        ArrayList list = new ArrayList();
        
        list.add( new TopLevelDavItem(this) );
        
        return list;
    }

    public DavItem getItem( DavPath path )
    {
        return new TopLevelDavItem(this);
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

    public String getURL( String path )
    {
        if( path.equals("/") ) path = "";
        return m_engine.getURL( WikiContext.NONE, "dav/"+path, null, false );
    }

}
