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
import org.apache.wiki.parser.MarkupParser;


/**
 * {@link NodePostProcessorState} which further post processes local links.
 */
public class LocalLinkNodePostProcessorState implements NodePostProcessorState< JSPWikiLink > {

    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;

    public LocalLinkNodePostProcessorState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodePostProcessorState#process(NodeTracker, Node) 
     */
    @Override
    public void process( final NodeTracker state, final JSPWikiLink link ) {
        final int hashMark = link.getUrl().toString().indexOf( '#' );
        final String attachment = wikiContext.getEngine().getAttachmentManager().getAttachmentInfoName( wikiContext, link.getUrl().toString() );
        if( attachment != null  ) {
            if( !linkOperations.isImageLink( link.getUrl().toString() ) ) {
                final String attlink = wikiContext.getURL( WikiContext.ATTACH, link.getUrl().toString() );
                link.setUrl( CharSubSequence.of( attlink ) );
                link.removeChildren();
                final HtmlInline content = new HtmlInline( CharSubSequence.of( link.getText().toString() ) );
                link.appendChild( content );
                state.nodeAddedWithChildren( content );
                addAttachmentLink( state, link );
            } else {
                new ImageLinkNodePostProcessorState( wikiContext, attachment, link.hasRef() ).process( state, link );
            }
        } else if( hashMark != -1 ) { // It's an internal Wiki link, but to a named section
            final String namedSection = link.getUrl().toString().substring( hashMark + 1 );
            link.setUrl( CharSubSequence.of( link.getUrl().toString().substring( 0, hashMark ) ) );
            final String matchedLink = linkOperations.linkIfExists( link.getUrl().toString() );
            if( matchedLink != null ) {
                String sectref = "#section-" + wikiContext.getEngine().encodeName( matchedLink + "-" + MarkupParser.wikifyLink( namedSection ) );
                sectref = sectref.replace('%', '_');
                link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.VIEW, link.getUrl().toString() + sectref ) ) );
            } else {
                link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.EDIT, link.getUrl().toString() ) ) );
            }
        } else {
            if( linkOperations.linkExists( link.getUrl().toString() ) ) {
                link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.VIEW, link.getUrl().toString() ) ) );
            } else {
                link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.EDIT, link.getUrl().toString() ) ) );
            }
        }
    }

    void addAttachmentLink( final NodeTracker state, final JSPWikiLink link ) {
        final String infolink = wikiContext.getURL( WikiContext.INFO, link.getWikiLink() );
        final String imglink = wikiContext.getURL( WikiContext.NONE, "images/attachment_small.png" );
        final HtmlInline aimg = new HtmlInline( CharSubSequence.of( "<a href=\""+ infolink + "\" class=\"infolink\">" +
                                                                       "<img src=\""+ imglink + "\" border=\"0\" alt=\"(info)\" />" +
                                                                     "</a>" ) );
        link.insertAfter( aimg );
        state.nodeAdded( aimg );
    }

}
