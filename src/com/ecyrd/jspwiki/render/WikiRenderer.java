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

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.parser.MarkupParser;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Provides an interface to the basic rendering engine.
 *  This class is an abstract class instead of an interface because
 *  it is expected that rendering capabilities are increased at some
 *  point, and I would hate if renderers broke.  This class allows
 *  some sane defaults to be implemented.
 *  
 *  @author jalkanen
 *  @since  2.4
 */
public abstract class WikiRenderer
{
    protected WikiContext     m_context;
    protected WikiDocument    m_document;
    protected boolean         m_enablePlugins = true;
    
    protected WikiRenderer( WikiContext context, WikiDocument doc )
    {
        m_context = context;
        m_document = doc;
        doc.setContext( context ); // Make sure it is set
        
        //
        //  Do some sane defaults
        //
        WikiEngine engine = m_context.getEngine();
        String runplugins = engine.getVariable( m_context, MarkupParser.PROP_RUNPLUGINS );
        if( runplugins != null ) enablePlugins( TextUtil.isPositive(runplugins));
    }

    /**
     *  Can be used to turn on plugin execution on a translator-reader basis
     */
    public void enablePlugins( boolean toggle )
    {
        m_enablePlugins = toggle;
    }

    public abstract String getString()
        throws IOException;

}
