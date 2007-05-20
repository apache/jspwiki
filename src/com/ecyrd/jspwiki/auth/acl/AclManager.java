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
package com.ecyrd.jspwiki.auth.acl;

import java.util.Properties;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.WikiSecurityException;

/**
 *  Specifies how to parse and return ACLs from wiki pages.
 *  @author Andrew Jaquith
 *  @since 2.3
 */
public interface AclManager
{

    /**
     * Initializes the AclManager with a supplied wiki engine and properties.
     * @param engine the wiki engine
     * @param props the initialization properties
     */
    public void initialize( WikiEngine engine, Properties props );

    /**
     * A helper method for parsing textual AccessControlLists. The line is in
     * form "(ALLOW) <permission><principal>, <principal>, <principal>". This
     * method was moved from Authorizer.
     * @param page The current wiki page. If the page already has an ACL, it
     *            will be used as a basis for this ACL in order to avoid the
     *            creation of a new one.
     * @param ruleLine The rule line, as described above.
     * @return A valid Access Control List. May be empty.
     * @throws WikiSecurityException, if the ruleLine was faulty somehow.
     * @since 2.1.121
     */
    public Acl parseAcl( WikiPage page, String ruleLine ) throws WikiSecurityException;

    /**
     * Returns the access control list for the page.
     * If the ACL has not been parsed yet, it is done
     * on-the-fly. If the page has a parent page, then that is tried also.
     * This method was moved from Authorizer;
     * it was consolidated with some code from AuthorizationManager.
     * @param page
     * @since 2.2.121
     * @return the Acl representing permissions for the page
     */
    public Acl getPermissions( WikiPage page );

    /**
     * Sets the access control list for the page and persists it.
     * @param page the wiki page
     * @param acl the access control list
     * @since 2.5
     * @throws WikiSecurityException
     */
    public void setPermissions( WikiPage page, Acl acl ) throws WikiSecurityException;
}
