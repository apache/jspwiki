/*
 * (C) Janne Jalkanen 2005
 * 
 */
package com.ecyrd.jspwiki.dav.items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.ecyrd.jspwiki.WikiEngine;

/**
 *  @author jalkanen
 *
 *  @since 
 */
public abstract class DavItem
{
    protected WikiEngine m_engine;
    protected ArrayList  m_items = new ArrayList();
    
    protected DavItem( WikiEngine engine )
    {
        m_engine = engine;
    }
    
    public abstract Collection getPropertySet();
    
    public abstract String getHref();
    
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
