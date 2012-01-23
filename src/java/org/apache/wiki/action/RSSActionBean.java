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

package org.apache.wiki.action;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.rss.RSSGenerator;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.util.HttpUtil;

/**
 * Generates RSS feeds for WikiPages.
 */
@UrlBinding( "/rss.jsp" )
public class RSSActionBean extends AbstractPageActionBean
{
    private static final Logger log = LoggerFactory.getLogger( RSSActionBean.class );

    /**
     * {@inheritDoc} The {@code page} attribute is required.
     */
    @Validate( required = true, on = "rss" )
    public void setPage( WikiPage page )
    {
        super.setPage( page );
    }

    /**
     * Generates a {@link StreamingResolution} containing the RSS feed for the
     * current page. If the feed is already cached and hasn't expired, it is
     * returned. Otherwise, a new feed is generated.
     * 
     * @return the resolution
     * @throws IOException if anything goes wrong
     */
    @DefaultHandler
    @HandlesEvent( "rss" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.name}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "rss" )
    public Resolution rss() throws IOException
    {
        // Get the RSS mode and type
        final WikiActionBeanContext context = getContext();
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();
        final WikiEngine wiki = context.getEngine();
        final WikiPage page = context.getPage();
        final String mode = getRssMode( request );
        final String type = getRssType( request );
        final Cache cache = getRssCache();

        // Redirect if baseURL not set or RSS generation not on
        if( wiki.getBaseURL().length() == 0 )
        {
            response.sendError( 500, "The jspwiki.baseURL property has not been defined for this wiki - cannot generate RSS" );
            return null;
        }

        if( wiki.getRSSGenerator() == null )
        {
            response.sendError( 404, "RSS feeds are disabled." );
            return null;
        }

        if( page == null || !wiki.pageExists( page.getName() ) )
        {
            response.sendError( 404, "No such page " + page.getName() );
            return null;
        }

        // All good, so generate the RSS now.
        Resolution r = new StreamingResolution( RSSGenerator.getContentType( type ) + "; charset=UTF-8" ) {
            @Override
            protected void stream( HttpServletResponse response ) throws Exception
            {
                // Force the TranslatorReader to output absolute URLs
                // regardless of the current settings.
                context.setVariable( WikiEngine.PROP_REFSTYLE, "absolute" );

                //
                // Now, list items.
                //
                List<WikiPage> changed;

                if( mode.equals( "blog" ) )
                {
                    org.apache.wiki.plugin.WeblogPlugin plug = new org.apache.wiki.plugin.WeblogPlugin();
                    changed = plug.findBlogEntries( wiki.getContentManager(), page.getName(), new Date( 0L ), new Date() );
                }
                else
                {
                    changed = wiki.getVersionHistory( page.getName() );
                }

                //
                // Check if nothing has changed, so we can just return a 304
                //
                boolean hasChanged = false;
                Date latest = new Date( 0 );

                for( Iterator<WikiPage> i = changed.iterator(); i.hasNext(); )
                {
                    WikiPage p = i.next();

                    if( !HttpUtil.checkFor304( request, p ) )
                        hasChanged = true;
                    if( p.getLastModified().after( latest ) )
                        latest = p.getLastModified();
                }

                if( !hasChanged && changed.size() > 0 )
                {
                    response.sendError( HttpServletResponse.SC_NOT_MODIFIED );
                    log.info( "Requested RSS feed for page " + page.getPath().toString() + ", but nothing changed." );
                    return;
                }

                response.addDateHeader( "Last-Modified", latest.getTime() );
                response.addHeader( "ETag", HttpUtil.createETag( page ) );

                //
                // Try to get the RSS XML from the cache. We build the hashkey
                // based on the LastModified-date, so whenever it changes, so
                // does
                // the hashkey so we don't have to make any special
                // modifications.
                //
                // TODO: Figure out if it would be a good idea to use a
                // disk-based
                // cache here.
                //
                String hashKey = page.getName() + ";" + mode + ";" + type + ";" + latest.getTime();

                String rss = "";

                Element e = cache.get( hashKey );

                if( e != null )
                {
                    log.info( "Returning cached RSS feed for page " + page.getPath().toString() + "." );
                    rss = (String) e.getValue();
                }
                else
                {
                    try
                    {
                        log.info( "Generating RSS feed for page " + page.getPath().toString() + "." );
                        rss = wiki.getRSSGenerator().generateFeed( context, changed, mode, type );
                        cache.put( new Element( hashKey, rss ) );
                    }
                    catch( Exception e1 )
                    {
                    }
                }

                response.getWriter().println( rss );
            }
        };
        return r;
    }

    /**
     * Returns the RSS cache. If one does not exist, it will be initialized.
     * 
     * @return the cache
     */
    private Cache getRssCache()
    {
        CacheManager cacheManager = CacheManager.getInstance();
        Cache cache = cacheManager.getCache( "jspwiki.rssCache" );
        if( cache == null )
        {
            cache = new Cache( "jspwiki.rssCache", 500, false, false, 24 * 3600, 24 * 3600 );
            cacheManager.addCache( cache );
        }
        return cache;
    }

    /**
     * Returns the RSS mode, based on request parameters. The default is
     * {@link RSSGenerator#MODE_BLOG}.
     * 
     * @param request the HTTP request
     * @return the mode
     */
    private String getRssMode( HttpServletRequest request )
    {
        String mode = request.getParameter( "mode" );
        if( mode == null || !(mode.equals( RSSGenerator.MODE_BLOG ) || mode.equals( RSSGenerator.MODE_WIKI )) )
        {
            mode = RSSGenerator.MODE_BLOG;
        }
        return mode;
    }

    /**
     * Returns the RSS type, based on request parameters. The default is
     * {@link RSSGenerator#RSS20}.
     * 
     * @param request the HTTP request
     * @return the type
     */
    private String getRssType( HttpServletRequest request )
    {
        String type = request.getParameter( "type" );
        if( type == null
            || !(type.equals( RSSGenerator.RSS10 ) || type.equals( RSSGenerator.RSS20 ) || type.equals( RSSGenerator.ATOM )) )
        {
            type = RSSGenerator.RSS20;
        }
        return type;
    }
}
