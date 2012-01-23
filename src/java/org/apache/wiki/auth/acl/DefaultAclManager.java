/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.auth.acl;

import java.security.Permission;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.PrincipalComparator;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;



/**
 * Default implementation that parses Acls from wiki page markup.
 * @since 2.3
 */
public class DefaultAclManager implements AclManager
{
    static Logger                log    = LoggerFactory.getLogger( DefaultAclManager.class );

    private AuthorizationManager m_auth = null;
    private WikiEngine           m_engine = null;
    private static final String PERM_REGEX = "(" +
        PagePermission.COMMENT_ACTION + "|" +
        PagePermission.DELETE_ACTION  + "|" +
        PagePermission.EDIT_ACTION    + "|" +
        PagePermission.MODIFY_ACTION  + "|" +
        PagePermission.RENAME_ACTION  + "|" +
        PagePermission.UPLOAD_ACTION  + "|" +
        PagePermission.VIEW_ACTION    + ")";
    private static final String ACL_REGEX = "\\[\\{\\s*ALLOW\\s+" + PERM_REGEX + "\\s*(.*?)\\s*\\}\\]";

    /**
     * Identifies ACL strings in wiki text; the first group is the action (view, edit) and
     * the second is the list of Principals separated by commas. The overall match is
     * the ACL string from [{ to }].
     * */
    public static final Pattern ACL_PATTERN = Pattern.compile( ACL_REGEX );

    /**
     * Initializes the AclManager with a supplied wiki engine and properties.
     * @param engine the wiki engine
     * @param props the initialization properties
     * @see org.apache.wiki.auth.acl.AclManager#initialize(org.apache.wiki.WikiEngine,
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
     * @throws WikiSecurityException if the ruleLine was faulty somehow.
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
                    oldEntry.addPermission( PermissionFactory.getPagePermission( page, actions ) );
                }
                else
                {
                    log.debug( "Adding new acl entry for " + actions );
                    AclEntry entry = new AclEntryImpl();

                    entry.setPrincipal( principal );
                    entry.addPermission( PermissionFactory.getPagePermission( page, actions ) );

                    acl.addEntry( entry );
                }
            }

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( NoSuchElementException nsee )
        {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            throw new WikiSecurityException( "Invalid access rule: " + ruleLine, nsee );
        }
        catch( IllegalArgumentException iae )
        {
            throw new WikiSecurityException( "Invalid permission type: " + ruleLine, iae );
        }

        return acl;
    }


    /**
     * Returns the access control list for the page.
     * If the ACL has not been parsed yet, it is done
     * on-the-fly. If the page has a parent page, then that is tried also.
     * This method was moved from Authorizer;
     * it was consolidated with some code from AuthorizationManager.
     * This method is guaranteed to return a non-<code>null</code> Acl.
     * @param page the page
     * @since 2.2.121
     * @return the Acl representing permissions for the page
     * @throws WikiSecurityException If something goes wrong.
     */
    public Acl getPermissions( WikiPage page ) throws WikiSecurityException
    {
        //
        //  Does the page already have cached ACLs?
        //
        Acl acl = page.getAcl();
        if( log.isDebugEnabled() ) log.debug( "page="+page.getName()+"\n"+acl );

        if( acl == null )
        {
            try
            {
                WikiPage parent = page.getParent();
                acl = getPermissions( parent );
            }
            catch( PageNotFoundException e )
            {
                // There is no parent
                return new AclImpl();
            }
            catch( ProviderException e )
            {
                throw new WikiSecurityException( "Unable to get parent page to check for permissions.", e );
            }
        }
        return acl;
    }

    /**
     * Sets the access control list for the page. The Acl is stored by calling
     * {@link WikiPage#setAcl(Acl)}. When this method is called, all pre-3.0
     * ACL markup in the page is removed. Note that the ACL is not actually
     * persisted to the back-end repository until {@link WikiPage#save()} is
     * called. Any ProviderExceptions will be re-thrown as
     * WikiSecurityExceptions.
     * 
     * @param page the wiki page
     * @param acl the access control list
     * @since 2.5
     * @throws WikiSecurityException of the Acl cannot be set
     */
    public void setPermissions( WikiPage page, Acl acl ) throws WikiSecurityException
    {
        // Remove all of the existing ACLs.
        try
        {
            String pageText = page.getContentAsString();
            Matcher matcher = DefaultAclManager.ACL_PATTERN.matcher( pageText );
            String cleansedText = matcher.replaceAll( "" );
            page.setAcl( acl );
            if ( pageText != null && !pageText.equals( cleansedText ) )
            {
                page.setContent( cleansedText );
            }
        }
        catch ( ProviderException e )
        {
            throw new WikiSecurityException( "Could not set Acl. Reason: ProviderException " + e.getMessage(), e );
        }
    }

    /**
     * Generates an ACL string for inclusion in a wiki page, based on a supplied Acl object.
     * All of the permissions in this Acl are assumed to apply to the same page scope.
     * The names of the pages are ignored; only the actions and principals matter.
     * @param acl the ACL
     * @return the ACL string
     */
    protected static String printAcl( Acl acl )
    {
        // Extract the ACL entries into a Map with keys == permissions, values == principals
        Map<String, List<Principal>> permissionPrincipals = new TreeMap<String, List<Principal>>();
        Enumeration<AclEntry> entries = acl.entries();
        while ( entries.hasMoreElements() )
        {
            AclEntry entry = entries.nextElement();
            Principal principal = entry.getPrincipal();
            Enumeration<Permission> permissions = entry.permissions();
            while ( permissions.hasMoreElements() )
            {
                Permission permission = permissions.nextElement();
                List<Principal> principals = permissionPrincipals.get( permission.getActions() );
                if ( principals == null )
                {
                    principals = new ArrayList<Principal>();
                    String action = permission.getActions();
                    if ( action.indexOf(',') != -1 )
                    {
                        throw new IllegalStateException( "AclEntry permission cannot have multiple targets." );
                    }
                    permissionPrincipals.put( action, principals );
                }
                principals.add( principal );
            }
        }

        // Now, iterate through each permission in the map and generate an ACL string

        StringBuilder s = new StringBuilder();
        for ( Map.Entry<String,List<Principal>>entry : permissionPrincipals.entrySet() )
        {
            String action = entry.getKey();
            List<Principal> principals = entry.getValue();
            Collections.sort( principals, new PrincipalComparator() );
            s.append( "[{ALLOW ");
            s.append( action );
            s.append( " ");
            for ( int i = 0; i < principals.size(); i++ )
            {
                Principal principal = principals.get( i );
                s.append( principal.getName() );
                if ( i < ( principals.size() - 1 ) )
                {
                    s.append(",");
                }
            }
            s.append( "}]\n");
        }
        return s.toString();
    }

}
