package com.ecyrd.jspwiki.auth;

import com.ecyrd.jspwiki.InternalWikiException;
import java.security.Principal;

/**
 *  A special kind of WikiGroup.  Anyone who has logged in
 *  and has been authenticated is a part of this group.
 */
public class KnownGroup
    extends AllGroup
{
    public boolean isMember( Principal user )
    {
        if( user instanceof UserProfile )
        {
            UserProfile p = (UserProfile) user;

            return ( p.getLoginStatus() == UserProfile.PASSWORD );
        }

        throw new InternalWikiException("Someone offered us a Principal that is not an UserProfile!");
    }

    public boolean equals( Object o )
    {
        return o != null && o instanceof KnownGroup;
    }
}
