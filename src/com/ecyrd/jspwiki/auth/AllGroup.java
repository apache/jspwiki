package com.ecyrd.jspwiki.auth;

import java.security.Principal;

/**
 *  A special kind of WikiGroup.  Everyone is a member of this group.
 */
public class AllGroup
    extends WikiGroup
{
    public AllGroup()
    {
        setName( UserManager.GROUP_GUEST );
    }

    public boolean addMember( Principal user )
    {
        return true;
    }

    public boolean removeMember( Principal user )
    {
        return true;
    }

    public boolean isMember( Principal user )
    {
        return true;
    }

    public boolean equals( Object o )
    {
        return o != null && o instanceof AllGroup;
    }
}
