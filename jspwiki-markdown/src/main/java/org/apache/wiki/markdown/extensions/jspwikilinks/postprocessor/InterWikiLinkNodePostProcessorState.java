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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.render.RenderingManager;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.NodeTracker;


/**
 * {@link NodePostProcessorState} which further post processes interwiki links.
 */
public class InterWikiLinkNodePostProcessorState implements NodePostProcessorState< Link > {

    private static final Logger LOG = Logger.getLogger( InterWikiLinkNodePostProcessorState.class );
    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;
    private final Document document;
    private final boolean m_wysiwygEditorMode;
    private final boolean hasRef;
    private boolean m_useOutlinkImage = true;

    public InterWikiLinkNodePostProcessorState( final WikiContext wikiContext, final Document document, final boolean hasRef ) {
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.document = document;
        this.hasRef = hasRef;
        this.m_useOutlinkImage = wikiContext.getBooleanWikiProperty( MarkupParser.PROP_USEOUTLINKIMAGE, m_useOutlinkImage );
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( RenderingManager.WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null ? wysiwygVariable.booleanValue() : false;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Link)
     */
    @Override
    public void process( NodeTracker state, Link link ) {
        if( !m_wysiwygEditorMode ) {
            final String[] refAndPage = link.getUrl().toString().split( ":" );
            final String urlReference = wikiContext.getEngine().getInterWikiURL( refAndPage[ 0 ] );
            if( urlReference != null ) {
                if( linkOperations.isImageLink( urlReference ) ) {
                    new ImageLinkNodePostProcessorState( wikiContext, urlReference, hasRef ).process( state, link );
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
        }
    }

}
