/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.util.*;
import org.apache.log4j.*;


/*
  BUGS

  - if a wikilink is added to a page, then removed, RefMan still thinks that
    the page refers to the wikilink page. Hm.
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
   Keeps track of wikipage references:
   <UL>
   <LI>What pages a given page refers to
   <LI>What pages refer to a given page
   </UL>

   This is a quick'n'dirty approach without any finesse in storage and
   searching algorithms; we trust java.util.*.

   This class contains two HashMaps, m_refersTo and m_referredBy. The 
   first is indexed by WikiPage names and contains a Collection of all
   WikiPages the page refers to. (Multiple references are not counted,
   naturally.) The second is indexed by WikiPage names and contains 
   a HashSet of all pages that refer to the indexing page. (Notice - 
   the keys of both HashMaps should be kept in sync.)

   When a page is added or edited, its references are parsed, a Collection
   is received, and we crudely replace anything previous with this new
   Collection. We then check each referenced page name and make sure they
   know they are referred to by the new page.

   Based on this information, we can perform non-optimal searches for 
   e.g. unreferenced pages, top ten lists, etc.

   The owning class must take responsibility of filling in any pre-existing
   information, probably by loading each and every WikiPage and calling this
   class to update the references when created.

   @author ebu@memecry.net
*/
public class ReferenceManager
{
    private HashMap m_refersTo;
    private HashMap m_referredBy;
    private WikiEngine m_engine;

    /**
       Builds a new ReferenceManager with default (null) entries for
       the WikiPages contained in the pages Collection. (This collection
       must be given for subsequent updateReferences() calls to work.)
       <P>
       The collection should contain an entry for all currently existing WikiPages.

       @param pages   a Collection of WikiPages 
    */
    public ReferenceManager( WikiEngine engine, Collection pages )
    {
        m_refersTo = new HashMap();
        m_referredBy = new HashMap();
        m_engine = engine;

        buildKeyLists( pages );
    }

    
    /**
       Updates the referred pages of a new or edited WikiPage. If a refersTo
       entry for this page already exists, it is removed and a new one is built
       from scratch. Also calls updateReferredBy() for each referenced page.
       <P>
       This is the method to call when a new page has been created and we 
       want to a) set up its references and b) notify the referred pages
       of the references. Use this method during run-time.
    */
    public synchronized void updateReferences( String page, Collection references )
    {
        // Create a new entry in m_refersTo.
        Collection oldRefTo = (Collection)m_refersTo.get( page );
        m_refersTo.remove( page );
        m_refersTo.put( page, references );

        // We know the page exists, since it's making references somewhere.
        // If an entry for it didn't exist previously in m_referredBy, make 
        // sure one is added now.
        if( !m_referredBy.containsKey( page ) )
            m_referredBy.put( page, new HashSet() );

        // Get all pages that used to be referred to by 'page' and
        // remove that reference. (We don't want to try to figure out
        // which particular references were removed...)
        cleanReferredBy( page, oldRefTo, references );

        // Notify all referred pages of their referinesshoodicity.
        Iterator it = references.iterator();
        while( it.hasNext() )
        {
            String referredPageName = (String)it.next();
            updateReferredBy( referredPageName, page );
        }

        //dump();

    }


    /**
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
            HashSet oldRefBy = (HashSet)m_referredBy.get( referredPage );
            if( oldRefBy != null )
            {
                oldRefBy.remove( referrer );
            }
        }
        
    }


    /**
       When initially building a ReferenceManager from scratch, call this method
       BEFORE calling updateReferences() with a full list of existing page names.
       It builds the refersTo and referredBy key lists, thus enabling 
       updateReferences() to function correctly.
       <P>
       This method should NEVER be called after initialization. It clears all mappings 
       from the reference tables.
       
       @param pages   a Collection containing WikiPage objects.
    */
    private synchronized void buildKeyLists( Collection pages )
    {
        m_refersTo.clear();
        m_referredBy.clear();

        Iterator it = pages.iterator();
        try
        {
            while( it.hasNext() )
            {
                WikiPage page = (WikiPage)it.next();
                // We add a non-null entry to referredBy to indicate the referred page exists
                m_referredBy.put( page.getName(), new HashSet() );
                // Just add a key to refersTo; the keys need to be in sync with referredBy.
                m_refersTo.put( page.getName(), null );
            }
        }
        catch( ClassCastException e )
        {
            //log.info( "" );
            System.out.println( "Invalid collection entry in ReferenceManager.buildKeyLists()." );
        }
    }


