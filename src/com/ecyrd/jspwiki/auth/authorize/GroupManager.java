package com.ecyrd.jspwiki.auth.authorize;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.auth.Authorizer;
import com.ecyrd.jspwiki.event.WikiEventListener;

/**
 * Specifies how to add, remove, and persist groups to an external group
 * management entity.
 * @author Andrew Jaquith
 * @version $Revision: 1.6 $
 * @since 2.3
 */
public interface GroupManager extends Authorizer
{
    /** Property name for the manager class in jspwiki.properties. */
    public static final String  PROP_GROUPMANAGER = "jspwiki.groupmanager";
    
    public void initialize( WikiEngine engine, Properties props );

    /**
     * Adds a Group to the group cache. Note that this method fail, and will
     * throw an <code>IllegalArgumentException</code>, if the proposed group
     * is the same name as one of the built-in Roles: e.g., Admin,
     * Authenticated, etc. When a Group is added successfully, the GroupManager
     * implementation should send a WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_ADD}  to all of its registered
     * WikiEventListeners. It should also register itself as a WikiEventListener for the Group
     * itself, so that additions to the group can be detected and
     * forwarded on to the GroupManager's own listeners.
     * @param group the Group to add
     */
    public void add( Group group );

    /** 
     * Registers a WikiEventListener with this GroupManager.
     * @param listener the event listener
     */
    public void addWikiEventListener( WikiEventListener listener );
    
    /**
     * Removes a Group from the group cache. When a Group is removed
     * successfully, the GroupManager implementation should send a
     * WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_REMOVE} to all of
     * its registered WikiEventListeners.
     * @param group the group to remove
     */
    public void remove( Group group );

    /**
     * Returns <code>true</code> if a Group is known to the GroupManager
     * (contained in the group cache), <code>false</code> otherwise.
     */
    public boolean exists( Group group );

    /**
     * Commits the groups to persistent storage.
     */
    public void commit() throws WikiException;

    /**
     * Reloads the group cache from persistent storage. If this requires all
     * groups in the cache to be flushed, the implementing class should send a
     * WikiSecurityEvent of type
     * {@link com.ecyrd.jspwiki.auth.WikiSecurityEvent#GROUP_CLEAR_GROUPS} to
     * all of its registered WikiEventListeners.
     */
    public void reload();
    
    /**
     * Un-registers a WikiEventListener with this GroupManager.
     * @param listener the event listener
     */
    public void removeWikiEventListener( WikiEventListener listener );

}
