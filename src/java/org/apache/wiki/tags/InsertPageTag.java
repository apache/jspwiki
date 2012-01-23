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
import javax.servlet.jsp.JspWriter;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.PageNotFoundException;
import org.apache.wiki.providers.ProviderException;


/**
 *  Renders WikiPage content.  For InsertPage tag and the InsertPage plugin
 *  the difference is that the tag will always render in the context of the page
 *  which is referenced (i.e. a LeftMenu inserted on a JSP page with the InsertPage tag
 *  will always render in the context of the actual URL, e.g. Main.), whereas
 *  the InsertPage plugin always renders in local context.  This allows this like
 *  ReferringPagesPlugin to really refer to the Main page instead of having to
 *  resort to any trickery.
 *  <p>
 *  This tag sets the "realPage" field of the WikiContext to point at the inserted
 *  page, while the "page" will contain the actual page in which the rendering
 *  is being made.
 *   
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <li>mode - In which format to insert the page.  Can be either "plain" or "html".
 *  </UL>
 *
 *  @since 2.0
 */
public class InsertPageTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final int HTML  = 0;
    public static final int PLAIN = 1;

    protected String m_pageName = null;
    private   int    m_mode = HTML;

    public void initTag()
    {
        super.initTag();
        m_pageName = null;
        m_mode = HTML;
    }

    public void setPage( String page )
    {
        m_pageName = page;
    }

    public String getPage()
    {
        return m_pageName;
    }

    public void setMode( String arg )
    {
        if( "plain".equals(arg) )
        {
            m_mode = PLAIN;
        }
        else
        {
            m_mode = HTML;
        }
    }

    public final int doWikiStartTag()
        throws IOException,
               ProviderException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        WikiPage   insertedPage = null;

        try
        {
            insertedPage = m_pageName == null ? m_wikiContext.getPage() : engine.getPage( m_pageName );
        }
        catch ( PageNotFoundException e )
        {
            // Couldn't find either the context page, or the one supplied to the tag. Oops!
        }
        
        if( insertedPage == null )
        {
            return SKIP_BODY;
        }
        
        // FIXME: Do version setting later.
        // page.setVersion( WikiProvider.LATEST_VERSION );

        log.debug("Inserting page "+insertedPage);

        JspWriter out = pageContext.getOut();

        WikiPage oldPage = m_wikiContext.setRealPage( insertedPage );
        
        switch( m_mode )
        {
          case HTML:
            out.print( engine.getHTML( m_wikiContext, insertedPage ) );
            break;
          case PLAIN:
            out.print( engine.getText( m_wikiContext, insertedPage ) );
            break;
        }
        m_wikiContext.setRealPage( oldPage );

        return SKIP_BODY;
    }
}
