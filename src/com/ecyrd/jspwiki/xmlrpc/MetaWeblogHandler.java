/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.xmlrpc;

import java.io.*;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.plugin.WeblogEntryPlugin;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;

/**
 *  Provides handlers for all RPC routines of the MetaWeblog API.
 *  <P>
 *  Currently only implements the newPost and newMediaObject methods.
 *
 *  @author Janne Jalkanen
 *  @since 2.1.7
 */

public class MetaWeblogHandler
    implements WikiRPCHandler
{
    Category log = Category.getInstance( MetaWeblogHandler.class ); 

    private WikiEngine m_engine;

    public void initialize( WikiEngine engine )
    {
        m_engine = engine;
    }

    public String newPost( String blogid,
                           String username,
                           String password,
                           Hashtable content,
                           boolean publish )
        throws XmlRpcException
    {
        log.info("metaWeblog.newPost() called");

        try
        {
            WeblogEntryPlugin plugin = new WeblogEntryPlugin();

            String pageName = plugin.getNewEntryPage( m_engine, blogid );

            WikiPage entryPage = new WikiPage( pageName );
            entryPage.setAuthor( username );

            StringBuffer text = new StringBuffer();
            text.append( "!"+content.get("title") );
            text.append( "\n\n" );
            text.append( content.get("description") );

            log.debug("Writing entry: "+text);

            m_engine.saveText( entryPage, text.toString() );
        }
        catch( Exception e )
        {
            log.error("Failed to create weblog entry",e);
            throw new XmlRpcException( 0, "Failed to create weblog entry: "+e.getMessage() );
        }

        return ""; // FIXME:
    }

    public Hashtable newMediaObject( String blogid, 
                                     String username,
                                     String password,
                                     Hashtable content )
        throws XmlRpcException
    {
        String url = "";

        log.info("metaWeblog.newMediaObject() called");

        String name = (String) content.get( "name" );
        byte[] data = (byte[]) content.get( "bits" );

        AttachmentManager attmgr = m_engine.getAttachmentManager();

        try
        {
            Attachment att = new Attachment( blogid, name );
            att.setAuthor( username );
            attmgr.storeAttachment( att, new ByteArrayInputStream( data ) );

            url = m_engine.getBaseURL()+"attach?page="+att.getName();
        }
        catch( Exception e )
        {
            log.error( "Failed to upload attachment", e );
            throw new XmlRpcException( 0, "Failed to upload media object: "+e.getMessage() );
        }

        Hashtable result = new Hashtable();
        result.put("url", url);

        return result;
    }
}
