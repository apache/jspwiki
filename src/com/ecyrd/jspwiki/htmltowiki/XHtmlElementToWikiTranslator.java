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
import org.jdom.Attribute;
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
                String cssClass = base.getAttributeValue( "class" );
                
                // accomodate a FCKeditor bug with Firefox: when a link is removed, it becomes <span class="wikipage">text</span>.
                boolean ignoredCssClass = cssClass != null && cssClass.matches( "wikipage|editpage|external" );
                
                Map styleProps = getStylePropertiesLowerCase( base );
                if( styleProps != null )
                {
                    String fontFamily = (String)styleProps.get( "font-family" );
                    String whiteSpace = (String)styleProps.get( "white-space" );
                    if( fontFamily != null && ( fontFamily.indexOf( "monospace" ) >= 0 
                            && whiteSpace != null && whiteSpace.indexOf( "pre" ) >= 0  ) )
                    {
                        styleProps.remove( "font-family" );
                        styleProps.remove( "white-space" );
                        monospace = true;
                    }
                    
                    String weight = (String)styleProps.remove( "font-weight" );
                    String style = (String)styleProps.remove( "font-style" );
                    
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
                        m_out.print( "\n%%\n" );
                    }
                    else
                    {
                        m_out.print( "%%" );
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
                        m_out.print( "\n%%\n" );
                    }
                    else if( n.equals( "span" ) )
                    {
                        m_out.print( "%%" );
                    }
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
                    m_out.print( "!!!" );
                    print( e );
                    m_out.println();
                }
                else if( n.equals( "h3" ) )
                {
                    m_out.print( "!!" );
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
                                    if( ref.startsWith( "#" ) )
                                    {
                                        print( e );
                                    }
                                    else
                                    {
                                        String target = e.getAttributeValue( "target" );

                                        m_out.print( " [" );
                                        print( e );
                                        if( !e.getTextTrim().replaceAll( "\\s", "" ).equals( ref ) )
                                        {
                                            m_out.print( "|" );
                                            print( ref );

                                            if( target != null )
                                            {
                                                m_out.print( "|target='" +  target + "'" );
                                            }
                                        }
                                        else if( target != null )
                                        {
                                            // if the ref has the same value as the text and also if there's
                                            // a target attribute, then just print [ref|ref|target='targetValue']
                                            m_out.print( "|" + ref + "|target='" +  target + "'" );
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
                else if( n.equals( "i" ) || n.equals("em") || n.equals( "address" ) )
                {
                    m_out.print( "''" );
                    print( e );
                    m_out.print( "''" );
                }
                else if( n.equals( "u" ) )
                {
                    m_out.print( "%%(text-decoration:underline;) " );
                    print( e );
                    m_out.print( "%%" );
                }
                else if( n.equals( "strike" ) )
                {
                    m_out.print( "%%strike " );
                    print( e );
                    m_out.print( "%%" );
                    // NOTE: don't print a space before or after the double percents because that can break words into two.
                    // For example: %%(color:red)ABC%%%%(color:green)DEF%% is different from %%(color:red)ABC%% %%(color:green)DEF%%
                }
                else if( n.equals( "sup" ) )
                {
                    m_out.print( "%%sup " );
                    print( e );
                    m_out.print( "%%" );
                }
                else if( n.equals( "sub" ) )
                {
                    m_out.print( "%%sub " );
                    print( e );
                    m_out.print( "%%" );
                }
                else if( n.equals("dl") )
                {
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
                    m_out.println();
                }
                else if( n.equals( "pre" ) )
                {
                    m_out.print( "\n{{{" ); // start JSPWiki "code blocks" on a new line
                    m_preStack.push();
                    print( e );
                    m_preStack.pop();
                    
                    // print a newline before the closing braces for aesthetics and a newline after it
                    // to avoid breaking any subsequent wiki markup that follows.
                    m_out.print( "\n}}}\n" );
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
                        m_out.print( " " );
                    }
                }
                else if( n.equals( "form" ) )
                {
                    // remove the hidden input where name="formname" since a new one will be generated again when the xhtml is rendered.
                    Element formName = (Element)XPath.selectSingleNode( e, "INPUT[@name='formname']" );
                    if( formName != null ){
                        formName.detach();
                    }
                    
                    String name = e.getAttributeValue( "name" );
                    
                    m_out.print( "[{FormOpen" );
                    
                    if( name != null )
                    {
                        m_out.print( " form='" + name + "'" );
                    }
                    
                    m_out.print( "}]\n" );
                    
                    print( e );
                    m_out.print( "[{FormClose}]\n" );
                }
                else if( n.equals( "input" ) )
                {
                    String type = e.getAttributeValue( "type" );
                    String name = e.getAttributeValue( "name" );
                    String value = e.getAttributeValue( "value" );
                    String checked = e.getAttributeValue( "checked" );
                    
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
                    if( value != null )
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
                    String rows = e.getAttributeValue( "rows" );
                    String cols = e.getAttributeValue( "cols" );
                    
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
                    Attribute selected = e.getAttribute( "selected" );
                    if( selected !=  null )
                    {
                        m_out.print( "*" );
                    }
                    print( e );
                    m_out.print( ";" );
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
    
    /**
     * Checks if the link points to an undefined page.
     */
    private boolean isUndefinedPageLink( Element a)
    {
        String classVal = a.getAttributeValue( "class" );

        return ( classVal != null && classVal.equals( "editpage" ) );
    }

    private Map getStylePropertiesLowerCase( Element base ) throws IOException
    {
        String n = base.getName().toLowerCase();
        
        //"font-weight: bold; font-style: italic;"
        String style = base.getAttributeValue( "style" );
        if( style == null )
        {
            style = "";
        }
        
        if( n.equals( "p" ) || n.equals( "div" ) )
        {
            String align = base.getAttributeValue( "align" );
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
            String color = base.getAttributeValue( "color" );
            String face = base.getAttributeValue( "face" );
            String size = base.getAttributeValue( "size" );
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
                
                // Handle links with section anchors. 
                // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                // to this wiki string "TargetPage#Heading2".
                ref = ref.replaceFirst( ".+#section-(.+)-(.+)", "$1#$2" );                
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
