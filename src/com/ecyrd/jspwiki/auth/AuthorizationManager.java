package com.ecyrd.jspwiki.auth;

import java.util.Properties;
import java.util.List;
import java.util.Iterator;
import java.security.Permissions;
import java.security.Permission;
import java.security.acl.NotOwnerException;
import java.security.Principal;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.acl.AclEntryImpl;
import com.ecyrd.jspwiki.acl.AclImpl;
import com.ecyrd.jspwiki.auth.permissions.*;

/**
 *  Manages all access control and authorization.
 *
 *  @see UserManager
 */
public class AuthorizationManager
{
    public static final String PROP_STRICTLOGINS = "jspwiki.policy.strictLogins";

    static Category log = Category.getInstance( AuthorizationManager.class );

    private WikiAuthorizer    m_authorizer;
    private AccessControlList m_defaultPermissions;

    private boolean           m_strictLogins = false;

    private WikiEngine        m_engine;

    public AuthorizationManager( WikiEngine engine, Properties properties )
    {
        m_engine = engine;

        m_authorizer = new PageAuthorizer();  // FIXME: Should be settable
        m_authorizer.initialize( engine, properties );

        m_strictLogins = TextUtil.getBooleanProperty( properties,
                                                      PROP_STRICTLOGINS,
                                                      false );

        AclEntryImpl ae = new AclEntryImpl();

        WikiGroup allGroup = new AllGroup();
        allGroup.setName("All");
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
     *  Returns true, if strict logins are required.  Strict logins
     *  mean that all pages are accessible only to users who have logged in.
     */
    public boolean strictLogins()
    {
        return m_strictLogins;
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

        if( acl == null )
        {
            log.debug("No ACL, querying from authorizer");
            acl = m_authorizer.getPermissions( page );
        }

        //
        //  Does the page in question have an access control list?
        //
        if( acl != null )
        {
            log.debug("ACL for this page is: "+acl);
            log.debug("Checking for wup: "+wup);
            log.debug("Permission: "+permission);
            res = acl.findPermission( wup, permission );

            //
            //  If there as no entry for the user, then try all of his groups
            //

            if( res == AccessControlList.NONE )
            {
                log.debug("Checking groups...");

                List list = m_engine.getUserManager().getGroupsForPrincipal( wup );

                for( Iterator i = list.iterator(); i.hasNext(); )
                {
                    res = acl.findPermission( (Principal) i.next(), permission );

                    if( res != AccessControlList.NONE )
                        break;
                }
            }
        }

        //
        //  If there was no result, then query from the default
        //  permission set of the authorizer.
        //

        if( res == AccessControlList.NONE )
        {
            log.debug("Page defines no permissions for "+wup.getName()+", checking defaults.");

            acl = m_authorizer.getDefaultPermissions();

            if( acl != null )
            {
                res = acl.findPermission( wup, permission );
            }
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
