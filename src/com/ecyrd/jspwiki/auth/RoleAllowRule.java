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

import com.ecyrd.jspwiki.UserProfile;

/**
 * RoleAllowRule is an AccessRule that returns a positive if
 * the user has the specified role. 
 */
public class RoleAllowRule
    extends AccessRule
{

    private String m_allowedRole;

    public RoleAllowRule( String allowedRole )
    {
        m_allowedRole = allowedRole;
    }

    /**
     * If the WikiUserPrincipal contains this object's role, 
     * returns PERMIT, otherwise returns CONTINUE.
     */
    public int evaluate( UserProfile wup )
    {
        if( wup != null && 
            m_allowedRole != null &&
            wup.hasRole( m_allowedRole ) )
        {
            return( ALLOW );
        }
        
        return( CONTINUE );
    }

    public String toString()
    {
        return( "[allow role " + m_allowedRole + "]" );
    }
}



