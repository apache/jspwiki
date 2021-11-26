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

import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.html.MutableAttributes;
import org.apache.oro.text.regex.Pattern;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.parser.LinkParsingOperations;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.util.TextUtil;

import java.util.List;


/**
 * {@link NodeAttributeProviderState} which sets the attributes for interwiki links.
 */
public class InterWikiLinkAttributeProviderState implements NodeAttributeProviderState< JSPWikiLink > {

    private final boolean hasRef;
    private final boolean m_wysiwygEditorMode;
    private final Context wikiContext;
    private final LinkParsingOperations linkOperations;
    private final boolean isImageInlining;
    private final List< Pattern > inlineImagePatterns;

    public InterWikiLinkAttributeProviderState( final Context wikiContext,
                                                final boolean hasRef,
                                                final boolean isImageInlining,
                                                final List< Pattern > inlineImagePatterns ) {
        this.hasRef = hasRef;
        this.wikiContext = wikiContext;
        this.linkOperations = new LinkParsingOperations( wikiContext );
        this.isImageInlining = isImageInlining;
        this.inlineImagePatterns = inlineImagePatterns;
        final Boolean wysiwygVariable = wikiContext.getVariable( Context.VAR_WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null ? wysiwygVariable : false;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(MutableAttributes, Node)
     */
    @Override
    public void setAttributes( final MutableAttributes attributes, final JSPWikiLink link ) {
        final String[] refAndPage = link.getWikiLink().split( ":" );
        if( !m_wysiwygEditorMode ) {
            String urlReference = wikiContext.getEngine().getInterWikiURL( refAndPage[ 0 ] );
            if( urlReference != null ) {
                urlReference = TextUtil.replaceString( urlReference, "%s", refAndPage[ 1 ] );
                if( linkOperations.isImageLink( urlReference, isImageInlining, inlineImagePatterns ) ) {
                    new ImageLinkAttributeProviderState( wikiContext, urlReference, hasRef ).setAttributes( attributes, link );
                } else {
                    setInterWikiLinkAttrs( attributes, link, urlReference );
                }
            }
        } else {
            setInterWikiLinkAttrs( attributes, link, refAndPage[0] + ":" + refAndPage[1] );
        }
    }

    void setInterWikiLinkAttrs( final MutableAttributes attributes, final Link link, final String url ) {
        attributes.replaceValue( "class", MarkupParser.CLASS_INTERWIKI );
        attributes.replaceValue( "href", url );
    }

}
