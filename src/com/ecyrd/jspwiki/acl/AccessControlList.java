package com.ecyrd.jspwiki.acl;

import java.security.acl.Acl;
import java.security.acl.AclEntry;
import java.security.acl.Permission;
import java.security.Principal;

public interface AccessControlList
    extends Acl
{
    public static final int ALLOW = 0;
    public static final int DENY  = 1;
    public static final int NONE  = 2;

    public int findPermission(Principal principal,
                              Permission permission);

    public AclEntry getEntry( Principal principal, boolean isNegative );
}
