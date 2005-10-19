package com.ecyrd.jspwiki.auth.login;

import java.security.Principal;

/**
 * Wrapper class for container-managed or externally-provided principals.
 * Instances of PrincipalWrapper are immutable.
 * @author Andrew Jaquith
 * @version $Revision$ $Date$
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