    /**
       Marks the page as referred to by the referrer. If the page does not
       exist previously, nothing is done. (This means that some page, somewhere,
       has a link to a page that does not exist.)
       <P>
       This method is NOT synchronized. It should only be referred to from
       within a synchronized method, or it should be made synced if necessary.       
     */
    private void updateReferredBy( String page, String referrer )
    {
        // We're not really interested in first level self-references.
        if( page.equals( referrer ) )
            return;
        HashSet referrers = (HashSet)m_referredBy.get( page );
        if( referrers == null )
            return;
        referrers.add( referrer );
    }



    /** 
        Finds all unreferenced pages. This requires a linear scan through
        m_referredBy to locate keys with null or empty values.
    */
    public synchronized Collection findUnreferenced()
    {
        ArrayList unref = new ArrayList();

        Set keys = m_referredBy.keySet();
        Iterator it = keys.iterator();
        while( it.hasNext() )
        {
            String key = (String) it.next();
            HashSet refs = (HashSet) m_referredBy.get( key );
            if( refs == null || refs.isEmpty() )
                unref.add( key );
        }

        return( unref );
    }

    
    /**
       Finds all references to non-existant pages. This requires a linear
       scan through m_refersTo values; each value must have a corresponding
       key entry in the reference HashMaps, otherwise such a page has never
       been created. 
       <P>
       Returns a Collection containing Strings of unreferenced page names.
       Each non-existant page name is shown only once - we don't return information
       on who referred to it.
    */
    public synchronized Collection findUncreated()
    {
        HashSet uncreated = new HashSet();

        // Go through m_refersTo values and check that m_refersTo has the corresponding keys.
        // We want to reread the code to make sure our HashMaps are in sync...

        Collection allReferences = m_refersTo.values();
        Iterator it = allReferences.iterator();
        while( it.hasNext() )
        {
            ArrayList refs = (ArrayList)it.next();
            if( refs != null )
            {
                Iterator rit = refs.iterator();
                while( rit.hasNext() )
                {
                    String aReference = (String)rit.next();
                    if( m_engine.pageExists( aReference ) == false )
                        uncreated.add( aReference );
                }
            }
        }

        return( uncreated );
    }


    /**
       Find all pages that refer to this page. Returns null if the page
       does not exist or is not referenced at all, otherwise returns a 
       collection containint page names (String) that refer to this one.
    */
    public synchronized Collection findReferrers( String pagename )
    {
        HashSet refs = (HashSet)m_referredBy.get( pagename );
        if( refs == null || refs.isEmpty() )
            return( null );
        else
            return( refs );
    }



    /**
       Test method: dumps the contents of our link lists to stdout.
       This method is NOT synchronized, and should be used in testing
       with one user, one WikiEngine only.
    */
    public void dump()
    {
        try
        {
            System.out.println( "================================================================" );
            System.out.println( "Referred By list:" );
            Set keys = m_referredBy.keySet();
            Iterator it = keys.iterator();
            while( it.hasNext() )
            {
                String key = (String) it.next();
                System.out.print( key + " referred by: " );
                HashSet refs = (HashSet)m_referredBy.get( key );
                Iterator rit = refs.iterator();
                while( rit.hasNext() )
                {
                    String aRef = (String)rit.next();
                    System.out.print( aRef + " " );
                }
                System.out.println();
            }
            
            
            System.out.println( "----------------------------------------------------------------" );
            System.out.println( "Refers To list:" );
            keys = m_refersTo.keySet();
            it = keys.iterator();
            while( it.hasNext() )
            {
                String key = (String) it.next();
                System.out.print( key + " refers to: " );
                Collection refs = (Collection)m_refersTo.get( key );
                if(refs != null)
                {
                    Iterator rit = refs.iterator();
                    while( rit.hasNext() )
                    {
                        String aRef = (String)rit.next();
                        System.out.print( aRef + " " );
                    }
                    System.out.println();
                }
                else
                    System.out.println("(no references)");
            }
            System.out.println( "================================================================" );
        }
        catch(Exception e)
        {
            System.out.println("Problem in dump(): " + e.getMessage());
            e.printStackTrace();
        }
    }

}
