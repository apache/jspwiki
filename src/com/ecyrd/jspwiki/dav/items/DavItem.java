/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.WikiEngine;
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
