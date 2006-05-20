package com.ecyrd.jspwiki.auth.permissions;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A collection of AllPermission objects.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.2 $ $Date: 2006-05-20 05:20:34 $
 */
public class AllPermissionCollection extends PermissionCollection
{

    private static final long serialVersionUID = 1L;

    private boolean           m_not_empty      = false;

    private boolean           m_read_only      = false;

    protected final Hashtable m_permissions    = new Hashtable();

    /**
     * Adds an AllPermission object to this AllPermissionCollection. If this
     * collection was previously marked read-only, or if the permission supplied
     * is not of type {@link AllPermission}, a {@link SecurityException} is
     * thrown.
     * @see java.security.PermissionCollection#add(java.security.Permission)
     */
    public void add( Permission permission )
    {
        if ( !( permission instanceof AllPermission ) && !( permission instanceof WikiPermission )
                && !( permission instanceof PagePermission ) )
        {
            throw new IllegalArgumentException(
                    "Permission must be of type com.ecyrd.jspwiki.permissions.AllPermission, com.ecyrd.jspwiki.permissions.PagePermission or com.ecyrd.jspwiki.permissions.WikiPermission." );
        }
        
        if ( m_read_only )
        {
            throw new SecurityException( "attempt to add a Permission to a readonly PermissionCollection" );
        }
        
        m_not_empty = true;
        
        // This is a filthy hack, but it keeps us from having to write our own
        // Enumeration implementation
        m_permissions.put( permission, permission );
    }

    /**
     * Returns an enumeration of all AllPermission objects stored in this
     * collection.
     * @see java.security.PermissionCollection#elements()
     */
    public Enumeration elements()
    {
        return m_permissions.elements();
    }

    /**
     * Iterates through the AllPermission objects stored by this
     * AllPermissionCollection and determines if any of them imply a supplied
     * Permission. If the Permission is not of type {@link AllPermission},
     * {@link PagePermission} or {@link WikiPermission}, this method will
     * return <code>false</code>. If none of the AllPermissions stored in
     * this collection imply the permission, the method returns
     * <code>false</code>; conversely, if one of the AllPermission objects
     * implies the permission, the method returns <code>true</code>.
     * @param permission the Permission to test. It may be any Permission type,
     * but only the AllPermission, PagePermission or WikiPermission types are
     * actually evaluated.
     * @see java.security.PermissionCollection#implies(java.security.Permission)
     */
    public boolean implies( Permission permission )
    {
        // If nothing in the collection yet, fail fast
        if ( !m_not_empty )
        {
            return false;
        }

        // If not one of our permission types, it's not implied
        if ( !( permission instanceof AllPermission ) && !( permission instanceof WikiPermission )
                && !( permission instanceof PagePermission ) )
        {
            return false;
        }

        // Step through each AllPermission
        Enumeration permEnum = m_permissions.elements();
        while( permEnum.hasMoreElements() )
        {
            Permission storedPermission = (Permission) permEnum.nextElement();
            if ( storedPermission.implies( permission ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @see java.security.PermissionCollection#isReadOnly()
     */
    public boolean isReadOnly()
    {
        return m_read_only;
    }

    /**
     * @see java.security.PermissionCollection#setReadOnly()
     */
    public void setReadOnly()
    {
        m_read_only = true;
    }
    
    /**
     * Factory method that returns an AllPermissionCollection for
     * a specified wiki. For each wiki, only a single collection
     * item will ever be created and returned. The AllPermissionCollection
     * class itself maintains an internal static cache of permission
     * collections, where newly-created collections are kept.
     * @return the unique permission collection for the wiki
     */
    protected static final AllPermissionCollection getInstance( String wiki )
    {
        return new AllPermissionCollection();
    }
}
