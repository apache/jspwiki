package com.ecyrd.jspwiki.htmltowiki;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Text;
import org.jdom.xpath.XPath;

/**
 * Converting XHtml to Wiki Markup
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class XHtmlElementToWikiTranslator
{
    private static final String UTF8 = "UTF-8";
    
    private XHtmlToWikiConfig m_config;

    private WhitespaceTrimWriter m_outTimmer;

    private PrintWriter m_out;

    private LiStack m_liStack = new LiStack();

    private PreStack m_preStack = new PreStack();

    public XHtmlElementToWikiTranslator( Element base ) throws IOException, JDOMException
    {
        this( base, new XHtmlToWikiConfig() );
    }

    public XHtmlElementToWikiTranslator( Element base, XHtmlToWikiConfig config ) throws IOException, JDOMException
    {
        this.m_config = config;
        m_outTimmer = new WhitespaceTrimWriter();
        m_out = new PrintWriter( m_outTimmer );
        print( base );
    }

    public String getWikiString()
    {
        return m_outTimmer.toString();
    }

    private void print( String s )
    {
        s = StringEscapeUtils.unescapeHtml( s );
        m_out.print( s );
    }

    private void print( Object element ) throws IOException, JDOMException
    {
        if( element instanceof Text )
        {
            Text t = (Text)element;
            String s = t.getText();
            if( m_preStack.isPreMode() )
            {
                m_out.print( s );
            }
            else
            {
                s = s.replaceAll( "\\s+", " " );
                if( !s.equals( " " ) )
                {
                    m_out.print( s );
                }
            }
        }
        else if( element instanceof Element )
        {
            Element base = (Element)element;
            if( "imageplugin".equals( base.getAttributeValue( "class" ) ) )
            {
                printImage( base );
            }
            else
            {
                boolean bold = false;
                boolean italic = false;
                boolean monospace = false;
                String cssSpecial = null;
                Map styleProps = getStylePropertiesLowerCase( base );
                if( styleProps != null )
                {
                    String weight = (String)styleProps.remove( "font-weight" );
                    String style = (String)styleProps.remove( "font-style" );
                    String font = (String)styleProps.remove( "font-family" );
                    String center = (String)styleProps.get( "text-align" );
                    if( "center".equals( center ) )
                    {
                        styleProps.put( "display", "block" );
                    }
                    italic = "oblique".equals( style ) || "italic".equals( style );
                    bold = "bold".equals( weight ) || "bolder".equals( weight );
                    monospace = font != null && (font.indexOf( "mono" ) >= 0 || font.indexOf( "courier" ) >= 0);
                    if( !styleProps.isEmpty() )
                    {
                        cssSpecial = propsToStyleString( styleProps );
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
                    m_out.print( "%%(" + cssSpecial + " )" );
                }
                printChildren( base );
                if( cssSpecial != null )
                {
                    m_out.print( "%%" );
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
            }
        }
    }

    private void printChildren( Element base ) throws IOException, JDOMException
    {
        for( Iterator i = base.getContent().iterator(); i.hasNext(); )
        {
            Object c = i.next();
            if( c instanceof Element )
            {
                Element e = (Element)c;
                String n = e.getName().toLowerCase();
                if( n.equals( "h1" ) )
                {
                    m_out.print( "!!!" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h2" ) )
                {
                    m_out.print( "!!" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h3" ) )
                {
                    m_out.print( "!" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h4" ) )
                {
                    m_out.print( "!" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "p" ) )
                {
                    m_out.println();
                    m_out.println();
                    print( e );
                    m_out.println();
                    m_out.println();
                }
                else if( n.equals( "br" ) )
                {
                    if( m_preStack.isPreMode() )
                    {
                        m_out.println();
                    }
                    else
                    {
                        m_out.print( " \\\\" );
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
                                print( e );
                            }
                            else
                            {
                                ref = trimLink( ref );
                                if( ref != null )
                                {
                                    if( ref.startsWith( "#" ) )
                                    {
                                        print( e );
                                    }
                                    else
                                    {
                                        m_out.print( " [" );
                                        print( e );
                                        if( !e.getTextTrim().replaceAll( "\\s", "" ).equals( ref ) )
                                        {
                                            m_out.print( "|" );
                                            print( ref );
                                        }
                                        m_out.print( "]" );
                                    }
                                }
                            }
                            m_out.print( " " );
                        }
                    }
                }
                else if( n.equals( "b" ) || n.equals("strong") )
                {
                    m_out.print( "__" );
                    print( e );
                    m_out.print( "__" );
                }
                else if( n.equals( "i" ) || n.equals("em") )
                {
                    m_out.print( "''" );
                    print( e );
                    m_out.print( "''" );
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
                    m_out.println();
                }
                else if( n.equals( "pre" ) )
                {
                    m_out.print( "{{{" );
                    m_preStack.push();
                    print( e );
                    m_preStack.pop();
                    m_out.println( "}}}" );
                }
                else if( n.equals( "code" ) || n.equals( "tt" ) )
                {
                    m_out.print( "{{" );
                    m_preStack.push();
                    print( e );
                    m_preStack.pop();
                    m_out.println( "}}" );
                }
                else if( n.equals( "img" ) )
                {
                    if( !isIgnorableWikiMarkupLink( e ) )
                    {
                        m_out.print( "[" );
                        print( trimLink( e.getAttributeValue( "src" ) ) );
                        m_out.print( "]" );
                        m_out.print( " " );
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

    private void printImage( Element base ) throws JDOMException
    {
        Element child = (Element)XPath.selectSingleNode( base, "TBODY/TR/TD/*" );
        if( child == null )
        {
            child = base;
        }
        Element img;
        String href;
        Map map = new ForgetNullValuesLinkedHashMap();
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
        String src = trimLink( img.getAttributeValue( "src" ) );
        if( src == null )
        {
            return;
        }
        map.put( "align", base.getAttributeValue( "align" ) );
        map.put( "height", img.getAttributeValue( "height" ) );
        map.put( "width", img.getAttributeValue( "width" ) );
        map.put( "alt", img.getAttributeValue( "alt" ) );
        map.put( "caption", emptyToNull( XPath.newInstance( "CAPTION" ).valueOf( base ) ) );
        map.put( "link", href );
        map.put( "border", img.getAttributeValue( "border" ) );
        map.put( "style", base.getAttributeValue( "style" ) );
        if( map.size() > 0 )
        {
            m_out.print( "[{Image src='" + src + "'" );
            for( Iterator i = map.entrySet().iterator(); i.hasNext(); )
            {
                Map.Entry entry = (Map.Entry)i.next();
                m_out.print( " " + entry.getKey() + "='" + entry.getValue() + "'" );
            }
            m_out.print( "}]" );
        }
        else
        {
            m_out.print( "[" + src + "]" );
        }
    }

    private String emptyToNull( String s )
    {
        return s == null ? null : (s.replaceAll( "\\s", "" ).length() == 0 ? null : s);
    }

    private String propsToStyleString( Map styleProps )
    {
        StringBuffer style = new StringBuffer();
        for( Iterator i = styleProps.entrySet().iterator(); i.hasNext(); )
        {
            Map.Entry entry = (Map.Entry)i.next();
            style.append( " " ).append( entry.getKey() ).append( ": " ).append( entry.getValue() ).append( ";" );
        }
        return style.toString();
    }

    private boolean isIgnorableWikiMarkupLink( Element a )
    {
        String ref = a.getAttributeValue( "href" );
        String class_ = a.getAttributeValue( "class" );
        return (ref != null && ref.startsWith( m_config.getPageInfoJsp() ))
               || (class_ != null && class_.trim().equalsIgnoreCase( m_config.getOutlink() ));
    }

    private Map getStylePropertiesLowerCase( Element base ) throws IOException
    {
        //"font-weight: bold; font-style: italic;"
        String style = base.getAttributeValue( "style" );
        if( style == null )
        {
            return null;
        }

        style = style.replace( ';', '\n' ).toLowerCase();
        LinkedHashMap m = new LinkedHashMap();
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
            ref = URLDecoder.decode( ref, UTF8 );
            ref = ref.trim();
            if( ref.startsWith( m_config.getAttachPage() ) )
            {
                ref = ref.substring( m_config.getAttachPage().length() );
            }
            if( ref.startsWith( m_config.getWikiJspPage() ) )
            {
                ref = ref.substring( m_config.getWikiJspPage().length() );
            }
            if( m_config.getPageName() != null )
            {
                if( ref.startsWith( m_config.getPageName() ) )
                {
                    ref = ref.substring( m_config.getPageName().length() );
                }
            }
        }
        catch ( UnsupportedEncodingException e )
        {
            // Shouldn't happen...
        }
        return ref;
    }

    // FIXME: These should probably be better used with java.util.Stack
    
    static class LiStack
    {

        private String li = "";

        public void push( String c )
        {
            li += c;
        }

        public void pop()
        {
            li = li.substring( 0, li.length() - 1 );
        }

        public String toString()
        {
            return li;
        }

    }

    class PreStack
    {

        private int pre = 0;

        public boolean isPreMode()
        {
            return pre > 0;
        }

        public void push()
        {
            pre++;
            m_outTimmer.setWhitespaceTrimMode( !isPreMode() );
        }

        public void pop()
        {
            pre--;
            m_outTimmer.setWhitespaceTrimMode( !isPreMode() );
        }

    }

}
