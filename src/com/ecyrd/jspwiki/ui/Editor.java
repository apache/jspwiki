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
package com.ecyrd.jspwiki.ui;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Describes an editor.
 *  
 *  @author Chuck Smith
 *  @since 2.4.12
 */
public class Editor
{
    private String m_editorName;
    private WikiContext m_wikiContext;
    private EditorManager m_editorManager;
    
    public Editor( WikiContext wikiContext, String editorName )
    {
        m_wikiContext = wikiContext;
        m_editorName = editorName;
        m_editorManager = wikiContext.getEngine().getEditorManager();
    }

    public String getName()
    {
        return m_editorName;
    }
    
    public String getURL()
    {
        String uri = m_wikiContext.getHttpRequest().getRequestURI();
        String para = m_wikiContext.getHttpRequest().getQueryString();
        
        // if para already contains editor parameter, replace instead of append it
        // FIXME: Should cut out parameter instead of simple setting strin to null, maybe
        // in futur releases it may change and theres the danger that trailing parameters get lost
        int idx = para.indexOf(EditorManager.PARA_EDITOR + "=");
        if (idx >= 0)
        {
            para = para.substring(0, idx-1);
        }
        
        return uri + "?" + para + "&amp;" + EditorManager.PARA_EDITOR + "=" + m_editorName;
    }

    /**
     *  Convinience method which returns XHTML for an option element.
     * @return "selected='selected'", if this editor is selected.
     */
    public String isSelected( ) 
    {
        return isSelected( "selected='selected'", "" );
    }
    
    public String isSelected( String ifSelected )
    {
        return isSelected( ifSelected, "" );
    }
    
    public String isSelected( String ifSelected, String ifNotSelected )
    {
        if ( m_editorName.equals(m_editorManager.getEditorName(m_wikiContext) ) )
        {
            return ifSelected;
        }
        return ifNotSelected;
    }

    public String toString()
    {
        return m_editorName;
    }
}