/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.providers;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;

import java.util.Collection;
import java.util.Properties;
import java.util.ArrayList;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Provides basic, versioning attachments.
 *
 *  <PRE>
 *   Structure is as follows:
 *      attachment_dir/
 *         ThisPage/
 *            attachment.doc/
 *               attachment.properties
 *               1.doc
 *               2.doc
 *               3.doc
 *            picture.png/
 *               attachment.properties
 *               1.png
 *               2.png
 *         ThatPage/
 *            picture.png/
 *               attachment.properties
 *               1.png
 *             
 *  </PRE>
 *
 *  "attachment.properties" consists of the following items:
 *  <UL>
 *   <LI>1.author = author name for version 1 (etc)
 *  </UL>
 */
public class BasicAttachmentProvider
    implements WikiAttachmentProvider
{
    private String m_storageDir;
    public static final String PROP_STORAGEDIR = "jspwiki.basicAttachmentProvider.storageDir";

    public static final String PROPERTY_FILE   = "attachment.properties";

    public void initialize( Properties properties ) 
        throws NoRequiredPropertyException,
               IOException
    {
        m_storageDir = WikiEngine.getRequiredProperty( properties, PROP_STORAGEDIR );
    }

    /**
     *  Finds storage dir, makes sure it exists.
     *
     *  @param wikipage Page to which this attachment is attached.
     */
    private File findAttachmentDir( String wikipage )
        throws ProviderException
    {
        File f = new File( m_storageDir, wikipage );

        if( !f.exists() )
        {
            f.mkdirs();
        }

        if( !f.isDirectory() )
        {
            throw new ProviderException("Storage dir '"+f.getAbsolutePath()+"' is not a directory!");
        }

        return f;
    }

    /**
     *  Goes through the repository and decides which version is
     *  the newest one in that directory.
     *
     *  @return Latest version number in the repository, or -1, if
     *          there is no page in the repository.
     */
    private int findLatestVersion( Attachment att )
        throws ProviderException
    {
        File pageDir = findAttachmentDir( att.getName() );

        String[] pages = pageDir.list( new AttachmentFilter() );

        if( pages == null )
        {
            return -1; // No such thing found.
        }

        int version = -1;

        for( int i = 0; i < pages.length; i++ )
        {
            int cutpoint = pages[i].indexOf( '.' );
            if( cutpoint > 0 )
            {
                String pageNum = pages[i].substring( 0, cutpoint );

                try
                {
                    int res = Integer.parseInt( pageNum );

                    if( res > version )
                    {
                        version = res;
                    }
                }
                catch( NumberFormatException e ) {} // It's okay to skip these.
            }
        }

        return version;
    }

    public void putAttachmentData( Attachment att, InputStream data )
        throws ProviderException
    {
        File dir = findAttachmentDir( att.getName() );

        int latestVersion = findLatestVersion( att );

        try
        {
            File newfile = new File( dir, att.getFileName()+"."+(latestVersion+1) );

            FileUtil.copyContents( data, new FileOutputStream(newfile) );
        }
        catch( IOException e )
        {
            throw new ProviderException( "Failed to save attachment data: "+e.getMessage() );

        }
    }

    public String getProviderInfo()
    {
        return "";
    }

    public InputStream getAttachmentData( Attachment att )
    {
        return null;
    }

    public Collection listAttachments( WikiPage page )
        throws ProviderException
    {
        Collection result = new ArrayList();

        File dir = findAttachmentDir( page.getName() );

        String[] attachments = dir.list();

        for( int i = 0; i < attachments.length; i++ )
        {
            File f = new File( dir, attachments[i] );

            if( f.isDirectory() )
            {
                Attachment att = getAttachmentInfo( page, attachments[i],
                                                    WikiProvider.LATEST_VERSION );

                result.add( att );
            }
        }

        return result;
    }

    public Collection findAttachments( QueryItem[] query )
    {
        return null;
    }

    public Attachment getAttachmentInfo( WikiPage page, String name, int version )
        throws ProviderException
    {
        File dir = new File( findAttachmentDir( page.getName() ), name );

        if( !dir.exists() )
        {
            return null;
        }

        Attachment att = new Attachment( page.getName() );
        att.setFileName( name );
        
        int latest = findLatestVersion( att );

        if( version == WikiProvider.LATEST_VERSION )
        {
            version = latest;
        }

        att.setVersion( version );

        // FIXME: Check for existence of this particular version.

        return att;
    }

    public Collection getVersionHistory( String wikiname )
    {
        return null;
    }

    /**
     *  Returns only actual versions of files.
     */
    public class AttachmentFilter
        implements FilenameFilter
    {
        public boolean accept( File dir, String name )
        {
            return !name.equals( PROPERTY_FILE );
        }
    }

}

