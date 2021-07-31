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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.search.SearchResultComparator;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.stream.Collectors;


/**
 * This provider ensures backward compatibility with page providers not using the public API. As providers should use the public API
 * directly, the use of this class is considered deprecated.
 *
 * @deprecated adapted provider should use {@link PageProvider} instead.
 * @see PageProvider
 */
@Deprecated
public class WikiPageAdapterProvider implements PageProvider {

    private static final Logger LOG = LogManager.getLogger( WikiPageAdapterProvider.class );
    private static final String PROP_ADAPTER_IMPL = "jspwiki.pageProvider.adapter.impl";

    WikiPageProvider provider;

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        LOG.warn( "Using a page provider through org.apache.wiki.providers.WikiPageAdapterProviderAdapterProvider" );
        LOG.warn( "Please contact the page provider's author so there can be a new release of the provider " +
                  "implementing the new org.apache.wiki.api.providers.PageProvider public API" );
        final String classname = TextUtil.getRequiredProperty( properties, PROP_ADAPTER_IMPL );
        try {
            LOG.debug( "Page provider class: '" + classname + "'" );
            final Class<?> providerclass = ClassUtil.findClass("org.apache.wiki.providers", classname);
            provider = ( WikiPageProvider ) providerclass.newInstance();
        } catch( final IllegalAccessException | InstantiationException | ClassNotFoundException e ) {
            LOG.error( "Could not instantiate " + classname, e );
            throw new IOException( e.getMessage(), e );
        }

        LOG.debug( "Initializing page provider class " + provider );
        provider.initialize( engine, properties );
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderInfo() {
        return provider.getProviderInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void putPageText( final Page page, final String text ) throws ProviderException {
        provider.putPageText( ( WikiPage )page, text );
    }

    /** {@inheritDoc} */
    @Override
    public boolean pageExists( final String page ) {
        return provider.pageExists( page );
    }

    /** {@inheritDoc} */
    @Override
    public boolean pageExists( final String page, final int version ) {
        return provider.pageExists( page, version );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< SearchResult > findPages( final QueryItem[] query ) {
        final org.apache.wiki.search.QueryItem[] queryItems = Arrays.stream( query )
                                                                    .map( SearchAdapter::oldQueryItemfrom )
                                                                    .toArray( org.apache.wiki.search.QueryItem[]::new );
        final Collection< org.apache.wiki.search.SearchResult > results = provider.findPages( queryItems );
        return results.stream()
                      .map( SearchAdapter::newSearchResultFrom )
                      .collect( Collectors.toCollection( () -> new TreeSet<>( new SearchResultComparator() ) ) );
    }

    /** {@inheritDoc} */
    @Override
    public Page getPageInfo( final String page, final int version ) throws ProviderException {
        return provider.getPageInfo( page, version );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< Page > getAllPages() throws ProviderException {
        return provider.getAllPages().stream().map( wikiPage -> ( Page )wikiPage ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< Page > getAllChangedSince( final Date date ) {
        return provider.getAllChangedSince( date ).stream().map( wikiPage -> ( Page )wikiPage ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public int getPageCount() throws ProviderException {
        return provider.getPageCount();
    }

    /** {@inheritDoc} */
    @Override
    public List< Page > getVersionHistory( final String page ) throws ProviderException {
        return provider.getVersionHistory( page ).stream().map( wikiPage -> ( Page )wikiPage ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public String getPageText( final String page, final int version ) throws ProviderException {
        return provider.getPageText( page, version );
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVersion( final String pageName, final int version ) throws ProviderException {
        provider.deleteVersion( pageName, version );
    }

    /** {@inheritDoc} */
    @Override
    public void deletePage( final String pageName ) throws ProviderException {
        provider.deletePage( pageName );
    }

    /** {@inheritDoc} */
    @Override
    public void movePage( final String from, final String to ) throws ProviderException {
        provider.movePage( from, to );
    }

}
