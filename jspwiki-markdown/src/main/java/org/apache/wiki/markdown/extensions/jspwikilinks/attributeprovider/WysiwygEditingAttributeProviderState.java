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
import com.vladsch.flexmark.util.html.Attributes;
import org.apache.wiki.WikiContext;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.markdown.nodes.JSPWikiLink;
import org.apache.wiki.render.WikiRenderer;


/**
 * {@link NodeAttributeProviderState} to finish up polishing WYSIWYG editing mode. More or less equivalent to WysiwygEditingRenderer, the main difference
 * being that in here there isn't any node removal, those nodes are simply not inserted elsewhere if WYSIWYG editing is detected.
 */
public class WysiwygEditingAttributeProviderState implements NodeAttributeProviderState< JSPWikiLink > {

    private final WikiContext wikiContext;
    private final boolean m_wysiwygEditorMode;

    public WysiwygEditingAttributeProviderState( final WikiContext wikiContext ) {
        this.wikiContext = wikiContext;
        final Boolean wysiwygVariable = ( Boolean )wikiContext.getVariable( WikiContext.VAR_WYSIWYG_EDITOR_MODE );
        m_wysiwygEditorMode = wysiwygVariable != null ? wysiwygVariable : false;
    }

    /**
     * {@inheritDoc}
     *
     * @see NodeAttributeProviderState#setAttributes(Attributes, Node)
     */
    @Override
    public void setAttributes( final Attributes attributes, final JSPWikiLink link ) {
        if( m_wysiwygEditorMode ) {
            if( attributes.getValue( "class" ) != null ) {
                final String href = attributes.getValue( "href" );
                final XHtmlToWikiConfig wikiConfig = new XHtmlToWikiConfig( wikiContext );
                // Get the url for wiki page link - it's typically "Wiki.jsp?page=MyPage"
                // or when using the ShortURLConstructor option, it's "wiki/MyPage" .
                final String wikiPageLinkUrl = wikiConfig.getWikiJspPage();
                final String editPageLinkUrl = wikiConfig.getEditJspPage();
                if( href != null && href.startsWith( wikiPageLinkUrl ) ) {
                    // Remove the leading url string so that users will only see the wikipage's name when editing an existing wiki link.
                    // For example, change "Wiki.jsp?page=MyPage" to just "MyPage".
                    String newHref = href.substring( wikiPageLinkUrl.length() );

                    // Convert "This%20Pagename%20Has%20Spaces" to "This Pagename Has Spaces"
                    newHref = wikiContext.getEngine().decodeName( newHref );

                    // Handle links with section anchors.
                    // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                    // to this wiki string: "TargetPage#Heading2".
                    attributes.replaceValue( "href", newHref.replaceFirst( WikiRenderer.LINKS_SOURCE, WikiRenderer.LINKS_TRANSLATION ) );
                } else if( href != null && href.startsWith( editPageLinkUrl ) ) {
                    final String title = attributes.getValue( "title" );
                    if( title != null ) {
                        // remove the title since we don't want to eventually save the default undefined page title.
                        attributes.replaceValue( "title", "" );
                    }

                    String newHref = href.substring( editPageLinkUrl.length() );
                    newHref = wikiContext.getEngine().decodeName( newHref );

                    attributes.replaceValue( "href", newHref );
                }
            }
        }
    }

}
