/*
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.dav;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.dav.items.AttachmentItem;
import org.apache.wiki.dav.items.DavItem;
import org.apache.wiki.dav.items.DirectoryItem;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;



public class AttachmentDavProvider implements DavProvider
{
    protected WikiEngine m_engine;
    protected static final Logger log = LoggerFactory.getLogger( AttachmentDavProvider.class );

    public AttachmentDavProvider( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     *  Returns the engine used by this provider.
     *  
     *  @return The engine
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    private Collection listAllPagesWithAttachments()
    {
        ArrayList<String> pageNames = new ArrayList<String>();

        try
        {
            Collection atts = m_engine.getAttachmentManager().getAllAttachments();

            for( Iterator i = atts.iterator(); i.hasNext(); )
            {
                Attachment att = (Attachment)i.next();

                String pageName = att.getParentName();

                if( !pageNames.contains(pageName) )
                    pageNames.add( pageName );
            }
        }
        catch (ProviderException e)
        {
            log.error("Unable to get all attachments",e);
        }

        Collections.sort( pageNames );

        ArrayList<DirectoryItem> result = new ArrayList<DirectoryItem>();

        for( Iterator i = pageNames.iterator(); i.hasNext(); )
        {
            DirectoryItem di = new DirectoryItem( this, new DavPath( (String)i.next() ));

            result.add( di );
        }
        return result;
    }

    protected Collection listAttachmentsOfPage( DavPath path )
    {
        String pageName = path.getName();

        log.debug("Listing attachments for page "+pageName);

        ArrayList<DavItem> result = new ArrayList<DavItem>();
        try
        {
            WikiPage page = m_engine.getPage( pageName );
            Collection attachments = m_engine.getAttachmentManager().listAttachments(page);

            for( Iterator i = attachments.iterator(); i.hasNext(); )
            {
                Attachment att = (Attachment) i.next();

                DavPath thisPath = new DavPath( "/" );

                thisPath.append( att.getName() );

                AttachmentItem ai = new AttachmentItem( this, thisPath, att );

                result.add( ai );
            }
        }
        catch( ProviderException e )
        {
            log.error("Unable to list attachments, returning what I got",e);
            // FIXME: Not a good way to handle errors
        }

        return result;
    }

    public DavItem getItem(DavPath path)
    {
        if( path.isRoot() )
        {
            DirectoryItem di = new DirectoryItem( this, new DavPath("") );

            di.addDavItems( listAllPagesWithAttachments() );
            return di;
        }
        else if( path.isDirectory() )
        {
            DirectoryItem di = new DirectoryItem( this, path );

            di.addDavItems( listAttachmentsOfPage(path) );

            return di;
        }
        else
        {
            String attName = path.getPath();

            try
            {
                Attachment att = m_engine.getAttachmentManager().getAttachmentInfo( attName );

                if( att != null )
                {
                    AttachmentItem ai = new AttachmentItem( this, path, att );

                    return ai;
                }
            }
            catch( ProviderException e )
            {
                log.error("Unable to get the attachment data for "+attName,e);
            }
        }
        return null;
    }

    public void setItem(DavPath path, DavItem item)
    {
        // TODO Auto-generated method stub

    }

    public String getURL(DavPath path)
    {
        String p = path.getPath();

        if( p.startsWith("/") ) p = p.substring( 1 );

        return m_engine.getURL( WikiContext.ATTACH, p, null, true );
    }

}
