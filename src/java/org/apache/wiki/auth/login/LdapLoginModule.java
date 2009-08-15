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
package org.apache.wiki.auth.login;

import java.io.IOException;
import java.security.Principal;
import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.wiki.auth.LdapConfig;
import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * <p>
 * LoginModule that authenticates users against an LDAP server with the user's
 * supplied credentials. The authentication is performed by binding to the LDAP
 * server using the user's credentials, with or without SSL. After the user
 * successfully authenticates, the user's Subject will be populated with three
 * WikiPrincipals:
 * </p>
 * <ul>
 * <li>a WikiPrincipal of type {@link WikiPrincipal#LOGIN_NAME} whose value is
 * the distinguished name (DN) of the user.</li>
 * <li>a WikiPrincipal of type {@link WikiPrincipal#FULL_NAME} whose value is
 * set to either (first name + surname) or common name, with the former
 * preferred.</li>
 * <li>a WikiPrincipal of type {@link WikiPrincipal#WIKI_NAME} whose value is
 * identical to the full name, but from which all whitespace is removed.</li>
 * </ul>
 * <p>
 * To use this LoginModule, callers must initialize it with a JAAS options Map
 * that supplies the following key/value pairs:
 * </p>
 * <ul>
 * <li>{@link #OPTION_CONNECTION_URL} - the connection string for the LDAP
 * server, for example <code>ldap://ldap.jspwiki.org:389/</code>.</li>
 * <li>{@link #OPTION_LOGIN_ID_PATTERN} - optional string pattern indicating how the
 * login id should be formatted into a credential the LDAP server will
 * understand. The exact credential pattern varies by LDAP server. OpenLDAP
 * expects login IDs that match a distinguished name. Active Directory, on the
 * other hand, requires just the "short" login ID that is not in DN format. The
 * user ID supplied during the login will be substituted into the
 * <code>{0}</code> token in this pattern. Valid examples of login ID patterns
 * include <code>uid={0},ou=users,dc=jspwiki,dc=org</code> (for OpenLDAP) and
 * <code>{0}</code> (for Active Directory).</li>
 * <li>{@link #OPTION_USER_BASE} - the distinguished name of the base location
 * where user objects are located. This is generally an organizational unit (OU)
 * DN, such as <code>ou=people,dc=jspwiki,dc=org</code>. The user base and all
 * of its subtrees will be searched. For directories that contain multiple OUs
 * where users are located, use a higher-level base location (e.g.,
 * <code>dc=jspwiki,dc=org</code>).</li>
 * <li>{@link #OPTION_USER_PATTERN} - an RFC 2254 search filter string used for
 * locating the actual user object within the user base. The user ID supplied
 * during the login will be substituted into the <code>{0}</code> token in this
 * pattern, if it contains one. Only the first match will be selected, so it is
 * important that this pattern selects unique objects. For example, if the user
 * pattern is <code>(&(objectClass=inetOrgPerson)(uid={0}))</code> and the user
 * name supplied during login is <code>fflintstone</code>, the the first object
 * within {@link #OPTION_USER_BASE} that matches the filter
 * <code>(&(objectClass=inetOrgPerson)(uid=fflintstone))</code> will be
 * selected. A suitable value for this property that works with Active Directory
 * 2000 and later is <code>(&(objectClass=person)(sAMAccountName={0}))</code>.</li>
 * <li>{@link #OPTION_SSL} - Optional parameter that specifies whether to use
 * SSL when connecting to the LDAP server. Values like <code>true</code> or
 * <code>on</code> indicate that SSL should be used. If this parameter is not
 * supplied, SSL will not be used.</li>
 * <li>{@link #OPTION_AUTHENTICATION} - Optional parameter that specifies the
 * type of authentication method to be used. Valid values include
 * <code>simple</code> for plaintext username/password, and
 * <code>DIGEST-MD5</code> for digested passwords. Note that if SSL is not used,
 * for safety reasons this method will default to <code>DIGEST-MD5</code> to
 * prevent password interception.</li>
 * </ul>
 * <p>
 * If this LoginModule is used with a system-wide JAAS configuration (with or
 * without JSPWiki), the JAAS options can be specified as described above in the
 * JAAS configuration file. However, if this LoginModule is used with JSPWiki
 * custom authentication, the JAAS options map can be set in
 * <code>jspwiki.properties</code>. In this case, each option key must be
 * prefixed by <code>jspwiki.loginModule.options.</code> to indicate that the
 * values are, in fact, JAAS configuration items. Thus, the option
 * {@link #OPTION_CONNECTION_URL} would be configured in
 * <code>jspwiki.properties</code> using the key/value pair
 * <code>jspwiki.loginModule.options.{@value #OPTION_CONNECTION_URL}
 * = ldap://ldap.jspwiki.org:389/</code>.
 * </p>
 * 
 * @author Andrew Jaquith
 */
public class LdapLoginModule extends AbstractLoginModule
{

    private static final Logger log = LoggerFactory.getLogger( LdapLoginModule.class );

    private static final InternationalizationManager I18N = new InternationalizationManager( null );

    private static final Principal[] NO_PRINCIPALS = new Principal[0];

    /**
     * JAAS option that specifies the JNDI authentication type. Valid values are
     * the same as those for {@link Context#SECURITY_AUTHENTICATION}:
     * <code>none</code>, <code>simple</code>, <code>strong</code> or
     * <code>DIGEST-MD5</code>. The default is <code>simple</code> if SSL is
     * specified, and <code>DIGEST-MD5</code> otherwise.
     */
    public static final String OPTION_AUTHENTICATION = "ldap.authentication";

    /**
     * JAAS option that specifies the JNDI connection URL for the LDAP server,
     * <em>e.g.,</em> <code>ldap://127.0.0.1:4890/</code>
     */
    public static final String OPTION_CONNECTION_URL = "ldap.connectionURL";

    /**
     * JAAS option that specifies the base DN where users are contained,
     * <em>e.g.,</em> <code>ou=people,dc=jspwiki,dc=org</code>. This DN is
     * searched recursively.
     */
    public static final String OPTION_USER_BASE = LdapConfig.PROPERTY_USER_BASE;

    /**
     * JAAS option that specifies the DN pattern for finding users within the
     * user base, <em>e.g.,</em>
     * <code>(&(objectClass=inetOrgPerson)(uid={0}))</code>
     */
    public static final String OPTION_USER_PATTERN = LdapConfig.PROPERTY_USER_PATTERN;

    /**
     * JAAS option specifies the pattern for the username used to log in to the
     * LDAP server. This pattern maps the username supplied at login time by the
     * user to a username format the LDAP server can recognized. Usually this is
     * a pattern that produces a full DN, for example
     * <code>uid={0},ou=people,dc=jspwiki,dc=org</code>. However, sometimes (as
     * with Active Directory 2003 and later) only the userid is used, in which
     * case the principal will simply be <code>{0}</code>. The default value
     * if not supplied is <code>{0}</code>.
     */
    public static final String OPTION_LOGIN_ID_PATTERN = LdapConfig.PROPERTY_LOGIN_ID_PATTERN;

    /**
     * JAAS option specifies that indicates whether to use SSL for connecting to
     * the LDAP server.
     */
    protected static final String OPTION_SSL = "ldap.ssl";

    private static final String[] REQUIRED_OPTIONS = new String[] { OPTION_AUTHENTICATION, OPTION_CONNECTION_URL,
                                                                   OPTION_USER_BASE, OPTION_USER_PATTERN };

    private LdapConfig m_cfg = null;

    public boolean login() throws LoginException
    {
        // Initialize the LDAP configuration
        m_cfg = LdapConfig.getInstance( null, m_options, REQUIRED_OPTIONS );

        // Retrieve the essential callbacks: username and password
        String username;
        String loginId;
        String password;
        NameCallback ncb = new NameCallback( "User name" );
        PasswordCallback pcb = new PasswordCallback( "Password", false );
        Callback[] callbacks = new Callback[] { ncb, pcb };
        try
        {
            m_handler.handle( callbacks );
            String userPattern = m_cfg.loginIdPattern;
            username = ncb.getName();
            loginId = userPattern.replace( "{0}", username );
            password = new String( pcb.getPassword() );
        }
        catch( UnsupportedCallbackException e )
        {
            String message = "Unable to handle callback; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
        catch( IOException e )
        {
            String message = "IO exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }

        // Create LDAP context and log in!
        try
        {
            // Log in
            Hashtable<String, String> env = m_cfg.newJndiEnvironment( loginId, password );
            DirContext ctx = new InitialLdapContext( env, null );
            if( log.isDebugEnabled() )
            {
                log.debug( "Logged in user " + username + " with LDAP DN " + loginId );
            }
            m_principals.add( new WikiPrincipal( username, WikiPrincipal.LOGIN_NAME ) );

            // Go and get the DN and common name (and make the wiki name out of
            // it)
            Principal[] principals = extractNamePrincipals( ctx, username );
            for( Principal principal : principals )
            {
                m_principals.add( principal );
            }
            return true;
        }
        catch( AuthenticationException e )
        {
            String message = I18N.get( InternationalizationManager.CORE_BUNDLE, m_locale, "login.error.password" );
            throw new FailedLoginException( message );
        }
        catch( NamingException e )
        {
            String message = "Naming exception; disallowing login.";
            log.error( message, e );
            throw new LoginException( message );
        }
    }

    /**
     * Looks up the user in the user base and returns an array of WikiPrincipals
     * representing the full name and wiki name. The full
     * name will be equal to the user object's first name (givenName) + last
     * name (sn) attributes, separated by a space, <em>or</em> the user object's
     * common name (cn) attribute. The wiki name is simply the full name with
     * all spaces removed. If no user is found this method will return a
     * zero-length array.
     * 
     * @param ctx the previously initialized LDAP context
     * @param username the user name
     * @return the principals
     * @throws NamingException if anything goes wrong for any reason
     */
    protected Principal[] extractNamePrincipals( DirContext ctx, String username ) throws NamingException
    {
        // Find the user
        String dn = m_cfg.getUserDn( username );
        Attributes attributes = ctx.getAttributes( dn );
        if( attributes != null )
        {
            // Build the wiki principals
            // FIXME: This should be sanitized better.
            Principal[] principals = new Principal[2];
            String fullName = LdapConfig.getFullName( attributes );
            String wikiName = fullName.indexOf( ' ' ) == -1 ? fullName : fullName.replace( " ", "" );
            principals[0] = new WikiPrincipal( fullName, WikiPrincipal.FULL_NAME );
            principals[1] = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
            return principals;
        }

        return NO_PRINCIPALS;
    }

}
