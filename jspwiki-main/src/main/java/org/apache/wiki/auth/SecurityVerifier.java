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
package org.apache.wiki.auth;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Session;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupDatabase;
import org.apache.wiki.auth.authorize.GroupManager;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.auth.authorize.WebContainerAuthorizer;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.permissions.GroupPermission;
import org.apache.wiki.auth.permissions.PermissionFactory;
import org.apache.wiki.auth.permissions.WikiPermission;
import org.apache.wiki.auth.user.DummyUserDatabase;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.auth.user.UserProfile;
import org.freshcookies.security.policy.PolicyReader;

import javax.security.auth.Subject;
import javax.security.auth.spi.LoginModule;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.KeyStore;
import java.security.Permission;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Helper class for verifying JSPWiki's security configuration. Invoked by <code>admin/SecurityConfig.jsp</code>.
 *
 * @since 2.4
 */
public final class SecurityVerifier {

    private final Engine                m_engine;

    private boolean               m_isSecurityPolicyConfigured;

    private Principal[]           m_policyPrincipals           = new Principal[0];

    private final Session               m_session;

    /** Message prefix for errors. */
    public static final String    ERROR                        = "Error.";

    /** Message prefix for warnings. */
    public static final String    WARNING                      = "Warning.";

    /** Message prefix for information messages. */
    public static final String    INFO                         = "Info.";

    /** Message topic for policy errors. */
    public static final String    ERROR_POLICY                 = "Error.Policy";

    /** Message topic for policy warnings. */
    public static final String    WARNING_POLICY               = "Warning.Policy";

    /** Message topic for policy information messages. */
    public static final String    INFO_POLICY                  = "Info.Policy";

    /** Message topic for JAAS errors. */
    public static final String    ERROR_JAAS                   = "Error.Jaas";

    /** Message topic for JAAS warnings. */
    public static final String    WARNING_JAAS                 = "Warning.Jaas";

    /** Message topic for role-checking errors. */
    public static final String    ERROR_ROLES                  = "Error.Roles";

    /** Message topic for role-checking information messages. */
    public static final String    INFO_ROLES                   = "Info.Roles";

    /** Message topic for user database errors. */
    public static final String    ERROR_DB                     = "Error.UserDatabase";

    /** Message topic for user database warnings. */
    public static final String    WARNING_DB                   = "Warning.UserDatabase";

    /** Message topic for user database information messages. */
    public static final String    INFO_DB                      = "Info.UserDatabase";

    /** Message topic for group database errors. */
    public static final String    ERROR_GROUPS                 = "Error.GroupDatabase";

    /** Message topic for group database warnings. */
    public static final String    WARNING_GROUPS               = "Warning.GroupDatabase";

    /** Message topic for group database information messages. */
    public static final String    INFO_GROUPS                  = "Info.GroupDatabase";

    /** Message topic for JAAS information messages. */
    public static final String    INFO_JAAS                    = "Info.Jaas";

    private static final String[] CONTAINER_ACTIONS            = new String[] { "View pages",
                                                                                "Comment on existing pages",
                                                                                "Edit pages",
                                                                                "Upload attachments",
                                                                                "Create a new group",
                                                                                "Rename an existing page",
                                                                                "Delete pages"
                                                                              };

    private static final String[] CONTAINER_JSPS               = new String[] { "/Wiki.jsp",
                                                                                "/Comment.jsp",
                                                                                "/Edit.jsp",
                                                                                "/Upload.jsp",
                                                                                "/NewGroup.jsp",
                                                                                "/Rename.jsp",
                                                                                "/Delete.jsp"
                                                                              };

    private static final String   BG_GREEN                     = "bgcolor=\"#c0ffc0\"";

    private static final String   BG_RED                       = "bgcolor=\"#ffc0c0\"";

    private static final Logger LOG                          = Logger.getLogger( SecurityVerifier.class.getName() );

