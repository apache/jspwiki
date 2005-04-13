/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.rss;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Generates an RSS feed from the recent changes.
 *  <P>
 *  We use the 1.0 spec, including the wiki-specific extensions.  Wiki extensions
 *  have been defined in <A HREF="http://usemod.com/cgi-bin/mb.pl?ModWiki">UseMod:ModWiki</A>.
 *
 *  @author Janne Jalkanen
 *  @since  1.7.5.
 */
// FIXME: Limit diff and page content size.
public class RSSGenerator
{
    static Logger              log = Logger.getLogger( RSSGenerator.class );
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

    private static final int MAX_CHARACTERS             = Integer.MAX_VALUE;
    
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
    public static String format( String s )
    {
        s = TextUtil.replaceString( s, "&", "&amp;" );
        s = TextUtil.replaceString( s, "<", "&lt;" );
        s = TextUtil.replaceString( s, "]]>", "]]&gt;" );

        return s.trim();
    }

    private String getAuthor( WikiPage page )
    {
        String author = page.getAuthor();

        if( author == null ) author = "An unknown author";

        return author;
    }

    private String getAttachmentDescription( Attachment att )
    {
        String author = getAuthor(att);
        StringBuffer sb = new StringBuffer();
        
        if( att.getVersion() != 1 )
        {
            sb.append(author+" uploaded a new version of this attachment on "+att.getLastModified() );
        }
        else
        {
            sb.append(author+" created this attachment on "+att.getLastModified() );
        }
        
        sb.append("<br /><hr /><br />");
        sb.append( "Parent page: <a href=\""+
                   m_engine.getURL( WikiContext.VIEW, att.getParentName(), null, true ) +
                   "\">"+att.getParentName()+"</a><br />" );
        sb.append( "Info page: <a href=\""+
                   m_engine.getURL( WikiContext.INFO, att.getName(), null, true ) +
                   "\">"+att.getName()+"</a>" );
        
        return sb.toString();
    }

    private String getPageDescription( WikiPage page )
    {
        StringBuffer buf = new StringBuffer();
        String author = getAuthor(page);

        if( page.getVersion() > 1 )
        {
            String diff = m_engine.getDiff( page.getName(),
                                            page.getVersion()-1, // FIXME: Will fail when non-contiguous versions
                                            page.getVersion() );

            buf.append(author+" changed this page on "+page.getLastModified()+":<br /><hr /><br />" );
            buf.append(diff);
        }
        else
        {
            buf.append(author+" created this page on "+page.getLastModified()+":<br /><hr /><br />" );
            buf.append(m_engine.getHTML( page.getName() ));
        }

        return buf.toString();
    }

    private String getEntryDescription( WikiPage page )
    {
        String res;

        if( page instanceof Attachment ) 
        {
            res = getAttachmentDescription( (Attachment)page );
        }
        else
        {
            res = getPageDescription( page );
        }

        return res;
    }

    private String getEntryTitle( WikiPage page )
    {
        return page.getName();
    }

