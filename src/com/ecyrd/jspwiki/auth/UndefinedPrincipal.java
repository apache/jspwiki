package com.ecyrd.jspwiki.auth;

import java.security.acl.Group;
import java.security.Principal;

import java.util.Vector;
import java.util.Enumeration;
import java.util.Iterator;

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
