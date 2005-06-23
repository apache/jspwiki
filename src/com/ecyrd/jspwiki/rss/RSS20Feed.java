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

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.Release;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.providers.ProviderException;

/**
 *  Represents an RSS 2.0 feed (with enclosures).
 *  
 *  @author jalkanen
 *
 *  @since 2.2.27
 */
public class RSS20Feed extends Feed
{

    public RSS20Feed( WikiContext context )
    {
        super( context );
    }

    private List getItems()
    {
        ArrayList list = new ArrayList();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");
        
        WikiEngine engine = m_wikiContext.getEngine();
        ServletContext servletContext = null;
        
        if( m_wikiContext.getHttpRequest() != null )
            servletContext = m_wikiContext.getHttpRequest().getSession().getServletContext();
        
        for( Iterator i = m_entries.iterator(); i.hasNext(); )
        {
            Entry e = (Entry)i.next();
            WikiPage p = e.getPage();
            
            String url = e.getURL();
            
            Element item = new Element("item");
            
            item.addContent( new Element("link").setText(url) );
            
            item.addContent( new Element("title").setText( format(e.getTitle())) );
         
            item.addContent( new Element("description").setText( format(e.getContent())) );
            
            //
            //  Attachments for enclosures
            //
            
            if( engine.getAttachmentManager().hasAttachments(p) && servletContext != null )
            {
                try
                {
                    Collection c = engine.getAttachmentManager().listAttachments(p);
                
                    for( Iterator a = c.iterator(); a.hasNext(); )
                    {
                        Attachment att = (Attachment) a.next();
                    
                        Element attEl = new Element("enclosure");
                        attEl.setAttribute( "url", engine.getURL(WikiContext.ATTACH, att.getName(), null, true ) );
                        attEl.setAttribute( "length", Long.toString(att.getSize()) );
                        attEl.setAttribute( "type", servletContext.getMimeType( att.getFileName() ) );
                        
                        item.addContent( attEl );
                    }
                }
                catch( ProviderException ex )
                {
                    // FIXME: log.info("Can't get attachment data",ex);
                }
            }
            
            //
            //  Modification date.
            //
            Calendar cal = Calendar.getInstance();
            cal.setTime( p.getLastModified() );
            cal.add( Calendar.MILLISECOND, 
                     - (cal.get( Calendar.ZONE_OFFSET ) + 
                        (cal.getTimeZone().inDaylightTime( p.getLastModified() ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );

            item.addContent( new Element("pubDate").setText(fmt.format(cal.getTime())) );
            
            list.add( item );
        }
        
        return list;
    }
    
    public String getString()
    {
        Element root = new Element("rss");
        root.setAttribute("version","2.0");
        
        Element channel = new Element("channel");
        root.addContent( channel );
        
        //
        //  Mandatory parts
        //
        channel.addContent( new Element("title").setText( format(getChannelTitle()) ) );
        channel.addContent( new Element("link").setText(m_wikiContext.getEngine().getBaseURL()));
        channel.addContent( new Element("description").setText( format(getChannelDescription()) ));
        
        //
        //  Optional
        //
        channel.addContent( new Element("language").setText(getChannelLanguage()));
        channel.addContent( new Element("generator").setText("JSPWiki "+Release.VERSTR));
        
        //
        //  Items
        //
        
        channel.addContent( getItems() );
        
        //
        //  aaand output
        //
        XMLOutputter output = new XMLOutputter();
        
        output.setFormat( Format.getPrettyFormat() );
        
        try
        {
            StringWriter res = new StringWriter();
            output.output( root, res );

            return res.toString();
        }
        catch( IOException e )
        {
            return null;
        }
    }

}
