/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.security.Permission;
import java.security.Principal;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.freshcookies.security.policy.Grantee;
import org.freshcookies.security.policy.PolicyReader;
import org.jdom.JDOMException;

import com.ecyrd.jspwiki.InternalWikiException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer;
import com.ecyrd.jspwiki.auth.permissions.AllPermission;
import com.ecyrd.jspwiki.auth.permissions.PagePermission;
import com.ecyrd.jspwiki.auth.permissions.WikiPermission;

/**
 * Helper class for verifying JSPWiki's security configuration. Invoked by
 * <code>admin/SecurityConfig.jsp</code>.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2006-05-08 00:29:05 $
 * @since 2.4
 */
public class SecurityVerifier
{
    private static final long     serialVersionUID             = -3859563355089169941L;

    private WikiEngine            m_engine;

    private File                  m_jaasProperty               = null;

    private boolean               m_isJaasConfigured           = false;

    private File                  m_securityPolicyProperty     = null;

    private boolean               m_isSecurityPolicyConfigured = false;

    private Principal[]           m_policyPrincipals           = new Principal[0];

    private String                m_jaasConfigurationStatus;

    private String                m_securityPolicyStatus;

    private static final String[] CONTAINER_ACTIONS            = new String[]
                                                               { "View pages", "Comment on existing pages",
            "Edit pages", "Upload attachments", "Create a new group", "Rename an existing page", "Delete pages" };

    private static final String[] CONTAINER_JSPS               = new String[]
                                                               { "/Wiki.jsp", "/Comment.jsp", "/Edit.jsp",
            "/Upload.jsp", "/NewGroup.jsp", "/Rename.jsp", "/Delete.jsp" };

    Logger                        log                          = Logger.getLogger( this.getClass().getName() );

    public SecurityVerifier( WikiEngine engine )
    {
        super();
        m_engine = engine;
        verifyJaas();
        verifyPolicy();
    }

    /**
     * Returns an array of unique Principals from the JSPWIki security policy
     * file. This array will be zero-length if the policy file was not
     * successfully located, or if the file did not specify any Principals in
     * the policy.
     */
    public Principal[] policyPrincipals()
    {
        return m_policyPrincipals;
    }

    /**
     * Formats and returns an HTML table containing sample permissions and what
     * roles are allowed to have them.
     * @throws IllegalStateException if the authorizer is not of type
     *             {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer}
     */
    public String policyRoleTable()
    {
        Principal[] roles = m_policyPrincipals;
        String wiki = m_engine.getApplicationName();
        String[] pages = new String[]
        { "Main", "Index", "GroupTest", "GroupAdmin" };
        String[] actions = new String[]
        { "view", "edit", "modify", "rename", "delete" };

        StringBuffer s = new StringBuffer();

        // Write the table header
        s.append( "<table border=\"1\">" );
        s.append( "<tr><th>Permission</th>" );
        for( int i = 0; i < roles.length; i++ )
        {
            s.append( "<th>" + roles[i].getName() + "<br/><small>" + roles[i].getClass().getName() + "</small></th>" );
        }
        s.append( "</tr>" );

        // Write page permission tests first
        for( int i = 0; i < pages.length; i++ )
        {
            String page = pages[i];
            for( int j = 0; j < actions.length; j++ )
            {
                s.append( "<tr>" );
                Permission permission = new PagePermission( wiki + ":" + page, actions[j] );
                s.append( "<td>PagePermission \"" + permission.getName() + "\", \"" + permission.getActions()
                        + "\"</td>" );
                for( int k = 0; k < roles.length; k++ )
                {
                    Principal role = roles[k];
                    boolean allowed = verifyStaticPermission( role, permission );
                    s.append( "<td>" + ( allowed ? "allow" : "<font color=\"red\">deny</font>" ) + "</td>" );
                }
                s.append( "</tr>" );
            }
        }

        // Now check the wiki-wide permissions
        String[] wikiPerms = new String[]
        { "createGroups", "createPages", "login", "editPreferences", "editProfile" };
        for( int i = 0; i < wikiPerms.length; i++ )
        {
            s.append( "<tr>" );
            s.append( "<td>WikiPermission \"" + wiki + "\",\"" + wikiPerms[i] + "\"</td>" );
            for( int j = 0; j < roles.length; j++ )
            {
                Permission permission = new WikiPermission( wiki, wikiPerms[i] );
                Principal role = roles[j];
                boolean allowed = verifyStaticPermission( role, permission );
                s.append( "<td>" + ( allowed ? "allow" : "<font color=\"red\">deny</font>" ) + "</td>" );
            }
            s.append( "</tr>" );
        }

        // Lastly, check for AllPermission
        s.append( "<tr>" );
        s.append( "<td>AllPermission \"" + wiki + "\"</td>" );
        for( int j = 0; j < roles.length; j++ )
        {
            Permission permission = new AllPermission( wiki );
            Principal role = roles[j];
            boolean allowed = verifyStaticPermission( role, permission );
            s.append( "<td>" + ( allowed ? "allow" : "<font color=\"red\">deny</font>" ) + "</td>" );
        }
        s.append( "</tr>" );

        // We're done!
        s.append( "</table>" );
        return s.toString();
    }