    /**
     * Constructs a new SecurityVerifier for a supplied Engine and WikiSession.
     *
     * @param engine the wiki engine
     * @param session the wiki session (typically, that of an administrator)
     */
    public SecurityVerifier( final Engine engine, final Session session ) {
        m_engine = engine;
        m_session = session;
        m_session.clearMessages();
        verifyJaas();
        verifyPolicy();
        try {
            verifyPolicyAndContainerRoles();
        } catch( final WikiException e ) {
            m_session.addMessage( ERROR_ROLES, e.getMessage() );
        }
        verifyGroupDatabase();
        verifyUserDatabase();
    }

    /**
     * Returns an array of unique Principals from the JSPWIki security policy
     * file. This array will be zero-length if the policy file was not
     * successfully located, or if the file did not specify any Principals in
     * the policy.
     * @return the array of principals
     */
    public Principal[] policyPrincipals()
    {
        return m_policyPrincipals;
    }

    /**
     * Formats and returns an HTML table containing sample permissions and what
     * roles are allowed to have them. This method will throw an
     * {@link IllegalStateException} if the authorizer is not of type
     * {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer}
     * @return the formatted HTML table containing the result of the tests
     */
    public String policyRoleTable()
    {
        final Principal[] roles = m_policyPrincipals;
        final String wiki = m_engine.getApplicationName();

        final String[] pages = new String[]
        { "Main", "Index", "GroupTest", "GroupAdmin" };
        final String[] pageActions = new String[]
        { "view", "edit", "modify", "rename", "delete" };

        final String[] groups = new String[]
        { "Admin", "TestGroup", "Foo" };
        final String[] groupActions = new String[]
        { "view", "edit", null, null, "delete" };


        final int rolesLength = roles.length;
        final int pageActionsLength = pageActions.length;
        // Calculate column widths
        final String colWidth;
        if( pageActionsLength > 0 && rolesLength > 0 ) {
            colWidth = ( 67f / ( pageActionsLength * rolesLength ) ) + "%";
        } else {
            colWidth = "67%";
        }

        final StringBuilder s = new StringBuilder();

        // Write the table header
        s.append( "<table class=\"wikitable\" border=\"1\">\n" );
        s.append( "  <colgroup span=\"1\" width=\"33%\"/>\n" );
        s.append( "  <colgroup span=\"" + pageActionsLength * rolesLength + "\" width=\"" + colWidth
                + "\" align=\"center\"/>\n" );
        s.append( "  <tr>\n" );
        s.append( "    <th rowspan=\"2\" valign=\"bottom\">Permission</th>\n" );
        for( int i = 0; i < rolesLength; i++ )
        {
            s.append( "    <th colspan=\"" + pageActionsLength + "\" title=\"" + roles[i].getClass().getName() + "\">"
                    + roles[i].getName() + "</th>\n" );
        }
        s.append( "  </tr>\n" );

        // Print a column for each role
        s.append( "  <tr>\n" );
        for( int i = 0; i < rolesLength; i++ )
        {
            for( final String pageAction : pageActions )
            {
                final String action = pageAction.substring( 0, 1 );
                s.append( "    <th title=\"" + pageAction + "\">" + action + "</th>\n" );
            }
        }
        s.append( "  </tr>\n" );

        // Write page permission tests first
        for( final String page : pages ) {
            s.append( "  <tr>\n" );
            s.append( "    <td>PagePermission \"" + wiki + ":" + page + "\"</td>\n" );
            for( final Principal role : roles ) {
                for( final String pageAction : pageActions ) {
                    final Permission permission = PermissionFactory.getPagePermission( wiki + ":" + page, pageAction );
                    s.append( printPermissionTest( permission, role, 1 ) );
                }
            }
            s.append( "  </tr>\n" );
        }

        // Now do the group tests
        for( final String group : groups ) {
            s.append( "  <tr>\n" );
            s.append( "    <td>GroupPermission \"" + wiki + ":" + group + "\"</td>\n" );
            for( final Principal role : roles ) {
                for( final String groupAction : groupActions ) {
                    Permission permission = null;
                    if( groupAction != null ) {
                        permission = new GroupPermission( wiki + ":" + group, groupAction );
                    }
                    s.append( printPermissionTest( permission, role, 1 ) );
                }
            }
            s.append( "  </tr>\n" );
        }


        // Now check the wiki-wide permissions
        final String[] wikiPerms = new String[] { "createGroups", "createPages", "login", "editPreferences", "editProfile" };
        for( final String wikiPerm : wikiPerms ) {
            s.append( "  <tr>\n" );
            s.append( "    <td>WikiPermission \"" + wiki + "\",\"" + wikiPerm + "\"</td>\n" );
            for( final Principal role : roles ) {
                final Permission permission = new WikiPermission( wiki, wikiPerm );
                s.append( printPermissionTest( permission, role, pageActionsLength ) );
            }
            s.append( "  </tr>\n" );
        }

        // Lastly, check for AllPermission
        s.append( "  <tr>\n" );
        s.append( "    <td>AllPermission \"" + wiki + "\"</td>\n" );
        for( final Principal role : roles )
        {
            final Permission permission = new AllPermission( wiki );
            s.append( printPermissionTest( permission, role, pageActionsLength ) );
        }
        s.append( "  </tr>\n" );

        // We're done!
        s.append( "</table>" );
        return s.toString();
    }

