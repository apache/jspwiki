/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.util;

import java.util.AbstractList;
import java.util.ArrayList;

/**
 *  Builds a simple, priority-based List implementation.  The list
 *  will be sorted according to the priority.  If two items are
 *  inserted with the same priority, their order is the insertion order - i.e. the new one
 *  is appended last in the insertion list.
 *  <p>
 *  Priority is an integer, and the list is sorted in descending order
 *  (that is, 100 is before 10 is before 0 is before -40).
 *
 *  @author Janne Jalkanen
 */
public class PriorityList
    extends AbstractList
{
    private ArrayList m_elements = new ArrayList();

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
            Item item = (Item) m_elements.get(i);

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
        return ((Item)m_elements.get( index )).m_object;
    }

    /**
     *  Returns the current size of the list.
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