    /**
     * Formats and returns an HTML table containing the roles the web container
     * is aware of, and whether each role maps to particular JSPs.
     * @throws IllegalStateException if the authorizer is not of type
     *             {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer}
     */
    public String containerRoleTable()
    {

        AuthorizationManager authorizationManager = m_engine.getAuthorizationManager();
        Authorizer authorizer = authorizationManager.getAuthorizer();

        // If authorizer not WebContainerAuthozerer, print error message
        if ( !( authorizer instanceof WebContainerAuthorizer ) )
        {
            throw new IllegalStateException( "Authorizer should be WebContainerAuthorizer" );
        }

        // Now, print a table with JSP pages listed on the left, and
        // an evaluation of each pages' constraints for each role
        // we discovered
        StringBuffer s = new StringBuffer();
        Principal[] roles = authorizer.getRoles();
        s.append( "<table border=\"1\"><thead><tr>" + "<th rowspan=\"2\">Action</th>" + "<th rowspan=\"2\">Page</th>"
                + "<th colspan=\"" + roles.length + 1 + "\">Roles</th></tr>" );
        s.append( "<tr><th>Anonymous</th>" );
        for( int i = 0; i < roles.length; i++ )
        {
            s.append( "<th>" + roles[i].getName() + "</th>" );
        }
        s.append( "</tr></thead><tbody>" );

        try
        {
            WebContainerAuthorizer wca = (WebContainerAuthorizer) authorizer;
            for( int i = 0; i < CONTAINER_ACTIONS.length; i++ )
            {
                String action = CONTAINER_ACTIONS[i];
                String jsp = CONTAINER_JSPS[i];
                boolean allowsAnonymous = !wca.isConstrained( jsp, Role.ALL );
                s.append( "<tr><td>" + action + "</td>" );
                s.append( "<td>" + jsp + "</td>" );
                s.append( "<td>" + ( allowsAnonymous ? "allow" : "<font color=\"red\">deny</font>" ) + "</td>" );
                for( int j = 0; j < roles.length; j++ )
                {
                    Role role = (Role) roles[j];
                    boolean allowsRole = allowsAnonymous || wca.isConstrained( jsp, role );
                    s.append( "<td>" + ( allowsRole ? "allow" : "<font color=\"red\">deny</font>" ) + "</td>" );
                }
                s.append( "</tr>" );
            }
        }
        catch( JDOMException e )
        {
            // If we couldn't evaluate constraints it means
            // there's some sort of IO mess or parsing issue
            log.error( "Malformed XML in web.xml", e );
            throw new InternalWikiException( e.getClass().getName() + ": " + e.getMessage() );
        }

        s.append( "</tbody></table>" );
        return s.toString();
    }

    /**
     * Returns <code>true</code> if JAAS is configured correctly.
     * @return
     */
    public boolean isJaasConfigured()
    {
        return m_isJaasConfigured;
    }

