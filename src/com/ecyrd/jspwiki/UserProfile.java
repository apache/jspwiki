/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.util.StringTokenizer;

/**
 *  Contains user profile information.
 *
 *  @author Janne Jalkanen
 *  @since  1.7.2
 */
// FIXME: contains magic strings.
public class UserProfile
{
    private String m_userName;

    public UserProfile()
    {
    }

    public UserProfile( String representation )
    {
        parseStringRepresentation( representation );
    }

    public String getStringRepresentation()
    {
        return "username="+m_userName;
    }

    public String getName()
    {
        return m_userName;
    }

    public void setName( String name )
    {
        m_userName = name;
    }

    public void parseStringRepresentation( String res )
    {
        if( res != null )
        {
            StringTokenizer tok = new StringTokenizer( res, " ,=" );

            while( tok.hasMoreTokens() )
            {
                String param = tok.nextToken();
                String value = tok.nextToken();
                    
                if( param.equals("username") )
                {
                    m_userName = value;
                }
            }
        }
    }
}
