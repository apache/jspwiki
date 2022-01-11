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
package org.apache.wiki.markdown.extensions.jspwikilinks.attributeprovider;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.MutableAttributes;
import com.vladsch.flexmark.util.sequence.CharSubSequence;
import org.apache.oro.text.regex.Pattern;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;

import java.util.List;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for local links.
 */
public class LocalLinkAttributeProviderState implements NodeAttributeProviderState< JSPWikiLink > {

    private final boolean hasRef;
    private final Context wikiContext;
    private final LinkParsingOperations linkOperations;
    private final boolean isImageInlining;
    private final List< Pattern > inlineImagePatterns;

    public LocalLinkAttributeProviderState( final Context wikiContext,
                                            final boolean hasRef,
                                            final boolean isImageInlining,
                                            final List< Pattern > inlineImagePatterns ) {
        this.hasRef = hasRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.isImageInlining = isImageInlining;
        this.inlineImagePatterns = inlineImagePatterns;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(MutableAttributes, Node)
     */
    @Override
    public void setAttributes( final MutableAttributes attributes, final JSPWikiLink link ) {
        final int hashMark = link.getUrl().toString().indexOf( '#' );
        final String attachment = wikiContext.getEngine().getManager( AttachmentManager.class ).getAttachmentInfoName( wikiContext, link.getWikiLink() );
        if( attachment != null ) {
            if( !linkOperations.isImageLink( link.getUrl().toString(), isImageInlining, inlineImagePatterns ) ) {
                attributes.replaceValue( "class", MarkupParser.CLASS_ATTACHMENT );
                final String attlink = wikiContext.getURL( ContextEnum.PAGE_ATTACH.getRequestContext(), link.getWikiLink() );
                attributes.replaceValue( "href", attlink );
            } else {
                new ImageLinkAttributeProviderState( wikiContext, attachment, hasRef ).setAttributes( attributes, link );
            }
        } else if( hashMark != -1 ) { // It's an internal Wiki link, but to a named section
            final String namedSection = link.getUrl().toString().substring( hashMark + 1 );
            final String matchedLink = linkOperations.linkIfExists( link.getUrl().toString() );
            if( matchedLink != null ) {
                String sectref = "#section-" + wikiContext.getEngine().encodeName( matchedLink + "-" + MarkupParser.wikifyLink( namedSection ) );
                sectref = sectref.replace('%', '_');
                link.setUrl( CharSubSequence.of( link.getUrl().toString() + sectref ) );
                new LocalReadLinkAttributeProviderState( wikiContext ).setAttributes( attributes, link );
            } else {
                new LocalEditLinkAttributeProviderState( wikiContext, link.getWikiLink() ).setAttributes( attributes, link );
            }
        } else {
            if( linkOperations.linkExists( link.getWikiLink() ) ) {
                new LocalReadLinkAttributeProviderState( wikiContext ).setAttributes( attributes, link );
            } else {
                new LocalEditLinkAttributeProviderState( wikiContext, link.getWikiLink() ).setAttributes( attributes, link );
            }
        }
    }

}
