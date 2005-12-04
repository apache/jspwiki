package com.ecyrd.jspwiki.auth.user;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Properties;

import org.apache.catalina.util.HexUtils;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 * Abstract UserDatabase class that provides convenience methods for finding
 * profiles, building Principal collections and hashing passwords.
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-12-04 18:44:06 $
 * @since 2.3
 */
public abstract class AbstractUserDatabase implements UserDatabase
{
    
    protected static final Logger log = Logger.getLogger( AbstractUserDatabase.class );
    protected static final String SHA_PREFIX = "{SHA}";
    
    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#commit()
     */
    public abstract void commit() throws WikiSecurityException;

    /**
     * Looks up and returns the first {@link UserProfile}in the user database
     * that whose login name, full name, or wiki name matches the supplied
     * string. This method provides a "forgiving" search algorithm for resolving
     * principal names when the exact profile attribute that supplied the name
     * is unknown.
     * @param index the login name, full name, or wiki name
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#find(java.lang.String)
     */
    public UserProfile find( String index ) throws NoSuchPrincipalException
    {
        UserProfile profile = null;
        
        // Try finding by full name
        try {
            profile = findByFullName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }
        
        // Try finding by wiki name
        try {
            profile = findByWikiName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }
        
        // Try finding by login name
        try {
            profile = findByLoginName( index );
        }
        catch ( NoSuchPrincipalException e )
        {
        }
        if ( profile != null )
        {
            return profile;
        }
        
        throw new NoSuchPrincipalException( "Not in database: " + index );
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByEmail(java.lang.String)
     */
    public abstract UserProfile findByEmail( String index ) throws NoSuchPrincipalException;

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByFullName(java.lang.String)
     */
    public abstract UserProfile findByFullName( String index ) throws NoSuchPrincipalException;

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByLoginName(java.lang.String)
     */
    public abstract UserProfile findByLoginName( String index ) throws NoSuchPrincipalException;

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#findByWikiName(java.lang.String)
     */
    public abstract UserProfile findByWikiName( String index ) throws NoSuchPrincipalException;

    /**
     * <p>Looks up the Principals representing a user from the user database. These
     * are defined as a set of WikiPrincipals manufactured from the login name,
     * full name, and wiki name. If the user database does not contain a user
     * with the supplied identifier, throws a {@link NoSuchPrincipalException}.</p>
     * <p>When this method creates WikiPrincipals, the Principal containing
     * the user's full name is marked as containing the common name (see
     * {@link com.ecyrd.jspwiki.auth.WikiPrincipal#WikiPrincipal(String, String)}).
     * @param identifier the name of the principal to retrieve; this corresponds to
     *            value returned by the user profile's
     *            {@link UserProfile#getLoginName()}method.
     * @return the array of Principals representing the user
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#getPrincipals(java.lang.String)
     */
    public Principal[] getPrincipals( String identifier ) throws NoSuchPrincipalException
    {
        try
        {
            UserProfile profile = findByLoginName( identifier );
            ArrayList principals = new ArrayList();
            if ( profile.getLoginName() != null && profile.getLoginName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getLoginName(), WikiPrincipal.LOGIN_NAME ) );
            }
            if ( profile.getFullname() != null && profile.getFullname().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getFullname(), WikiPrincipal.FULL_NAME ) );
            }
            if ( profile.getWikiName() != null && profile.getWikiName().length() > 0 )
            {
                principals.add( new WikiPrincipal( profile.getWikiName(), WikiPrincipal.WIKI_NAME ) );
            }
            return (Principal[]) principals.toArray( new Principal[principals.size()] );
        }
        catch( NoSuchPrincipalException e )
        {
            throw e;
        }
    }

    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#initialize(com.ecyrd.jspwiki.WikiEngine, java.util.Properties)
     */
    public abstract void initialize( WikiEngine engine, Properties props ) throws NoRequiredPropertyException;

    /**
     * Factory method that instantiates a new DefaultUserProfile.
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#newProfile()
     */
    public UserProfile newProfile()
    {
        return new DefaultUserProfile();
    }
    
    /**
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#save(com.ecyrd.jspwiki.auth.user.UserProfile)
     */
    public abstract void save( UserProfile profile ) throws WikiSecurityException;
    
    /**
     * Validates the password for a given user. If the user does not exist in
     * the user database, this method always returns <code>false</code>. If
     * the user exists, the supplied password is compared to the stored
     * password. Note that if the stored password's value starts with
     * <code>{SHA}</code>, the supplied password is hashed prior to the
     * comparison.
     * @param loginName the user's login name
     * @param password the user's password (obtained from user input, e.g., a web form)
     * @return <code>true</code> if the supplied user password matches the
     * stored password
     * @see com.ecyrd.jspwiki.auth.user.UserDatabase#validatePassword(java.lang.String,
     *      java.lang.String)
     */
    public boolean validatePassword( String loginName, String password )
    {
        String hashedPassword = getHash( password );
        try
        {
            UserProfile profile = findByLoginName( loginName );
            String storedPassword = profile.getPassword();
            if ( storedPassword.startsWith( SHA_PREFIX ) )
            {
                storedPassword = storedPassword.substring( SHA_PREFIX.length() );
            }
            return ( hashedPassword.equals( storedPassword ) );
        }
        catch( NoSuchPrincipalException e )
        {
            return false;
        }
    }
    
    /**
     * Private method that calculates the SHA-1 hash of a given
     * <code>String</code>
     * @param text the text to hash
     * @return the result hash
     */
    protected String getHash( String text )
    {
        String hash = null;
        try
        {
            MessageDigest md = MessageDigest.getInstance( "SHA" );
            md.update( text.getBytes() );
            byte digestedBytes[] = md.digest();
            hash = HexUtils.convert( digestedBytes );
        }
        catch( NoSuchAlgorithmException e )
        {
            log.error( "Error creating SHA password hash:" + e.getMessage() );
            hash = text;
        }
        return hash;
    }
    
}
