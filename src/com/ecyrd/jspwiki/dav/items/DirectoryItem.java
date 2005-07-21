/*
 * (C) Janne Jalkanen 2005
 * 
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
        String davurl = m_path.getName(); //FIXME: Fixed, should determine from elsewhere
        
        if( !davurl.endsWith("/") ) davurl+="/";
        
        return m_provider.getURL( davurl );
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
