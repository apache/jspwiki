package com.ecyrd.jspwiki.auth.acl;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;

/**
 * Default implementation that parses Acls from wiki page markup.
 * @author Andrew Jaquith
 * @version $Revision: 1.6 $ $Date: 2005-12-12 06:31:36 $
 * @since 2.3
 */
public class DefaultAclManager implements AclManager
{
    static Logger                log    = Logger.getLogger( DefaultAclManager.class );

    private AuthorizationManager m_auth = null;
    private WikiEngine m_engine = null;
    
    /**
     * Initializes the AclManager with a supplied wiki engine and properties.
     * @param engine the wiki engine
     * @param props the initialization properties
     * @see com.ecyrd.jspwiki.auth.acl.AclManager#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_auth = engine.getAuthorizationManager();
        m_engine = engine;
    }

    /**
     * A helper method for parsing textual AccessControlLists. The line is in
     * form "ALLOW <permission> <principal>, <principal>, <principal>". This
     * method was moved from Authorizer.
     * @param page The current wiki page. If the page already has an ACL, it
     *            will be used as a basis for this ACL in order to avoid the
     *            creation of a new one.
     * @param ruleLine The rule line, as described above.
     * @return A valid Access Control List. May be empty.
     * @throws WikiSecurityException, if the ruleLine was faulty somehow.
     * @since 2.1.121
     */
    public Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException
    {
        Acl acl = page.getAcl();
        if ( acl == null )
            acl = new AclImpl();

        try
        {
            StringTokenizer fieldToks = new StringTokenizer( ruleLine );
            fieldToks.nextToken();
            String actions = fieldToks.nextToken();
            page.getName();

            while( fieldToks.hasMoreTokens() )
            {
                String principalName = fieldToks.nextToken( "," ).trim();
                Principal principal = m_auth.resolvePrincipal( principalName );
                AclEntry oldEntry = acl.getEntry( principal );

                if ( oldEntry != null )
                {
                    log.debug( "Adding to old acl list: " + principal + ", " + actions );
                    oldEntry.addPermission( new PagePermission( page, actions ) );
                }
                else
                {
                    log.debug( "Adding new acl entry for " + actions );
                    AclEntry entry = new AclEntryImpl();

                    entry.setPrincipal( principal );
                    entry.addPermission( new PagePermission( page, actions ) );

                    acl.addEntry( entry );
                }
            }

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            throw new WikiSecurityException( "Invalid access rule: " + ruleLine );
        }
        catch( IllegalArgumentException iae )
        {
            throw new WikiSecurityException( "Invalid permission type: " + ruleLine );
        }

        return acl;
    }


    /**
     * Returns the access control list for the page.
     * If the ACL has not been parsed yet, it is done
     * on-the-fly. If the page has a parent page, then that is tried also.
     * This method was moved from Authorizer;
     * it was consolidated with some code from AuthorizationManager.
     * @param page
     * @since 2.2.121
     * @return the Acl representing permissions for the page
     */
    public Acl getPermissions( WikiPage page )
    {
      //
      //  Does the page already have cached ACLs?
      //
      Acl acl = page.getAcl();
      log.debug( "page="+page.getName()+"\n"+acl );

      if( acl == null )
      {
          //
          //  If null, try the parent.
          //
          if( acl == null && page instanceof Attachment )
          {
              WikiPage parent = m_engine.getPage( ((Attachment)page).getParentName() );

              acl = getPermissions( parent );
          }
      }

      return acl;
    }

}