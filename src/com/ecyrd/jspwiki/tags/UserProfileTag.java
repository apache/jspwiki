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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.security.Principal;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * <p>
 * Returns user profile attributes, or empty strings if the user has not been
 * validated. This tag has a single attribute, "property," which may contain one
 * of the following case-insensitive values:
 * </p>
 * <ul>
 * <li>email</li>
 * <li>fullname</li>
 * <li>loginname</li>
 * <li>wikiname</li>
 * </ul>
 * @author Andrew Jaquith
 * @version $Revision: 1.2 $ $Date: 2005-06-29 22:43:17 $
 * @since 2.3
 */
public class UserProfileTag extends WikiTagBase
{
    public static final String BLANK = "(not set)";

    private String             m_prop;

    public final int doWikiStartTag() throws IOException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        Principal user = m_wikiContext.getCurrentUser();
        UserDatabase database = engine.getUserDatabase();
        UserProfile profile = null;
        try
        {
            profile = database.find( user.getName() );

            if ( "email".equals( m_prop ) )
            {
                pageContext.getOut().print( profile.getEmail() );
            }
            else if ( "fullname".equals( m_prop ) )
            {
                pageContext.getOut().print( profile.getFullname() );
            }
            else if ( "loginname".equals( m_prop ) )
            {
                pageContext.getOut().print( profile.getLoginName() );
            }
            else if ( "wikiname".equals( m_prop ) )
            {
                pageContext.getOut().print( profile.getWikiName() );
            }
        }
        catch( NoSuchPrincipalException e )
        {
            pageContext.getOut().print( BLANK );
        }

        return SKIP_BODY;
    }

    public void setProperty( String property )
    {
        m_prop = property.toLowerCase().trim();
    }
}