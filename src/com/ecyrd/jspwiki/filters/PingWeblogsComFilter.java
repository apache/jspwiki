package com.ecyrd.jspwiki.filters;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import org.apache.xmlrpc.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.Vector;
import org.apache.log4j.Category;

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
    static Category log = Category.getInstance( PingWeblogsComFilter.class );

    public void postSave( WikiContext context, String pagecontent )
    {
        String     blogName = context.getPage().getName();
        WikiEngine engine   = context.getEngine();
        
        if( blogName.equals( engine.getFrontPage() ) )
        {
            blogName = null;
        }

        try
        {
            XmlRpcClient xmlrpc = new XmlRpcClient("http://rpc.weblogs.com/RPC2");
            Vector params = new Vector();
            params.addElement( "The Butt Ugly Weblog" );
            params.addElement( engine.getViewURL(blogName) );

            log.debug("Pinging weblogs.com with URL: "+engine.getViewURL(blogName));

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
