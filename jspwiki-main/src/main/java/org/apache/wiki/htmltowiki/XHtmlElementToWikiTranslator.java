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
package org.apache.wiki.htmltowiki;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.htmltowiki.syntax.MarkupHelper;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.XmlUtil;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.xpath.XPathFactory;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Converting XHtml to Wiki Markup.  This is the class which orchestrates all the heavy loading.
 */
public class XHtmlElementToWikiTranslator {

    private static final Logger LOG = LogManager.getLogger( XHtmlElementToWikiTranslator.class );
    private static final String DEFAULT_SYNTAX_DECORATOR = "org.apache.wiki.htmltowiki.syntax.jspwiki.JSPWikiSyntaxDecorator";

    private final Engine e;
    private final XHtmlToWikiConfig config;
    private final WhitespaceTrimWriter outTrimmer = new WhitespaceTrimWriter();
    private final SyntaxDecorator syntax;

    /**
     *  Create a new translator using the default config.
     *
     *  @param base The base element from which to start translating.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator( final Engine e, final Element base ) throws JDOMException, ReflectiveOperationException {
        this( e, base, new XHtmlToWikiConfig() );
    }

    /**
     *  Create a new translator using the specified config.
     *
     *  @param base The base element from which to start translating.
     *  @param config The config to use.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator( final Engine e, final Element base, final XHtmlToWikiConfig config ) throws JDOMException, ReflectiveOperationException {
        this.e = e;
        this.config = config;
        syntax = getSyntaxDecorator();
        final PrintWriter out = new PrintWriter( outTrimmer );
        final Deque< String > liStack = new ArrayDeque<>();
        final Deque< String > preStack = new PreDeque();

        syntax.init( out, liStack, preStack, outTrimmer, config, this );
        translate( base );
    }

    SyntaxDecorator getSyntaxDecorator() throws ReflectiveOperationException {
        String sdClass = e.getWikiProperties().getProperty( "jspwiki.syntax.decorator", DEFAULT_SYNTAX_DECORATOR );
        if( !ClassUtil.assignable( sdClass, SyntaxDecorator.class.getName() ) ) {
            LOG.warn( "{} does not subclass {} reverting to default syntax decorator.", sdClass, SyntaxDecorator.class.getName() );
            sdClass = DEFAULT_SYNTAX_DECORATOR;
        }
        LOG.info( "Using {} as markup parser.", sdClass );
        return ClassUtil.buildInstance( sdClass );
    }

    /**
     * Outputs parsed wikitext.
     *
     * @return parsed wikitext.
     */
    public String getWikiString() {
        return outTrimmer.toString();
    }

    public void translate( final Content element ) throws JDOMException {
        if( element instanceof Text ) {
            translateText( ( Text ) element );
        } else if( element instanceof Element ) {
            final Element base = ( Element )element;
            if( "imageplugin".equals( base.getAttributeValue( "class" ) ) ) {
                translateImage( base );
            } else if( "wikiform".equals( base.getAttributeValue( "class" ) ) ) {
                // only print the children if the div's class="wikiform", but not the div itself.
                translateChildren( base );
            } else {
                translateParagraph( base );
            }
        }
    }

    public void translateText( final Text element ) {
        syntax.text( element );
    }

    public void translateImage( final Element base ) {
        Element child = XmlUtil.getXPathElement( base, "TBODY/TR/TD/*" );
        if( child == null ) {
            child = base;
        }
        final Element img;
        final String href;
        if( child.getName().equals( "A" ) ) {
            img = child.getChild( "IMG" );
            href = child.getAttributeValue( "href" );
        } else {
            img = child;
            href = null;
        }
        if( img == null ) {
            return;
        }
        final String src = config.trimLink( img.getAttributeValue( "src" ) );
        if( src == null ) {
            return;
        }

        final Map< String, Object > imageAttrs = new LinkedHashMap<>();
        putIfNotEmpty( imageAttrs, "align", base.getAttributeValue( "align" ) );
        putIfNotEmpty( imageAttrs, "height", img.getAttributeValue( "height" ) );
        putIfNotEmpty( imageAttrs, "width", img.getAttributeValue( "width" ) );
        putIfNotEmpty( imageAttrs, "alt", img.getAttributeValue( "alt" ) );
        putIfNotEmpty( imageAttrs, "caption", emptyToNull( ( Element )XPathFactory.instance().compile(  "CAPTION" ).evaluateFirst( base ) ) );
        putIfNotEmpty( imageAttrs, "link", href );
        putIfNotEmpty( imageAttrs, "border", img.getAttributeValue( "border" ) );
        putIfNotEmpty( imageAttrs, "style", base.getAttributeValue( "style" ) );
        syntax.image( src, imageAttrs );
    }

