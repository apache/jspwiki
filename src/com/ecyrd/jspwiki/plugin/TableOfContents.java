/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2004 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.plugin;

import org.apache.log4j.Logger;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.parser.Heading;
import com.ecyrd.jspwiki.parser.HeadingListener;
import com.ecyrd.jspwiki.parser.JSPWikiMarkupParser;
import com.ecyrd.jspwiki.render.RenderingManager;

import java.util.*;
import java.io.StringReader;
import java.io.IOException;

/**
 *  Provides a table of contents.
 *
 *  @since 2.2
 *  @author Janne Jalkanen
 */
public class TableOfContents
    implements WikiPlugin, HeadingListener
{
    private static Logger log = Logger.getLogger( TableOfContents.class );

    public static final String PARAM_TITLE = "title";

    StringBuffer m_buf = new StringBuffer();

    public void headingAdded( WikiContext context, Heading hd )
    {
        log.debug("HD: "+hd.m_level+", "+hd.m_titleText+", "+hd.m_titleAnchor);

        switch( hd.m_level )
        {
          case Heading.HEADING_SMALL:
            m_buf.append("<li class=\"toclevel-3\">");
            break;
          case Heading.HEADING_MEDIUM:
            m_buf.append("<li class=\"toclevel-2\">");
            break;
          case Heading.HEADING_LARGE:
            m_buf.append("<li class=\"toclevel-1\">");
            break;
          default:
            throw new InternalWikiException("Unknown depth in toc! (Please submit a bug report.)");
        }

        String url = context.getURL( WikiContext.VIEW, context.getPage().getName() );
        String sectref = "#section-"+context.getEngine().encodeName(context.getPage().getName())+"-"+hd.m_titleSection;

        m_buf.append( "<a class=\"wikipage\" href=\""+url+sectref+"\">"+hd.m_titleText+"</a></li>\n" );
    }

    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        WikiPage   page   = context.getPage();

        StringBuffer sb = new StringBuffer();

        sb.append("<div class=\"toc\">\n");

        String title = (String) params.get(PARAM_TITLE);
        
        if( title != null )
        {
            sb.append("<h4>"+TextUtil.replaceEntities(title)+"</h4>\n");
        }
        else
        {
            sb.append("<h4>Table of Contents</h4>\n");
        }

        try
        {
            String wikiText = engine.getPureText( page );
            
            JSPWikiMarkupParser parser = new JSPWikiMarkupParser( context,
                                                                  new StringReader(wikiText) );
            parser.addHeadingListener( this );

            parser.parse();

            sb.append( "<ul>\n"+m_buf.toString()+"</ul>\n" );
        }
        catch( IOException e )
        {
            log.error("Could not construct table of contents", e);
            throw new PluginException("Unable to construct table of contents (see logs)");
        }

        sb.append("</div>\n");

        return sb.toString();
    }

}
