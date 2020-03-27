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

import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

/**
 *  A provider who counts the hits to different parts.
 */
public class CounterProvider implements PageProvider {

    public int m_getPageCalls     = 0;
    public int m_pageExistsCalls  = 0;
    public int m_getPageTextCalls = 0;
    public int m_getAllPagesCalls = 0;
    public int m_initCalls        = 0;

    Page[]    m_pages         = new Page[0];
    
    String m_defaultText = "[Foo], [Bar], [Blat], [Blah]";


    @Override
    public void initialize( final Engine engine, final Properties props ) {
        m_pages = new Page[]
                  { Wiki.contents().page( engine, "Foo" ),
                    Wiki.contents().page( engine, "Bar" ),
                    Wiki.contents().page( engine, "Blat" ),
                    Wiki.contents().page( engine, "Blaa" ) };
        
        m_initCalls++;

        for( final Page m_page : m_pages ) {
            m_page.setAuthor( "Unknown" );
            m_page.setLastModified( new Date( 0L ) );
            m_page.setVersion( 1 );
        }
    }

    @Override
    public String getProviderInfo()
    {
        return "Very Simple Provider.";
    }

    @Override
    public void putPageText( final Page page, final String text ) throws ProviderException {
    }

    @Override
    public boolean pageExists( final String page ) {
        m_pageExistsCalls++;

        return findPage( page ) != null;
    }

    @Override
    public boolean pageExists( final String page, final int version )
    {
        return pageExists (page);
    }

    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query )
    {
        return null;
    }

    private Page findPage( final String page ) {
        for( final Page m_page : m_pages ) {
            if( m_page.getName().equals( page ) ) {
                return m_page;
            }
        }

        return null;
    }

    @Override
    public Page getPageInfo( final String page, final int version ) {
        m_getPageCalls++;
        return findPage(page);
    }

    @Override
    public Collection< Page > getAllPages() {
        m_getAllPagesCalls++;
        final List<Page> l = new ArrayList<>();
        Collections.addAll( l, m_pages );

        return l;
    }

    @Override
    public Collection< Page > getAllChangedSince( final Date date )
    {
        return new ArrayList<>();
    }

    @Override
    public int getPageCount()
    {
        return m_pages.length;
    }

    @Override
    public List< Page > getVersionHistory( final String page )
    {
        return new Vector<>();
    }

    @Override
    public String getPageText( final String page, final int version ) {
        m_getPageTextCalls++;
        return m_defaultText;
    }

    @Override
    public void deleteVersion( final String page, final int version ) {
    }

    @Override
    public void deletePage( final String page ) {
    }

    @Override
    public void movePage( final String from, final String to ) throws ProviderException {
    }

}