    private void putIfNotEmpty( final Map< String, Object > map, final String key, final Object value ) {
        if( value != null ) {
            map.put( key, value );
        }
    }

    private String emptyToNull( final Element e ) {
        if( e == null ) {
            return null;
        }
        final String s = e.getText();
        return s == null ? null : ( s.replaceAll( "\\s", "" ).isEmpty() ? null : s );
    }

    private Map< Object, Object > getStylePropertiesLowerCase( final Element base ) {
        final String n = base.getName().toLowerCase();

        // "font-weight: bold; font-style: italic;"
        String style = base.getAttributeValue( "style" );
        if( style == null ) {
            style = "";
        }

        if( n.equals( "p" ) || n.equals( "div" ) ) {
            final String align = base.getAttributeValue( "align" );
            if( align != null ) {
                // only add the value of the align attribute if the text-align style didn't already exist.
                if( !style.contains( "text-align" ) ) {
                    style += ";text-align:" + align + ";";
                }
            }
        }

        if( n.equals( "font" ) ) {
            final String color = base.getAttributeValue( "color" );
            final String face = base.getAttributeValue( "face" );
            final String size = base.getAttributeValue( "size" );
            if( color != null ) {
                style = style + "color:" + color + ";";
            }
            if( face != null ) {
                style = style + "font-family:" + face + ";";
            }
            if( size != null ) {
                switch ( size ) {
                    case "1": style += "font-size:xx-small;"; break;
                    case "2": style += "font-size:x-small;"; break;
                    case "3": style += "font-size:small;"; break;
                    case "4": style += "font-size:medium;"; break;
                    case "5": style += "font-size:large;"; break;
                    case "6": style += "font-size:x-large;"; break;
                    case "7": style += "font-size:xx-large;"; break;
                }
            }
        }

        if( style.equals( "" ) ) {
            return null;
        }

        final Map< Object, Object > m = new LinkedHashMap<>();
        Arrays.stream( style.toLowerCase().split( ";" ) )
              .filter( StringUtils::isNotEmpty )
              .forEach( prop -> m.put( prop.split( ":" )[ 0 ].trim(), prop.split( ":" )[ 1 ].trim() ) );
        return m;
    }

