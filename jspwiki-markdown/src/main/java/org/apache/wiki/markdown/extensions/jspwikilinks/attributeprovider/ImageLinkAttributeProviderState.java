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
import org.apache.wiki.WikiContext;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;

import com.vladsch.flexmark.util.html.Attributes;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for image links.
 */
public class ImageLinkAttributeProviderState implements NodeAttributeProviderState< JSPWikiLink > {

    private final boolean isLinkFromText;
    private final LinkParsingOperations linkOperations;
    private final WikiContext wikiContext;
    private final String urlRef;

    public ImageLinkAttributeProviderState( final WikiContext wikiContext, final String urlRef, final boolean isLinkFromText ) {
        this.isLinkFromText = isLinkFromText;
        this.urlRef = urlRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(Attributes, Node)
     */
    @Override
    public void setAttributes( final Attributes attributes, final JSPWikiLink link ) {
        if( isLinkFromText && linkOperations.isExternalLink( link.getText().toString() ) ) {
            attributes.replaceValue( "class", MarkupParser.CLASS_EXTERNAL );
            attributes.replaceValue( "href", urlRef );
        } else if ( isLinkFromText && linkOperations.linkExists( link.getText().toString() ) ) {
            final String pagelink = wikiContext.getURL( WikiContext.VIEW, link.getText().toString() );
            attributes.replaceValue( "class", MarkupParser.CLASS_WIKIPAGE );
            attributes.replaceValue( "href", pagelink );
        }
    }

}
