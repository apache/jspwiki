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

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.wiki.Release;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import javax.servlet.ServletContext;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 *  Provides an Atom 1.0 standard feed, with enclosures.
 *
 */
public class AtomFeed extends Feed {

    private Namespace m_atomNameSpace = Namespace.getNamespace("http://www.w3.org/2005/Atom");

    /** Defines a SimpleDateFormat string for RFC3339-formatted dates. */
    public static final String RFC3339FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZ";

    /**
     *  Create a new AtomFeed for a given WikiContext.
     *  
     *  @param c A WikiContext.
     */
    public AtomFeed( final WikiContext c )
    {
        super(c);
    }

    /**
     *   This is a bit complicated right now, as there is no proper metadata store in JSPWiki.
     *
     *   @return An unique feed ID.
     */
    private String getFeedID()
    {
        return m_wikiContext.getEngine().getBaseURL(); // FIXME: This is not a feed id
    }

    private String getEntryID( final Entry e )
    {
        return e.getURL(); // FIXME: Not really a feed id!
    }

    private Collection<Element> getItems() {
        final ArrayList< Element > list = new ArrayList<>();
        final WikiEngine engine = m_wikiContext.getEngine();
        ServletContext servletContext = null;
        if( m_wikiContext.getHttpRequest() != null ) {
            servletContext = m_wikiContext.getHttpRequest().getSession().getServletContext();
        }

        for( final Entry e : m_entries ) {
            final WikiPage p = e.getPage();
            final Element entryEl = getElement( "entry" );

            //  Mandatory elements
            entryEl.addContent( getElement( "id" ).setText( getEntryID( e ) ) );
            entryEl.addContent( getElement( "title" ).setAttribute( "type", "html" ).setText( e.getTitle() ) );
            entryEl.addContent( getElement( "updated" ).setText( DateFormatUtils.formatUTC( p.getLastModified(), RFC3339FORMAT ) ) );

            //  Optional elements
            entryEl.addContent( getElement( "author" ).addContent( getElement( "name" ).setText( e.getAuthor() ) ) );
            entryEl.addContent( getElement( "link" ).setAttribute( "rel", "alternate" ).setAttribute( "href", e.getURL() ) );
            entryEl.addContent( getElement( "content" ).setAttribute( "type", "html" ).setText( e.getContent() ) );

            //  Check for enclosures
            if( engine.getAttachmentManager().hasAttachments( p ) && servletContext != null ) {
                try {
                    final List< Attachment > c = engine.getAttachmentManager().listAttachments( p );
                    for( final Attachment att : c ) {
                        final Element attEl = getElement( "link" );
                        attEl.setAttribute( "rel", "enclosure" );
                        attEl.setAttribute( "href", engine.getURL( WikiContext.ATTACH, att.getName(), null ) );
                        attEl.setAttribute( "length", Long.toString( att.getSize() ) );
                        attEl.setAttribute( "type", getMimeType( servletContext, att.getFileName() ) );

                        entryEl.addContent( attEl );
                    }
                } catch( final ProviderException ex ) {
                    // FIXME: log.info("Can't get attachment data",ex);
                }
            }

            list.add( entryEl );
        }

        return list;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getString() {
        final Element root = getElement("feed");
        final WikiEngine engine = m_wikiContext.getEngine();

        Date lastModified = new Date(0L);

        for( final Entry e : m_entries ) {
            if( e.getPage().getLastModified().after( lastModified ) )
                lastModified = e.getPage().getLastModified();
        }

        //  Mandatory parts
        root.addContent( getElement("title").setText( getChannelTitle() ) );
        root.addContent( getElement("id").setText(getFeedID()) );
        root.addContent( getElement("updated").setText(DateFormatUtils.formatUTC( lastModified,
                                                                                  RFC3339FORMAT ) ));

        //  Optional
        // root.addContent( getElement("author").addContent(getElement("name").setText(format())))
        root.addContent( getElement("link").setAttribute("href",engine.getBaseURL()));
        root.addContent( getElement("generator").setText("JSPWiki "+Release.VERSTR));

        final String rssFeedURL  = engine.getURL(WikiContext.NONE, "rss.jsp",
                                                 "page=" + engine.encodeName( m_wikiContext.getPage().getName() ) +
                                                 "&mode=" + m_mode +
                                                 "&type=atom" );
        final Element self = getElement( "link" ).setAttribute( "rel","self" );
        self.setAttribute( "href", rssFeedURL );
        root.addContent( self );

        //  Items
        root.addContent( getItems() );

        //  aaand output
        final XMLOutputter output = new XMLOutputter();
        output.setFormat( Format.getPrettyFormat() );

        try {
            final StringWriter res = new StringWriter();
            output.output( root, res );

            return res.toString();
        } catch( final IOException e ) {
            return null;
        }
    }

    private Element getElement( final String name ) {
        return new Element( name, m_atomNameSpace );
    }

}
