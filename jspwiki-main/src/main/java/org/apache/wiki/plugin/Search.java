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

import org.apache.log4j.Logger;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.plugin.Plugin;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.render.RenderingManager;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.util.XHTML;
import org.apache.wiki.util.XhtmlUtil;
import org.jdom2.Element;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 *  The "Search" plugin allows you to access the JSPWiki search routines and show the displays in an array on your page.
 *
 *  <p>Parameters : </p>
 *  <ul>
 *  <li><b>query</b> - String. A standard JSPWiki search query.</li>
 *  <li><b>set</b> - String. The JSPWiki context variable that will hold the results of the query. This allows you to pass your queries to other plugins on the same page as well. </li>
 *  <li><b>max</b> - Integer. How many search results are shown at maximum.</li>
 *  </ul>
 *
 *  @since
 */
public class Search implements Plugin {

    private static final Logger log = Logger.getLogger(Search.class);

    /** Parameter name for setting the query string.  Value is <tt>{@value}</tt>. */
    public static final String PARAM_QUERY = "query";

    /** Parameter name for setting the name of the set where the results are stored. Value is <tt>{@value}</tt>. */
    public static final String PARAM_SET   = "set";

    /** The default name of the result set. */
    public static final String DEFAULT_SETNAME = "_defaultSet";

    /** The parameter name for setting the how many results will be fetched. Value is <tt>{@value}</tt>. */
    public static final String PARAM_MAX   = "max";

    /**
     * {@inheritDoc}
     */
    @Override
    public String execute( final Context context, final Map<String, String> params ) throws PluginException {
        int maxItems = Integer.MAX_VALUE;
        final Collection< SearchResult > results;

        final String queryString = params.get( PARAM_QUERY );
        String set               = params.get( PARAM_SET );
        final String max         = params.get( PARAM_MAX );

        if ( set == null ) set = DEFAULT_SETNAME;
        if ( max != null ) maxItems = Integer.parseInt( max );

        if ( queryString == null ) {
            results = context.getVariable( set );
        } else {
            try {
                results = doBasicQuery( context, queryString );
                context.setVariable( set, results );
            } catch( final Exception e ) {
                return "<div class='error'>" + e.getMessage() + "</div>\n";
            }
        }

        String res = "";

        if ( results != null ) {
            res = renderResults(results,context,maxItems);
        }

        return res;
    }

    private Collection<SearchResult> doBasicQuery( final Context context, final String query ) throws ProviderException, IOException {
        log.debug( "Searching for string " + query );
        return context.getEngine().getManager( SearchManager.class ).findPages( query, context );
    }

    private String renderResults( final Collection<SearchResult> results, final Context context, final int maxItems ) {
        final Engine engine = context.getEngine();

        final Element table = XhtmlUtil.element(XHTML.table);
        //table.setAttribute(XHTML.ATTR_border,"0");
        //table.setAttribute(XHTML.ATTR_cellpadding,"4");
        table.setAttribute(XHTML.ATTR_class,"wikitable search-result");

        Element row = XhtmlUtil.element(XHTML.tr);
        table.addContent(row);

        final Element th1 = XhtmlUtil.element(XHTML.th,"Page");
        th1.setAttribute(XHTML.ATTR_width,"30%");
        th1.setAttribute(XHTML.ATTR_align,"left");
        row.addContent(th1);

        final Element th2 = XhtmlUtil.element(XHTML.th,"Score");
        th2.setAttribute(XHTML.ATTR_align,"left");
        row.addContent(th2);

        int idx = 0;
        for ( final Iterator<SearchResult> i = results.iterator(); i.hasNext() && idx++ <= maxItems; ) {
            final SearchResult sr = i.next();
            row = XhtmlUtil.element(XHTML.tr);

            final Element name = XhtmlUtil.element(XHTML.td);
            name.setAttribute(XHTML.ATTR_width,"30%");

            name.addContent( XhtmlUtil.link(context.getURL( ContextEnum.PAGE_VIEW.getRequestContext(), sr.getPage().getName() ),
                             engine.getManager( RenderingManager.class ).beautifyTitle(sr.getPage().getName() ) ) );

            row.addContent(name);

            row.addContent(XhtmlUtil.element(XHTML.td,""+sr.getScore()));

            table.addContent(row);
        }

        if ( results.isEmpty() ) {
            row = XhtmlUtil.element(XHTML.tr);

            final Element td = XhtmlUtil.element(XHTML.td);
            td.setAttribute(XHTML.ATTR_colspan,"2");
            final Element b = XhtmlUtil.element(XHTML.b,"No results");
            td.addContent(b);

            row.addContent(td);

            table.addContent(row);
        }

        return XhtmlUtil.serialize(table);
    }
}
