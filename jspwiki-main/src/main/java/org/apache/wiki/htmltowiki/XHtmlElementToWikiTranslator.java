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
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wiki.util.XmlUtil;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.xpath.XPathFactory;

import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Stack;


/**
 * Converting XHtml to Wiki Markup.  This is the class which does all the heavy loading.
 */
public class XHtmlElementToWikiTranslator {

    private final XHtmlToWikiConfig config;

    private final WhitespaceTrimWriter outTrimmer = new WhitespaceTrimWriter();

    private final PrintWriter out = new PrintWriter( outTrimmer );

    private final Stack< String > liStack = new Stack<>();

    private final Stack< String > preStack = new PreStack();

    /**
     *  Create a new translator using the default config.
     *
     *  @param base The base element from which to start translating.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator(final Element base ) throws JDOMException {
        this( base, new XHtmlToWikiConfig() );
    }

    /**
     *  Create a new translator using the specified config.
     *
     *  @param base The base element from which to start translating.
     *  @param config The config to use.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator( final Element base, final XHtmlToWikiConfig config ) throws JDOMException {
        this.config = config;
        translate( base );
    }

    /**
     * Outputs parsed wikitext.
     *
     * @return parsed wikitext.
     */
    public String getWikiString() {
        return outTrimmer.toString();
    }

    private void translate( final Content element ) throws JDOMException {
        if( element instanceof Text ) {
            decorateMarkupForText( ( Text )element );
        } else if( element instanceof Element ) {
            final Element base = ( Element )element;
            if( "imageplugin".equals( base.getAttributeValue( "class" ) ) ) {
                translateImage( base );
            } else if( "wikiform".equals( base.getAttributeValue( "class" ) ) ) {
                // only print the children if the div's class="wikiform", but not the div itself.
                translateChildren( base );
            } else {
                final ElementDecoratorData dto = buildElementDecoratorDataFrom( base );
                decorateMarkupForElementWith( dto );
            }
        }
    }

    void decorateMarkupForString( final String s ) {
        out.print( StringEscapeUtils.unescapeHtml4( s ) );
    }

