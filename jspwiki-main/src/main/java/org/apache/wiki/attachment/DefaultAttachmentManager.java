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
package org.apache.wiki.attachment;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.log4j.Logger;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.parser.MarkupParser;
import org.apache.wiki.providers.WikiAttachmentProvider;
import org.apache.wiki.references.ReferenceManager;
import org.apache.wiki.search.SearchManager;
import org.apache.wiki.util.ClassUtil;
import org.apache.wiki.util.TextUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Properties;


/**
 *  Default implementation for {@link AttachmentManager}
 *
 * {@inheritDoc}
 *
 *  @since 1.9.28
 */
public class DefaultAttachmentManager implements AttachmentManager {

    /** List of attachment types which are forced to be downloaded */
    private String[] m_forceDownloadPatterns;

    private static final Logger log = Logger.getLogger( DefaultAttachmentManager.class );
    private WikiAttachmentProvider m_provider;
    private Engine m_engine;
    private CacheManager m_cacheManager = CacheManager.getInstance();
    private Cache m_dynamicAttachments;

    /**
     *  Creates a new AttachmentManager.  Note that creation will never fail, but it's quite likely that attachments do not function.
     *  <p><b>DO NOT CREATE</b> an AttachmentManager on your own, unless you really know what you're doing. Just use
     *  WikiEngine.getAttachmentManager() if you're making a module for JSPWiki.
     *
     *  @param engine The wikiengine that owns this attachment manager.
     *  @param props A list of properties from which the AttachmentManager will seek its configuration. Typically this is the "jspwiki.properties".
     */
    // FIXME: Perhaps this should fail somehow.
    public DefaultAttachmentManager( final Engine engine, final Properties props ) {
        m_engine = engine;

        //  If user wants to use a cache, then we'll use the CachingProvider.
        final boolean useCache = "true".equals( props.getProperty( PageManager.PROP_USECACHE ) );

        final String classname;
        if( useCache ) {
            classname = "org.apache.wiki.providers.CachingAttachmentProvider";
        } else {
            classname = props.getProperty( PROP_PROVIDER );
        }

        //  If no class defined, then will just simply fail.
        if( classname == null ) {
            log.info( "No attachment provider defined - disabling attachment support." );
            return;
        }

        //  Create and initialize the provider.
        final String cacheName = engine.getApplicationName() + "." + CACHE_NAME;
        try {
            if( m_cacheManager.cacheExists( cacheName ) ) {
                m_dynamicAttachments = m_cacheManager.getCache( cacheName );
            } else {
                log.info( "cache with name " + cacheName + " not found in ehcache.xml, creating it with defaults." );
                m_dynamicAttachments = new Cache( cacheName, DEFAULT_CACHECAPACITY, false, false, 0, 0 );
                m_cacheManager.addCache( m_dynamicAttachments );
            }

            final Class< ? > providerclass = ClassUtil.findClass( "org.apache.wiki.providers", classname );

            m_provider = ( WikiAttachmentProvider )providerclass.newInstance();
            m_provider.initialize( m_engine.adapt( WikiEngine.class ), props );
        } catch( final ClassNotFoundException e ) {
            log.error( "Attachment provider class not found",e);
        } catch( final InstantiationException e ) {
            log.error( "Attachment provider could not be created", e );
        } catch( final IllegalAccessException e ) {
            log.error( "You may not access the attachment provider class", e );
        } catch( final NoRequiredPropertyException e ) {
            log.error( "Attachment provider did not find a property that it needed: " + e.getMessage(), e );
            m_provider = null; // No, it did not work.
        } catch( final IOException e ) {
            log.error( "Attachment provider reports IO error", e );
            m_provider = null;
        }

        final String forceDownload = TextUtil.getStringProperty( props, PROP_FORCEDOWNLOAD, null );
        if( forceDownload != null && forceDownload.length() > 0 ) {
            m_forceDownloadPatterns = forceDownload.toLowerCase().split( "\\s" );
        } else {
            m_forceDownloadPatterns = new String[ 0 ];
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean attachmentsEnabled() {
        return m_provider != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getAttachmentInfoName( final WikiContext context, final String attachmentname ) {
        final Attachment att;
        try {
            att = getAttachmentInfo( context, attachmentname );
        } catch( final ProviderException e ) {
            log.warn( "Finding attachments failed: ", e );
            return null;
        }

        if( att != null ) {
            return att.getName();
        } else if( attachmentname.indexOf( '/' ) != -1 ) {
            return attachmentname;
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Attachment getAttachmentInfo( final WikiContext context, String attachmentname, final int version ) throws ProviderException {
        if( m_provider == null ) {
            return null;
        }

        WikiPage currentPage = null;

        if( context != null ) {
            currentPage = context.getPage();
        }

        //  Figure out the parent page of this attachment.  If we can't find it, we'll assume this refers directly to the attachment.
        final int cutpt = attachmentname.lastIndexOf( '/' );
        if( cutpt != -1 ) {
            String parentPage = attachmentname.substring( 0, cutpt );
            parentPage = MarkupParser.cleanLink( parentPage );
            attachmentname = attachmentname.substring( cutpt + 1 );

            // If we for some reason have an empty parent page name; this can't be an attachment
            if( parentPage.length() == 0 ) {
                return null;
            }

            currentPage = m_engine.getManager( PageManager.class ).getPage( parentPage );

            // Go check for legacy name
            // FIXME: This should be resolved using CommandResolver, not this adhoc way.  This also assumes that the
            //        legacy charset is a subset of the full allowed set.
            if( currentPage == null ) {
                currentPage = m_engine.getManager( PageManager.class ).getPage( MarkupParser.wikifyLink( parentPage ) );
            }
        }

        //  If the page cannot be determined, we cannot possibly find the attachments.
        if( currentPage == null || currentPage.getName().length() == 0 ) {
            return null;
        }

        //  Finally, figure out whether this is a real attachment or a generated attachment.
        Attachment att = getDynamicAttachment( currentPage.getName() + "/" + attachmentname );
        if( att == null ) {
            att = m_provider.getAttachmentInfo( currentPage, attachmentname, version );
        }

        return att;
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > listAttachments( final WikiPage wikipage ) throws ProviderException {
        if( m_provider == null ) {
            return new ArrayList<>();
        }

        final List< Attachment > atts = new ArrayList<>( m_provider.listAttachments( wikipage ) );
        atts.sort( Comparator.comparing( Attachment::getName, m_engine.getManager( PageManager.class ).getPageSorter() ) );

        return atts;
    }

    /** {@inheritDoc} */
    @Override
    public boolean forceDownload( String name ) {
        if( name == null || name.length() == 0 ) {
            return false;
        }

        name = name.toLowerCase();
        if( name.indexOf( '.' ) == -1 ) {
            return true;  // force download on attachments without extension or type indication
        }

        for( final String forceDownloadPattern : m_forceDownloadPatterns ) {
            if( name.endsWith( forceDownloadPattern ) && forceDownloadPattern.length() > 0 ) {
                return true;
            }
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAttachmentStream( final WikiContext ctx, final Attachment att ) throws ProviderException, IOException {
        if( m_provider == null ) {
            return null;
        }

        if( att instanceof DynamicAttachment ) {
            return ( ( DynamicAttachment )att ).getProvider().getAttachmentData( ctx, att );
        }

        return m_provider.getAttachmentData( att );
    }

    /** {@inheritDoc} */
    @Override
    public void storeDynamicAttachment( final WikiContext ctx, final DynamicAttachment att ) {
        m_dynamicAttachments.put( new Element( att.getName(), att ) );
    }

    /** {@inheritDoc} */
    @Override
    public DynamicAttachment getDynamicAttachment( final String name ) {
        final Element element = m_dynamicAttachments.get( name );
        if( element != null ) {
            return ( DynamicAttachment )element.getObjectValue();
        } else {
            // Remove from cache, it has expired.
            m_dynamicAttachments.put( new Element( name, null ) );
            return null;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void storeAttachment( final Attachment att, final InputStream in ) throws IOException, ProviderException {
        if( m_provider == null ) {
            return;
        }

        // Checks if the actual, real page exists without any modifications or aliases. We cannot store an attachment to a non-existent page.
        if( !m_engine.getManager( PageManager.class ).pageExists( att.getParentName() ) ) {
            // the caller should catch the exception and use the exception text as an i18n key
            throw new ProviderException( "attach.parent.not.exist" );
        }

        m_provider.putAttachmentData( att, in );
        m_engine.getManager( ReferenceManager.class ).updateReferences( att.getName(), new ArrayList<>() );

        final WikiPage parent = new WikiPage( m_engine, att.getParentName() );
        m_engine.getManager( ReferenceManager.class ).updateReferences( parent );
        m_engine.getManager( SearchManager.class ).reindexPage( att );
    }

    /** {@inheritDoc} */
    @Override
    public List< Attachment > getVersionHistory( final String attachmentName ) throws ProviderException {
        if( m_provider == null ) {
            return null;
        }

        final Attachment att = getAttachmentInfo( null, attachmentName );
        if( att != null ) {
            return m_provider.getVersionHistory( att );
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Attachment> getAllAttachments() throws ProviderException {
        if( attachmentsEnabled() ) {
            return m_provider.listAllChanged( new Date( 0L ) );
        }

        return new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    public WikiAttachmentProvider getCurrentProvider() {
        return m_provider;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteVersion( final Attachment att ) throws ProviderException {
        if( m_provider == null ) {
            return;
        }

        m_provider.deleteVersion( att );
    }

    /** {@inheritDoc} */
    @Override
    // FIXME: Should also use events!
    public void deleteAttachment( final Attachment att ) throws ProviderException {
        if( m_provider == null ) {
            return;
        }

        m_provider.deleteAttachment( att );
        m_engine.getManager( SearchManager.class ).pageRemoved( att );
        m_engine.getManager( ReferenceManager.class ).clearPageEntries( att.getName() );
    }

}
