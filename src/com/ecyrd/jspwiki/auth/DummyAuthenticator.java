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


import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

/**
 * DummyAuthenticator is a trivial WikiAuthenticator that accepts any
 * user name without a password. The WikiUserPrincipal is never marked
 * as valid.
 */
public class DummyAuthenticator
    implements WikiAuthenticator
{

    static Category log = Category.getInstance( DummyAuthenticator.class );

    public DummyAuthenticator()
    {
    }

    /**
     * Dummy initialization does nothing.
     */
    public void initialize( Properties props )
        throws NoRequiredPropertyException
    {
        log.info( "Using dummy authentication. Everyone can log in." );
    }
    
    /**
     * Dummy authentication always validates the WikiUserPrincipal
     * and returns true, unless a null object is offered.
     */
    public boolean authenticate( UserProfile wup )
    {
        if( wup == null )
            return( false );

        wup.setStatus( UserProfile.VALIDATED );
        return( true );
    }
}



