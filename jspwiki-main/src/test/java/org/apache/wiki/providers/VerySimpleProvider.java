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

import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.search.SearchResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

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
    
    private Engine m_engine;

    @Override
    public void initialize( final Engine engine, final Properties props )
    {
        m_engine = engine;
    }

    @Override
    public String getProviderInfo()
    {
        return "Very Simple Provider.";
    }

    @Override
    public void putPageText( final WikiPage page, final String text )
        throws ProviderException
    {
    }

    /**
     *  Always returns true.
     */
    @Override
    public boolean pageExists( final String page )
    {
        return true;
    }

    /**
     *  Always returns true.
     */
    @Override
    public boolean pageExists( final String page, final int version )
    {
        return true;
    }

    /**
     *  Always returns null.
     */
    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query )
    {
        return null;
    }

    /**
     *  Returns always a valid WikiPage.
     */
    @Override
    public WikiPage getPageInfo( final String page, final int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        final WikiPage p = new WikiPage( m_engine, page );
        p.setVersion( 5 );
        p.setAuthor( AUTHOR );
        p.setLastModified( new Date(0L) );
        return p;
    }

    /**
     *  Returns a single page.
     */
    @Override
    public Collection< WikiPage > getAllPages()
    {
        final List< WikiPage > l = new ArrayList<>();
        l.add( getPageInfo( PAGENAME, 5 ) );
        return l;
    }

    /**
     *  Returns the same as getAllPages().
     */
    @Override
    public Collection< WikiPage > getAllChangedSince( final Date date )
    {
        return getAllPages();
    }

    /**
     *  Always returns 1.
     */
    @Override
    public int getPageCount()
    {
        return 1;
    }

    /**
     *  Always returns an empty list.
     */
    @Override
    public List< WikiPage > getVersionHistory( final String page )
    {
        return new Vector<>();
    }

    /**
     *  Stores the page and version into public fields of this class,
     *  then returns an empty string.
     */
    @Override
    public String getPageText( final String page, final int version )
    {
        m_latestReq  = page;
        m_latestVers = version;

        return "";
    }

    @Override
    public void deleteVersion( final String page, final int version )
    {
    }

    @Override
    public void deletePage( final String page )
    {
    }

    /* (non-Javadoc)
     * @see org.apache.wiki.providers.WikiPageProvider#movePage(java.lang.String, java.lang.String)
     */
    @Override
    public void movePage( final String from, final String to ) throws ProviderException
    {
        // TODO Auto-generated method stub
        
    }
    
}
