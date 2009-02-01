/* 
    JSPWiki - a JSP-based WikiWiki clone.

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

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.xmlrpc.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;

/**
 *  A very dumb class that pings weblogs.com on each save.  INTERNAL USE ONLY SO FAR!
 *  Look, but don't use as-is.
 */
// FIXME: Needs to figure out when only weblogs have been saved.
// FIXME: rpc endpoint must be configurable
// FIXME: Should really be settable per-page.
// FIXME: Weblog name has been set to stone
public class PingWeblogsComFilter
    extends BasicPageFilter
{
    static Logger log = LoggerFactory.getLogger( PingWeblogsComFilter.class );

    private String m_pingURL;

    /**
     *  The property name for the URL to ping.  Value is <tt>{@value}</tt>.
     */
    public static final String PROP_PINGURL = "pingurl";

    /**
     *  {@inheritDoc}
     */
    public void initialize( WikiEngine engine, Properties props )
    {
        m_pingURL = props.getProperty( PROP_PINGURL, "http://rpc.weblogs.com/RPC2" );
    }

    /**
     *  {@inheritDoc}
     */
    public void postSave( WikiContext context, String pagecontent )
    {
        String     blogName = context.getPage().getName();
        WikiEngine engine   = context.getEngine();

        int blogentryTxt = blogName.indexOf("_blogentry_");
        if( blogentryTxt == -1 )
        {
            return; // This is not a weblog entry.
        }
        
        blogName = blogName.substring( 0, blogentryTxt );

        if( blogName.equals( engine.getFrontPage() ) )
        {
            blogName = null;
        }

        try
        {
            XmlRpcClient xmlrpc = new XmlRpcClient(m_pingURL);
            Vector<String> params = new Vector<String>();
            params.addElement( "The Butt Ugly Weblog" ); // FIXME: Must be settable
            params.addElement( engine.getURL( WikiContext.VIEW, blogName, null, true ) );

            if( log.isDebugEnabled() )
                log.debug("Pinging weblogs.com with URL: "+engine.getURL( WikiContext.VIEW, blogName, null, true ));

            xmlrpc.executeAsync("weblogUpdates.ping", params, 
                                new AsyncCallback() 
                                {
                                    public void handleError( Exception ex,
                                                             URL url,
                                                             String method )
                                    {
                                        log.error("Unable to execute weblogs.com ping to URL: "+url.toString(),ex);
                                    }

                                    public void handleResult( Object result,
                                                              URL url,
                                                              String method )
                                    {
                                        Hashtable res = (Hashtable) result;

                                        Boolean flerror = (Boolean)res.get("flerror");
                                        String  msg     = (String)res.get("message");

                                        if( flerror.booleanValue() )
                                        {
                                            log.error("Failed to ping: "+msg);
                                        }

                                        log.info("Weblogs.com has been pinged.");
                                    }
                                }
                                );
        }
        catch( MalformedURLException e )
        {
            log.error("Malformed URL",e);
        }
    }
}
