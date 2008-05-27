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
import java.util.Arrays;
import java.util.Comparator;

/**
 *  A lightweight, immutable Principal class. WikiPrincipals can be created with
 *  and optional "type" to denote what type of user profile Principal it represents
 *  (FULL_NAME, WIKI_NAME, LOGIN_NAME). Types are used to determine suitable
 *  user and login Principals in classes like WikiSession. However, the type
 *  property of a WikiPrincipal does not affect a WikiPrincipal's logical equality
 *  or hash code; two WikiPrincipals with the same name but different types are still
 *  considered equal.
 *
 *  @author Janne Jalkanen
 *  @author Andrew Jaquith
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

    /** WikiPrincipal type denoting a user's full name. */
    public static final String FULL_NAME  = "fullName";
    
    /** WikiPrincipal type denoting a user's login name. */
    public static final String LOGIN_NAME = "loginName";
    
    /** WikiPrincipal type denoting a user's wiki name. */
    public static final String WIKI_NAME  = "wikiName";
    
    /** Generic WikiPrincipal of unspecified type. */
    public static final String UNSPECIFIED  = "unspecified";
    
    /** Static instance of Comparator that allows Principals to be sorted. */
    public static final Comparator COMPARATOR = new PrincipalComparator();
    
    private static final String[] VALID_TYPES;
    
    static
    {
        VALID_TYPES = new String[] { FULL_NAME, LOGIN_NAME, WIKI_NAME, UNSPECIFIED };
        Arrays.sort( VALID_TYPES );
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
     * designator. If the supplied <code>type</code> parameter is not
     * {@link #LOGIN_NAME}, {@link #FULL_NAME}, {@link #WIKI_NAME}
     * or {@link #WIKI_NAME}, this method throws
     * an {@link IllegalArgumentException}.
     * @param name the name of the Principal
     * @param type the type for this principal, which may be {@link #LOGIN_NAME},
     *            {@link #FULL_NAME}, {@link #WIKI_NAME} or {@link #WIKI_NAME}.
     */
    public WikiPrincipal( String name, String type )
    {
        m_name = name;
        if ( Arrays.binarySearch( VALID_TYPES, type ) < 0 )
        {
            throw new IllegalArgumentException( "Principal type '" + type + "' is invalid.");
        }
        m_type = type;
    }

    /**
     *  Returns the wiki name of the Principal.
     *  @return the name
     */
    public final String getName()
    {
        return m_name;
    }

    /**
     * Two <code>WikiPrincipal</code>s are considered equal if their
     * names are equal (case-sensitive).
     * @param obj the object to compare
     * @return the result of the equality test
     */
    public final boolean equals( Object obj )
    {
        if ( obj == null || !( obj instanceof WikiPrincipal ) )
        {
            return false;
        }
        return m_name.equals( ( (WikiPrincipal) obj ).getName() );
    }

    /**
     *  The hashCode() returned for the WikiPrincipal is the same as
     *  for its name.
     *  @return the hash code
     */
    public final int hashCode()
    {
        return m_name.hashCode();
    }
    
    /**
     * Returns the Principal "type": {@link #LOGIN_NAME}, {@link #FULL_NAME},
     * {@link #WIKI_NAME} or {@link #WIKI_NAME}
     * @return the type
     */
    public final String getType()
    {
        return m_type;
    }
    
    /**
     * Returns a human-readable representation of the object.
     * @return the string representation
     */
    public final String toString()
    {
        return "[WikiPrincipal (" + m_type + "): " + getName() + "]";
    }
    

}
