package com.ecyrd.jspwiki.auth.permissions;

/**
 *  Represents a permission to delete a page or versions of it.  Also
 *  implies a permission to edit or comment a page.
 */
public class DeletePermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof DeletePermission);
    }    

    public boolean implies( WikiPermission p )
    {
        return (p instanceof EditPermission || p instanceof DeletePermission || p instanceof CommentPermission);
    }

    public String toString()
    {
        return "DeletePermission";
    }
}
