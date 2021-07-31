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
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.AttachmentProvider;
import org.apache.wiki.api.search.QueryItem;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;


/**
 * This provider ensures backward compatibility with attachment providers not using the public API. As providers should use the public API
 * directly, the use of this class is considered deprecated.
 *
 * @deprecated adapted provider should use {@link AttachmentProvider} instead.
 * @see AttachmentProvider
 */
@Deprecated
public class WikiAttachmentAdapterProvider implements AttachmentProvider {

    private static final Logger LOG = LogManager.getLogger( WikiAttachmentAdapterProvider.class );
    private static final String PROP_ADAPTER_IMPL = "jspwiki.attachmentProvider.adapter.impl";

    WikiAttachmentProvider provider;

    /** {@inheritDoc} */
    @Override
    public void initialize( final Engine engine, final Properties properties ) throws NoRequiredPropertyException, IOException {
        LOG.warn( "Using an attachment provider through org.apache.wiki.providers.WikiAttachmentAdapterProvider" );
        LOG.warn( "Please contact the attachment provider's author so there can be a new release of the provider " +
                  "implementing the new org.apache.wiki.api.providers.AttachmentProvider public API" );
        final String classname = TextUtil.getRequiredProperty( properties, PROP_ADAPTER_IMPL );
        try {
            LOG.debug( "Page provider class: '" + classname + "'" );
            final Class<?> providerclass = ClassUtil.findClass("org.apache.wiki.providers", classname);
            provider = ( WikiAttachmentProvider ) providerclass.newInstance();
        } catch( final IllegalAccessException | InstantiationException | ClassNotFoundException e ) {
            LOG.error( "Could not instantiate " + classname, e );
            throw new IOException( e.getMessage(), e );
        }

        LOG.debug( "Initializing attachment provider class " + provider );
        provider.initialize( engine, properties );
    }

    /** {@inheritDoc} */
    @Override
    public String getProviderInfo() {
        return provider.getProviderInfo();
    }

    /** {@inheritDoc} */
    @Override
    public void putAttachmentData( final Attachment att, final InputStream data ) throws ProviderException, IOException {
        provider.putAttachmentData( ( org.apache.wiki.attachment.Attachment )att, data );
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAttachmentData( final Attachment att ) throws ProviderException, IOException {
        return provider.getAttachmentData( ( org.apache.wiki.attachment.Attachment )att );
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > listAttachments( final Page page ) throws ProviderException {
        return provider.listAttachments( ( WikiPage )page ).stream().map( att -> ( Attachment )att ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public Collection< Attachment > findAttachments( final QueryItem[] query ) {
        final org.apache.wiki.search.QueryItem[] queryItems = Arrays.stream( query )
                                                                    .map( SearchAdapter::oldQueryItemfrom )
                                                                    .toArray( org.apache.wiki.search.QueryItem[]::new );
        return provider.findAttachments( queryItems ).stream().map( att -> ( Attachment )att ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > listAllChanged( final Date timestamp ) throws ProviderException {
        return provider.listAllChanged( timestamp ).stream().map( att -> ( Attachment )att ).collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public Attachment getAttachmentInfo( final Page page, final String name, final int version ) throws ProviderException {
        return provider.getAttachmentInfo( ( WikiPage )page, name, version );
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > getVersionHistory( final Attachment att ) {
        return provider.getVersionHistory( ( org.apache.wiki.attachment.Attachment )att )
                       .stream()
                       .map( attr -> ( Attachment )attr )
                       .collect( Collectors.toList() );
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVersion( final Attachment att ) throws ProviderException {
        provider.deleteVersion( ( org.apache.wiki.attachment.Attachment )att );
    }

    /** {@inheritDoc} */
    @Override
    public void deleteAttachment( final Attachment att ) throws ProviderException {
        provider.deleteAttachment( ( org.apache.wiki.attachment.Attachment )att );
    }

    /** {@inheritDoc} */
    @Override
    public void moveAttachmentsForPage( final String oldParent, final String newParent ) throws ProviderException {
        provider.moveAttachmentsForPage( oldParent, newParent );
    }

}
