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
package org.apache.wiki.pages;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.providers.WikiPageProvider;

import java.util.Collection;
import java.util.List;
import java.util.Set;


public interface PageManager extends WikiEventListener {

    /** The property value for setting the current page provider.  Value is {@value}. */
    String PROP_PAGEPROVIDER = "jspwiki.pageProvider";
    /** The property value for setting the cache on/off.  Value is {@value}. */
    String PROP_USECACHE = "jspwiki.usePageCache";
    /** The property value for setting the amount of time before the page locks expire. Value is {@value}. */
    String PROP_LOCKEXPIRY = "jspwiki.lockExpiryTime";

    /**
     * Returns the page provider currently in use.
     *
     * @return A WikiPageProvider instance.
     */
    WikiPageProvider getProvider();

    /**
     * Returns all pages in some random order.  If you need just the page names,
     * please see {@link org.apache.wiki.references.ReferenceManager#findCreated() ReferenceManager#findCreated()}, which is probably a lot
     * faster.  This method may cause repository access.
     *
     * @return A Collection of WikiPage objects.
     * @throws ProviderException If the backend has problems.
     */
    Collection< WikiPage > getAllPages() throws ProviderException;

    /**
     * Fetches the page text from the repository.  This method also does some sanity checks, like checking for the pageName validity, etc.
     * Also, if the page repository has been modified externally, it is smart enough to handle such occurrences.
     *
     * @param pageName The name of the page to fetch.
     * @param version  The version to find
     * @return The page content as a raw string
     * @throws ProviderException If the backend has issues.
     */
    String getPageText( String pageName, int version ) throws ProviderException;

    /**
     *  Returns the pure text of a page, no conversions.  Use this if you are writing something that depends on the parsing
     *  of the page. Note that you should always check for page existence through pageExists() before attempting to fetch
     *  the page contents.
     *
     *  This method is pretty similar to {@link #getPageText(String, int)}, except that it doesn't throw {@link ProviderException},
     *  it logs and swallows them.
     *
     *  @param page The name of the page to fetch.
     *  @param version If WikiPageProvider.LATEST_VERSION, then uses the latest version.
     *  @return The page contents.  If the page does not exist, returns an empty string.
     */
    String getPureText( String page, int version );

    /**
     *  Returns the pure text of a page, no conversions.  Use this if you are writing something that depends on the parsing
     *  the page. Note that you should always check for page existence through pageExists() before attempting to fetch
     *  the page contents.
     *
     *  This method is pretty similar to {@link #getPageText(String, int)}, except that it doesn't throw {@link ProviderException},
     *  it logs and swallows them.
     *
     *  @param page A handle to the WikiPage
     *  @return String of WikiText.
     *  @since 2.1.13, moved to PageManager on 2.11.0.
     */
    default String getPureText( final WikiPage page ) {
        return getPureText( page.getName(), page.getVersion() );
    }

    /**
     *  Returns the un-HTMLized text of the given version of a page. This method also replaces the &lt; and &amp; -characters with
     *  their respective HTML entities, thus making it suitable for inclusion on an HTML page.  If you want to have the page text
     *  without any conversions, use {@link #getPureText(String, int)}.
     *
     * @param page WikiName of the page to fetch
     * @param version  Version of the page to fetch
     * @return WikiText.
     */
    String getText( String page, int version );

    /**
     *  Returns the un-HTMLized text of the latest version of a page. This method also replaces the &lt; and &amp; -characters with
     *  their respective HTML entities, thus making it suitable for inclusion on an HTML page.  If you want to have the page text
     *  without any conversions, use {@link #getPureText(String, int)}.
     *
     *  @param page WikiName of the page to fetch.
     *  @return WikiText.
     */
    default String getText( final String page ) {
        return getText( page, WikiPageProvider.LATEST_VERSION );
    }

    /**
     *  Returns the un-HTMLized text of the given version of a page in the given context.  USE THIS METHOD if you don't know what doing.
     *  <p>
     *  This method also replaces the &lt; and &amp; -characters with their respective HTML entities, thus making it suitable
     *  for inclusion on an HTML page.  If you want to have the page text without any conversions, use {@link #getPureText(WikiPage)}.
     *
     *  @since 1.9.15.
     *  @param page A page reference (not an attachment)
     *  @return The page content as HTMLized String.
     *  @see PageManager#getPureText(WikiPage)
     */
    default String getText( final WikiPage page ) {
        return getText( page.getName(), page.getVersion() );
    }

