package com.ecyrd.jspwiki.auth.permissions;

public class EditPermission
    extends WikiPermission
{
    public boolean equals( Object p )
    {
        return (p != null) && (p instanceof EditPermission);
    }    

    public String toString()
    {
        return "EditPermission";
    }
}