    /**
     * Prints a &lt;td&gt; HTML element with the results of a permission test.
     * @param permission the permission to format
     * @param principal
     * @param cols
     */
    private String printPermissionTest( final Permission permission, final Principal principal, final int cols ) {
    	final StringBuilder s = new StringBuilder();
        if( permission == null ) {
            s.append( "    <td colspan=\"" + cols + "\" align=\"center\" title=\"N/A\">" );
            s.append( "&nbsp;</td>\n" );
        } else {
            final boolean allowed = verifyStaticPermission( principal, permission );
            s.append( "    <td colspan=\"" + cols + "\" align=\"center\" title=\"" );
            s.append( allowed ? "ALLOW: " : "DENY: " );
            s.append( permission.getClass().getName() );
            s.append( " &quot;" );
            s.append( permission.getName() );
            s.append( "&quot;" );
            if ( permission.getName() != null )
            {
                s.append( ",&quot;" );
                s.append( permission.getActions() );
                s.append( "&quot;" );
            }
            s.append( " " );
            s.append( principal.getClass().getName() );
            s.append( " &quot;" );
            s.append( principal.getName() );
            s.append( "&quot;" );
            s.append( "\"" );
            s.append( allowed ? BG_GREEN + ">" : BG_RED + ">" );
            s.append( "&nbsp;</td>\n" );
        }
        return s.toString();
    }

