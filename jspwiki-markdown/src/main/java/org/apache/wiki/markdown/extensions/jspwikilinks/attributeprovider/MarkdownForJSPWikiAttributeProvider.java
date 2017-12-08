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

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.util.TextUtil;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.util.html.Attributes;


/**
 * {@link AttributeProvider} for JSPWiki links.
 *
 * Acts as a factory of {@link NodeAttributeProviderState}, which are the classes setting the attributes for each concrete type of link.
 */
public class MarkdownForJSPWikiAttributeProvider implements AttributeProvider {

    protected final WikiContext wikiContext;
    protected final LinkParsingOperations linkOperations;

    public MarkdownForJSPWikiAttributeProvider( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
    }

    /**
     * {@inheritDoc}
     *
     * @see AttributeProvider#setAttributes(Node, AttributablePart, Attributes)
     */
    @Override
    public void setAttributes( final Node node, final AttributablePart part, final Attributes attributes ) {
        if( node instanceof Link ) {
            final Link link = ( Link )node;
            boolean hasRef = true;
            if( StringUtils.isEmpty( link.getUrl().toString() ) ) { // empty link == link.getText() is a wiki page
                link.setUrl( link.getText() );
                hasRef = false;
            }
            final NodeAttributeProviderState< Link > linkState;
            if( linkOperations.isExternalLink( link.getUrl().toString() ) ) {
                linkState = new ExternalLinkAttributeProviderState( wikiContext, hasRef );
            } else if( linkOperations.isInterWikiLink( link.getUrl().toString() ) ) {
                linkState = new InterWikiLinkAttributeProviderState( wikiContext, hasRef );
            } else if( StringUtils.startsWith( link.getUrl().toString(), "#" ) ) {
                linkState = new LocalFootnoteLinkAttributeProviderState( wikiContext );
            } else if( TextUtil.isNumber( link.getUrl().toString() ) ) {
                linkState = new LocalFootnoteRefLinkAttributeProviderState( wikiContext );
            } else {
                linkState = new LocalLinkAttributeProviderState( wikiContext, hasRef );
            }
            linkState.setAttributes( attributes, link );
        }
    }

}
