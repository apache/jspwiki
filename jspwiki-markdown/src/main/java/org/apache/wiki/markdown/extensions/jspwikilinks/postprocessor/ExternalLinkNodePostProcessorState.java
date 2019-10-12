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
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;


/**
 * {@link NodePostProcessorState} which further post processes external links.
 */
public class ExternalLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;
    private boolean m_useOutlinkImage = true;

    public ExternalLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.m_useOutlinkImage = wikiContext.getBooleanWikiProperty( MarkupParser.PROP_USEOUTLINKIMAGE, m_useOutlinkImage );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node)
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        if( linkOperations.isImageLink( link.getUrl().toString() ) ) {
            new ImageLinkNodePostProcessorState( wikiContext, link.getUrl().toString(), link.hasRef() ).process( state, link );
        } else {
            link.setUrl( CharSubSequence.of( link.getUrl().toString() ) );
            NodePostProcessorStateCommonOperations.addOutlinkImage( state, link, wikiContext, m_useOutlinkImage );
        }
    }

}