    /**
     * Formats and returns an HTML table containing the roles the web container
     * is aware of, and whether each role maps to particular JSPs. This method
     * throws an {@link IllegalStateException} if the authorizer is not of type
     * {@link org.apache.wiki.auth.authorize.WebContainerAuthorizer}
     * @return the formatted HTML table containing the result of the tests
     * @throws WikiException if tests fail for unexpected reasons
     */
    public String containerRoleTable() throws WikiException {
        final AuthorizationManager authorizationManager = m_engine.getManager( AuthorizationManager.class );
        final Authorizer authorizer = authorizationManager.getAuthorizer();

        // If authorizer not WebContainerAuthorizer, print error message
        if ( !( authorizer instanceof WebContainerAuthorizer ) ) {
            throw new IllegalStateException( "Authorizer should be WebContainerAuthorizer" );
        }

        // Now, print a table with JSP pages listed on the left, and
        // an evaluation of each pages' constraints for each role
        // we discovered
        final StringBuilder s = new StringBuilder();
        final Principal[] roles = authorizer.getRoles();
        s.append( "<table class=\"wikitable\" border=\"1\">\n" );
        s.append( "<thead>\n" );
        s.append( "  <tr>\n" );
        s.append( "    <th rowspan=\"2\">Action</th>\n" );
        s.append( "    <th rowspan=\"2\">Page</th>\n" );
        s.append( "    <th colspan=\"" + roles.length + 1 + "\">Roles</th>\n" );
        s.append( "  </tr>\n" );
        s.append( "  <tr>\n" );
        s.append( "    <th>Anonymous</th>\n" );
        for( final Principal role : roles ) {
            s.append( "    <th>" + role.getName() + "</th>\n" );
        }
        s.append( "</tr>\n" );
        s.append( "</thead>\n" );
        s.append( "<tbody>\n" );

        final WebContainerAuthorizer wca = (WebContainerAuthorizer) authorizer;
        for( int i = 0; i < CONTAINER_ACTIONS.length; i++ ) {
            final String action = CONTAINER_ACTIONS[i];
            final String jsp = CONTAINER_JSPS[i];

            // Print whether the page is constrained for each role
            final boolean allowsAnonymous = !wca.isConstrained( jsp, Role.ALL );
            s.append( "  <tr>\n" );
            s.append( "    <td>" + action + "</td>\n" );
            s.append( "    <td>" + jsp + "</td>\n" );
            s.append( "    <td title=\"" );
            s.append( allowsAnonymous ? "ALLOW: " : "DENY: " );
            s.append( jsp );
            s.append( " Anonymous" );
            s.append( "\"" );
            s.append( allowsAnonymous ? BG_GREEN + ">" : BG_RED + ">" );
            s.append( "&nbsp;</td>\n" );
            for( final Principal role : roles )
            {
                final boolean allowed = allowsAnonymous || wca.isConstrained( jsp, (Role)role );
                s.append( "    <td title=\"" );
                s.append( allowed ? "ALLOW: " : "DENY: " );
                s.append( jsp );
                s.append( " " );
                s.append( role.getClass().getName() );
                s.append( " &quot;" );
                s.append( role.getName() );
                s.append( "&quot;" );
                s.append( "\"" );
                s.append( allowed ? BG_GREEN + ">" : BG_RED + ">" );
                s.append( "&nbsp;</td>\n" );
            }
            s.append( "  </tr>\n" );
        }

        s.append( "</tbody>\n" );
        s.append( "</table>\n" );
        return s.toString();
    }

    /**
     * Returns <code>true</code> if the Java security policy is configured
     * correctly, and it verifies as valid.
     * @return the result of the configuration check
     */
    public boolean isSecurityPolicyConfigured()
    {
        return m_isSecurityPolicyConfigured;
    }

    /**
     * If the active Authorizer is the WebContainerAuthorizer, returns the roles it knows about; otherwise, a zero-length array.
     *
     * @return the roles parsed from <code>web.xml</code>, or a zero-length array
     * @throws WikiException if the web authorizer cannot obtain the list of roles
     */
    public Principal[] webContainerRoles() throws WikiException {
        final Authorizer authorizer = m_engine.getManager( AuthorizationManager.class ).getAuthorizer();
        if ( authorizer instanceof WebContainerAuthorizer ) {
            return authorizer.getRoles();
        }
        return new Principal[0];
    }

    /**
     * Verifies that the roles given in the security policy are reflected by the
     * container <code>web.xml</code> file.
     * @throws WikiException if the web authorizer cannot verify the roles
     */
    protected void verifyPolicyAndContainerRoles() throws WikiException
    {
        final Authorizer authorizer = m_engine.getManager( AuthorizationManager.class ).getAuthorizer();
        final Principal[] containerRoles = authorizer.getRoles();
        boolean missing = false;
        for( final Principal principal : m_policyPrincipals )
        {
            if ( principal instanceof Role )
            {
                final Role role = (Role) principal;
                final boolean isContainerRole = ArrayUtils.contains( containerRoles, role );
                if ( !Role.isBuiltInRole( role ) && !isContainerRole )
                {
                    m_session.addMessage( ERROR_ROLES, "Role '" + role.getName() + "' is defined in security policy but not in web.xml." );
                    missing = true;
                }
            }
        }
        if ( !missing )
        {
            m_session.addMessage( INFO_ROLES, "Every non-standard role defined in the security policy was also found in web.xml." );
        }
    }

    /**
     * Verifies that the group datbase was initialized properly, and that
     * user add and delete operations work as they should.
     */
    protected void verifyGroupDatabase()
    {
        final GroupManager mgr = m_engine.getManager( GroupManager.class );
        GroupDatabase db = null;
        try {
            db = m_engine.getManager( GroupManager.class ).getGroupDatabase();
        } catch ( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_GROUPS, "Could not retrieve GroupManager: " + e.getMessage() );
        }

