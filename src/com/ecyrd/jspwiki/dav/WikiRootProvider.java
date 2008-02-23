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

    public DavItem refreshItem( DavItem old, DavPath path )
    {
        return new TopLevelDavItem(this);
    }

    public void setItem( DavPath path, DavItem item )
    {
    // TODO Auto-generated method stub

    }

    public String getURL( DavPath path )
    {
        return DavUtil.combineURL( DavUtil.combineURL( m_engine.getBaseURL() , "dav/"), path.getPath() );
    }

}
