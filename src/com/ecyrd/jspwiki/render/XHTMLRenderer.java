/* 
  JSPWiki - a JSP-based WikiWiki clone.

  Copyright (C) 2001-2006 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Implements a WikiRendered that outputs XHTML.  Because the internal DOM
 *  representation is in XHTML already, this just basically dumps out everything
 *  out in a non-prettyprinted format.
 *   
 *  @author jalkanen
 *  @since  2.4
 */
public class XHTMLRenderer
    extends WikiRenderer 
{
    public XHTMLRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    public String getString()
        throws IOException
    {
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
