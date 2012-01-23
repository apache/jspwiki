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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.ajax.JavaScriptResolution;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.search.SearchResult;
import org.apache.wiki.ui.stripes.AjaxEvent;
import org.apache.wiki.ui.stripes.AjaxResolution;
import org.apache.wiki.ui.stripes.TemplateResolution;
import org.apache.wiki.ui.stripes.WikiRequestContext;

/**
 * Searches the WikiPage collection for a given wiki.
 */
@UrlBinding( "/Search.jsp" )
public class SearchActionBean extends AbstractActionBean
{
    private static Logger log = LoggerFactory.getLogger( "JSPWikiSearch" );

    public static final Collection<SearchResult> NO_RESULTS = Collections.emptyList();

    private Collection<SearchResult> m_results = NO_RESULTS;

    private String m_query = null;

    private int m_maxItems = 20;

    private int m_start = 0;

    private boolean m_details = false;

    /**
     * Enumeration of the search scope options.
     */
    public enum SearchScope
    {
        /** All page contents and attributes. */
        ALL,
        /** Author names only. */
        AUTHORS,
        /** Page names only. */
        PAGE_NAMES,
        /** Contents only. */
        CONTENTS,
        /** Attachment contents only. */
        ATTACHMENTS
    }

    public boolean getDetails()
    {
        return m_details;
    }

    /**
     * Sets the search results so that details for each result are displayed.
     * 
     * @param details whether details should be displayed
     */
    public void setDetails( boolean details )
    {
        m_details = details;
    }

    public int getMaxItems()
    {
        return m_maxItems;
    }

    public void setMaxItems( int maxItems )
    {
        m_maxItems = maxItems;
    }

    public int getStart()
    {
        return m_start;
    }

    public void setStart( int start )
    {
        m_start = start;
    }

    /**
     * Returns the query string for the search.
     * 
     * @return the query string
     */
    public String getQuery()
    {
        return m_query;
    }

    /**
     * Returns the results of the search.
     * 
     * @return the results
     */
    public Collection<SearchResult> getResults()
    {
        return m_results;
    }

    /**
     * Performs a search and returns the results as a list. For a given WikiPage
     * to be included in the results, the user must have permission to view it.
     * If the underlying providers encounter an abnormal IOException or other
     * error, it will be added to the ActionBeanContext's validation messages
     * collection.
     * 
     * @param query the query
     * @return the results
     */
    private List<SearchResult> doSearch( String query )
    {
        log.info( "Searching with query '" + query + "'." );
        WikiEngine engine = getContext().getEngine();
        AuthorizationManager mgr = engine.getAuthorizationManager();

        //
        // Filter down to only those that we actually have a permission to view
        //
        List<SearchResult> filteredResults = new ArrayList<SearchResult>();
        try
        {
            List<SearchResult> results = engine.findPages( query );
            for( SearchResult result : results )
            {
                WikiPage page = result.getPage();
                PagePermission permission = new PagePermission( page, PagePermission.VIEW_ACTION );
                try
                {
                    if( mgr.checkPermission( getContext().getWikiSession(), permission ) )
                    {
                        filteredResults.add( result );
                    }
                }
                catch( Exception e )
                {
                    log.error( "Searching for page " + page, e );
                }
            }
        }
        catch( Exception e )
        {
            log.debug( "Could not search using query '" + query + "'.", e );
            Message message = new SimpleMessage( e.getMessage() );
            getContext().getMessages().add( message );
            e.printStackTrace();
        }
        return filteredResults;
    }

    /**
     * Sets the query string for the search.
     * 
     * @param query the query string
     */
    public void setQuery( String query )
    {
        m_query = query;
    }

    /**
     * Searches the wiki using the query string set for this ActionBean. Search
     * results are made available to callers via the {@link #getResults()}
     * method (and EL expression <code>$wikiActionBean.results</code>).
     * 
     * @return always returns a {@link ForwardResolution} to the template JSP
     *         <code>/Search.jsp</code>.
     */
    @DefaultHandler
    @HandlesEvent( "search" )
    @WikiRequestContext( "find" )
    public Resolution search()
    {
        m_results = m_query == null ? NO_RESULTS : doSearch( m_query );
        return new TemplateResolution( "Search.jsp" );
    }

    /**
     * Using AJAX, searches a specified wiki space using the query string set
     * for this ActionBean. Results are streamed back to the client as an array
     * of JSON-encoded SearchResult objects.
     * 
     * @return always returns an {@link AjaxResolution} containing the
     *         results; this may be a zero-length array
     */
    @AjaxEvent
    @HandlesEvent( "ajaxSearch" )
    public Resolution ajaxSearch()
    {
        m_results = m_query == null ? NO_RESULTS : doSearch( m_query );
        return new JavaScriptResolution( m_results );
    }

    /**
     * AJAX event method that provides quick-search used in {@code SearchBox.jsp}.
     * 
     * @return an {@link AjaxResolution} containing HTML to be inserted into an
     *         element.
     */
    @AjaxEvent
    @HandlesEvent( "quickSearch" )
    public Resolution quickSearch()
    {
        m_results = m_query == null ? NO_RESULTS : doSearch( m_query );
 
        //FIXME: casting m_results (bean) to 'Object' type seems NOK.
        //   So, for now, still using manual conversion to html
        //   iso straight translation to json object.
        //
        //return new AjaxResolution( getContext(), m_results );

        String html = null;
        StringBuilder b = new StringBuilder();
        if( m_results.size() > 0 )
        {
            b.append( "<ul>" );
            for( SearchResult result : m_results )
            {
                String url = getContext().getViewURL( result.getPage().getName() );
                b.append( "<li>" );
                b.append( "<a href=\"" + url + "\">" + result.getPage().getName() + "</a>" );
                b.append( " <span class=\"small\">(" + result.getScore() + ")</span>" );
                b.append( "</li>" );
            }
            b.append( "</ul>" );
            html = b.toString();
        }
        return new AjaxResolution( getContext(), html );
    }
}
