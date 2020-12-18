/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.htmltowiki;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Adds the load / save - functionality known from the Properties - class to any
 * Map implementation.
 * 
 */
public class PersistentMapDecorator extends Properties
{
    private static final long serialVersionUID = 0L;
    
    private final Map< Object, Object > m_delegate;

    /**
     *  Creates a new decorator for a given map.
     *  
     *  @param delegate The map to create a decorator for.
     */
    public PersistentMapDecorator(final Map< Object, Object > delegate )
    {
        m_delegate = delegate;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void clear()
    {
        m_delegate.clear();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean containsKey(final Object key )
    {
        return m_delegate.containsKey( key );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean containsValue(final Object value )
    {
        return m_delegate.containsValue( value );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Set< Map.Entry< Object, Object > > entrySet()
    {
        return m_delegate.entrySet();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj )
    {
        return m_delegate.equals( obj );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Object get(final Object key )
    {
        return m_delegate.get( key );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return m_delegate.hashCode();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public boolean isEmpty()
    {
        return m_delegate.isEmpty();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Set< Object > keySet()
    {
        return m_delegate.keySet();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Object put(final Object arg0, final Object arg1 )
    {
        return m_delegate.put( arg0, arg1 );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void putAll(final Map< ?, ? > arg0 )
    {
        m_delegate.putAll( arg0 );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Object remove(final Object key )
    {
        return m_delegate.remove( key );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int size()
    {
        return m_delegate.size();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return m_delegate.toString();
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< Object > values()
    {
        return m_delegate.values();
    }
}
