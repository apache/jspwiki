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

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.UserProfile;

/**
 *  Includes the content if an user check validates.  This has
 *  been considerably enhanced for 2.2.
 *
 * <ul>
 * <li>"unknown"     - the body of the tag is included 
 *                     if the user is completely unknown (no cookie, no password)
 * <li>"known"       - the body of the tag is included 
 *                     if the user is not unknown (i.e has a cookie, 
 *                     or has been authenticated.
 * <li>"named"       - the body of the tag is included 
 *                     if the user has either been named by a cookie, but
 *                     not been authenticated.
 * <li>"validated"   - the body of the tag is included 
 *                     if the user is validated either through the container,
 *                     or by our own authentication.
 * <li>"unvalidated" - the body of the tag is included 
 *                     if the user is not validated (i.e. he could have a cookie,
 *                     but has not been authenticated.)
 *
 *  @author Janne Jalkanen
 *  @author Erik Bunn
 *  @since 2.0
 */
public class UserCheckTag
    extends WikiTagBase
{
    private String m_status;

    public String getStatus()
    {
        return( m_status );
    }

    public void setStatus( String arg )
    {
        m_status = arg;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine  engine = m_wikiContext.getEngine();
        UserProfile wup    = m_wikiContext.getCurrentUser();

        if( m_status != null )
        {
            if( wup == null )
            {
                // This may happen when strict login policy is used.
                return( SKIP_BODY );
            }

            if( "unknown".equals( m_status ) && 
                wup.getLoginStatus() == UserProfile.NONE )
            {
                return EVAL_BODY_INCLUDE;
            }
            else if( "known".equals( m_status ) && 
                     wup.getLoginStatus() > UserProfile.NONE )
            {
                return EVAL_BODY_INCLUDE;
            }
            else if( "named".equals( m_status ) && 
                     wup.getLoginStatus() == UserProfile.COOKIE )
            {
                return EVAL_BODY_INCLUDE;
            }
            else if( "validated".equals( m_status ) && 
                     wup.getLoginStatus() > UserProfile.CONTAINER )
            {
                return EVAL_BODY_INCLUDE;
            }
            else if( "unvalidated".equals( m_status ) && 
                     wup.getLoginStatus() < UserProfile.CONTAINER )
            {
                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }

}
