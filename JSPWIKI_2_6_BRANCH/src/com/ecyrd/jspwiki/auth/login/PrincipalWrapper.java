/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth.login;

import java.security.Principal;

/**
 * Wrapper class for container-managed or externally-provided principals.
 * Instances of PrincipalWrapper are immutable.
 * @author Andrew Jaquith
 * @since 2.3
 */
public final class PrincipalWrapper implements Principal
{
    private final Principal m_principal;
    
    /**
     * Constructs a new instance of this class by wrapping (decorating)
     * the supplied principal.
     * @param principal
     */
    public PrincipalWrapper( Principal principal )
    {
        m_principal = principal;
    }

    /**
     * Returns the wrapped Principal used to construct this instance.
     * @return the wrapped Principal decorated by this instance.
     */
    public final Principal getPrincipal()
    {
        return m_principal;
    }
    
    /**
     * Returns the name of the wrapped principal.
     */
    public final String getName()
    {
        return m_principal.getName();
    }

    /**
     * Two PrincipalWrapper objects are equal if their internally-wrapped
     * principals are also equal.
     */
    public boolean equals( Object obj )
    {
        if ( ! ( obj instanceof PrincipalWrapper ) )
        {
            return false;
        }
        return m_principal.equals( ( (PrincipalWrapper)obj ).getPrincipal() );
    }

    public int hashCode()
    {
        return m_principal.hashCode() * 13;
    }

}
