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
package com.ecyrd.jspwiki;

import java.util.HashMap;
import java.util.TreeSet;
import java.util.Iterator;
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
    /** Status of an unknown user. */
    public static final int UNKNOWN   = 0;
    /** Status of a user whose name is known, but no more. */
    public static final int NAMED     = 1;
    /** Status of a user who is registered and has been validated. */
    public static final int VALIDATED = 2;

    private String m_userName;
    private String m_passwd;
    private TreeSet m_roles       = new TreeSet();
    private TreeSet m_permissions = new TreeSet();
    private HashMap m_info        = new HashMap(4);
    private int m_status          = UNKNOWN;

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



    /**
     * Sets the password.
     */
    public void setPassword( String pwd )
    {
        // This really should obfuscate...
        m_passwd = pwd;
    }

    /**
     * Returns the password.
     */
    public String getPassword()
    {
        // This really should de-obfuscate...
        return( m_passwd );
    }

    /**
     * Returns true, if the user has the specified role.
     */
    public boolean hasRole( String roleName )
    {
        return( m_roles.contains( roleName ) );
    }

    /**
     * Adds a role to this user. Does NOT add the 
     * equivalent permissions; that is the responsibility
     * of an Authorizer implementation.
     */
    public void addRole( String role )
    {
        m_roles.add( role );
    }
    
    /**
     * Returns a list of roles the user has.
     */
    public Iterator getRoles()
    {
        return( m_roles.iterator() );
    }

    /**
     * Returns true if the user has the named permission.
     */
    public boolean hasPermission( String permission )
    {
        return( m_permissions.contains( permission ) );
    }

    /**
     * Adds a permission to this user.
     */
    public void addPermission( String permission )
    {
        m_permissions.add( permission );
    }

    /**
     * Returns a list of permissions the user has.
     */
    public Iterator getPermissions()
    {
        return( m_permissions.iterator() );
    }

    /**
     * Returns custom information stored by the given key.
     */
    public String getInfo( String key )
    {
        return( (String)m_info.get( key ) );
    }

    /**
     * Sets custom information stored by the given key.
     */
    public void setInfo( String key, String value )
    {
        if( key != null )
            m_info.put( key, value );
    }

    /**
     * Removes custom information stored by the key.
     */
    public void removeInfo( String key )
    {
        if( key != null )
            m_info.remove( key );
    }

    /**
     * Returns the status of this user. JSPWiki knows the following
     * statuses: UNKNOWN, NAMED, VALIDATED.
     */
    public int getStatus()
    {
        return( m_status );
    }

    /**
     * Sets the validity of this WUP.
     */
    public void setStatus( int status )
    {
        m_status = status;
    }

    /**
     * Utility method returns true if the user's status is VALIDATED
     * (matched against a password).
     */
    public boolean isValidated()
    {
        return( m_status == VALIDATED );
    }

    /**
     * For debugging. XXX remove, ebu
     */
    public String dump()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "UserProfile name=" + m_userName );
        buf.append( "\nRoles: [" );
        Iterator it = m_roles.iterator();
        while( it.hasNext() )
        {
            buf.append( it.next() + " " );
        }
        buf.append( "] Permissions: [" );
        it = m_permissions.iterator();
        while( it.hasNext() )
        {
            buf.append( it.next() + " " );
        }
        buf.append( "]" );
        return( buf.toString() );
    }
    
}
