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

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.LinkParsingOperations;

import com.vladsch.flexmark.ast.HtmlInline;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.NodeTracker;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodePostProcessorState} which further post processes local links.
 */
public class LocalLinkNodePostProcessorState implements NodePostProcessorState< Link > {

    private final boolean hasRef;
    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;

    public LocalLinkNodePostProcessorState( final WikiContext wikiContext, final boolean hasRef ) {
        this.hasRef = hasRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Link)
     */
    @Override
    public void process( NodeTracker state, Link link ) {
        final String attachment = wikiContext.getEngine().getAttachmentManager().getAttachmentInfoName( wikiContext, link.getUrl().toString() );
        if( attachment != null  ) {
            if( linkOperations.isImageLink( link.getUrl().toString() ) ) {
                new ImageLinkNodePostProcessorState( wikiContext, attachment, hasRef ).process( state, link );
            } else {
                link.removeChildren();
                final HtmlInline content = new HtmlInline( CharSubSequence.of( link.getText().toString() ) );
                link.appendChild( content );
                state.nodeAddedWithChildren( content );
                addAttachmentLink( state, link );
            }
        }
    }

    void addAttachmentLink( final NodeTracker state, final Link link ) {
        final String infolink = wikiContext.getURL( WikiContext.INFO, link.getUrl().toString() );
        final String imglink = wikiContext.getURL( WikiContext.NONE, "images/attachment_small.png" );
        final HtmlInline aimg = new HtmlInline( CharSubSequence.of( "<a href=\""+ infolink + "\" class=\"infolink\">" +
                                                                       "<img src=\""+ imglink + "\" border=\"0\" alt=\"(info)\" />" +
                                                                     "</a>" ) );
        link.insertAfter( aimg );
        state.nodeAdded( aimg );
    }

}
