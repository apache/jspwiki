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
package org.apache.wiki.tags;

import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.ui.Editor;
import org.apache.wiki.ui.EditorManager;

import java.util.ArrayList;
import java.util.Collection;

/**
 *  Iterates through editors.
 *
 *  @since 2.4.12
 */

public class EditorIteratorTag extends IteratorTag  {

    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override
    public final int doStartTag() {
        m_wikiContext = Context.findContext(pageContext);
        final Engine engine = m_wikiContext.getEngine();
        final EditorManager mgr = engine.getManager( EditorManager.class );
        final String[] editorList = mgr.getEditorList();
        final Collection< Editor > editors = new ArrayList<>();

        for( final String editor : editorList ) {
            editors.add( new Editor( m_wikiContext, editor ) );
        }
        setList( editors );

        return super.doStartTag();
    }

}
