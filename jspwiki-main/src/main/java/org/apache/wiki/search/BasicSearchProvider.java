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
package org.apache.wiki.search;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.pages.PageManager;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 *  Interface for the search providers that handle searching the Wiki
 *
 *  @since 2.2.21.
 */
public class BasicSearchProvider implements SearchProvider {

    private static final Logger log = LogManager.getLogger( BasicSearchProvider.class );
    private Engine m_engine;

    /**
     *  {@inheritDoc}
     */
    @Override
    public void initialize( final Engine engine, final Properties props ) throws NoRequiredPropertyException, IOException {
        m_engine = engine;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void pageRemoved( final Page page ) {}

    /**
     *  {@inheritDoc}
     */
    @Override
    public void reindexPage( final Page page ) {}

    /**
     *  Parses a query into something that we can use.
     *  
     *  @param query A query string.
     *  @return A parsed array.
     */
    public QueryItem[] parseQuery( final String query) {
        final StringTokenizer st = new StringTokenizer( query, " \t," );
        final QueryItem[] items = new QueryItem[st.countTokens()];
        int word = 0;

        log.debug("Expecting "+items.length+" items");

        //  Parse incoming search string
        while( st.hasMoreTokens() ) {
            log.debug( "Item " + word );
            String token = st.nextToken().toLowerCase();

            items[ word ] = new QueryItem();

            switch( token.charAt( 0 ) ) {
            case '+':
                items[ word ].type = QueryItem.REQUIRED;
                token = token.substring( 1 );
                log.debug( "Required word: " + token );
                break;

            case '-':
                items[ word ].type = QueryItem.FORBIDDEN;
                token = token.substring( 1 );
                log.debug( "Forbidden word: " + token );
                break;

            default:
                items[ word ].type = QueryItem.REQUESTED;
                log.debug( "Requested word: " + token );
                break;
            }

            items[ word++ ].word = token;
        }

        return items;
    }

    private String attachmentNames( final Page page ) {
        if( m_engine.getManager( AttachmentManager.class ).hasAttachments( page ) ) {
            final List< Attachment > attachments;
            try {
                attachments = m_engine.getManager( AttachmentManager.class ).listAttachments( page );
            } catch( final ProviderException e ) {
                log.error( "Unable to get attachments for page", e );
                return "";
            }

            final StringBuilder attachmentNames = new StringBuilder();
            for( final Iterator< Attachment > it = attachments.iterator(); it.hasNext(); ) {
                final Attachment att = it.next();
                attachmentNames.append( att.getName() );
                if( it.hasNext() ) {
                    attachmentNames.append( " " );
                }
            }
            return attachmentNames.toString();
        }

        return "";
    }

    private Collection< SearchResult > findPages( final QueryItem[] query, final Context wikiContext ) {
        final TreeSet< SearchResult > res = new TreeSet<>( new SearchResultComparator() );
        final SearchMatcher matcher = new SearchMatcher( m_engine, query );
        final Collection< Page > allPages;
        try {
            allPages = m_engine.getManager( PageManager.class ).getAllPages();
        } catch( final ProviderException pe ) {
            log.error( "Unable to retrieve page list", pe );
            return null;
        }

        final AuthorizationManager mgr = m_engine.getManager( AuthorizationManager.class );

        for( final Page page : allPages ) {
            try {
                if( page != null ) {
                    final PagePermission pp = new PagePermission( page, PagePermission.VIEW_ACTION );
                    if( wikiContext == null || mgr.checkPermission( wikiContext.getWikiSession(), pp ) ) {
                        final String pageName = page.getName();
                        final String pageContent =
                                m_engine.getManager( PageManager.class ).getPageText( pageName, PageProvider.LATEST_VERSION ) + attachmentNames( page );
                        final SearchResult comparison = matcher.matchPageContent( pageName, pageContent );
                        if( comparison != null ) {
                            res.add( comparison );
                        }
                    }
                }
            } catch( final ProviderException pe ) {
                log.error( "Unable to retrieve page from cache", pe );
            } catch( final IOException ioe ) {
                log.error( "Failed to search page", ioe );
            }
        }

        return res;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Collection< SearchResult > findPages( final String query, final Context wikiContext ) {
        return findPages( parseQuery( query ), wikiContext );
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public String getProviderInfo() {
        return "BasicSearchProvider";
    }

}
