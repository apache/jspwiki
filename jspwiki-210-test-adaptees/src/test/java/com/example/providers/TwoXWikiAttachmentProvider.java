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
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.pages.PageTimeComparator;
import org.apache.wiki.providers.WikiAttachmentProvider;
import org.apache.wiki.search.QueryItem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


public class TwoXWikiAttachmentProvider implements WikiAttachmentProvider {

    WikiEngine engine;
    Map< String, List < Attachment > > attachments = new ConcurrentHashMap<>();
    Map< String, List < InputStream > > contents = new ConcurrentHashMap<>();

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
            putAttachmentData( new Attachment( engine, "page1", "att11.txt" ), new ByteArrayInputStream( "blurb".getBytes( StandardCharsets.UTF_8 ) ) );
            putAttachmentData( new Attachment( engine, "page1", "att12.txt" ), new ByteArrayInputStream( "blerb".getBytes( StandardCharsets.UTF_8) ) );
            putAttachmentData( new Attachment( engine, "page2", "att21.txt" ), new ByteArrayInputStream( "blarb".getBytes( StandardCharsets.UTF_8) ) );
        } catch( final ProviderException e ) {
            throw new IOException( e.getMessage(), e );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void putAttachmentData( final Attachment att, final InputStream data ) throws ProviderException, IOException {
        att.setVersion( att.getVersion() + 1 );
        att.setLastModified( new Date() );
        if( attachmentExists( att ) ) {
            attachments.get( att.getName() ).add( att );
            contents.get( att.getName() ).add( data );
        } else {
            attachments.put( att.getName(), new ArrayList<>( Arrays.asList( att ) ) );
            contents.put( att.getName(), new ArrayList<>( Arrays.asList( data ) ) );
        }
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAttachmentData( final Attachment att ) throws ProviderException, IOException {
        final int v = att.getVersion() == WikiProvider.LATEST_VERSION ? contents.get( att.getName() ).size() - 1 : att.getVersion();
        return contents.get( att.getName() ).get( v );
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > listAttachments( final WikiPage page ) throws ProviderException {
        return attachments.entrySet()
                          .stream()
                          .filter( e -> e.getKey().startsWith( page.getName() + "/" ) )
                          .map( Map.Entry::getValue )
                          .flatMap( List::stream )
                          .collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< Attachment > findAttachments( final QueryItem[] query ) {
        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > listAllChanged( final Date timestamp ) throws ProviderException {
        final List< Attachment > attachs = attachments.values()
                                                      .stream()
                                                      .map( attrs -> attrs.get( attrs.size() -1 ) )
                                                      .filter( att -> att.getLastModified() != null && att.getLastModified().after( timestamp ) )
                                                      .collect( Collectors.toList() );
        attachs.sort( new PageTimeComparator() );
        return attachs;
    }

    /** {@inheritDoc} */
    @Override
    public Attachment getAttachmentInfo( final WikiPage page, final String name, final int version ) throws ProviderException {
        if( attachments.get( page.getName() + "/" + name ) != null ) {
            final int v = version == WikiProvider.LATEST_VERSION ? contents.get( page.getName() + "/" + name ).size() - 1 : version;
            return attachments.get( page.getName() + "/" + name ).get( v );
        }
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > getVersionHistory( final Attachment att ) {
        if( att != null && attachments.get( att.getName() ) != null ) {
            return attachments.get( att.getName() );
        }
        return Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVersion( final Attachment att ) throws ProviderException {
        if( attachmentVersionExists( att ) ) {
            attachments.get( att.getName() ).remove( att.getVersion() - 1 );
            contents.get( att.getName() ).remove( att.getVersion() - 1 );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttachment( final Attachment att ) throws ProviderException {
        if( att != null ) {
            attachments.remove( att.getName() );
            contents.remove( att.getName() );
        }
    }

    /** {@inheritDoc} */
    @Override
    public void moveAttachmentsForPage( final String oldParent, final String newParent ) throws ProviderException {
        final Map< String, List < Attachment > > oldAttachments = attachments.entrySet()
                                                                             .stream()
                                                                             .filter( e -> e.getKey().startsWith( oldParent + "/" ) )
                                                                             .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );
        final Map< String, List < InputStream > > oldContents = contents.entrySet()
                                                                        .stream()
                                                                        .filter( e -> e.getKey().startsWith( oldParent + "/" ) )
                                                                        .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );

        // If it exists, we're overwriting an old page (this has already been confirmed at a higher level), so delete any existing attachments.
        oldAttachments.forEach( ( key, value ) -> {
            final String newKey = newParent + "/" + key.substring( key.indexOf( '/' ) + 1 );
            attachments.putIfAbsent( newKey, value );
            if( attachments.get( key ) != null ) {
                attachments.remove( key );
            }
        } );
        oldContents.forEach( ( key, value ) -> {
            final String newKey = newParent + "/" + key.substring( key.indexOf( '/' ) + 1 );
            contents.putIfAbsent( newKey, value );
            if( contents.get( key ) != null ) {
                contents.remove( key );
            }
        } );
    }

    boolean attachmentExists( final Attachment att ) {
        return att != null && attachments.get( att.getName() ) != null && contents.get( att.getName() ) != null;
    }

    boolean attachmentVersionExists( final Attachment att ) {
        return attachmentExists( att ) &&
               attachments.get( att.getName() ).size() >= att.getVersion() &&
               contents.get( att.getName() ).size() >= att.getVersion();
    }

}
