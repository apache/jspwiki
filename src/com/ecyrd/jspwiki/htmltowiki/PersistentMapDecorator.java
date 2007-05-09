package com.ecyrd.jspwiki.htmltowiki;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Adds the load / save - functionality known from the Properties - class to any
 * Map implementation.
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class PersistentMapDecorator extends Properties
{
    private static final long serialVersionUID = 0L;
    
    private Map m_delegate;

    public PersistentMapDecorator( Map delegate )
    {
        this.m_delegate = delegate;
    }

    public void clear()
    {
        m_delegate.clear();
    }

    public boolean containsKey( Object key )
    {
        return m_delegate.containsKey( key );
    }

    public boolean containsValue( Object value )
    {
        return m_delegate.containsValue( value );
    }

    public Set entrySet()
    {
        return m_delegate.entrySet();
    }

    public boolean equals( Object obj )
    {
        return m_delegate.equals( obj );
    }

    public Object get( Object key )
    {
        return m_delegate.get( key );
    }

    public int hashCode()
    {
        return m_delegate.hashCode();
    }

    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }

    public Set keySet()
    {
        return m_delegate.keySet();
    }

    public Object put( Object arg0, Object arg1 )
    {
        return m_delegate.put( arg0, arg1 );
    }

    public void putAll( Map arg0 )
    {
        m_delegate.putAll( arg0 );
    }

    public Object remove( Object key )
    {
        return m_delegate.remove( key );
    }

    public int size()
    {
        return m_delegate.size();
    }

    public String toString()
    {
        return m_delegate.toString();
    }

    public Collection values()
    {
        return m_delegate.values();
    }
}
