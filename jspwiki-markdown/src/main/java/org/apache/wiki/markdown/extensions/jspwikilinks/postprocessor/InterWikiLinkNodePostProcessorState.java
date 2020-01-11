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

import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.TextUtil;

import java.text.MessageFormat;
import java.util.ResourceBundle;


/**
 * {@link NodePostProcessorState} which further post processes interwiki links.
 */
public class InterWikiLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private static final Logger LOG = Logger.getLogger( InterWikiLinkNodePostProcessorState.class );
    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;
    private final Document document;
    private final boolean m_wysiwygEditorMode;
    private boolean m_useOutlinkImage = true;

    public InterWikiLinkNodePostProcessorState( final WikiContext wikiContext, final Document document ) {
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.document = document;
        this.m_useOutlinkImage = wikiContext.getBooleanWikiProperty( MarkupParser.PROP_USEOUTLINKIMAGE, m_useOutlinkImage );
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
        final String[] refAndPage = link.getUrl().toString().split( ":" );
        if( !m_wysiwygEditorMode ) {
            String urlReference = wikiContext.getEngine().getInterWikiURL( refAndPage[ 0 ] );
            if( urlReference != null ) {
                urlReference = TextUtil.replaceString( urlReference, "%s", refAndPage[ 1 ] );
                if( linkOperations.isImageLink( urlReference ) ) {
                    new ImageLinkNodePostProcessorState( wikiContext, urlReference, link.hasRef() ).process( state, link );
                } else {
                    link.setUrl( CharSubSequence.of( urlReference ) );
                }
                if( linkOperations.isExternalLink( urlReference ) ) {
                    NodePostProcessorStateCommonOperations.addOutlinkImage( state, link, wikiContext, m_useOutlinkImage );
                }
            } else {
                LOG.debug( refAndPage[0] + " not recognized as InterWiki link [document node: " + document + "]" );
                final Object[] args = { refAndPage[ 0 ] };
                final ResourceBundle rb = Preferences.getBundle( wikiContext, InternationalizationManager.CORE_BUNDLE );
                final String errMsg = MessageFormat.format( rb.getString( "markupparser.error.nointerwikiref" ), args );
                NodePostProcessorStateCommonOperations.makeError( state, link, errMsg );
            }
        } else {
            link.setUrl( CharSubSequence.of( refAndPage[0] + ":" + refAndPage[1] ) );
        }
    }

}
