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
import java.util.NoSuchElementException;
import org.apache.log4j.Category;

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

    private Category log = Category.getInstance( UserProfile.class );

    public UserProfile()
    {
    }

    public UserProfile( String representation )
    {
        parseStringRepresentation( representation );
    }

    public String getStringRepresentation()
    {
        String res = "username="+TextUtil.urlEncodeUTF8(m_userName);

        return res;
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
        try
        {
            if( res != null )
            {
                //
                //  Not all browsers or containers do proper cookie
                //  decoding, which is why we can suddenly get stuff
                //  like "username=3DJanneJalkanen", so we have to
                //  do the conversion here.
                //
                res = TextUtil.urlDecodeUTF8( res );
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
        catch( NoSuchElementException e )
        {
            log.warn("Missing or broken token in user profile: '"+res+"'");
        }
    }
}
