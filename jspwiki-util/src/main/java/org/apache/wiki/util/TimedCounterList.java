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
package org.apache.wiki.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 *  Provides a List in which all items store their addition time. This
 *  can then be used to clean the list from old items.
 *  <p>
 *  This class is thread-safe - all modifications are blocking, but
 *  reading is non-blocking (unless a write is ongoing).
 *  
 *  @param <T> The class you wish to store here
 *  @since 2.8
 */
public class TimedCounterList<T> extends AbstractList<T>
{
    private ArrayList<CounterItem<T>> m_list = new ArrayList<CounterItem<T>>();
    private ReadWriteLock             m_lock = new ReentrantReadWriteLock();
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public T set( int index, T element )
    {
        m_lock.writeLock().lock();
        
        T t;
        
        try
        {
            t = m_list.set(index,new CounterItem<T>(element)).m_obj;
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
        
        return t;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public T get( int index )
    {
        m_lock.readLock().lock();
        
        T t;
        
        try
        {
            t = m_list.get(index).m_obj;
        }
        finally
        {
            m_lock.readLock().unlock();
        }
        
        return t;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public int size()
    {
        m_lock.readLock().lock();
        int size = 0;

        try
        {
            size = m_list.size();
        }
        finally
        {
            m_lock.readLock().unlock();
        }
        
        return size;
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public void add( int index, T element )
    {
        m_lock.writeLock().lock();
        
        try
        {
            m_list.add(index, new CounterItem<T>(element));
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
    }
    
    /**
     *  {@inheritDoc}
     */
    @Override
    public T remove( int index )
    {
        m_lock.writeLock().lock();
        T t;

        try
        {
            t = m_list.remove( index ).m_obj;
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
        
        return t;
    }

    /**
     *  Returns the count how many times this object is available in
     *  this list, using equals().
     *  
     *  @param obj The object to count.
     *  @return The count of the objects.
     */
    public int count( T obj )
    {
        int c = 0;
        m_lock.readLock().lock();
        
        try
        {
            for( CounterItem< T > i : m_list )
            {
                if( i.m_obj.equals( obj ) )
                {
                    c++;
                }
            }
        }
        finally
        {
            m_lock.readLock().unlock();
        }
        
        return c;
    }
    
    /**
     *  Performs a cleanup of all items older than maxage.
     *  
     *  @param maxage The maximum age in milliseconds after an item is removed.
     */
    public void cleanup( long maxage )
    {
        m_lock.writeLock().lock();
        
        try
        {
            long now = System.currentTimeMillis();
        
            for( Iterator<CounterItem<T>> i = m_list.iterator(); i.hasNext(); )
            {
                CounterItem<T> ci = i.next();
            
                long age = now - ci.m_addTime;
            
                if( age > maxage )
                {
                    i.remove();
                }
            }
        }
        finally
        {
            m_lock.writeLock().unlock();
        }
    }
    
    /**
     *  Returns the time when this particular item was added on the list.
     *  
     *  @param index The index of the object.
     *  @return The addition time in milliseconds (@see System.currentTimeMillis()).
     */
    public long getAddTime( int index )
    {
        m_lock.readLock().lock();
        long res = 0;
        
        try
        {
            res = m_list.get( index ).m_addTime;
        }
        finally
        {
            m_lock.readLock().unlock();
        }
        
        return res;
    }
    
    private static class CounterItem<E>
    {
        private E      m_obj;
        private long   m_addTime;
        
        public CounterItem(E o)
        {
            m_addTime = System.currentTimeMillis();
            m_obj = o;
        }
    }


}