        // Check for obvious error conditions
        if ( mgr == null || db == null ) {
            if ( mgr == null ) {
                m_session.addMessage( ERROR_GROUPS, "GroupManager is null; JSPWiki could not initialize it. Check the error logs." );
            }
            if ( db == null ) {
                m_session.addMessage( ERROR_GROUPS, "GroupDatabase is null; JSPWiki could not initialize it. Check the error logs." );
            }
            return;
        }

        // Everything initialized OK...

        // Tell user what class of database this is.
        m_session.addMessage( INFO_GROUPS, "GroupDatabase is of type '" + db.getClass().getName() + "'. It appears to be initialized properly." );

        // Now, see how many groups we have.
        final int oldGroupCount;
        try {
            final Group[] groups = db.groups();
            oldGroupCount = groups.length;
            m_session.addMessage( INFO_GROUPS, "The group database contains " + oldGroupCount + " groups." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_GROUPS, "Could not obtain a list of current groups: " + e.getMessage() );
            return;
        }

        // Try adding a bogus group with random name
        final String name = "TestGroup" + System.currentTimeMillis();
        final Group group;
        try {
            // Create dummy test group
            group = mgr.parseGroup( name, "", true );
            final Principal user = new WikiPrincipal( "TestUser" );
            group.add( user );
            db.save( group, new WikiPrincipal( "SecurityVerifier" ) );

            // Make sure the group saved successfully
            if( db.groups().length == oldGroupCount ) {
                m_session.addMessage( ERROR_GROUPS, "Could not add a test group to the database." );
                return;
            }
            m_session.addMessage( INFO_GROUPS, "The group database allows new groups to be created, as it should." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_GROUPS, "Could not add a group to the database: " + e.getMessage() );
            return;
        }

        // Now delete the group; should be back to old count
        try {
            db.delete( group );
            if( db.groups().length != oldGroupCount ) {
                m_session.addMessage( ERROR_GROUPS, "Could not delete a test group from the database." );
                return;
            }
            m_session.addMessage( INFO_GROUPS, "The group database allows groups to be deleted, as it should." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_GROUPS, "Could not delete a test group from the database: " + e.getMessage() );
            return;
        }

