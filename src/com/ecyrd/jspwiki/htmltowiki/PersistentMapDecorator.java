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

    private Map delegate;

    public PersistentMapDecorator( Map delegate )
    {
        this.delegate = delegate;
    }

    public void clear()
    {
        delegate.clear();
    }

    public boolean containsKey( Object key )
    {
        return delegate.containsKey( key );
    }

    public boolean containsValue( Object value )
    {
        return delegate.containsValue( value );
    }

    public Set entrySet()
    {
        return delegate.entrySet();
    }

    public boolean equals( Object obj )
    {
        return delegate.equals( obj );
    }

    public Object get( Object key )
    {
        return delegate.get( key );
    }

    public int hashCode()
    {
        return delegate.hashCode();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public Set keySet()
    {
        return delegate.keySet();
    }

    public Object put( Object arg0, Object arg1 )
    {
        return delegate.put( arg0, arg1 );
    }

    public void putAll( Map arg0 )
    {
        delegate.putAll( arg0 );
    }

    public Object remove( Object key )
    {
        return delegate.remove( key );
    }

    public int size()
    {
        return delegate.size();
    }

    public String toString()
    {
        return delegate.toString();
    }

    public Collection values()
    {
        return delegate.values();
    }
}
