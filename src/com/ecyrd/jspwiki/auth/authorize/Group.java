package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;

import com.ecyrd.jspwiki.WikiEventListener;

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
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.5 $ $Date: 2006-02-21 08:39:39 $
 * @since 2.3
 */
public interface Group extends Principal
{
    
    public static String[] RESTRICTED_GROUPNAMES = new String[]{"Admin", "Anonymous", "All", "Asserted", "Authenticated"};
    
    /**
     * Adds a Principal to the group. When a Principal is added successfully, 
     * the Group implementation should send a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD_MEMBER}
     * to all of its registered WikiEventListeners.
     * @param principal the principal to add
     * @return <code>true</code> if the operation was successful
     */
    public boolean add( Principal principal );

    /** 
     * Registers a WikiEventListener with this Group.
     * @param listener the event listener
     */
    public void addWikiEventListener( WikiEventListener listener );
    
    /**
     * Removes a Principal from the group. When a Principal is added successfully, 
     * the Group implementation should send a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE_MEMBER}
     * to all of its registered WikiEventListeners.
     * @param principal the principal to remove
     * @return <code>true</code> if the operation was successful
     */
    public boolean remove( Principal principal );

    /**
     * Un-registers a WikiEventListener from this Group.
     * @param listener the event listener
     */
    public void removeWikiEventListener( WikiEventListener listener );
    
    /**
     * Clears all Principals from the group list. When a 
     * Group's members are cleared successfully, 
     * the Group implementation should send a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_CLEAR_MEMBERS}
     * to all of its registered WikiEventListeners.
     */
    public void clear();

    /**
     * Returns <code>true</code> if a Principal is a member of the group.
     * Specifically, the Principal's <code>getName()</code>
     * method must return the same value as one of the Principals in the group
     * member list. The Principal's type does <em>not</em> need to match.
     * @param principal the principal about whom membeship status is sought
     * @return the result of the operation
     */
    public boolean isMember( Principal principal );

    /**
     * The name of the group. Typically, this is set in concrete classes'
     * constructors.
     * @return the name of the Group
     */
    public String getName();

    /**
     * Two DefaultGroups are considered equal if they both contains the
     * same Principals and have the same name (case-insentive comparison).
     */
    public boolean equals( Object o );

    /**
     * Provides a String representation.
     * @return the string
     */
    public String toString();
}