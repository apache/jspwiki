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
import java.util.*;

import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import org.apache.wiki.auth.WikiPrincipal;
import org.apache.wiki.auth.authorize.Role;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.util.TextUtil;

/**
 * <p>
 * LoginModule that authenticates users against an LDAP server with the user's
 * supplied credentials. The authentication is performed by binding to the LDAP
 * server using the user's credentials, with or without SSL. To use this
 * LoginModule, callers must initialize it with a JAAS options Map that supplies
 * the following key/value pairs:
 * </p>
 * <ul>
 * <li>{@link #OPTIONS_CONNECTION_URL} - the connection string for the LDAP
 * server, for example <code>ldap://ldap.jspwiki.org:389/</code></li>
 * <li>{@link #OPTIONS_USER_PATTERN} - the pattern used for forming the
 * distinguished name for the user. The user ID supplied during the login will
 * be substituted into the <code>{0}</code> token in this pattern, if it
 * contains one. For example, if the user pattern is
 * <code>uid={0},ou=people,dc=jspwiki,dc=org</code> and the user name supplied
 * during login is <code>fflintstone</code>, the DN that will be used for
 * authentication will be
 * <code>uid=fflintstone,ou=people,dc=jspwiki,dc=org</code></li>
 * <li>{@link #OPTIONS_SSL} - Optional parameter that specifies whether to use
 * SSL when connecting to the LDAP server. Values like <code>true</code> or
 * <code>on</code> indicate that SSL should be used. If this parameter is not
 * supplied, SSL will not be used.</li>
 * </ul>
 * <p>
 * If this LoginModule is used with a system-wide JAAS configuration (with or
 * without JSPWiki), the JAAS options can be specified as described above in the
 * JAAS configuration file. However, if this LoginModule is used with JSPWiki
 * custom authentication, the JAAS options map can be set in
 * <code>jspwiki.properties</code>. In this case, each option key must be
 * prefixed by <code>jspwiki.loginModule.options.</code> to indicate that the
 * values are, in fact, JAAS configuration items. Thus, the option
 * {@link #OPTIONS_CONNECTION_URL} would be configured in
 * <code>jspwiki.properties</code> using the key/value pair
 * <code>jspwiki.loginModule.options.{@value #OPTIONS_CONNECTION_URL}
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
     * JAAS option that supplies the connection URL for the LDAP server, e.g.
     * ldap://127.0.0.1:4890/
     */
    protected static final String OPTIONS_CONNECTION_URL = "ldap.connectionURL";

    /**
     * JAAS option that supplies the DN pattern for finding users, e.g.
     * uid={0},ou=people,dc=jspwiki,dc=org
     */
    protected static final String OPTIONS_USER_PATTERN = "ldap.userPattern";

    /**
     * JAAS option that indicates whether to use SSL for connecting to the LDAP
     * server.
     */
    protected static final String OPTIONS_SSL = "ldap.ssl";

    public boolean login() throws LoginException
    {
        // Retrieve the essential callbacks: username and password
        String username;
        String password;
        NameCallback ncb = new NameCallback( "User name" );
        PasswordCallback pcb = new PasswordCallback( "Password", false );
        Callback[] callbacks = new Callback[] { ncb, pcb };
        try
        {
            m_handler.handle( callbacks );
            username = ncb.getName();
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
            Hashtable<String, String> env = buildJndiEnvironment( username, password );
            String dn = env.get( Context.SECURITY_PRINCIPAL );
            DirContext ctx = new InitialLdapContext( env, null );

            // If login succeeds, commit the login principal
            m_principals.add( new WikiPrincipal( username, WikiPrincipal.LOGIN_NAME ) );
            if( log.isDebugEnabled() )
            {
                log.debug( "Logged in user " + username + " with LDAP DN " + dn );
            }

            // Also look up the full name (and make the wiki name out of it)
            Principal[] principals = extractNamePrincipals( ctx, dn );
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
     * Builds a JNDI environment hashtable for authenticating to the LDAP
     * server. The hashtable is built using information extracted from the
     * options map supplied to the LoginModule via
     * {@link #initialize(javax.security.auth.Subject, javax.security.auth.callback.CallbackHandler, Map, Map)}
     * . The options map supplies the LDAP connection URL and SSL handling flag;
     * the username and password parameters supply the LDAP credentials.
     * 
     * @param username the user's distinguished name (DN), used for
     *            authentication
     * @param password the password
     * @return the constructed hash table
     */
    private Hashtable<String, String> buildJndiEnvironment( String username, String password )
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put( Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory" );
        env.put( Context.SECURITY_AUTHENTICATION, "simple" );
        env.put( Context.SECURITY_CREDENTIALS, password );

        // LDAP server to search
        String option = (String) m_options.get( OPTIONS_CONNECTION_URL );
        if( option != null && option.trim().length() > 0 )
        {
            env.put( Context.PROVIDER_URL, option.trim() );
        }

        // DN pattern for finding users
        option = (String) m_options.get( OPTIONS_USER_PATTERN );
        if( option != null && option.trim().length() > 0 )
        {
            option = option.replace( "{0}", username ).trim();
            env.put( Context.SECURITY_PRINCIPAL, option );
        }

        // Use SSL?
        option = (String) m_options.get( OPTIONS_SSL );
        boolean ssl = TextUtil.isPositive( option );
        env.put( Context.SECURITY_PROTOCOL, ssl ? "ssl" : "none" );

        if( log.isDebugEnabled() )
        {
            log.debug( "Built JNDI environment for LDAP login.", env );
        }

        return env;
    }

    /**
     * Looks up the user at a supplied DN and returns an array of WikiPrincipals
     * whose values are equal to the node's common name (cn) attribute, and a
     * trimmed version without spaces. If no user is found this method will
     * return a zero-length array.
     * 
     * @param ctx the previously initialized LDAP context
     * @param dn the distinguished name
     * @return the principal
     * @throws NamingException if anything goes wrong for any reason
     */
    protected Principal[] extractNamePrincipals( DirContext ctx, String dn ) throws NamingException
    {
        // Look up the user in the current LDAP context
        Attributes attributes = ctx.getAttributes( dn, new String[] { "cn" } );
        Attribute attribute = attributes.get( "cn" );
        if( attribute != null && attribute.size() > 0 )
        {
            String fullName = attribute.get( 0 ).toString();
            Principal[] principals = new Principal[2];
            
            // FIXME: This should be sanitized better.
            String wikiName = fullName.indexOf( ' ' ) == -1 ? fullName : fullName.replace( " ", "" );
            
            principals[0] = new WikiPrincipal( fullName, WikiPrincipal.FULL_NAME );
            principals[1] = new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME );
            return principals;
        }
        return NO_PRINCIPALS;
    }

    protected Map<Role, Set<Principal>> getRoles( String ldapUrl )
    {
        String roleSearch = "ou=roles,dc=jspwiki,dc=org";
        Map<Role, Set<Principal>> roleMap = new HashMap<Role, Set<Principal>>();
        try
        {
            InitialContext iCtx = new InitialContext();
            DirContext ctx = (DirContext) iCtx.lookup( ldapUrl );
            SearchControls controls = new SearchControls();
            NamingEnumeration<SearchResult> ne = ctx.search( roleSearch, "(objectClass=groupOfUniqueNames)", controls );
            while ( ne.hasMore() )
            {
                SearchResult result = ne.next();
                Attributes attributes = result.getAttributes();

                // Get role name
                String role = attributes.get( "cn" ).get().toString();

                // Build role membership
                Attribute attribute = attributes.get( "uniqueMember" );
                Set<Principal> members = new HashSet<Principal>();
                if( attribute != null )
                {
                    for( int i = 0; i < attribute.size(); i++ )
                    {
                        Principal[] principals = extractNamePrincipals( ctx, attribute.get( i ).toString() );
                        for( Principal principal : principals )
                        {
                            members.add( principal );
                        }
                    }
                }
                roleMap.put( new Role( role ), members );
            }
        }
        catch( NamingException e )
        {
            e.printStackTrace();
        }
        return roleMap;
    }
}
