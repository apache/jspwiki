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

import org.apache.wiki.WikiContext;
import org.apache.wiki.parser.MarkupParser;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for local read links.
 */
public class LocalReadLinkAttributeProviderState implements NodeAttributeProviderState< Link > {

    private final WikiContext wikiContext;
    private final String url;

    public LocalReadLinkAttributeProviderState( final WikiContext wikiContext, final String url ) {
        this.wikiContext = wikiContext;
        this.url = url;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(Attributes, Link)
     */
    @Override
    public void setAttributes( final Attributes attributes, final Link link ) {
        attributes.replaceValue( "class", MarkupParser.CLASS_WIKIPAGE );
        link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.VIEW, url ) ) );
        attributes.replaceValue( "href", link.getUrl().toString() );
    }

}
