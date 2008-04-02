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
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Does a version check on the page.  Mode is as follows:
 *  <UL>
 *   <LI>latest = Include page content, if the page is the latest version.
 *   <LI>notlatest = Include page content, if the page is NOT the latest version.
 *  </UL>
 *  If the page does not exist, body content is never included.
 *
 *  @since 2.0
 */
public class CheckVersionTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final int LATEST    = 0;
    public static final int NOTLATEST = 1;
    public static final int FIRST     = 2;
    public static final int NOTFIRST  = 3;

    private int m_mode;

    public void initTag()
    {
        super.initTag();
        m_mode = 0;
    }

    public void setMode( String arg )
    {
        if( "latest".equals(arg) )
        {
            m_mode = LATEST;
        }
        else if( "notfirst".equals(arg) )
        {
            m_mode = NOTFIRST;
        }
        else if( "first".equals(arg) )
        {
            m_mode = FIRST;
        }
        else
        {
            m_mode = NOTLATEST;
        }
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        WikiEngine engine = m_actionBean.getEngine();

        if( m_page != null && engine.pageExists(m_page.getName()) )
        {
            int version = m_page.getVersion();
            boolean include = false;

            WikiPage latest = engine.getPage( m_page.getName() );

            //log.debug("Doing version check: this="+page.getVersion()+
            //          ", latest="+latest.getVersion());

            switch( m_mode )
            {
              case LATEST:
                include = (version < 0) || (latest.getVersion() == version);
                break;

              case NOTLATEST:
                include = (version > 0) && (latest.getVersion() != version);
                break;

              case FIRST:
                include = (version == 1 ) || (version < 0 && latest.getVersion() == 1);
                break;

              case NOTFIRST:
                include = version > 1;
                break;
            }

            if( include )
            {
                // log.debug("INCLD");
                return EVAL_BODY_INCLUDE;
            }
        }

        return SKIP_BODY;
    }
}
