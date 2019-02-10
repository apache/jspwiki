package org.apache.wiki.pages;

import java.util.Collection;
import java.util.List;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.providers.WikiPageProvider;


public interface PageManager extends WikiEventListener {

    /** The property value for setting the current page provider.  Value is {@value}. */
    String PROP_PAGEPROVIDER = "jspwiki.pageProvider";
    /** The property value for setting the cache on/off.  Value is {@value}. */
    String PROP_USECACHE = "jspwiki.usePageCache";
    /** The property value for setting the amount of time before the page locks expire. Value is {@value}. */
    String PROP_LOCKEXPIRY = "jspwiki.lockExpiryTime";
    /** The message key for storing the text for the presave task.  Value is <tt>{@value}</tt> */
    String PRESAVE_TASK_MESSAGE_KEY = "task.preSaveWikiPage";
    /** The workflow attribute which stores the wikiContext. */
    String PRESAVE_WIKI_CONTEXT = "wikiContext";
    /** The name of the key from jspwiki.properties which defines who shall approve the workflow of storing a wikipage.  Value is <tt>{@value}</tt> */
    String SAVE_APPROVER = "workflow.saveWikiPage";
    /** The message key for storing the Decision text for saving a page.  Value is {@value}. */
    String SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    /** The message key for rejecting the decision to save the page.  Value is {@value}. */
    String SAVE_REJECT_MESSAGE_KEY = "notification.saveWikiPage.reject";
    /** The message key of the text to finally approve a page save.  Value is {@value}. */
    String SAVE_TASK_MESSAGE_KEY = "task.saveWikiPage";
    /** Fact name for storing the page name.  Value is {@value}. */
    String FACT_PAGE_NAME = "fact.pageName";
    /** Fact name for storing a diff text. Value is {@value}. */
    String FACT_DIFF_TEXT = "fact.diffText";
    /** Fact name for storing the current text.  Value is {@value}. */
    String FACT_CURRENT_TEXT = "fact.currentText";
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    String FACT_PROPOSED_TEXT = "fact.proposedText";
    /** Fact name for storing whether the user is authenticated or not.  Value is {@value}. */
    String FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    /**
     * Returns the page provider currently in use.
     *
     * @return A WikiPageProvider instance.
     */
    WikiPageProvider getProvider();

    /**
     * Returns all pages in some random order.  If you need just the page names,
     * please see {@link org.apache.wiki.ReferenceManager#findCreated() ReferenceManager#findCreated()}, which is probably a lot
     * faster.  This method may cause repository access.
     *
     * @return A Collection of WikiPage objects.
     * @throws ProviderException If the backend has problems.
     */
    Collection<WikiPage> getAllPages() throws ProviderException;

    /**
     * Fetches the page text from the repository.  This method also does some sanity checks,
     * like checking for the pageName validity, etc.  Also, if the page repository has been
     * modified externally, it is smart enough to handle such occurrences.
     *
     * @param pageName The name of the page to fetch.
     * @param version  The version to find
     * @return The page content as a raw string
     * @throws ProviderException If the backend has issues.
     */
    String getPageText(String pageName, int version) throws ProviderException;

    /**
     * Returns the WikiEngine to which this PageManager belongs to.
     *
     * @return The WikiEngine object.
     */
    WikiEngine getEngine();

    /**
     * Puts the page text into the repository.  Note that this method does NOT update
     * JSPWiki internal data structures, and therefore you should always use WikiEngine.saveText()
     *
     * @param page    Page to save
     * @param content Wikimarkup to save
     * @throws ProviderException If something goes wrong in the saving phase
     */
    void putPageText(WikiPage page, String content) throws ProviderException;

    /**
     * Locks page for editing.  Note, however, that the PageManager
     * will in no way prevent you from actually editing this page;
     * the lock is just for information.
     *
     * @param page WikiPage to lock
     * @param user Username to use for locking
     * @return null, if page could not be locked.
     */
    PageLock lockPage(WikiPage page, String user);

    /**
     * Marks a page free to be written again.  If there has not been a lock, will fail quietly.
     *
     * @param lock A lock acquired in lockPage().  Safe to be null.
     */
    void unlockPage(PageLock lock);

    /**
     * Returns the current lock owner of a page.  If the page is not
     * locked, will return null.
     *
     * @param page The page to check the lock for
     * @return Current lock, or null, if there is no lock
     */
    PageLock getCurrentLock(WikiPage page);

    /**
     * Returns a list of currently applicable locks.  Note that by the time you get the list,
     * the locks may have already expired, so use this only for informational purposes.
     *
     * @return List of PageLock objects, detailing the locks.  If no locks exist, returns
     *         an empty list.
     * @since 2.0.22.
     */
    List<PageLock> getActiveLocks();

    /**
     * Finds a WikiPage object describing a particular page and version.
     *
     * @param pageName The name of the page
     * @param version  A version number
     * @return A WikiPage object, or null, if the page does not exist
     * @throws ProviderException If there is something wrong with the page
     *                           name or the repository
     */
    WikiPage getPageInfo(String pageName, int version) throws ProviderException;

    /**
     * Gets a version history of page.  Each element in the returned List is a WikiPage.
     *
     * @param pageName The name of the page to fetch history for
     * @return If the page does not exist, returns null, otherwise a List of WikiPages.
     * @throws ProviderException If the repository fails.
     */
    List<WikiPage> getVersionHistory(String pageName) throws ProviderException;

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
     * Returns true, if the page exists (any version).
     *
     * @param pageName Name of the page.
     * @return A boolean value describing the existence of a page
     * @throws ProviderException If the backend fails or the name is illegal.
     */
    boolean pageExists(String pageName) throws ProviderException;

    /**
     * Checks for existence of a specific page and version.
     *
     * @param pageName Name of the page
     * @param version  The version to check
     * @return <code>true</code> if the page exists, <code>false</code> otherwise
     * @throws ProviderException If backend fails or name is illegal
     * @since 2.3.29
     */
    boolean pageExists(String pageName, int version) throws ProviderException;

    /**
     * Deletes only a specific version of a WikiPage.
     *
     * @param page The page to delete.
     * @throws ProviderException if the page fails
     */
    void deleteVersion(WikiPage page) throws ProviderException;

    /**
     * Deletes an entire page, all versions, all traces.
     *
     * @param page The WikiPage to delete
     * @throws ProviderException If the repository operation fails
     */
    void deletePage(WikiPage page) throws ProviderException;

    /**
     * Listens for {@link org.apache.wiki.event.WikiSecurityEvent#PROFILE_NAME_CHANGED}
     * events. If a user profile's name changes, each page ACL is inspected. If an entry contains
     * a name that has changed, it is replaced with the new one. No events are emitted
     * as a consequence of this method, because the page contents are still the same; it is
     * only the representations of the names within the ACL that are changing.
     *
     * @param event The event
     */
    void actionPerformed(WikiEvent event);

    /**
     * Returns the configured {@link PageSorter}.
     * 
     * @return the configured {@link PageSorter}.
     */
    PageSorter getPageSorter();

}