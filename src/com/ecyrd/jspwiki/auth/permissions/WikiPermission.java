package com.ecyrd.jspwiki.auth.permissions;

import java.security.acl.Permission;

public abstract class WikiPermission
    implements Permission
{
    private static Permission c_viewPermission = new ViewPermission();
    private static Permission c_editPermission = new EditPermission();

    public static Permission newInstance( String representation )
        throws IllegalArgumentException
    {
        if( representation.equalsIgnoreCase( "view" ) )
            return c_viewPermission;
        else if( representation.equalsIgnoreCase( "edit" ) )
            return c_editPermission;

        throw new IllegalArgumentException("No such permission: "+representation);
    }
}
