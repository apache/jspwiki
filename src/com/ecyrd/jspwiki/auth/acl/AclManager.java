package com.ecyrd.jspwiki.auth.acl;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 *  Specifies how to parse and return ACLs from wiki pages.
 *  @author Andrew Jaquith
 *  @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 *  @since 2.3
 */
public interface AclManager
{

    public void initialize( WikiEngine engine, Properties props );

    /**
     * A helper method for parsing textual AccessControlLists. The line is in
     * form "(ALLOW) <permission><principal>, <principal>, <principal>". This
     * method was moved from Authorizer.
     * @param page The current wiki page. If the page already has an ACL, it
     *            will be used as a basis for this ACL in order to avoid the
     *            creation of a new one.
     * @param ruleLine The rule line, as described above.
     * @return A valid Access Control List. May be empty.
     * @throws WikiSecurityException, if the ruleLine was faulty somehow.
     * @since 2.1.121
     */
    public Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException;
    
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
    public Acl getPermissions( WikiPage page );
}