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
package org.apache.wiki.htmltowiki.syntax;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.jdom2.Element;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


/**
 * Wiki syntax helper operations
 */
public class MarkupHelper {

    public static void printUnescaped( final PrintWriter out, final String s ) {
        out.print( StringEscapeUtils.unescapeHtml4( s ) );
    }

    /**
     * Checks if the link points to a footnote.
     */
    public static boolean isFootnoteLink( final String ref ) {
        return ref.startsWith( "#" );
    }

    /**
     * Checks if the link points to an undefined page.
     */
    public static boolean isUndefinedPageLink( final Element a ) {
        final String classVal = a.getAttributeValue( "class" );
        return "createpage".equals( classVal );
    }

    public static boolean isHtmlBaseDiv( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) {
        return "div".equals( dto.htmlBase );
    }

    public static boolean isHtmlBaseSpan( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) {
        return "span".equals( dto.htmlBase );
    }

    /**
     *  Returns a Map containing the valid augmented wiki link attributes.
     */
    public static Map< String, String > getAugmentedWikiLinkAttributes( final Element a ) {
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

    static void addAttributeIfPresent( final Element a, final Map< String, String > attributesMap, final String attribute ) {
        final String attr = a.getAttributeValue( attribute );
        if( StringUtils.isNotEmpty( attr ) ) {
            attributesMap.put( attribute, attr.replace( "'", "\"" ) );
        }
    }

    /**
     * Converts the entries in the map to a string for use in a wiki link.
     */
    public static String augmentedWikiLinkMapToString( final Map< String, String > attributesMap ) {
        final StringBuilder sb = new StringBuilder();
        for( final Map.Entry< String, String > entry : attributesMap.entrySet() ) {
            final String attributeName = entry.getKey();
            final String attributeValue = entry.getValue();

            sb.append( " " ).append( attributeName ).append( "='" ).append( attributeValue ).append( "'" );
        }

        return sb.toString().trim();
    }

    public static String nameSansNbf( final Element e ) {
        final String name = e.getAttributeValue( "name" );
        // remove the "nbf_" that was prepended since new one will be generated again when the xhtml is rendered.
        if( name != null && name.startsWith( "nbf_" ) ) {
            return name.substring( 4 );
        }
        return name;
    }

}
