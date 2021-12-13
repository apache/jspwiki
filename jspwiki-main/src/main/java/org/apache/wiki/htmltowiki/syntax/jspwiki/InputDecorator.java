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

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Translates to JSPWiki syntax from a INPUT element.
 */
class InputDecorator {

    final PrintWriter out;
    final XHtmlElementToWikiTranslator chain;

    InputDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into JSPWiki markup.
     *
     * @param e XHTML element being translated.
     */
    void decorate( final Element e ) throws JDOMException {
        final String type = e.getAttributeValue( "type" );
        String name = e.getAttributeValue( "name" );
        final String value = e.getAttributeValue( "value" );
        final String checked = e.getAttributeValue( "checked" );

        out.print( "[{FormInput" );
        if( type != null ) {
            out.print( " type='" + type + "'" );
        }
        if( name != null ) {
            // remove the "nbf_" that was prepended since new one will be generated again when the xhtml is rendered.
            if( name.startsWith( "nbf_" ) ) {
                name = name.substring( 4 );
            }
            out.print( " name='" + name + "'" );
        }
        if( value != null && !value.equals( "" ) ) {
            out.print( " value='" + value + "'" );
        }
        if( checked != null ) {
            out.print( " checked='" + checked + "'" );
        }
        out.print( "}]" );

        chain.translate( e );
    }

}
