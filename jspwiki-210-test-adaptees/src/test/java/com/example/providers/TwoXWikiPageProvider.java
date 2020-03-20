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
package com.example.providers;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.providers.WikiPageProvider;
import org.apache.wiki.search.QueryItem;
import org.apache.wiki.search.SearchMatcher;
import org.apache.wiki.search.SearchResult;
import org.apache.wiki.search.SearchResultComparator;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class TwoXWikiPageProvider implements WikiPageProvider {

    WikiEngine engine;
    Map< String, List < WikiPage > > pages = new ConcurrentHashMap<>();
    Map< String, List < String > > contents = new ConcurrentHashMap<>();

    /** {@inheritDoc} */
    @Override
    public String getProviderInfo() {
        return this.getClass().getName();
    }

    /** {@inheritDoc} */
    @Override
    public void initialize( final WikiEngine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        this.engine = engine;
        try {
            putPageText( new WikiPage( engine, "page1" ), "blablablabla" );
            putPageText( new WikiPage( engine, "page2" ), "bleblebleble" );
            putPageText( new WikiPage( engine, "page3" ), "blibliblibli" );
        } catch( final ProviderException e ) {
            throw new IOException( e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void putPageText( final WikiPage page, final String text ) throws ProviderException {
        page.setVersion( page.getVersion() + 1 );
        page.setLastModified( new Date() );
        if( pageExists( page.getName() ) ) {
            pages.get( page.getName() ).add( page );
            contents.get( page.getName() ).add( text );
        } else {
            pages.put( page.getName(), new ArrayList<>( Arrays.asList( page ) ) );
            contents.put( page.getName(), new ArrayList<>( Arrays.asList( text ) ) );
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean pageExists( final String page ) {
        return pages.get( page ) != null && contents.get( page ) != null;
    }

    < S > S executeIfPageExists( final String page, final int version, final Supplier< S > s ) throws ProviderException {
        if( pageExists( page, version ) ) {
            try {
                return s.get();
            } catch( final Exception e ) {
                throw new ProviderException( e.getMessage() );
            }
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean pageExists( final String page, final int version ) {
        return pages.get( page ) != null    && pages.get( page ).size() >= version &&
               contents.get( page ) != null && contents.get( page ).size() >= version;
    }

    /** {@inheritDoc} */
    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query ) {
        final TreeSet< SearchResult > res = new TreeSet<>( new SearchResultComparator() );
        final SearchMatcher matcher = new SearchMatcher( engine, query );
        final Map< String, WikiPage > wikipages = pages.entrySet()
                                                       .stream()
                                                       .map( e -> new AbstractMap.SimpleEntry<>( e.getKey(), e.getValue().get( e.getValue().size() - 1 ) ) )
                                                       .collect( Collectors.toMap( AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue ) );
        for( final String wikipage : wikipages.keySet() ) {
            final String pagetext = contents.get( wikipage ).get( contents.get( wikipage ).size() - 1 );
            try {
                final SearchResult comparison = matcher.matchPageContent( wikipage, pagetext );
                if( comparison != null ) {
                    res.add( comparison );
                }
            } catch( final IOException e ) {
                // ok to ignore, shouldn't happen
            }
        }
        return res;
    }

    /** {@inheritDoc} */
    @Override
    public WikiPage getPageInfo( final String page, final int version ) throws ProviderException {
        return executeIfPageExists( page, version, () -> pages.get( page ).get( version ) );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< WikiPage > getAllPages() throws ProviderException {
        return pages.values().stream().map( versions -> versions.get( versions.size() - 1 ) ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< WikiPage > getAllChangedSince( final Date date ) {
        try {
            return getAllPages();
        } catch( final ProviderException e ) {
            return Collections.emptyList();
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getPageCount() throws ProviderException {
        return pages.size();
    }

    /** {@inheritDoc} */
    @Override
    public List< WikiPage > getVersionHistory( final String page ) throws ProviderException {
        return pageExists( page ) ? pages.get( page ) : Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public String getPageText( final String page, final int version ) throws ProviderException {
        return executeIfPageExists( page, version, () -> {
            final int v = version == WikiProvider.LATEST_VERSION ? contents.get( page ).size() - 1 : version;
            return contents.get( page ).get( v );
        } );
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVersion( final String pageName, final int version ) throws ProviderException {
        executeIfPageExists( pageName, version, () -> {
            pages.get( pageName ).remove( version );
            contents.get( pageName ).remove( version );
            return null;
        } );
    }

    /** {@inheritDoc} */
    @Override
    public void deletePage( final String pageName ) throws ProviderException {
        pages.remove( pageName );
        contents.remove( pageName );
    }

    /** {@inheritDoc} */
    @Override
    public void movePage( final String from, final String to ) throws ProviderException {
        if( pageExists( to ) ) {
            throw new ProviderException( to + " page already exists, can't move there" );
        }
        pages.put( to, pages.get( from ) );
        contents.put( to, contents.get( from ) );
        pages.remove( from );
        contents.remove( from );
    }

}
