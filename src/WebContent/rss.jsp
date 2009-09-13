<%-- 
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
--%>
<?xml version="1.0" encoding="UTF-8"?>
<%@ page import="java.util.*,org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.log.Logger" %>
<%@ page import="org.apache.wiki.log.LoggerFactory" %>
<%@ page import="java.text.*" %>
<%@ page import="org.apache.wiki.rss.*" %>
<%@ page import="org.apache.wiki.util.*" %>
<%@ page import="net.sf.ehcache.*" %>
<%@ page import="org.apache.wiki.api.WikiPage" %>

<%!
    Logger log = LoggerFactory.getLogger("JSPWiki");
    CacheManager m_cacheManager = CacheManager.getInstance();
%>

<%
    Cache cache = m_cacheManager.getCache("jspwiki.rssCache");
    if( cache == null )
    {
        cache = new Cache( "jspwiki.rssCache", 500, false, false, 24*3600, 24*3600 );
        m_cacheManager.addCache(cache);
    }

    WikiEngine wiki = WikiEngine.getInstance( getServletConfig() );
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, "rss" );
    WikiPage    wikipage    = wikiContext.getPage();

    // Redirect if baseURL not set or RSS generation not on
    if( wiki.getBaseURL().length() == 0 )
    {
        response.sendError( 500, "The jspwiki.baseURL property has not been defined for this wiki - cannot generate RSS" );
        return;
    }
    
    if( wiki.getRSSGenerator() == null )
    {
        response.sendError( 404, "RSS feeds are disabled at administrator request" );
        return;
    }

    if( wikipage == null || !wiki.pageExists(wikipage.getName()) )
    {
        response.sendError( 404, "No such page "+wikipage.getName() );
        return;
    }

    WatchDog w = wiki.getCurrentWatchDog();
    w.enterState("Generating RSS",60);
    
    // Set the mode and type for the feed
    String      mode        = request.getParameter("mode");
    String      type        = request.getParameter("type");
    
    if( mode == null || !(mode.equals(RSSGenerator.MODE_BLOG) || mode.equals(RSSGenerator.MODE_WIKI)) ) 
    	   mode = RSSGenerator.MODE_BLOG;
    if( type == null || !(type.equals(RSSGenerator.RSS10) || type.equals(RSSGenerator.RSS20) || type.equals(RSSGenerator.ATOM)) ) 
    	   type = RSSGenerator.RSS20;
    
    // Force the TranslatorReader to output absolute URLs
    // regardless of the current settings.
    wikiContext.setVariable( WikiEngine.PROP_REFSTYLE, "absolute" );

    // Set the content type and include the response content
    response.setContentType( RSSGenerator.getContentType(type)+"; charset=UTF-8");

    StringBuffer result = new StringBuffer();
    SimpleDateFormat iso8601fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    Properties properties = wiki.getWikiProperties();
    String channelDescription = WikiEngine.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_DESCRIPTION );
    String channelLanguage    = WikiEngine.getRequiredProperty( properties, RSSGenerator.PROP_CHANNEL_LANGUAGE );

    //
    //  Now, list items.
    //
    List<WikiPage> changed;
    
    if( mode.equals("blog") )
    {
        org.apache.wiki.plugin.WeblogPlugin plug = new org.apache.wiki.plugin.WeblogPlugin();
        changed = plug.findBlogEntries(wiki.getContentManager(), 
                                       wikipage.getName(),
                                       new Date(0L),
                                       new Date());
    }
    else
    {
        changed = wiki.getVersionHistory( wikipage.getName() );
    }
    
    //
    //  Check if nothing has changed, so we can just return a 304
    //
    boolean hasChanged = false;
    Date    latest     = new Date(0);

    for( Iterator<WikiPage> i = changed.iterator(); i.hasNext(); )
    {
        WikiPage p = i.next();

        if( !HttpUtil.checkFor304( request, p ) ) hasChanged = true;
        if( p.getLastModified().after( latest ) ) latest = p.getLastModified();
    }

    if( !hasChanged && changed.size() > 0 )
    {
        response.sendError( HttpServletResponse.SC_NOT_MODIFIED );
        w.exitState();
        return;
    }

    response.addDateHeader("Last-Modified",latest.getTime());
    response.addHeader("ETag", HttpUtil.createETag(wikipage) );
    
    //
    //  Try to get the RSS XML from the cache.  We build the hashkey
    //  based on the LastModified-date, so whenever it changes, so does
    //  the hashkey so we don't have to make any special modifications.
    //
    //  TODO: Figure out if it would be a good idea to use a disk-based
    //        cache here.
    //
    String hashKey = wikipage.getName()+";"+mode+";"+type+";"+latest.getTime();
    
    String rss = "";
    
    Element e = cache.get(hashKey);
    
    if( e != null )
    {
        rss = (String)e.getValue();
    }
    else
    { 
        try
        {
            rss = wiki.getRSSGenerator().generateFeed( wikiContext, changed, mode, type );
            cache.put( new Element(hashKey,rss) );
        }
        catch( Exception e1 )
        {
        }
    }
    
    out.println(rss);
    
    w.exitState(); 
    %>
