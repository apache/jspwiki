package com.ecyrd.jspwiki.providers;

import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.*;

/**
 *  A provider who counts the hits to different parts.
 */
public class CounterProvider
    implements WikiPageProvider
{
    public int m_getPageCalls     = 0;
    public int m_pageExistsCalls  = 0;
    public int m_getPageTextCalls = 0;
    public int m_getAllPagesCalls = 0;
    public int m_initCalls        = 0;

    static Logger log = Logger.getLogger( CounterProvider.class );

    WikiPage[]    m_pages = { new WikiPage("Foo"),
                              new WikiPage("Bar"),
                              new WikiPage("Blat"),
                              new WikiPage("Blaa") };

    String defaultText = "[Foo], [Bar], [Blat], [Blah]";


    public void initialize( WikiEngine engine, Properties props )
    {
        m_initCalls++;
    }

    public String getProviderInfo()
    {
        return "Very Simple Provider.";
    }

    public void putPageText( WikiPage page, String text )
        throws ProviderException
    {
    }

    public boolean pageExists( String page )
    {
        m_pageExistsCalls++;

        //System.out.println("PAGE="+page);
        //TestEngine.trace();

        return findPage( page ) != null;
    }

    public Collection findPages( QueryItem[] query )
    {
        return null;
    }

    private WikiPage findPage( String page )
    {
        for( int i = 0; i < m_pages.length; i++ )
        {
            if( m_pages[i].getName().equals(page) )
                return m_pages[i];
        }

        return null;
    }

    public WikiPage getPageInfo( String page, int version )
    {            
        m_getPageCalls++;

        //System.out.println("GETPAGEINFO="+page);
        //TestEngine.trace();

        WikiPage p = findPage(page);
        p.setVersion( 1 );
        p.setAuthor( "default-author" );
        return p;
    }

    public Collection getAllPages()
    {
        m_getAllPagesCalls++;

        Vector v = new Vector();

        for( int i = 0; i < m_pages.length; i++ )
        {
            v.add( m_pages[i] );
        }

        return v;
    }

    public Collection getAllChangedSince( Date date )
    {
        return new Vector();
    }

    public int getPageCount()
    {
        return m_pages.length;
    }

    public List getVersionHistory( String page )
    {
        return new Vector();
    }

    public String getPageText( String page, int version )
    {
        m_getPageTextCalls++;
        return defaultText;
    }

    public void deleteVersion( String page, int version )
    {
    }

    public void deletePage( String page )
    {
    }
}
