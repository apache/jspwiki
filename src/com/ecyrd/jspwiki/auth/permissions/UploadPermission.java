package com.ecyrd.jspwiki.auth.permissions;

public class UploadPermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof UploadPermission);
    }    

    public boolean implies( WikiPermission p )
    {
        return (p instanceof UploadPermission);
    }

    public String toString()
    {
        return "UploadPermission";
    }
}
