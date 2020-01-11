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

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.parser.PostProcessor;
import com.vladsch.flexmark.parser.block.NodePostProcessor;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeTracker;
import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;


/**
 * {@link NodePostProcessor} to convert {@link Link}s into {@link JSPWikiLink}s.
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
            final JSPWikiLink link = replaceLinkWithJSPWikiLink( state, node );

            final NodePostProcessorState< JSPWikiLink > linkPostProcessor;
            if( linkOperations.isAccessRule( link.getUrl().toString() ) ) {
                linkPostProcessor = new AccessRuleLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isMetadata( link.getUrl().toString() ) ) {
                linkPostProcessor = new MetadataLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isPluginLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new PluginLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isVariableLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new VariableLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isExternalLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new ExternalLinkNodePostProcessorState( m_context );
            } else if( linkOperations.isInterWikiLink( link.getUrl().toString() ) ) {
                linkPostProcessor = new InterWikiLinkNodePostProcessorState( m_context, document );
            } else if( StringUtils.startsWith( link.getUrl().toString(), "#" ) ) {
                linkPostProcessor = new LocalFootnoteLinkNodePostProcessorState( m_context );
            } else if( TextUtil.isNumber( link.getUrl().toString() ) ) {
                linkPostProcessor = new LocalFootnoteRefLinkNodePostProcessorState( m_context );
            } else {
                linkPostProcessor = new LocalLinkNodePostProcessorState( m_context );
            }
            linkPostProcessor.process( state, link );
        }
    }

    JSPWikiLink replaceLinkWithJSPWikiLink( final NodeTracker state, final Node node ) {
        final JSPWikiLink link = new JSPWikiLink( ( Link )node );
        final Node previous = node.getPrevious();
        final Node parent = node.getParent();

        link.takeChildren( node );
        node.unlink();

        if( previous != null ) {
            previous.insertAfter( link );
        } else {
            parent.appendChild( link );
        }

        state.nodeRemoved( node );
        state.nodeAddedWithChildren( link );
        return link;
    }

}
