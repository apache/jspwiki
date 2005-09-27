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
            m_buf.append("***");
            break;
          case Heading.HEADING_MEDIUM:
            m_buf.append("**");
            break;
          case Heading.HEADING_LARGE:
            m_buf.append("*");
            break;
          default:
            throw new InternalWikiException("Unknown depth in toc! (Please submit a bug report.)");
        }

        m_buf.append(" ["+hd.m_titleText+"|"+context.getPage().getName()+"#"+hd.m_titleSection+"]\n");
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
            
            TranslatorReader in = new TranslatorReader( context,
                                                        new StringReader(wikiText) );
            in.enablePlugins( false );
            in.addHeadingListener( this );

            FileUtil.readContents( in );

            in.close();
            
            in = new TranslatorReader( context,
                                       new StringReader( m_buf.toString() ) );
            sb.append(FileUtil.readContents( in ));
            
            in.close();
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
