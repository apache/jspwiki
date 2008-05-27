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
import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;
import org.jdom.Element;
import org.jdom.Namespace;

import com.ecyrd.jspwiki.dav.DavPath;
import com.ecyrd.jspwiki.dav.DavProvider;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class DirectoryItem extends DavItem
{
    public DirectoryItem( DavProvider provider, DavPath path )
    {
        super( provider, path );
    }
    
    public String getContentType()
    {
        return "text/plain; charset=UTF-8";
    }

    public long getLength()
    {
        return -1;
    }

    public Collection getPropertySet()
    {
        ArrayList ts = new ArrayList();
        Namespace davns = Namespace.getNamespace( "DAV:" );
        
        ts.add( new Element("resourcetype",davns).addContent(new Element("collection",davns)) );
        
        Element txt = new Element("displayname",davns);
        txt.setText( m_path.getName() );
        ts.add( txt );

        ts.add( new Element("getcontentlength",davns).setText("0") );
        ts.add( new Element("getlastmodified", davns).setText(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date())));
        
        
        return ts;
    }

    public String getHref()
    {
        return m_provider.getURL( m_path );
    }
    
    public void addDavItem( DavItem di )
    {
        m_items.add( di );
    }

    public void addDavItems( Collection c )
    {
        m_items.addAll( c );
    }
    
    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.dav.items.DavItem#getInputStream()
     */
    public InputStream getInputStream()
    {
        // TODO Auto-generated method stub
        return null;
    }
}
