package com.ecyrd.jspwiki.auth;

import com.ecyrd.jspwiki.InternalWikiException;
import java.security.Principal;

/**
 *  A special kind of WikiGroup.  Anyone who has set their name in
 *  the cookie is a part of this group.
 */
public class NamedGroup
    extends AllGroup
{
    public NamedGroup()
    {
        setName( UserManager.GROUP_NAMEDGUEST );
    }

    public boolean isMember( Principal user )
    {
        if( user instanceof UserProfile )
        {
            UserProfile p = (UserProfile) user;

            return p.getLoginStatus() >= UserProfile.COOKIE;
        }

        throw new InternalWikiException("Someone offered us a Principal that is not an UserProfile!");
    }

    public boolean equals( Object o )
    {
        return o != null && o instanceof NamedGroup;
    }
}
