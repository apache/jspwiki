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
 * Implementing classes must be able to query the
 * WikiUserPrincipal and determine whether access 
 * is permitted. 
 */
public abstract class AccessRule
{
    public static final int ALLOW    = 1;
    public static final int DENY     = -1;
    public static final int CONTINUE = 0;

    /**
     * Implementing classes evaluate their custom rule and
     * return ALLOW, DENY, or CONTINUE depending on whether
     * the result is a positive, negative, or non-committal
     * match.
     */
    public abstract int evaluate( UserProfile wup );
}




