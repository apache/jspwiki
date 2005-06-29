package com.ecyrd.jspwiki.auth.acl;

import java.security.Permission;
import java.security.Principal;
import java.util.Enumeration;

/**
 * <p>
 * Represents one entry in an Access Control List (ACL).
 * </p>
 * <p>
 * An ACL can be thought of as a data structure with multiple ACL entry objects.
 * Each ACL entry object contains a set of positive page permissions associated
 * with a particular principal. (A principal represents an entity such as an
 * individual user, group, or role). Each principal can have at most one ACL
 * entry; that is, multiple ACL entries are not allowed for any principal.
 * </p>
 * <p>
 * This interface is functionally equivalent to the java.security.acl.AclEntry
 * interface, minus negative permissions.
 * </p>
 * @see Acl
 * @author Janne Jalkanen
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public interface AclEntry {

  /**
   * Adds the specified permission to this ACL entry. The permission
   * <em>must</em> be of type
   * {@link com.ecyrd.jspwiki.auth.permissions.PagePermission}. Note: An entry
   * can have multiple permissions.
   * @param permission the permission to be associated with the principal in
   *            this entry
   * @return true if the permission was added, false if the permission was
   *         already part of this entry's permission set, and false if
   *         the permission is not of type PagePermission
   */
  public boolean addPermission(Permission permission);

  /**
   * Checks if the specified permission is part of the permission set in this
   * entry.
   * @param permission the permission to be checked for.
   * @return true if the permission is part of the permission set in this entry,
   *         false otherwise.
   */
  public boolean checkPermission(Permission permission);

  /**
   * Returns a string representation of the contents of this ACL entry.
   * @return a string representation of the contents.
   */
  public Object clone();

  /**
   * Returns the principal for which permissions are granted or denied by this
   * ACL entry. Returns null if there is no principal set for this entry yet.
   * @return the principal associated with this entry.
   */
  public Principal getPrincipal();

  /**
   * Returns an enumeration of the permissions in this ACL entry.
   * @return an enumeration of the permissions in this ACL entry.
   */
  public Enumeration permissions();

  /**
   * Removes the specified permission from this ACL entry.
   * @param permission the permission to be removed from this entry.
   * @return true if the permission is removed, false if the permission was not
   *         part of this entry's permission set.
   */
  public boolean removePermission(Permission permission);
    /**
     * Specifies the principal for which permissions are granted or denied by
     * this ACL entry. If a principal was already set for this ACL entry, false
     * is returned, otherwise true is returned.
     * @param user the principal to be set for this entry
     * @return true if the principal is set, false if there was already a
     *         principal set for this entry
     */
  public boolean setPrincipal(Principal user);

  /**
   * Clones this ACL entry.
   * @return a clone of this ACL entry.
   */
  public String toString();
}