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
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;


/**
 * {@link NodePostProcessorState} which further post processes image links.
 */
public class ImageLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private final boolean isLinkFromText;
    private final String urlRef;
    private final LinkParsingOperations linkOperations;

    public ImageLinkNodePostProcessorState( final WikiContext wikiContext, final String urlRef, final boolean isLinkFromText ) {
        this.isLinkFromText = isLinkFromText;
        this.urlRef = urlRef;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node)
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        final HtmlInline img = new HtmlInline( CharSubSequence.of( "<img class=\"inline\" " +
                                                                        "src=\"" + urlRef + "\" " +
                                                                        "alt=\"" + link.getText().toString() + "\" />" ) );
        if( ( isLinkFromText && linkOperations.isExternalLink( link.getText().toString() ) ) ||
                ( isLinkFromText && linkOperations.linkExists( link.getText().toString() ) ) ) {
            link.setUrl( CharSubSequence.of( urlRef ) );
            link.removeChildren();
            link.appendChild( img );
            state.nodeAdded( img );
        } else {
            NodePostProcessorStateCommonOperations.addContent( state, link, img );
        }
    }

}
