/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.security.Principal;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.TextUtil;

/**
 *  Contains user profile information.
 *
 *  @author Janne Jalkanen
 *  @since  1.7.2
 */
// FIXME: contains magic strings.
public class UserProfile
    extends WikiPrincipal
{
    private Category log = Category.getInstance( UserProfile.class );

    private int m_loginStatus = NONE;

    public static final int NONE      = 0;
    public static final int COOKIE    = 1;
    public static final int CONTAINER = 2;  // Container has done auth for us.
    public static final int PASSWORD  = 3;


    private String m_password  = null;
    private String m_loginName = null;

    public UserProfile()
    {
    }

    /**
     *  The login name may be different from your WikiName.  The WikiName
     *  is typically of type FirstnameLastName (like JanneJalkanen), whereas
     *  the login name is typically a shorter one, such as "jannej" or something
     *  similar.
     */
    public void setLoginName( String name )
    {
        m_loginName = name;
    }

    /**
     *  Returns the login name.
     */

    public String getLoginName()
    {
        return m_loginName;
    }

    /**
     *  Returns true, if the user has been authenticated properly.
     */
    public boolean isAuthenticated()
    {
        return m_loginStatus >= CONTAINER;
    }

    /*
    public UserProfile( String representation )
    {
        parseStringRepresentation( representation );
    }
    */
    public String getStringRepresentation()
    {
        String res = "username="+TextUtil.urlEncodeUTF8(getName());

        return res;
    }

    public static UserProfile parseStringRepresentation( String res )
        throws NoSuchElementException
    {
        UserProfile prof = new UserProfile();

        if( res != null && res.length() > 0 )
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
                    prof.setName( value );
                }
            }
        }

        return prof;
    }

    public boolean equals( Object o )
    {
        // System.out.println(this+".equals("+o+")");
        if( (o != null) && (o instanceof UserProfile) )
        {
            String name = getName();

            if( name != null && name.equals( ((UserProfile)o).getName() ) )
            {
                return true;
            }
        }

        return false;
    }

    public int getLoginStatus()
    {
        return m_loginStatus;
    }

    public void setLoginStatus( int arg )
    {
        m_loginStatus = arg;
    }

    /**
     *  Returns the password that the user gave.  We store the password
     *  because some authenticators may need to reissue it at periodical
     *  intervals; or possibly use the same password to multiple services.
     */
    public String getPassword()
    {
        return m_password;
    }

    public void setPassword( String arg )
    {
        m_password = arg;
    }

    public String toString()
    {
        return "[UserProfile: '"+getName()+"']";
    }
}
