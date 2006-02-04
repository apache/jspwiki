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
    public static final String PARAM_NUMBERED = "numbered";
    public static final String PARAM_START = "start";
    public static final String PARAM_PREFIX = "prefix";

    StringBuffer m_buf = new StringBuffer();
    private boolean m_usingNumberedList = false; 
    private String m_prefix = ""; 
    private int m_starting = 0; 
    private int m_level1Index = 0; 
    private int m_level2Index = 0; 
    private int m_level3Index = 0; 
    private int m_lastLevel = 0;

    public void headingAdded( WikiContext context, Heading hd )
    {
        log.debug("HD: "+hd.m_level+", "+hd.m_titleText+", "+hd.m_titleAnchor);

        switch( hd.m_level )
        {
          case Heading.HEADING_SMALL:
            m_buf.append("<li class=\"toclevel-3\">");
            m_level3Index++;
            break;
          case Heading.HEADING_MEDIUM:
            m_buf.append("<li class=\"toclevel-2\">");
            m_level2Index++;
            break;
          case Heading.HEADING_LARGE:
            m_buf.append("<li class=\"toclevel-1\">");
            m_level1Index++;
            break;
          default:
            throw new InternalWikiException("Unknown depth in toc! (Please submit a bug report.)");
        }

        if (m_level1Index < m_starting) 
        {
            // in case we never had a large heading ...
            m_level1Index++;
        }
        if ((m_lastLevel == Heading.HEADING_SMALL) && (hd.m_level != Heading.HEADING_SMALL)) 
        {
            m_level3Index = 0;
        }
        if ( ((m_lastLevel == Heading.HEADING_SMALL) || (m_lastLevel == Heading.HEADING_MEDIUM)) &&
                  (hd.m_level == Heading.HEADING_LARGE) ) 
        {
            m_level3Index = 0;
            m_level2Index = 0;
        }

        String titleSection = hd.m_titleSection.replace( '%', '_' );
        
        String url = context.getURL( WikiContext.VIEW, context.getPage().getName() );
        String sectref = "#section-"+context.getEngine().encodeName(context.getPage().getName())+"-"+titleSection;

        m_buf.append( "<a class=\"wikipage\" href=\""+url+sectref+"\">");
        if (m_usingNumberedList) 
        {
            switch( hd.m_level )
            {
            case Heading.HEADING_SMALL:
                m_buf.append(m_prefix + m_level1Index + "." + m_level2Index + "."+ m_level3Index +" ");
                break;
            case Heading.HEADING_MEDIUM:
                m_buf.append(m_prefix + m_level1Index + "." + m_level2Index + " ");
                break;
            case Heading.HEADING_LARGE:
                m_buf.append(m_prefix + m_level1Index +" ");
                break;
            default:
                throw new InternalWikiException("Unknown depth in toc! (Please submit a bug report.)");
            }
        }
        m_buf.append( hd.m_titleText+"</a></li>\n" );

        m_lastLevel = hd.m_level;
    }
    
    public String execute( WikiContext context, Map params )
        throws PluginException
    {
        WikiEngine engine = context.getEngine();
        WikiPage   page   = context.getPage();

        StringBuffer sb = new StringBuffer();

        sb.append("<div class=\"toc\">\n");
        sb.append("<div class=\"collapsebox\">\n");

        String title = (String) params.get(PARAM_TITLE);
        if( title != null )
        {
            sb.append("<h4>"+TextUtil.replaceEntities(title)+"</h4>\n");
        }
        else
        {
            sb.append("<h4>Table of Contents</h4>\n");
        }

        // should we use an ordered list?
        m_usingNumberedList = false;
        if (params.containsKey(PARAM_NUMBERED)) 
        {
            String numbered = (String)params.get(PARAM_NUMBERED);
            if (numbered.equalsIgnoreCase("true")) 
            {
                m_usingNumberedList = true;
            }
            else if (numbered.equalsIgnoreCase("yes")) 
            {
                m_usingNumberedList = true;
            }
        }
        
        // if we are using a numbered list, get the rest of the parameters (if any) ...
        if (m_usingNumberedList) 
        {
            int start = 0;
            String startStr = (String)params.get(PARAM_START);
            if ((startStr != null) && (startStr.matches("^\\d+$"))) 
            {
                start = Integer.parseInt(startStr);
            }
            if (start < 0) start = 0;
            
            m_starting = start;
            m_level1Index = start - 1;
            if (m_level1Index < 0) m_level1Index = 0;
            m_level2Index = 0;
            m_level3Index = 0;
            m_prefix = (String)params.get(PARAM_PREFIX);
            if (m_prefix == null) m_prefix = "";
            m_lastLevel = Heading.HEADING_LARGE;
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

        sb.append("</div>\n</div>\n");

        return sb.toString();
    }

}
