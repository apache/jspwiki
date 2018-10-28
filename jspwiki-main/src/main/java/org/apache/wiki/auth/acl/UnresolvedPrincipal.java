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
package org.apache.wiki.auth.acl;

import java.io.Serializable;
import java.security.Principal;

/**
 * Represents a Principal, typically read from an ACL, that cannot
 * be resolved based on the current state of the user database, group
 * manager, and built-in role definitions.
 * Creating a principal marked "unresolved" allows
 * delayed resolution, which enables principals to be resolved
 * lazily during a later access control check. Conceptuallly,
 * UnresolvedPrincipal performs a function similar to
 * {@link java.security.UnresolvedPermission}.
 * 
 * @since 2.3
 */
public final class UnresolvedPrincipal implements Principal, Serializable
{
    private static final long serialVersionUID = 1L;
    private final String m_name;

    /**
     * Constructs a new UnresolvedPrincipal instance.
     * @param name the name of the Principal
     */
    public UnresolvedPrincipal( String name )
    {
        m_name = name;
    }

    /**
     * Returns the name of the principal.
     * @return the name
     * @see java.security.Principal#getName()
     */
    public String getName()
    {
        return m_name;
    }
    
    /**
     * Returns a String representation of the UnresolvedPrincipal.
     * @return the String
     */
    public String toString()
    {
        return "[UnresolvedPrincipal: " + m_name + "]";
    }

    /**
     * An unresolved principal is equal to another
     * unresolved principal if their names match.
     * @param obj the object to compare to this one
     * @return the result of the equality test
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object obj )
    {
        if ( obj instanceof UnresolvedPrincipal )
        {
            return m_name.equals( ( (UnresolvedPrincipal) obj ).m_name );
        }
        return false;
    }

    /**
     *  The hashCode of this object is equal to the hash code of its name.
     *  @return the hash code
     */
    public int hashCode()
    {
        return m_name.hashCode();
    }
}
