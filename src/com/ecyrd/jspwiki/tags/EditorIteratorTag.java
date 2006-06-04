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
import java.util.ArrayList;
import java.util.Collection;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.Editor;
import com.ecyrd.jspwiki.ui.EditorManager;

/**
 *  Iterates through editors.
 *
 *  @author Chuck Smith
 *  @since 2.4.12
 */

public class EditorIteratorTag
    extends IteratorTag
{
    private static final long serialVersionUID = 0L;
    
    static    Logger    log = Logger.getLogger( EditorIteratorTag.class );

    public final int doStartTag()
    {
        m_wikiContext = (WikiContext) pageContext.getAttribute( WikiTagBase.ATTR_CONTEXT,
                                                                PageContext.REQUEST_SCOPE );

        WikiEngine engine = m_wikiContext.getEngine();
        EditorManager mgr    = engine.getEditorManager();

        try
        {
            if( mgr != null )
            {
                String[] editorList = mgr.getEditorList();
                
                Collection editors = new ArrayList();
                
                for ( int i = 0; i < editorList.length; i++ )
                {
                    editors.add(new Editor(m_wikiContext, editorList[i]));
                } 

                if( editors == null )
                {
                    log.debug("No editors configured.");

                    // There are no editors.
                    return SKIP_BODY;
                }

                m_iterator = editors.iterator();

                if ( m_iterator.hasNext() )
                {
                    WikiContext context = (WikiContext)m_wikiContext.clone();
                    Editor editor = (Editor) m_iterator.next();
                    
                    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                              context,
                                              PageContext.REQUEST_SCOPE );
                    pageContext.setAttribute( getId(), editor );
                    return EVAL_BODY_BUFFERED;
                }
                else
                {
                    return SKIP_BODY;
                }
            }
        }
        catch( Exception e )
        {
            log.fatal("Provider failed while trying to iterate through editors",e);
            // FIXME: Throw something.
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
        
        if ( m_iterator != null && m_iterator.hasNext() )
        {
            Editor edtr = (Editor) m_iterator.next(); 
            WikiContext context = (WikiContext)m_wikiContext.clone();
            pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                                      context,
                                      PageContext.REQUEST_SCOPE );
            pageContext.setAttribute( getId(), edtr );
            return EVAL_BODY_BUFFERED;
        }

        return SKIP_BODY;
    }
}
