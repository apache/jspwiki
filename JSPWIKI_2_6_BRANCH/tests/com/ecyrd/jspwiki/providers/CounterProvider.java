/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

    WikiPage[]    m_pages         = new WikiPage[0];
    
    String m_defaultText = "[Foo], [Bar], [Blat], [Blah]";


    public void initialize( WikiEngine engine, Properties props )
    {
        m_pages = new WikiPage[]
                  { new WikiPage(engine, "Foo"),
                    new WikiPage(engine, "Bar"),
                    new WikiPage(engine, "Blat"),
                    new WikiPage(engine, "Blaa") };
        
        m_initCalls++;
        
        for( int i = 0; i < m_pages.length; i++ ) 
        {
            m_pages[i].setAuthor("Unknown");
            m_pages[i].setLastModified( new Date(0L) );
            m_pages[i].setVersion(1);
        }
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
        return m_defaultText;
    }

    public void deleteVersion( String page, int version )
    {
    }

    public void deletePage( String page )
    {
    }

    /* (non-Javadoc)
     * @see com.ecyrd.jspwiki.providers.WikiPageProvider#movePage(java.lang.String, java.lang.String)
     */
    public void movePage( String from, String to ) throws ProviderException
    {
        // TODO Auto-generated method stub
        
    }
    
    
}
