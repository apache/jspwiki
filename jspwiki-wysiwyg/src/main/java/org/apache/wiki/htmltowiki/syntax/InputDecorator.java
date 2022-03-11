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

import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import java.io.PrintWriter;


/**
 * Translates to wiki syntax from a {@code INPUT} element.
 */
public abstract class InputDecorator {

    final protected PrintWriter out;
    final protected XHtmlElementToWikiTranslator chain;

    protected InputDecorator( final PrintWriter out, final XHtmlElementToWikiTranslator chain ) {
        this.out = out;
        this.chain = chain;
    }

    /**
     * Translates the given XHTML element into wiki markup.
     *
     * @param e XHTML element being translated.
     */
    public void decorate( final Element e ) throws JDOMException {
        final String type = e.getAttributeValue( "type" );
        final String name = MarkupHelper.nameSansNbf( e );
        final String value = e.getAttributeValue( "value" );
        final String checked = e.getAttributeValue( "checked" );

        out.print( markupInputOpen() );
        printInputAttribute( "type", type );
        printInputAttribute( "name", name );
        printInputAttribute( "value", value );
        printInputAttribute( "checked", checked );
        out.print( markupInputClose() );

        chain.translate( e );
    }

    void printInputAttribute( final String attr, final String value ) {
        if( value != null ) {
            out.print( markupForInputAttribute( attr, value ) );
        }
    }

    /**
     * Wiki markup for an {@code INPUT}'s attribute.
     *
     * @param attr attribute's name.
     * @param value attribute's value.
     * @return Opening wiki markup for an {@code INPUT} element.
     */
    protected abstract String markupForInputAttribute( String attr, String value );

    /**
     * Opening wiki markup for an {@code INPUT} element.
     *
     * @return Opening wiki markup for an {@code INPUT} element.
     */
    protected abstract String markupInputOpen();

    /**
     * Closing wiki markup for an {@code INPUT} element.
     *
     * @return Closing wiki markup for an {@code INPUT} element.
     */
    protected abstract String markupInputClose();

}
