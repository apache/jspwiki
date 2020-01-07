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

import org.apache.log4j.Logger;
import org.apache.wiki.WikiPage;
import org.apache.wiki.api.exceptions.ProviderException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

/**
 *  Provides a simple directory based repository for Wiki pages.
 *  <P>
 *  All files have ".txt" appended to make life easier for those
 *  who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *
 */
public class FileSystemProvider
    extends AbstractFileProvider
{
    private static final Logger   log = Logger.getLogger(FileSystemProvider.class);
    /**
     *  All metadata is stored in a file with this extension.
     */
    public static final String PROP_EXT = ".properties";

    /**
     *  {@inheritDoc}
     */
    public void putPageText( WikiPage page, String text )        
        throws ProviderException
    {
        try
        {
            super.putPageText( page, text );
            putPageProperties( page );
        }
        catch( IOException e )
        {
            log.error( "Saving failed" );
        }
    }

    /**
     *  Stores basic metadata to a file.
     */
    private void putPageProperties( WikiPage page )        
        throws IOException
    {
        Properties props = new Properties();        
        OutputStream out = null;

        try
        {
            String author = page.getAuthor();
            String changenote = page.getAttribute( WikiPage.CHANGENOTE );
            String viewcount = page.getAttribute( WikiPage.VIEWCOUNT );
            
            if( author != null )
            {
                props.setProperty( WikiPage.AUTHOR, author );
            }
            
            if( changenote != null )
            {
                props.setProperty( WikiPage.CHANGENOTE, changenote );
            }

            if( viewcount != null )
            {
                props.setProperty( WikiPage.VIEWCOUNT, viewcount );
            }
            
            // Get additional custom properties from page and add to props
            getCustomProperties(page, props);
            	
            
            File file = new File( getPageDirectory(), 
                                  mangleName(page.getName())+PROP_EXT );
     
            out = new FileOutputStream( file );

            props.store( out, "JSPWiki page properties for page "+page.getName() );
        }
        finally
        {
            if( out != null ) out.close();
        }
    }
    
    /**
     *  Gets basic metadata from file.
     */
    private void getPageProperties( WikiPage page )
        throws IOException
    {
        Properties  props = new Properties();
        InputStream in    = null;

        try
        {
            File file = new File( getPageDirectory(), 
                                  mangleName(page.getName())+PROP_EXT );

            if( file.exists() )
            {
                in = new FileInputStream( file );

                props.load(in);

                page.setAuthor( props.getProperty( WikiPage.AUTHOR ) );
                
                String changenote = props.getProperty( WikiPage.CHANGENOTE );
                if( changenote != null )
                {
                    page.setAttribute( WikiPage.CHANGENOTE, changenote );
                }
                
                String viewcount = props.getProperty( WikiPage.VIEWCOUNT );
                if( viewcount != null )
                {
                    page.setAttribute( WikiPage.VIEWCOUNT, viewcount );
                }
                
                // Set the props values to the page attributes
                setCustomProperties(page, props);
            }            
        }
        finally
        {
            if( in != null ) in.close();
        }
    }

    /**
     *  {@inheritDoc}
     */
    public WikiPage getPageInfo( String page, int version )
        throws ProviderException
    {
        WikiPage p = super.getPageInfo( page, version );

        if( p != null )
        {
            try
            {
                getPageProperties( p );
            }
            catch( IOException e )
            {
                log.error("Unable to read page properties", e );
                throw new ProviderException("Unable to read page properties, check logs.");
            }
        }

        return p;
    }

    /**
     *  {@inheritDoc}
     */
    public void deletePage(String pageName) throws ProviderException
    {
        super.deletePage(pageName);

        File file = new File( getPageDirectory(), 
                              mangleName(pageName)+PROP_EXT );
        
        if( file.exists() ) file.delete();
    }

    /**
     *  {@inheritDoc}
     */
    public void movePage( String from,
                          String to )
        throws ProviderException
    {
        File fromPage = findPage( from );
        File toPage = findPage( to );
        
        fromPage.renameTo( toPage );
    }
}
