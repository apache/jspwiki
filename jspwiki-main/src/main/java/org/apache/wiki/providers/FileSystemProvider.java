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
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.ProviderException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;

/**
 *  Provides a simple directory based repository for Wiki pages.
 *  <P>
 *  All files have ".txt" appended to make life easier for those
 *  who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *
 */
public class FileSystemProvider extends AbstractFileProvider {

    private static final Logger LOG = LogManager.getLogger( FileSystemProvider.class );

    /** All metadata is stored in a file with this extension. */
    public static final String PROP_EXT = ".properties";

    /**
     *  {@inheritDoc}
     */
    @Override
    public void putPageText( final Page page, final String text ) throws ProviderException {
        try {
            super.putPageText( page, text );
            putPageProperties( page );
        } catch( final IOException e ) {
            LOG.error( "Saving failed" );
        }
    }

    /**
     *  Stores basic metadata to a file.
     */
    private void putPageProperties( final Page page ) throws IOException {
        final Properties props = new Properties();
        final String author = page.getAuthor();
        final String changenote = page.getAttribute( Page.CHANGENOTE );
        final String viewcount = page.getAttribute( Page.VIEWCOUNT );

        if( author != null ) {
            props.setProperty( Page.AUTHOR, author );
        }

        if( changenote != null ) {
            props.setProperty( Page.CHANGENOTE, changenote );
        }

        if( viewcount != null ) {
            props.setProperty( Page.VIEWCOUNT, viewcount );
        }

        // Get additional custom properties from page and add to props
        getCustomProperties( page, props );

        final File file = new File( getPageDirectory(), mangleName( page.getName() ) + PROP_EXT );
        try( final OutputStream out = Files.newOutputStream( file.toPath() ) ) {
            props.store( out, "JSPWiki page properties for page "+page.getName() );
        }
    }
    
    /**
     *  Gets basic metadata from file.
     */
    private void getPageProperties( final Page page ) throws IOException {
        final File file = new File( getPageDirectory(), mangleName( page.getName() ) + PROP_EXT );
        if( file.exists() ) {
            try( final InputStream in = Files.newInputStream( file.toPath() ) ) {
                final Properties  props = new Properties();
                props.load( in );
                page.setAuthor( props.getProperty( Page.AUTHOR ) );

                final String changenote = props.getProperty( Page.CHANGENOTE );
                if( changenote != null ) {
                    page.setAttribute( Page.CHANGENOTE, changenote );
                }

                final String viewcount = props.getProperty( Page.VIEWCOUNT );
                if( viewcount != null ) {
                    page.setAttribute( Page.VIEWCOUNT, viewcount );
                }

                // Set the props values to the page attributes
                setCustomProperties( page, props );
            }
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public Page getPageInfo( final String page, final int version ) throws ProviderException {
        final Page p = super.getPageInfo( page, version );
        if( p != null ) {
            try {
                getPageProperties( p );
            } catch( final IOException e ) {
                LOG.error( "Unable to read page properties", e );
                throw new ProviderException( "Unable to read page properties, check logs." );
            }
        }

        return p;
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void deletePage( final String pageName) throws ProviderException {
        super.deletePage( pageName );
        final File file = new File( getPageDirectory(), mangleName(pageName)+PROP_EXT );
        if( file.exists() ) {
            file.delete();
        }
    }

    /**
     *  {@inheritDoc}
     */
    @Override
    public void movePage( final String from, final String to ) throws ProviderException {
        final File fromPage = findPage( from );
        final File toPage = findPage( to );
        fromPage.renameTo( toPage );
    }

}
