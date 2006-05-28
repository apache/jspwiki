package com.ecyrd.jspwiki.auth.authorize;

import java.lang.ref.WeakReference;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;
import java.util.WeakHashMap;

import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;

/**
 * Provides a concrete implementation of the {@link Group} interface.
 * @author Andrew Jaquith
 * @version $Revision: 1.8 $ $Date: 2006-05-28 23:23:12 $
 * @since 2.3
 */
public class DefaultGroup implements Group

{
    private final Map          m_listeners = new WeakHashMap();
    
    private final Vector       m_members = new Vector();

    private final String       m_name;

    public DefaultGroup( String name )
    {
        m_name = name;
    }

    /**
     * Adds a Principal to the group. When a Principal is 
     * added successfully, also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD_MEMBER}
     * to all of its registered WikiEventListeners.
     * @param user the principal to add
     * @return <code>true</code> if the operation was successful
     */
    public boolean add( Principal user )
    {
        if ( isMember( user ) )
        {
            return false;
        }

        m_members.add( user );
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_ADD_MEMBER, user ) );
        return true;
    }

    /**
     * Registers a WikiEventListener with this Group.
     * @param listener the event listener
     */
    public synchronized void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.put( listener, new WeakReference( listener ) );
    }
    
    /**
     * Clears all Principals from the group list. When a 
     * Group's members are cleared successfully, 
     * also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_CLEAR_MEMBERS}
     * to all of its registered WikiEventListeners.
     */
    public void clear()
    {
        m_members.clear();
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_CLEAR_MEMBERS, null ) );
    }

    /**
     * Two DefaultGroups are equal if they contain identical member Principals
     * and have the same name.
     */
    public boolean equals( Object o )
    {
        if ( o == null || !( o instanceof DefaultGroup ) )
            return false;

        DefaultGroup g = (DefaultGroup) o; // Just a shortcut.

        if ( g.m_members.size() != m_members.size() )
            return false;

        if ( getName() != null && !getName().equals( g.getName() ) )
        {
            return false;
        }
        else if ( getName() == null && g.getName() != null )
        {
            return false;
        }

        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            if ( !( g.isMember( (Principal) i.next() ) ) )
            {
                return false;
            }
        }

        return true;
    }

    public String getName()
    {
        return m_name;
    }

    /**
     * Returns <code>true</code> if a Principal is a member of the group.
     * Specifically, the Principal's <code>getName()</code>
     * method must return the same value as one of the Principals in the group
     * member list. The Principal's type does <em>not</em> need to match.
     */
    public boolean isMember( Principal principal )
    {
        return findMember( principal.getName() ) != null;
    }

    /**
     * Removes a Principal from the group. When a Principal is 
     * added successfully, also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE_MEMBER}
     * to all of its registered WikiEventListeners.
     * @param user the principal to remove
     * @return <code>true</code> if the operation was successful
     */
    public boolean remove( Principal user )
    {
        user = findMember( user.getName() );

        if ( user == null )
            return false;

        m_members.remove( user );
        fireEvent( new WikiSecurityEvent( this, WikiSecurityEvent.GROUP_REMOVE_MEMBER, user ) );

        return true;
    }

    /**
     * Un-registers a WikiEventListener with this Group.
     * @param listener the event listener
     */
    public synchronized void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.remove( listener );
    }
    
    /**
     * Returns a string representation of the group.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "[DefaultGroup: " + getName() + "]" );
        return sb.toString();
    }

    private Principal findMember( String name )
    {
        for( Iterator i = m_members.iterator(); i.hasNext(); )
        {
            Principal member = (Principal) i.next();

            if ( member.getName().equals( name ) )
            {
                return member;
            }
        }

        return null;
    }
    
    /**
     * Fires a wiki event to all registered listeners.
     * @param event the event
     */
    private void fireEvent( WikiSecurityEvent event )
    {
        for (Iterator it = m_listeners.keySet().iterator(); it.hasNext(); )
        {
            WikiEventListener listener = (WikiEventListener)it.next();
            listener.actionPerformed(event);
        }
    }
}