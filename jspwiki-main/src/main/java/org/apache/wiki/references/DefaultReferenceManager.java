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

import org.apache.commons.lang3.time.StopWatch;
import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.LinkCollector;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.filters.BasicPageFilter;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventManager;
import org.apache.wiki.event.WikiPageEvent;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.util.TextUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/*
  BUGS

  - if a wikilink is added to a page, then removed, RefMan still thinks that the page refers to the wikilink page. Hm.

  - if a page is deleted, gets very confused.

  - Serialization causes page attributes to be missing, when InitializablePlugins are not executed properly.  Thus,
    serialization should really also mark whether a page is serializable or not...
 */


/*
   A word about synchronizing:

   I expect this object to be accessed in three situations:
   - when a WikiEngine is created and it scans its wikipages
   - when the WE saves a page
   - when a JSP page accesses one of the WE's ReferenceManagers to display a list of (un)referenced pages.

   So, access to this class is fairly rare, and usually triggered by user interaction. OTOH, the methods in this class use their storage
   objects intensively (and, sorry to say, in an unoptimized manner =). My deduction: using unsynchronized HashMaps etc and syncing methods
   or code blocks is preferrable to using slow, synced storage objects. We don't have iterative code here, so I'm going to use synced
   methods for now.

   Please contact me if you notice problems with ReferenceManager, and especially with synchronization, or if you have suggestions about
   syncing.

   ebu@memecry.net
*/

/**
 *  Keeps track of wikipage references:
 *  <UL>
 *  <LI>What pages a given page refers to
 *  <LI>What pages refer to a given page
 *  </UL>
 *
 *  This is a quick'n'dirty approach without any finesse in storage and searching algorithms; we trust java.util.*.
 *  <P>
 *  This class contains two HashMaps, m_refersTo and m_referredBy. The first is indexed by WikiPage names and contains a Collection of all
 *  WikiPages the page refers to. (Multiple references are not counted, naturally.) The second is indexed by WikiPage names and contains
 *  a Set of all pages that refer to the indexing page. (Notice - the keys of both Maps should be kept in sync.)
 *  <P>
 *  When a page is added or edited, its references are parsed, a Collection is received, and we crudely replace anything previous with
 *  this new Collection. We then check each referenced page name and make sure they know they are referred to by the new page.
 *  <P>
 *  Based on this information, we can perform non-optimal searches for e.g. unreferenced pages, top ten lists, etc.
 *  <P>
 *  The owning class must take responsibility of filling in any pre-existing information, probably by loading each and every WikiPage
 *  and calling this class to update the references when created.
 *
 *  @since 1.6.1 (as of 2.11.0, moved to org.apache.wiki.references)
 */

// FIXME: The way that we save attributes is now a major booboo, and must be
//        replace forthwith.  However, this is a workaround for the great deal
//        of problems that occur here...
public class DefaultReferenceManager extends BasicPageFilter implements ReferenceManager {

    /**
     *  Maps page wikiname to a Collection of pages it refers to. The Collection must contain Strings. The Collection may contain
     *  names of non-existing pages.
     */
    private Map< String, Collection< String > > m_refersTo;
    private Map< String, Collection< String > > m_unmutableRefersTo;

    /**
     *  Maps page wikiname to a Set of referring pages. The Set must contain Strings. Non-existing pages (a reference exists, but
     *  not a file for the page contents) may have an empty Set in m_referredBy.
     */
    private Map< String, Set< String > > m_referredBy;
    private Map< String, Set< String > > m_unmutableReferredBy;

    private boolean m_matchEnglishPlurals;

    private static final Logger log = Logger.getLogger( DefaultReferenceManager.class);
    private static final String SERIALIZATION_FILE = "refmgr.ser";
    private static final String SERIALIZATION_DIR  = "refmgr-attr";

    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 4L;

