package com.ecyrd.jspwiki.auth;

import java.util.Properties;
import java.security.Permissions;
import java.security.Permission;
import java.security.acl.NotOwnerException;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.acl.AclEntryImpl;
import com.ecyrd.jspwiki.acl.AclImpl;
import com.ecyrd.jspwiki.auth.permissions.*;

public class AuthorizationManager
{
    static Category log = Category.getInstance( AuthorizationManager.class );

    private WikiAuthorizer    m_authorizer;
    private AccessControlList m_defaultPermissions;

    public AuthorizationManager( Properties properties )
    {
        AclEntryImpl ae = new AclEntryImpl();

        WikiGroup allGroup = new AllGroup();
        allGroup.setName("all");
        ae.setPrincipal( allGroup );
        ae.addPermission( new ViewPermission() );
        ae.addPermission( new EditPermission() );

        try
        {
            m_defaultPermissions = new AclImpl();
            m_defaultPermissions.addEntry( null, ae );
        }
        catch( NotOwnerException e )
        {
            throw new InternalWikiException("Nobody told me that owners were in use");
        }
    }

    /**
     *  Returns true or false, depending on whether this action
     *  is allowed.
     */
    public boolean checkPermission( WikiPage page, 
                                    UserProfile wup, 
                                    WikiPermission permission )
    {
        int res = AccessControlList.NONE;

        AccessControlList acl = page.getAcl();

        //
        //  Does the page in question have an access control list?
        //
        if( acl != null )
        {
            log.debug("ACL for this page is: "+acl);
            res = acl.findPermission( wup, permission );
        }

        //
        //  If there was no result, then query from the default
        //  permission set of the authorizer.
        //

        if( res == AccessControlList.NONE )
        {
            log.debug("Page defines no permissions for "+wup.getName()+", checking defaults.");
            /*
              FIXME!
            acl = m_authorizer.getDefaultPermissions();

            res = acl.findPermission( wup, permission );
            */
        }

        //
        //  If there still is nothing, then query from the Wiki default
        //  set of permissions.
        //

        if( res == AccessControlList.NONE )
        {
            log.debug("No defaults exist, falling back to hardcoded permissions.");
            res = m_defaultPermissions.findPermission( wup, permission );
        }

        log.debug("Permission "+permission+" for user "+wup+" is "+res );
        
        if( res == AccessControlList.NONE )
        {
            throw new InternalWikiException("No default policy has been defined!");
        }

        return (res == AccessControlList.ALLOW);
    }
}
