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
package org.apache.wiki.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.filters.BasePageFilter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Properties;

/**
 *  A very dumb class that pings weblogs.com on each save.  INTERNAL USE ONLY SO FAR! Look, but don't use as-is.
 */
// FIXME: Needs to figure out when only weblogs have been saved.
// FIXME: rpc endpoint must be configurable
// FIXME: Should really be settable per-page.
// FIXME: Weblog name has been set to stone
public class PingWeblogsComFilter extends BasePageFilter {

    private static final Logger LOG = LogManager.getLogger( PingWeblogsComFilter.class );

    private static final Duration TIMEOUT = Duration.ofSeconds( 30 );

    private final HttpClient m_client = HttpClient.newBuilder().connectTimeout( TIMEOUT ).build();

    private String m_pingURL;

    /**
     *  The property name for the URL to ping.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_PINGURL = "pingurl";

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) {
        m_pingURL = props.getProperty( PROP_PINGURL, "http://rpc.weblogs.com/RPC2" );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void postSave( final Context context, final String pagecontent ) {
        String blogName = context.getPage().getName();
        final Engine engine   = context.getEngine();

        final int blogentryTxt = blogName.indexOf("_blogentry_");
        if( blogentryTxt == -1 ) {
            return; // This is not a weblog entry.
        }
        
        blogName = blogName.substring( 0, blogentryTxt );

        if( blogName.equals( engine.getFrontPage() ) ) {
            blogName = null;
        }

        final String url = engine.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), blogName, null );
        LOG.debug( "Pinging weblogs.com with URL: {}", url );

        final HttpRequest request;
        try {
            request = HttpRequest.newBuilder( URI.create( m_pingURL ) )
                                 .timeout( TIMEOUT )
                                 .header( "Content-Type", "text/xml; charset=UTF-8" )
                                 .POST( HttpRequest.BodyPublishers.ofString( pingRequest( "The Butt Ugly Weblog", url ), StandardCharsets.UTF_8 ) ) // FIXME: Must be settable
                                 .build();
        } catch( final IllegalArgumentException e ) {
            LOG.error( "Malformed URL", e );
            return;
        }

        m_client.sendAsync( request, HttpResponse.BodyHandlers.ofString( StandardCharsets.UTF_8 ) )
                .whenComplete( ( response, error ) -> {
                    if( error != null ) {
                        LOG.error( "Unable to execute weblogs.com ping to URL: " + m_pingURL, error );
                    } else if( response.statusCode() != 200 ) {
                        LOG.error( "Failed to ping: HTTP status {}", response.statusCode() );
                    } else {
                        handleResponse( response.body() );
                    }
                } );
    }

    /**
     *  Builds the XML-RPC call of <tt>weblogUpdates.ping</tt>.
     *
     *  @param name the name of the weblog
     *  @param url the URL of the weblog
     *  @return the body of the request
     */
    static String pingRequest( final String name, final String url ) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?><methodCall><methodName>weblogUpdates.ping</methodName><params>"
               + "<param><value><string>" + escape( name ) + "</string></value></param>"
               + "<param><value><string>" + escape( url ) + "</string></value></param>"
               + "</params></methodCall>";
    }

    private static String escape( final String text ) {
        return text.replace( "&", "&amp;" ).replace( "<", "&lt;" ).replace( ">", "&gt;" );
    }

    private static void handleResponse( final String body ) {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );
            factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
            final Document doc = factory.newDocumentBuilder().parse( new ByteArrayInputStream( body.getBytes( StandardCharsets.UTF_8 ) ) );
            final String flerror = memberValue( doc, "flerror" );
            if( "1".equals( flerror ) || "true".equalsIgnoreCase( flerror ) ) {
                LOG.error( "Failed to ping: " + memberValue( doc, "message" ) );
                return;
            }
            LOG.info( "Weblogs.com has been pinged." );
        } catch( final ParserConfigurationException | java.io.IOException | org.xml.sax.SAXException e ) {
            LOG.error( "Unable to read the answer of weblogs.com", e );
        }
    }

    private static String memberValue( final Document doc, final String member ) {
        final NodeList members = doc.getElementsByTagName( "member" );
        for( int i = 0; i < members.getLength(); i++ ) {
            final Element m = ( Element )members.item( i );
            final NodeList names = m.getElementsByTagName( "name" );
            final NodeList values = m.getElementsByTagName( "value" );
            if( names.getLength() > 0 && values.getLength() > 0 && member.equals( names.item( 0 ).getTextContent().trim() ) ) {
                final Node value = values.item( 0 );
                return value.getTextContent().trim();
            }
        }
        return null;
    }

}
