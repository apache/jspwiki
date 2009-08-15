package org.apache.wiki.auth.user;

import java.security.Principal;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.InitialLdapContext;

import org.apache.wiki.NoRequiredPropertyException;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.*;
import org.apache.wiki.util.TextUtil;

public class LdapUserDatabase extends AbstractUserDatabase
{
    /**
     * LdapUserDatabase does not support this operation.
     */
    public void deleteByLoginName( String loginName ) throws NoSuchPrincipalException, WikiSecurityException
    {
        throw new WikiSecurityException( "Operation not supported" );
    }

    public UserProfile findByEmail( String index ) throws NoSuchPrincipalException
    {
        index = sanitize( index );
        return findLdapUser( "(&(objectClass=" + m_cfg.userObjectClass + ")(mail=" + index + "))" );
    }
    
    private UserProfile findLdapUser( String filter ) throws NoSuchPrincipalException
    {
        Hashtable<String, String> env = m_cfg.newJndiEnvironment( null, null );
        try
        {
            DirContext ctx = new InitialLdapContext( env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[] { m_cfg.userLoginNameAttribute, "cn", "givenName", "sn", "mail" } );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration<SearchResult> results = ctx.search( m_cfg.userBase, filter, searchControls );
            if( results.hasMore() )
            {
                SearchResult user = results.next();
                Attributes attributes = user.getAttributes();
                Attribute loginName = attributes.get( m_cfg.userLoginNameAttribute );
                Attribute cn = attributes.get( "cn" );
                Attribute mail = attributes.get( "mail" );
                if ( loginName == null || cn == null || mail == null )
                {
                    throw new NamingException( "Malformed directory entry; missing cn, mail or uid." );
                }
                UserProfile p = newProfile();
                p.setUid( user.getNameInNamespace() );
                p.setLoginName( loginName.get( 0 ).toString() );
                p.setFullname( LdapConfig.getFullName( user.getAttributes() ) );
                p.setEmail( mail.get( 0 ).toString() );
                return p;
            }
        }
        catch( NamingException e )
        {
            throw new NoSuchPrincipalException( "Could not find object: " + e.getMessage() );
        }
        throw new NoSuchPrincipalException( "Could not find object." );
    }

    public UserProfile findByFullName( String index ) throws NoSuchPrincipalException
    {
        index = sanitize( index );
        return findLdapUser( "(&(objectClass=" + m_cfg.userObjectClass + ")(cn=" + index + "))" );
    }

    public UserProfile findByLoginName( String index ) throws NoSuchPrincipalException
    {
        index = sanitize( index );
        return findLdapUser( "(&(objectClass=" + m_cfg.userObjectClass + ")(" + m_cfg.userLoginNameAttribute + "=" + index + "))" );
    }

    public UserProfile findByUid( String uid ) throws NoSuchPrincipalException
    {
        String filter = "(objectClass=" + m_cfg.userObjectClass + ")";
        Hashtable<String, String> env = m_cfg.newJndiEnvironment( null, null );
        try
        {
            DirContext ctx = new InitialLdapContext( env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[] { m_cfg.userLoginNameAttribute, "cn", "givenName", "sn", "mail" } );
            searchControls.setSearchScope( SearchControls.SUBTREE_SCOPE );
            NamingEnumeration<SearchResult> results = ctx.search( uid, filter, searchControls );
            if( results.hasMore() )
            {
                SearchResult user = results.next();
                Attributes attributes = user.getAttributes();
                Attribute loginName = attributes.get( m_cfg.userLoginNameAttribute );
                Attribute cn = attributes.get( "cn" );
                Attribute mail = attributes.get( "mail" );
                if ( loginName == null || cn == null || mail == null )
                {
                    throw new NamingException( "Malformed directory entry; missing cn, mail or uid." );
                }
                UserProfile p = newProfile();
                p.setUid( user.getNameInNamespace() );
                p.setLoginName( loginName.get( 0 ).toString() );
                p.setFullname( LdapConfig.getFullName( user.getAttributes() ) );
                p.setEmail( mail.get( 0 ).toString() );
                return p;
            }
        }
        catch( NamingException e )
        {
            throw new NoSuchPrincipalException( "Could not find object: " + e.getMessage() );
        }
        throw new NoSuchPrincipalException( "Could not find object." );
    }

    public UserProfile findByWikiName( String index ) throws NoSuchPrincipalException
    {
        index = sanitize( index );
        index = TextUtil.beautifyString( index );
        return findLdapUser( "(&(objectClass=" + m_cfg.userObjectClass + ")(cn=" + index + "))" );
    }

    private String sanitize( String index )
    {
        index = index.replace( " ", " " );
        return index.replace( "=", "\\3D" );
    }
    
    public Principal[] getWikiNames() throws WikiSecurityException
    {
        String filter = "(objectClass=" + m_cfg.userObjectClass + ")";
        Hashtable<String, String> env = m_cfg.newJndiEnvironment( null, null );
        Set<Principal> principals = new HashSet<Principal>();
        try
        {
            DirContext ctx = new InitialLdapContext( env, null );
            SearchControls searchControls = new SearchControls();
            searchControls.setReturningAttributes( new String[]{ "cn", "givenName", "sn" } );
            NamingEnumeration<SearchResult> results = ctx.search( m_cfg.userBase, filter, searchControls );
            while( results.hasMore() )
            {
                SearchResult user = results.next();
                String fullName = LdapConfig.getFullName( user.getAttributes() );
                String wikiName = fullName.indexOf( ' ' ) == -1 ? fullName : fullName.replace( " ", "" );
                principals.add( new WikiPrincipal( wikiName, WikiPrincipal.WIKI_NAME ) );
            }
        }
        catch( NamingException e )
        {
            throw new WikiSecurityException( "Could not find object: " + e.getMessage(), e );
        }
        return principals.toArray(new Principal[principals.size()]);
    }

    private LdapConfig m_cfg = null;

    private static final String[] REQUIRED_PROPERTIES = new String[] { LdapConfig.PROPERTY_CONNECTION_URL,
                                                                       LdapConfig.PROPERTY_USER_BASE };

    public void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException
    {
        m_cfg = LdapConfig.getInstance( null, props, REQUIRED_PROPERTIES );
    }

    /**
     * LdapUserDatabase does not support this operation.
     */
    public void rename( String loginName, String newName )
                                                          throws NoSuchPrincipalException,
                                                              DuplicateUserException,
                                                              WikiSecurityException
    {
        throw new WikiSecurityException( "Operation not supported" );
    }

    /**
     * LdapUserDatabase does not support this operation.
     */
    public void save( UserProfile profile ) throws WikiSecurityException
    {
        throw new WikiSecurityException( "Operation not supported" );
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation validates the password by binding to the LDAP server
     * as the user. The value of <code>loginName</code> is substituted into the
     * <code>{0}</code> pattern specified by property
     * {@link LdapConfig#PROPERTY_LOGIN_ID_PATTERN}.
     * </p>
     */
    public boolean validatePassword( String loginName, String password )
    {
        String userPattern = m_cfg.loginIdPattern;
        String username = userPattern.replace( "{0}", loginName );

        Hashtable<String, String> env = m_cfg.newJndiEnvironment( username, password );
        try
        {
            new InitialLdapContext( env, null );
            return true;
        }
        catch( NamingException e )
        {
        }
        return false;
    }

}
