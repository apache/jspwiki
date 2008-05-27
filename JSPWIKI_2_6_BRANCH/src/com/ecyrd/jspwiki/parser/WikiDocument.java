/* 
   JSPWiki - a JSP-based WikiWiki clone.

   Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.parser;

import java.lang.ref.WeakReference;

import org.jdom.Document;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Stores the DOM tree of a rendered WikiPage.  This class
 *  extends the org.jdom.Document to provide some extra metadata
 *  specific to JSPWiki.
 *  <p>
 *  The document is not stored as metadata in the WikiPage because
 *  otherwise it could not be cached separately.
 *  
 *  @author Janne Jalkanen
 *  @since  2.4
 */
public class WikiDocument extends Document
{
    private static final long serialVersionUID = 0L;
    
    private WikiPage m_page;
    private String   m_wikiText;

    private WeakReference m_context;
    
    /**
     *  Creates a new WikiDocument for a specific page.
     * 
     *  @param page The page to which this document refers to.
     */
    public WikiDocument( WikiPage page )
    {
        m_page     = page;
    }
    
    public void setPageData( String data )
    {
        m_wikiText = data;
    }
    
    public String getPageData()
    {
        return m_wikiText;
    }
    
    public WikiPage getPage()
    {
        return m_page;
    }

    public void setContext( WikiContext ctx )
    {
        m_context = new WeakReference( ctx );
    }
    
    /**
     * Returns the wiki context for this document. This method
     * may return <code>null</code> if the associated wiki session
     * had previously been garbage-collected.
     * @return the wiki context
     */
    public WikiContext getContext()
    {
        return (WikiContext) m_context.get();
    }
}
