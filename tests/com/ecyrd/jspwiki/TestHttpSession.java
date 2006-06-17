package com.ecyrd.jspwiki;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

/**
 * This is a mock HttpSession implementation. It is used for testing.
 * Most methods work as the should; notable exceptions include
 * getSessionContext/getServletContext.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.4 $ $Date: 2006-06-17 23:21:08 $
 */
public class TestHttpSession implements HttpSession
{

    protected final long          m_createTime;

    protected static final Random RANDOM = new Random();

    protected final String        m_id;

    protected final long          m_lastAccessTime;

    protected final Map           m_attributes;
    
    protected int                 m_inactiveInterval;

    protected boolean             m_invalidated;

    /**
     * Constructs a new instance of this class.
     */
    public TestHttpSession()
    {
        m_createTime = System.currentTimeMillis();
        m_lastAccessTime = m_createTime;
        m_id = String.valueOf( RANDOM.nextLong() );
        m_attributes = new HashMap();
        m_invalidated = false;
        m_inactiveInterval = 1800;
    }

    /**
     * @see javax.servlet.http.HttpSession#getCreationTime()
     */
    public long getCreationTime()
    {
        return m_createTime;
    }

    /**
     * @see javax.servlet.http.HttpSession#getId()
     */
    public String getId()
    {
        return m_id;
    }

    /**
     * @see javax.servlet.http.HttpSession#getLastAccessedTime()
     */
    public long getLastAccessedTime()
    {
        return m_lastAccessTime;
    }

    /**
     * Always returns null.
     * @see javax.servlet.http.HttpSession#getServletContext()
     */
    public ServletContext getServletContext()
    {
        return null;
    }

    /**
     * No-op.
     * @see javax.servlet.http.HttpSession#setMaxInactiveInterval(int)
     */
    public void setMaxInactiveInterval( int arg0 )
    {
        m_inactiveInterval = arg0;
    }

    /**
     * Returns the number of seconds of allowed inactivity;
     * returns 1800 seconds (30 minutes) by default.
     * @see javax.servlet.http.HttpSession#getMaxInactiveInterval()
     */
    public int getMaxInactiveInterval()
    {
        return m_inactiveInterval;
    }

    /**
     * No-op; returns null;
     * @see javax.servlet.http.HttpSession#getSessionContext()
     * @deprecated
     */
    public HttpSessionContext getSessionContext()
    {
        return null;
    }

    /**
     * Returns the attribute with a given name, or null of no object is bound
     * with that name.
     * @see javax.servlet.http.HttpSession#getAttribute(java.lang.String)
     */
    public Object getAttribute( String arg0 )
    {
        return m_attributes.get( arg0 );
    }

    /**
     * Delegates to {@link #getAttribute(String)}.
     * @see javax.servlet.http.HttpSession#getValue(java.lang.String)
     * @deprecated
     */
    public Object getValue( String arg0 )
    {
        return getAttribute( arg0 );
    }

    /**
     * @see javax.servlet.http.HttpSession#getAttributeNames()
     */
    public Enumeration getAttributeNames()
    {
        Vector items = new Vector();
        for( Iterator it = m_attributes.keySet().iterator(); it.hasNext(); )
        {
            items.add( it.next() );
        }
        return items.elements();
    }

    /**
     * @see javax.servlet.http.HttpSession#getValueNames()
     * @deprecated
     */
    public String[] getValueNames()
    {
        Vector items = new Vector();
        for( Iterator it = m_attributes.keySet().iterator(); it.hasNext(); )
        {
            items.add( it.next() );
        }
        return (String[]) items.toArray( new String[items.size()] );
    }

    /**
     * @see javax.servlet.http.HttpSession#setAttribute(java.lang.String,
     *      java.lang.Object)
     */
    public void setAttribute( String arg0, Object arg1 )
    {
        m_attributes.put( arg0, arg1 );
    }

    /**
     * @see javax.servlet.http.HttpSession#putValue(java.lang.String,
     *      java.lang.Object)
     * @deprecated
     */
    public void putValue( String arg0, Object arg1 )
    {
        setAttribute( arg0, arg1 );
    }

    /**
     * @see javax.servlet.http.HttpSession#removeAttribute(java.lang.String)
     */
    public void removeAttribute( String arg0 )
    {
        m_attributes.remove( arg0 );
    }

    /**
     * @see javax.servlet.http.HttpSession#removeValue(java.lang.String)
     * @deprecated
     */
    public void removeValue( String arg0 )
    {
        removeAttribute( arg0 );
    }

    /**
     * Invalidates the session.
     * @see javax.servlet.http.HttpSession#invalidate()
     */
    public void invalidate()
    {
        if ( m_invalidated )
        {
            throw new IllegalStateException( "Session is already invalidated." );
        }
        m_invalidated = true;
        m_attributes.clear();
    }

    /**
     * Always returns false.
     * @see javax.servlet.http.HttpSession#isNew()
     */
    public boolean isNew()
    {
        return false;
    }

}