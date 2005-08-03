package com.ecyrd.jspwiki.auth.user;

import java.util.Date;

/**
 * Class for representing wiki user information, such as the login name, full
 * name, wiki name, and e-mail address.
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-08-03 03:54:07 $
 * @since 2.3
 */
public interface UserProfile
{

    public boolean equals( Object o );

    /**
     * Returns the creation date.
     * @return the creation date
     */
    public Date getCreated();
    
    /**
     * Returns the user's e-mail address.
     * @return the e-mail address
     */
    public String getEmail();

    /**
     * Returns the user's full name.
     * @return the full name
     */
    public String getFullname();
    
    /**
     * Returns the last-modified date.
     * @return the date and time of last modification
     */
    public Date getLastModified();

    /**
     * Returns the user's login name.
     * @return the login name
     */
    public String getLoginName();

    /**
     * Returns the user password for use with custom authentication. Note that
     * the password field is not meaningful for container authentication; the
     * user's private credentials are generally stored elsewhere. While it
     * depends on the {@link UserDatabase}implementation, in most cases the
     * value returned by this method will be a password hash, not the password
     * itself.
     * @return the password
     */
    public String getPassword();

    /**
     * Returns the user's wiki name.
     * @return the wiki name.
     */
    public String getWikiName();

    /**
     * Returns <code>true</code> if the profile has never been 
     * saved before. Implementing classes might check the
     * last modified date, for example, to determine this.
     */
    public boolean isNew();
    
    /**
     * Sets the created date.
     * @param date the creation date
     */
    public void setCreated( Date date );
    
    /**
     * Sets the user's e-mail address.
     * @param email the e-mail address
     */
    public void setEmail( String email );

    /**
     * Sets the user's full name. For example, "Janne Jalkanen."
     * @param arg the full name
     */
    public void setFullname( String arg );

    /**
     * Sets the last-modified date
     * @param date the last-modified date
     */
    public void setLastModified( Date date );
    
    /**
     * Sets the name by which the user logs in. The login name is used as the
     * username for custom authentication (see
     * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#loginCustom(String, String, HttpServletRequest)}.
     * {@link com.ecyrd.jspwiki.auth.login.UserDatabaseLoginModule}). The login
     * name is typically a short name ("jannej"). In contrast, the wiki name is
     * typically of type FirstnameLastName ("JanneJalkanen").
     * @param name the login name
     */
    public void setLoginName( String name );

    /**
     * Sets the user's password for use with custom authentication. It is
     * <em>not</em> the responsibility of implementing classes to hash the
     * password; that responsibility is borne by the UserDatabase implementation
     * during save operations (see {@link UserDatabase#save(UserProfile)}).
     * Note that the password field is not meaningful for container
     * authentication; the user's private credentials are generally stored
     * elsewhere.
     * @param arg the password
     */
    public void setPassword( String arg );

    /**
     * Sets the user's wiki name. This is typically of type FirstnameLastName
     * ("JanneJalkanen").
     * @param name the wiki name
     */
    public void setWikiName( String name );

    /**
     * Returns a string representation of this user profile.
     * @return the string
     */
    public String toString();
}