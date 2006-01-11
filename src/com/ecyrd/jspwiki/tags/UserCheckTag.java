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

import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.auth.AuthenticationManager;

/**
 *  Includes the content if an user check validates.  This has
 *  been considerably enhanced for 2.2.  The possibilities for the "status"-argument are:
 *
 * <ul>
 * <li>"anonymous"     - the body of the tag is included 
 *                       if the user is completely unknown (no cookie, no password)</li>
 * <li>"asserted"      - the body of the tag is included 
 *                       if the user has either been named by a cookie, but
 *                       not been authenticated.</li>
 * <li>"authenticated" - the body of the tag is included 
 *                       if the user is validated either through the container,
 *                       or by our own authentication.</li>
 * <li>"assertionsAllowed"
 *                     - the body of the tag is included 
 *                       if wiki allows identities to be asserted using cookies.</li>
 * <li>"assertionsNotAllowed"
 *                     - the body of the tag is included 
 *                       if wiki does <i>not</i> allow identities to 
 *                       be asserted using cookies.</li>
 * <li>"containerAuth" - the body of the tag is included 
 *                       if the user is validated through the container.</li>
 * <li>"customAuth"    - the body of the tag is included 
 *                       if the user is validated through our own authentication.</li>
 * <li>"known"         - if the user is not anonymous</li>                      
 * <li>"notAuthenticated"
 *                     - the body of the tag is included 
 *                       if the user is not yet authenticated.</li>
 * <li>"setPassword"   - always true if custom auth used; also true for container auth 
 *                       and current UserDatabase.isSharedWithContainer() is true.</li>
 * </ul>
 *
 *  If the old "exists" -argument is used, it corresponds as follows:
 *  <p>
 *  <tt>exists="true" ==> status="known"<br>
 *  <tt>exists="false" ==> status="unknown"<br>
 *
 *  It is NOT a good idea to use BOTH of the arguments.
 *
 *  @author Janne Jalkanen
 *  @author Erik Bunn
 *  @author Andrew Jaquith
 *  @since 2.0
 */
public class UserCheckTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 3256438110127863858L;
    private static final String ASSERTED = "asserted";
    private static final String AUTHENTICATED = "authenticated";
    private static final String ANONYMOUS = "anonymous";
    private static final String ASSERTIONS_ALLOWED = "assertionsallowed";
    private static final String ASSERTIONS_NOT_ALLOWED = "assertionsnotallowed";
    private static final String CONTAINER_AUTH = "containerauth";
    private static final String CUSTOM_AUTH = "customauth";
    private static final String KNOWN = "known";
    private static final String NOT_AUTHENTICATED = "notauthenticated";
    private static final String SET_PASSWORD = "setpassword";

    private String m_status;

    public String getStatus()
    {
        return( m_status );
    }

    public void setStatus( String arg )
    {
        m_status = arg.toLowerCase();
    }


    /**
     *  Sets the "exists" attribute, which is converted on-the-fly into
     *  an equivalent "status" -attribute.  This is only for backwards compatibility.
     *
     *  @deprecated
     */
    public void setExists( String arg )
    {
        if("true".equals(arg))
        {
            m_status = AUTHENTICATED;
        }
        else
        {
            m_status = ANONYMOUS;
        }
    }


    /**
     * @see com.ecyrd.jspwiki.tags.WikiTagBase#doWikiStartTag()
     */
    public final int doWikiStartTag()
        throws IOException
    {
        WikiSession session = m_wikiContext.getWikiSession();
        String status = session.getStatus();
        AuthenticationManager mgr = m_wikiContext.getEngine().getAuthenticationManager();
        boolean containerAuth = mgr.isContainerAuthenticated();
        boolean cookieAssertions = AuthenticationManager.allowsCookieAssertions();

        if( m_status != null )
        {
            if ( ANONYMOUS.equals( m_status )) 
            {
                if (status.equals(WikiSession.ANONYMOUS))
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
            else if( AUTHENTICATED.equals( m_status ))
            { 
                if (status.equals(WikiSession.AUTHENTICATED)) 
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
            else if( ASSERTED.equals( m_status )) 
            { 
                if (status.equals(WikiSession.ASSERTED)) 
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
            else if( ASSERTIONS_ALLOWED.equals( m_status ))
            { 
                if ( cookieAssertions )
                {
                    return EVAL_BODY_INCLUDE;
                }
                return SKIP_BODY;
            }
            else if( ASSERTIONS_NOT_ALLOWED.equals( m_status ))
            { 
                if ( !cookieAssertions )
                {
                    return EVAL_BODY_INCLUDE;
                }
                return SKIP_BODY;
            }
            else if( CONTAINER_AUTH.equals( m_status )) 
            { 
                if ( containerAuth )
                {
                    return EVAL_BODY_INCLUDE;
                }
                return SKIP_BODY;
            }
            else if( CUSTOM_AUTH.equals( m_status )) 
            { 
                if ( !containerAuth )
                {
                    return EVAL_BODY_INCLUDE;
                }
                return SKIP_BODY;
            }
            else if( KNOWN.equals( m_status )) 
            { 
                if ( !session.isAnonymous() )
                {
                    return EVAL_BODY_INCLUDE;
                }
                return SKIP_BODY;
            }
            else if( NOT_AUTHENTICATED.equals( m_status ))
            { 
                if (!status.equals(WikiSession.AUTHENTICATED)) 
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
            else if ( SET_PASSWORD.equals( m_status ) )
            {
                if ( !mgr.isContainerAuthenticated() ||
                     m_wikiContext.getEngine().getUserDatabase().isSharedWithContainer() )
                {
                    return EVAL_BODY_INCLUDE;
                }
            }
        }

        return SKIP_BODY;
    }

}
