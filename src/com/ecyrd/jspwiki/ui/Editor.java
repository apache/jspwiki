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

    // FIXME: Fails, if the editoriterator is on a non-editor page.
    /** @deprecated */
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
