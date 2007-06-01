/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth.acl;

import java.security.Permission;
import java.security.Principal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.PrincipalComparator;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.PermissionFactory;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.render.RenderingManager;

/**
 * Default implementation that parses Acls from wiki page markup.
 * @author Andrew Jaquith
 * @since 2.3
 */
public class DefaultAclManager implements AclManager
{
    static Logger                log    = Logger.getLogger( DefaultAclManager.class );

    private AuthorizationManager m_auth = null;
    private WikiEngine m_engine = null;
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
     * This method is guaranteed to return a non-<code>null</code> Acl.
     * @param page the page
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
            if( page instanceof Attachment )
            {
                WikiPage parent = m_engine.getPage( ((Attachment)page).getParentName() );

                acl = getPermissions( parent );
            }
            else
            {
                //
                //  Or, try parsing the page
                //
                WikiContext ctx = new WikiContext( m_engine, page );

                ctx.setVariable( RenderingManager.VAR_EXECUTE_PLUGINS, Boolean.FALSE );

                m_engine.getHTML( ctx, page );

                page = m_engine.getPage( page.getName(), page.getVersion() );
                acl = page.getAcl();

                if( acl == null )
                {
                    acl = new AclImpl();
                    page.setAcl( acl );
                }
            }
        }

        return acl;
    }

    /**
     * Sets the access control list for the page and persists it by prepending
     * it to the wiki page markup and saving the page. When this method is
     * called, all other ACL markup in the page is removed. This method will forcibly
     * expire locks on the wiki page if they exist. Any ProviderExceptions will be
     * re-thrown as WikiSecurityExceptions.
     * @param page the wiki page
     * @param acl the access control list
     * @since 2.5
     * @throws WikiSecurityException of the Acl cannot be set
     */
    public void setPermissions( WikiPage page, Acl acl ) throws WikiSecurityException
    {
        PageManager pageManager = m_engine.getPageManager();

        // Forcibly expire any page locks
        PageLock lock = pageManager.getCurrentLock( page );
        if ( lock != null )
        {
            pageManager.unlockPage( lock );
        }

        // Remove all of the existing ACLs.
        String pageText = m_engine.getPureText( page );
        Matcher matcher = DefaultAclManager.ACL_PATTERN.matcher( pageText );
        String cleansedText = matcher.replaceAll( "" );
        String newText = DefaultAclManager.printAcl( page.getAcl() ) + cleansedText;
        try
        {
            pageManager.putPageText( page, newText );
        }
        catch ( ProviderException e )
        {
            throw new WikiSecurityException( "Could not set Acl. Reason: ProviderExcpetion " + e.getMessage() );
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
        Map permissionPrincipals = new TreeMap();
        Enumeration entries = acl.entries();
        while ( entries.hasMoreElements() )
        {
            AclEntry entry = (AclEntry)entries.nextElement();
            Principal principal = entry.getPrincipal();
            Enumeration permissions = entry.permissions();
            while ( permissions.hasMoreElements() )
            {
                Permission permission = (Permission)permissions.nextElement();
                List principals = (List)permissionPrincipals.get( permission.getActions() );
                if ( principals == null )
                {
                    principals = new ArrayList();
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

        StringBuffer s = new StringBuffer();
        for ( Iterator it = permissionPrincipals.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)it.next();
            String action = (String)entry.getKey();
            List principals = (List)entry.getValue();
            Collections.sort( principals, new PrincipalComparator() );
            s.append( "[{ALLOW ");
            s.append( action );
            s.append( " ");
            for ( int i = 0; i < principals.size(); i++ )
            {
                Principal principal = (Principal)principals.get( i );
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