    void decorateMarkupForText( final Text text ) {
        String s = text.getText();
        if( preStack.isEmpty() ) {
            // remove all "line terminator" characters
            s = s.replaceAll( "[\\r\\n\\f\\u0085\\u2028\\u2029]", "" );
        }
        out.print( s );
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

    void decorateMarkupForElementWith( final ElementDecoratorData dto ) throws JDOMException {
        decorateMarkupForCssClass( dto );
    }

    void decorateMarkupForCssClass( final ElementDecoratorData dto ) throws JDOMException {
        if( dto.cssClass != null && !dto.ignoredCssClass ) {
            if ( dto.htmlBase.equals( "div" ) ) {
                out.print( "\n%%" + dto.cssClass + " \n" );
            } else if ( dto.htmlBase.equals( "span" ) ) {
                out.print( "%%" + dto.cssClass + " " );
            }
        }
        decorateMarkupForBold( dto );
        if( dto.cssClass != null && !dto.ignoredCssClass ) {
            if ( dto.htmlBase.equals( "div" ) ) {
                out.print( "\n/%\n" );
            } else if ( dto.htmlBase.equals( "span" ) ) {
                out.print( "/%" );
            }
        }
    }

    void decorateMarkupForBold( final ElementDecoratorData dto ) throws JDOMException {
        if( dto.bold ) {
            out.print( "__" );
        }
        decorateMarkupForItalic( dto );
        if( dto.bold ) {
            out.print( "__" );
        }
    }

    void decorateMarkupForItalic( final ElementDecoratorData dto ) throws JDOMException {
        if( dto.italic ) {
            out.print( "''" );
        }
        decorateMarkupForMonospace( dto );
        if( dto.italic ) {
            out.print( "''" );
        }
    }

    void decorateMarkupForMonospace( final ElementDecoratorData dto ) throws JDOMException {
        if( dto.monospace ) {
            out.print( "{{{" );
            preStack.push( "{{{" );
        }
        decorateMarkupForCssSpecial( dto );
        if( dto.monospace ) {
            preStack.pop();
            out.print( "}}}" );
        }
    }

    void decorateMarkupForCssSpecial( final ElementDecoratorData dto ) throws JDOMException {
        if( dto.cssSpecial != null ) {
            if ( dto.htmlBase.equals( "div" ) ) {
                out.print( "\n%%(" + dto.cssSpecial + " )\n" );
            } else {
                out.print( "%%(" + dto.cssSpecial + " )" );
            }
        }
        translateChildren( dto.base );
        if( dto.cssSpecial != null ) {
            if ( dto.htmlBase.equals( "div" ) ) {
                out.print( "\n/%\n" );
            } else {
                out.print( "/%" );
            }
        }
    }

    private void translateChildren( final Element base ) throws JDOMException {
        for( final Content c : base.getContent() ) {
            if ( c instanceof Element ) {
                final Element e = ( Element )c;
                final String n = e.getName().toLowerCase();
                switch( n ) {
                    case "h1": decorateMarkupForH1( e ); break;
                    case "h2": decorateMarkupForH2( e ); break;
                    case "h3": decorateMarkupForH3( e ); break;
                    case "h4": decorateMarkupForH4( e ); break;
                    case "p": decorateMarkupForP( e ); break;
                    case "br": decorateMarkupForBR( base, e ); break;
                    case "hr": decorateMarkupForHR( e ); break;
                    case "table": decorateMarkupForTable( e ); break;
                    case "tr": decorateMarkupForTR( e ); break;
                    case "td": decorateMarkupForTD( e ); break;
                    case "th": decorateMarkupForTH( e ); break;
                    case "a": decorateMarkupForA( e ); break;
                    case "b":
                    case "strong": decorateMarkupForStrong( e ); break;
                    case "i":
                    case "em":
                    case "address": decorateMarkupForEM( e ); break;
                    case "u": decorateMarkupForUnderline( e ); break;
                    case "strike": decorateMarkupForStrike( e ); break;
                    case "sup": decorateMarkupForSup( e ); break;
                    case "sub": decorateMarkupForSub( e ); break;
                    case "dl": decorateMarkupForDL( e ); break;
                    case "dt": decorateMarkupForDT( e ); break;
                    case "dd": decorateMarkupForDD( e ); break;
                    case "ul": decorateMarkupForUL( e ); break;
                    case "ol": decorateMarkupForOL( e ); break;
                    case "li": decorateMarkupForLI( base, e ); break;
                    case "pre": decorateMarkupForPre( e ); break;
                    case "code":
                    case "tt": decorateMarkupForCode( e ); break;
                    case "img": decorateMarkupForImg( e ); break;
                    case "form": decorateMarkupForForm( e ); break;
                    case "input": decorateMarkupForInput( e ); break;
                    case "textarea": decorateMarkupForTextarea( e ); break;
                    case "select": decorateMarkupForSelect( e ); break;
                    case "option": decorateMarkupForOption( base, e ); break;
                    default: translate( e ); break;
                }
            } else {
                translate( c );
            }
        }
    }

    void decorateMarkupForH1( final Element e ) throws JDOMException {
        out.print( "\n!!! " );
        translate( e );
        out.println();
    }

    void decorateMarkupForH2( final Element e ) throws JDOMException {
        out.print( "\n!!! " );
        translate( e );
        out.println();
    }

    void decorateMarkupForH3( final Element e ) throws JDOMException {
        out.print( "\n!! " );
        translate( e );
        out.println();
    }

    void decorateMarkupForH4( final Element e ) throws JDOMException {
        out.print( "\n! " );
        translate( e );
        out.println();
    }

    void decorateMarkupForP( final Element e ) throws JDOMException {
        if ( e.getContentSize() != 0 ) { // we don't want to print empty elements: <p></p>
            out.println();
            translate( e );
            out.println();
        }
    }

    void decorateMarkupForHR( final Element e ) throws JDOMException {
        out.println();
        decorateMarkupForString( "----" );
        translate( e );
        out.println();
    }

    void decorateMarkupForBR( final Element base, final Element e ) throws JDOMException {
        if ( !preStack.isEmpty() ) {
            out.println();
        } else {
            final String parentElementName = base.getName().toLowerCase();

            // To beautify the generated wiki markup, we print a newline character after a linebreak.
            // It's only safe to do this when the parent element is a <p> or <div>; when the parent
            // element is a table cell or list item, a newline character would break the markup.
            // We also check that this isn't being done inside a plugin body.
            if ( parentElementName.matches( "p|div" ) && !base.getText().matches( "(?s).*\\[\\{.*\\}\\].*" ) ) {
                out.print( " \\\\\n" );
            } else {
                out.print( " \\\\" );
            }
        }
        translate( e );
    }

    void decorateMarkupForTable( final Element e ) throws JDOMException {
        if ( !outTrimmer.isCurrentlyOnLineBegin() ) {
            out.println();
        }
        translate( e );
    }

    void decorateMarkupForTR( final Element e ) throws JDOMException {
        translate( e );
        out.println();
    }

    void decorateMarkupForTD( final Element e ) throws JDOMException {
        out.print( "| " );
        translate( e );
        if( preStack.isEmpty() ) {
            decorateMarkupForString( " " );
        }
    }

    void decorateMarkupForTH( final Element e ) throws JDOMException {
        out.print( "|| " );
        translate( e );
        if( preStack.isEmpty() ) {
            decorateMarkupForString( " " );
        }
    }

    void decorateMarkupForA( final Element e ) throws JDOMException {
        if( isNotIgnorableWikiMarkupLink( e ) ) {
            if( e.getChild( "IMG" ) != null ) {
                translateImage( e );
            } else {
                String ref = e.getAttributeValue( "href" );
                if ( ref == null ) {
                    if ( isUndefinedPageLink( e ) ) {
                        out.print( "[" );
                        translate( e );
                        out.print( "]" );
                    } else {
                        translate( e );
                    }
                } else {
                    ref = trimLink( ref );
                    if ( ref != null ) {
                        if ( ref.startsWith( "#" ) ) { // This is a link to a footnote.
                            // convert "#ref-PageName-1" to just "1"
                            final String href = ref.replaceFirst( "#ref-.+-(\\d+)", "$1" );

                            // remove the brackets around "[1]"
                            final String textValue = e.getValue().substring( 1, ( e.getValue().length() - 1 ) );

                            if ( href.equals( textValue ) ) { // handles the simplest case. Example: [1]
                                translate( e );
                            } else { // handles the case where the link text is different from the href. Example: [something|1]
                                out.print( "[" + textValue + "|" + href + "]" );
                            }
                        } else {
                            final Map< String, String > augmentedWikiLinkAttributes = getAugmentedWikiLinkAttributes( e );

                            out.print( "[" );
                            translate( e );
                            if ( !e.getTextTrim().equalsIgnoreCase( ref ) ) {
                                out.print( "|" );
                                decorateMarkupForString( ref );

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

    void decorateMarkupForStrong( final Element e ) throws JDOMException {
        out.print( "__" );
        translate( e );
        out.print( "__" );
    }

    void decorateMarkupForEM( final Element e ) throws JDOMException {
        out.print( "''" );
        translate( e );
        out.print( "''" );
    }

    void decorateMarkupForUnderline( final Element e ) throws JDOMException {
        out.print( "%%( text-decoration:underline; )" );
        translate( e );
        out.print( "/%" );
    }

    void decorateMarkupForStrike( final Element e ) throws JDOMException {
        out.print( "%%strike " );
        translate( e );
        out.print( "/%" );
        // NOTE: don't print a space before or after the double percents because that can break words into two.
        // For example: %%(color:red)ABC%%%%(color:green)DEF%% is different from %%(color:red)ABC%% %%(color:green)DEF%%
    }

    void decorateMarkupForSup( final Element e ) throws JDOMException {
        out.print( "%%sup " );
        translate( e );
        out.print( "/%" );
    }

    void decorateMarkupForSub( final Element e ) throws JDOMException {
        out.print( "%%sub " );
        translate( e );
        out.print( "/%" );
    }

    void decorateMarkupForDL( final Element e ) throws JDOMException {
        out.print( "\n" );
        translate( e );

        // print a newline after the definition list. If we don't,
        // it may cause problems for the subsequent element.
        out.print( "\n" );
    }

    void decorateMarkupForDT( final Element e ) throws JDOMException {
        out.print( ";" );
        translate( e );
    }

    void decorateMarkupForDD( final Element e ) throws JDOMException {
        out.print( ":" );
        translate( e );
    }

    void decorateMarkupForUL( final Element e ) throws JDOMException {
        out.println();
        liStack.push( "*" );
        translate( e );
        liStack.pop();
    }

    void decorateMarkupForOL( final Element e ) throws JDOMException {
        out.println();
        liStack.push( "#" );
        translate( e );
        liStack.pop();
    }

    void decorateMarkupForLI( final Element base, final Element e ) throws JDOMException {
        out.print( String.join( "", liStack ) + " " );
        translate( e );

        // The following line assumes that the XHTML has been "pretty-printed"
        // (newlines separate child elements from their parents).
        final boolean lastListItem = base.indexOf( e ) == ( base.getContentSize() - 2 );
        final boolean sublistItem = liStack.size() > 1;

        // only print a newline if this <li> element is not the last item within a sublist.
        if ( !sublistItem || !lastListItem ) {
            out.println();
        }
    }

    void decorateMarkupForPre( final Element e ) throws JDOMException {
        out.print( "\n{{{" ); // start JSPWiki "code blocks" on its own line

        preStack.push( "\n{{{" );
        translate( e );
        preStack.pop();

        // print a newline after the closing braces to avoid breaking any subsequent wiki markup that follows.
        out.print( "}}}\n" );
    }

    void decorateMarkupForCode( final Element e ) throws JDOMException {
        out.print( "{{" );
        preStack.push( "{{" );
        translate( e );
        preStack.pop();
        out.print( "}}" );
        // NOTE: don't print a newline after the closing brackets because if the Text is inside
        // a table or list, it would break it if there was a subsequent row or list item.
    }

    void decorateMarkupForImg( final Element e ) {
        if( isNotIgnorableWikiMarkupLink( e ) ) {
            out.print( "[" );
            decorateMarkupForString( trimLink( e.getAttributeValue( "src" ) ) );
            out.print( "]" );
        }
    }

    void decorateMarkupForForm( final Element e ) throws JDOMException {
        // remove the hidden input where name="formname" since a new one will be generated again when the xhtml is rendered.
        final Element formName = XmlUtil.getXPathElement( e, "INPUT[@name='formname']" );
        if ( formName != null ) {
            formName.detach();
        }

        final String name = e.getAttributeValue( "name" );

        out.print( "\n[{FormOpen" );

        if( name != null ) {
            out.print( " form='" + name + "'" );
        }

        out.print( "}]\n" );

        translate( e );
        out.print( "\n[{FormClose}]\n" );
    }

    void decorateMarkupForInput( final Element e ) throws JDOMException {
        final String type = e.getAttributeValue( "type" );
        String name = e.getAttributeValue( "name" );
        final String value = e.getAttributeValue( "value" );
        final String checked = e.getAttributeValue( "checked" );

        out.print( "[{FormInput" );

        if ( type != null ) {
            out.print( " type='" + type + "'" );
        }
        if ( name != null ) {
            // remove the "nbf_" that was prepended since new one will be generated again when the xhtml is rendered.
            if ( name.startsWith( "nbf_" ) ) {
                name = name.substring( 4 );
            }
            out.print( " name='" + name + "'" );
        }
        if ( value != null && !value.equals( "" ) ) {
            out.print( " value='" + value + "'" );
        }
        if ( checked != null ) {
            out.print( " checked='" + checked + "'" );
        }

        out.print( "}]" );

        translate( e );
    }

    void decorateMarkupForTextarea( final Element e ) throws JDOMException {
        String name = e.getAttributeValue( "name" );
        final String rows = e.getAttributeValue( "rows" );
        final String cols = e.getAttributeValue( "cols" );

        out.print( "[{FormTextarea" );

        if ( name != null ) {
            if ( name.startsWith( "nbf_" ) ) {
                name = name.substring( 4 );
            }
            out.print( " name='" + name + "'" );
        }
        if ( rows != null ) {
            out.print( " rows='" + rows + "'" );
        }
        if ( cols != null ) {
            out.print( " cols='" + cols + "'" );
        }

        out.print( "}]" );
        translate( e );
    }

    void decorateMarkupForSelect( final Element e ) throws JDOMException {
        String name = e.getAttributeValue( "name" );

        out.print( "[{FormSelect" );

        if ( name != null ) {
            if ( name.startsWith( "nbf_" ) ) {
                name = name.substring( 4 );
            }
            out.print( " name='" + name + "'" );
        }

        out.print( " value='" );
        translate( e );
        out.print( "'}]" );
    }

    void decorateMarkupForOption( final Element base, final Element e ) throws JDOMException {
        // If this <option> element isn't the second child element within the parent <select>
        // element, then we need to print a semicolon as a separator. (The first child element
        // is expected to be a newline character which is at index of 0).
        if ( base.indexOf( e ) != 1 ) {
            out.print( ";" );
        }

        final Attribute selected = e.getAttribute( "selected" );
        if ( selected != null ) {
            out.print( "*" );
        }

        final String value = e.getAttributeValue( "value" );
        if ( value != null ) {
            out.print( value );
        } else {
            translate( e );
        }
    }

    private void translateImage( final Element base ) {
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
        final String src = trimLink( img.getAttributeValue( "src" ) );
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
        decorateMarkupforImage( src, imageAttrs );
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

    void decorateMarkupforImage( final String src, final Map< String, Object > imageAttrs ) {
        if( imageAttrs.isEmpty() ) {
            out.print( "[" + src + "]" );
        } else {
            out.print( "[{Image src='" + src + "'" );
            for( final Map.Entry< String, Object > objectObjectEntry : imageAttrs.entrySet() ) {
                if ( !objectObjectEntry.getValue().equals( "" ) ) {
                    out.print( " " + objectObjectEntry.getKey() + "='" + objectObjectEntry.getValue() + "'" );
                }
            }
            out.print( "}]" );
        }
    }

    private boolean isNotIgnorableWikiMarkupLink( final Element a ) {
        final String ref = a.getAttributeValue( "href" );
        final String clazz = a.getAttributeValue( "class" );
        return ( ref == null || !ref.startsWith( config.getPageInfoJsp() ) )
                && ( clazz == null || !clazz.trim().equalsIgnoreCase( config.getOutlink() ) );
    }

    private String trimLink( String ref ) {
        if( ref == null ) {
            return null;
        }
        try {
            ref = URLDecoder.decode( ref, StandardCharsets.UTF_8.name() );
            ref = ref.trim();
            if( ref.startsWith( config.getAttachPage() ) ) {
                ref = ref.substring( config.getAttachPage().length() );
            }
            if( ref.startsWith( config.getWikiJspPage() ) ) {
                ref = ref.substring( config.getWikiJspPage().length() );

                // Handle links with section anchors.
                // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                // to this wiki string "TargetPage#Heading2".
                ref = ref.replaceFirst( ".+#section-(.+)-(.+)", "$1#$2" );
            }
            if( ref.startsWith( config.getEditJspPage() ) ) {
                ref = ref.substring( config.getEditJspPage().length() );
            }
            if( config.getPageName() != null ) {
                if( ref.startsWith( config.getPageName() ) ) {
                    ref = ref.substring( config.getPageName().length() );
                }
            }
        } catch ( final UnsupportedEncodingException e ) {
            // Shouldn't happen...
        }
        return ref;
    }

    private class PreStack extends Stack< String > {

        @Override
        public String push( final String item ) {
            final String push = super.push( item );
            outTrimmer.setWhitespaceTrimMode( isEmpty() );
            return push;
        }

        @Override
        public synchronized String pop() {
            final String pop = super.pop();
            outTrimmer.setWhitespaceTrimMode( isEmpty() );
            return pop;
        }

    }

    static class ElementDecoratorData {
        Element base;
        String htmlBase;
        String cssClass;
        String cssSpecial;
        boolean monospace;
        boolean bold;
        boolean italic;
        boolean ignoredCssClass;
    }

}
