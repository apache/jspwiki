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

import java.text.MessageFormat;
import java.util.ResourceBundle;

import org.apache.wiki.WikiContext;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.preferences.Preferences;

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.sequence.CharSubSequence;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for local edit links.
 */
public class LocalEditLinkAttributeProviderState implements NodeAttributeProviderState< Link > {

    private final WikiContext wikiContext;
    private final String url;

    public LocalEditLinkAttributeProviderState( final WikiContext wikiContext, final String url ) {
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
        final ResourceBundle rb = Preferences.getBundle( wikiContext, InternationalizationManager.CORE_BUNDLE );
        attributes.replaceValue( "title", MessageFormat.format( rb.getString( "markupparser.link.create" ), url ) );
        attributes.replaceValue( "class", MarkupParser.CLASS_EDITPAGE );
        link.setUrl( CharSubSequence.of( wikiContext.getURL( WikiContext.EDIT, url ) ) );
        attributes.replaceValue( "href", link.getUrl().toString() );
    }

}
