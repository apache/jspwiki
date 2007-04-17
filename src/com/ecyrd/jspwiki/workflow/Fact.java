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

    public String getMessageKey()
    {
        return m_key;
    }

    public Object getValue()
    {
        return m_obj;
    }
    
    public boolean equals( Object obj )
    {
        if ( !( obj instanceof Fact ) ) 
        {
            return false;
        }
        
        Fact f = (Fact)obj;
        return ( m_key.equals( f.m_key) && m_obj.equals( f.m_obj ) );
    }
    
    public int hashCode()
    {
        return m_key.hashCode() + 41 * m_obj.hashCode();
    }

    /**
     * Returns a String representation of this Fact.
     */
    public String toString()
    {
        return "[Fact:" + m_obj.toString() + "]";
    }
}
