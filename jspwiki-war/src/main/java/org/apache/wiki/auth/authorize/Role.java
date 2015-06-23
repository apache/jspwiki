/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.auth.authorize;

import java.io.Serializable;
import java.security.Principal;

/**
 * A lightweight, immutable Principal that represents a built-in wiki role such
 * as Anonymous, Asserted and Authenticated. It can also represent dynamic roles
 * used by an external {@link org.apache.wiki.auth.Authorizer}, such as a web
 * container.
 * @since 2.3
 */
public final class Role implements Principal, Serializable
{
    private static final long serialVersionUID = 1L;

    /** All users, regardless of authentication status */
    public static final Role ALL           = new Role( "All" );

    /** If the user hasn't supplied a name */
    public static final Role ANONYMOUS     = new Role( "Anonymous" );

    /** If the user has supplied a cookie with a username */
    public static final Role ASSERTED      = new Role( "Asserted" );

    /** If the user has authenticated with the Container or UserDatabase */
    public static final Role AUTHENTICATED = new Role( "Authenticated" );

    private final String   m_name;

    /**
     *  Create an empty Role.
     */
    protected Role()
    {
        this(null);
    }
    
    /**
     * Constructs a new Role with a given name.
     * @param name the name of the Role
     */
    public Role( String name )
    {
        m_name = name;
    }

    /**
     * Returns <code>true</code> if a supplied Role is a built-in Role:
     * {@link #ALL}, {@link #ANONYMOUS}, {@link #ASSERTED},
     * or {@link #AUTHENTICATED}.
     * @param role the role to check
     * @return the result of the check
     */
    public static boolean isBuiltInRole(Role role)
    {
        return  role.equals( ALL ) || role.equals( ANONYMOUS ) ||
                role.equals( ASSERTED ) || role.equals( AUTHENTICATED );

    }

    /**
     * Returns <code>true</code> if the supplied name is identical to the name
     * of a built-in Role; that is, the value returned by <code>getName()</code>
     * for built-in Roles {@link #ALL}, {@link #ANONYMOUS},
     * {@link #ASSERTED}, or {@link #AUTHENTICATED}.
     * @param name the name to be tested
     * @return <code>true</code> if the name is reserved; <code>false</code>
     *         if not
     */
    public static boolean isReservedName(String name)
    {
        return  name.equals( ALL.m_name ) ||
                name.equals( ANONYMOUS.m_name ) || name.equals( ASSERTED.m_name ) ||
                name.equals( AUTHENTICATED.m_name );
    }

    /**
     * Returns a unique hashcode for the Role.
     * @return the hashcode
     */
    public int hashCode()
    {
        return m_name.hashCode();
    }

    /**
     * Two Role objects are considered equal if their names are identical.
     * @param obj the object to test
     * @return <code>true</code> if both objects are of type Role and have identical names
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj == null || !( obj instanceof Role ) ) return false;
        return m_name.equals( ( (Role) obj ).getName() );
    }

    /**
     *  Returns the name of the Principal.
     * @return the name of the Role
     */
    public String getName()
    {
        return m_name;
    }

    /**
     * Returns a String representation of the role
     * @return the string representation of the role
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "[" + this.getClass().getName() + ": " + m_name + "]";
    }

}
