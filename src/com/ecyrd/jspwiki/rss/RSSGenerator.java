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
import java.util.Calendar;
import java.text.SimpleDateFormat;

import com.ecyrd.jspwiki.*;

/**
 *  Generates an RSS feed from the recent changes.
 *  <P>
 *  We use the 1.0 spec, including the wiki-specific extensions.  Wiki extensions
 *  have been defined in <A HREF="http://usemod.com/cgi-bin/mb.pl?ModWiki">UseMod:ModWiki</A>.
 *
 *  @author Janne Jalkanen
 *  @since  1.7.5.
 */
public class RSSGenerator
{
    private WikiEngine         m_engine;

    private String             m_channelDescription = "";
    private String             m_channelLanguage    = "en-us";

    /**
     *  Defines the property name for the RSS channel description.  Default value for the 
     *  channel description is an empty string.
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_DESCRIPTION = "jspwiki.rss.channelDescription";

    /**
     *  Defines the property name for the RSS channel language.  Default value for the
     *  language is "en-us".
     *  @since 1.7.6.
     */
    public static final String PROP_CHANNEL_LANGUAGE    = "jspwiki.rss.channelLanguage";

    /**
     *  Defines the property name for the RSS generator main switch.
     *  @since 1.7.6.
     */
    public static final String PROP_GENERATE_RSS        = "jspwiki.rss.generate";

    /**
     *  Defines the property name for the RSS file that the wiki should generate.
     *  @since 1.7.6.
     */
    public static final String PROP_RSSFILE             = "jspwiki.rss.fileName";

    /**
     *  Defines the property name for the RSS generation interval in seconds.
     *  @since 1.7.6.
     */
    public static final String PROP_INTERVAL            = "jspwiki.rss.interval";

    /**
     *  Initialize the RSS generator.
     */
    public RSSGenerator( WikiEngine engine, Properties properties )
        throws NoRequiredPropertyException
    {
        m_engine = engine;        

        // FIXME: This assumes a bit too much.
        if( engine.getBaseURL() == null || engine.getBaseURL().length() == 0 )
        {
            throw new NoRequiredPropertyException( "RSS requires jspwiki.baseURL to be set!",
                                                   WikiEngine.PROP_BASEURL );
        }

        m_channelDescription = properties.getProperty( PROP_CHANNEL_DESCRIPTION, 
                                                       m_channelDescription );
        m_channelLanguage    = properties.getProperty( PROP_CHANNEL_LANGUAGE,
                                                       m_channelLanguage );
    }

    /**
     *  Does the required formatting and entity replacement for XML.
     */
    private String format( String s )
    {
        s = TextUtil.replaceString( s, "&", "&amp;" );
        s = TextUtil.replaceString( s, "<", "&lt;" );
        s = TextUtil.replaceString( s, "]]>", "]]&gt;" );

        return s;
    }

    /**
     *  Generates the RSS resource.  You probably want to output this
     *  result into a file or something, or serve as output from a servlet.
     */
    public String generate()
    {
        StringBuffer result = new StringBuffer();
        SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

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
        result.append( format(m_channelDescription) );
        result.append("</description>\n");
        
        result.append("  <language>");
        result.append( m_channelLanguage );
        result.append("</language>\n");

        //
        //  Now, list items.
        //

        Collection changed = m_engine.getRecentChanges();

        //  We need two lists, which is why we gotta make a separate list if
        //  we want to do just a single pass.
        StringBuffer itemBuffer = new StringBuffer();

        result.append("  <items>\n   <rdf:Seq>\n");

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();

            String encodedName = m_engine.encodeName(page.getName());

            String url = m_engine.getViewURL(page.getName());

            result.append("    <rdf:li rdf:resource=\""+url+"\" />\n");

            itemBuffer.append(" <item rdf:about=\""+url+"\">\n");

            itemBuffer.append("  <title>");
            itemBuffer.append( page.getName() );
            itemBuffer.append("</title>\n");

            itemBuffer.append("  <link>");
            itemBuffer.append( url );
            itemBuffer.append("</link>\n");

            itemBuffer.append("  <description>");

            String author = page.getAuthor();
            if( author == null ) author = "An unknown author";

            if( page.getVersion() != 1 )
            {
                itemBuffer.append(author+" changed this page on "+page.getLastModified() );
            }
            else
            {
                itemBuffer.append(author+" created this page on "+page.getLastModified() );
            }
            itemBuffer.append("</description>\n");

            if( page.getVersion() != -1 )
            {
                itemBuffer.append("  <wiki:version>"+page.getVersion()+"</wiki:version>\n");
            }

            if( page.getVersion() > 1 )
            {
                itemBuffer.append("  <wiki:diff>"+
                              m_engine.getBaseURL()+"Diff.jsp?page="+
                              encodedName+
                              "&amp;r1=-1"+
                              "</wiki:diff>\n");
            }

            //
            //  Modification date.
            //
            itemBuffer.append("  <dc:date>");
            Calendar cal = Calendar.getInstance();
            cal.setTime( page.getLastModified() );
            cal.add( Calendar.MILLISECOND, 
                     - (cal.get( Calendar.ZONE_OFFSET ) + 
                        (cal.getTimeZone().inDaylightTime( page.getLastModified() ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );
            itemBuffer.append( iso8601fmt.format( cal.getTime() ) );
            itemBuffer.append("</dc:date>\n");

            //
            //  Author.
            //
            itemBuffer.append("  <dc:contributor>\n");
            itemBuffer.append("   <rdf:Description");
            if( m_engine.pageExists(author) )
            {
                itemBuffer.append(" link=\""+m_engine.getViewURL(author)+"\"");
            }
            itemBuffer.append(">\n");
            itemBuffer.append("    <rdf:value>"+author+"</rdf:value>\n");
            itemBuffer.append("   </rdf:Description>\n");
            itemBuffer.append("  </dc:contributor>\n");


            //  PageHistory

            itemBuffer.append("  <wiki:history>");
            itemBuffer.append( m_engine.getBaseURL()+"PageInfo.jsp?page="+
                           encodedName );
            itemBuffer.append("</wiki:history>\n");

            //  Close up.
            itemBuffer.append(" </item>\n");
        }

        result.append("   </rdf:Seq>\n  </items>\n");
        result.append(" </channel>\n");

        result.append( itemBuffer );

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