    /**
     *  Generates the RSS resource.  You probably want to output this
     *  result into a file or something, or serve as output from a servlet.
     */
    public String generate()
    {
        WikiContext context = new WikiContext(m_engine,new WikiPage("__DUMMY"));
        context.setRequestContext( WikiContext.RSS );
        
        String result = generateWikiRSS( context );
        
        result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;
        
        return result;
    }
/*
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

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH, 
                                       page.getName(),
                                       null,
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW, 
                                       page.getName(),
                                       null,
                                       true );
            }

            result.append("    <rdf:li rdf:resource=\""+url+"\" />\n");

            itemBuffer.append(" <item rdf:about=\""+url+"\">\n");

            itemBuffer.append("  <title>");
            itemBuffer.append( getEntryTitle(page) );
            itemBuffer.append("</title>\n");

            itemBuffer.append("  <link>");
            itemBuffer.append( url );
            itemBuffer.append("</link>\n");

            itemBuffer.append("  <description>");

            itemBuffer.append( format(getEntryDescription(page)) );

            itemBuffer.append("</description>\n");

            if( page.getVersion() != -1 )
            {
                itemBuffer.append("  <wiki:version>"+page.getVersion()+"</wiki:version>\n");
            }

            if( page.getVersion() > 1 )
            {
                itemBuffer.append("  <wiki:diff>"+
                                  m_engine.getURL(WikiContext.DIFF,
                                                  page.getName(),
                                                  "r1=-1",
                                                  true)+
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
            String author = getAuthor(page);
            itemBuffer.append("  <dc:contributor>\n");
            itemBuffer.append("   <rdf:Description");
            if( m_engine.pageExists(author) )
            {
                itemBuffer.append(" link=\""+m_engine.getURL(WikiContext.VIEW,author,null,true)+"\"");
            }
            itemBuffer.append(">\n");
            itemBuffer.append("    <rdf:value>"+author+"</rdf:value>\n");
            itemBuffer.append("   </rdf:Description>\n");
            itemBuffer.append("  </dc:contributor>\n");


            //  PageHistory

            itemBuffer.append("  <wiki:history>");
            itemBuffer.append( m_engine.getURL(WikiContext.INFO,
                                               page.getName(),
                                               null,
                                               true ) );
            itemBuffer.append("</wiki:history>\n");

            //  Close up.
            itemBuffer.append(" </item>\n");
        }

        result.append("   </rdf:Seq>\n  </items>\n");
        result.append(" </channel>\n");

        result.append( itemBuffer.toString() );

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
*/
    /**
     *  Generates an RSS/RDF 1.0 feed for the entire wiki.  Each item should be an instance of the RSSItem class.
     */
    public String generateWikiRSS( WikiContext wikiContext )
    {
        RSS10Feed feed = new RSS10Feed( wikiContext );
        
        feed.setChannelTitle( m_engine.getApplicationName() );
        feed.setFeedURL( m_engine.getBaseURL() );
        feed.setChannelLanguage( m_channelLanguage );
        feed.setChannelDescription( m_channelDescription );

        Collection changed = m_engine.getRecentChanges();

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();
            
            Entry e = new Entry();
            
            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH, 
                                       page.getName(),
                                       null,
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW, 
                                       page.getName(),
                                       null,
                                       true );
            }
            
            e.setURL( url );
            e.setTitle( getEntryTitle(page) );
            e.setContent( getEntryDescription(page) );
            e.setAuthor( getAuthor(page) );
            
            feed.addEntry( e );
        }
        
        return feed.getString();
    }

    public String generatePageRSS( WikiContext wikiContext, List changed )
    {
        RSS10Feed feed = new RSS10Feed( wikiContext );
        
        feed.setChannelTitle( m_engine.getApplicationName() );
        feed.setFeedURL( m_engine.getBaseURL() );
        feed.setChannelLanguage( m_channelLanguage );
        feed.setChannelDescription( m_channelDescription );

        Collections.sort( changed, new PageTimeComparator() );
                
        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();
            
            Entry e = new Entry();
            
            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH, 
                                       page.getName(),
                                       "version="+page.getVersion(),
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW, 
                                       page.getName(),
                                       "version="+page.getVersion(),
                                       true );
            }
            
            e.setURL( url );
            e.setTitle( getEntryTitle(page) );
            e.setContent( getEntryDescription(page) );
            e.setAuthor( getAuthor(page) );
            
            feed.addEntry( e );
        }
        
        return feed.getString();
    }

    
    public String generateBlogRSS( WikiContext wikiContext, List changed )
        throws ProviderException
    {
        RSS10Feed feed = new RSS10Feed( wikiContext );
        
        log.debug("Generating RSS for blog, size="+changed.size());
        
        feed.setChannelTitle( m_engine.getApplicationName()+":"+wikiContext.getPage().getName() );
        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );
        feed.setChannelLanguage( m_channelLanguage );
        
        String channelDescription;
        
        try
        {
            channelDescription = m_engine.getVariableManager().getValue( wikiContext, PROP_CHANNEL_DESCRIPTION );
            feed.setChannelDescription( format(channelDescription) );
        }
        catch( NoSuchVariableException e ) {}

        Collections.sort( changed, new PageTimeComparator() );

        int items = 0;
        for( Iterator i = changed.iterator(); i.hasNext() && items < 15; items++ )
        {
            WikiPage page = (WikiPage) i.next();
            
            Entry e = new Entry();
            
            e.setPage( page );

            String url;

            if( page instanceof Attachment )
            {
                url = m_engine.getURL( WikiContext.ATTACH, 
                                       page.getName(),
                                       null,
                                       true );
            }
            else
            {
                url = m_engine.getURL( WikiContext.VIEW, 
                                       page.getName(),
                                       null,
                                       true );
            }
            
            e.setURL( url );
            
            //
            //  Title
            //
            
            String pageText = m_engine.getText(page.getName());
            String title = "";
            int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 )
            {
                title = pageText.substring( 0, firstLine );
            }
            
            if( title.trim().length() == 0 ) title = page.getName();

            // Remove wiki formatting
            while( title.startsWith("!") ) title = title.substring(1);
            
            e.setTitle( format(title) );
            
            //
            //  Description
            //
            
            if( firstLine > 0 )
            {
                int maxlen = pageText.length();
                if( maxlen > MAX_CHARACTERS ) maxlen = MAX_CHARACTERS;

                if( maxlen > 0 )
                {
                    pageText = m_engine.textToHTML( wikiContext, 
                                                    pageText.substring( firstLine+1,
                                                                        maxlen ).trim() );
                    
                    if( maxlen == MAX_CHARACTERS ) pageText += "...";
                    
                    e.setContent( format(pageText) );
                }
                else
                {
                    e.setContent( format(title) );
                }
            }
            else
            {
                e.setContent( format(title) );
            }

            e.setAuthor( getAuthor(page) );
            
            feed.addEntry( e );
        }
        
        return feed.getString();
    }

}
