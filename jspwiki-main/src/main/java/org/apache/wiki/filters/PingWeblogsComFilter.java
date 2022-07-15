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
import org.apache.xmlrpc.AsyncCallback;
import org.apache.xmlrpc.XmlRpcClient;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Vector;

/**
 *  A very dumb class that pings weblogs.com on each save.  INTERNAL USE ONLY SO FAR! Look, but don't use as-is.
 */
// FIXME: Needs to figure out when only weblogs have been saved.
// FIXME: rpc endpoint must be configurable
// FIXME: Should really be settable per-page.
// FIXME: Weblog name has been set to stone
public class PingWeblogsComFilter extends BasePageFilter {

    private static final Logger LOG = LogManager.getLogger( PingWeblogsComFilter.class );

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

        try {
            final XmlRpcClient xmlrpc = new XmlRpcClient(m_pingURL);
            final Vector< String > params = new Vector<>();
            params.addElement( "The Butt Ugly Weblog" ); // FIXME: Must be settable
            params.addElement( engine.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), blogName, null ) );

            LOG.debug( "Pinging weblogs.com with URL: {}", engine.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), blogName, null ) );

            xmlrpc.executeAsync("weblogUpdates.ping", params, 
                                new AsyncCallback() {
                                    @Override
                                    public void handleError( final Exception ex, final URL url, final String method ) {
                                        LOG.error( "Unable to execute weblogs.com ping to URL: " + url.toString(), ex );
                                    }

                                    @Override
                                    public void handleResult( final Object result, final URL url, final String method ) {
                                        @SuppressWarnings("unchecked")
                                        final Hashtable< String, Object > res = (Hashtable < String, Object > ) result;

                                        final Boolean flerror = ( Boolean )res.get( "flerror" );
                                        final String  msg     = ( String )res.get( "message" );

                                        if( flerror ) {
                                            LOG.error( "Failed to ping: " + msg );
                                        }

                                        LOG.info( "Weblogs.com has been pinged." );
                                    }
                                }
                                );
        } catch( final MalformedURLException e ) {
            LOG.error("Malformed URL",e);
        }
    }

}
