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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;
import org.jdom2.Namespace;

import java.text.SimpleDateFormat;
import java.util.Calendar;


/**
 * Provides an implementation of an RSS 1.0 feed.  In addition, this class is
 * capable of adding RSS 1.0 Wiki Extensions to the Feed, as defined in
 * <A HREF="http://usemod.com/cgi-bin/mb.pl?ModWiki">UseMod:ModWiki</A>.
 */
public class RSS10Feed extends Feed {

    private static final Namespace NS_XMNLS = Namespace.getNamespace( "http://purl.org/rss/1.0/" );
    private static final Namespace NS_RDF = Namespace.getNamespace( "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#" );
    private static final Namespace NS_DC = Namespace.getNamespace( "dc", "http://purl.org/dc/elements/1.1/" );
    private static final Namespace NS_WIKI = Namespace.getNamespace( "wiki", "http://purl.org/rss/1.0/modules/wiki/" );

    /**
     * Create an RSS 1.0 feed for a given context.
     *
     * @param context The WikiContext.
     */
    public RSS10Feed( WikiContext context ) {
        super( context );
    }

    private Element getRDFItems() {
        Element items = new Element( "items", NS_XMNLS );
        Element rdfseq = new Element( "Seq", NS_RDF );

        for( Entry e : m_entries ) {
            String url = e.getURL();
            rdfseq.addContent( new Element( "li", NS_RDF ).setAttribute( "resource", url, NS_RDF ) );
        }
        items.addContent( rdfseq );

        return items;
    }

    private void addItemList( Element root ) {
        SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        WikiEngine engine = m_wikiContext.getEngine();

        for( Entry entry : m_entries ) {
            String url = entry.getURL();

            Element item = new Element( "item", NS_XMNLS );
            item.setAttribute( "about", url, NS_RDF );
            item.addContent( new Element( "title", NS_XMNLS ).addContent( entry.getTitle() ) );
            item.addContent( new Element( "link", NS_XMNLS ).addContent( url ) );

            Element content = new Element( "description", NS_XMNLS );
            // TODO: Add a size limiter here
            content.addContent( entry.getContent() );
            item.addContent( content );

            WikiPage p = entry.getPage();
            if( p.getVersion() != -1 ) {
                item.addContent( new Element( "version", NS_WIKI ).addContent( Integer.toString( p.getVersion() ) ) );
            }

            if( p.getVersion() > 1 ) {
                item.addContent( new Element( "diff", NS_WIKI )
                                         .addContent( engine.getURL( WikiContext.DIFF, p.getName(), "r1=-1" ) ) );
            }

            //
            //  Modification date.
            Calendar cal = Calendar.getInstance();
            cal.setTime(p.getLastModified());
            cal.add( Calendar.MILLISECOND,
                    - ( cal.get( Calendar.ZONE_OFFSET ) +
                             ( cal.getTimeZone().inDaylightTime( p.getLastModified() ) ? cal.get( Calendar.DST_OFFSET )
                                                                                       : 0 ) ) );

            item.addContent( new Element( "date", NS_DC ).addContent( iso8601fmt.format( cal.getTime() ) ) );

            //
            //  Author
            String author = entry.getAuthor();
            if( author == null ) {
                author = "unknown";
            }

            Element contributor = new Element( "creator", NS_DC );
            item.addContent( contributor );

            /*
            Element description = new Element("Description", NS_RDF);
            if( m_wikiContext.getEngine().pageExists(author) ) {
                description.setAttribute( "link", engine.getURL( WikiContext.VIEW, author, null, true ), NS_XMNLS );
            }

            description.addContent( new Element("value", NS_XMNLS).addContent( author) );
            contributor.addContent( description );
           */

            // Not too many aggregators seem to like this.  Therefore we're just adding the name here.
            contributor.addContent( author );

            //
            //  PageHistory
            item.addContent( new Element( "history", NS_WIKI )
                                    .addContent( engine.getURL( WikiContext.INFO, p.getName(), null ) ) );

            //
            // Add to root
            root.addContent( item );
        }
    }

    private Element getChannelElement() {
        Element channel = new Element( "channel", NS_XMNLS );
        channel.setAttribute( "about", m_feedURL, NS_RDF )
               .addContent( new Element( "link", NS_XMNLS ).addContent( m_feedURL ) );

        if( m_channelTitle != null ) {
            channel.addContent( new Element( "title", NS_XMNLS ).addContent( m_channelTitle ) );
        }

        if( m_channelDescription != null ) {
            channel.addContent( new Element( "description", NS_XMNLS ).addContent( m_channelDescription ) );
        }

        if( m_channelLanguage != null ) {
            channel.addContent( new Element( "language", NS_DC ).addContent( m_channelLanguage ) );
        }
        channel.addContent( getRDFItems() );

        return channel;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString() {
        Element root = new Element( "RDF", NS_RDF );
        root.addContent( getChannelElement() );
        root.addNamespaceDeclaration( NS_XMNLS );
        root.addNamespaceDeclaration( NS_RDF );
        root.addNamespaceDeclaration( NS_DC );
        root.addNamespaceDeclaration( NS_WIKI );
        addItemList( root );

        return XhtmlUtil.serialize( root, true );
    }

}