    /**
     *  Builds a new ReferenceManager.
     *
     *  @param engine The WikiEngine to which this is managing references to.
     */
    public DefaultReferenceManager( final WikiEngine engine ) {
        m_refersTo = new HashMap<>();
        m_referredBy = new HashMap<>();
        m_engine = engine;
        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(), WikiEngine.PROP_MATCHPLURALS, false );

        //
        //  Create two maps that contain unmutable versions of the two basic maps.
        //
        m_unmutableReferredBy = Collections.unmodifiableMap( m_referredBy );
        m_unmutableRefersTo   = Collections.unmodifiableMap( m_refersTo );
    }

    /**
     *  Does a full reference update.  Does not sync; assumes that you do it afterwards.
     */
    private void updatePageReferences( final WikiPage page ) throws ProviderException {
        final String content = m_engine.getPageManager().getPageText( page.getName(), WikiPageProvider.LATEST_VERSION );
        final Collection< String > links = scanWikiLinks( page, content );
        final TreeSet< String > res = new TreeSet<>( links );
        final List< Attachment > attachments = m_engine.getAttachmentManager().listAttachments( page );
        for( final Attachment att : attachments ) {
            res.add( att.getName() );
        }

        internalUpdateReferences( page.getName(), res );
    }

    /**
     *  Initializes the entire reference manager with the initial set of pages from the collection.
     *
     *  @param pages A collection of all pages you want to be included in the reference count.
     *  @since 2.2
     *  @throws ProviderException If reading of pages fails.
     */
    public void initialize( final Collection< WikiPage > pages ) throws ProviderException {
        log.debug( "Initializing new ReferenceManager with " + pages.size() + " initial pages." );
        final StopWatch sw = new StopWatch();
        sw.start();
        log.info( "Starting cross reference scan of WikiPages" );

        //  First, try to serialize old data from disk.  If that fails, we'll go and update the entire reference lists (which'll take time)
        try {
            //  Unserialize things.  The loop below cannot be combined with the other loop below, simply because
            //  engine.getPage() has side effects such as loading initializing the user databases, which in turn want all
            //  of the pages to be read already...
            //
            //  Yes, this is a kludge.  We know.  Will be fixed.
            final long saved = unserializeFromDisk();

            for( final WikiPage page : pages ) {
                unserializeAttrsFromDisk( page );
            }

            //  Now we must check if any of the pages have been changed  while we were in the electronic la-la-land,
            //  and update the references for them.
            for( final WikiPage page : pages ) {
                if( !( page instanceof Attachment ) ) {
                    // Refresh with the latest copy
                    final WikiPage wp = m_engine.getPageManager().getPage( page.getName() );

                    if( wp.getLastModified() == null ) {
                        log.fatal( "Provider returns null lastModified.  Please submit a bug report." );
                    } else if( wp.getLastModified().getTime() > saved ) {
                        updatePageReferences( wp );
                    }
                }
            }

        } catch( final Exception e ) {
            log.info( "Unable to unserialize old refmgr information, rebuilding database: " + e.getMessage() );
            buildKeyLists( pages );

            // Scan the existing pages from disk and update references in the manager.
            for( final WikiPage page : pages ) {
                // We cannot build a reference list from the contents of attachments, so we skip them.
                if( !( page instanceof Attachment ) ) {
                    updatePageReferences( page );
                    serializeAttrsToDisk( page );
                }
            }

            serializeToDisk();
        }

        sw.stop();
        log.info( "Cross reference scan done in "+sw );

        WikiEventManager.getInstance().addWikiEventListener( m_engine.getPageManager(), this );
    }

    /**
     *  Reads the serialized data from the disk back to memory. Returns the date when the data was last written on disk
     */
    @SuppressWarnings("unchecked")
    private synchronized long unserializeFromDisk() throws IOException, ClassNotFoundException {
        final long saved;

        final File f = new File( m_engine.getWorkDir(), SERIALIZATION_FILE );
        try( final ObjectInputStream in = new ObjectInputStream( new BufferedInputStream( new FileInputStream( f ) ) ) ) {
            final StopWatch sw = new StopWatch();
            sw.start();

            final long ver = in.readLong();

            if( ver != serialVersionUID ) {
                throw new IOException("File format has changed; I need to recalculate references.");
            }

            saved        = in.readLong();
            m_refersTo   = ( Map< String, Collection< String > > ) in.readObject();
            m_referredBy = ( Map< String, Set< String > > ) in.readObject();

            m_unmutableReferredBy = Collections.unmodifiableMap( m_referredBy );
            m_unmutableRefersTo   = Collections.unmodifiableMap( m_refersTo );

            sw.stop();
            log.debug("Read serialized data successfully in "+sw);
        }

        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeToDisk() {
        final File f = new File( m_engine.getWorkDir(), SERIALIZATION_FILE );
        try( final ObjectOutputStream out = new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( f ) ) ) ) {
            final StopWatch sw = new StopWatch();
            sw.start();

            out.writeLong( serialVersionUID );
            out.writeLong( System.currentTimeMillis() ); // Timestamp
            out.writeObject( m_refersTo );
            out.writeObject( m_referredBy );

            sw.stop();

            log.debug("serialization done - took "+sw);
        } catch( final IOException ioe ) {
            log.error("Unable to serialize!", ioe);
        }
    }

    private String getHashFileName( final String pageName ) {
        if( pageName == null ) {
            return null;
        }
		try {
            final MessageDigest digest = MessageDigest.getInstance( "MD5" );
            final byte[] dig = digest.digest( pageName.getBytes( StandardCharsets.UTF_8 ) );

	        return TextUtil.toHexString( dig ) + ".cache";
		} catch( final NoSuchAlgorithmException e ) {
			log.fatal( "What do you mean - no such algorithm?", e );
			return null;
		}
    }

    /**
     *  Reads the serialized data from the disk back to memory. Returns the date when the data was last written on disk
     */
    private synchronized long unserializeAttrsFromDisk( final WikiPage p ) throws IOException, ClassNotFoundException {
        long saved = 0L;

        //  Find attribute cache, and check if it exists
        final String hashName = getHashFileName( p.getName() );
        if( hashName != null ) {
        	File f = new File( m_engine.getWorkDir(), SERIALIZATION_DIR );
            f = new File( f, hashName );
            if( !f.exists() ) {
                return 0L;
            }

            try( final ObjectInputStream in = new ObjectInputStream( new BufferedInputStream( new FileInputStream( f ) ) ) ) {
                final StopWatch sw = new StopWatch();
                sw.start();
                log.debug( "Deserializing attributes for " + p.getName() );

                final long ver = in.readLong();
                if( ver != serialVersionUID ) {
                    log.debug("File format has changed; cannot deserialize.");
                    return 0L;
                }

                saved = in.readLong();
                final String name  = in.readUTF();
                if( !name.equals( p.getName() ) ) {
                    log.debug("File name does not match (" + name + "), skipping...");
                    return 0L; // Not here
                }

                final long entries = in.readLong();
                for( int i = 0; i < entries; i++ ) {
                    final String key   = in.readUTF();
                    final Object value = in.readObject();
                    p.setAttribute( key, value );
                    log.debug("   attr: "+key+"="+value);
                }

                sw.stop();
                log.debug("Read serialized data for "+name+" successfully in "+sw);
                p.setHasMetadata();
            }
        }

        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeAttrsToDisk( final WikiPage p ) {
        final StopWatch sw = new StopWatch();
        sw.start();

        final String hashName = getHashFileName( p.getName() );
        if( hashName != null ) {
        	File f = new File( m_engine.getWorkDir(), SERIALIZATION_DIR );
            if( !f.exists() ) {
                f.mkdirs();
            }

            //  Create a digest for the name
            f = new File( f, hashName );

            try( final ObjectOutputStream out =  new ObjectOutputStream( new BufferedOutputStream( new FileOutputStream( f ) ) ) ) {
                // new Set to avoid concurrency issues
                final Set< Map.Entry < String, Object > > entries = new HashSet<>( p.getAttributes().entrySet() );

                if( entries.size() == 0 ) {
                    //  Nothing to serialize, therefore we will just simply remove the serialization file so that the
                    //  next time we boot, we don't deserialize old data.
                    f.delete();
                    return;
                }

                out.writeLong( serialVersionUID );
                out.writeLong( System.currentTimeMillis() ); // Timestamp
                out.writeUTF( p.getName() );
                out.writeLong( entries.size() );

                for( final Map.Entry< String, Object > e : entries ) {
                    if( e.getValue() instanceof Serializable ) {
                        out.writeUTF( e.getKey() );
                        out.writeObject( e.getValue() );
                    }
                }

            } catch( final IOException e ) {
                log.error( "Unable to serialize!", e );
            } finally {
                sw.stop();
                log.debug( "serialization for " + p.getName() + " done - took " + sw );
            }
        }

    }

    /**
     *  After the page has been saved, updates the reference lists.
     *
     *  @param context {@inheritDoc}
     *  @param content {@inheritDoc}
     */
    @Override
	public void postSave( final WikiContext context, final String content ) {
        final WikiPage page = context.getPage();
        updateReferences( page.getName(), scanWikiLinks( page, content ) );
        serializeAttrsToDisk( page );
    }

    /**
     *  Reads a WikiPageful of data from a String and returns all links internal to this Wiki in a Collection.
     *
     *  @param page The WikiPage to scan
     *  @param pagedata The page contents
     *  @return a Collection of Strings
     */
    public Collection< String > scanWikiLinks( final WikiPage page, final String pagedata ) {
        final LinkCollector localCollector = new LinkCollector();
        m_engine.getRenderingManager().textToHTML( new WikiContext( m_engine, page ),
                                                   pagedata,
                                                   localCollector,
                                                   null,
                                                   localCollector,
                                                   false,
                                                   true );

        return localCollector.getLinks();
    }

    /**
     * Updates the m_referedTo and m_referredBy hashmaps when a page has been deleted.
     * <P>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set has to be removed to keep the map clean.
     * Within the m_referredBy map the name is stored as a value. Since a key can have more than one value we have to
     * delete just the key-value-pair referring page:deleted page.
     *
     *  @param page Name of the page to remove from the maps.
     */
    public synchronized void pageRemoved( final WikiPage page ) {
        pageRemoved( page.getName() );
    }

    private void pageRemoved( final String pageName ) {
        final Collection< String > refTo = m_refersTo.get( pageName );
        if( refTo != null ) {
            for( final String referredPageName : refTo ) {
                final Set< String > refBy = m_referredBy.get( referredPageName );
                if( refBy == null ) {
                    throw new InternalWikiException( "Refmgr out of sync: page " + pageName +
                                                     " refers to " + referredPageName + ", which has null referrers." );
                }

                refBy.remove( pageName );
                m_referredBy.remove( referredPageName );

                // We won't put it back again if it becomes empty and does not exist.  It will be added
                // later on anyway, if it becomes referenced again.
                if( !( refBy.isEmpty() && !m_engine.getPageManager().wikiPageExists( referredPageName ) ) ) {
                    m_referredBy.put( referredPageName, refBy );
                }
            }

            log.debug("Removing from m_refersTo HashMap key:value "+pageName+":"+m_refersTo.get( pageName ));
            m_refersTo.remove( pageName );
        }

        final Set< String > refBy = m_referredBy.get( pageName );
        if( refBy == null || refBy.isEmpty() ) {
            m_referredBy.remove( pageName );
        }

        //  Remove any traces from the disk, too
        serializeToDisk();

        final String hashName = getHashFileName( pageName );
        if( hashName != null ) {
        	File f = new File( m_engine.getWorkDir(), SERIALIZATION_DIR );
            f = new File( f, getHashFileName( pageName ) );
            if( f.exists() ) {
                f.delete();
            }
        }
    }

    /**
     *  Updates all references for the given page.
     *
     *  @param page wiki page for which references should be updated
     */
    public void updateReferences( final WikiPage page ) {
        final String pageData = m_engine.getPageManager().getPureText( page.getName(), WikiProvider.LATEST_VERSION );
        updateReferences( page.getName(), scanWikiLinks( page, pageData ) );
    }

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
    public synchronized void updateReferences( final String page, final Collection< String > references ) {
        internalUpdateReferences( page, references );
        serializeToDisk();
    }

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo entry for this page already exists, it is
     *  removed and a new one is built from scratch. Also calls updateReferredBy() for each referenced page.
     *  <p>
     *  This method does not synchronize the database to disk.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    private void internalUpdateReferences( String page, final Collection< String > references) {
        page = getFinalPageName( page );

        // Create a new entry in m_refersTo.
        final Collection< String > oldRefTo = m_refersTo.get( page );
        m_refersTo.remove( page );

        final TreeSet< String > cleanedRefs = new TreeSet<>();
        for( final String ref : references ) {
            final String reference = getFinalPageName( ref );
            cleanedRefs.add( reference );
        }

        m_refersTo.put( page, cleanedRefs );

        //  We know the page exists, since it's making references somewhere. If an entry for it didn't exist previously
        //  in m_referredBy, make sure one is added now.
        if( !m_referredBy.containsKey( page ) ) {
            m_referredBy.put( page, new TreeSet<>() );
        }

        //  Get all pages that used to be referred to by 'page' and remove that reference. (We don't want to try to figure out
        //  which particular references were removed...)
        cleanReferredBy( page, oldRefTo, cleanedRefs );

        //  Notify all referred pages of their referinesshoodicity.
        for( final String referredPageName : cleanedRefs ) {
            updateReferredBy( getFinalPageName( referredPageName ), page );
        }
    }

    /**
     * Returns the refers-to list. For debugging.
     *
     * @return The refers-to list.
     */
    protected Map< String, Collection< String > > getRefersTo() {
        return m_refersTo;
    }

    /**
     * Returns the referred-by list. For debugging.
     *
     * @return Referred-by lists.
     */
    protected Map< String, Set< String > > getReferredBy() {
        return m_referredBy;
    }

    /**
     * Cleans the 'referred by' list, removing references by 'referrer' to any other page. Called after 'referrer' is removed.
     *
     * Two ways to go about this. One is to look up all pages previously referred by referrer and remove referrer
     * from their lists, and let the update put them back in (except possibly removed ones).
     *
     * The other is to get the old referred-to list, compare to the new, and tell the ones missing in the latter to remove referrer from
     * their list.
     *
     * We'll just try the first for now. Need to come back and optimize this a bit.
     */
    private void cleanReferredBy( final String referrer,
                                  final Collection< String > oldReferred,
                                  final Collection< String > newReferred ) {
        if( oldReferred == null ) {
            return;
        }

        for( final String referredPage : oldReferred ) {
            final Set< String > oldRefBy = m_referredBy.get( referredPage );
            if( oldRefBy != null ) {
                oldRefBy.remove( referrer );
            }

            // If the page is referred to by no one AND it doesn't even exist, we might just as well forget about this
            // entry. It will be added again elsewhere if new references appear.
            if( ( oldRefBy == null || oldRefBy.isEmpty() ) && !m_engine.getPageManager().wikiPageExists( referredPage ) ) {
                m_referredBy.remove( referredPage );
            }
        }
    }

    /**
     * When initially building a ReferenceManager from scratch, call this method BEFORE calling updateReferences() with
     * a full list of existing page names. It builds the refersTo and referredBy key lists, thus enabling updateReferences()
     * to function correctly.
     * <P>
     * This method should NEVER be called after initialization. It clears all mappings from the reference tables.
     *
     * @param pages a Collection containing WikiPage objects.
     */
    private synchronized void buildKeyLists( final Collection< WikiPage > pages ) {
        m_refersTo.clear();
        m_referredBy.clear();
        if( pages == null ) {
            return;
        }

        try {
            for( final WikiPage page : pages ) {
                // We add a non-null entry to referredBy to indicate the referred page exists
                m_referredBy.put( page.getName(), new TreeSet<>() );
                // Just add a key to refersTo; the keys need to be in sync with referredBy.
                m_refersTo.put( page.getName(), null );
            }
        } catch( final ClassCastException e ) {
            log.fatal( "Invalid collection entry in ReferenceManager.buildKeyLists().", e );
        }
    }


    /**
     * Marks the page as referred to by the referrer. If the page does not exist previously, nothing is done. (This means
     * that some page, somewhere, has a link to a page that does not exist.)
     * <P>
     * This method is NOT synchronized. It should only be referred to from within a synchronized method, or it should be
     * made synced if necessary.
     */
    private void updateReferredBy( final String page, final String referrer ) {
        // We're not really interested in first level self-references.
        /*
        if( page.equals( referrer ) )
        {
            return;
        }
        */
        // Neither are we interested if plural forms refer to each other.
        if( m_matchEnglishPlurals ) {
            final String p2 = page.endsWith( "s" ) ? page.substring( 0, page.length() - 1 ) : page + "s";
            if( referrer.equals( p2 ) ) {
                return;
            }
        }

        // Even if 'page' has not been created yet, it can still be referenced. This requires we don't use m_referredBy
        // keys when looking up missing pages, of course.
        final Set< String > referrers = m_referredBy.computeIfAbsent( page, k -> new TreeSet<>() );
        referrers.add( referrer );
    }


    /**
     * Clears the references to a certain page so it's no longer in the map.
     *
     * @param pagename  Name of the page to clear references for.
     */
    public synchronized void clearPageEntries( String pagename ) {
        pagename = getFinalPageName( pagename );

        //  Remove this item from the referredBy list of any page which this item refers to.
        final Collection< String > c = m_refersTo.get( pagename );
        if( c != null ) {
            for( final String key : c ) {
                final Collection< ? > dref = m_referredBy.get( key );
                dref.remove( pagename );
            }
        }

        //  Finally, remove direct references.
        m_referredBy.remove( pagename );
        m_refersTo.remove( pagename );
    }


    /**
     *  Finds all unreferenced pages. This requires a linear scan through m_referredBy to locate keys with null or empty values.
     *
     *  @return The Collection of Strings
     */
    public synchronized Collection< String > findUnreferenced() {
        final ArrayList< String > unref = new ArrayList<>();
        for( final String key : m_referredBy.keySet() ) {
            final Set< ? > refs = getReferenceList( m_referredBy, key );
            if( refs == null || refs.isEmpty() ) {
                unref.add( key );
            }
        }

        return unref;
    }


    /**
     * Finds all references to non-existant pages. This requires a linear scan through m_refersTo values; each value
     * must have a corresponding key entry in the reference Maps, otherwise such a page has never been created.
     * <P>
     * Returns a Collection containing Strings of unreferenced page names. Each non-existant page name is shown only
     * once - we don't return information on who referred to it.
     *
     * @return A Collection of Strings
     */
    public synchronized Collection< String > findUncreated() {
        final TreeSet< String > uncreated = new TreeSet<>();

        // Go through m_refersTo values and check that m_refersTo has the corresponding keys.
        // We want to reread the code to make sure our HashMaps are in sync...
        final Collection< Collection< String > > allReferences = m_refersTo.values();
        for( final Collection<String> refs : allReferences ) {
            if( refs != null ) {
                for( final String aReference : refs ) {
                    if( !m_engine.getPageManager().wikiPageExists( aReference ) ) {
                        uncreated.add( aReference );
                    }
                }
            }
        }

        return uncreated;
    }

    /**
     *  Searches for the given page in the given Map, and returns the set of references. This method also takes care of
     *  English plural matching.
     *
     *  @param coll The Map to search in
     *  @param pagename The name to find.
     *  @return The references list.
     */
    private < T > Set< T > getReferenceList( final Map< String, Set< T > > coll, final String pagename ) {
        Set< T > refs = coll.get( pagename );

        if( m_matchEnglishPlurals ) {
            //  We'll add also matches from the "other" page.
            final Set< T > refs2;

            if( pagename.endsWith( "s" ) ) {
                refs2 = coll.get( pagename.substring( 0, pagename.length() - 1 ) );
            } else {
                refs2 = coll.get( pagename + "s" );
            }

            if( refs2 != null ) {
                if( refs != null ) {
                    refs.addAll( refs2 );
                } else {
                    refs = refs2;
                }
            }
        }
        return refs;
    }

    /**
     * Find all pages that refer to this page. Returns null if the page does not exist or is not referenced at all,
     * otherwise returns a collection containing page names (String) that refer to this one.
     * <p>
     * @param pagename The page to find referrers for.
     * @return A Set of Strings.  May return null, if the page does not exist, or if it has no references.
     */
    public synchronized Set< String > findReferrers( final String pagename ) {
        final Set< String > refs = getReferenceList( m_referredBy, pagename );
        if( refs == null || refs.isEmpty() ) {
            return null;
        }

        return refs;
    }

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
    public Set< String > findReferredBy( final String pageName ) {
        return m_unmutableReferredBy.get( getFinalPageName(pageName) );
    }

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
    public Collection< String > findRefersTo( final String pageName ) {
        return m_unmutableRefersTo.get( getFinalPageName( pageName ) );
    }

    /**
     * This 'deepHashCode' can be used to determine if there were any modifications made to the underlying to and by maps of the
     * ReferenceManager. The maps of the ReferenceManager are not synchronized, so someone could add/remove entries in them while the
     * hashCode is being computed.
     *
     * This method traps and retries if a concurrent modification occurs.
     *
     * @return Sum of the hashCodes for the to and by maps of the ReferenceManager
     * @since 2.3.24
     */
    //
    //   TODO: It is unnecessary to calculate the hashcode; it should be calculated only when the hashmaps are changed.  This is slow.
    //
    public int deepHashCode() {
        boolean failed = true;
        int signature = 0;

        while( failed ) {
            signature = 0;
            try {
                signature ^= m_referredBy.hashCode();
                signature ^= m_refersTo.hashCode();
                failed = false;
            } catch ( final ConcurrentModificationException e) {
                Thread.yield();
            }
        }

        return signature;
    }

    /**
     *  Returns a list of all pages that the ReferenceManager knows about. This should be roughly equivalent to
     *  PageManager.getAllPages(), but without the potential disk access overhead.  Note that this method is not guaranteed
     *  to return a Set of really all pages (especially during startup), but it is very fast.
     *
     *  @return A Set of all defined page names that ReferenceManager knows about.
     *  @since 2.3.24
     */
    public Set< String > findCreated() {
        return new HashSet<>( m_refersTo.keySet() );
    }

    private String getFinalPageName( final String orig ) {
        try {
            final String s = m_engine.getFinalPageName( orig );
            return s != null ? s : orig;
        } catch( final ProviderException e ) {
            log.error("Error while trying to fetch a page name; trying to cope with the situation.",e);
            return orig;
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
	public void actionPerformed( final WikiEvent event ) {
        if( event instanceof WikiPageEvent && event.getType() == WikiPageEvent.PAGE_DELETED ) {
            final String pageName = ( ( WikiPageEvent ) event ).getPageName();
            if( pageName != null ) {
                pageRemoved( pageName );
            }
        }
    }

}
