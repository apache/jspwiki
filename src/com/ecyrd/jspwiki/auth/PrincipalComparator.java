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
package com.ecyrd.jspwiki.auth;

import java.io.Serializable;
import java.security.Principal;
import java.text.Collator;
import java.util.Comparator;

/**
 * Comparator class for sorting objects of type Principal.
 * Used for sorting arrays or collections of Principals.
 * @since 2.3
 */
public class PrincipalComparator
    implements Comparator<Principal>, Serializable 
{
    private static final long serialVersionUID = 1L;

    /**
     * Compares two Principal objects.
     * @param o1 the first Principal
     * @param o2 the second Principal
     * @return the result of the comparison
     * @see java.util.Comparator#compare(Object, Object)
     */
    public int compare( Principal o1, Principal o2 )
    {
        Collator collator = Collator.getInstance();
        return collator.compare( o1.getName(), o2.getName() );
    }

}
