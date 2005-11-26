/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import javax.servlet.ServletException;
import javax.servlet.jsp.JspException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.ui.EditorManager;

/**
 *  Creates an editor component with all the necessary parts
 *  to get it working.
 *  <p>
 *  In the future, this component should be expanded to provide
 *  a customized version of the editor according to user preferences.
 *
 *  @author Janne Jalkanen
 *  @since 2.2
 */
public class EditorTag
    extends WikiBodyTag
{
    private static final long serialVersionUID = 0L;
    
    public final int doWikiStartTag()
        throws IOException
    {
        return SKIP_BODY;
    }
       
    public int doEndTag() throws JspException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        EditorManager mgr = engine.getEditorManager();
        
        String editorPath = mgr.getEditorPath( m_wikiContext );
        
        try
        {
            String page = engine.getTemplateManager().findJSP( pageContext,
                                                               m_wikiContext.getTemplate(),
                                                               editorPath );
            
            if( page == null )
            {
                pageContext.getOut().println("Unable to find editor '"+editorPath+"'");
            }
            else
            {
                pageContext.include( page );
            }
        }
        catch( ServletException e )
        {
            log.error("Failed to include editor",e);
            throw new JspException("Failed to include editor: "+e.getMessage() );
        }
        catch( IOException e )
        {
            throw new JspException("Could not print Editor tag: "+e.getMessage() );
        }
        
        return EVAL_PAGE;
    }
}
