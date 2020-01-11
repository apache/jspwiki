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
package org.apache.wiki.markdown.extensions.jspwikilinks.postprocessor;

import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.api.exceptions.NoSuchVariableException;
import org.apache.wiki.markdown.nodes.JSPWikiLink;


/**
 * {@link NodePostProcessorState} which further post processes WikiVariable links.
 */
public class VariableLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private final WikiContext wikiContext;
    private final boolean m_wysiwygEditorMode;

    public VariableLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null && wysiwygVariable;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node)
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        final String variable = NodePostProcessorStateCommonOperations.inlineLinkTextOnWysiwyg( state, link, m_wysiwygEditorMode );
        if( !m_wysiwygEditorMode ) {
            try {
                final String parsedVariable = wikiContext.getEngine().getVariableManager().parseAndGetValue( wikiContext, variable );
                final HtmlInline content = new HtmlInline( CharSubSequence.of( StringEscapeUtils.escapeXml11( parsedVariable ) ) );
                NodePostProcessorStateCommonOperations.addContent( state, link, content );
            } catch( final NoSuchVariableException e ) {
                NodePostProcessorStateCommonOperations.makeError( state, link, "No such variable: " + variable + " (" + e.getMessage() + ")" );
            }
        }
    }

}
