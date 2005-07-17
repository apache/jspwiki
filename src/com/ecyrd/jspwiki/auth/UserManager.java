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

import java.security.Principal;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.authorize.DefaultGroupManager;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;
import com.ecyrd.jspwiki.auth.user.DefaultUserProfile;
import com.ecyrd.jspwiki.auth.user.DuplicateUserException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;
import com.ecyrd.jspwiki.util.ClassUtil;

/**
 *  Provides a facade for user and group information.
 *  
 *  @author Janne Jalkanen
 *
 */
public class UserManager
{
    private WikiEngine m_engine;
    
    private static final Logger log = Logger.getLogger(UserManager.class);

    private static final String  PROP_USERDATABASE   = "jspwiki.userdatabase";
    
    // private static final String  PROP_ACLMANAGER     = "jspwiki.aclManager";

    /** The user database loads, manages and persists user identities */
    private UserDatabase     m_database     = null;
    
    /** The group manager loads, manages and persists wiki groups */
    private GroupManager     m_groupManager = null;
    
    /**
     *  Initializes the engine for its nefarious purposes.
     *  
     * @param engine
     * @param props
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
    }
    
    
    /**
     * Returns the GroupManager employed by this WikiEngine.
     * The GroupManager is lazily initialized.
     * @since 2.3
     */
    public GroupManager getGroupManager()
    {
        if( m_groupManager == null ) 
        {
            // TODO: make this pluginizable
            m_groupManager = new DefaultGroupManager();
            m_groupManager.initialize( m_engine, m_engine.getWikiProperties() );
        }
        return m_groupManager;
    }

    /**
     *  Returns the UserDatabase employed by this WikiEngine.
     *  The UserDatabase is lazily initialized by this method, if
     *  it does not exist yet.  If the initialization fails, this
     *  method will use the inner class DummyUserDatabase as
     *  a default (which is enough to get JSPWiki running).
     *  
     *  @since 2.3
     */
    
    // FIXME: Must not throw RuntimeException, but something else.
    public UserDatabase getUserDatabase()
    {
        if( m_database != null ) 
        {
            return m_database;
        }
        
        String dbClassName = "<unknown>";
        
        try
        {
            dbClassName = WikiEngine.getRequiredProperty( m_engine.getWikiProperties(), 
                                                          PROP_USERDATABASE );

            log.info("Attempting to load user database class "+dbClassName);
            Class dbClass = ClassUtil.findClass( "com.ecyrd.jspwiki.auth.user", dbClassName );
            m_database = (UserDatabase) dbClass.newInstance();
            m_database.initialize( m_engine, m_engine.getWikiProperties() );
            log.info("UserDatabase initialized.");
        }
        catch( NoRequiredPropertyException e )
        {
            log.error( "You have not set the '"+PROP_USERDATABASE+"'. You need to do this if you want to enable user management by JSPWiki." );
        }
        catch( ClassNotFoundException e )
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be found", e );
        }
        catch( InstantiationException e )
        {
            log.error( "UserDatabase class " + dbClassName + " cannot be created", e );
        }
        catch( IllegalAccessException e )
        {
            log.error( "You are not allowed to access this user database class", e );
        }
        finally
        {
            if( m_database == null )
            {
                log.info("I could not create a database object you specified (or didn't specify), so I am falling back to a default.");
                m_database = new DummyUserDatabase();
            }
        }
        
        return m_database;
    }

    /**
     *  This is a database that gets used if nothing else is available.  It does
     *  nothing of note - it just mostly thorws NoSuchPrincipalExceptions
     *  if someone tries to log in.
     *  
     *  @author jalkanen
     *
     */
    public class DummyUserDatabase implements UserDatabase
    {

        public void commit() throws WikiSecurityException
        {
            // No operation
        }

        public UserProfile find(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByEmail(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByFullName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByLoginName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public UserProfile findByWikiName(String index) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public Principal[] getPrincipals(String identifier) throws NoSuchPrincipalException
        {
            throw new NoSuchPrincipalException("No user profiles available");
        }

        public void initialize(WikiEngine engine, Properties props) throws NoRequiredPropertyException
        {
        }

        public UserProfile newProfile()
        {
            return new DefaultUserProfile();
        }

        public void save(UserProfile profile) throws WikiSecurityException, DuplicateUserException
        {
        }

        public boolean validatePassword(String loginName, String password)
        {
            return false;
        }
        
    }
}
