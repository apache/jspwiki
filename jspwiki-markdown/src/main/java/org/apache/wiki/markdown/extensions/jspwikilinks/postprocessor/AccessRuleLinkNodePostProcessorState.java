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

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.acl.Acl;
import org.apache.wiki.markdown.nodes.JSPWikiLink;


/**
 * {@link NodePostProcessorState} which further post processes access rules links.
 */
public class AccessRuleLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private static final Logger LOG = Logger.getLogger( AccessRuleLinkNodePostProcessorState.class );
    private final WikiContext wikiContext;
    private final boolean m_wysiwygEditorMode;

    public AccessRuleLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null ? wysiwygVariable : false;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node)
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        String ruleLine = NodePostProcessorStateCommonOperations.inlineLinkTextOnWysiwyg( state, link, m_wysiwygEditorMode );
        if( wikiContext.getEngine().getRenderingManager().getParser( wikiContext, link.getUrl().toString() ).isParseAccessRules() ) {
            final WikiPage page = wikiContext.getRealPage();
            if( ruleLine.startsWith( "{" ) ) {
                ruleLine = ruleLine.substring( 1 );
            }
            if( ruleLine.endsWith( "}" ) ) {
                ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );
            }
            LOG.debug( "page=" + page.getName() + ", ACL = " + ruleLine );

            try {
                final Acl acl = wikiContext.getEngine().getAclManager().parseAcl( page, ruleLine );
                page.setAcl( acl );
                link.unlink();
                state.nodeRemoved( link );
                LOG.debug( acl.toString() );
            } catch( final WikiSecurityException wse ) {
                NodePostProcessorStateCommonOperations.makeError( state, link, wse.getMessage() );
            }
        }
    }

}
