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

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiException;
import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.UserProfile;


/**
 * Authenticator acts as a unified interface and simplified loader of 
 * a dynamically defined WikiAuthenticator object.
 *
 * <P>By default, no authenticator is defined in <i>jspwiki.properties</i>.
 * In this case, Authenticator merely validates the UserProfile and
 * returns, ensuring full access to a default, unprotected site. 
 * If an authenticator <i>is</i> specified, however, the
 * profile is passed to it for a more thorough handling. 
 * 
 * <P>The responsibility of a WikiAuthenticator is to check that
 * the correct password was supplied by a user, and, if so, to
 * mark the UserProfile object as valid. Any further processing -
 * access permissions, etc. - is taken care of by Authorizer
 * and WikiAuthorizer implementations. 

 * <p>The source of user information depends on the actual
 * authenticator class used. The simplest case is the FileAuthenticator
 * with simple file-based entries.
 *
 * <p>A successful authentication modifies the checked UserProfile
 * by setting its status to VALIDATED and adding its roles and
 * permissions. (In practice, WikiEngine also creates a non-validated
 * UserProfile with certain default roles for guest users. This,
 * however, is not the concern of the auth package.)
 *
 * <P>See AccessRuleSet for more information on how page-level
 * access configuration works.
 */
public class Authenticator
{
    public static final String PROP_AUTHENTICATOR = "jspwiki.authenticator";
    public static final String MSG_AUTH = "authmsg";

    static Category log = Category.getInstance( Authenticator.class );

    private WikiAuthenticator m_auth;


    /**
     * Creates a new Authenticator, using the class defined in the
     * properties (jspwiki.properties file). If none is defined, 
     * uses the default DummyAuthenticator.
     */
    public Authenticator( Properties props )
        throws WikiException, NoRequiredPropertyException
    {
        String classname = null;
        try
        {
            classname = props.getProperty( PROP_AUTHENTICATOR );
            if( classname == null )
            {
                log.warn( "No " + PROP_AUTHENTICATOR + " defined in jspwiki.properties; " +
                          "users will be validated always." );
                m_auth = null;
            }
            else
            {
                Class authClass = WikiEngine.findWikiClass( classname, "com.ecyrd.jspwiki.auth" );
                m_auth = (WikiAuthenticator)authClass.newInstance();
                m_auth.initialize( props );
            }
        }
        catch( ClassNotFoundException e )
        {
            log.error( "Unable to locate authenticator class " + classname, e );
            throw new WikiException( "no authenticator class" );
        }
        catch( InstantiationException e )
        {
            log.error( "Unable to create authenticator class " + classname, e );
            throw new WikiException( "faulty provider class" );
        }
        catch( IllegalAccessException e )
        {
            log.error( "Illegal access to authenticator class " + classname, e );
            throw new WikiException( "illegal provider class" );
        }
        catch( NoRequiredPropertyException e )
        {
            log.error("Authenticator did not found a property it was looking for: " + e.getMessage(), e );
            throw e;  // Same exception works.
        }
    }


    /**
     * Calls the current authenticator's authenticate() method.
     * If no authenticator has been specified, validates the 
     * user and returns true. 
     */
    public boolean authenticate( UserProfile wup )
    {
        if( wup == null )
            return( false );

        if( m_auth != null )
        {
            return( m_auth.authenticate( wup ) );
        }
        else
        {
            wup.setStatus( UserProfile.VALIDATED );
            return( true );
        }
    }


    /**
     * Returns the Authenticator in use.
     */
    public WikiAuthenticator getAuthenticator()
    {
        return( m_auth );
    }
    
}



