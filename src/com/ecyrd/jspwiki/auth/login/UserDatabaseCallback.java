package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;

import com.ecyrd.jspwiki.auth.user.UserDatabase;

/**
 * Callback for requesting and supplying a wiki UserDatabase. This callback is
 * used by LoginModules that need access to a user database for looking up users
 * by id.
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class UserDatabaseCallback implements Callback
{

    private UserDatabase m_database;

    /**
     * Returns the user database object. LoginModules call this method after a
     * CallbackHandler sets the user database.
     * @return the user database
     */
    public UserDatabase getUserDatabase()
    {
        return m_database;
    }

    /**
     * Sets the user database. CallbackHandler objects call this method..
     * @param database the user database
     */
    public void setUserDatabase( UserDatabase database )
    {
        this.m_database = database;
    }

}