/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2004 Janne Jalkanen (Janne.Jalkanen@iki.fi),
                            Erik Bunn (ebu@memecry.net)

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
package com.ecyrd.jspwiki;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import org.apache.commons.lang.time.StopWatch;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.filters.BasicPageFilter;
import com.ecyrd.jspwiki.modules.InternalModule;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.providers.WikiPageProvider;

/*
  BUGS

  - if a wikilink is added to a page, then removed, RefMan still thinks that
    the page refers to the wikilink page. Hm.

  - if a page is deleted, gets very confused.

  - Serialization causes page attributes to be missing, when InitializablePlugins
    are not executed properly.  Thus, serialization should really also mark whether
    a page is serializable or not...
 */


/* 
   A word about synchronizing:
   
   I expect this object to be accessed in three situations:
   - when a WikiEngine is created and it scans its wikipages
   - when the WE saves a page
   - when a JSP page accesses one of the WE's ReferenceManagers 
     to display a list of (un)referenced pages.

   So, access to this class is fairly rare, and usually triggered by 
   user interaction. OTOH, the methods in this class use their storage
   objects intensively (and, sorry to say, in an unoptimized manner =).
   My deduction: using unsynchronized HashMaps etc and syncing methods
   or code blocks is preferrable to using slow, synced storage objects.
   We don't have iterative code here, so I'm going to use synced methods
   for now.
   
   Please contact me if you notice problems with ReferenceManager, and
   especially with synchronization, or if you have suggestions about
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
 *  This is a quick'n'dirty approach without any finesse in storage and
 *  searching algorithms; we trust java.util.*.
 *  <P>
 *  This class contains two HashMaps, m_refersTo and m_referredBy. The 
 *  first is indexed by WikiPage names and contains a Collection of all
 *  WikiPages the page refers to. (Multiple references are not counted,
 *  naturally.) The second is indexed by WikiPage names and contains 
 *  a Set of all pages that refer to the indexing page. (Notice - 
 *  the keys of both Maps should be kept in sync.)
 *  <P>
 *  When a page is added or edited, its references are parsed, a Collection
 *  is received, and we crudely replace anything previous with this new
 *  Collection. We then check each referenced page name and make sure they
 *  know they are referred to by the new page.
 *  <P>
 *  Based on this information, we can perform non-optimal searches for 
 *  e.g. unreferenced pages, top ten lists, etc.
 *  <P>
 *  The owning class must take responsibility of filling in any pre-existing
 *  information, probably by loading each and every WikiPage and calling this
 *  class to update the references when created.
 *
 *  @author ebu@memecry.net
 *  @since 1.6.1
 */

// FIXME: The way that we save attributes is now a major booboo, and must be
//        replace forthwith.  However, this is a workaround for the great deal
//        of problems that occur here...

