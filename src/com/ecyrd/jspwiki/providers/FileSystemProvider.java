/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.*;
import java.util.Properties;
import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.*;

/**
 *  Provides a simple directory based repository for Wiki pages.
 *  <P>
 *  All files have ".txt" appended to make life easier for those
 *  who insist on using Windows or other software which makes assumptions
 *  on the files contents based on its name.
 *
 *  @author Janne Jalkanen
 */
public class FileSystemProvider
    extends AbstractFileProvider
{
    private static final Logger   log = Logger.getLogger(FileSystemProvider.class);
    /**
     *  All metadata is stored in a file with this extension.
     */
    public static final String PROP_EXT = ".properties";

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

            if( author != null )
            {
                props.setProperty( "author", author );
                File file = new File( getPageDirectory(), 
                                      mangleName(page.getName())+PROP_EXT );
     
                out = new FileOutputStream( file );

                props.store( out, "JSPWiki page properties for page "+page.getName() );
            }
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

            if( file != null && file.exists() )
            {
                in = new FileInputStream( file );

                props.load(in);

                page.setAuthor( props.getProperty( "author" ) );
            }            
        }
        finally
        {
            if( in != null ) in.close();
        }
    }

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
}
