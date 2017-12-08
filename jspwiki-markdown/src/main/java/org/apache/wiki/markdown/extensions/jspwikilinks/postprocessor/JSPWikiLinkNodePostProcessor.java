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

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.PostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.util.NodeTracker;


/**
 * {@link NodePostProcessor} for JSPWiki links.
 *
 * Acts as a factory of {@link NodePostProcessorState}, which are the classes generating the extra markup for each concrete type of link.
 */
public class JSPWikiLinkNodePostProcessor extends NodePostProcessor {

    protected final WikiContext m_context;
    protected final LinkParsingOperations linkOperations;
    protected boolean m_useOutlinkImage = true;
    protected final Document document;

    public JSPWikiLinkNodePostProcessor( final WikiContext m_context, final Document document ) {
        this.m_context = m_context;
        this.document = document;
        linkOperations = new LinkParsingOperations( m_context );
        m_useOutlinkImage = m_context.getBooleanWikiProperty( MarkupParser.PROP_USEOUTLINKIMAGE, m_useOutlinkImage );
    }

    /**
     * {@inheritDoc}
     *
     * @see PostProcessor#process(NodeTracker, Node)
     */
    @Override
    public void process( final NodeTracker state, final Node node ) {
        if( node instanceof Link ) {
            final Link link = ( Link )node;
            boolean hasRef = true;
            if( StringUtils.isEmpty( link.getUrl().toString() ) ) { // empty link == link.getText() is a wiki page
                link.setUrl( link.getText() );
                hasRef = false;
            }

            final NodePostProcessorState< Link > linkPostProcessor;
            if( linkOperations.isAccessRule( link.getUrl().toString() ) ) {
                linkPostProcessor = new AccessRuleLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isMetadata( link.getUrl().toString() ) ) {
                linkPostProcessor = new MetadataLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isPluginLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new PluginLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isVariableLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new VariableLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isExternalLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new ExternalLinkNodePostProcessorState( m_context, hasRef );
            } else if( linkOperations.isInterWikiLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new InterWikiLinkNodePostProcessorState( m_context, document, hasRef );
            } else if( TextUtil.isNumber( link.getUrl().toString() ) ) {
                linkPostProcessor = new FootnoteRefLinkNodePostProcessorState();
            } else {
                linkPostProcessor = new LocalLinkNodePostProcessorState( m_context, hasRef );
            }
            linkPostProcessor.process( state, link );
        }
    }

}
