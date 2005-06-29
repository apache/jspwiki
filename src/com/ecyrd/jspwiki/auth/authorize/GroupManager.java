package com.ecyrd.jspwiki.auth.authorize;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.Authorizer;

/**
 * Specifies how to add, remove, and persist groups to an external group
 * management entity.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public interface GroupManager extends Authorizer
{

    public void initialize( WikiEngine engine, Properties props );

    /**
     * Adds a Group to the group cache. Note that this method fail, and will
     * throw an <code>IllegalArgumentException</code>, if the proposed group
     * is the same name as one of the built-in Roles: e.g., Admin,
     * Authenticated, etc.
     * @param group the Group to add
     */
    public void add( Group group );

    /**
     * Removes a Group from the group cache.
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
    public void commit();

    /**
     * Reloads the group cache from persistent storage.
     */
    public void reload();

}