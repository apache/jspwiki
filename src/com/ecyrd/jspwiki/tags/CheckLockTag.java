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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.PageManager;
import com.ecyrd.jspwiki.PageLock;
import com.ecyrd.jspwiki.providers.ProviderException;

import javax.servlet.http.HttpSession;

/**
 *
 *  @since 2.0
 */
public class CheckLockTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final int LOCKED    = 0;
    public static final int NOTLOCKED = 1;
    public static final int OWNED     = 2;

    private int m_mode;

    public void initTag()
    {
        super.initTag();
        m_mode = 0;
    }

    public void setMode( String arg )
    {
        if( "locked".equals(arg) )
        {
            m_mode = LOCKED;
        }
        else if("owned".equals(arg) )
        {
            m_mode = OWNED;
        }
        else
        {
            m_mode = NOTLOCKED;
        }
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            PageManager mgr = engine.getPageManager();

            PageLock lock = mgr.getCurrentLock( page );

            HttpSession session = pageContext.getSession();

            PageLock userLock = (PageLock) session.getAttribute("lock-"+page.getName());

            if( (lock != null && m_mode == LOCKED && lock != userLock ) ||
                (lock != null && m_mode == OWNED && lock == userLock ) ||
                (lock == null && m_mode == NOTLOCKED) )
            {
                String tid = getId();

                if( tid != null && lock != null )
                {
                    pageContext.setAttribute( tid, lock );
                }

                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }

}