    /**
     * Returns <code>true</code> if the JAAS login configuration was already
     * set when JSPWiki started up. We determine this value by consulting a
     * protected member field of {@link AuthenticationManager}, which was set
     * at in initialization by {@link PolicyLoader}.
     * @return <code>true</code> if {@link PolicyLoader} successfully set the
     *         policy, or <code>false</code> for any other reason.
     */
    public boolean isJaasConfiguredAtStartup()
    {
        return m_engine.getAuthenticationManager().m_isJaasConfiguredAtStartup;
    }

    /**
     * Returns <code>true</code> if JSPWiki can locate a named JAAS login
     * configuration.
     * @param config the name of the application (e.g.,
     *            <code>JSPWiki-container</code>).
     * @return <code>true</code> if found; <code>false</code> otherwise
     */
    protected boolean isJaasConfigurationAvailable( String config, StringBuffer messages )
    {
        try
        {
            messages.append( "<p>Success: We found the <code>" + config + "</code> login configuration.</p>" );
            new LoginContext( config );
            return true;
        }
        catch( LoginException e )
        {
            messages.append( "<p class=\"error\">Error: We could not find the <code>" + config
                    + "</code> login configuration.</p>" );
            return false;
        }
    }

    /**
     * Returns <code>true</code> if the Java security policy is configured
     * correctly, and it verifies as valid.
     * @return
     */
    public boolean isSecurityPolicyConfigured()
    {
        return m_isSecurityPolicyConfigured;
    }

    /**
     * Returns <code>true</code> if the Java security policy file was already
     * set when JSPWiki started up. We determine this value by consulting a
     * protected member field of {@link AuthenticationManager}, which was set
     * at in initialization by {@link PolicyLoader}.
     * @return <code>true</code> if {@link PolicyLoader} successfully set the
     *         policy, or <code>false</code> for any other reason.
     */
    public boolean isSecurityPolicyConfiguredAtStartup()
    {
        return m_engine.getAuthenticationManager().m_isJavaPolicyConfiguredAtStartup;
    }

    /**
     * Returns JAAS login configuration system property as an absolute file
     * name, or <code>null</code> if not found.
     * @return
     */
    public String jaasProperty()
    {
        return m_jaasProperty.getAbsolutePath();
    }

    /**
     * Prints the JAAS system property status, or an error string if not
     * configured.
     * @return
     */
    public String jaasConfigurationStatus()
    {
        return m_jaasConfigurationStatus;
    }

    /**
     * Returns the Java security policy system property as an absolute file
     * name, or <code>null</code> if not found.
     * @return
     */
    public String securityPolicyProperty()
    {
        return m_securityPolicyProperty.getAbsolutePath();
    }

    /**
     * Prints the Java security policy status, or an error string if not
     * configured.
     * @return
     */
    public String securityPolicyStatus()
    {
        return m_securityPolicyStatus;
    }

    protected boolean verifyStaticPermission( Principal principal, Permission permission )
    {
        Subject subject = new Subject();
        subject.getPrincipals().add( principal );
        return m_engine.getAuthorizationManager().checkStaticPermission( subject, permission );
    }

    /**
     * If the active Authorizer is the WebContainerAuthorizer, returns the roles
     * it knows about; otherwise, a zero-length array.
     * @return
     */
    public Principal[] webContainerRoles()
    {
        Authorizer authorizer = m_engine.getAuthorizationManager().getAuthorizer();
        if ( authorizer instanceof WebContainerAuthorizer )
        {
            return ( (WebContainerAuthorizer) authorizer ).getRoles();
        }
        return new Principal[0];
    }

    /**
     * Verfies the JAAS configuration. The configuration is valid if value of
     * the system property <code>java.security.auth.login.config</code>
     * resolves to an existing file, and we can find the JAAS login
     * configurations for <code>JSPWiki-container</code> and
     * <code>JSPWiki-custom</code>.
     */
    protected void verifyJaas()
    {
        // Validate the property is set correctly
        StringBuffer messages = new StringBuffer();
        m_jaasProperty = getFileFromProperty( "java.security.auth.login.config", messages );

        // Look for the JSPWiki-container config
        boolean foundJaasContainerConfig = isJaasConfigurationAvailable( "JSPWiki-container", messages );

        // Look for the JSPWiki-custom config
        boolean foundJaasCustomConfig = isJaasConfigurationAvailable( "JSPWiki-custom", messages );
        m_jaasConfigurationStatus = messages.toString();

        m_isJaasConfigured = ( m_jaasProperty != null && foundJaasContainerConfig && foundJaasCustomConfig );
    }

