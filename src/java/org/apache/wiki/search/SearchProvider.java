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
package org.apache.wiki.search;

import java.io.IOException;
import java.util.Collection;

import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.providers.ProviderException;


/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @author Arent-Jan Banck
 *  @since 2.2.21.
 */
public interface SearchProvider extends WikiProvider
{
    /**
     * Delete a page from the search index
     * @param page Page to remove from search index
     */
    public void pageRemoved(WikiPage page);

    /**
     *  Adds a WikiPage for indexing queue.  This is called a queue, since
     *  this method is expected to return pretty quickly, and indexing to
     *  be done in a separate thread.
     *
     *  @param page The WikiPage to be indexed.
     */
    public void reindexPage(WikiPage page);

    /**
     * Search for pages matching a search query
     * @param query query to search for
     * @return collection of pages that match query
     * @throws ProviderException if the search provider failed.
     * @throws IOException if for some reason the query could not be executed.
     */
    public Collection<SearchResult> findPages(String query) throws ProviderException, IOException;
}