    private String propsToStyleString( final Map< Object, Object >  styleProps ) {
        final StringBuilder style = new StringBuilder();
        for( final Map.Entry< Object, Object > entry : styleProps.entrySet() ) {
            style.append( " " ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( ";" );
        }
        return style.toString();
    }

    public void translateChildren( final Element base ) throws JDOMException {
        for( final Content c : base.getContent() ) {
            if( c instanceof Element ) {
                final Element e = ( Element )c;
                final String n = e.getName().toLowerCase();
                switch( n ) {
                    case "h1": syntax.h1( e ); break;
                    case "h2": syntax.h2( e ); break;
                    case "h3": syntax.h3( e ); break;
                    case "h4": syntax.h4( e ); break;
                    case "p": syntax.p( e ); break;
                    case "br": syntax.br( base, e ); break;
                    case "hr": syntax.hr( e ); break;
                    case "table": syntax.table( e ); break;
                    case "tbody": syntax.tbody( e ); break;
                    case "tr": syntax.tr( e ); break;
                    case "td": syntax.td( e ); break;
                    case "thead": syntax.thead( e ); break;
                    case "th": syntax.th( e ); break;
                    case "a": translateA( e ); break;
                    case "b":
                    case "strong": syntax.strong( e ); break;
                    case "i":
                    case "em":
                    case "address": syntax.em( e ); break;
                    case "u": syntax.underline( e ); break;
                    case "strike": syntax.strike( e ); break;
                    case "sub": syntax.sub( e ); break;
                    case "sup": syntax.sup( e ); break;
                    case "dl": syntax.dl( e ); break;
                    case "dt": syntax.dt( e ); break;
                    case "dd": syntax.dd( e ); break;
                    case "ul": syntax.ul( e ); break;
                    case "ol": syntax.ol( e ); break;
                    case "li": syntax.li( base, e ); break;
                    case "pre": syntax.pre( e ); break;
                    case "code":
                    case "tt": syntax.code( e ); break;
                    case "img": syntax.img( e ); break;
                    case "form": syntax.form( e ); break;
                    case "input": syntax.input( e ); break;
                    case "textarea": syntax.textarea( e ); break;
                    case "select": syntax.select( e ); break;
                    case "option": syntax.option( base, e ); break;
                    default: translate( e ); break;
                }
            } else {
                translate( c );
            }
        }
    }

    void translateA( final Element e ) throws JDOMException {
        if( config.isNotIgnorableWikiMarkupLink( e ) ) {
            if( e.getChild( "IMG" ) != null ) {
                translateImage( e );
            } else {
                final String ref = config.trimLink( e.getAttributeValue( "href" ) );
                if( ref == null ) {
                    if( MarkupHelper.isUndefinedPageLink( e ) ) {
                        syntax.aUndefined( e );
                    } else {
                        translate( e );
                    }
                } else if( MarkupHelper.isFootnoteLink( ref ) ) {
                    final String href = ref.replaceFirst( "#ref-.+-(\\d+)", "$1" ); // convert "#ref-PageName-1" to just "1"
                    final String textValue = e.getValue().substring( 1, ( e.getValue().length() - 1 ) ); // remove the brackets around "[1]"
                    syntax.aFootnote( textValue, href );
                } else {
                    syntax.a( e, ref );
                }
            }
        }
    }

    public void translateParagraph( final Element base ) throws JDOMException {
        final ElementDecoratorData dto = buildElementDecoratorDataFrom( base );
        syntax.paragraph( dto );
    }

    ElementDecoratorData buildElementDecoratorDataFrom( final Element base ) {
        String n = base.getName().toLowerCase();
        boolean bold = false;
        boolean italic = false;
        boolean monospace = false;
        String cssSpecial = null;
        final String cssClass = base.getAttributeValue( "class" );

        // accomodate a FCKeditor bug with Firefox: when a link is removed, it becomes <span class="wikipage">text</span>.
        final boolean ignoredCssClass = cssClass != null && cssClass.matches( "wikipage|createpage|external|interwiki|attachment|inline-code" );

        Map< Object, Object > styleProps = null;

        // Only get the styles if it's not a link element. Styles for link elements are handled as an AugmentedWikiLink instead.
        if( !n.equals( "a" ) ) {
            styleProps = getStylePropertiesLowerCase( base );
        }

        if( "inline-code".equals( cssClass ) ) {
            monospace = true;
        }

        if( styleProps != null ) {
            final String weight = ( String ) styleProps.remove( "font-weight" );
            final String style = ( String ) styleProps.remove( "font-style" );

            if ( n.equals( "p" ) ) {
                // change it, so we can print out the css styles for <p>
                n = "div";
            }

            italic = "oblique".equals( style ) || "italic".equals( style );
            bold = "bold".equals( weight ) || "bolder".equals( weight );
            if ( !styleProps.isEmpty() ) {
                cssSpecial = propsToStyleString( styleProps );
            }
        }

        final ElementDecoratorData dto = new ElementDecoratorData();
        dto.base = base;
        dto.bold = bold;
        dto.cssClass = cssClass;
        dto.cssSpecial = cssSpecial;
        dto.htmlBase = n;
        dto.ignoredCssClass = ignoredCssClass;
        dto.italic = italic;
        dto.monospace = monospace;
        return dto;
    }

    private class PreDeque extends ArrayDeque< String > {

        @Override
        public void addFirst( final String item ) {
            super.addFirst( item );
            outTrimmer.setWhitespaceTrimMode( isEmpty() );
        }

        @Override
        public String removeFirst() {
            final String pop = super.removeFirst();
            outTrimmer.setWhitespaceTrimMode( isEmpty() );
            return pop;
        }

    }

    /**
     * Simple data placeholder class to move decoration state between plain text syntax translation related classes.
     */
    public static class ElementDecoratorData {

        /** don't allow instantiation outside enclosing class. */
        private ElementDecoratorData() {}

        public Element base;
        public String htmlBase;
        public String cssClass;
        public String cssSpecial;
        public boolean monospace;
        public boolean bold;
        public boolean italic;
        public boolean ignoredCssClass;
    }

}
