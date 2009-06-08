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
package org.apache.wiki.tags;

import java.util.Collection;

import org.apache.wiki.action.SearchActionBean;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.search.SearchResult;
import org.apache.wiki.ui.stripes.WikiInterceptor;

/**
 * Iterator tag for the current search results, as identified by a
 * request-scoped attribute set elsewhere; for example, by an ActionBean.
 */
public class SearchResultIteratorTag extends IteratorTag<SearchResult>
{
    private static final long serialVersionUID = 1L;

    /**
     * Returns the list of SearchResults to iterate over.
     */
    @Override
    protected Collection<SearchResult> initItems()
    {
        WikiActionBean actionBean = WikiInterceptor.findActionBean( pageContext );
        if ( actionBean != null && actionBean instanceof SearchActionBean )
        {
            return ((SearchActionBean)actionBean).getResults();
        }
        return SearchActionBean.NO_RESULTS;
    }

    /**
     * When the next iterated item is encountered, this method sets the current
     * WikiContext's page property to the value of
     * {@link SearchResult#getPage()}.
     */
    @Override
    protected void nextItem( SearchResult item )
    {
        m_wikiContext.setPage( item.getPage() );
    }
}
