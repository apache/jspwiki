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
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *  Builds a simple, priority-based List implementation.  The list
 *  will be sorted according to the priority.  If two items are
 *  inserted with the same priority, their order is the insertion order - i.e. the new one
 *  is appended last in the insertion list.
 *  <p>
 *  Priority is an integer, and the list is sorted in descending order
 *  (that is, 100 is before 10 is before 0 is before -40).
 */
public class PriorityList
    extends AbstractList
{
    private CopyOnWriteArrayList<Item> m_elements = new CopyOnWriteArrayList<Item>();

    /**
     *  This is the default priority, which is used if no priority
     *  is defined.  It's current value is zero.
     */
    public static final int DEFAULT_PRIORITY = 0;

    /**
     *  Adds an object to its correct place in the list, using the
     *  given priority.
     *
     *  @param o Object to add.
     *  @param priority Priority.
     */
    public void add( Object o, int priority )
    {
        int i = 0;

        for( ; i < m_elements.size(); i++ )
        {
            Item item = m_elements.get(i);

            if( item.m_priority < priority )
            {
                break;
            }
        }

        Item newItem = new Item();
        newItem.m_priority = priority;
        newItem.m_object   = o;

        m_elements.add( i, newItem );
    }

    /**
     *  Adds an object using the default priority to the List.
     *
     *  @param o Object to add.
     *  @return true, as per the general Collections.add contract.
     */
    public boolean add( Object o )
    {
        add( o, DEFAULT_PRIORITY );

        return true;
    }

    /**
     *  Returns the object at index "index".
     *
     *  @param index The index.
     *  @return The object at the list at the position "index".
     */
    public Object get( int index )
    {
        return m_elements.get( index ).m_object;
    }

    /**
     *  Returns the current size of the list.
     *  
     *  @return size of the list.
     */
    public int size()
    {
        return m_elements.size();
    }

    /**
     *  Provides a holder for the priority-object 2-tuple.
     */
    private static class Item
    {
        public int     m_priority;
        public Object  m_object;
    }
}
