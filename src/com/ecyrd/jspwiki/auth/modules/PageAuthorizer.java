package com.ecyrd.jspwiki.auth.modules;

import java.util.Properties;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.security.acl.AclEntry;
import java.security.acl.NotOwnerException;
import java.security.Principal;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.acl.AccessControlList;
import com.ecyrd.jspwiki.acl.AclImpl;
import com.ecyrd.jspwiki.acl.AclEntryImpl;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 *  This is a simple authorizer that just simply takes the permissions
 *  from a page.
 *
 *  @author Janne Jalkanen
 */
public class PageAuthorizer
    implements WikiAuthorizer
{
    private WikiEngine m_engine;

    static Logger log = Logger.getLogger( PageAuthorizer.class );

    // FIXME: Should be settable.

    public static final String DEFAULT_PERMISSIONPAGE = "DefaultPermissions";
    public static final String VAR_PERMISSIONS        = "defaultpermissions";

    private AccessControlList m_defaultPermissions = null;

    public void initialize( WikiEngine engine,
                            Properties properties )
    {
        m_engine = engine;
    }

    private void buildDefaultPermissions()
    {
        m_defaultPermissions = new AclImpl();
        
        WikiPage defpage = m_engine.getPage( DEFAULT_PERMISSIONPAGE );

        if( defpage == null ) return;

        String defperms = (String)defpage.getAttribute( VAR_PERMISSIONS );

        if( defperms != null )
        {
            StringTokenizer tokety = new StringTokenizer( defperms, ";" );

            WikiPage p = new WikiPage("Dummy");

            while( tokety.hasMoreTokens() )
            {
                String rule = tokety.nextToken();

                try
                {
                    AccessControlList acl = parseAcl( p,
                                                      m_engine.getUserManager(),
                                                      rule );
                    p.setAcl( acl );
                }
                catch( WikiSecurityException wse )
                {
                    log.error("Error on the default permissions page '"+
                              DEFAULT_PERMISSIONPAGE+"':"+wse.getMessage());
                    // FIXME: SHould do something else as well?  This msg only goes to the logs, and is thus not visible to users...
                }

            }

            m_defaultPermissions = p.getAcl();
        }
    }

    /**
     *  A helper method for parsing textual AccessControlLists.  The line
     *  is in form "(ALLOW|DENY) <permission> <principal>,<principal>,<principal>
     *
     *  @param page The current wiki page.  If the page already has an ACL,
     *              it will be used as a basis for this ACL in order to
     *              avoid the creation of a new one.
     *  @param mgr  The UserManager, which is used to query things like the Principal.
     *  @param ruleLine The rule line, as described above.
     *

     *  @return A valid Access Control List.  May be empty.
     *
     *  @throws WikiSecurityException, if the ruleLine was faulty somehow.
     *
     *  @since 2.2
     */
    public static AccessControlList parseAcl( WikiPage page, 
                                              UserManager mgr, 
                                              String ruleLine )
        throws WikiSecurityException
    {        
        AccessControlList acl = page.getAcl();
        if( acl == null ) acl = new AclImpl();

        try
        {
            StringTokenizer fieldToks = new StringTokenizer( ruleLine );
            String policy  = fieldToks.nextToken();
            String chain   = fieldToks.nextToken();            

            while( fieldToks.hasMoreTokens() )
            {
                String roleOrPerm = fieldToks.nextToken( "," ).trim();
                boolean isNegative = true;

                Principal principal = mgr.getPrincipal( roleOrPerm );

                if( policy.equals("ALLOW") ) isNegative = false;

                AclEntry oldEntry = acl.getEntry( principal, isNegative );

                if( oldEntry != null )
                {
                    log.debug("Adding to old acl list: "+principal+", "+chain);
                    oldEntry.addPermission( WikiPermission.newInstance( chain ) );
                }
                else
                {
                    log.debug("Adding new acl entry for "+chain);
                    AclEntry entry = new AclEntryImpl();
            
                    entry.setPrincipal( principal );
                    if( isNegative ) entry.setNegativePermissions();
                    entry.addPermission( WikiPermission.newInstance( chain ) );
                    
                    acl.addEntry( principal, entry );
                }
            }

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            throw new WikiSecurityException("Invalid access rule: "+ruleLine);
        }
        catch( NotOwnerException noe )
        {
            throw new InternalWikiException("Someone has implemented access control on access control lists without telling me.");
        }
        catch( IllegalArgumentException iae )
        {
            throw new WikiSecurityException("Invalid permission type: "+ruleLine);
        }

        return acl;
    }

    public AccessControlList getPermissions( WikiPage page )
    {
        AccessControlList acl = page.getAcl();

        //
        //  If the ACL has not yet been parsed, we'll do it here.
        //
        if( acl == null )
        {
            WikiContext context = new WikiContext( m_engine, page );
            String html = m_engine.getHTML( context, page );

            acl = page.getAcl();
        }

        log.debug( "page="+page.getName()+"\n"+acl );

        return acl;
    }

    public AccessControlList getDefaultPermissions()
    {
        if( m_defaultPermissions == null )
        {
            buildDefaultPermissions();
        }

        return m_defaultPermissions;
    }

}
