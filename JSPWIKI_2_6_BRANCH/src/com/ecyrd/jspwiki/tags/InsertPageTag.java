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
import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.providers.ProviderException;

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
 *  @author Janne Jalkanen
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
        WikiPage   insertedPage;

        //
        //  NB: The page might not really exist if the user is currently
        //      creating it (i.e. it is not yet in the cache or providers), 
        //      AND we got the page from the wikiContext.
        //

        if( m_pageName == null )
        {
            insertedPage = m_wikiContext.getPage();
            if( !engine.pageExists(insertedPage) ) return SKIP_BODY;
        }
        else
        {
            insertedPage = engine.getPage( m_pageName );
        }

        if( insertedPage != null )
        {
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
        }

        return SKIP_BODY;
    }
}
