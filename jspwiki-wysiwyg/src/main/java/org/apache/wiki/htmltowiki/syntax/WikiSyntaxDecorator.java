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

import org.apache.wiki.htmltowiki.SyntaxDecorator;
import org.apache.wiki.htmltowiki.WhitespaceTrimWriter;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;

import java.io.PrintWriter;
import java.util.Deque;
import java.util.Map;


/**
 * <p>Base Syntax decorator which translates to JSPWiki syntax. Delegates each kind of XHTML element to its specific
 * decorator.</p>
 * <p>This class is useful when developing a new wiki syntax decorator - just extend from it and provide the implementations of all
 * the needed Decorators on the {@link SyntaxDecorator#init(PrintWriter, Deque, Deque, WhitespaceTrimWriter, XHtmlToWikiConfig, XHtmlElementToWikiTranslator)} method</p>
 */
public abstract class WikiSyntaxDecorator implements SyntaxDecorator {

    protected ADecorator a;
    protected BrDecorator br;
    protected CodeDecorator code;
    protected DdDecorator dd;
    protected DlDecorator dl;
    protected DtDecorator dt;
    protected FormDecorator form;
    protected HrDecorator hr;
    protected H1Decorator h1;
    protected H2Decorator h2;
    protected H3Decorator h3;
    protected H4Decorator h4;
    protected ImageDecorator img;
    protected InputDecorator input;
    protected LiDecorator li;
    protected OlDecorator ol;
    protected OptionDecorator option;
    protected PDecorator p;
    protected PlainTextDecorator plainText;
    protected PlainTextBoldDecorator strong;
    protected PlainTextCssDecorator css;
    protected PlainTextCssSpecialDecorator cssStyle;
    protected PlainTextItalicDecorator em;
    protected PlainTextMonospaceDecorator pre;
    protected SelectDecorator select;
    protected StrikeDecorator strike;
    protected SubDecorator sub;
    protected SupDecorator sup;
    protected TableDecorator table;
    protected TbodyDecorator tbody;
    protected TdDecorator td;
    protected TextAreaDecorator textarea;
    protected TextElementDecorator textElement;
    protected ThDecorator th;
    protected TheadDecorator thead;
    protected TrDecorator tr;
    protected UlDecorator ul;
    protected UnderlineDecorator underline;
    protected WhitespaceTrimWriter outTrimmer;
    protected XHtmlElementToWikiTranslator chain;
    protected XHtmlToWikiConfig config;

    /** {@inheritDoc} */
    @Override
    public void a( final Element e, final String ref ) throws JDOMException {
        a.decorate( e, ref );
    }

    /** {@inheritDoc} */
    @Override
    public void aFootnote( final String text, final String ref ) {
        a.decorateFootnote( text, ref );
    }

    /** {@inheritDoc} */
    @Override
    public void aUndefined( final Element e ) throws JDOMException {
        a.decorateUndefinedLink( e );
    }

    /** {@inheritDoc} */
    @Override
    public void br( final Element base, final Element e ) throws JDOMException {
        br.decorate( base, e );
    }

    /** {@inheritDoc} */
    @Override
    public void code( final Element e ) throws JDOMException {
        code.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void dd( final Element e ) throws JDOMException {
        dd.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void dl( final Element e ) throws JDOMException {
        dl.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void dt( final Element e ) throws JDOMException {
        dt.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void em( final Element e ) throws JDOMException {
        em.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void form( final Element e ) throws JDOMException {
        form.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void hr( final Element e ) throws JDOMException {
        hr.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void h1( final Element e ) throws JDOMException {
        h1.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void h2( final Element e ) throws JDOMException {
        h2.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void h3( final Element e ) throws JDOMException {
        h3.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void h4( final Element e ) throws JDOMException {
        h4.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void image( final String src, final Map< String, Object > imageAttrs ) {
        img.decorate( src, imageAttrs );
    }

    /** {@inheritDoc} */
    @Override
    public void img( final Element e ) {
        img.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void input( final Element e ) throws JDOMException {
        input.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void li( final Element base, final Element e ) throws JDOMException {
        li.decorate( base, e );
    }

    /** {@inheritDoc} */
    @Override
    public void ol( final Element e ) throws JDOMException {
        ol.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void option( final Element base, final Element e ) throws JDOMException {
        option.decorate( base, e );
    }

    /** {@inheritDoc} */
    @Override
    public void p( final Element e ) throws JDOMException {
        p.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void paragraph( final XHtmlElementToWikiTranslator.ElementDecoratorData dto ) throws JDOMException {
        plainText.decorate( dto );
    }

    /** {@inheritDoc} */
    @Override
    public void pre( final Element e ) throws JDOMException {
        pre.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void strong( final Element e ) throws JDOMException {
        strong.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void table( final Element e ) throws JDOMException {
        table.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void tbody( final Element e ) throws JDOMException {
        tbody.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void td( final Element e ) throws JDOMException {
        td.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void text( final Text element ) {
        textElement.decorate( element );
    }

    /** {@inheritDoc} */
    @Override
    public void textarea( final Element e ) throws JDOMException {
        textarea.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void th( final Element e ) throws JDOMException {
        th.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void thead( final Element e ) throws JDOMException {
        thead.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void tr( final Element e ) throws JDOMException {
        tr.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void select( final Element e ) throws JDOMException {
        select.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void strike( final Element e ) throws JDOMException {
        strike.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void sub( final Element e ) throws JDOMException {
        sub.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void sup( final Element e ) throws JDOMException {
        sup.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void ul( final Element e ) throws JDOMException {
        ul.decorate( e );
    }

    /** {@inheritDoc} */
    @Override
    public void underline( final Element e ) throws JDOMException {
        underline.decorate( e );
    }

}
