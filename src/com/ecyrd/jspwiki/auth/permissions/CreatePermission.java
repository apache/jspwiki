package com.ecyrd.jspwiki.auth.permissions;

/**
 *  Represents the permission to edit a page.  Also implies the
 *  permission to comment on a page (CommentPermission) and uploading
 *  of files.
 */
public class CreatePermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof CreatePermission);
    }    

    public boolean implies( WikiPermission p )
    {
        return (p instanceof CreatePermission);
    }

    public String toString()
    {
        return "CreatePermission";
    }
}
