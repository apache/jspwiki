/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.acl;

import java.security.Permission;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 *  JSPWiki implementation of an Access Control List.
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 *  @since 2.3
 */
public class AclImpl
    implements Acl
{
    private Vector m_entries = new Vector();

    /**
     * @see com.ecyrd.jspwiki.auth.acl.Acl#findPrincipals(java.security.Permission)
     */
    public Principal[] findPrincipals(Permission permission)
    {
      Vector principals = new Vector();
      Enumeration entries = entries();
      while (entries.hasMoreElements()) {
        AclEntry entry = (AclEntry)entries.nextElement();
        Enumeration permissions = entry.permissions();
        while (permissions.hasMoreElements()) {
          Permission perm = (Permission)permissions.nextElement();
          if (perm.implies(permission)) {
            principals.add(entry.getPrincipal());
          }
        }
      }
      return (Principal[])principals.toArray(new Principal[principals.size()]);
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
            
            if( ep.getName().equals( entryp.getName() ))
            {
                return true;
            }
        }

        return false;
    }

    public boolean addEntry( AclEntry entry )
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

    public boolean removeEntry( AclEntry entry )
    {
        return m_entries.remove( entry );
    }

    public Enumeration entries()
    {
        return m_entries.elements();
    }

    public AclEntry getEntry( Principal principal )
    {
        for( Enumeration e = m_entries.elements(); e.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) e.nextElement();
        
            if( entry.getPrincipal().getName().equals(principal.getName()) )
            {
                return entry;
            }
        }

        return null;
    }

    /**
     *  Returns a string representation of the contents of this Acl.
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for( Enumeration myEnum = entries(); myEnum.hasMoreElements(); )
        {
            AclEntry entry = (AclEntry) myEnum.nextElement();

            Principal pal = entry.getPrincipal();

            if( pal != null )
                sb.append("  user = "+pal.getName()+": ");
            else
                sb.append("  user = null: ");

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
    
