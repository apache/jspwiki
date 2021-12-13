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

import org.apache.wiki.htmltowiki.SyntaxDecorator;
import org.apache.wiki.htmltowiki.WhitespaceTrimWriter;
import org.apache.wiki.htmltowiki.XHtmlElementToWikiTranslator;
import org.apache.wiki.htmltowiki.XHtmlToWikiConfig;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;

import java.io.PrintWriter;
import java.util.Map;
import java.util.Stack;


/**
 * Syntax decorator which translates to JSPWiki syntax. Delegates each kind of XHTML element to its specific decorator.
 */
public class JSPWikiSyntaxDecorator implements SyntaxDecorator {

    ADecorator a;
    BrDecorator br;
    CodeDecorator code;
    DdDecorator dd;
    DlDecorator dl;
    DtDecorator dt;
    FormDecorator form;
    HrDecorator hr;
    H1Decorator h1;
    H2Decorator h2;
    H3Decorator h3;
    H4Decorator h4;
    ImageDecorator img;
    InputDecorator input;
    LiDecorator li;
    OlDecorator ol;
    OptionDecorator option;
    PDecorator p;
    PlainTextDecorator plainText;
    PlainTextBoldDecorator strong;
    PlainTextItalicDecorator em;
    PlainTextMonospaceDecorator pre;
    TableDecorator table;
    TdDecorator td;
    TextAreaDecorator textarea;
    TextElementDecorator textElement;
    ThDecorator th;
    TrDecorator tr;
    SelectDecorator select;
    StrikeDecorator strike;
    SubDecorator sub;
    SupDecorator sup;
    UlDecorator ul;
    UnderlineDecorator underline;
    WhitespaceTrimWriter outTrimmer;
    XHtmlElementToWikiTranslator chain;
    XHtmlToWikiConfig config;

    /** {@inheritDoc} */
    @Override
    public void init( final PrintWriter out,
                      final Stack< String > liStack,
                      final Stack< String > preStack,
                      final WhitespaceTrimWriter outTrimmer,
                      final XHtmlToWikiConfig config,
                      final XHtmlElementToWikiTranslator chain ) {
        this.config = config;
        this.outTrimmer = outTrimmer;
        this.chain = chain;

        this.a = new ADecorator( out, config, chain );
        this.br = new BrDecorator( out, preStack, chain );
        this.code = new CodeDecorator( out, preStack, chain );
        this.dd = new DdDecorator( out, chain );
        this.dl = new DlDecorator( out, chain );
        this.dt = new DtDecorator( out, chain );
        this.em = new PlainTextItalicDecorator( out, preStack, chain );
        this.form = new FormDecorator( out, chain );
        this.hr = new HrDecorator( out, chain );
        this.h1 = new H1Decorator( out, chain );
        this.h2 = new H2Decorator( out, chain );
        this.h3 = new H3Decorator( out, chain );
        this.h4 = new H4Decorator( out, chain );
        this.img = new ImageDecorator( out, config );
        this.input = new InputDecorator( out, chain );
        this.li = new LiDecorator( out, liStack, chain );
        this.ol = new OlDecorator( out, liStack, chain );
        this.option = new OptionDecorator( out, chain );
        this.p = new PDecorator( out, chain );
        this.plainText = new PlainTextDecorator( out, preStack, chain );
        this.pre = new PlainTextMonospaceDecorator( out, preStack, chain );
        this.strong = new PlainTextBoldDecorator( out, preStack, chain );
        this.table = new TableDecorator( out, outTrimmer, chain );
        this.td = new TdDecorator( out, preStack, chain );
        this.th = new ThDecorator( out, preStack, chain );
        this.tr = new TrDecorator( out, chain );
        this.textarea = new TextAreaDecorator( out, chain );
        this.textElement = new TextElementDecorator( out, preStack );
        this.select = new SelectDecorator( out, chain );
        this.strike = new StrikeDecorator( out, chain );
        this.sub = new SubDecorator( out, chain );
        this.sup = new SupDecorator( out, chain );
        this.ul = new UlDecorator( out, liStack, chain );
        this.underline = new UnderlineDecorator( out, chain );
    }

    /** {@inheritDoc} */
    @Override
    public void a( final Element e ) throws JDOMException {
        a.decorate( e );
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
