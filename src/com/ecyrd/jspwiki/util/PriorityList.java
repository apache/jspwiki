package com.ecyrd.jspwiki.util;

import java.util.AbstractList;
import java.util.ArrayList;

/**
 *  Builds a simple, priority-based List implementation.  The list
 *  will be sorted according to the priority.  If two items are
 *  inserted with the same priority, their order is undetermined.
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

    public boolean add( Object o )
    {
        add( o, DEFAULT_PRIORITY );

        return true;
    }

    public Object get( int index )
    {
        return ((Item)m_elements.get( index )).m_object;
    }

    public int size()
    {
        return m_elements.size();
    }

    private class Item
    {
        public int     m_priority;
        public Object  m_object;
    }
}