public class ReferenceManager
    extends BasicPageFilter
    implements InternalModule
{
    /** Maps page wikiname to a Collection of pages it refers to. The Collection 
     *  must contain Strings. The Collection may contain names of non-existing
     *  pages.
     */
    private Map            m_refersTo;
    private Map            m_unmutableRefersTo;
    
    /** Maps page wikiname to a Set of referring pages. The Set must
     *  contain Strings. Non-existing pages (a reference exists, but not a file
     *  for the page contents) may have an empty Set in m_referredBy.
     */
    private Map            m_referredBy;
    private Map            m_unmutableReferredBy;
    
    /** The WikiEngine that owns this object. */
    private WikiEngine     m_engine;

    private boolean        m_matchEnglishPlurals = false;
    
    private static Logger log = Logger.getLogger(ReferenceManager.class);

    private static final String SERIALIZATION_FILE = "refmgr.ser";
    private static final String SERIALIZATION_DIR  = "refmgr-attr";
    
    /** We use this also a generic serialization id */
    private static final long serialVersionUID = 2L;
    
    /**
     *  Builds a new ReferenceManager.
     *
     *  @param engine The WikiEngine to which this is managing references to.
     */
    public ReferenceManager( WikiEngine engine )
    {
        m_refersTo   = new HashMap();
        m_referredBy = new HashMap();
        m_engine = engine;

        m_matchEnglishPlurals = TextUtil.getBooleanProperty( engine.getWikiProperties(),
                                                             WikiEngine.PROP_MATCHPLURALS, 
                                                             m_matchEnglishPlurals );

        //
        //  Create two maps that contain unmutable versions of the two basic maps.
        //
        m_unmutableReferredBy = Collections.unmodifiableMap( m_referredBy );
        m_unmutableRefersTo   = Collections.unmodifiableMap( m_refersTo );
    }

    /**
     *  Does a full reference update.  Does not sync; assumes that you do it afterwards.
     */
    private void updatePageReferences( WikiPage page )
        throws ProviderException
    {
        String content = m_engine.getPageManager().getPageText( page.getName(), 
                                                                WikiPageProvider.LATEST_VERSION );
        
        TreeSet res = new TreeSet();
        Collection links = m_engine.scanWikiLinks( page, content );
        
        res.addAll( links );
        Collection attachments = m_engine.getAttachmentManager().listAttachments( page );

        for( Iterator atti = attachments.iterator(); atti.hasNext(); )
        {
            res.add( ((Attachment)(atti.next())).getName() );
        }

        internalUpdateReferences( page.getName(), res );
    }

    /**
     *  Initializes the entire reference manager with the initial set of pages
     *  from the collection.
     *
     *  @param pages A collection of all pages you want to be included in the reference
     *               count.
     *  @since 2.2
     */
    public void initialize( Collection pages )
        throws ProviderException
    {
        log.debug( "Initializing new ReferenceManager with "+pages.size()+" initial pages." );
        StopWatch sw = new StopWatch();
        sw.start();
        log.info( "Starting cross reference scan of WikiPages" );

        //
        //  First, try to serialize old data from disk.  If that fails,
        //  we'll go and update the entire reference lists (which'll take
        //  time)
        //
        try
        {
            //
            //  Unserialize things.  The loop below cannot be combined with
            //  the other loop below, simply because engine.getPage() has
            //  side effects such as loading initializing the user databases,
            //  which in turn want all of the pages to be read already...
            //
            //  Yes, this is a kludge.  We know.  Will be fixed.
            //
            long saved = unserializeFromDisk();

            for( Iterator it = pages.iterator(); it.hasNext(); )
            {
                WikiPage page = (WikiPage) it.next();
                
                unserializeAttrsFromDisk( page );
            }
            
            //
            //  Now we must check if any of the pages have been changed
            //  while we were in the electronic la-la-land, and update
            //  the references for them.
            //
            
            Iterator it = pages.iterator();
            
            while( it.hasNext() )
            {
                WikiPage page = (WikiPage) it.next();

                if( page instanceof Attachment )
                {
                    // Skip attachments
                }
                else
                {

                    // Refresh with the latest copy
                    page = m_engine.getPage( page.getName() );
                    
                    if( page.getLastModified() == null )
                    {
                        log.fatal( "Provider returns null lastModified.  Please submit a bug report." );
                    }
                    else if( page.getLastModified().getTime() > saved )
                    {
                        updatePageReferences( page );
                    }
                }
            }
            
        }
        catch( Exception e )
        {
            log.info("Unable to unserialize old refmgr information, rebuilding database: "+e.getMessage());
            buildKeyLists( pages );

            // Scan the existing pages from disk and update references in the manager.
            Iterator it = pages.iterator();
            while( it.hasNext() )
            {
                WikiPage page  = (WikiPage)it.next();

                if( page instanceof Attachment )
                {
                    // We cannot build a reference list from the contents
                    // of attachments, so we skip them.
                }
                else
                {
                    updatePageReferences( page );

                    serializeAttrsToDisk( page );
                }
                
            }

            serializeToDisk();
        }

        sw.stop();
        log.info( "Cross reference scan done in "+sw );
    }

    /**
     *  Reads the serialized data from the disk back to memory.
     *  Returns the date when the data was last written on disk
     */
    private synchronized long unserializeFromDisk()
        throws IOException,
               ClassNotFoundException
    {
        ObjectInputStream in = null;
        long saved = 0L;

        try
        {
            StopWatch sw = new StopWatch();
            sw.start();
            
            File f = new File( m_engine.getWorkDir(), SERIALIZATION_FILE );

            in = new ObjectInputStream( new BufferedInputStream(new FileInputStream(f)) );

            long ver     = in.readLong();
            
            if( ver != serialVersionUID )
            {
                throw new IOException("File format has changed; I need to recalculate references.");
            }
            
            saved        = in.readLong();
            m_refersTo   = (Map) in.readObject();
            m_referredBy = (Map) in.readObject();

            in.close();

            m_unmutableReferredBy = Collections.unmodifiableMap( m_referredBy );
            m_unmutableRefersTo   = Collections.unmodifiableMap( m_refersTo );
            
            sw.stop();
            log.debug("Read serialized data successfully in "+sw);
        }
        finally
        {
            try {
                if( in != null ) in.close();
            } catch( IOException ex ) {}
        }

        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeToDisk()
    {
        ObjectOutputStream out = null;

        try
        {
            StopWatch sw = new StopWatch();
            sw.start();
            
            File f = new File( m_engine.getWorkDir(), SERIALIZATION_FILE );

            out = new ObjectOutputStream( new BufferedOutputStream(new FileOutputStream(f)) );

            out.writeLong( serialVersionUID );
            out.writeLong( System.currentTimeMillis() ); // Timestamp
            out.writeObject( m_refersTo );
            out.writeObject( m_referredBy );

            out.close();

            sw.stop();
            
            log.debug("serialization done - took "+sw);
        }
        catch( IOException e )
        {
            log.error("Unable to serialize!");

            try {
                if( out != null ) out.close();
            } catch( IOException ex ) {}
        }
    }

    private String getHashFileName( String pageName ) 
        throws NoSuchAlgorithmException
    {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        
        byte[] dig;
        try
        {
            dig = digest.digest( pageName.getBytes("UTF-8") );
        }
        catch (UnsupportedEncodingException e)
        {
            throw new InternalWikiException("AAAAGH!  UTF-8 is gone!  My eyes!  It burns...!");
        }
        
        return TextUtil.toHexString(dig)+".cache";
    }
    
    /**
     *  Reads the serialized data from the disk back to memory.
     *  Returns the date when the data was last written on disk
     */
    private synchronized long unserializeAttrsFromDisk(WikiPage p)
        throws IOException,
               ClassNotFoundException
    {
        ObjectInputStream in = null;
        long saved = 0L;

        try
        {
            StopWatch sw = new StopWatch();
            sw.start();
            
            //
            //  Find attribute cache, and check if it exists
            //
            File f = new File( m_engine.getWorkDir(), SERIALIZATION_DIR );

            f = new File( f, getHashFileName(p.getName()) );

            if( !f.exists() ) 
            {
                return 0L;
            }
            
            log.debug("Deserializing attributes for "+p.getName());
            
            in = new ObjectInputStream( new BufferedInputStream(new FileInputStream(f)) );

            long ver     = in.readLong();
            
            if( ver != serialVersionUID )
            {
                log.debug("File format has changed; cannot deserialize.");
                return 0L;
            }
            
            saved        = in.readLong();

            String name  = in.readUTF();
            
            if( !name.equals(p.getName()) ) 
            {
                log.debug("File name does not match ("+name+"), skipping...");
                return 0L; // Not here
            }
            
            long entries = in.readLong();
            
            for( int i = 0; i < entries; i++ )
            {
                String key   = in.readUTF();
                Object value = in.readObject();
                
                p.setAttribute( key, value );
                
                log.debug("   attr: "+key+"="+value);
            }
            
            in.close();

            sw.stop();
            log.debug("Read serialized data for "+name+" successfully in "+sw);
        }
        catch( NoSuchAlgorithmException e )
        {
            log.fatal("No MD5!?!");
        }
        finally
        {
            try {
                if( in != null ) in.close();
            } catch( IOException ex ) {}
        }

        return saved;
    }

    /**
     *  Serializes hashmaps to disk.  The format is private, don't touch it.
     */
    private synchronized void serializeAttrsToDisk( WikiPage p )
    {
        ObjectOutputStream out = null;

        try
        {
            // FIXME: There is a concurrency issue here...
            Set entries = p.getAttributes().entrySet();

            if( entries.size() == 0 ) return;
            
            StopWatch sw = new StopWatch();
            sw.start();
            
            File f = new File( m_engine.getWorkDir(), SERIALIZATION_DIR );

            if( !f.exists() ) f.mkdirs();
            
            //
            //  Create a digest for the name
            //
            f = new File( f, getHashFileName(p.getName()) );
            
            out = new ObjectOutputStream( new BufferedOutputStream(new FileOutputStream(f)) );

            out.writeLong( serialVersionUID );
            out.writeLong( System.currentTimeMillis() ); // Timestamp

            out.writeUTF( p.getName() );
            out.writeLong( entries.size() );
            
            for( Iterator i = entries.iterator(); i.hasNext(); )
            {
                Map.Entry e = (Map.Entry) i.next();
                
                if( e.getValue() instanceof Serializable )
                {
                    out.writeUTF( (String)e.getKey() );
                    out.writeObject( e.getValue() );
                }
            }
            
            out.close();

            sw.stop();
            
            log.debug("serialization for "+p.getName()+" done - took "+sw);
        }
        catch( IOException e )
        {
            log.error("Unable to serialize!");

            try {
                if( out != null ) out.close();
            } catch( IOException ex ) {}
        }
        catch( NoSuchAlgorithmException e )
        {
            log.fatal("No MD5 algorithm!?!");
        }
    }

    /**
     *  After the page has been saved, updates the reference lists.
     */
    public void postSave( WikiContext context, String content )
    {
        WikiPage page = context.getPage();

        updateReferences( page.getName(),
                          context.getEngine().scanWikiLinks( page, content ) );
        
        serializeAttrsToDisk( page );
    }
    
    /**
     * Updates the m_referedTo and m_referredBy hashmaps when a page has been
     * deleted.
     * <P>
     * Within the m_refersTo map the pagename is a key. The whole key-value-set
     * has to be removed to keep the map clean.
     * Within the m_referredBy map the name is stored as a value. Since a key 
     * can have more than one value we have to delete just the key-value-pair
     * referring page:deleted page.
     * 
     *  @param page Name of the page to remove from the maps.
     */
    public synchronized void pageRemoved( WikiPage page )
    {
        String pageName = page.getName();
        
        Collection refTo = (Collection)m_refersTo.get( pageName );
        
        if( refTo != null )
        {
            Iterator it_refTo = refTo.iterator();
            while( it_refTo.hasNext() )
            {
                String referredPageName = (String)it_refTo.next();
                Set refBy = (Set)m_referredBy.get( referredPageName );

                if( refBy == null )
                    throw new InternalWikiException("Refmgr out of sync: page "+pageName+" refers to "+referredPageName+", which has null referrers.");

                refBy.remove(pageName);
            
                m_referredBy.remove( referredPageName );
            
                // We won't put it back again if it becomes empty and does not exist.  It will be added
                // later on anyway, if it becomes referenced again.
                if( !(refBy.isEmpty() && !m_engine.pageExists(referredPageName)) )
                {
                    m_referredBy.put( referredPageName, refBy );
                }
            }
            
            log.debug("Removing from m_refersTo HashMap key:value "+pageName+":"+m_refersTo.get( pageName ));
            m_refersTo.remove( pageName );
        }

        Set refBy = (Set) m_referredBy.get( pageName );
        if( refBy == null || refBy.isEmpty() )
        {
            m_referredBy.remove( pageName );
        }
        
        serializeToDisk();
    }
    
    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <P>
     *  This is the method to call when a new page has been created and we 
     *  want to a) set up its references and b) notify the referred pages
     *  of the references. Use this method during run-time.
     *
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */
    public synchronized void updateReferences( String page, Collection references )
    {
        internalUpdateReferences(page, references);
        
        serializeToDisk();
    }

    /**
     *  Updates the referred pages of a new or edited WikiPage. If a refersTo
     *  entry for this page already exists, it is removed and a new one is built
     *  from scratch. Also calls updateReferredBy() for each referenced page.
     *  <p>
     *  This method does not synchronize the database to disk.
     *  
     *  @param page Name of the page to update.
     *  @param references A Collection of Strings, each one pointing to a page this page references.
     */

    private void internalUpdateReferences(String page, Collection references)
    {
        page = getFinalPageName( page );
        
        //
        // Create a new entry in m_refersTo.
        //
        Collection oldRefTo = (Collection)m_refersTo.get( page );
        m_refersTo.remove( page );
        
        TreeSet cleanedRefs = new TreeSet();
        for( Iterator i = references.iterator(); i.hasNext(); )
        {
            String ref = (String)i.next();
            
            ref = getFinalPageName( ref );
            
            cleanedRefs.add( ref );
        }
        
        m_refersTo.put( page, cleanedRefs );

        //
        //  We know the page exists, since it's making references somewhere.
        //  If an entry for it didn't exist previously in m_referredBy, make 
        //  sure one is added now.
        //
        if( !m_referredBy.containsKey( page ) )
        {
            m_referredBy.put( page, new TreeSet() );
        }

        //
        //  Get all pages that used to be referred to by 'page' and
        //  remove that reference. (We don't want to try to figure out
        //  which particular references were removed...)
        //
        cleanReferredBy( page, oldRefTo, cleanedRefs );

        // 
        //  Notify all referred pages of their referinesshoodicity.
        //
        Iterator it = cleanedRefs.iterator();
        while( it.hasNext() )
        {
            String referredPageName = (String)it.next();
            updateReferredBy( getFinalPageName(referredPageName), page );
        }
    }

    /**
     * Returns the refers-to list. For debugging.
     */
    protected Map getRefersTo()
    {
        return( m_refersTo );
    }

    /**
     * Returns the referred-by list. For debugging.
     */
    protected Map getReferredBy()
    {
        return( m_referredBy );
    }

    /**
     * Cleans the 'referred by' list, removing references by 'referrer' to
     * any other page. Called after 'referrer' is removed. 
     */
    private void cleanReferredBy( String referrer, 
                                  Collection oldReferred,
                                  Collection newReferred )
    {
        // Two ways to go about this. One is to look up all pages previously
        // referred by referrer and remove referrer from their lists, and let
        // the update put them back in (except possibly removed ones). 
        // The other is to get the old referred to list, compare to the new, 
        // and tell the ones missing in the latter to remove referrer from
        // their list. Hm. We'll just try the first for now. Need to come
        // back and optimize this a bit.

        if( oldReferred == null )
            return;

        Iterator it = oldReferred.iterator();
        while( it.hasNext() )
        {
            String referredPage = (String)it.next();
            Set oldRefBy = (Set)m_referredBy.get( referredPage );
            if( oldRefBy != null )
            {
                oldRefBy.remove( referrer );
            }

            // If the page is referred to by no one AND it doesn't even
            // exist, we might just as well forget about this entry.
            // It will be added again elsewhere if new references appear.
            if( ( ( oldRefBy == null ) || ( oldRefBy.isEmpty() ) ) &&
                ( m_engine.pageExists( referredPage ) == false ) )
            {
                m_referredBy.remove( referredPage );
            }
        }
        
    }


    /**
     *  When initially building a ReferenceManager from scratch, call this method
     * BEFORE calling updateReferences() with a full list of existing page names.
     * It builds the refersTo and referredBy key lists, thus enabling 
     * updateReferences() to function correctly.
     * <P>
     * This method should NEVER be called after initialization. It clears all mappings 
     * from the reference tables.
     * 
     * @param pages   a Collection containing WikiPage objects.
     */
    private synchronized void buildKeyLists( Collection pages )
    {
        m_refersTo.clear();
        m_referredBy.clear();

        if( pages == null )
            return;

        Iterator it = pages.iterator();
        try
        {
            while( it.hasNext() )
            {
                WikiPage page = (WikiPage)it.next();
                // We add a non-null entry to referredBy to indicate the referred page exists
                m_referredBy.put( page.getName(), new TreeSet() );
                // Just add a key to refersTo; the keys need to be in sync with referredBy.
                m_refersTo.put( page.getName(), null );
            }
        }
        catch( ClassCastException e )
        {
            log.fatal( "Invalid collection entry in ReferenceManager.buildKeyLists().", e );
        }
    }


    /**
     * Marks the page as referred to by the referrer. If the page does not
     * exist previously, nothing is done. (This means that some page, somewhere,
     * has a link to a page that does not exist.)
     * <P>
     * This method is NOT synchronized. It should only be referred to from
     * within a synchronized method, or it should be made synced if necessary.       
     */
    private void updateReferredBy( String page, String referrer )
    {
        // We're not really interested in first level self-references.
        if( page.equals( referrer ) )
        {
            return;
        }

        // Neither are we interested if plural forms refer to each other.
        if( m_matchEnglishPlurals )
        {
            String p2 = page.endsWith("s") ? page.substring(0,page.length()-1) : page+"s";
            
            if( referrer.equals(p2) )
            {
                return;
            }
        }
        
        Set referrers = (Set)m_referredBy.get( page );

        // Even if 'page' has not been created yet, it can still be referenced.
        // This requires we don't use m_referredBy keys when looking up missing
        // pages, of course. 
        if(referrers == null)
        {
            referrers = new TreeSet();
            m_referredBy.put( page, referrers );
        }
        referrers.add( referrer );
    }

    
    /**
     * Clears the references to a certain page so it's no longer in the map.
     *
     * @param pagename  Name of the page to clear references for.
     */
    public synchronized void clearPageEntries( String pagename )
    {
        m_referredBy.remove( getFinalPageName(pagename) );
    }


    /** 
     *  Finds all unreferenced pages. This requires a linear scan through
     *  m_referredBy to locate keys with null or empty values.
     */
    public synchronized Collection findUnreferenced()
    {
        ArrayList unref = new ArrayList();

        Set keys = m_referredBy.keySet();
        Iterator it = keys.iterator();

        while( it.hasNext() )
        {
            String key = (String) it.next();
            //Set refs = (Set) m_referredBy.get( key );
            Set refs = getReferenceList( m_referredBy, key );
            if( refs == null || refs.isEmpty() )
            {
                unref.add( key );
            }
        }

        return unref;
    }

    
    /**
     * Finds all references to non-existant pages. This requires a linear
     * scan through m_refersTo values; each value must have a corresponding
     * key entry in the reference Maps, otherwise such a page has never
     * been created. 
     * <P>
     * Returns a Collection containing Strings of unreferenced page names.
     * Each non-existant page name is shown only once - we don't return information
     * on who referred to it.
     */
    public synchronized Collection findUncreated()
    {
        TreeSet uncreated = new TreeSet();

        // Go through m_refersTo values and check that m_refersTo has the corresponding keys.
        // We want to reread the code to make sure our HashMaps are in sync...

        Collection allReferences = m_refersTo.values();
        Iterator it = allReferences.iterator();

        while( it.hasNext() )
        {
            Collection refs = (Collection)it.next();

            if( refs != null )
            {
                Iterator rit = refs.iterator();

                while( rit.hasNext() )
                {
                    String aReference = (String)rit.next();

                    if( m_engine.pageExists( aReference ) == false )
                    {
                        uncreated.add( aReference );
                    }
                }
            }
        }

        return uncreated;
    }

    /**
     *  Searches for the given page in the given Map.
     */
    private Set getReferenceList( Map coll, String pagename )
    {
        Set refs = (Set)coll.get( pagename );
        
        if( m_matchEnglishPlurals )
        {
            //
            //  We'll add also matches from the "other" page.
            //
            Set refs2;
            
            if( pagename.endsWith("s") )
            {
                refs2 = (Set)coll.get( pagename.substring(0,pagename.length()-1) );
            }
            else
            {
                refs2 = (Set)coll.get( pagename+"s" );
            }
            
            if( refs2 != null )
            {
                if( refs != null )
                    refs.addAll( refs2 );
                else
                    refs = refs2;
            }
        }
        return refs;
    }

    /**
     * Find all pages that refer to this page. Returns null if the page
     * does not exist or is not referenced at all, otherwise returns a 
     * collection containing page names (String) that refer to this one.
     * <p>
     * @param pagename The page to find referrers for.
     * @return A Collection of Strings.  (This is, in fact, a Set, and is likely
     *         to change at some point to a Set).  May return null, if the page
     *         does not exist, or if it has no references.
     */
    // FIXME: Return a Set instead of a Collection.
    public synchronized Collection findReferrers( String pagename )
    {
        Set refs = getReferenceList( m_referredBy, pagename );

        if( refs == null || refs.isEmpty() )
        {
            return null;
        }
        
        return refs;
       
    }

    /**
     *  Returns all pages that refer to this page.  Note that this method
     *  returns an unmodifiable Map, which may be abruptly changed.  So any
     *  access to any iterator may result in a ConcurrentModificationException.
     *  <p>
     *  The advantages of using this method over findReferrers() is that
     *  it is very fast, as it does not create a new object.  The disadvantages
     *  are that it does not do any mapping between plural names, and you
     *  may end up getting a ConcurrentModificationException.
     *  
     * @param pageName Page name to query.
     * @return A Set of Strings containing the names of all the pages that refer
     *         to this page.  May return null, if the page does not exist or
     *         has not been indexed yet.
     * @since 2.2.33
     */
    public Set findReferredBy( String pageName )
    {
        return (Set)m_unmutableReferredBy.get( getFinalPageName(pageName) );
    }
    
    /**
     *  Returns all pages that this page refers to.  You can use this as a quick
     *  way of getting the links from a page, but note that it does not link any
     *  InterWiki, image, or external links.  It does contain attachments, though.
     *  <p>
     *  The Collection returned is unmutable, so you cannot change it.  It does reflect
     *  the current status and thus is a live object.  So, if you are using any
     *  kind of an iterator on it, be prepared for ConcurrentModificationExceptions. 
     *  <p>
     *  The returned value is a Collection, because a page may refer to another page
     *  multiple times.
     *  
     * @param pageName Page name to query
     * @return A Collection of Strings containing the names of the pages that this page
     *         refers to. May return null, if the page does not exist or has not
     *         been indexed yet.
     * @since 2.2.33
     */
    public Collection findRefersTo( String pageName )
    {
        return (Collection)m_unmutableRefersTo.get( getFinalPageName(pageName) );
    }
    
    /**
     * This 'deepHashCode' can be used to determine if there were any
     * modifications made to the underlying to and by maps of the
     * ReferenceManager. The maps of the ReferenceManager are not
     * synchronized, so someone could add/remove entries in them while the
     * hashCode is being computed.
     * 
     * @return Sum of the hashCodes for the to and by maps of the
     *         ReferenceManager
     * @since 2.3.24
     */
    /*
       This method traps and retries if a concurrent
       modifcaition occurs.
       TODO: It is unnecessary to calculate the hashcode; it should be calculated only
             when the hashmaps are changed.  This is slow. 
    */ 
    public int deepHashCode()
    {
        boolean failed = true;
        int signature = 0;
        
        while (failed)
        {
            signature = 0;
            try
            {
                signature ^= m_referredBy.hashCode();
                signature ^= m_refersTo.hashCode();
                failed = false;
            }
            catch (ConcurrentModificationException e)
            {
                Thread.yield();
            }
        }
        
        return signature;
    }
    
    /**
     *  Returns a list of all pages that the ReferenceManager knows about. 
     *  This should be roughly equivalent to PageManager.getAllPages(), but without
     *  the potential disk access overhead.  Note that this method is not guaranteed
     *  to return a Set of really all pages (especially during startup), but it is
     *  very fast.
     * 
     *  @return A Set of all defined page names that ReferenceManager knows about.
     *  @since 2.3.24
     */
    public Set findCreated()
    {
        return new HashSet( m_refersTo.keySet() );
    }
    
    private String getFinalPageName( String orig )
    {
        try
        {
            String s = m_engine.getFinalPageName( orig );
            
            if( s == null ) s = orig;
            
            return s;
        }
        catch( ProviderException e )
        {
            log.error("Error while trying to fetch a page name; trying to cope with the situation.",e);
            
            return orig;
        }
    }
}
