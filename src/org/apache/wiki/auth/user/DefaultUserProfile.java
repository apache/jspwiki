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
package org.apache.wiki.auth.user;

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
    
    private Map<String,Serializable> m_attributes = new HashMap<String,Serializable>();

    private Date     m_created   = null;

    private String   m_email     = null;

    private String   m_fullname  = null;
    
    private Date m_lockExpiry = null;

    private String   m_loginName = null;

    private Date     m_modified  = null;

    private String   m_password  = null;
    
    private String m_uid = null;

    private String   m_wikiname  = null;

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
    public boolean equals( Object o )
    {
        if ( ( o != null ) && ( o instanceof UserProfile ) )
        {
            DefaultUserProfile u = (DefaultUserProfile) o;
            return  same( m_fullname, u.m_fullname ) && same( m_password, u.m_password )
                    && same( m_loginName, u.m_loginName ) && same( m_email, u.m_email ) && same( m_wikiname,
                    u.m_wikiname );
        }

        return false;
    }

    public int hashCode()
    {
        return (m_fullname  != null ? m_fullname.hashCode()  : 0) ^
               (m_password  != null ? m_password.hashCode()  : 0) ^
               (m_loginName != null ? m_loginName.hashCode() : 0) ^
               (m_wikiname  != null ? m_wikiname.hashCode()  : 0) ^
               (m_email     != null ? m_email.hashCode()     : 0);
    }

    /**
     * Returns the creation date
     * @return the creation date
     * @see org.apache.wiki.auth.user.UserProfile#getCreated()
     */
    public Date getCreated()
    {
        return m_created;
    }

    /**
     * Returns the user's e-mail address.
     * @return the e-mail address
     */
    public String getEmail()
    {
        return m_email;
    }

    /**
     * Returns the user's full name.
     * @return the full name
     */
    public String getFullname()
    {
        return m_fullname;
    }

    /**
     * Returns the last-modified date.
     * @return the last-modified date
     * @see org.apache.wiki.auth.user.UserProfile#getLastModified()
     */
    public Date getLastModified()
    {
        return m_modified;
    }

    /**
     * Returns the user's login name.
     * @return the login name
     */
    public String getLoginName()
    {
        return m_loginName;
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
    public String getPassword()
    {
        return m_password;
    }

    /**
     * Returns the user's wiki name.
     * @return the wiki name.
     */
    public String getWikiName()
    {
        return m_wikiname;
    }

    /**
     * Returns <code>true</code> if the user profile is
     * new. This implementation checks whether
     * {@link #getLastModified()} returns <code>null</code>
     * to determine the status.
     * @see org.apache.wiki.auth.user.UserProfile#isNew()
     */
    public boolean isNew()
    {
        return  m_modified == null;
    }

    /**
     * @param date the creation date
     * @see org.apache.wiki.auth.user.UserProfile#setCreated(java.util.Date)
     */
    public void setCreated(Date date)
    {
        m_created = date;
    }

    /**
     * Sets the user's e-mail address.
     * @param email the e-mail address
     */
    public void setEmail( String email )
    {
        m_email = email;
    }

    /**
     * Sets the user's full name. For example, "Janne Jalkanen."
     * @param arg the full name
     */
    public void setFullname( String arg )
    {
        m_fullname = arg;

        // Compute wiki name
        if ( m_fullname != null )
        {
            m_wikiname = m_fullname.replaceAll(WHITESPACE, EMPTY_STRING);
        }
    }

    /**
     * Sets the last-modified date.
     * @param date the last-modified date
     * @see org.apache.wiki.auth.user.UserProfile#setLastModified(java.util.Date)
     */
    public void setLastModified( Date date )
    {
        m_modified = date;
    }

    /**
     * Sets the name by which the user logs in. The login name is used as the
     * username for custom authentication (see
     * {@link org.apache.wiki.auth.AuthenticationManager#login(WikiSession,HttpServletRequest, String, String)}).
     * The login name is typically a short name ("jannej"). In contrast, the
     * wiki name is typically of type FirstnameLastName ("JanneJalkanen").
     * @param name the login name
     */
    public void setLoginName( String name )
    {
        m_loginName = name;
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
    public void setPassword( String arg )
    {
        m_password = arg;
    }

    /**
     * No-op method. In previous versions of JSPWiki, the method
     * set the user's wiki name directly. Now, the wiki name is automatically
     * calculated based on the full name.
     * @param name the wiki name
     * @deprecated This method will be removed in a future release.
     */
    @SuppressWarnings("deprecation")
    public void setWikiName( String name )
    {
    }

    /**
     * Returns a string representation of this user profile.
     * @return the string
     */
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
    public Map<String,Serializable> getAttributes()
    {
        return m_attributes;
    }

    /**
     * {@inheritDoc}
     */
    public Date getLockExpiry()
    {
        return isLocked() ? m_lockExpiry : null;
    }
    
    /**
     * {@inheritDoc}
     */
    public String getUid()
    {
        return m_uid;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLocked()
    {
        boolean locked =  m_lockExpiry != null && System.currentTimeMillis() < m_lockExpiry.getTime();

        // Clear the lock if it's expired already
        if ( !locked && m_lockExpiry != null )
        {
            m_lockExpiry = null;
        }
        return locked;
    }

    /**
     * {@inheritDoc}
     */
    public void setLockExpiry( Date expiry )
    {
        m_lockExpiry = expiry;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setUid( String uid )
    {
        m_uid = uid;
    }
}
