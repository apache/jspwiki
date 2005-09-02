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
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;

/**
 *  A lightweight, immutable Principal class.
 *
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
 *  @version $Revision: 1.7 $ $Date: 2005-09-02 23:54:42 $
 *  @since  2.2
 */
public final class WikiPrincipal implements Principal
{

    /**
     * Represents an anonymous user. WikiPrincipals may be
     * created with an optional type designator: 
     * LOGIN_NAME, WIKI_NAME, FULL_NAME or UNSPECIFIED.
     */
    public static final Principal GUEST = new WikiPrincipal( "Guest" );
    public static final String FULL_NAME  = "fullName";
    public static final String LOGIN_NAME = "loginName";
    public static final String WIKI_NAME  = "wikiName";
    public static final String UNSPECIFIED  = "unspecified";
    public static final Comparator COMPARATOR = new PrincipalComparator();
    
    private static final String[] validTypes;
    
    static
    {
        validTypes = new String[] { FULL_NAME, LOGIN_NAME, WIKI_NAME, UNSPECIFIED };
        Arrays.sort( validTypes );
    }

    private final String          m_name;
    private final String          m_type;

    /**
     * Constructs a new WikiPrincipal with a given name and a type of
     * {@link #UNSPECIFIED}.
     * @param name the name of the Principal
     */
    public WikiPrincipal( String name )
    {
        m_name = name;
        m_type = UNSPECIFIED;
    }
    
    /**
     * Constructs a new WikiPrincipal with a given name and optional type
     * designator.
     * @param name the name of the Principal
     * @param type the type for this principal, which may be {@link #LOGIN_NAME},
     *            {@link #FULL_NAME}, {@link #WIKI_NAME} or {@link #WIKI_NAME}.
     * @throws IllegalArgumentException if the type is not {@link #LOGIN_NAME},
     *             {@link #FULL_NAME}, {@link #WIKI_NAME} or {@link #WIKI_NAME}
     */
    public WikiPrincipal( String name, String type )
    {
        m_name = name;
        if ( Arrays.binarySearch( validTypes, type ) < 0 )
        {
            throw new IllegalArgumentException( "Principal type '" + type + "' is invalid.");
        }
        m_type = type;
    }

    /**
     *  Returns the wiki name of the Principal.
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Two <code>WikiPrincipal</code>s are considered equal if their
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
     * Returns the Principal "type": {@link #LOGIN_NAME}, {@link #FULL_NAME},
     * {@link #WIKI_NAME} or {@link #WIKI_NAME}
     */
    public String getType()
    {
        return m_type;
    }
    
    /**
     * Returns a human-readable representation of the object.
     * @see java.lang.Object#toString()
     */
    public final String toString()
    {
        return "[WikiPrincipal: " + getName() + "]";
    }
    
    /**
     * Tiny little class that compares objects of type Principal.
     * Used for sorting arrays or collections of Principals.
     * @since 2.3
     */
    public static class PrincipalComparator implements Comparator 
    {
        public int compare( Object o1, Object o2 )
        {
            Collator collator = Collator.getInstance();
            if ( o1 instanceof Principal && o2 instanceof Principal )
            {
                return collator.compare( ((Principal)o1).getName(), ((Principal)o2).getName() );
            }
            throw new ClassCastException( "Objects must be of type Principal.");
        }
          
    }
    

}