/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.rss;

import org.apache.wiki.Release;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

/**
 *  Represents an RSS 2.0 feed (with enclosures).  This feed provides no
 *  fizz-bang features.
 *
 *  @since 2.2.27
 */
public class RSS20Feed extends Feed
{
    /**
     *  Creates an RSS 2.0 feed for the specified Context.
     *
     *  @param context The WikiContext.
     */
    public RSS20Feed( WikiContext context )
    {
        super( context );
    }

    private List<Element> getItems()
    {
        ArrayList<Element> list = new ArrayList<>();
        SimpleDateFormat fmt = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'");

        WikiEngine engine = m_wikiContext.getEngine();
        ServletContext servletContext = null;

        if( m_wikiContext.getHttpRequest() != null )
            servletContext = m_wikiContext.getHttpRequest().getSession().getServletContext();

        for( Iterator< Entry > i = m_entries.iterator(); i.hasNext(); )
        {
            Entry e = i.next();
            WikiPage p = e.getPage();

            String url = e.getURL();

            Element item = new Element("item");

            item.addContent( new Element("link").setText(url) );

            item.addContent( new Element("title").setText( e.getTitle()) );

            item.addContent( new Element("description").setText( e.getContent()) );

            //
            //  Attachments for enclosures
            //

            if( engine.getAttachmentManager().hasAttachments(p) && servletContext != null )
            {
                try
                {
                    List< Attachment > c = engine.getAttachmentManager().listAttachments(p);

                    for( Iterator< Attachment > a = c.iterator(); a.hasNext(); )
                    {
                        Attachment att = a.next();

                        Element attEl = new Element("enclosure");
                        attEl.setAttribute( "url", engine.getURL( WikiContext.ATTACH, att.getName(), null ) );
                        attEl.setAttribute( "length", Long.toString(att.getSize()) );
                        attEl.setAttribute( "type", getMimeType( servletContext, att.getFileName() ) );

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

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getString()
    {
        WikiEngine engine = m_wikiContext.getEngine();
        Element root = new Element("rss");
        root.setAttribute("version","2.0");

        Element channel = new Element("channel");
        root.addContent( channel );

        //
        //  Mandatory parts
        //
        channel.addContent( new Element("title").setText( getChannelTitle() ) );
        channel.addContent( new Element("link").setText(engine.getBaseURL()));
        channel.addContent( new Element("description").setText( getChannelDescription() ));

        //
        //  Optional
        //
        channel.addContent( new Element("language").setText(getChannelLanguage()));
        channel.addContent( new Element("generator").setText("JSPWiki "+Release.VERSTR));

        String mail = engine.getVariableManager().getVariable(m_wikiContext,RSSGenerator.PROP_RSS_AUTHOREMAIL);
        if( mail != null )
        {
            String editor = engine.getVariableManager().getVariable( m_wikiContext,RSSGenerator.PROP_RSS_AUTHOR );

            if( editor != null )
                mail = mail + " ("+editor+")";

            channel.addContent( new Element("managingEditor").setText(mail) );
        }

        //
        //  Items
        //

        channel.addContent( getItems() );

        //
        //  aaand output
        //
        XMLOutputter output = new XMLOutputter();

        output.setFormat( Format.getPrettyFormat() );

        try {
            final StringWriter res = new StringWriter();
            output.output( root, res );

            return res.toString();
        } catch( final IOException e ) {
            return null;
        }
    }

}
