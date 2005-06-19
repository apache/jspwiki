/*
JSPWiki - a JSP-based WikiWiki clone.

Copyright (C) 2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation; either version 2.1 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package com.ecyrd.jspwiki.search;

import java.util.Collection;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.WikiProvider;

/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @author Arent-Jan Banck for Informatica
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
     */
    public Collection findPages(String query);
}
