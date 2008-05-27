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
package com.ecyrd.jspwiki.dav.items;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public abstract class DavItem
{
    protected DavProvider m_provider;
    protected ArrayList  m_items = new ArrayList();
    protected DavPath     m_path;
    
    protected DavItem( DavProvider provider, DavPath path )
    {
        m_provider = provider;
        m_path     = path;
    }
    
    public DavPath getPath()
    {
        return m_path;
    }
    
    public abstract Collection getPropertySet();
    
    public abstract String getHref();
        
    public abstract InputStream getInputStream();
    
    public abstract long getLength();
    
    public abstract String getContentType();
    
    public Iterator iterator( int depth )
    {
        ArrayList list = new ArrayList();
        
        if( depth == 0 )
        {
            list.add( this );
        }
        else if( depth == 1 )
        {
            list.add( this );
            list.addAll( m_items );
        }
        else if( depth == -1 )
        {
            list.add( this );
            
            for( Iterator i = m_items.iterator(); i.hasNext(); )
            {
                DavItem di = (DavItem)i.next();
                                
                for( Iterator j = di.iterator(-1); i.hasNext(); )
                {
                    list.add( j.next() );
                }
            }
        }

        return list.iterator();
    }
}
