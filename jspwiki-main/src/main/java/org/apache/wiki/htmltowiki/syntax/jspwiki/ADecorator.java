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
package org.apache.wiki.htmltowiki.syntax.jspwiki;

import org.apache.commons.lang3.StringUtils;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * Translates to JSPWiki syntax from an A element.
 */
class ADecorator {

    final PrintWriter out;
    final XHtmlToWikiConfig config;
    final XHtmlElementToWikiTranslator chain;

    ADecorator( final PrintWriter out, final XHtmlToWikiConfig config, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.config = config;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param e XHTML element being translated.
     */
    void decorate( final Element e ) throws JDOMException {
        if( config.isNotIgnorableWikiMarkupLink( e ) ) {
            if( e.getChild( "IMG" ) != null ) {
                chain.translateImage( e );
            } else {
                String ref = e.getAttributeValue( "href" );
                if ( ref == null ) {
                    if ( isUndefinedPageLink( e ) ) {
                        out.print( "[" );
                        chain.translate( e );
                        out.print( "]" );
                    } else {
                        chain.translate( e );
                    }
                } else {
                    ref = config.trimLink( ref );
                    if ( ref != null ) {
                        if ( ref.startsWith( "#" ) ) { // This is a link to a footnote.
                            // convert "#ref-PageName-1" to just "1"
                            final String href = ref.replaceFirst( "#ref-.+-(\\d+)", "$1" );

                            // remove the brackets around "[1]"
                            final String textValue = e.getValue().substring( 1, ( e.getValue().length() - 1 ) );

                            if ( href.equals( textValue ) ) { // handles the simplest case. Example: [1]
                                chain.translate( e );
                            } else { // handles the case where the link text is different from the href. Example: [something|1]
                                out.print( "[" + textValue + "|" + href + "]" );
                            }
                        } else {
                            final Map< String, String > augmentedWikiLinkAttributes = getAugmentedWikiLinkAttributes( e );

                            out.print( "[" );
                            chain.translate( e );
                            if ( !e.getTextTrim().equalsIgnoreCase( ref ) ) {
                                out.print( "|" );
                                MarkupHelper.printUnescaped( out, ref );

                                if ( !augmentedWikiLinkAttributes.isEmpty() ) {
                                    out.print( "|" );

                                    final String augmentedWikiLink = augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
                                    out.print( augmentedWikiLink );
                                }
                            } else if ( !augmentedWikiLinkAttributes.isEmpty() ) {
                                // If the ref has the same value as the text and also if there
                                // are attributes, then just print: [ref|ref|attributes] .
                                out.print( "|" + ref + "|" );
                                final String augmentedWikiLink = augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
                                out.print( augmentedWikiLink );
                            }

                            out.print( "]" );
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the link points to an undefined page.
     */
    private boolean isUndefinedPageLink( final Element a ) {
        final String classVal = a.getAttributeValue( "class" );
        return "createpage".equals( classVal );
    }

    /**
     *  Returns a Map containing the valid augmented wiki link attributes.
     */
    private Map< String, String > getAugmentedWikiLinkAttributes( final Element a ) {
        final Map< String, String > attributesMap = new HashMap<>();
        final String cssClass = a.getAttributeValue( "class" );
        if( StringUtils.isNotEmpty( cssClass )
                && !cssClass.matches( "wikipage|createpage|external|interwiki|attachment" ) ) {
            attributesMap.put( "class", cssClass.replace( "'", "\"" ) );
        }
        addAttributeIfPresent( a, attributesMap, "accesskey" );
        addAttributeIfPresent( a, attributesMap, "charset" );
        addAttributeIfPresent( a, attributesMap, "dir" );
        addAttributeIfPresent( a, attributesMap, "hreflang" );
        addAttributeIfPresent( a, attributesMap, "id" );
        addAttributeIfPresent( a, attributesMap, "lang" );
        addAttributeIfPresent( a, attributesMap, "rel" );
        addAttributeIfPresent( a, attributesMap, "rev" );
        addAttributeIfPresent( a, attributesMap, "style" );
        addAttributeIfPresent( a, attributesMap, "tabindex" );
        addAttributeIfPresent( a, attributesMap, "target" );
        addAttributeIfPresent( a, attributesMap, "title" );
        addAttributeIfPresent( a, attributesMap, "type" );
        return attributesMap;
    }

    private void addAttributeIfPresent( final Element a, final Map< String, String > attributesMap, final String attribute ) {
        final String attr = a.getAttributeValue( attribute );
        if( StringUtils.isNotEmpty( attr ) ) {
            attributesMap.put( attribute, attr.replace( "'", "\"" ) );
        }
    }

    /**
     * Converts the entries in the map to a string for use in a wiki link.
     */
    private String augmentedWikiLinkMapToString( final Map< String, String > attributesMap ) {
        final StringBuilder sb = new StringBuilder();
        for( final Map.Entry< String, String > entry : attributesMap.entrySet() ) {
            final String attributeName = entry.getKey();
            final String attributeValue = entry.getValue();

            sb.append( " " ).append( attributeName ).append( "='" ).append( attributeValue ).append( "'" );
        }

        return sb.toString().trim();
    }

}
