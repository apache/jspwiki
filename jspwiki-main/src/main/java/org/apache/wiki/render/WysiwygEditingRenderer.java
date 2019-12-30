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
package org.apache.wiki.render;

import org.apache.wiki.WikiContext;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.parser.WikiDocument;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

/**
 *  Implements a WikiRenderer that outputs XHTML in a format that is suitable for use by a WYSIWYG XHTML editor.
 *
 *  @since  2.5
 */
public class WysiwygEditingRenderer extends WikiRenderer {

    private static final String A_ELEMENT = "a";
    private static final String IMG_ELEMENT = "img";
    // private static final String PRE_ELEMENT = "pre";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String TITLE_ATTRIBUTE = "title";
    private static final String LINEBREAK = "\n";

    /**
     *  Creates a WYSIWYG editing renderer.
     *
     *  @param context A WikiContext in which the rendering will take place.
     *  @param doc The WikiDocument which shall be rendered.
     */
    public WysiwygEditingRenderer( final WikiContext context, final WikiDocument doc )
    {
        super( context, doc );
    }

    /*
     * Recursively walk the XHTML DOM tree and manipulate specific elements to
     * make them better for WYSIWYG editing.
     */
    private void processChildren( final Element baseElement ) {
        for( final Iterator< Element > itr = baseElement.getChildren().iterator(); itr.hasNext(); ) {
            final Element element = itr.next();
            final String elementName = element.getName().toLowerCase();
            final Attribute classAttr = element.getAttribute( CLASS_ATTRIBUTE );

            if( elementName.equals( A_ELEMENT ) ) {
                if( classAttr != null ) {
                    final String classValue = classAttr.getValue();
                    final Attribute hrefAttr = element.getAttribute( HREF_ATTRIBUTE );
                    final XHtmlToWikiConfig wikiConfig = new XHtmlToWikiConfig( m_context );

                    // Get the url for wiki page link - it's typically "Wiki.jsp?page=MyPage"
                    // or when using the ShortURLConstructor option, it's "wiki/MyPage" .
                    final String wikiPageLinkUrl = wikiConfig.getWikiJspPage();
                    final String editPageLinkUrl = wikiConfig.getEditJspPage();

                    //if( classValue.equals( WIKIPAGE )
                    //    || ( hrefAttr != null && hrefAttr.getValue().startsWith( wikiPageLinkUrl ) ) )
                    if( //classValue.equals( WIKIPAGE ) &&
                        ( hrefAttr != null ) &&  ( hrefAttr.getValue().startsWith( wikiPageLinkUrl ) ) ) {
                        // Remove the leading url string so that users will only see the
                        // wikipage's name when editing an existing wiki link.
                        // For example, change "Wiki.jsp?page=MyPage" to just "MyPage".

                        String newHref = hrefAttr.getValue().substring( wikiPageLinkUrl.length() );

                        // Convert "This%20Pagename%20Has%20Spaces" to "This Pagename Has Spaces"
                        newHref = m_context.getEngine().decodeName( newHref );

                        // Handle links with section anchors.
                        // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                        // to this wiki string: "TargetPage#Heading2".
                        hrefAttr.setValue( newHref.replaceFirst( LINKS_SOURCE, LINKS_TRANSLATION ) );

                    } else if( //classValue.equals( EDITPAGE ) &&
                            ( hrefAttr != null ) && ( hrefAttr.getValue().startsWith( editPageLinkUrl ) ) ) {

                        final Attribute titleAttr = element.getAttribute( TITLE_ATTRIBUTE );
                        if( titleAttr != null ) {
                                // remove the title since we don't want to eventually save the default undefined page title.
                                titleAttr.detach();
                        }

                        String newHref = hrefAttr.getValue().substring( editPageLinkUrl.length() );
                        newHref = m_context.getEngine().decodeName( newHref );

                        hrefAttr.setValue( newHref );
                    } else if( classValue.equals( MarkupParser.HASHLINK ) ) {
                        itr.remove(); //remove element without disturbing the ongoing iteration
                        continue;  //take next iteration of the for loop
                    }
                }
            // end of check for "a" element
            } else if ( elementName.equals( IMG_ELEMENT ) ) {
                if( classAttr != null ) {
                    final String classValue = classAttr.getValue();
                    if( classValue.equals( MarkupParser.OUTLINK ) ) {
                        itr.remove(); // remove element without disturbing the ongoing iteration
                        continue; // take next iteration of the for loop
                    }
                }
            }

            processChildren( element );
        }
    }

    /**
     *  {@inheritDoc}
     */
    public String getString() throws IOException {
        final Element rootElement = m_document.getRootElement();
        processChildren( rootElement );

        m_document.setContext( m_context );

        final CustomXMLOutputProcessor processor = new CustomXMLOutputProcessor();
        final XMLOutputter output = new XMLOutputter(processor);
        final StringWriter out = new StringWriter();
        final Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator( LINEBREAK );

        output.setFormat( fmt );
        output.outputElementContent( m_document.getRootElement(), out );

        return out.toString();
    }

}
