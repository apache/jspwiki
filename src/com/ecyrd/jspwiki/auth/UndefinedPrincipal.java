package com.ecyrd.jspwiki.auth;

/**
 *  If a proper group/user cannot be located, then we use this
 *  class.
 */
public class UndefinedPrincipal
    extends WikiPrincipal
{
    public UndefinedPrincipal( String name )
    {
        super( name );
    }

    public String toString()
    {
        return "[Undefined: "+getName()+"]";
    }

    public boolean equals( Object o )
    {
        return o != null && o instanceof WikiPrincipal && ((WikiPrincipal)o).getName().equals( getName());
    }
}
