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
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.preferences.Preferences;

import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * {@link NodePostProcessorState} which further post processes metadata links.
 */
public class MetadataLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private static final Logger LOG = Logger.getLogger( MetadataLinkNodePostProcessorState.class );
    private final WikiContext wikiContext;
    private final boolean m_wysiwygEditorMode;

    public MetadataLinkNodePostProcessorState( final WikiContext wikiContext ) {
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
        final String metadataLine = NodePostProcessorStateCommonOperations.inlineLinkTextOnWysiwyg( state, link, m_wysiwygEditorMode );
        try {
            final String args = metadataLine.substring( metadataLine.indexOf(' '), metadataLine.length() - 1 );
            String name = args.substring( 0, args.indexOf( '=' ) );
            String val = args.substring( args.indexOf( '=' ) + 1 );

            name = name.trim();
            val = val.trim();

            if( val.startsWith( "'" ) ) {
                val = val.substring( 1 );
            }
            if( val.endsWith( "'" ) ) {
                val = val.substring( 0, val.length()-1 );
            }

            LOG.debug( "page=" + wikiContext.getRealPage().getName() + " SET name='" + name + "', value='" + val + "'" );

            if( name.length() > 0 && val.length() > 0 ) {
                val = wikiContext.getEngine().getVariableManager().expandVariables( wikiContext, val );
                wikiContext.getPage().setAttribute( name, val );
                link.unlink();
                state.nodeRemoved( link );
            }
        } catch( final Exception e ) {
            final ResourceBundle rb = Preferences.getBundle( wikiContext, InternationalizationManager.CORE_BUNDLE );
            NodePostProcessorStateCommonOperations.makeError( state, link,
                                                              MessageFormat.format( rb.getString( "markupparser.error.invalidset" ), metadataLine ) );
        }
    }

}
