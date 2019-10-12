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
 * {@link NodeAttributeProviderState} which sets the attributes for external links.
 */
public class ExternalLinkAttributeProviderState implements NodeAttributeProviderState< JSPWikiLink > {

    private final boolean hasRef;
    private final boolean m_useRelNofollow;
    private final WikiContext wikiContext;
    private final LinkParsingOperations linkOperations;

    public ExternalLinkAttributeProviderState( final WikiContext wikiContext, final boolean hasRef ) {
        this.hasRef = hasRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.m_useRelNofollow = wikiContext.getBooleanWikiProperty( MarkupParser.PROP_USERELNOFOLLOW, false );
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(Attributes, Node)
     */
    @Override
    public void setAttributes( final Attributes attributes, final JSPWikiLink link ) {
        if( linkOperations.isImageLink( link.getUrl().toString() ) ) {
            new ImageLinkAttributeProviderState( wikiContext, link.getText().toString(), hasRef ).setAttributes( attributes, link );
        } else {
            attributes.replaceValue( "class", MarkupParser.CLASS_EXTERNAL );
            attributes.replaceValue( "href", link.getUrl().toString() );
        }
        if( m_useRelNofollow ) {
            attributes.replaceValue( "rel", "nofollow" );
        }
    }

}
