/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.workflow;

/**
 * Represents a contextual artifact, which can be any Object, useful for making
 * a Decision. Facts are key-value pairs, where the key is a String (message
 * key) and the value is an arbitrary Object. Generally, the supplied object's
 * {@link #toString()} method should return a human-readable String. Facts are
 * immutable objects.
 * 
 * @author Andrew Jaquith
 * @since 2.5
 */
public final class Fact
{
    private final String m_key;

    private final Object m_obj;

    /**
     * Constructs a new Fact with a supplied message key and value.
     * 
     * @param messageKey
     *            the "name" of this fact, which should be an i18n message key
     * @param value
     *            the object to associate with the name
     */
    public Fact(String messageKey, Object value)
    {
        if ( messageKey == null || value == null )
        {
            throw new IllegalArgumentException( "Fact message key or value parameters must not be null." );
        }
        m_key = messageKey;
        m_obj = value;
    }

    /**
     * Returns this Fact's name, as represented an i18n message key.
     * @return the message key
     */
    public String getMessageKey()
    {
        return m_key;
    }

    /**
     * Returns this Fact's value.
     * @return the value object
     */
    public Object getValue()
    {
        return m_obj;
    }
    
    /**
     * Two Facts are considered equal if their message keys and value objects are equal.
     * @param obj the object to test
     * @return <code>true</code> if logically equal, <code>false</code> if not
     */
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof Fact ) ) 
        {
            return false;
        }
        
        Fact f = (Fact)obj;
        return m_key.equals( f.m_key) && m_obj.equals( f.m_obj );
    }
    
    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return m_key.hashCode() + 41 * m_obj.hashCode();
    }

    /**
     * Returns a String representation of this Fact.
     * @return the representation
     */
    public String toString()
    {
        return "[Fact:" + m_obj.toString() + "]";
    }
}
