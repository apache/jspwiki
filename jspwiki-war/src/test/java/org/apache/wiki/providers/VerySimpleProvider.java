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
package org.apache.wiki.providers;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.search.QueryItem;

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
    
    private WikiEngine m_engine;

    public void initialize( WikiEngine engine, Properties props )
    {
        m_engine = engine;
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
     *  Always returns true.
     */
    public boolean pageExists( String page, int version )
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

        WikiPage p = new WikiPage( m_engine, page );
        p.setVersion( 5 );
        p.setAuthor( AUTHOR );
        p.setLastModified( new Date(0L) );
        return p;
    }

    /**
     *  Returns a single page.
     */
    public Collection getAllPages()
    {
        Vector<WikiPage> v = new Vector<WikiPage>();
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

    /* (non-Javadoc)
     * @see org.apache.wiki.providers.WikiPageProvider#movePage(java.lang.String, java.lang.String)
     */
    public void movePage( String from, String to ) throws ProviderException
    {
        // TODO Auto-generated method stub
        
    }
    
}