    protected File getFileFromProperty( String property, StringBuffer messages )
    {
        String propertyValue = null;
        try
        {
            propertyValue = System.getProperty( property );
            if ( propertyValue == null )
            {
                messages
                        .append( "<p class=\"error\">"
                                + "Error: We can't find the Java security policy. This is a problem.Check to see if your web container configures JAAS itself, or if you've got the system property <code>java.security.policy</code> pointing to a non-existent file."
                                + "</p>" );
                return null;
            }
            try
            {
                URL url = new URL( propertyValue );
                File file = new File( url.getPath() );
                if ( file.exists() )
                {
                    return file;
                }
            }
            catch( MalformedURLException e )
            {
                // Swallow exception because we can't find it anyway
            }
            messages.append( "<p class=\"warning\">" + "Warning: File <code>" + propertyValue
                    + "</code> doesn't seem to exist. This might be a problem." + "</p>" );
            return null;
        }
        catch( SecurityException e )
        {
            messages
                    .append( "<p class=\"error\">"
                            + "Error: we could not read system property <code>java.security.policy</code>. This is probably because you are running with a security manager."
                            + "</p>" );
            return null;
        }
    }

    /**
     * Verfies the Java security policy configuration. The configuration is
     * valid if value of the system property <code>java.security.policy</code>
     * resolves to an existing file, and the policy file that this file
     * represents a valid policy.
     */
    protected void verifyPolicy()
    {
        // Look up the policy property and set the status text.
        StringBuffer messages = new StringBuffer();
        m_securityPolicyProperty = getFileFromProperty( "java.security.policy", messages );

        // Next, verify the policy
        if ( m_securityPolicyProperty != null )
        {
            // Get the file
            PolicyReader policy = new PolicyReader( m_securityPolicyProperty );

            try
            {
                // See if there is a keystore that's valid
                KeyStore ks = policy.getKeyStore();
                if ( ks == null )
                {
                    messages
                            .append( "<p class=\"error\">Policy file does not have a keystore... at least not one that we can locate." );
                }
                else
                {
                    messages
                            .append( "<p>The security policy specifies a keystore, and we were able to locate it in the filesystem." );
                }

                // Verify the file
                messages.append( "<p>Verifying security policy <code>" + policy.getFile() + "</code>... " );
                policy.read();
                List errors = policy.getMessages();
                if ( errors.size() > 0 )
                {
                    for( Iterator it = errors.iterator(); it.hasNext(); )
                    {
                        Exception e = (Exception) it.next();
                        messages.append( "<p class=\"error\">Error: " + e.getMessage() + "</p>" );
                    }
                }
                else
                {
                    messages.append( "done. It looks fine.</p>" );
                    m_isSecurityPolicyConfigured = true;
                }

                // Stash the unique principals mentioned in the file,
                // plus our standard roles.
                Set principals = new LinkedHashSet();
                principals.add( Role.ALL );
                principals.add( Role.ANONYMOUS );
                principals.add( Role.ASSERTED );
                principals.add( Role.AUTHENTICATED );
                Grantee[] grantees = policy.grantees();
                for( int i = 0; i < grantees.length; i++ )
                {
                    Principal[] granteePrincipals = grantees[i].getPrincipals();
                    for( int j = 0; j < granteePrincipals.length; j++ )
                    {
                        principals.add( granteePrincipals[j] );
                    }
                }
                m_policyPrincipals = (Principal[]) principals.toArray( new Principal[principals.size()] );
            }
            catch( IOException e )
            {
                messages.append( "<p class=\"error\">Error: " + e.getMessage() + "</p>" );
            }
        }
        m_securityPolicyStatus = messages.toString();
    }

    /**
     * @return the m_jaasProperty
     */
    public final File jaasConfigurationFile()
    {
        return m_jaasProperty;
    }

    /**
     * @return the m_securityPolicyProperty
     */
    public final File securityPolicyFile()
    {
        return m_securityPolicyProperty;
    }
}
