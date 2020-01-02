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
package org.apache.wiki.references;

import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.filters.PageFilter;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.modules.InternalModule;

import java.util.Collection;
import java.util.Set;

/**
 *  Keeps track of wikipage references:
 *  <ul>
 *  <li>What pages a given page refers to</li>
 *  <li>What pages refer to a given page</li>
 *  </ul>
 *
 *  When a page is added or edited, its references are parsed, a Collection is received, and we crudely replace anything previous with
 *  this new Collection. We then check each referenced page name and make sure they know they are referred to by the new page.
 *  <p>
 *  Based on this information, we can perform non-optimal searches for e.g. unreferenced pages, top ten lists, etc.
 *  <p>
 *  The owning class must take responsibility of filling in any pre-existing information, probably by loading each and every WikiPage
 *  and calling this class to update the references when created.
 */
public interface ReferenceManager extends PageFilter, InternalModule, WikiEventListener {

    /**
     *  Initializes the entire reference manager with the initial set of pages from the collection.
     *
     *  @param pages A collection of all pages you want to be included in the reference count.
     *  @since 2.2
     *  @throws ProviderException If reading of pages fails.
     */
    void initialize( final Collection<WikiPage> pages ) throws ProviderException;

    /**
     *  Reads a WikiPageful of data from a String and returns all links internal to this Wiki in a Collection.
     *
     *  @param page The WikiPage to scan
     *  @param pagedata The page contents
     *  @return a Collection of Strings
     */
    Collection< String > scanWikiLinks( final WikiPage page, final String pagedata );

    /**
     * Updates the m_referedTo and m_referredBy hashmaps when a page has been deleted.
     * <P>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set has to be removed to keep the map clean.
     * Within the m_referredBy map the name is stored as a value. Since a key can have more than one value we have to
     * delete just the key-value-pair referring page:deleted page.
     *
     *  @param page Name of the page to remove from the maps.
     */
    void pageRemoved( final WikiPage page );

    /**
     *  Updates all references for the given page.
     *
     *  @param page wiki page for which references should be updated
     */
    void updateReferences( final WikiPage page );

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo entry for this page already exists, it is removed
     *  and a new one is built from scratch. Also calls updateReferredBy() for each referenced page.
     *  <P>
     *  This is the method to call when a new page has been created and we want to a) set up its references and b) notify the
     *  referred pages of the references. Use this method during run-time.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    void updateReferences( final String page, final Collection<String> references );

    /**
     * Clears the references to a certain page so it's no longer in the map.
     *
     * @param pagename  Name of the page to clear references for.
     */
    void clearPageEntries( String pagename );


    /**
     *  Finds all unreferenced pages. This requires a linear scan through m_referredBy to locate keys with null or empty values.
     *
     *  @return The Collection of Strings
     */
    Collection< String > findUnreferenced();

    /**
     * Finds all references to non-existant pages. This requires a linear scan through m_refersTo values; each value
     * must have a corresponding key entry in the reference Maps, otherwise such a page has never been created.
     * <P>
     * Returns a Collection containing Strings of unreferenced page names. Each non-existant page name is shown only
     * once - we don't return information on who referred to it.
     *
     * @return A Collection of Strings
     */
    Collection< String > findUncreated();

    /**
     * Find all pages that refer to this page. Returns null if the page does not exist or is not referenced at all,
     * otherwise returns a collection containing page names (String) that refer to this one.
     * <p>
     * @param pagename The page to find referrers for.
     * @return A Set of Strings.  May return null, if the page does not exist, or if it has no references.
     */
    Set< String > findReferrers( String pagename );

    /**
     *  Returns all pages that refer to this page.  Note that this method returns an unmodifiable Map, which may be abruptly changed.
     *  So any access to any iterator may result in a ConcurrentModificationException.
     *  <p>
     *  The advantages of using this method over findReferrers() is that it is very fast, as it does not create a new object.
     *  The disadvantages are that it does not do any mapping between plural names, and you may end up getting a
     *  ConcurrentModificationException.
     *
     * @param pageName Page name to query.
     * @return A Set of Strings containing the names of all the pages that refer to this page.  May return null, if the page does
     *         not exist or has not been indexed yet.
     * @since 2.2.33
     */
    Set< String > findReferredBy( String pageName );

    /**
     *  Returns all pages that this page refers to.  You can use this as a quick way of getting the links from a page, but note
     *  that it does not link any InterWiki, image, or external links.  It does contain attachments, though.
     *  <p>
     *  The Collection returned is unmutable, so you cannot change it.  It does reflect the current status and thus is a live
     *  object.  So, if you are using any kind of an iterator on it, be prepared for ConcurrentModificationExceptions.
     *  <p>
     *  The returned value is a Collection, because a page may refer to another page multiple times.
     *
     * @param pageName Page name to query
     * @return A Collection of Strings containing the names of the pages that this page refers to. May return null, if the page
     *         does not exist or has not been indexed yet.
     * @since 2.2.33
     */
    Collection< String > findRefersTo( String pageName );

    /**
     *  Returns a list of all pages that the ReferenceManager knows about. This should be roughly equivalent to
     *  PageManager.getAllPages(), but without the potential disk access overhead.  Note that this method is not guaranteed
     *  to return a Set of really all pages (especially during startup), but it is very fast.
     *
     *  @return A Set of all defined page names that ReferenceManager knows about.
     *  @since 2.3.24
     */
    Set< String > findCreated();

}
