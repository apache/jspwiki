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
package org.apache.wiki.auth.user;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.WikiSession;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation for representing wiki user information, such as the
 * login name, full name, wiki name, and e-mail address.
 * @since 2.3
 */

public final class DefaultUserProfile implements UserProfile
{
    private static final long serialVersionUID = -5600466893735300647L;

    private static final String EMPTY_STRING = "";

    private static final String WHITESPACE = "\\s";
    
    private Map<String,Serializable> attributes = new HashMap<>();

    private Date     created   = null;

    private String   email     = null;

    private String   fullname  = null;
    
    private Date lockExpiry = null;

    private String   loginName = null;

    private Date     modified  = null;

    private String   password  = null;
    
    private String uid = null;

    private String   wikiname  = null;

    /**
     * Private constructor to prevent direct instantiation.
     */
    private DefaultUserProfile() {}

    /**
     * Static factory method that creates a new DefaultUserProfile
     * and sets a unique identifier (uid) for the supplied UserDatabase.
     * @param db the UserDatabase for which the uid should be
     * created
     * @return the new profile
     */
    protected static UserProfile newProfile( UserDatabase db )
    {
        UserProfile profile = new DefaultUserProfile();
        profile.setUid( AbstractUserDatabase.generateUid( db ) );
        return profile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object o )
    {
        if ( ( o != null ) && ( o instanceof UserProfile ) )
        {
            DefaultUserProfile u = (DefaultUserProfile) o;
            return  same( fullname, u.fullname ) && same( password, u.password )
                    && same( loginName, u.loginName ) && same(StringUtils.lowerCase( email ), StringUtils.lowerCase( u.email ) ) && same( wikiname,
                    u.wikiname );
        }

        return false;
    }

    @Override
    public int hashCode()
    {
        return (fullname  != null ? fullname.hashCode()  : 0) ^
               (password  != null ? password.hashCode()  : 0) ^
               (loginName != null ? loginName.hashCode() : 0) ^
               (wikiname  != null ? wikiname.hashCode()  : 0) ^
               (email     != null ? StringUtils.lowerCase( email ).hashCode()     : 0);
    }

    /**
     * Returns the creation date
     * @return the creation date
     * @see org.apache.wiki.auth.user.UserProfile#getCreated()
     */
    @Override
    public Date getCreated()
    {
        return created;
    }

    /**
     * Returns the user's e-mail address.
     * @return the e-mail address
     */
    @Override
    public String getEmail()
    {
        return email;
    }

    /**
     * Returns the user's full name.
     * @return the full name
     */
    @Override
    public String getFullname()
    {
        return fullname;
    }

    /**
     * Returns the last-modified date.
     * @return the last-modified date
     * @see org.apache.wiki.auth.user.UserProfile#getLastModified()
     */
    @Override
    public Date getLastModified()
    {
        return modified;
    }

    /**
     * Returns the user's login name.
     * @return the login name
     */
    @Override
    public String getLoginName()
    {
        return loginName;
    }

    /**
     * Returns the user password for use with custom authentication. Note that
     * the password field is not meaningful for container authentication; the
     * user's private credentials are generally stored elsewhere. While it
     * depends on the {@link UserDatabase}implementation, in most cases the
     * value returned by this method will be a password hash, not the password
     * itself.
     * @return the password
     */
    @Override
    public String getPassword()
    {
        return password;
    }

    /**
     * Returns the user's wiki name.
     * @return the wiki name.
     */
    @Override
    public String getWikiName()
    {
        return wikiname;
    }

    /**
     * Returns <code>true</code> if the user profile is
     * new. This implementation checks whether
     * {@link #getLastModified()} returns <code>null</code>
     * to determine the status.
     * @see org.apache.wiki.auth.user.UserProfile#isNew()
     */
    @Override
    public boolean isNew()
    {
        return  modified == null;
    }

    /**
     * @param date the creation date
     * @see org.apache.wiki.auth.user.UserProfile#setCreated(java.util.Date)
     */
    @Override
    public void setCreated(Date date)
    {
        created = date;
    }

    /**
     * Sets the user's e-mail address.
     * @param email the e-mail address
     */
    @Override
    public void setEmail( String email )
    {
    	this.email = email;
    }

    /**
     * Sets the user's full name. For example, "Janne Jalkanen."
     * @param arg the full name
     */
    @Override
    public void setFullname( String arg )
    {
        fullname = arg;

        // Compute wiki name
        if ( fullname != null )
        {
            wikiname = fullname.replaceAll(WHITESPACE, EMPTY_STRING);
        }
    }

    /**
     * Sets the last-modified date.
     * @param date the last-modified date
     * @see org.apache.wiki.auth.user.UserProfile#setLastModified(java.util.Date)
     */
    @Override
    public void setLastModified( Date date )
    {
        modified = date;
    }

    /**
     * Sets the name by which the user logs in. The login name is used as the
     * username for custom authentication (see
     * {@link org.apache.wiki.auth.AuthenticationManager#login(WikiSession,HttpServletRequest, String, String)}).
     * The login name is typically a short name ("jannej"). In contrast, the
     * wiki name is typically of type FirstnameLastName ("JanneJalkanen").
     * @param name the login name
     */
    @Override
    public void setLoginName( String name )
    {
        loginName = name;
    }

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
    @Override
    public void setPassword( String arg )
    {
        password = arg;
    }

    /**
     * Returns a string representation of this user profile.
     * @return the string
     */
    @Override
    public String toString()
    {
        return "[DefaultUserProfile: '" + getFullname() + "']";
    }

    /**
     * Private method that compares two objects and determines whether they are
     * equal. Two nulls are considered equal.
     * @param arg1 the first object
     * @param arg2 the second object
     * @return the result of the comparison
     */
    private boolean same( Object arg1, Object arg2 )
    {
        if ( arg1 == null && arg2 == null )
        {
            return true;
        }
        if ( arg1 == null || arg2 == null )
        {
            return false;
        }
        return arg1.equals( arg2 );
    }

    //--------------------------- Attribute and lock interface implementations ---------------------------
    
    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String,Serializable> getAttributes()
    {
        return attributes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Date getLockExpiry()
    {
        return isLocked() ? lockExpiry : null;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getUid()
    {
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLocked()
    {
        boolean locked =  lockExpiry != null && System.currentTimeMillis() < lockExpiry.getTime();

        // Clear the lock if it's expired already
        if ( !locked && lockExpiry != null )
        {
            lockExpiry = null;
        }
        return locked;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLockExpiry( Date expiry )
    {
    	this.lockExpiry = expiry;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setUid( String uid )
    {
        this.uid = uid;
    }
}
