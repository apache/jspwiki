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

import org.apache.commons.text.StringEscapeUtils;
import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Text;
import org.jdom2.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * Converting XHtml to Wiki Markup.  This is the class which does all of the heavy loading.
 *
 */
public class XHtmlElementToWikiTranslator
{

    private final XHtmlToWikiConfig m_config;

    private final WhitespaceTrimWriter m_outTimmer;

    private final PrintWriter m_out;

    private final LiStack m_liStack = new LiStack();

    private final PreStack m_preStack = new PreStack();

    /**
     *  Create a new translator using the default config.
     *
     *  @param base The base element from which to start translating.
     *  @throws IOException If reading of the DOM tree fails.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator(final Element base ) throws IOException, JDOMException
    {
        this( base, new XHtmlToWikiConfig() );
    }

    /**
     *  Create a new translator using the specified config.
     *
     *  @param base The base element from which to start translating.
     *  @param config The config to use.
     *  @throws IOException If reading of the DOM tree fails.
     *  @throws JDOMException If the DOM tree is faulty.
     */
    public XHtmlElementToWikiTranslator(final Element base, final XHtmlToWikiConfig config ) throws IOException, JDOMException
    {
        this.m_config = config;
        m_outTimmer = new WhitespaceTrimWriter();
        m_out = new PrintWriter( m_outTimmer );
        print( base );
    }

    /**
     *  FIXME: I have no idea what this does...
     *
     *  @return Something.
     */
    public String getWikiString()
    {
        return m_outTimmer.toString();
    }

    private void print( String s )
    {
        s = StringEscapeUtils.unescapeHtml4( s );
        m_out.print( s );
    }

    private void print(final Object element ) throws IOException, JDOMException
    {
        if( element instanceof Text )
        {
            final Text t = (Text)element;
            String s = t.getText();
            if( m_preStack.isPreMode() )
            {
                m_out.print( s );
            }
            else
            {
                // remove all "line terminator" characters
                s = s.replaceAll( "[\\r\\n\\f\\u0085\\u2028\\u2029]", "" );
                m_out.print( s );
            }
        }
        else if( element instanceof Element )
        {
            final Element base = (Element)element;
            String n = base.getName().toLowerCase();
            if( "imageplugin".equals( base.getAttributeValue( "class" ) ) )
            {
                printImage( base );
            }
            else if( "wikiform".equals( base.getAttributeValue( "class" ) ) )
            {
                // only print the children if the div's class="wikiform", but not the div itself.
                printChildren( base );
            }
            else
            {
                boolean bold = false;
                boolean italic = false;
                boolean monospace = false;
                String cssSpecial = null;
                final String cssClass = base.getAttributeValue( "class" );

                // accomodate a FCKeditor bug with Firefox: when a link is removed, it becomes <span class="wikipage">text</span>.
                final boolean ignoredCssClass = cssClass != null && cssClass.matches( "wikipage|createpage|external|interwiki|attachment|inline-code" );

                Map< Object, Object > styleProps = null;

                // Only get the styles if it's not a link element. Styles for link elements are
                // handled as an AugmentedWikiLink instead.
                if( !n.equals( "a" ) )
                {
                    styleProps = getStylePropertiesLowerCase( base );
                }

                if("inline-code".equals(cssClass))
                {
                    monospace = true;
                }

                if( styleProps != null )
                {
                    final String weight = (String)styleProps.remove( "font-weight" );
                    final String style = (String)styleProps.remove( "font-style" );

                    if( n.equals( "p" ) )
                    {
                        // change it so we can print out the css styles for <p>
                        n = "div";
                    }

                    italic = "oblique".equals( style ) || "italic".equals( style );
                    bold = "bold".equals( weight ) || "bolder".equals( weight );
                    if( !styleProps.isEmpty() )
                    {
                        cssSpecial = propsToStyleString( styleProps );
                    }
                }
                if( cssClass != null && !ignoredCssClass )
                {
                    if( n.equals( "div" ) )
                    {
                        m_out.print( "\n%%" + cssClass + " \n" );
                    }
                    else if( n.equals( "span" ) )
                    {
                        m_out.print( "%%" + cssClass + " " );
                    }
                }
                if( bold )
                {
                    m_out.print( "__" );
                }
                if( italic )
                {
                    m_out.print( "''" );
                }
                if( monospace )
                {
                    m_out.print( "{{{" );
                    m_preStack.push();
                }
                if( cssSpecial != null )
                {
                    if( n.equals( "div" ) )
                    {
                        m_out.print( "\n%%(" + cssSpecial + " )\n" );
                    }
                    else
                    {
                        m_out.print( "%%(" + cssSpecial + " )" );
                    }
                }
                printChildren( base );
                if( cssSpecial != null )
                {
                    if( n.equals( "div" ) )
                    {
                        m_out.print( "\n/%\n" );
                    }
                    else
                    {
                        m_out.print( "/%" );
                    }
                }
                if( monospace )
                {
                    m_preStack.pop();
                    m_out.print( "}}}" );
                }
                if( italic )
                {
                    m_out.print( "''" );
                }
                if( bold )
                {
                    m_out.print( "__" );
                }
                if( cssClass != null && !ignoredCssClass )
                {
                    if( n.equals( "div" ) )
                    {
                        m_out.print( "\n/%\n" );
                    }
                    else if( n.equals( "span" ) )
                    {
                        m_out.print( "/%" );
                    }
                }
            }
        }
    }

