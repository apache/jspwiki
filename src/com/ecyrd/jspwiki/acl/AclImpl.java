package com.ecyrd.jspwiki.acl;

import java.security.acl.AclEntry;
import java.security.acl.Acl;
import java.security.acl.Permission;
import java.security.acl.Group;
import java.security.Principal;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

/**
 *  JSPWiki implementation of an Access Control List.
 *  <p>
 *  This implementation does not care about owners, and thus all
 *  actions are allowed by default. 
 */
public class AclImpl
    implements AccessControlList
{
    private Vector m_entries = new Vector();
    private String m_name    = null;

    public boolean addOwner( Principal caller, Principal owner )
    {
        return false;
    }

    public boolean deleteOwner(Principal caller,
                               Principal owner)
    {
        return false;
    }

    public boolean isOwner(Principal owner)
    {
        return true;
    }

    public void setName( Principal caller, String name )
    {
        m_name = name;
    }

    public String getName()
    {
        return m_name;
    }

    private boolean hasEntry( AclEntry entry )
    {
        if( entry == null )
        {
            return false;
        }

        for( Iterator i = m_entries.iterator(); i.hasNext(); )
        {
            AclEntry e = (AclEntry) i.next();

            Principal ep     = e.getPrincipal();
            Principal entryp = entry.getPrincipal();

            if( ep == null || entryp == null )
            {
                throw new IllegalArgumentException("Entry is null; check code, please (entry="+entry+"; e="+e+")");
            }
            
            if( ep.getName().equals( entryp.getName() ) &&
                e.isNegative() == entry.isNegative() )
            {
                return true;
            }
        }

        return false;
    }

    public boolean addEntry(Principal caller,
                            AclEntry entry)
    {
        if( entry.getPrincipal() == null )
        {
            throw new IllegalArgumentException("Entry principal cannot be null");
        }

        if( hasEntry( entry ) )
        {
            return false;
        }
        
        m_entries.add( entry );

        return true;
    }

    public boolean removeEntry(Principal caller,
                               AclEntry entry)
    {
        return m_entries.remove( entry );
    }

    // FIXME: Does not understand anything about groups yet.
    public Enumeration getPermissions(Principal user)
    {
        Vector perms = new Vector();

        for( Iterator i = m_entries.iterator(); i.hasNext(); )
        {
            AclEntry ae = (AclEntry)i.next();

            if( ae.getPrincipal().getName().equals( user.getName() ) )
            {
                //
                //  Principal direct match.
                //

                for( Enumeration enum = ae.permissions(); enum.hasMoreElements(); )
                {
                    perms.add( enum.nextElement() );
                }
            }
        }

        return perms.elements();
    }

    public Enumeration entries()
    {
        return m_entries.elements();
    }

    public boolean checkPermission(Principal principal,
                                   Permission permission)
    {
        int res = findPermission( principal, permission );

        return (res == ALLOW);
    }

    public AclEntry getEntry( Principal principal, boolean isNegative )
    {
        for( Enumeration e = m_entries.elements(); e.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) e.nextElement();
        
            if( entry.getPrincipal().getName().equals(principal.getName()) &&
                entry.isNegative() == isNegative )
            {
                return entry;
            }
        }

        return null;
    }

    /**
     *  A new kind of an interface, where the possible results are
     *  either ALLOW, DENY, or NONE.
     */
   
    public int findPermission(Principal principal,
                              Permission permission)
    {
        boolean posEntry = false;

        /*
        System.out.println("****");
        System.out.println( toString() );
        System.out.println("Checking user="+principal);
        System.out.println("Checking permission="+permission);
        */
        for( Enumeration e = m_entries.elements(); e.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) e.nextElement();

            if( entry.getPrincipal().getName().equals(principal.getName()) )
            {
                if( entry.checkPermission( permission ) )
                {
                    // System.out.println("  Found person/permission match");
                    if( entry.isNegative() )
                    {
                        return DENY;
                    }
                    else
                    {
                        return ALLOW;
                        // posEntry = true;
                    }
                }
            }
        }

        //
        //  In case both positive and negative permissions have been set, 
        //  we'll err for the negative by quitting immediately if we see
        //  a match.  For positive, we have to wait until here.
        //

        if( posEntry ) return ALLOW;
        
        // System.out.println("-> groups");

        //
        //  Now, if the individual permissions did not match, we'll go through
        //  it again but this time looking at groups.
        //

        for( Enumeration e = m_entries.elements(); e.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) e.nextElement();

            // System.out.println("  Checking entry="+entry);

            if( entry.getPrincipal() instanceof Group )
            {
                Group entryGroup = (Group) entry.getPrincipal();

                // System.out.println("  Checking group="+entryGroup);

                if( entryGroup.isMember( principal ) && entry.checkPermission( permission ) )
                {
                    // System.out.println("    ismember&haspermission");
                    if( entry.isNegative() )
                    {
                        // System.out.println("    DENY");
                        return DENY;
                    }
                    else
                    {
                        // System.out.println("    ALLOW");
                        return ALLOW;
                        //                        posEntry = true;
                    }
                }
            }
        }

        if( posEntry ) return ALLOW;

        return NONE;
    }

    /**
     *  Returns a string representation of the contents of this Acl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for( Enumeration enum = entries(); enum.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) enum.nextElement();

            Principal pal = entry.getPrincipal();

            if( pal != null )
                sb.append("  user = "+pal.getName()+": ");
            else
                sb.append("  user = null: ");

            if( entry.isNegative() ) sb.append("NEG");

            sb.append("(");
            for( Enumeration perms = entry.permissions(); perms.hasMoreElements(); )
            {
                Permission perm = (Permission) perms.nextElement();
                sb.append( perm.toString() );
            }
            sb.append(")\n");
        }

        return sb.toString();
    }

}
    
