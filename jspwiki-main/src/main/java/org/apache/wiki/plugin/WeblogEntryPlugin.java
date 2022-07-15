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
package org.apache.wiki.plugin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.pages.PageLock;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.util.TextUtil;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Builds a simple weblog.
 * <p/>
 * <p>Parameters : </p>
 * <ul>
 * <li><b>entrytext</b> - text of the link </li>
 * <li><b>page</b> - if set, the entry is added to the named blog page. The default is the current page. </li>
 * </ul>
 *
 * @since 1.9.21
 */
public class WeblogEntryPlugin implements Plugin {

    private static final Logger LOG = LogManager.getLogger(WeblogEntryPlugin.class);
    private static final int MAX_BLOG_ENTRIES = 10_000; // Just a precaution.

    /**
     * Parameter name for setting the entrytext  Value is <tt>{@value}</tt>.
     */
    public static final String PARAM_ENTRYTEXT = "entrytext";

    /*
     * Optional parameter: page that actually contains the blog. This lets us provide a "new entry" link for a blog page
     * somewhere else than on the page itself.
     */
    // "page" for uniform naming with WeblogPlugin...
    
    /**
     * Parameter name for setting the page Value is <tt>{@value}</tt>.
     */
    public static final String PARAM_BLOGNAME = "page";

    /**
     * Returns a new page name for entries.  It goes through the list of all blog pages, and finds out the next in line.
     *
     * @param engine   A Engine
     * @param blogName The page (or blog) name.
     * @return A new name.
     * @throws ProviderException If something goes wrong.
     */
    public String getNewEntryPage( final Engine engine, final String blogName ) throws ProviderException {
        final SimpleDateFormat fmt = new SimpleDateFormat(WeblogPlugin.DEFAULT_DATEFORMAT);
        final String today = fmt.format(new Date());
        final int entryNum = findFreeEntry( engine, blogName, today );

        return WeblogPlugin.makeEntryPage( blogName, today,"" + entryNum );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute( final Context context, final Map< String, String > params ) throws PluginException {
        final ResourceBundle rb = Preferences.getBundle(context, Plugin.CORE_PLUGINS_RESOURCEBUNDLE);
        final Engine engine = context.getEngine();

        String weblogName = params.get(PARAM_BLOGNAME);
        if (weblogName == null) {
            weblogName = context.getPage().getName();
        }

        String entryText = TextUtil.replaceEntities( params.get( PARAM_ENTRYTEXT ) );
        if (entryText == null) {
            entryText = rb.getString("weblogentryplugin.newentry");
        }

        final String url = context.getURL( ContextEnum.PAGE_NONE.getRequestContext(), "NewBlogEntry.jsp", "page=" + engine.encodeName( weblogName ) );
        return "<a href=\"" + url + "\">" + entryText + "</a>";
    }

    private int findFreeEntry( final Engine engine, final String baseName, final String date ) throws ProviderException {
        final Collection< Page > everyone = engine.getManager( PageManager.class ).getAllPages();
        final String startString = WeblogPlugin.makeEntryPage(baseName, date, "");
        int max = 0;

        for( final Page p : everyone ) {
            if( p.getName().startsWith( startString ) ) {
                try {
                    final String probableId = p.getName().substring( startString.length() );
                    final int id = Integer.parseInt( probableId );
                    if( id > max ) {
                        max = id;
                    }
                } catch( final NumberFormatException e ) {
                    LOG.debug( "Was not a log entry: " + p.getName() );
                }
            }
        }

        //  Find the first page that has no page lock.
        int idx = max + 1;
        while( idx < MAX_BLOG_ENTRIES ) {
            final Page page = Wiki.contents().page( engine, WeblogPlugin.makeEntryPage( baseName, date, Integer.toString( idx ) ) );
            final PageLock lock = engine.getManager( PageManager.class ).getCurrentLock(page);
            if (lock == null) {
                break;
            }

            idx++;
        }

        return idx;
    }

}
