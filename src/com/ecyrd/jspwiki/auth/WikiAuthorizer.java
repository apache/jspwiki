/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.auth;


import java.util.Properties;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.UserProfile;



/**
 * WikiAuthorizer defines the functionalities required of pluggable
 * authorization classes. 
 */
public interface WikiAuthorizer
{
    /**
     * Initializes the WikiAuthorizer based on values from a Properties
     * object.
     */
    public void initialize( Properties props )
        throws NoRequiredPropertyException;
    
    /**
     * Loads the user's roles and permissions from the Authorizer's
     * storage and sets them in the WikiUserPrincipal.
     */
    public void loadPermissions( UserProfile wup );

    /**
     * Explicitly adds a given role to a UserProfile.
     * Fetches the corresponding permissions and adds them, as well.
     */
    public void addRole( UserProfile wup, String roleName );

    /**
     * Explicitly adds a given permission to a UserProfile.
     */
    public void addPermission( UserProfile wup, String permName );

    /**
     * Returns the default permissions of WikiPages.
     * If none exist, returns an empty AccessRuleSet.
     */
    public AccessRuleSet getDefaultPermissions();
    
}



