/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.tags;

import java.io.IOException;

import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthenticationManager;


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

    private String m_status;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initTag()
    {
        super.initTag();
        m_status = null;
    }

    /**
     *  Get the status as defined above.
     *  
     *  @return The status to be checked.
     */
    public String getStatus()
    {
        return m_status;
    }

    /**
     *  Sets the status as defined above.
     *  
     *  @param status The status to be checked.
     */
    public void setStatus( String status )
    {
        m_status = status.toLowerCase();
    }


    /**
     *  Sets the "exists" attribute, which is converted on-the-fly into
     *  an equivalent "status" -attribute.  This is only for backwards compatibility.
     *
     *  @param arg If true, works exactly as status = authenticated.  If false, works
     *             as if status = anonymous.
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
     * {@inheritDoc}
     * @see org.apache.wiki.tags.WikiTagBase#doWikiStartTag()
     */
    @Override
    public final int doWikiStartTag()
        throws IOException
    {
        WikiSession session = m_wikiContext.getWikiSession();
        String status = session.getStatus();
        AuthenticationManager mgr = m_wikiContext.getEngine().getAuthenticationManager();
        boolean containerAuth = mgr.isContainerAuthenticated();
        boolean cookieAssertions = mgr.allowsCookieAssertions();

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
        }

        return SKIP_BODY;
    }

}
