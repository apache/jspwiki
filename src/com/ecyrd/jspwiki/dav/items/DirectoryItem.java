/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.TreeSet;

import org.apache.commons.lang.time.DateFormatUtils;
import org.jdom.Element;
import org.jdom.Namespace;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public class DirectoryItem extends DavItem
{
    private String    m_name;
    
    public DirectoryItem( WikiEngine engine, String name )
    {
        super( engine );
        m_name = name;
    }
    
    public Collection getPropertySet()
    {
        ArrayList ts = new ArrayList();
        Namespace davns = Namespace.getNamespace( "DAV:" );
        
        ts.add( new Element("resourcetype",davns).addContent(new Element("collection",davns)) );
        
        Element txt = new Element("displayname",davns);
        txt.setText( m_name );
        ts.add( txt );

        ts.add( new Element("getcontentlength",davns).setText("0") );
        ts.add( new Element("getlastmodified", davns).setText(DateFormatUtils.ISO_DATETIME_TIME_ZONE_FORMAT.format(new Date())));
        
        
        return ts;
    }

    public String getHref()
    {
        String davurl = "dav"+(m_name.equals("/") ? "" : "/") +m_name; //FIXME: Fixed, should determine from elsewhere
        
        if( !davurl.endsWith("/") ) davurl+="/";
        
        return m_engine.getURL( WikiContext.NONE, davurl, null, true );
    }
    
    public void addDavItem( DavItem di )
    {
        m_items.add( di );
    }
}
