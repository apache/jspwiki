/* 
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
package org.apache.wiki.ui;

import org.apache.wiki.WikiContext;

/**
 *  Describes an editor.
 *
 *  @since 2.4.12
 */
public class Editor {

    private final String m_editorName;
    private final WikiContext m_wikiContext;
    private final EditorManager m_editorManager;

    public Editor( final WikiContext wikiContext, final String editorName ) {
        m_wikiContext = wikiContext;
        m_editorName = editorName;
        m_editorManager = wikiContext.getEngine().getEditorManager();
    }

    public String getName() {
        return m_editorName;
    }

    /**
     *  Convenience method which returns XHTML for an option element.
     * @return "selected='selected'", if this editor is selected.
     */
    public String isSelected( )
    {
        return isSelected( "selected='selected'", "" );
    }

    public String isSelected( final String ifSelected )
    {
        return isSelected( ifSelected, "" );
    }

    public String isSelected( final String ifSelected, final String ifNotSelected ) {
        if( m_editorName.equals( m_editorManager.getEditorName( m_wikiContext ) ) ) {
            return ifSelected;
        }
        return ifNotSelected;
    }

    @Override
    public String toString() {
        return m_editorName;
    }

}
