/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.rss;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

import com.ecyrd.jspwiki.*;

/**
 *  Generates an RSS feed from the recent changes.
 *  <P>
 *  We use the 1.0 spec.  We'll add Wiki-specific extensions soon.
 *
 *  @author Janne Jalkanen
 *  @since  1.7.5.
 */
public class RSSGenerator
{
    private String m_channelName;
    private WikiEngine m_engine;

    /**
     *  Initialize the RSS generator.
     */
    public RSSGenerator( WikiEngine engine, Properties properties )
    {
        m_engine = engine;        

        // FIXME: add check for baseURL - it must exist.
    }

    /**
     *  Generates the RSS resource.  You probably want to output this
     *  result into a file or something, or serve as output from a servlet.
     */
    public String generate()
    {
        StringBuffer result = new StringBuffer();

        //
        //  Preamble
        //
        result.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

        result.append("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"\n"+
                      "   xmlns=\"http://purl.org/rss/1.0/\"\n"+
                      "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"+
                      "   xmlns:wiki=\"http://purl.org/rss/1.0/modules/wiki/\">\n");

        //
        //  Channel.
        //

        result.append(" <channel rdf:about=\""+m_engine.getBaseURL()+"\">\n");

        result.append("  <title>").append(m_engine.getApplicationName()).append("</title>\n");

        // FIXME: This might fail in case the base url is not defined.
        result.append("  <link>").append(m_engine.getBaseURL()).append("</link>\n");

        result.append("  <description>");
        result.append("FIXME");
        result.append("</description>\n");
        
        result.append("  <language>");
        result.append("en-us");
        result.append("</language>\n");

        // FIXME: add resource list here.

        result.append(" </channel>\n");

        //
        //  Now, list items.
        //

        Collection changed = m_engine.getRecentChanges();

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();

            String url = m_engine.getBaseURL()+"Wiki.jsp?page="+m_engine.encodeName(page.getName());

            result.append(" <item rdf:about=\""+url+"\">\n");

            result.append("  <title>");
            result.append( page.getName() );
            result.append("</title>\n");

            result.append("  <link>");
            result.append( url );
            result.append("</link>\n");

            result.append(" </item>\n");
        }

        //
        //  In the end, add a search box for JSPWiki
        //
        
        String searchURL = m_engine.getBaseURL()+"Search.jsp";

        result.append(" <textinput rdf:about=\""+searchURL+"\">\n");

        result.append("  <title>Search</title>\n");
        result.append("  <description>Search this Wiki</description>\n");
        result.append("  <name>query</name>\n");
        result.append("  <link>"+searchURL+"</link>\n");

        result.append(" </textinput>\n");

        //
        //  Be a fine boy and close things.
        //


        result.append("</rdf:RDF>");

        return result.toString();
    }
}
