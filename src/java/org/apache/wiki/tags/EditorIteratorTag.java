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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.WikiContextFactory;
import org.apache.wiki.ui.Editor;
import org.apache.wiki.ui.EditorManager;

/**
 * Iterator tag for editors.
 * 
 * @since 2.4.12
 */
public class EditorIteratorTag extends IteratorTag<Editor>
{
    private static final long serialVersionUID = 1L;

    /**
     * Returns the default list of editors into the collection that will be iterated
     * over, as returned by {@link EditorManager#getEditorList()}.
     */
    protected Collection<Editor> initItems()
    {
        // Retrieve the list of editors
        WikiContext wikiContext = WikiContextFactory.findContext( pageContext );
        WikiEngine engine = wikiContext.getEngine();
        EditorManager mgr = engine.getEditorManager();
        String[] editorList = mgr.getEditorList();

        // Create a new collection of Editors
        Collection<Editor> editors = new ArrayList<Editor>();
        for( int i = 0; i < editorList.length; i++ )
        {
            editors.add( new Editor( m_wikiContext, editorList[i] ) );
        }
        return editors;
    }
}
