/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.util.Properties;
import java.util.List;
import java.security.Principal;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  Defines an interface for grouping users to groups, etc.
 *  @author Janne Jalkanen
 *  @since 2.2.
 */
public interface UserDatabase
{
    /**
     * Initializes the WikiPrincipalist based on values from a Properties
     * object.
     */
    public void initialize( WikiEngine engine, Properties props )
        throws NoRequiredPropertyException;

    /**
     *  Returns a list of WikiGroup objects for the given Principal.
     */
    public List getGroupsForPrincipal( Principal p )
        throws NoSuchPrincipalException;

    /**
     *  Creates a principal.  This method should return either a WikiGroup
     *  or a UserProfile (or a subclass, if you need them for your own
     *  usage. 
     *  <p>
     *  It is the responsibility of the UserDatabase to implement appropriate
     *  caching of UserProfiles and other principals.
     *  <p>
     *  Yes, all this means that user names and user groups do actually live
     *  in the same namespace.
     *  <p>
     *  FIXME: UserDatabase currently requires that getPrincipal() never return null.
     */
    public WikiPrincipal getPrincipal( String name );
}