    private void printChildren(final Element base ) throws IOException, JDOMException
    {
        for(final Iterator< Content > i = base.getContent().iterator(); i.hasNext(); )
        {
            final Content c = i.next();
            if( c instanceof Element )
            {
                final Element e = (Element)c;
                final String n = e.getName().toLowerCase();
                if( n.equals( "h1" ) )
                {
                    m_out.print( "\n!!! " );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h2" ) )
                {
                    m_out.print( "\n!!! " );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h3" ) )
                {
                    m_out.print( "\n!! " );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h4" ) )
                {
                    m_out.print( "\n! " );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "p" ) )
                {
                    if( e.getContentSize() != 0 ) // we don't want to print empty elements: <p></p>
                    {
                        m_out.println();
                        print( e );
                        m_out.println();
                    }
                }
                else if( n.equals( "br" ) )
                {
                    if( m_preStack.isPreMode() )
                    {
                        m_out.println();
                    }
                    else
                    {
                        final String parentElementName = base.getName().toLowerCase();

                        //
                        // To beautify the generated wiki markup, we print a newline character after a linebreak.
                        // It's only safe to do this when the parent element is a <p> or <div>; when the parent
                        // element is a table cell or list item, a newline character would break the markup.
                        // We also check that this isn't being done inside a plugin body.
                        //
                        if( parentElementName.matches( "p|div" )
                            && !base.getText().matches( "(?s).*\\[\\{.*\\}\\].*" ) )
                        {
                            m_out.print( " \\\\\n" );
                        }
                        else
                        {
                            m_out.print( " \\\\" );
                        }
                    }
                    print( e );
                }
                else if( n.equals( "hr" ) )
                {
                    m_out.println();
                    print( "----" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "table" ) )
                {
                    if( !m_outTimmer.isCurrentlyOnLineBegin() )
                    {
                        m_out.println();
                    }
                    print( e );
                }
                else if( n.equals( "tr" ) )
                {
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "td" ) )
                {
                    m_out.print( "| " );
                    print( e );
                    if( !m_preStack.isPreMode() )
                    {
                        print( " " );
                    }
                }
                else if( n.equals( "th" ) )
                {
                    m_out.print( "|| " );
                    print( e );
                    if( !m_preStack.isPreMode() )
                    {
                        print( " " );
                    }
                }
                else if( n.equals( "a" ) )
                {
                    if( !isIgnorableWikiMarkupLink( e ) )
                    {
                        if( e.getChild( "IMG" ) != null )
                        {
                            printImage( e );
                        }
                        else
                        {
                            String ref = e.getAttributeValue( "href" );
                            if( ref == null )
                            {
                                if( isUndefinedPageLink( e ) )
                                {
                                    m_out.print( "[" );
                                    print( e );
                                    m_out.print( "]" );
                                }
                                else
                                {
                                    print( e );
                                }
                            }
                            else
                            {
                                ref = trimLink( ref );
                                if( ref != null )
                                {
                                    if( ref.startsWith( "#" ) ) // This is a link to a footnote.
                                    {
                                        // convert "#ref-PageName-1" to just "1"
                                        final String href = ref.replaceFirst( "#ref-.+-(\\d+)", "$1" );

                                        // remove the brackets around "[1]"
                                        final String textValue = e.getValue().substring( 1, (e.getValue().length() - 1) );

                                        if( href.equals( textValue ) ){ // handles the simplest case. Example: [1]
                                            print( e );
                                        }
                                        else{ // handles the case where the link text is different from the href. Example: [something|1]
                                            m_out.print( "[" + textValue + "|" + href + "]" );
                                        }
                                    }
                                    else
                                    {
                                        final Map<String, String> augmentedWikiLinkAttributes = getAugmentedWikiLinkAttributes( e );

                                        m_out.print( "[" );
                                        print( e );
                                        if( !e.getTextTrim().equalsIgnoreCase( ref ) )
                                        {
                                            m_out.print( "|" );
                                            print( ref );

                                            if( !augmentedWikiLinkAttributes.isEmpty() )
                                            {
                                                m_out.print( "|" );

                                                final String augmentedWikiLink = augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
                                                m_out.print( augmentedWikiLink );
                                            }
                                        }
                                        else if( !augmentedWikiLinkAttributes.isEmpty() )
                                        {
                                            // If the ref has the same value as the text and also if there
                                            // are attributes, then just print: [ref|ref|attributes] .
                                            m_out.print( "|" + ref + "|" );
                                            final String augmentedWikiLink = augmentedWikiLinkMapToString( augmentedWikiLinkAttributes );
                                            m_out.print( augmentedWikiLink );
                                        }

                                        m_out.print( "]" );
                                    }
                                }
                            }
                        }
                    }
                }
                else if( n.equals( "b" ) || n.equals("strong") )
                {
                    m_out.print( "__" );
                    print( e );
                    m_out.print( "__" );
                }
                else if( n.equals( "i" ) || n.equals("em") || n.equals( "address" ) )
                {
                    m_out.print( "''" );
                    print( e );
                    m_out.print( "''" );
                }
                else if( n.equals( "u" ) )
                {
                    m_out.print( "%%( text-decoration:underline; )" );
                    print( e );
                    m_out.print( "/%" );
                }
                else if( n.equals( "strike" ) )
                {
                    m_out.print( "%%strike " );
                    print( e );
                    m_out.print( "/%" );
                    // NOTE: don't print a space before or after the double percents because that can break words into two.
                    // For example: %%(color:red)ABC%%%%(color:green)DEF%% is different from %%(color:red)ABC%% %%(color:green)DEF%%
                }
                else if( n.equals( "sup" ) )
                {
                    m_out.print( "%%sup " );
                    print( e );
                    m_out.print( "/%" );
                }
                else if( n.equals( "sub" ) )
                {
                    m_out.print( "%%sub " );
                    print( e );
                    m_out.print( "/%" );
                }
                else if( n.equals("dl") )
                {
                    m_out.print( "\n" );
                    print( e );

                    // print a newline after the definition list. If we don't,
                    // it may cause problems for the subsequent element.
                    m_out.print( "\n" );
                }
                else if( n.equals("dt") )
                {
                    m_out.print( ";" );
                    print( e );
                }
                else if( n.equals("dd") )
                {
                    m_out.print( ":" );
                    print( e );
                }
                else if( n.equals( "ul" ) )
                {
                    m_out.println();
                    m_liStack.push( "*" );
                    print( e );
                    m_liStack.pop();
                }
                else if( n.equals( "ol" ) )
                {
                    m_out.println();
                    m_liStack.push( "#" );
                    print( e );
                    m_liStack.pop();
                }
                else if( n.equals( "li" ) )
                {
                    m_out.print( m_liStack + " " );
                    print( e );

                    // The following line assumes that the XHTML has been "pretty-printed"
                    // (newlines separate child elements from their parents).
                    final boolean lastListItem = base.indexOf( e ) == ( base.getContentSize() - 2 );
                    final boolean sublistItem = m_liStack.toString().length() > 1;

                    // only print a newline if this <li> element is not the last item within a sublist.
                    if( !sublistItem || !lastListItem )
                    {
                        m_out.println();
                    }
                }
                else if( n.equals( "pre" ) )
                {
                    m_out.print( "\n{{{" ); // start JSPWiki "code blocks" on its own line
                    m_preStack.push();
                    print( e );
                    m_preStack.pop();

                    // print a newline after the closing braces
                    // to avoid breaking any subsequent wiki markup that follows.
                    m_out.print( "}}}\n" );
                }
                else if( n.equals( "code" ) || n.equals( "tt" ) )
                {
                    m_out.print( "{{" );
                    m_preStack.push();
                    print( e );
                    m_preStack.pop();
                    m_out.print( "}}" );
                    // NOTE: don't print a newline after the closing brackets because if the Text is inside
                    // a table or list, it would break it if there was a subsequent row or list item.
                }
                else if( n.equals( "img" ) )
                {
                    if( !isIgnorableWikiMarkupLink( e ) )
                    {
                        m_out.print( "[" );
                        print( trimLink( e.getAttributeValue( "src" ) ) );
                        m_out.print( "]" );
                    }
                }
                else if( n.equals( "form" ) )
                {
                    // remove the hidden input where name="formname" since a new one will be generated again when the xhtml is rendered.
                    final Element formName = getXPathElement( e, "INPUT[@name='formname']" );
                    if( formName != null )
                    {
                        formName.detach();
                    }

                    final String name = e.getAttributeValue( "name" );

                    m_out.print( "\n[{FormOpen" );

                    if( name != null )
                    {
                        m_out.print( " form='" + name + "'" );
                    }

                    m_out.print( "}]\n" );

                    print( e );
                    m_out.print( "\n[{FormClose}]\n" );
                }
                else if( n.equals( "input" ) )
                {
                    final String type = e.getAttributeValue( "type" );
                    String name = e.getAttributeValue( "name" );
                    final String value = e.getAttributeValue( "value" );
                    final String checked = e.getAttributeValue( "checked" );

                    m_out.print( "[{FormInput" );

                    if( type != null )
                    {
                        m_out.print( " type='" + type + "'" );
                    }
                    if( name != null )
                    {
                        // remove the "nbf_" that was prepended since new one will be generated again when the xhtml is rendered.
                        if( name.startsWith( "nbf_" ) )
                        {
                            name = name.substring( 4, name.length( ));
                        }
                        m_out.print( " name='" + name + "'" );
                    }
                    if( value != null && !value.equals( "" ) )
                    {
                        m_out.print( " value='" + value + "'" );
                    }
                    if( checked != null )
                    {
                        m_out.print( " checked='" + checked + "'" );
                    }

                    m_out.print( "}]" );

                    print( e );
                }
                else if( n.equals( "textarea" ) )
                {
                    String name = e.getAttributeValue( "name" );
                    final String rows = e.getAttributeValue( "rows" );
                    final String cols = e.getAttributeValue( "cols" );

                    m_out.print( "[{FormTextarea" );

                    if( name != null )
                    {
                        if( name.startsWith( "nbf_" ) )
                        {
                            name = name.substring( 4, name.length( ));
                        }
                        m_out.print( " name='" + name + "'" );
                    }
                    if( rows != null )
                    {
                        m_out.print( " rows='" + rows + "'" );
                    }
                    if( cols != null )
                    {
                        m_out.print( " cols='" + cols + "'" );
                    }

                    m_out.print( "}]" );
                    print( e );
                }
                else if( n.equals( "select" ) )
                {
                    String name = e.getAttributeValue( "name" );

                    m_out.print( "[{FormSelect" );

                    if( name != null )
                    {
                        if( name.startsWith( "nbf_" ) )
                        {
                            name = name.substring( 4, name.length( ));
                        }
                        m_out.print( " name='" + name + "'" );
                    }

                    m_out.print( " value='" );
                    print( e );
                    m_out.print( "'}]" );
                }
                else if( n.equals( "option" ) )
                {
                    // If this <option> element isn't the second child element within the parent <select>
                    // element, then we need to print a semicolon as a separator. (The first child element
                    // is expected to be a newline character which is at index of 0).
                    if( base.indexOf( e ) != 1 )
                    {
                        m_out.print( ";" );
                    }

                    final Attribute selected = e.getAttribute( "selected" );
                    if( selected !=  null )
                    {
                        m_out.print( "*" );
                    }

                    final String value = e.getAttributeValue( "value" );
                    if( value != null )
                    {
                        m_out.print( value );
                    }
                    else
                    {
                        print( e );
                    }
                }
                else
                {
                    print( e );
                }
            }
            else
            {
                print( c );
            }
        }
    }

    private void printImage(final Element base )
    {
        Element child = getXPathElement( base, "TBODY/TR/TD/*" );
        if( child == null )
        {
            child = base;
        }
        final Element img;
        final String href;
        final Map<Object,Object> map = new ForgetNullValuesLinkedHashMap<>();
        if( child.getName().equals( "A" ) )
        {
            img = child.getChild( "IMG" );
            href = child.getAttributeValue( "href" );
        }
        else
        {
            img = child;
            href = null;
        }
        if( img == null )
        {
            return;
        }
        final String src = trimLink( img.getAttributeValue( "src" ) );
        if( src == null )
        {
            return;
        }
        map.put( "align", base.getAttributeValue( "align" ) );
        map.put( "height", img.getAttributeValue( "height" ) );
        map.put( "width", img.getAttributeValue( "width" ) );
        map.put( "alt", img.getAttributeValue( "alt" ) );
        map.put( "caption", emptyToNull( ( Element )XPathFactory.instance().compile(  "CAPTION" ).evaluateFirst( base ) ) );
        map.put( "link", href );
        map.put( "border", img.getAttributeValue( "border" ) );
        map.put( "style", base.getAttributeValue( "style" ) );
        if( map.size() > 0 )
        {
            m_out.print( "[{Image src='" + src + "'" );
            for(final Iterator i = map.entrySet().iterator(); i.hasNext(); )
            {
                final Map.Entry entry = (Map.Entry)i.next();
                if( !entry.getValue().equals( "" ) )
                {
                    m_out.print( " " + entry.getKey() + "='" + entry.getValue() + "'" );
                }
            }
            m_out.print( "}]" );
        }
        else
        {
            m_out.print( "[" + src + "]" );
        }
    }

    Element getXPathElement( final Element base, final String expression ) {
        final List< ? > nodes = XPathFactory.instance().compile( expression ).evaluate( base );
        if( nodes == null || nodes.size() == 0 ) {
            return null;
        } else {
            return ( Element )nodes.get( 0 );
        }
    }

    private String emptyToNull( final Element e ) {
        if( e == null ) {
            return null;
        }
        final String s = e.getText();
        return s == null ? null : ( s.replaceAll( "\\s", "" ).isEmpty() ? null : s );
    }

    private String propsToStyleString( final Map< Object, Object >  styleProps ) {
    	final StringBuilder style = new StringBuilder();
        for(final Iterator< Map.Entry< Object, Object > > i = styleProps.entrySet().iterator(); i.hasNext(); ) {
            final Map.Entry< Object, Object > entry = i.next();
            style.append( " " ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( ";" );
        }
        return style.toString();
    }

    private boolean isIgnorableWikiMarkupLink( final Element a ) {
        final String ref = a.getAttributeValue( "href" );
        final String clazz = a.getAttributeValue( "class" );
        return ( ref != null && ref.startsWith( m_config.getPageInfoJsp() ) )
            || ( clazz != null && clazz.trim().equalsIgnoreCase( m_config.getOutlink() ) );
    }

    /**
     * Checks if the link points to an undefined page.
     */
    private boolean isUndefinedPageLink(final Element a)
    {
        final String classVal = a.getAttributeValue( "class" );

        return "createpage".equals( classVal );
    }

    /**
     *  Returns a Map containing the valid augmented wiki link attributes.
     */
    private Map<String,String> getAugmentedWikiLinkAttributes(final Element a )
    {
        final Map<String,String> attributesMap = new HashMap<>();

        final String id = a.getAttributeValue( "id" );
        if( id != null && !id.equals( "" ) )
        {
            attributesMap.put( "id", id.replaceAll( "'", "\"" ) );
        }

        final String cssClass = a.getAttributeValue( "class" );
        if( cssClass != null && !cssClass.equals( "" )
            && !cssClass.matches( "wikipage|createpage|external|interwiki|attachment" ) )
        {
            attributesMap.put( "class", cssClass.replaceAll( "'", "\"" ) );
        }

        final String style = a.getAttributeValue( "style" );
        if( style != null && !style.equals( "" ) )
        {
            attributesMap.put( "style", style.replaceAll( "'", "\"" ) );
        }

        final String title = a.getAttributeValue( "title" );
        if( title != null && !title.equals( "" ) )
        {
            attributesMap.put( "title", title.replaceAll( "'", "\"" ) );
        }

        final String lang = a.getAttributeValue( "lang" );
        if( lang != null && !lang.equals( "" ) )
        {
            attributesMap.put( "lang", lang.replaceAll( "'", "\"" ) );
        }

        final String dir = a.getAttributeValue( "dir" );
        if( dir != null && !dir.equals( "" ) )
        {
            attributesMap.put( "dir", dir.replaceAll( "'", "\"" ) );
        }

        final String charset = a.getAttributeValue( "charset" );
        if( charset != null && !charset.equals("") )
        {
            attributesMap.put( "charset", charset.replaceAll( "'", "\"" ) );
        }

        final String type = a.getAttributeValue( "type" );
        if( type != null && !type.equals( "" ) )
        {
            attributesMap.put( "type", type.replaceAll( "'", "\"" ) );
        }

        final String hreflang = a.getAttributeValue( "hreflang" );
        if( hreflang != null && !hreflang.equals( "" ) )
        {
            attributesMap.put( "hreflang", hreflang.replaceAll( "'", "\"" ) );
        }

        final String rel = a.getAttributeValue( "rel" );
        if( rel != null && !rel.equals( "" ) )
        {
            attributesMap.put( "rel", rel.replaceAll( "'", "\"" ) );
        }

        final String rev = a.getAttributeValue( "rev" );
        if( rev != null && !rev.equals( "" ) )
        {
            attributesMap.put( "rev", rev.replaceAll( "'", "\"" ) );
        }

        final String accesskey = a.getAttributeValue( "accesskey" );
        if( accesskey != null && !accesskey.equals( "" ) )
        {
            attributesMap.put( "accesskey", accesskey.replaceAll( "'", "\"" ) );
        }

        final String tabindex = a.getAttributeValue( "tabindex" );
        if( tabindex != null && !tabindex.equals( "" ) )
        {
            attributesMap.put( "tabindex", tabindex.replaceAll( "'", "\"" ) );
        }

        final String target = a.getAttributeValue( "target" );
        if( target != null && !target.equals( "" ) )
        {
            attributesMap.put( "target", target.replaceAll( "'", "\"" ) );
        }

        return attributesMap;
    }

    /**
     * Converts the entries in the map to a string for use in a wiki link.
     */
    private String augmentedWikiLinkMapToString(final Map attributesMap )
    {
    	final StringBuilder sb = new StringBuilder();

        for (final Iterator itr = attributesMap.entrySet().iterator(); itr.hasNext(); )
        {
            final Map.Entry entry = (Map.Entry)itr.next();
            final String attributeName = (String)entry.getKey();
            final String attributeValue = (String)entry.getValue();

            sb.append( " " + attributeName + "='" + attributeValue + "'" );
        }

        return sb.toString().trim();
    }

    private Map< Object, Object > getStylePropertiesLowerCase(final Element base ) throws IOException
    {
        final String n = base.getName().toLowerCase();

        //"font-weight: bold; font-style: italic;"
        String style = base.getAttributeValue( "style" );
        if( style == null )
        {
            style = "";
        }

        if( n.equals( "p" ) || n.equals( "div" ) )
        {
            final String align = base.getAttributeValue( "align" );
            if( align != null )
            {
                // only add the value of the align attribute if the text-align style didn't already exist.
                if( style.indexOf( "text-align" ) == -1 )
                {
                    style = style + ";text-align:" + align + ";";
                }
            }
        }



        if( n.equals( "font" ) )
        {
            final String color = base.getAttributeValue( "color" );
            final String face = base.getAttributeValue( "face" );
            final String size = base.getAttributeValue( "size" );
            if( color != null )
            {
                style = style + "color:" + color + ";";
            }
            if( face != null )
            {
                style = style + "font-family:" + face + ";";
            }
            if( size != null )
            {
                if( size.equals( "1" ) )
                {
                    style = style + "font-size:xx-small;";
                }
                else if( size.equals( "2" ) )
                {
                    style = style + "font-size:x-small;";
                }
                else if( size.equals( "3" ) )
                {
                    style = style + "font-size:small;";
                }
                else if( size.equals( "4" ) )
                {
                    style = style + "font-size:medium;";
                }
                else if( size.equals( "5" ) )
                {
                    style = style + "font-size:large;";
                }
                else if( size.equals( "6" ) )
                {
                    style = style + "font-size:x-large;";
                }
                else if( size.equals( "7" ) )
                {
                    style = style + "font-size:xx-large;";
                }
            }
        }

        if( style.equals( "" ) )
        {
            return null;
        }

        style = style.replace( ';', '\n' ).toLowerCase();
        final LinkedHashMap< Object, Object > m = new LinkedHashMap<>();
        new PersistentMapDecorator( m ).load( new ByteArrayInputStream( style.getBytes() ) );
        return m;
    }

    private String trimLink( String ref )
    {
        if( ref == null )
        {
            return null;
        }
        try
        {
            ref = URLDecoder.decode( ref, StandardCharsets.UTF_8.name() );
            ref = ref.trim();
            if( ref.startsWith( m_config.getAttachPage() ) )
            {
                ref = ref.substring( m_config.getAttachPage().length() );
            }
            if( ref.startsWith( m_config.getWikiJspPage() ) )
            {
                ref = ref.substring( m_config.getWikiJspPage().length() );

                // Handle links with section anchors.
                // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                // to this wiki string "TargetPage#Heading2".
                ref = ref.replaceFirst( ".+#section-(.+)-(.+)", "$1#$2" );
            }
            if( ref.startsWith( m_config.getEditJspPage() ) )
            {
                ref = ref.substring( m_config.getEditJspPage().length() );
            }
            if( m_config.getPageName() != null )
            {
                if( ref.startsWith( m_config.getPageName() ) )
                {
                    ref = ref.substring( m_config.getPageName().length() );
                }
            }
        }
        catch ( final UnsupportedEncodingException e )
        {
            // Shouldn't happen...
        }
        return ref;
    }

    // FIXME: These should probably be better used with java.util.Stack

    private static class LiStack
    {

        private StringBuffer m_li = new StringBuffer();

        public void push(final String c )
        {
            m_li.append( c );
        }

        public void pop()
        {
            m_li = m_li.deleteCharAt( m_li.length()-1 );
            // m_li = m_li.substring( 0, m_li.length() - 1 );
        }

        @Override
        public String toString()
        {
            return m_li.toString();
        }

    }

    private class PreStack
    {

        private int m_pre;

        public boolean isPreMode()
        {
            return m_pre > 0;
        }

        public void push()
        {
            m_pre++;
            m_outTimmer.setWhitespaceTrimMode( !isPreMode() );
        }

        public void pop()
        {
            m_pre--;
            m_outTimmer.setWhitespaceTrimMode( !isPreMode() );
        }

    }

}
