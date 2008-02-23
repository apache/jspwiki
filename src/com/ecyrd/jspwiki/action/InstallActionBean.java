package com.ecyrd.jspwiki.action;

import java.security.Permission;

import net.sourceforge.stripes.action.UrlBinding;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.ui.Installer;

@WikiRequestContext("install")
@UrlBinding("/Install.action")
public class InstallActionBean extends AbstractActionBean
{
    public Permission requiredPermission()
    {
        // See if admin users exists
        Permission perm = null;
        WikiEngine engine = getContext().getWikiEngine();
        try
        {
            UserManager userMgr = engine.getUserManager();
            UserDatabase userDb = userMgr.getUserDatabase();
            userDb.findByLoginName( Installer.ADMIN_ID );
            perm = new AllPermission( engine.getApplicationName() );
        }
        catch ( NoSuchPrincipalException e )
        {
            // No admin user; thus, no permission required
        }
            
        return perm;
    }
}
