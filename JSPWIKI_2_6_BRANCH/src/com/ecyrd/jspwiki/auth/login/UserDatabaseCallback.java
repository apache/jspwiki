/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.auth.login;

import javax.security.auth.callback.Callback;

import com.ecyrd.jspwiki.auth.user.UserDatabase;

/**
 * Callback for requesting and supplying a wiki UserDatabase. This callback is
 * used by LoginModules that need access to a user database for looking up users
 * by id.
 * @author Andrew Jaquith
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