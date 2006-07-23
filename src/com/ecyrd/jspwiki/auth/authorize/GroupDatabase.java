package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Properties;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * Defines an interface for loading, persisting and storing wiki groups.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $
 * @since 2.4.22
 */
public interface GroupDatabase
{
    /**
     * Persists the current state of the group database to back-end storage.
     * This method is intended to be atomic; results cannot be partially
     * committed. If the commit fails, it should roll back its state
     * appropriately. Implementing classes that persist to the file system may
     * wish to make this method <code>synchronized</code>.
     * @throws WikiSecurityException
     */
    public void commit() throws WikiSecurityException;

    /**
     * Looks up and deletes a {@link Group} from the group database. If the
     * group database does not contain the supplied Group. this method throws a
     * {@link NoSuchPrincipalException}. The method does not commit the results
     * of the delete; it only alters the database in memory.
     * @param group the group to remove
     */
    public void delete( Group group ) throws NoSuchPrincipalException, WikiSecurityException;

    /**
     * Initializes the group database based on values from a Properties object.
     * @param engine the wiki engine
     * @param props the properties used to initialize the group database
     */
    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException,
            WikiSecurityException;

    /**
     * Saves a Group to the group database. Note that this method <em>must</em>
     * fail, and throw an <code>IllegalArgumentException</code>, if the
     * proposed group is the same name as one of the built-in Roles: e.g.,
     * Admin, Authenticated, etc. The database is responsible for setting
     * create/modify timestamps, upon a successful save, to the Group.
     * @param group the Group to save
     * @param saver the user who saved the Group
     */
    public void save( Group group, Principal saver ) throws WikiSecurityException;

    /**
     * Returns all wiki groups that are stored in the GroupDatabase as an array
     * of Group objects. If the database does not contain any groups, this
     * method will return a zero-length array. This method causes back-end
     * storage to load the entire set of group; thus, it should be called
     * infrequently (e.g., at initialization time). Note that this method should
     * use the protected constructor {@link Group#Group(String, String)} rather
     * than the various "parse" methods ({@link GroupManager#parseGroup(String, String, boolean)})
     * to construct the group. This is so as not to flood GroupManager's event
     * queue with spurious events.
     * @return the wiki groups
     */
    public Group[] groups() throws WikiSecurityException;
}
