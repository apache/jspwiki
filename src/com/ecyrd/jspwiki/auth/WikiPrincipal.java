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
package com.ecyrd.jspwiki.auth;

import java.security.Principal;

/**
 *  A lightweight, immutable Principal class.
 *
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @version $Revision: 1.3 $ $Date: 2005-06-29 22:43:17 $
 *  @since  2.2
 */
public final class WikiPrincipal implements Principal
{

    /**
     * Represents an anonymous user. WikiPrincipals may be
     * created with an optional flag indicating that the
     * Principal should be marked as representing a user's
     * common name (first and last name). By default,
     * this is <code>false</code>.
     * 
     */
    public static final Principal GUEST = new WikiPrincipal( "Guest" );

    private final String          m_name;
    private final boolean         m_isCommonName;

    /**
     * Constructs a new WikiPrincipal with a given name.
     * @param name the name of the Principal
     */
    public WikiPrincipal( String name )
    {
        this( name, false );
    }
    
    /**
     * Constructs a new WikiPrincipal with a given name
     * and optional flag indicating that this Principal 
     * represents a user's common name.
     * @param name the name of the Principal
     * @param isUserName whether this principal 
     */
    public WikiPrincipal( String name, boolean isUserName )
    {
        m_name = name;
        m_isCommonName = false;
    }

    /**
     *  Returns the WikiName of the Principal.
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Two WikiPrincipals are considered equal if their
     * names are equal (case-sensitive).
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public final boolean equals( Object obj )
    {
        if ( obj == null || !( obj instanceof WikiPrincipal ) )
        {
            return false;
        }
        return ( m_name.equals( ( (WikiPrincipal) obj ).getName() ) );
    }

    /**
     * Returns <code>true</code> if this Principal represents a user's
     * common name.
     * @return <code>true</code> if this Principal was created
     * with the "common name" flag set to <code>true</code>;
     * <code>false</code> otherwise.
     */
    public boolean isCommonName()
    {
        return m_isCommonName;
    }
    
    /**
     * Returns a human-readable representation of the object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "[WikiPrincipal: " + getName() + "]";
    }

}