        m_session.addMessage( INFO_GROUPS, "The group database configuration looks fine." );
    }

    /**
     * Verfies the JAAS configuration. The configuration is valid if value of the
     * <code>jspwiki.properties<code> property
     * {@value org.apache.wiki.auth.AuthenticationManager#PROP_LOGIN_MODULE}
     * resolves to a valid class on the classpath.
     */
    protected void verifyJaas() {
        // Verify that the specified JAAS moduie corresponds to a class we can load successfully.
        final String jaasClass = m_engine.getWikiProperties().getProperty( AuthenticationManager.PROP_LOGIN_MODULE );
        if( jaasClass == null || jaasClass.length() == 0 ) {
            m_session.addMessage( ERROR_JAAS, "The value of the '" + AuthenticationManager.PROP_LOGIN_MODULE
                    + "' property was null or blank. This is a fatal error. This value should be set to a valid LoginModule implementation "
                    + "on the classpath." );
            return;
        }

        // See if we can find the LoginModule on the classpath
        Class< ? > c = null;
        try {
            m_session.addMessage( INFO_JAAS,
                    "The property '" + AuthenticationManager.PROP_LOGIN_MODULE + "' specified the class '" + jaasClass + ".'" );
            c = Class.forName( jaasClass );
        } catch( final ClassNotFoundException e ) {
            m_session.addMessage( ERROR_JAAS, "We could not find the the class '" + jaasClass + "' on the " + "classpath. This is fatal error." );
        }

        // Is the specified class actually a LoginModule?
        if( LoginModule.class.isAssignableFrom( c ) ) {
            m_session.addMessage( INFO_JAAS, "We found the the class '" + jaasClass + "' on the classpath, and it is a LoginModule implementation. Good!" );
        } else {
            m_session.addMessage( ERROR_JAAS, "We found the the class '" + jaasClass + "' on the classpath, but it does not seem to be LoginModule implementation! This is fatal error." );
        }
    }

    /**
     * Looks up a file name based on a JRE system property and returns the associated
     * File object if it exists. This method adds messages with the topic prefix 
     * {@link #ERROR} and {@link #INFO} as appropriate, with the suffix matching the 
     * supplied property.
     * @param property the system property to look up
     * @return the file object, or <code>null</code> if not found
     */
    protected File getFileFromProperty( final String property )
    {
        String propertyValue = null;
        try
        {
            propertyValue = System.getProperty( property );
            if ( propertyValue == null )
            {
                m_session.addMessage( "Error." + property, "The system property '" + property + "' is null." );
                return null;
            }

            //
            //  It's also possible to use "==" to mark a property.  We remove that
            //  here so that we can actually find the property file, then.
            //
            if( propertyValue.startsWith("=") )
            {
                propertyValue = propertyValue.substring(1);
            }

            try
            {
                m_session.addMessage( "Info." + property, "The system property '" + property + "' is set to: "
                        + propertyValue + "." );

                // Prepend a file: prefix if not there already
                if ( !propertyValue.startsWith( "file:" ) )
                {
                  propertyValue = "file:" + propertyValue;
                }
                final URL url = new URL( propertyValue );
                final File file = new File( url.getPath() );
                if ( file.exists() )
                {
                    m_session.addMessage( "Info." + property, "File '" + propertyValue + "' exists in the filesystem." );
                    return file;
                }
            }
            catch( final MalformedURLException e )
            {
                // Swallow exception because we can't find it anyway
            }
            m_session.addMessage( "Error." + property, "File '" + propertyValue
                    + "' doesn't seem to exist. This might be a problem." );
            return null;
        }
        catch( final SecurityException e )
        {
            m_session.addMessage( "Error." + property, "We could not read system property '" + property
                    + "'. This is probably because you are running with a security manager." );
            return null;
        }
    }

    /**
     * Verfies the Java security policy configuration. The configuration is
     * valid if value of the local policy (at <code>WEB-INF/jspwiki.policy</code>
     * resolves to an existing file, and the policy file contained therein
     * represents a valid policy.
     */
    @SuppressWarnings("unchecked")
    protected void verifyPolicy() {
        // Look up the policy file and set the status text.
        final URL policyURL = m_engine.findConfigFile( AuthorizationManager.DEFAULT_POLICY );
        String path = policyURL.getPath();
        if ( path.startsWith("file:") ) {
            path = path.substring( 5 );
        }
        final File policyFile = new File( path );

        // Next, verify the policy
        try {
            // Get the file
            final PolicyReader policy = new PolicyReader( policyFile );
            m_session.addMessage( INFO_POLICY, "The security policy '" + policy.getFile() + "' exists." );

            // See if there is a keystore that's valid
            final KeyStore ks = policy.getKeyStore();
            if ( ks == null ) {
                m_session.addMessage( WARNING_POLICY,
                    "Policy file does not have a keystore... at least not one that we can locate. If your policy file " +
                    "does not contain any 'signedBy' blocks, this is probably ok." );
            } else {
                m_session.addMessage( INFO_POLICY,
                    "The security policy specifies a keystore, and we were able to locate it in the filesystem." );
            }

            // Verify the file
            policy.read();
            final List<Exception> errors = policy.getMessages();
            if ( errors.size() > 0 ) {
                for( final Exception e : errors ) {
                    m_session.addMessage( ERROR_POLICY, e.getMessage() );
                }
            } else {
                m_session.addMessage( INFO_POLICY, "The security policy looks fine." );
                m_isSecurityPolicyConfigured = true;
            }

            // Stash the unique principals mentioned in the file,
            // plus our standard roles.
            final Set<Principal> principals = new LinkedHashSet<>();
            principals.add( Role.ALL );
            principals.add( Role.ANONYMOUS );
            principals.add( Role.ASSERTED );
            principals.add( Role.AUTHENTICATED );
            final ProtectionDomain[] domains = policy.getProtectionDomains();
            for ( final ProtectionDomain domain : domains ) {
                principals.addAll(Arrays.asList(domain.getPrincipals()));
            }
            m_policyPrincipals = principals.toArray( new Principal[principals.size()] );
        } catch( final IOException e ) {
            m_session.addMessage( ERROR_POLICY, e.getMessage() );
        }
    }

    /**
     * Verifies that a particular Principal possesses a Permission, as defined
     * in the security policy file.
     * @param principal the principal
     * @param permission the permission
     * @return the result, based on consultation with the active Java security
     *         policy
     */
    protected boolean verifyStaticPermission( final Principal principal, final Permission permission )
    {
        final Subject subject = new Subject();
        subject.getPrincipals().add( principal );
        final boolean allowedByGlobalPolicy = (Boolean)
            Subject.doAsPrivileged( subject, ( PrivilegedAction< Object > )() -> {
                try {
                    AccessController.checkPermission( permission );
                    return Boolean.TRUE;
                } catch( final AccessControlException e ) {
                    return Boolean.FALSE;
                }
            }, null );

        if ( allowedByGlobalPolicy )
        {
            return true;
        }

        // Check local policy
        final Principal[] principals = new Principal[]{ principal };
        return m_engine.getManager( AuthorizationManager.class ).allowedByLocalPolicy( principals, permission );
    }

    /**
     * Verifies that the user datbase was initialized properly, and that
     * user add and delete operations work as they should.
     */
    protected void verifyUserDatabase()
    {
        final UserDatabase db = m_engine.getManager( UserManager.class ).getUserDatabase();

        // Check for obvious error conditions
        if ( db == null )
        {
            m_session.addMessage( ERROR_DB, "UserDatabase is null; JSPWiki could not " +
                    "initialize it. Check the error logs." );
            return;
        }

        if ( db instanceof DummyUserDatabase )
        {
            m_session.addMessage( ERROR_DB, "UserDatabase is DummyUserDatabase; JSPWiki " +
                    "may not have been able to initialize the database you supplied in " +
                    "jspwiki.properties, or you left the 'jspwiki.userdatabase' property " +
                    "blank. Check the error logs." );
        }

        // Tell user what class of database this is.
        m_session.addMessage( INFO_DB, "UserDatabase is of type '" + db.getClass().getName() +
                                       "'. It appears to be initialized properly." );

        // Now, see how many users we have.
        final int oldUserCount;
        try {
            final Principal[] users = db.getWikiNames();
            oldUserCount = users.length;
            m_session.addMessage( INFO_DB, "The user database contains " + oldUserCount + " users." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_DB, "Could not obtain a list of current users: " + e.getMessage() );
            return;
        }

        // Try adding a bogus user with random name
        final String loginName = "TestUser" + System.currentTimeMillis();
        try {
            final UserProfile profile = db.newProfile();
            profile.setEmail( "jspwiki.tests@mailinator.com" );
            profile.setLoginName( loginName );
            profile.setFullname( "FullName" + loginName );
            profile.setPassword( "password" );
            db.save( profile );

            // Make sure the profile saved successfully
            if( db.getWikiNames().length == oldUserCount ) {
                m_session.addMessage( ERROR_DB, "Could not add a test user to the database." );
                return;
            }
            m_session.addMessage( INFO_DB, "The user database allows new users to be created, as it should." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_DB, "Could not add a test user to the database: " + e.getMessage() );
            return;
        }

        // Now delete the profile; should be back to old count
        try {
            db.deleteByLoginName( loginName );
            if( db.getWikiNames().length != oldUserCount ) {
                m_session.addMessage( ERROR_DB, "Could not delete a test user from the database." );
                return;
            }
            m_session.addMessage( INFO_DB, "The user database allows users to be deleted, as it should." );
        } catch( final WikiSecurityException e ) {
            m_session.addMessage( ERROR_DB, "Could not delete a test user to the database: " + e.getMessage() );
            return;
        }

        m_session.addMessage( INFO_DB, "The user database configuration looks fine." );
    }
}
