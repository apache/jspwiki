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

import java.util.*;

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

    public static final String RSS10 = "rss10";
    public static final String RSS20 = "rss20";
    public static final String ATOM  = "atom";
    
    public static final String MODE_BLOG = "blog";
    public static final String MODE_WIKI = "wiki";
    public static final String MODE_FULL = "full";
    
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

    public static final String PROP_CHANNEL_TITLE       = "jspwiki.rss.channelTitle";
    
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

    public static final String PROP_RSSAUTHOR           = "jspwiki.rss.author";
    public static final String PROP_RSSAUTHOREMAIL      = "jspwiki.rss.author.email";
    
    /**
     *  Defines the property name for the RSS generation interval in seconds.
     *  @since 1.7.6.
     */
    public static final String PROP_INTERVAL            = "jspwiki.rss.interval";

    public static final String PROP_RSS_AUTHOR          = "jspwiki.rss.author";
    public static final String PROP_RSS_AUTHOREMAIL     = "jspwiki.rss.author.email";
    public static final String PROP_RSS_COPYRIGHT       = "jspwiki.rss.copyright";
    
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
        WikiContext context = new WikiContext( m_engine,new WikiPage( m_engine, "__DUMMY" ) );
        context.setRequestContext( WikiContext.RSS );
        Feed feed = new RSS10Feed( context );
        
        String result = generateFullWikiRSS( context, feed );
        
        result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + result;
        
        return result;
    }

    /**
     *  @since 2.3.15
     * @param mode
     * @return
     */
    public static String getContentType( String mode )
    {
        if( mode.equals( RSS10 )||mode.equals(RSS20) )
        {
            return "application/rss+xml";
        }
        else if( mode.equals(ATOM) )
        {
            return "application/atom+xml";
        }
       
        return "application/octet-stream"; // Unknown type
    }
    
    public String generateFeed( WikiContext wikiContext, List changed, String mode, String type )
        throws ProviderException
    {
        Feed feed = null;
        String res = null;
        
        if( type.equals( ATOM ) )
        {
            feed = new AtomFeed( wikiContext );
        }
        else if( type.equals( RSS20 ) )
        {
            feed = new RSS20Feed( wikiContext );
        }
        else
        {
            feed = new RSS10Feed( wikiContext );
        }
        
        if( mode.equals( MODE_BLOG ) )
        {
            res = generateBlogRSS( wikiContext, changed, feed );
        }
        else if( mode.equals( MODE_FULL ) )
        {
            res = generateFullWikiRSS( wikiContext, feed );
        }
        else if( mode.equals( MODE_WIKI ) )
        {
            res = generateWikiPageRSS( wikiContext, changed, feed );
        }
        else
            throw new IllegalArgumentException( "Invalid value for feed mode: "+mode );
        
        return res;
    }
    
    /**
     *  Generates an RSS feed for the entire wiki.  Each item should be an instance of the RSSItem class.
     */
    protected String generateFullWikiRSS( WikiContext wikiContext, Feed feed )
    {
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

    protected String generateWikiPageRSS( WikiContext wikiContext, List changed, Feed feed )
    {        
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

    
    protected String generateBlogRSS( WikiContext wikiContext, List changed, Feed feed )
        throws ProviderException
    {
        log.debug("Generating RSS for blog, size="+changed.size());
        
        String ctitle = m_engine.getVariable( wikiContext, PROP_CHANNEL_TITLE );
        
        if( ctitle != null )
            feed.setChannelTitle( ctitle );
        else
            feed.setChannelTitle( m_engine.getApplicationName()+":"+wikiContext.getPage().getName() );
        
        feed.setFeedURL( wikiContext.getViewURL( wikiContext.getPage().getName() ) );
        
        String language = m_engine.getVariable( wikiContext, PROP_CHANNEL_LANGUAGE );
        
        if( language != null )
            feed.setChannelLanguage( language );
        else
            feed.setChannelLanguage( m_channelLanguage );
        
        String channelDescription = m_engine.getVariable( wikiContext, PROP_CHANNEL_DESCRIPTION );
        
        if( channelDescription != null )
        {
            feed.setChannelDescription( channelDescription );
        }

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
            
            String pageText = m_engine.getPureText(page.getName(), WikiProvider.LATEST_VERSION );

            String title = "";
            int firstLine = pageText.indexOf('\n');

            if( firstLine > 0 )
            {
                title = pageText.substring( 0, firstLine ).trim();
            }
            
            if( title.length() == 0 ) title = page.getName();

            // Remove wiki formatting
            while( title.startsWith("!") ) title = title.substring(1);
            
            e.setTitle( title );
            
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
                    
                    e.setContent( pageText );
                }
                else
                {
                    e.setContent( title );
                }
            }
            else
            {
                e.setContent( title );
            }

            e.setAuthor( getAuthor(page) );
            
            feed.addEntry( e );
        }
        
        return feed.getString();
    }

}