    /**
     *  Writes the WikiText of a page into the page repository. If the <code>jspwiki.properties</code> file contains
     *  the property <code>jspwiki.approver.workflow.saveWikiPage</code> and its value resolves to a valid user,
     *  {@link org.apache.wiki.auth.authorize.Group} or {@link org.apache.wiki.auth.authorize.Role}, this method will
     *  place a {@link org.apache.wiki.workflow.Decision} in the approver's workflow inbox and throw a
     *  {@link org.apache.wiki.workflow.DecisionRequiredException}. If the submitting user is authenticated and the
     *  page save is rejected, a notification will be placed in the user's decision queue.
     *
     *  @since 2.1.28, moved to PageManager on 2.11.0
     *  @param context The current WikiContext
     *  @param text    The Wiki markup for the page.
     *  @throws WikiException if the save operation encounters an error during the save operation. If the page-save
     *  operation requires approval, the exception will be of type {@link org.apache.wiki.workflow.DecisionRequiredException}.
     *  Individual PageFilters, such as the {@link org.apache.wiki.filters.SpamFilter} may also throw a
     *  {@link org.apache.wiki.api.exceptions.RedirectException}.
     */
    void saveText( WikiContext context, String text ) throws WikiException;

    /**
     * Puts the page text into the repository.  Note that this method does NOT update
     * JSPWiki internal data structures, and therefore you should always use WikiEngine.saveText()
     *
     * @param page    Page to save
     * @param content Wikimarkup to save
     * @throws ProviderException If something goes wrong in the saving phase
     */
    void putPageText( WikiPage page, String content ) throws ProviderException;

    /**
     * Locks page for editing.  Note, however, that the PageManager will in no way prevent you from actually editing this page;
     * the lock is just for information.
     *
     * @param page WikiPage to lock
     * @param user Username to use for locking
     * @return null, if page could not be locked.
     */
    PageLock lockPage( WikiPage page, String user );

    /**
     * Marks a page free to be written again.  If there has not been a lock, will fail quietly.
     *
     * @param lock A lock acquired in lockPage().  Safe to be null.
     */
    void unlockPage( PageLock lock );

    /**
     * Returns the current lock owner of a page.  If the page is not locked, will return null.
     *
     * @param page The page to check the lock for
     * @return Current lock, or null, if there is no lock
     */
    PageLock getCurrentLock( WikiPage page );

    /**
     * Returns a list of currently applicable locks.  Note that by the time you get the list,
     * the locks may have already expired, so use this only for informational purposes.
     *
     * @return List of PageLock objects, detailing the locks.  If no locks exist, returns an empty list.
     * @since 2.0.22.
     */
    List< PageLock > getActiveLocks();

    /**
     *  Finds the corresponding WikiPage object based on the page name.  It always finds
     *  the latest version of a page.
     *
     *  @param pagereq The name of the page to look for.
     *  @return A WikiPage object, or null, if the page by the name could not be found.
     */
    WikiPage getPage( String pagereq );

    /**
     *  Finds the corresponding WikiPage object base on the page name and version.
     *
     *  @param pagereq The name of the page to look for.
     *  @param version The version number to look for.  May be WikiProvider.LATEST_VERSION,
     *  in which case it will look for the latest version (and this method then becomes
     *  the equivalent of getPage(String).
     *
     *  @return A WikiPage object, or null, if the page could not be found; or if there
     *  is no such version of the page.
     *  @since 1.6.7 (moved to PageManager on 2.11.0).
     */
    WikiPage getPage( String pagereq, int version );

    /**
     * Finds a WikiPage object describing a particular page and version.
     *
     * @param pageName The name of the page
     * @param version  A version number
     * @return A WikiPage object, or null, if the page does not exist
     * @throws ProviderException If there is something wrong with the page name or the repository
     */
    WikiPage getPageInfo( String pageName, int version ) throws ProviderException;

    /**
     * Gets a version history of page.  Each element in the returned List is a WikiPage.
     *
     * @param pageName The name of the page or attachment to fetch history for
     * @return If the page does not exist or there's some problem retrieving the version history, returns null,
     *         otherwise a List of WikiPages / Attachments, each corresponding to a different revision of the page / attachment.
     */
    < T extends WikiPage > List< T > getVersionHistory( String pageName );

