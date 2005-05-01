package com.ecyrd.jspwiki.htmltowiki;

import java.io.IOException;
import java.io.StringReader;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Converting Html to Wiki Markup with NekoHtml for converting html to xhtml and
 * Xhtml2WikiTranslator for converting xhtml to Wiki Markup.
 * 
 * @author Sebastian Baltes (sbaltes@gmx.com)
 */
public class HtmlStringToWikiTranslator
{

    public HtmlStringToWikiTranslator()
    {}

    public String translate( String html ) throws JDOMException, IOException
    {
        return translate( html, new XHtmlToWikiConfig() );
    }

    public String translate( String html, WikiContext wikiContext ) throws JDOMException, IOException
    {
        return translate( html, new XHtmlToWikiConfig( wikiContext ) );
    }

    public String translate( String html, XHtmlToWikiConfig config ) throws JDOMException, IOException
    {
        Element element = htmlStringToElement( html );
        XHtmlElementToWikiTranslator xhtmlTranslator = new XHtmlElementToWikiTranslator( element, config );
        String wikiMarkup = xhtmlTranslator.getWikiString();
        return wikiMarkup;
    }

    /**
     * use NekoHtml to parse HTML like well formed XHTML
     * 
     * @param html
     * @return xhtml jdom root element (node "HTML")
     * @throws JDOMException
     * @throws IOException
     */
    private Element htmlStringToElement( String html ) throws JDOMException, IOException
    {
        SAXBuilder builder = new SAXBuilder( "org.cyberneko.html.parsers.SAXParser", true );
        Document doc = builder.build( new StringReader( html ) );
        Element element = doc.getRootElement();
        return element;
    }

    public static String element2String( Element element )
    {
        Document document = new Document( element );
        XMLOutputter outputter = new XMLOutputter();
        return outputter.outputString( document );
    }

}
