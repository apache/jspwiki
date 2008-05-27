/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.providers;

import com.ecyrd.jspwiki.WikiException;

/**
 *  This exception represents the superclass of all exceptions that providers
 *  may throw.  It is okay to throw it in case you cannot use any of
 *  the specific subclasses, in which case the page loading is
 *  considered to be broken, and the user is notified.
 */
public class ProviderException
    extends WikiException
{
    private static final long serialVersionUID = 0L;
    
    public ProviderException( String msg )
    {
        super( msg );
    }
}