    /**
     *  Returns the provider name.
     *
     *  @return The full class name of the current page provider.
     */
    String getCurrentProvider();

    /**
     * Returns a human-readable description of the current provider.
     *
     * @return A human-readable description.
     */
    String getProviderDescription();

    /**
     * Returns the total count of all pages in the repository. This
     * method is equivalent of calling getAllPages().size(), but
     * it swallows the ProviderException and returns -1 instead of
     * any problems.
     *
     * @return The number of pages, or -1, if there is an error.
     */
    int getTotalPageCount();

    /**
     *  Returns a Collection of WikiPages, sorted in time order of last change (i.e. first object is the most recently changed).
     *  This method also includes attachments.
     *
     *  @return Set of WikiPage objects.
     */
    Set< WikiPage > getRecentChanges();

    /**
     * Returns true, if the page exists (any version) on the underlying WikiPageProvider.
     *
     * @param pageName Name of the page.
     * @return A boolean value describing the existence of a page
     * @throws ProviderException If the backend fails or the name is illegal.
     */
    boolean pageExists( String pageName ) throws ProviderException;

    /**
     * Checks for existence of a specific page and version on the underlying WikiPageProvider.
     *
     * @param pageName Name of the page
     * @param version  The version to check
     * @return <code>true</code> if the page exists, <code>false</code> otherwise
     * @throws ProviderException If backend fails or name is illegal
     * @since 2.3.29
     */
    boolean pageExists( String pageName, int version ) throws ProviderException;

    /**
     *  Checks for existence of a specific page and version denoted by a WikiPage on the underlying WikiPageProvider.
     *
     *  @param page A WikiPage object describing the name and version.
     *  @return true, if the page (or alias, or attachment) exists.
     *  @throws ProviderException If something goes badly wrong.
     *  @since 2.0
     */
    default boolean pageExists( final WikiPage page ) throws ProviderException {
        if( page != null ) {
            return pageExists( page.getName(), page.getVersion() );
        }
        return false;
    }

    /**
     *  Returns true, if the requested page (or an alias) exists.  Will consider any version as existing. Will check for all types of
     *  WikiPages: wiki pages themselves, attachments and special pages (non-existant references to other pages).
     *
     *  @param page WikiName of the page.
     *  @return true, if page (or attachment) exists.
     */
    boolean wikiPageExists( String page );

    /**
     *  Returns true, if the requested page (or an alias) exists with the requested version. Will check for all types of
     *  WikiPages: wiki pages themselves, attachments and special pages (non-existant references to other pages).
     *
     *  @param page Page name
     *  @param version Page version
     *  @return True, if page (or alias, or attachment) exists
     *  @throws ProviderException If the provider fails.
     */
    boolean wikiPageExists( String page, int version ) throws ProviderException;

    /**
     *  Returns true, if the requested page (or an alias) exists, with the specified version in the WikiPage. Will check for all types of
     *  WikiPages: wiki pages themselves, attachments and special pages (non-existant references to other pages).
     *
     *  @param page A WikiPage object describing the name and version.
     *  @return true, if the page (or alias, or attachment) exists.
     *  @throws ProviderException If something goes badly wrong.
     *  @since 2.0
     */
    default boolean wikiPageExists( final WikiPage page ) throws ProviderException {
        if( page != null ) {
            return wikiPageExists( page.getName(), page.getVersion() );
        }
        return false;
    }

    /**
     * Deletes only a specific version of a WikiPage.
     *
     * @param page The page to delete.
     * @throws ProviderException if the page fails
     */
    void deleteVersion( WikiPage page ) throws ProviderException;

    /**
     *  Deletes a page or an attachment completely, including all versions.  If the page does not exist, does nothing.
     *
     * @param pageName The name of the page.
     * @throws ProviderException If something goes wrong.
     */
    void deletePage( String pageName ) throws ProviderException;

    /**
     * Deletes an entire page, all versions, all traces.
     *
     * @param page The WikiPage to delete
     * @throws ProviderException If the repository operation fails
     */
    void deletePage( WikiPage page ) throws ProviderException;

    /**
     * Returns the configured {@link PageSorter}.
     * 
     * @return the configured {@link PageSorter}.
     */
    PageSorter getPageSorter();

}