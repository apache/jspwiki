/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import java.util.Iterator;
import java.util.Collection;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.SearchResult;

/**
 *  Iterates through Search result results.
 *
 *  <P><B>Attributes</B></P>
 *  <UL>
 *    <LI>max = how many search results should be shown.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */

// FIXME: Shares MUCH too much in common with IteratorTag.  Must refactor.
public class SearchResultIteratorTag
    extends IteratorTag
{
    private   int         m_maxItems;
    private   int         m_count = 0;

    public void setMaxItems( String arg )
    {
        m_maxItems = Integer.parseInt(arg);
    }

    public final int doStartTag()
    {
        m_count       = 0;
        m_wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT,
                                                                PageContext.REQUEST_SCOPE );

        return nextResult();
    }

    private int nextResult()
    {
        if( m_iterator.hasNext() && m_count++ < m_maxItems )
        {
            SearchResult r = (SearchResult) m_iterator.next();

            WikiContext context = new WikiContext( m_wikiContext.getEngine(), r.getPage() );
            pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                      context,
                                      PageContext.REQUEST_SCOPE );
            pageContext.setAttribute( getId(),
                                      r );

            return EVAL_BODY_TAG;
        }

        return SKIP_BODY;
    }

    public int doAfterBody()
    {
        if( bodyContent != null )
        {
            try
            {
                JspWriter out = getPreviousOut();
                out.print(bodyContent.getString());
                bodyContent.clearBody();
            }
            catch( IOException e )
            {
                log.error("Unable to get inner tag text", e);
                // FIXME: throw something?
            }
        }

        return nextResult();
    }
}
