/*
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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.core.Acl;
import org.apache.wiki.api.core.AclEntry;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.pages.PageLock;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.util.comparators.PrincipalComparator;

import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default implementation that parses Acls from wiki page markup.
 *
 * @since 2.3
 */
public class DefaultAclManager implements AclManager {

    private static final Logger log = Logger.getLogger(DefaultAclManager.class);

    private AuthorizationManager m_auth = null;
    private Engine m_engine = null;
    private static final String PERM_REGEX = "("
                                              + PagePermission.COMMENT_ACTION + "|"
                                              + PagePermission.DELETE_ACTION  + "|"
                                              + PagePermission.EDIT_ACTION    + "|"
                                              + PagePermission.MODIFY_ACTION  + "|"
                                              + PagePermission.RENAME_ACTION  + "|"
                                              + PagePermission.UPLOAD_ACTION  + "|"
                                              + PagePermission.VIEW_ACTION    +
                                             ")";
    private static final String ACL_REGEX = "\\[\\{\\s*ALLOW\\s+" + PERM_REGEX + "\\s*(.*?)\\s*\\}\\]";

    /**
     * Identifies ACL strings in wiki text; the first group is the action (view, edit) and
     * the second is the list of Principals separated by commas. The overall match is
     * the ACL string from [{ to }].
     */
    public static final Pattern ACL_PATTERN = Pattern.compile( ACL_REGEX );

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties props ) {
        m_auth = engine.getManager( AuthorizationManager.class );
        m_engine = engine;
    }

    /** {@inheritDoc} */
    @Override
    public Acl parseAcl( final Page page, final String ruleLine ) throws WikiSecurityException {
        Acl acl = page.getAcl();
        if (acl == null) {
            acl = new AclImpl();
        }

        try {
            final StringTokenizer fieldToks = new StringTokenizer(ruleLine);
            fieldToks.nextToken();
            final String actions = fieldToks.nextToken();

            while( fieldToks.hasMoreTokens() ) {
                final String principalName = fieldToks.nextToken(",").trim();
                final Principal principal = m_auth.resolvePrincipal(principalName);
                final AclEntry oldEntry = acl.getAclEntry(principal);

                if( oldEntry != null ) {
                    log.debug( "Adding to old acl list: " + principal + ", " + actions );
                    oldEntry.addPermission( PermissionFactory.getPagePermission( page, actions ) );
                } else {
                    log.debug( "Adding new acl entry for " + actions );
                    final AclEntry entry = new AclEntryImpl();
                    entry.setPrincipal( principal );
                    entry.addPermission( PermissionFactory.getPagePermission( page, actions ) );

                    acl.addEntry( entry );
                }
            }

            page.setAcl( acl );
            log.debug( acl.toString() );
        } catch( final NoSuchElementException nsee ) {
            log.warn( "Invalid access rule: " + ruleLine + " - defaults will be used." );
            throw new WikiSecurityException( "Invalid access rule: " + ruleLine, nsee );
        } catch( final IllegalArgumentException iae ) {
            throw new WikiSecurityException("Invalid permission type: " + ruleLine, iae);
        }

        return acl;
    }


    /** {@inheritDoc} */
    @Override
    public Acl getPermissions( final Page page ) {
        //  Does the page already have cached ACLs?
        Acl acl = page.getAcl();
        log.debug( "page=" + page.getName() + "\n" + acl );

        if( acl == null ) {
            //  If null, try the parent.
            if( page instanceof Attachment ) {
                final Page parent = m_engine.getManager( PageManager.class ).getPage( ( ( Attachment ) page ).getParentName() );
                acl = getPermissions(parent);
            } else {
                //  Or, try parsing the page
                final WikiContext ctx = new WikiContext( m_engine, page );
                ctx.setVariable( Context.VAR_EXECUTE_PLUGINS, Boolean.FALSE );
                m_engine.getManager( RenderingManager.class ).getHTML(ctx, page);

                if (page.getAcl() == null) {
                    page.setAcl( new AclImpl() );
                }
                acl = page.getAcl();
            }
        }

        return acl;
    }

    /** {@inheritDoc} */
    @Override
    public void setPermissions( final Page page, final Acl acl ) throws WikiSecurityException {
        final PageManager pageManager = m_engine.getManager( PageManager.class );

        // Forcibly expire any page locks
        final PageLock lock = pageManager.getCurrentLock( page );
        if( lock != null ) {
            pageManager.unlockPage( lock );
        }

        // Remove all of the existing ACLs.
        final String pageText = m_engine.getManager( PageManager.class ).getPureText( page );
        final Matcher matcher = DefaultAclManager.ACL_PATTERN.matcher( pageText );
        final String cleansedText = matcher.replaceAll("" );
        final String newText = DefaultAclManager.printAcl( page.getAcl() ) + cleansedText;
        try {
            pageManager.putPageText( page, newText );
        } catch( final ProviderException e ) {
            throw new WikiSecurityException( "Could not set Acl. Reason: ProviderExcpetion " + e.getMessage(), e );
        }
    }

    /**
     * Generates an ACL string for inclusion in a wiki page, based on a supplied Acl object. All of the permissions in this Acl are
     * assumed to apply to the same page scope. The names of the pages are ignored; only the actions and principals matter.
     *
     * @param acl the ACL
     * @return the ACL string
     */
    protected static String printAcl( final Acl acl ) {
        // Extract the ACL entries into a Map with keys == permissions, values == principals
        final Map< String, List< Principal > > permissionPrincipals = new TreeMap<>();
        final Enumeration< AclEntry > entries = acl.aclEntries();
        while( entries.hasMoreElements() ) {
            final AclEntry entry = entries.nextElement();
            final Principal principal = entry.getPrincipal();
            final Enumeration< Permission > permissions = entry.permissions();
            while( permissions.hasMoreElements() ) {
                final Permission permission = permissions.nextElement();
                List< Principal > principals = permissionPrincipals.get( permission.getActions() );
                if (principals == null) {
                    principals = new ArrayList<>();
                    final String action = permission.getActions();
                    if( action.indexOf(',') != -1 ) {
                        throw new IllegalStateException("AclEntry permission cannot have multiple targets.");
                    }
                    permissionPrincipals.put( action, principals );
                }
                principals.add( principal );
            }
        }

        // Now, iterate through each permission in the map and generate an ACL string
        final StringBuilder s = new StringBuilder();
        for( final Map.Entry< String, List< Principal > > entry : permissionPrincipals.entrySet() ) {
            final String action = entry.getKey();
            final List< Principal > principals = entry.getValue();
            principals.sort( new PrincipalComparator() );
            s.append( "[{ALLOW " ).append( action ).append( " " );
            for( int i = 0; i < principals.size(); i++ ) {
                final Principal principal = principals.get( i );
                s.append( principal.getName() );
                if( i < ( principals.size() - 1 ) ) {
                    s.append( "," );
                }
            }
            s.append( "}]\n" );
        }
        return s.toString();
    }

}
