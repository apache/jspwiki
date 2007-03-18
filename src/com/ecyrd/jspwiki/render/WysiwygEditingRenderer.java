/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation; either version 2.1 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with this program; if not, write to the Free Software
  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

import com.ecyrd.jspwiki.htmltowiki.XHtmlToWikiConfig;

/**
 *  Implements a WikiRendered that outputs XHTML in a format that is suitable
 *  for use by a WYSIWYG XHTML editor.
 *   
 *  @author David Au
 *  @since  2.5
 */
public class WysiwygEditingRenderer
    extends WikiRenderer
{
    
    public WysiwygEditingRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    /*
     * Recursively walk the XHTML DOM tree and manipulate specific elements to
     * make them better for WYSIWYG editing.
     */
    private void processChildren(Element baseElement)
    {
        for( Iterator itr = baseElement.getChildren().iterator(); itr.hasNext(); )
        {
            Object childElement = itr.next();
            if( childElement instanceof Element )
            {
                Element element = (Element)childElement;
                String elementName = element.getName().toLowerCase();
                Attribute classAttr = element.getAttribute( "class" );

                if( elementName.equals( "a" ) )
                {
                    if( classAttr != null )
                    {
                        String classValue = classAttr.getValue();
                        Attribute hrefAttr = element.getAttribute("href");
                        
                        XHtmlToWikiConfig wikiConfig = new XHtmlToWikiConfig( m_context );
                        
                        if( classValue.equals( "wikipage" ) )
                        {
                            // get the url for wiki page link - it's typically "Wiki.jsp?page=MyPage"
                            // or when using the ShortURLConstructor option, it's "wiki/MyPage" .
                            String wikiPageLinkUrl = wikiConfig.getWikiJspPage();

                            if( hrefAttr != null && hrefAttr.getValue().startsWith( wikiPageLinkUrl ) ) // we might not need this check
                            {
                                String newHref = null;

                                // Remove the leading url string so that users will only see the
                                // wikipage's name when editing an existing wiki link.
                                // For example, change "Wiki.jsp?page=MyPage" to just "MyPage".
                                newHref = hrefAttr.getValue().substring( wikiPageLinkUrl.length() );
                                
                                // Handle links with section anchors.
                                // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                                // to this wiki string: "TargetPage#Heading2".
                                hrefAttr.setValue( newHref.replaceFirst( ".+#section-(.+)-(.+)", "$1#$2" ) );
                            }
                        }
                        else if( classValue.equals( "editpage" ) )
                        {
                            String editPageLinkUrl = wikiConfig.getEditJspPage();

                            if( hrefAttr != null && hrefAttr.getValue().startsWith( editPageLinkUrl ) ) // we might not need this check
                            {
                                Attribute titleAttr = element.getAttribute( "title" );
                                titleAttr.detach();
                                
                                String newHref = null;
                                newHref = hrefAttr.getValue().substring( editPageLinkUrl.length() );
                                hrefAttr.setValue( newHref.replaceFirst( ".+#section-(.+)-(.+)", "$1#$2" ) );
                                                             
                                // only remove the href if it's the same as the link text.
                                if( hrefAttr.getValue().equals( element.getText() ) )
                                {
                                    hrefAttr.detach();
                                }
                            }
                        }
                    }
                } // end of check for "a" element

                processChildren( element );
            }
        }
    }
    
    public String getString()
        throws IOException
    {
        Element rootElement = m_document.getRootElement();
        processChildren( rootElement );
        
        m_document.setContext( m_context );

        XMLOutputter output = new XMLOutputter();
        
        StringWriter out = new StringWriter();
        
        Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator("\n");

        output.setFormat( fmt );
        output.outputElementContent( m_document.getRootElement(), out );
        
        return out.toString();
    }
}
