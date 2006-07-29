package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import com.ecyrd.jspwiki.auth.GroupPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;
import com.ecyrd.jspwiki.event.EventSourceDelegate;
import com.ecyrd.jspwiki.event.WikiEvent;
import com.ecyrd.jspwiki.event.WikiEventListener;
import com.ecyrd.jspwiki.event.WikiEventSource;

/**
 * <p>
 * Groups are a specialized type of ad-hoc role used by the wiki system. Unlike
 * externally-provided roles (such as those provided by an LDAP server or web
 * container), JSPWiki groups can be created dynamically by wiki users, without
 * requiring special container privileges or administrator intervention. They
 * are designed to provide a lightweight role-based access control system that
 * complements existing role systems.
 * </p>
 * <p>
 * Group names are case-insensitive, and have a few naming restrictions, which
 * are enforced by the {@link GroupManager}:
 * </p>
 * <ul>
 * <li>Groups cannot have the same name as a built-in Role (e.g., "Admin",
 * "Authenticated" etc.)</li>
 * <li>Groups cannot have the same name as an existing user</li>
 * </ul>
 * <p>
 * <em>Note: prior to JSPWiki 2.4.19, Group was an interface; it
 * is now a concrete, final class.</em>
 * </p>
 * <p>
 * Groups are related to {@link GroupPrincipal}s. A GroupPrincipal, when
 * injected into the Principal set of a WikiSession's Subject, means that the
 * user is a member of a Group of the same name -- it is, in essence, an
 * "authorization token." GroupPrincipals, unlike Groups, are thread-safe,
 * lightweight and immutable. That's why we use them in Subjects rather than the
 * Groups themselves.
 * </p>
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.8 $ $Date: 2006-07-29 19:18:01 $
 * @since 2.3
 */
public class Group implements WikiEventSource
{

    public static String[]  RESTRICTED_GROUPNAMES = new String[]
                                                  { "Anonymous", "All", "Asserted", "Authenticated" };

    /** Delegate for managing event listeners */
    private EventSourceDelegate m_listeners       = new EventSourceDelegate();

    private final Vector    m_members             = new Vector();

    private String          m_creator             = null;

    private Date            m_created             = null;

    private String          m_modifier            = null;

    private Date            m_modified            = null;

    private final String    m_name;

    private final Principal m_principal;

    private final String    m_wiki;

    /**
     * Protected constructor to prevent direct instantiation except by other
     * package members. Callers should use
     * {@link GroupManager#parseGroup(String, String, boolean)} or
     * {@link GroupManager#parseGroup(com.ecyrd.jspwiki.WikiContext, boolean)}.
     * instead.
     * @param name the name of the group
     */
    protected Group( String name, String wiki )
    {
        m_name = name;
        m_wiki = wiki;
        m_principal = new GroupPrincipal( wiki, name );
    }

    /**
     * Adds a Principal to the group. When a Principal is added successfully,
     * also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD_MEMBER} to all
     * of its registered WikiEventListeners.
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
     * @see com.ecyrd.jspwiki.event.WikiEventSource#addWikiEventListener(WikiEventListener)
     */
    public final void addWikiEventListener( WikiEventListener listener )
    {
        m_listeners.addWikiEventListener( listener );
    }

    /**
     * Clears all Principals from the group list. When a Group's members are
     * cleared successfully, also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_CLEAR_MEMBERS} to
     * all of its registered WikiEventListeners.
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
        if ( o == null || !( o instanceof Group ) )
            return false;

        Group g = (Group) o; // Just a shortcut.

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

    /**
     * Returns the creation date.
     * @return the creation date
     */
    public Date getCreated()
    {
        return m_created;
    }

    /**
     * Returns the creator of this Group.
     * @return the creator
     */
    public final String getCreator()
    {
        return m_creator;
    }

    /**
     * Returns the last-modified date.
     * @return the date and time of last modification
     */
    public Date getLastModified()
    {
        return m_modified;
    }

    /**
     * Returns the name of the user who last modified this group.
     * @return the modifier
     */
    public final String getModifier()
    {
        return m_modifier;
    }

    /**
     * The name of the group. This is set in the class constructor.
     * @return the name of the Group
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Returns the GroupPrincipal that represents this Group.
     * @return the group principal
     */
    public Principal getPrincipal()
    {
        return m_principal;
    }

    /**
     * Returns the wiki name.
     * @return the wiki name
     */
    public String getWiki()
    {
        return m_wiki;
    }

    /**
     * Returns <code>true</code> if a Principal is a member of the group.
     * Specifically, the Principal's <code>getName()</code> method must return
     * the same value as one of the Principals in the group member list. The
     * Principal's type does <em>not</em> need to match.
     * @param principal the principal about whom membeship status is sought
     * @return the result of the operation
     */
    public boolean isMember( Principal principal )
    {
        return findMember( principal.getName() ) != null;
    }

    /**
     * Returns the members of the group as an array of Principal objects.
     */
    public Principal[] members()
    {
        return (Principal[]) m_members.toArray( new Principal[m_members.size()] );
    }

    /**
     * Removes a Principal from the group. When a Principal is added
     * successfully, also sends a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE_MEMBER} to
     * all of its registered WikiEventListeners.
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
     * @see com.ecyrd.jspwiki.event.WikiEventSource#removeWikiEventListener(WikiEventListener)
     */
    public final void removeWikiEventListener( WikiEventListener listener )
    {
        m_listeners.removeWikiEventListener( listener );
    }

    /**
     * Sets the created date.
     * @param date the creation date
     */
    public void setCreated( Date date )
    {
        m_created = date;
    }

    /**
     * Sets the creator of this Group.
     * @param creator the creator
     */
    public final void setCreator( String creator )
    {
        this.m_creator = creator;
    }

    /**
     * Sets the last-modified date
     * @param date the last-modified date
     */
    public void setLastModified( Date date )
    {
        m_modified = date;
    }

    /**
     * Sets the name of the user who last modified this group.
     * @param modifier the modifier
     */
    public final void setModifier( String modifier )
    {
        this.m_modifier = modifier;
    }

    /**
     * Provides a String representation.
     * @return the string
     */
    /**
     * Returns a string representation of the Group.
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();
        sb.append( "(Group " + getName() + ")" );
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
     * @see com.ecyrd.jspwiki.event.EventSourceDelegate#fireEvent(com.ecyrd.jspwiki.event.WikiEvent)
     */
    protected final void fireEvent( WikiEvent event )
    {
        m_listeners.fireEvent( event );
    }
}