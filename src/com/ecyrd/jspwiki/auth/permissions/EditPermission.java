package com.ecyrd.jspwiki.auth.permissions;

/**
 *  Represents the permission to edit a page.  Also implies the
 *  permission to comment on a page (CommentPermission) and uploading
 *  of files.
 */
public class EditPermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof EditPermission);
    }    

    public boolean implies( WikiPermission p )
    {
        return (p instanceof CommentPermission) || (p instanceof EditPermission) ||
               (p instanceof UploadPermission);
    }

    public String toString()
    {
        return "EditPermission";
    }
}
