package com.ecyrd.jspwiki.auth.permissions;

public class ViewPermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof ViewPermission);
    }

    public String toString()
    {
        return "ViewPermission";
    }
}
