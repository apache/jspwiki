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
package org.apache.wiki.auth.login;

import java.security.Principal;

/**
 * Wrapper class for container-managed or externally-provided principals.
 * Instances of PrincipalWrapper are immutable.
 * @since 2.3
 */
public final class PrincipalWrapper implements Principal
{
    private final Principal m_principal;
    
    /**
     * Constructs a new instance of this class by wrapping (decorating)
     * the supplied principal.
     * @param principal The principal to wrap
     */
    public PrincipalWrapper( Principal principal )
    {
        m_principal = principal;
    }

    /**
     * Returns the wrapped Principal used to construct this instance.
     * @return the wrapped Principal decorated by this instance.
     */
    public Principal getPrincipal()
    {
        return m_principal;
    }
    
    /**
     * Returns the name of the wrapped principal.
     * 
     * @return The name of the wrapped principal.
     */
    public String getName()
    {
        return m_principal.getName();
    }

    /**
     *  Two PrincipalWrapper objects are equal if their internally-wrapped
     *  principals are also equal.
     *  
     *  @param obj {@inheritDoc}
     *  @return True, if the wrapped object is also equal to our wrapped object.
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( ! ( obj instanceof PrincipalWrapper ) )
        {
            return false;
        }
        return m_principal.equals( ( (PrincipalWrapper)obj ).getPrincipal() );
    }

    /**
     *  The hashcode is based on the hashcode of the wrapped principal.
     *  
     *  @return A hashcode.
     */
    @Override
    public int hashCode()
    {
        return m_principal.hashCode() * 13;
    }

}
