package com.ecyrd.jspwiki.auth.permissions;

public class CommentPermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof CommentPermission);
    }    

    public boolean implies( WikiPermission p )
    {
        return (p instanceof CommentPermission);
    }

    public String toString()
    {
        return "CommentPermission";
    }
}
