package com.ecyrd.jspwiki.providers;

import java.util.*;
import com.ecyrd.jspwiki.*;

/**
 *  This is a simple provider that is used by some of the tests.  It has some
 *  specific behaviours, like it always contains a single page.
 */
public class VerySimpleProvider implements WikiPageProvider
{
    /** The last request is stored here. */
    public String m_latestReq = null;
    /** The version number of the last request is stored here. */
    public int    m_latestVers = -123989;

    /**
     *  This provider has only a single page, when you ask 
     *  a list of all pages.
     */
    public static final String PAGENAME = "foo";

    /**
     *  The name of the page list.
     */
    public static final String AUTHOR   = "default-author";

    public void initialize( Properties props )
    {
    }

    public String getProviderInfo()
    {
        return "Very Simple Provider.";
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
    }

    /**
     *  Always returns true.
     */
    public boolean pageExists( String page )
    {
        return true;
    }

    /**
     *  Always returns null.
     */
    public Collection findPages( QueryItem[] query )
    {
        return null;
    }

    /**
     *  Returns always a valid WikiPage.
     */
    public WikiPage getPageInfo( String page, int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        WikiPage p = new WikiPage( page );
        p.setVersion( 5 );
        p.setAuthor( AUTHOR );
        return p;
    }

    /**
     *  Returns a single page.
     */
    public Collection getAllPages()
    {
        Vector v = new Vector();
        v.add( getPageInfo( PAGENAME, 5 ) );
        return v;
    }

    /**
     *  Returns the same as getAllPages().
     */
    public Collection getAllChangedSince( Date date )
    {
        return getAllPages();
    }

    /**
     *  Always returns 1.
     */
    public int getPageCount()
    {
        return 1;
    }

    /**
     *  Always returns an empty list.
     */
    public List getVersionHistory( String page )
    {
        return new Vector();
    }

    /**
     *  Stores the page and version into public fields of this class,
     *  then returns an empty string.
     */
    public String getPageText( String page, int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        return "";
    }

    public void deleteVersion( String page, int version )
    {
    }

    public void deletePage( String page )
    {
    }
}
